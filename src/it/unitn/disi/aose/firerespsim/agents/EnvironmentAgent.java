package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.FireResponseOntology;
import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.SimulationArea;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensions;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensionsInfo;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensionsRequest;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.OnFireStatus;
import it.unitn.disi.aose.firerespsim.ontology.OnFireStatusRequest;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
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
    
    /**
     * Codec for message content encoding. Package scoped for faster access by inner classes.
     */
    final Codec codec = new SLCodec();
    /**
     * Simulation ontology. Package scoped for faster access by inner classes.
     */
    final Ontology onto = FireResponseOntology.getInstance();
    
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        logger.debug("starting up");
        
        super.setup();
        
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(onto);
        
        // read start-up arguments
        Object[] params = getArguments();
        if (params == null) {
            params = new Object[] {};
        }
        area = new SimulationArea(new AreaDimensions((params.length > 0) ? (Integer) params[0] : DEFAULT_AREA_WIDTH,
                                                     (params.length > 1) ? (Integer) params[1] : DEFAULT_AREA_HEIGHT));
        final int spawnFireIval = (params.length > 2) ? (Integer) params[2] : DEFAULT_SPAWN_FIRE_IVAL;
        fireIncreaseIval = (params.length > 3) ? (Integer) params[3] : DEFAULT_FIRE_INCREASE_IVAL;
        
        // add behaviors
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new RequestDispatchService(), new SpawnFire(this, spawnFireIval)}));
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
     * Service that accepts all request messages and dispatches them to the corresponding methods.
     */
    class RequestDispatchService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology(onto.getName()));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            ContentElement ce;
            try {
                ce = getContentManager().extractContent(requestMsg);
            } catch (final Exception e) {
                logger.error("error extracting message content");
                e.printStackTrace();
                return;
            }
            if (ce instanceof AreaDimensionsRequest) {
                sendAreaDimensions(requestMsg, (AreaDimensionsRequest) ce);
            } else if (ce instanceof OnFireStatusRequest) {
                sendOnFireStatus(requestMsg, (OnFireStatusRequest) ce);
            } else {
                logger.error("request message has unrecognized content");
            }
        }
    }
    
    /**
     * Sends the area dimensions. Are requested by the monitor agent. Package scoped for faster access by inner classes.
     * 
     * @param requestMsg
     * @param request
     */
    void sendAreaDimensions(final ACLMessage requestMsg, final AreaDimensionsRequest request) {

//      logger.debug("received request for area dimensions");
        
        final ACLMessage replyMsg = requestMsg.createReply();
        replyMsg.setPerformative(ACLMessage.INFORM);
        
        try {
            getContentManager().fillContent(replyMsg, new AreaDimensionsInfo(area.dimensions));
            send(replyMsg);
//                logger.debug("sent area dimensions reply");
        } catch (final Exception e) {
            logger.error("error filling message content");
        }
    }
    
    /**
     * Sends the on fire status for an area position. Package scoped for faster access by inner classes.
     * 
     * @param requestMsg
     * @param request
     */
    void sendOnFireStatus(final ACLMessage requestMsg, final OnFireStatusRequest request) {

        final Coordinate requestCoord = request.getCoordinate();
        
//      logger.debug("received on fire status request for coordinate (" + requestCoord + ")");
        
        if (area.getOnFireState(requestCoord)) {
            // check if fire agent still alive (fire still burning)
            AgentController fireAgent = null;
            try {
                fireAgent = getContainerController().getAgent("fire " + requestCoord);
            } catch (final ControllerException e) {
                // pass
            }
            if (fireAgent == null) {
                logger.debug("fire at (" + requestCoord + ") no longer burning");
                area.setOnFireState(requestCoord, false);
            }
        }
        
        // send fire status
        final ACLMessage replyMsg = requestMsg.createReply();
        replyMsg.setPerformative(ACLMessage.INFORM);
        try {
            getContentManager().fillContent(replyMsg, new OnFireStatus(requestCoord, area.getOnFireState(requestCoord)));
            send(replyMsg);
//                logger.debug("sent on fire status reply");
        } catch (final Exception e) {
            logger.error("error filling message content");
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
                firePosition.setRow(RandomUtils.nextInt(area.dimensions.getHeight() - 1) + 1);
                firePosition.setCol(RandomUtils.nextInt(area.dimensions.getWidth() - 1) + 1);
            } while (area.getOnFireState(firePosition.getCoordinate()));
            
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
            area.setOnFireState(firePosition.getCoordinate(), true);
            
            logger.info("started fire at (" + firePosition + ")");
        }
    }
}
