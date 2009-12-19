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
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireMonitorAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Package scoped for faster access by inner classes.
     */
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    /**
     * Package scoped for faster access by inner classes.
     */
    private final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
    private static final int DEFAULT_SCAN_AREA_IVAL = 10000;
    
    private final DFAgentDescription areaDimensionsAD = new DFAgentDescription();
    private final ServiceDescription areaDimensionsSD = new ServiceDescription();
    private AID areaDimensionsAID = null;
    private final DFAgentDescription fireStatusAD = new DFAgentDescription();
    private final ServiceDescription fireStatusSD = new ServiceDescription();
    private AID fireStatusAID = null;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
        
        areaDimensionsSD.setType("AreaDimensions");
        areaDimensionsAD.addServices(areaDimensionsSD);
        fireStatusSD.setType("FireStatus");
        fireStatusAD.addServices(fireStatusSD);
        
        // initialization parameters
        int scanAreaIval = 0;
        Object[] params = getArguments();
        if (params == null) {
            params = new Object[] {};
        }
        scanAreaIval = (params.length > 0) ? (Integer) params[0] : DEFAULT_SCAN_AREA_IVAL;
        
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
        
        // register at DF service
        final DFAgentDescription descr = new DFAgentDescription();
        final ServiceDescription sd = new ServiceDescription();
        sd.setName("FireAlert");
        sd.setType("FireMonitor");
        descr.addServices(sd);
        try {
            DFService.register(this, descr);
        } catch (final FIPAException e) {
            logger.error("cannot register at DF");
            e.printStackTrace();
            doDelete();
        }
    }
    
    /**
     * @see jade.core.Agent#takeDown()
     */
    @Override
    protected void takeDown() {

        logger.debug("takeDown");
        
        for (final Behaviour b : threadedBehaviours) {
            if (b != null) {
                tbf.getThread(b).interrupt();
            }
        }
        
        super.takeDown();
    }
    
    /**
     * Package scoped for faster access by inner classes.
     * 
     * @return AID of the environment agent providing an AreaDimensions service
     */
    AID getAreaDimensionsAID() {

        if (areaDimensionsAID != null) return areaDimensionsAID;
        
        DFAgentDescription[] result = null;
        try {
            result = DFService.search(this, areaDimensionsAD);
        } catch (final FIPAException e) {
            logger.error("no environment agent with AreaDimensions service at DF");
            e.printStackTrace();
        }
        
        areaDimensionsAID = (result != null && result.length > 0) ? result[0].getName() : null;
        logger.debug("areaDimensionsAID = " + areaDimensionsAID);
        return fireStatusAID;
    }
    
    /**
     * Package scoped for faster access by inner classes.
     * 
     * @return AID of the environment agent providing an FireStatus service
     */
    AID getFireStatusAID() {

        if (fireStatusAID != null) return fireStatusAID;
        
        DFAgentDescription[] result = null;
        try {
            result = DFService.search(this, fireStatusAD);
        } catch (final FIPAException e) {
            logger.error("no environment agent with FireStatus service at DF");
            e.printStackTrace();
        }
        
        fireStatusAID = (result != null && result.length > 0) ? result[0].getName() : null;
        logger.debug("fireStatusAID = " + fireStatusAID);
        return fireStatusAID;
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
     * @author tom
     */
    class GetAreaDimensions extends SimpleBehaviour {
        
        private boolean done = false;
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology("AreaDimensions"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final AID aid = getAreaDimensionsAID();
            if (aid == null) {
                logger.error("no AreaDimensions AID");
                return;
            }
            
            final ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(aid);
            requestMsg.setOntology("AreaDimensions");
            requestMsg.setContent(areaColumn + " " + areaRow); // XXX
            send(requestMsg);
            logger.debug("sent AreaDimensions request");
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            logger.debug("received AreaDimensions reply");
            final String[] areaDimensions = replyMsg.getContent().split(" ");
            areaWidth = Integer.parseInt(areaDimensions[0]);
            areaHeight = Integer.parseInt(areaDimensions[1]);
            logger.info("got AreaDimensions: " + areaWidth + "x" + areaHeight);
            
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
        
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology("FireStatus"));
        
        /**
         * @param a
         * @param period
         */
        public ScanArea(final Agent a, final long period) {

            super(a, period);
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
            
            final AID aid = getFireStatusAID();
            if (aid == null) {
                logger.error("no FireStatus AID");
                return;
            }
            
            // get fire status for current position
            final ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(aid);
            requestMsg.setOntology("FireStatus");
            requestMsg.setContent(areaColumn + " " + areaRow);
            send(requestMsg);
            logger.debug("sent FireStatus request");
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            logger.debug("received FireStatus reply");
            if (Boolean.parseBoolean(replyMsg.getContent())) {
                // position is on fire
                logger.info("current position (" + areaColumn + ", " + areaRow + ") is on fire");
                // TODO tell registered agents
            } else {
                // position is not on fire
                logger.debug("current position (" + areaColumn + ", " + areaRow + ") is not on fire");
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
            logger.info("moved to position (" + areaColumn + ", " + areaRow + ")");
        }
    }
    
    /**
     * @author tom
     */
    class FireAlertService extends CyclicBehaviour {
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

        // TODO Auto-generated method stub
        
        }
        
    }
}
