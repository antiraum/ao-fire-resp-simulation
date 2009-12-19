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
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class EnvironmentAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    private static final int DEFAULT_AREA_WIDTH = 5;
    private static final int DEFAULT_AREA_HEIGHT = 5;
    private static final int DEFAULT_SPAWN_FIRE_IVAL = 100000;
    
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
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    /**
     * Package scoped for faster access by inner classes.
     */
    private final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
        
        // initialization parameters
        int spawnFireIval = 0;
        Object[] params = getArguments();
        if (params == null) {
            params = new Object[] {};
        }
        areaWidth = (params.length > 0) ? (Integer) params[0] : DEFAULT_AREA_WIDTH;
        areaHeight = (params.length > 1) ? (Integer) params[1] : DEFAULT_AREA_HEIGHT;
        spawnFireIval = (params.length > 2) ? (Integer) params[2] : DEFAULT_SPAWN_FIRE_IVAL;
        
        // initialize fire statuses
        fireStatuses = new boolean[areaHeight][areaWidth];
        for (int row = 0; row < areaHeight; row++) {
            for (int col = 0; col < areaWidth; col++) {
                fireStatuses[row][col] = false;
            }
        }
        
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

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received AreaDimensions request");
            
            // send area dimensions
            final ACLMessage replyMsg = requestMsg.createReply();
            replyMsg.setPerformative(ACLMessage.INFORM);
            replyMsg.setContent(areaWidth + " " + areaHeight);
            replyMsg.setOntology("AreaDimensions");
            replyMsg.addReceiver(requestMsg.getSender());
            send(replyMsg);
            logger.debug("sent AreaDimensions reply");
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

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received FireStatus request");
            
            // get requested position
            final String[] position = requestMsg.getContent().split(" ");
            final int row = Integer.parseInt(position[0]);
            final int col = Integer.parseInt(position[1]);
            logger.debug("requested position (" + row + ", " + col + ")");
            
            // send fire status
            final ACLMessage replyMsg = requestMsg.createReply();
            replyMsg.setPerformative(ACLMessage.INFORM);
            replyMsg.setContent(Boolean.toString(fireStatuses[row - 1][col - 1]));
            replyMsg.setOntology("FireStatus");
            replyMsg.addReceiver(requestMsg.getSender());
            send(replyMsg);
            logger.debug("sent FireStatus reply");
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

            int row, col;
            do {
                row = RandomUtils.nextInt(areaHeight - 1) + 1;
                col = RandomUtils.nextInt(areaWidth - 1) + 1;
            } while (fireStatuses[row - 1][col - 1]);
            final int accel = RandomUtils.nextInt(100);
            
            // spawn fire agent
            try {
                final AgentController fireAgent = getContainerController().createNewAgent(
                                                                                          "fire (" + row + "," + col +
                                                                                                  ")",
                                                                                          FireAgent.class.getName(),
                                                                                          new Object[] {row, col, accel});
                fireAgent.start();
                logger.debug("started agent " + fireAgent.getName());
            } catch (final StaleProxyException e) {
                logger.error("couldn't start fire agent");
                e.printStackTrace();
            }
            
            // set fire status
            fireStatuses[row - 1][col - 1] = true;
            
            logger.info("created fire at (" + row + ", " + col + ") with acceleration " + accel);
        }
        
    }
}
