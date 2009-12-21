package it.unitn.disi.aose.firerespsim;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * This agent scans the simulation area for new fires. Agents can subscribe to get notified about newly detected fires.
 * Corresponds to the 911 emergency service in the real world. Start-up parameter is the scan area interval.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireMonitorAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Defaults for start-up arguments.
     */
    private static final int DEFAULT_SCAN_AREA_IVAL = 10000;
    
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
    /**
     * Package scoped for faster access by inner classes.
     */
    Agent thisAgent = this;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
        
        // read start-up arguments
        Object[] params = getArguments();
        if (params == null) {
            params = new Object[] {};
        }
        final int scanAreaIval = (params.length > 0) ? (Integer) params[0] : DEFAULT_SCAN_AREA_IVAL;
        
        // add behaviors
        final SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new GetAreaDimensions());
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new ScanArea(this, scanAreaIval), new FireAlertService()}));
        final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        sb.addSubBehaviour(pb);
        addBehaviour(sb);
        
        // register at the DF
        final DFAgentDescription descr = new DFAgentDescription();
        final ServiceDescription sd = new ServiceDescription();
        sd.setName(getName());
        sd.setType("FireAlert");
        descr.addServices(sd);
        try {
            DFService.register(this, descr);
        } catch (final FIPAException e) {
            logger.error("cannot register at the DF");
            e.printStackTrace();
            doDelete();
        }
    }
    
    /**
     * @see jade.core.Agent#takeDown()
     */
    @Override
    protected void takeDown() {

        logger.info("shutting down");
        
        for (final Behaviour b : threadedBehaviours) {
            if (b != null) {
                tbf.getThread(b).interrupt();
            }
        }
        
        super.takeDown();
    }
    
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaWidth = 0;
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaHeight = 0;
    
    /**
     * Gets the area dimensions from the environment agent.
     */
    class GetAreaDimensions extends SimpleBehaviour {
        
        private final static String ONTOLOGY_TYPE = "AreaDimensions";
        
        private boolean done = false;
        private final DFAgentDescription areaDimensionsAD = new DFAgentDescription();
        private AID environmentAID = null;
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology(ONTOLOGY_TYPE));
        
        /**
         * Constructor
         */
        public GetAreaDimensions() {

            super();
            
            final ServiceDescription areaDimensionsSD = new ServiceDescription();
            areaDimensionsSD.setType(ONTOLOGY_TYPE);
            areaDimensionsAD.addServices(areaDimensionsSD);
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            if (environmentAID == null) {
                DFAgentDescription[] result = null;
                try {
                    result = DFService.search(thisAgent, areaDimensionsAD);
                } catch (final FIPAException e) {
                    logger.error("no agent with AreaDimensions service at DF");
                    e.printStackTrace();
                    return;
                }
                if (result != null && result.length > 0) {
                    environmentAID = result[0].getName();
                }
            }
            if (environmentAID == null) {
                logger.error("no AreaDimensions AID");
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
//                    done = true;
                }
                return;
            }
            
            final ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(environmentAID);
            requestMsg.setOntology(ONTOLOGY_TYPE);
            send(requestMsg);
            logger.debug("sent AreaDimensions request");
            
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            logger.debug("received AreaDimensions reply");
            final String[] areaDimensions = replyMsg.getContent().split(" ");
            areaWidth = Integer.parseInt(areaDimensions[0]);
            areaHeight = Integer.parseInt(areaDimensions[1]);
            logger.info("received area dimensions: " + areaWidth + "x" + areaHeight);
            
            done = true;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#done()
         */
        @Override
        public boolean done() {

            return done;
        }
    }
    
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaColumn = 1;
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaRow = 1;
    
    /**
     * @author tom
     */
    class ScanArea extends TickerBehaviour {
        
        private final static String ONTOLOGY_TYPE = "FireStatus";
        
        private final DFAgentDescription fireStatusAD = new DFAgentDescription();
        private AID environmentAID = null;
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology(ONTOLOGY_TYPE));
        
        private final Set<String> detectedFires = new HashSet<String>();
        
        /**
         * @param a
         * @param period
         */
        public ScanArea(final Agent a, final long period) {

            super(a, period);
            
            final ServiceDescription fireStatusSD = new ServiceDescription();
            fireStatusSD.setType(ONTOLOGY_TYPE);
            fireStatusAD.addServices(fireStatusSD);
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            if (areaWidth == 0 || areaHeight == 0) {
                // area dimensions not yet set
                logger.error("area dimensions not yet set");
                return;
            }
            
            if (environmentAID == null) {
                DFAgentDescription[] result = null;
                try {
                    result = DFService.search(thisAgent, fireStatusAD);
                } catch (final FIPAException e) {
                    logger.error("no agent with FireStatus service at DF");
                    e.printStackTrace();
                    return;
                }
                if (result != null && result.length > 0) {
                    environmentAID = result[0].getName();
                }
            }
            if (environmentAID == null) {
                logger.error("no FireStatus AID");
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
//                    done = true;
                }
                return;
            }
            
            // get fire status for current position
            logger.debug("scanning position (" + areaRow + ", " + areaColumn + ")");
            final ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(environmentAID);
            requestMsg.setOntology(ONTOLOGY_TYPE);
            requestMsg.setContent(areaRow + " " + areaColumn);
            send(requestMsg);
            logger.debug("sent FireStatus request");
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            logger.debug("received FireStatus reply");
            if (Boolean.parseBoolean(replyMsg.getContent())) {
                // position is on fire
                if (detectedFires.contains(areaRow + " " + areaColumn)) {
                    // known fire
                    logger.debug("detected known fire at (" + areaRow + ", " + areaColumn + ")");
                } else {
                    // new fire
                    logger.info("detected new fire at (" + areaRow + ", " + areaColumn + ")");
                    detectedFires.add(areaRow + " " + areaColumn);
                    if (fireAlertSubscribers.size() > 0) {
                        // tell registered agents
                        final ACLMessage alertMsg = new ACLMessage(ACLMessage.INFORM);
                        alertMsg.setOntology("FireAlert");
                        alertMsg.setContent(areaRow + " " + areaColumn);
                        for (final AID agentAID : fireAlertSubscribers) {
                            alertMsg.addReceiver(agentAID);
                        }
                        send(alertMsg);
                    }
                }
            } else {
                // position is not on fire
                logger.debug("no fire at (" + areaRow + ", " + areaColumn + ")");
                if (detectedFires.contains(areaRow + " " + areaColumn)) {
                    // remove known fire
                    logger.info("fire at (" + areaRow + ", " + areaColumn + ") no longer burning");
                    detectedFires.remove(areaRow + " " + areaColumn);
                }
            }
            
            // move to next position
            if (areaColumn == areaWidth) {
                // to new row
                areaColumn = 1;
                if (areaRow == areaHeight) {
                    // to first row
                    areaRow = 1;
                } else {
                    // to next row
                    areaRow++;
                }
            } else {
                // to next column
                areaColumn++;
            }
            logger.debug("moved to position (" + areaRow + ", " + areaColumn + ")");
        }
    }
    
    /**
     * Set of all Hospitals and Fire Brigades that subscribed to be notified about new fires. Package scoped for faster
     * access by inner classes.
     */
    final Set<AID> fireAlertSubscribers = new HashSet<AID>();
    
    /**
     * Service for subscribing to alerts for new fires.
     */
    class FireAlertService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
                                                                       MessageTemplate.MatchOntology("FireAlert"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received FireAlert request");
            
            final AID aid = requestMsg.getSender();
            final ACLMessage replyMsg = requestMsg.createReply();
            if (fireAlertSubscribers.contains(aid)) {
                // already subscribed
                replyMsg.setPerformative(ACLMessage.DISCONFIRM);
            } else {
                // new subscriber
                fireAlertSubscribers.add(aid);
                replyMsg.setPerformative(ACLMessage.CONFIRM);
            }
            send(replyMsg);
            logger.debug("sent FireAlert reply");
        }
        
    }
}
