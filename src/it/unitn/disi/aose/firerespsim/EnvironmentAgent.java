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
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

/**
 * This agent maintains the area of the simulation and generates new fires. Start-up parameters area simulation area
 * width, simulation area height, and fire spawn interval.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class EnvironmentAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Defaults for start-up arguments.
     */
    private static final int DEFAULT_AREA_WIDTH = 5;
    private static final int DEFAULT_AREA_HEIGHT = 5;
    private static final int DEFAULT_SPAWN_FIRE_IVAL = 100000;
    private static final int DEFAULT_FIRE_INCREASE_IVAL = 10000;
    
    /**
     * With of the simulation area. Package scoped for faster access by inner classes.
     */
    int areaWidth;
    /**
     * Height of the simulation area. Package scoped for faster access by inner classes.
     */
    int areaHeight;
    /**
     * On fire status of the positions in the simulation area. Package scoped for faster access by inner classes.
     */
    boolean[][] fireStatuses;
    /**
     * Increase interval parameter for the fire agents. Package scoped for faster access by inner classes.
     */
    int fireIncreaseIval;
    
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
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
        areaWidth = (params.length > 0) ? (Integer) params[0] : DEFAULT_AREA_WIDTH;
        areaHeight = (params.length > 1) ? (Integer) params[1] : DEFAULT_AREA_HEIGHT;
        final int spawnFireIval = (params.length > 2) ? (Integer) params[2] : DEFAULT_SPAWN_FIRE_IVAL;
        fireIncreaseIval = (params.length > 3) ? (Integer) params[3] : DEFAULT_FIRE_INCREASE_IVAL;
        
        // initialize on fire statuses
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
        
        // register at the DF
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

        logger.debug("takeDown");
        
        for (final Behaviour b : threadedBehaviours) {
            if (b != null) {
                tbf.getThread(b).interrupt();
            }
        }
        
        super.takeDown();
    }
    
    /**
     * Service that provides the area dimensions. Used by the monitor agent. Content of the reply message consists of
     * area width and height separated by a space.
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
            send(replyMsg);
            logger.debug("sent AreaDimensions reply");
        }
    }
    
    /**
     * Service that provides the on fire status for an area position. Content of the request message must consist of row
     * and column separated by a space.
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
            if (requestMsg.getContent() == null) {
                logger.error("request message has no content");
                return;
            }
            final String[] requestContent = requestMsg.getContent().split(" ");
            if (requestContent.length != 2) {
                logger.error("request message has wrong format");
                return;
            }
            final int row = Integer.parseInt(requestContent[0]);
            final int col = Integer.parseInt(requestContent[1]);
            logger.debug("requested position (" + row + ", " + col + ")");
            
            if (fireStatuses[row - 1][col - 1]) {
                // check if fire agent still alive (fire still burning)
                AgentController fireAgent = null;
                try {
                    fireAgent = getContainerController().getAgent("fire " + row + "-" + col);
                } catch (final ControllerException e) {
                    // pass
                }
                if (fireAgent == null) {
                    logger.debug("fire at (" + row + ", " + col + ") no longer burning");
                    fireStatuses[row - 1][col - 1] = false;
                }
            }
            
            // send fire status
            final ACLMessage replyMsg = requestMsg.createReply();
            replyMsg.setPerformative(ACLMessage.INFORM);
            replyMsg.setContent(Boolean.toString(fireStatuses[row - 1][col - 1]));
            send(replyMsg);
            logger.debug("sent FireStatus reply");
        }
    }
    
    /**
     * Starts a new fire at a random position (that is not yet on fire).
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
            
            // spawn fire agent
            try {
                final AgentController fireAgent = getContainerController().createNewAgent(
                                                                                          "fire " + row + "-" + col,
                                                                                          FireAgent.class.getName(),
                                                                                          new Object[] {
                                                                                              row, col,
                                                                                              fireIncreaseIval});
                fireAgent.start();
                logger.debug("started agent " + fireAgent.getName());
            } catch (final StaleProxyException e) {
                logger.error("couldn't start fire agent");
                e.printStackTrace();
            }
            
            // set fire status
            fireStatuses[row - 1][col - 1] = true;
            
            logger.info("created fire at (" + row + ", " + col + ")");
        }
    }
}
