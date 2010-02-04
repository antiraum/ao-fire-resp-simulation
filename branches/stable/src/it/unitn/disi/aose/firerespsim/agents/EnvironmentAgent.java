package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.SimulationArea;
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
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class EnvironmentAgent extends Agent {
    
    /**
     * DF type of area dimension service. Package scoped for faster access by inner classes.
     */
    final static String AREA_DIM_DF_TYPE = "AreaDimensions";
    /**
     * DF type of on fire status service. Package scoped for faster access by inner classes.
     */
    final static String ON_FIRE_STATUS_DF_TYPE = "OnFireStatus";
    /**
     * Ontology type of area dimension messages. Package scoped for faster access by inner classes.
     */
    final static String AREA_DIMENSIONS_ONT_TYPE = "AreaDimensions";
    /**
     * Ontology type of on fire status messages. Package scoped for faster access by inner classes.
     */
    final static String ON_FIRE_STATUS_ONT_TYPE = "OnFireStatus";
    
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
     * Representation of the simulation area. Package scoped for faster access by inner classes.
     */
    SimulationArea area;
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

        logger.debug("starting up");
        
        super.setup();
        
        // read start-up arguments
        Object[] params = getArguments();
        if (params == null) {
            params = new Object[] {};
        }
        area = new SimulationArea((params.length > 0) ? (Integer) params[0] : DEFAULT_AREA_WIDTH,
                                  (params.length > 1) ? (Integer) params[1] : DEFAULT_AREA_HEIGHT);
        final int spawnFireIval = (params.length > 2) ? (Integer) params[2] : DEFAULT_SPAWN_FIRE_IVAL;
        fireIncreaseIval = (params.length > 3) ? (Integer) params[3] : DEFAULT_FIRE_INCREASE_IVAL;
        
        // add behaviors
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new AreaDimensionsService(), new OnFireStatusService(), new SpawnFire(this, spawnFireIval)}));
        final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        addBehaviour(pb);
        
        // register at the DF
        final DFAgentDescription descr = new DFAgentDescription();
        final ServiceDescription areaDimensionsSD = new ServiceDescription();
        areaDimensionsSD.setName(getName());
        areaDimensionsSD.setType(AREA_DIM_DF_TYPE);
        descr.addServices(areaDimensionsSD);
        final ServiceDescription onFireStatusSD = new ServiceDescription();
        onFireStatusSD.setName(getName());
        onFireStatusSD.setType(ON_FIRE_STATUS_DF_TYPE);
        descr.addServices(onFireStatusSD);
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
     * Service that provides the area dimensions. Used by the monitor agent. Content of the reply message is
     * {@link SimulationArea#toString()}.
     */
    class AreaDimensionsService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology(EnvironmentAgent.AREA_DIMENSIONS_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
//            logger.debug("received request for area dimensions");
            
            // send area dimensions
            final ACLMessage replyMsg = requestMsg.createReply();
            replyMsg.setPerformative(ACLMessage.INFORM);
            replyMsg.setContent(area.toString());
            send(replyMsg);
//            logger.debug("sent area dimensions reply");
        }
    }
    
    /**
     * Service that provides the on fire status for an area position. Content of the request message must consist of
     * {@link Position#toString()} of the requested position.
     */
    class OnFireStatusService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology(EnvironmentAgent.ON_FIRE_STATUS_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            // get requested position
            if (requestMsg.getContent() == null) {
                logger.error("request message has no content");
                return;
            }
            final Position requestPosition = Position.fromString(requestMsg.getContent());
//            logger.debug("received on fire status request for position (" + requestPosition + ")");
            
            if (area.getOnFireState(requestPosition)) {
                // check if fire agent still alive (fire still burning)
                AgentController fireAgent = null;
                try {
                    fireAgent = getContainerController().getAgent("fire " + requestPosition);
                } catch (final ControllerException e) {
                    // pass
                }
                if (fireAgent == null) {
                    logger.debug("fire at (" + requestPosition + ") no longer burning");
                    area.setOnFireState(requestPosition, false);
                }
            }
            
            // send fire status
            final ACLMessage replyMsg = requestMsg.createReply();
            replyMsg.setPerformative(ACLMessage.INFORM);
            replyMsg.setContent(Boolean.toString(area.getOnFireState(requestPosition)));
            send(replyMsg);
//            logger.debug("sent on fire status reply");
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

            final Position firePosition = new Position(0, 0);
            do {
                firePosition.setRow(RandomUtils.nextInt(area.height - 1) + 1);
                firePosition.setCol(RandomUtils.nextInt(area.width - 1) + 1);
            } while (area.getOnFireState(firePosition));
            
            // spawn fire agent
            try {
                final AgentController fireAgent = getContainerController().createNewAgent(
                                                                                          FireAgent.FIRE_AGENT_NAME_PREFIX +
                                                                                                  firePosition,
                                                                                          FireAgent.class.getName(),
                                                                                          new Object[] {
                                                                                              firePosition.getRow(),
                                                                                              firePosition.getCol(),
                                                                                              fireIncreaseIval});
                fireAgent.start();
            } catch (final StaleProxyException e) {
                logger.error("couldn't start fire");
                e.printStackTrace();
            }
            
            // set fire state
            area.setOnFireState(firePosition, true);
            
            logger.info("started fire at (" + firePosition + ")");
        }
    }
}
