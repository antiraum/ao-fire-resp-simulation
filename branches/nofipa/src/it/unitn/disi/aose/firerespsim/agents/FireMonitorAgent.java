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
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.SubscriptionResponder.Subscription;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Vector;

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
    final Set<Coordinate> detectedFires = new HashSet<Coordinate>();
    
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
        final GetAreaDimensions getAreaDim = new GetAreaDimensions(this, getAreaDimReqMsg, ds, areaDimAIDKey,
                                                                   areaDimKey);
        final FindAgent findOnFireStatusAgent = new FindAgent(this, EnvironmentAgent.ON_FIRE_STATUS_DF_TYPE,
                                                              onFireStatusAIDKey);
        findOnFireStatusAgent.setDataStore(ds);
        final MessageTemplate fireAlertSubsTpl = createMessageTemplate(null, FIRE_ALERT_PROTOCOL, ACLMessage.SUBSCRIBE);
        final ScanArea scanArea = new ScanArea(this, (Integer) params.get("SCAN_AREA_IVAL"), onFireStatusAIDKey,
                                               areaDimKey);
        scanArea.setDataStore(ds);
        final SubscriptionService fireAlertSubsService = new SubscriptionService(this, fireAlertSubsTpl,
                                                                                 fireAlertSubscribers);
        
        // add behaviors
        sequentialBehaviours.add(findAreaDimAgent);
        sequentialBehaviours.add(getAreaDim);
        sequentialBehaviours.add(findOnFireStatusAgent);
        parallelBehaviours.addAll(Arrays.asList(scanArea, fireAlertSubsService));
        addBehaviours();
    }
    
    /**
     * Gets the simulation area dimensions from the environment agent.
     */
    private class GetAreaDimensions extends AchieveREInitiator {
        
        private final String areaDimAIDKey;
        private final String areaDimKey;
        
        /**
         * @param a
         * @param msg
         * @param store
         * @param areaDimAIDKey Data store key of the area dimensions service AID.
         * @param areaDimKey Data store key of the area dimensions.
         */
        public GetAreaDimensions(final Agent a, final ACLMessage msg, final DataStore store,
                                 final String areaDimAIDKey, final String areaDimKey) {

            super(a, msg, store);
            this.areaDimAIDKey = areaDimAIDKey;
            this.areaDimKey = areaDimKey;
        }
        
        /**
         * @see jade.proto.AchieveREInitiator#prepareRequests(jade.lang.acl.ACLMessage)
         */
        @SuppressWarnings("unchecked")
        @Override
        protected Vector prepareRequests(final ACLMessage request) {

//            logger.debug("sending request for area dimensions");
            
            request.addReceiver((AID) getDataStore().get(areaDimAIDKey));
            return super.prepareRequests(request);
        }
        
        /**
         * @see jade.proto.AchieveREInitiator#handleInform(jade.lang.acl.ACLMessage)
         */
        @Override
        protected void handleInform(final ACLMessage inform) {

            try {
                getDataStore().put(areaDimKey,
                                   extractMessageContent(AreaDimensionsInfo.class, inform, false).getAreaDimensions());
            } catch (final Exception e) {
                return;
            }
            logger.info("received area dimensions");
        }
    }
    
    /**
     * Scans the simulation area line by line for fires. Starts a {@link GetOnFireStatus} for each coordinate to check.
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
            if (getOnFireStatus == null) {
                getOnFireStatus = new GetOnFireStatus(myAgent, thisReqMsg, getDataStore(), areaCoord.clone());
            } else {
                getOnFireStatus.reset(thisReqMsg, areaCoord.clone());
            }
            addParallelBehaviour(getOnFireStatus);
            
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
    
    /**
     * Instance of {@link GetOnFireStatus} that gets re-used for scanned coordinate. Package scoped for faster access by
     * inner classes.
     */
    GetOnFireStatus getOnFireStatus = null;
    
    /**
     * Gets the on fire status of a position on the simulation area from the environment agent. Stores the detected
     * fires in {@link #detectedFires}. Starts a {@link SendFireAlert} for every newly detected fire.
     */
    private class GetOnFireStatus extends AchieveREInitiator {
        
        private Coordinate fireCoord;
        
        /**
         * @param a
         * @param msg
         * @param store
         * @param fireCoord
         */
        public GetOnFireStatus(final Agent a, final ACLMessage msg, final DataStore store, final Coordinate fireCoord) {

            super(a, msg, store);
            this.fireCoord = fireCoord;
        }
        
        /**
         * @see jade.proto.AchieveREInitiator#handleInform(jade.lang.acl.ACLMessage)
         */
        @Override
        protected void handleInform(final ACLMessage inform) {

            boolean onFireStatus;
            try {
                onFireStatus = extractMessageContent(OnFireStatusInfo.class, inform, false).getStatus();
            } catch (final Exception e) {
                return;
            }
//            logger.debug("received on fire status");
            handleOnFireStatus(onFireStatus);
        }
        
        /**
         * @param onFireStatus
         */
        private void handleOnFireStatus(final boolean onFireStatus) {

            if (onFireStatus) {
                // position is on fire
                if (detectedFires.contains(fireCoord)) {
                    // known fire
                    logger.debug("detected known fire at (" + fireCoord + ")");
                } else {
                    // new fire
                    logger.info("detected new fire at (" + fireCoord + ")");
                    detectedFires.add(fireCoord);
                    if (sendFireAlert == null) {
                        sendFireAlert = new SendFireAlert(fireCoord);
                    } else {
                        sendFireAlert.reset(fireCoord);
                    }
                    addParallelBehaviour(sendFireAlert);
                }
            } else {
                // position is not on fire
//                logger.debug("no fire at (" + fireCoord + ")");
                if (detectedFires.contains(fireCoord)) {
                    // remove known fire
                    logger.info("fire at (" + fireCoord + ") no longer burning");
                    detectedFires.remove(fireCoord);
                }
            }
        }
        
        /**
         * @param msg
         * @param fireCoord
         */
        public void reset(final ACLMessage msg, final Coordinate fireCoord) {

            super.reset(msg);
            this.fireCoord = fireCoord;
        }
    }
    
    /**
     * Instance of {@link SendFireAlert} that gets re-used for every fire.
     */
    SendFireAlert sendFireAlert = null;
    
    /**
     * Sends a fire alert to all subscribers.
     */
    private class SendFireAlert extends OneShotBehaviour {
        
        private Coordinate fireCoord;
        
        /**
         * @param fireCoord
         */
        public SendFireAlert(final Coordinate fireCoord) {

            this.fireCoord = fireCoord;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final Set<Subscription> fireAlertSubscriptions = fireAlertSubscribers.getSubscriptions();
            if (fireAlertSubscriptions.isEmpty()) {
                logger.error("no agents registered to send fire alert to - fire will not be handled!");
                return;
            }
            final ACLMessage alertMsg = createMessage(ACLMessage.INFORM, FIRE_ALERT_PROTOCOL,
                                                      Arrays.asList(new AID[] {}), new FireAlert(fireCoord));
            for (final Subscription sub : fireAlertSubscriptions) {
                sub.notify(alertMsg);
            }
            logger.debug("sent alert for fire at (" + fireCoord + ")");
        }
        
        public void reset(final Coordinate fireCoord) {

            super.reset();
            this.fireCoord = fireCoord;
        }
    }
}
