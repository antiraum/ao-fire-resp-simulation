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
    static Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    private static final DFAgentDescription areaDimensionsAD = new DFAgentDescription();
    private static final ServiceDescription areaDimensionsSD = new ServiceDescription();
    private AID areaDimensionsAID = null;
    private static final DFAgentDescription fireStatusAD = new DFAgentDescription();
    private static final ServiceDescription fireStatusSD = new ServiceDescription();
    private AID fireStatusAID = null;
    
    /**
     * Package scoped for faster access by inner classes.
     */
    final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    /**
     * Package scoped for faster access by inner classes.
     */
    final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        logger.debug("setup start");
        
        super.setup();
        
//        BasicConfigurator.configure();
        
        areaDimensionsSD.setType("AreaDimensions");
        areaDimensionsAD.addServices(areaDimensionsSD);
        fireStatusSD.setType("FireStatus");
        fireStatusAD.addServices(fireStatusSD);
        
        // add behaviors
        final SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new GetAreaDimensions());
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {new ScanArea(this, 1000), new FireAlertService()}));
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
        
        logger.debug("setup end");
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

            logger.debug("action start");
            
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
            logger.info("sent AreaDimensions request");
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            logger.info("received AreaDimensions reply");
            final String[] areaDimensions = replyMsg.getContent().split(" ");
            areaWidth = Integer.parseInt(areaDimensions[0]);
            areaHeight = Integer.parseInt(areaDimensions[1]);
            logger.info("AreaDimensions: " + areaWidth + "x" + areaHeight);
            
            done = true;
            
            logger.debug("action end");
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

            logger.debug("onTick start");
            
            if (areaWidth == 0 || areaHeight == 0) {
                // area dimensions not yet set
                logger.error("area dimensions not yet set");
                return;
            }
            
            final AID aid = getFireStatusAID();
            if (aid == null) {
                logger.info("no FireStatus AID");
                return;
            }
            
            // get fire status for current position
            final ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(aid);
            requestMsg.setOntology("FireStatus");
            requestMsg.setContent(areaColumn + " " + areaRow);
            send(requestMsg);
            logger.info("sent FireStatus request");
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            logger.info("received FireStatus reply");
            if (Boolean.parseBoolean(replyMsg.getContent())) {
                // position is on fire
                logger.info("current position (" + areaColumn + ", " + areaRow + ") is on fire");
                // TODO tell registered agents
            } else {
                // position is not on fire
                logger.info("current position (" + areaColumn + ", " + areaRow + ") is not on fire");
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
            
            logger.debug("onTick end");
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
