package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.SimulationArea;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensions;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensionsInfo;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.OnFireStatusInfo;
import it.unitn.disi.aose.firerespsim.ontology.OnFireStatusRequest;
import it.unitn.disi.aose.firerespsim.util.AgentUtil;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.apache.commons.lang.math.RandomUtils;

/**
 * This agent maintains the area of the simulation and generates new fires. Start-up parameters area simulation area
 * width, simulation area height, and fire spawn interval.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class EnvironmentAgent extends ExtendedAgent {
    
    /**
     * DF type of area dimension service.
     */
    final static String AREA_DIMENSIONS_DF_TYPE = "AreaDimensions";
    /**
     * Protocol for area dimensions messages.
     */
    final static String AREA_DIMENSIONS_PROTOCOL = "AreaDimensions";
    /**
     * DF type of on fire status service.
     */
    final static String ON_FIRE_STATUS_DF_TYPE = "OnFireStatus";
    /**
     * Protocol for on fire status messages.
     */
    final static String ON_FIRE_STATUS_PROTOCOL = "OnFireStatus";
    
    /**
     * Model of the simulation area. Package scoped for faster access by inner classes.
     */
    SimulationArea area;
    /**
     * Increase interval to be passed to started fire agents.
     */
    int fireIncreaseIval;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        params = new LinkedHashMap<String, Object>() {
            
            {
                put("AREA_WIDTH", 10);
                put("AREA_HEIGHT", 10);
                put("SPAWN_FIRE_IVAL", 100000);
                put("FIRE_INCREASE_IVAL", 10000);
            }
        };
        
        dfTypes = new String[] {AREA_DIMENSIONS_DF_TYPE, ON_FIRE_STATUS_DF_TYPE};
        
        super.setup();
        
        area = new SimulationArea(new AreaDimensions((Integer) params.get("AREA_WIDTH"),
                                                     (Integer) params.get("AREA_HEIGHT")));
        fireIncreaseIval = (Integer) params.get("FIRE_INCREASE_IVAL");
        
        // create behaviors
        final MessageTemplate areaDimReqTpl = createMessageTemplate(null, AREA_DIMENSIONS_PROTOCOL, ACLMessage.REQUEST);
        final AreaDimensionsService areaDimService = new AreaDimensionsService(this, areaDimReqTpl);
        final MessageTemplate onFireStatusReqTpl = createMessageTemplate(null, ON_FIRE_STATUS_PROTOCOL,
                                                                         ACLMessage.REQUEST);
        final OnFireStatusService onFireStatusService = new OnFireStatusService(this, onFireStatusReqTpl);
        final SpawnFire spawnFires = new SpawnFire(this, (Integer) params.get("SPAWN_FIRE_IVAL"));
        
        // add behaviors
        parallelBehaviours.addAll(Arrays.asList(areaDimService, onFireStatusService, spawnFires));
        addBehaviours();
    }
    
    /**
     * Provides the dimensions of the simulation area. Is used by the fire monitor agent.
     */
    private class AreaDimensionsService extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public AreaDimensionsService(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage request = blockingReceive(mt);
            if (request == null) return;
            
            sendReply(request, ACLMessage.INFORM, new AreaDimensionsInfo(area.dimensions));
        }
    }
    
    /**
     * Provides the on fire status of a coordinate on the simulation area. Is used by the fire monitor agent.
     */
    private class OnFireStatusService extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public OnFireStatusService(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage request = blockingReceive(mt);
            if (request == null) return;
            
            Coordinate coord;
            try {
                coord = extractMessageContent(OnFireStatusRequest.class, request, false).getCoordinate();
            } catch (final Exception e) {
                sendReply(request, ACLMessage.NOT_UNDERSTOOD, "could not read request message content");
                return;
            }
            
//            logger.debug("received on fire status request for (" + coord + ")");
            
            if (area.getOnFireState(coord)) {
                
                // check if fire agent still alive (fire still burning)
                AgentController fireAgent = null;
                try {
                    fireAgent = getContainerController().getAgent(FireAgent.FIRE_AGENT_NAME_PREFIX + coord);
                } catch (final ControllerException e) {
                    // pass
                }
                if (fireAgent == null) {
                    logger.debug("fire at (" + coord + ") no longer burning");
                    area.setOnFireState(coord, false);
                }
            }
            
            sendReply(request, ACLMessage.INFORM, new OnFireStatusInfo(coord, area.getOnFireState(coord)));
        }
    }
    
    /**
     * Starts a new fire at a random position (that is not yet on fire).
     */
    private class SpawnFire extends TickerBehaviour {
        
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

            // find fire position
            final Position firePosition = new Position(0, 0);
            do {
                firePosition.setRow(RandomUtils.nextInt(area.dimensions.getHeight() - 1) + 1);
                firePosition.setCol(RandomUtils.nextInt(area.dimensions.getWidth() - 1) + 1);
            } while (area.getOnFireState(firePosition.getCoordinate()));
            
            // start fire agent
            AgentUtil.startAgent(getContainerController(), FireAgent.FIRE_AGENT_NAME_PREFIX + firePosition,
                                 FireAgent.class.getName(), new Object[] {
                                     firePosition.getRow(), firePosition.getCol(), fireIncreaseIval});
            
            // set fire state
            area.setOnFireState(firePosition.getCoordinate(), true);
            
            logger.info("started fire at (" + firePosition + ")");
        }
    }
}
