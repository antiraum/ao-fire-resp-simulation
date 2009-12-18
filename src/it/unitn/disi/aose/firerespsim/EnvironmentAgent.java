package it.unitn.disi.aose.firerespsim;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
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
public final class EnvironmentAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    private static final int DEFAULT_AREA_WIDTH = 20;
    private static final int DEFAULT_AREA_HEIGHT = 20;
    private static final int DEFAULT_SPAWN_FIRE_IVAL = 10000;
    private static final int DEFAULT_NUMBER_OF_HOSPITALS = 3;
    private static final int DEFAULT_NUMBER_OF_FIRE_BRIGADES = 3;
    
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaWidth = 0;
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaHeight = 0;
    /**
     * Package scoped for faster access by inner classes.
     */
    boolean[][] fireStatuses;
    /**
     * Package scoped for faster access by inner classes.
     */
    int spawnFireIval = 0;
    
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
        
        // initialization parameters
        int numberOfHospitals = 0;
        final int numberOfFireBrigades = 0;
        
        final Object[] params = getArguments();
        if (params != null) {
            // TODO let user control initialization parameters
        }
        
        if (areaWidth == 0) {
            areaWidth = DEFAULT_AREA_WIDTH;
        }
        if (areaHeight == 0) {
            areaHeight = DEFAULT_AREA_HEIGHT;
        }
        if (spawnFireIval == 0) {
            spawnFireIval = DEFAULT_SPAWN_FIRE_IVAL;
        }
        if (numberOfHospitals == 0) {
            numberOfHospitals = DEFAULT_NUMBER_OF_HOSPITALS;
        }
        if (numberOfFireBrigades == 0) {
            numberOfHospitals = DEFAULT_NUMBER_OF_FIRE_BRIGADES;
        }
        
        // initialize fire statuses
        fireStatuses = new boolean[areaHeight][areaWidth];
        for (int row = 0; row < areaHeight; row++) {
            for (int col = 0; col < areaWidth; col++) {
                fireStatuses[row][col] = false;
            }
        }
        
        // simulation initialization
        // TODO spawn agents
        
        // add behaviors
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new AreaDimensionsService(), new FireStatusService(), new SpawnFire(this, spawnFireIval)}));
        final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        addBehaviour(pb);
        
        // register at DF service
        final DFAgentDescription descr = new DFAgentDescription();
        final ServiceDescription areaDimensionsSD = new ServiceDescription();
        areaDimensionsSD.setName(getName());
        areaDimensionsSD.setType("AreaDimensions");
        descr.addServices(areaDimensionsSD);
        final ServiceDescription fireStatusSD = new ServiceDescription();
        fireStatusSD.setName(getName());
        fireStatusSD.setType("FireStatus");
        descr.addServices(fireStatusSD);
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
     * @author tom
     */
    class AreaDimensionsService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology("AreaDimensions"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            logger.debug("action start");
            
            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.info("received AreaDimensions request");
            
            // send area dimensions
            final ACLMessage replyMsg = requestMsg.createReply();
            replyMsg.setPerformative(ACLMessage.INFORM);
            replyMsg.setContent(areaWidth + " " + areaHeight);
            replyMsg.setOntology("AreaDimensions");
            replyMsg.addReceiver(requestMsg.getSender());
            send(replyMsg);
            
            logger.debug("action end");
        }
    }
    
    /**
     * @author tom
     */
    class FireStatusService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology("FireStatus"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            logger.debug("action start");
            
            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.info("received FireStatus request");
            
            // get requested position
            final String[] position = requestMsg.getContent().split(" ");
            final int row = Integer.parseInt(position[0]);
            final int col = Integer.parseInt(position[1]);
            
            // send fire status
            final ACLMessage replyMsg = requestMsg.createReply();
            replyMsg.setPerformative(ACLMessage.INFORM);
            replyMsg.setContent(Boolean.toString(fireStatuses[row][col]));
            replyMsg.setOntology("FireStatus");
            replyMsg.addReceiver(requestMsg.getSender());
            send(replyMsg);
            
            logger.debug("action end");
        }
    }
    
    /**
     * @author tom
     */
    class SpawnFire extends TickerBehaviour {
        
        /**
         * @param a
         * @param period
         */
        public SpawnFire(final Agent a, final long period) {

            super(a, period);
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

        // TODO Auto-generated method stub
        
        }
        
    }
}
