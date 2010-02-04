package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.behaviours.FindAgent;
import it.unitn.disi.aose.firerespsim.behaviours.SubscriptionService;
import it.unitn.disi.aose.firerespsim.model.Subscribers;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensions;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensionsInfo;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireAlert;
import it.unitn.disi.aose.firerespsim.ontology.OnFireStatusInfo;
import it.unitn.disi.aose.firerespsim.ontology.OnFireStatusRequest;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * This agent scans the simulation area for new fires. Agents can subscribe to get notified about newly detected fires.
 * Corresponds to the 911 emergency service in the real world. Start-up parameter is the scan area interval.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireMonitorAgent extends ExtendedAgent {
    
    /**
     * DF type of fire alert service.
     */
    public final static String FIRE_ALERT_DF_TYPE = "FireMonitor";
    /**
     * Protocol for fire alert messages.
     */
    final static String FIRE_ALERT_PROTOCOL = "FireAlert";
    
    /**
     * Package scoped for faster access by inner classes.
     */
    final Subscribers fireAlertSubscribers = new Subscribers();
    /**
     * List of detected, currently burning fires. Package scoped for faster access by inner classes.
     */
    final Set<String> detectedFires = new HashSet<String>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        params = new LinkedHashMap<String, Object>() {
            
            {
                put("SCAN_AREA_IVAL", 10000);
            }
        };
        
        dfTypes = new String[] {FIRE_ALERT_DF_TYPE};
        
        super.setup();
        
        // create data store
        final String areaDimAIDKey = "AREA_DIMENSIONS_AID";
        final String onFireStatusAIDKey = "ON_FIRE_STATUS_AID";
        final String areaDimKey = "AREA_DIMENSIONS";
        final DataStore ds = new DataStore();
        
        // create behaviors
        final FindAgent findAreaDimAgent = new FindAgent(this, EnvironmentAgent.AREA_DIMENSIONS_DF_TYPE, areaDimAIDKey);
        findAreaDimAgent.setDataStore(ds);
        final ACLMessage getAreaDimReqMsg = createMessage(ACLMessage.REQUEST, EnvironmentAgent.AREA_DIMENSIONS_PROTOCOL);
        final RequestAreaDimensions reqAreaDim = new RequestAreaDimensions(this, getAreaDimReqMsg, areaDimAIDKey);
        reqAreaDim.setDataStore(ds);
        final MessageTemplate areaDimTpl = createMessageTemplate(null, REQUEST_RESPONSE_PERFORMATIVES,
                                                                 EnvironmentAgent.AREA_DIMENSIONS_PROTOCOL);
        final HandleAreaDimensions handleAreaDim = new HandleAreaDimensions(this, areaDimTpl, areaDimKey);
        handleAreaDim.setDataStore(ds);
        final FindAgent findOnFireStatusAgent = new FindAgent(this, EnvironmentAgent.ON_FIRE_STATUS_DF_TYPE,
                                                              onFireStatusAIDKey);
        findOnFireStatusAgent.setDataStore(ds);
        final MessageTemplate fireAlertSubsTpl = createMessageTemplate(null, SUBSCRIBE_REQUEST_PERFORMATIVES,
                                                                       FIRE_ALERT_PROTOCOL);
        final ScanArea scanArea = new ScanArea(this, (Integer) params.get("SCAN_AREA_IVAL"), onFireStatusAIDKey,
                                               areaDimKey);
        scanArea.setDataStore(ds);
        final MessageTemplate onFireStatusTpl = createMessageTemplate(null, REQUEST_RESPONSE_PERFORMATIVES,
                                                                      EnvironmentAgent.ON_FIRE_STATUS_PROTOCOL);
        final HandleOnFireStatus handleOnFireStatus = new HandleOnFireStatus(this, onFireStatusTpl);
        final SubscriptionService fireAlertSubsService = new SubscriptionService(this, fireAlertSubsTpl,
                                                                                 fireAlertSubscribers);
        
        // add behaviors
        sequentialBehaviours.add(findAreaDimAgent);
        sequentialBehaviours.add(reqAreaDim);
        sequentialBehaviours.add(handleAreaDim);
        sequentialBehaviours.add(findOnFireStatusAgent);
        parallelBehaviours.addAll(Arrays.asList(scanArea, handleOnFireStatus, fireAlertSubsService));
        addBehaviours();
    }
    
    /**
     * Gets the simulation area dimensions from the environment agent.
     */
    private class RequestAreaDimensions extends OneShotBehaviour {
        
        private final ACLMessage msg;
        private final String areaDimAIDKey;
        
        /**
         * @param a
         * @param msg
         * @param areaDimAIDKey Data store key of the area dimensions service AID.
         */
        public RequestAreaDimensions(final Agent a, final ACLMessage msg, final String areaDimAIDKey) {

            super(a);
            this.msg = msg;
            this.areaDimAIDKey = areaDimAIDKey;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            logger.debug("sending request for area dimensions");
            
            msg.addReceiver((AID) getDataStore().get(areaDimAIDKey));
            send(msg);
        }
    }
    
    private class HandleAreaDimensions extends OneShotBehaviour {
        
        private final MessageTemplate mt;
        private final String areaDimKey;
        
        /**
         * @param a
         * @param mt
         * @param areaDimKey Data store key of the area dimensions.
         */
        public HandleAreaDimensions(final Agent a, final MessageTemplate mt, final String areaDimKey) {

            super(a);
            this.mt = mt;
            this.areaDimKey = areaDimKey;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage response = blockingReceive(mt);
            if (response == null) return;
            
            if (response.getPerformative() == ACLMessage.INFORM) {
                try {
                    getDataStore().put(
                                       areaDimKey,
                                       extractMessageContent(AreaDimensionsInfo.class, response, false).getAreaDimensions());
                } catch (final Exception e) {
                    logger.error("cannot read area dimensions message content");
                    return;
                }
                logger.info("received area dimensions");
            } else {
                logger.error("received no area dimensions");
            }
        }
    }
    
    /**
     * Scans the simulation area line by line for fires. Sends a on fire status request for each coordinate to the
     * environment agent.
     */
    private class ScanArea extends TickerBehaviour {
        
        private final String onFireStatusAIDKey;
        private final String areaDimKey;
        private AreaDimensions areaDim = null;
        private final ACLMessage onFireStatusReqMsg = createMessage(ACLMessage.REQUEST,
                                                                    EnvironmentAgent.ON_FIRE_STATUS_PROTOCOL);
        
        private final Coordinate areaCoord = new Coordinate(1, 1);
        
        /**
         * @param a
         * @param period
         * @param onFireStatusAIDKey Data store key of the on fire status service AID.
         * @param areaDimKey Data store key of the area dimensions.
         */
        public ScanArea(final Agent a, final long period, final String onFireStatusAIDKey, final String areaDimKey) {

            super(a, period);
            this.onFireStatusAIDKey = onFireStatusAIDKey;
            this.areaDimKey = areaDimKey;
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            if (!onFireStatusReqMsg.getAllReceiver().hasNext()) {
                onFireStatusReqMsg.addReceiver((AID) getDataStore().get(onFireStatusAIDKey));
            }
            if (areaDim == null) {
                areaDim = (AreaDimensions) getDataStore().get(areaDimKey);
                if (areaDim == null) {
                    logger.error("area dimensions not set, cannot scan!");
                    return;
                }
            }
            
            // scan current coordinate
//            logger.debug("scanning coordinate (" + areaCoord + ")");
            final ACLMessage thisReqMsg = copyMessage(onFireStatusReqMsg);
            fillMessage(thisReqMsg, new OnFireStatusRequest(areaCoord));
            send(thisReqMsg);
            
            // move to next coordinate
            if (areaCoord.getCol() == areaDim.getWidth()) {
                // to new row
                areaCoord.setCol(1);
                if (areaCoord.getRow() == areaDim.getHeight()) {
                    // to first row
                    areaCoord.setRow(1);
                } else {
                    // to next row
                    areaCoord.increaseRow(1);
                }
            } else {
                // to next column
                areaCoord.increaseCol(1);
            }
//            logger.debug("moved to coordinate (" + areaCoord + ")");
        }
    }
    
    private class HandleOnFireStatus extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public HandleOnFireStatus(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage response = blockingReceive(mt);
            if (response == null) return;
            
            if (response.getPerformative() != ACLMessage.INFORM) return;
            
            OnFireStatusInfo onFireStatus;
            try {
                onFireStatus = extractMessageContent(OnFireStatusInfo.class, response, false);
            } catch (final Exception e) {
                return;
            }
//            logger.debug("received on fire status");
            
            final Coordinate coord = onFireStatus.getCoordinate();
            if (onFireStatus.getStatus()) {
                // position is on fire
                if (detectedFires.contains(coord.toString())) {
                    // known fire
//                    logger.debug("detected known fire at (" + coord + ")");
                } else {
                    // new fire
                    logger.info("detected new fire at (" + coord + ")");
                    detectedFires.add(coord.toString());
                    if (fireAlertSubscribers.isEmpty()) {
                        logger.error("no agents registered to send fire alert to - fire will not be handled!");
                        return;
                    }
                    sendMessage(ACLMessage.INFORM, FIRE_ALERT_PROTOCOL, fireAlertSubscribers.getAIDs(),
                                new FireAlert(coord));
                    logger.debug("sent alert for fire at (" + coord + ")");
                }
            } else {
                // position is not on fire
//                logger.debug("no fire at (" + coord + ")");
                if (detectedFires.contains(coord.toString())) {
                    // remove known fire
                    logger.info("fire at (" + coord + ") no longer burning");
                    detectedFires.remove(coord.toString());
                }
            }
        }
    }
}
