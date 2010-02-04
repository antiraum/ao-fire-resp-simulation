package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.behaviours.FindAgent;
import it.unitn.disi.aose.firerespsim.behaviours.Subscriber;
import it.unitn.disi.aose.firerespsim.model.SimulationArea;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.ontology.FireStatusInfo;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireCFP;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireProposal;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireProposalResponse;
import it.unitn.disi.aose.firerespsim.ontology.SetTargetRequest;
import it.unitn.disi.aose.firerespsim.ontology.VehicleStatus;
import it.unitn.disi.aose.firerespsim.ontology.VehicleStatusInfo;
import it.unitn.disi.aose.firerespsim.util.AgentUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.DataStore;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This is the super class for the fire brigade and hospital agents. Handles the communication with the
 * {@link CoordinatorAgent} and the owned {@link VehicleAgent}s and assigns the vehicle agents to the different fires it
 * is responsible for. Concrete subclasses can customize the behavior by implementing the abstract methods. Start-up
 * parameters are an id, the row, column, and the vehicle move interval.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public abstract class StationaryAgent extends ExtendedAgent {
    
    /**
     * Class of the vehicle agents this agent owns. Must be set in concrete subclasses.
     */
    protected String vehicleAgentClass;
    /**
     * Nickname of the vehicle agents this agent owns. Must be set in concrete subclasses.
     */
    protected String vehicleName;
    /**
     * DF service type of the coordinator to use. Must be set in the concrete subclasses.
     */
    protected String coordinatorDfType;
    
    /**
     * Vehicles. Package scoped for faster access by inner classes.
     */
    final Map<AID, VehicleStatus> vehicles = new HashMap<AID, VehicleStatus>();
    
    /**
     * Fires responsible for. Package scoped for faster access by inner classes.
     */
    final Map<String, FireStatus> fires = new HashMap<String, FireStatus>();
    /**
     * Fire vehicle distribution. Package scoped for faster access by inner classes.
     */
    final Map<String, Integer> fireVehicles = new HashMap<String, Integer>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        params = new LinkedHashMap<String, Object>() {
            
            {
                put("ID", null);
                put("ROW", null);
                put("COLUMN", null);
                put("VEHICLE_MOVE_IVAL", 10000);
            }
        };
        
        super.setup();
        
        final Coordinate position = new Coordinate((Integer) params.get("ROW"), (Integer) params.get("COLUMN"));
        
        // create vehicle agents
        final int numVehicles = 1;//RandomUtils.nextInt(4) + 1; // between 1 and 5
        final Object[] args = {getName(), position.getRow(), position.getCol(), params.get("VEHICLE_MOVE_IVAL")};
        for (int i = 0; i < numVehicles; i++) {
            final String nickname = vehicleName + " " + params.get("ID") + "-" + i;
            final AgentController ac = AgentUtil.startAgent(getContainerController(), nickname, vehicleAgentClass, args);
            try {
                vehicles.put(new AID(ac.getName(), true), null);
//                logger.debug("created '" + nickname + "'");
            } catch (final StaleProxyException e) {
                logger.error("error getting vehicle agent name");
            }
        }
        logger.info("created " + numVehicles + " vehicle agents");
        
        // create data store
        final String coordinatorAIDKey = "COORDINATOR_AID";
        final DataStore ds = new DataStore();
        
        // create behaviors
        final FindAgent findCoordinator = new FindAgent(this, coordinatorDfType, coordinatorAIDKey);
        findCoordinator.setDataStore(ds);
        final ACLMessage coordSubsMsg = createMessage(ACLMessage.SUBSCRIBE, CoordinatorAgent.COORDINATION_PROTOCOL);
        final MessageTemplate coordSubsTpl = createMessageTemplate(null, SUBSCRIBE_RESPONSE_PERFORMATIVES,
                                                                   CoordinatorAgent.COORDINATION_PROTOCOL);
        final Subscriber coordSubscriber = new Subscriber(this, coordSubsMsg, coordSubsTpl, coordinatorAIDKey);
        coordSubscriber.setDataStore(ds);
        final MessageTemplate cfpTpl = createMessageTemplate(null, CoordinatorAgent.COORDINATION_PROTOCOL,
                                                             ACLMessage.CFP);
        final HandleCFP handleCFP = new HandleCFP(this, cfpTpl, position);
        final MessageTemplate acceptProposalTpl = createMessageTemplate(null, CoordinatorAgent.COORDINATION_PROTOCOL,
                                                                        ACLMessage.ACCEPT_PROPOSAL);
        final HandleAcceptProposal handleAcceptProposal = new HandleAcceptProposal(this, acceptProposalTpl);
        final MessageTemplate rejectProposalTpl = createMessageTemplate(null, CoordinatorAgent.COORDINATION_PROTOCOL,
                                                                        ACLMessage.REJECT_PROPOSAL);
        final HandleRejectProposal rejectAcceptProposal = new HandleRejectProposal(this, rejectProposalTpl);
        final MessageTemplate vehicleStatusTpl = createMessageTemplate(null, VehicleAgent.VEHICLE_STATUS_PROTOCOL,
                                                                       ACLMessage.INFORM);
        final HandleVehicleStatus handleVehicleStatus = new HandleVehicleStatus(this, vehicleStatusTpl);
        final MessageTemplate fireStatusTpl = createMessageTemplate(null, FireAgent.FIRE_STATUS_PROTOCOL,
                                                                    ACLMessage.INFORM);
        final HandleFireStatus handleFireStatus = new HandleFireStatus(this, fireStatusTpl);
        
        // add behaviors
        sequentialBehaviours.add(findCoordinator);
        parallelBehaviours.addAll(Arrays.asList(coordSubscriber, handleCFP, handleAcceptProposal, rejectAcceptProposal,
                                                handleVehicleStatus, handleFireStatus));
        addBehaviours();
    }
    
    private class HandleCFP extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        private final Coordinate position;
        
        /**
         * @param a
         * @param mt
         * @param position
         */
        public HandleCFP(final Agent a, final MessageTemplate mt, final Coordinate position) {

            super(a);
            this.mt = mt;
            this.position = position;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage cfp = blockingReceive(mt);
            if (cfp == null) return;
            
            Coordinate fireCoord;
            try {
                fireCoord = extractMessageContent(HandleFireCFP.class, cfp, false).getCoordinate();
            } catch (final Exception e) {
                sendReply(cfp, ACLMessage.FAILURE, "cannot extract message content");
                return;
            }
            logger.debug("received CFP for fire at (" + fireCoord + ")");
            
            // create proposal
            final HandleFireProposal proposal = new HandleFireProposal(fireCoord,
                                                                       SimulationArea.getDistance(position, fireCoord),
                                                                       vehicles.size() - fires.size()); // need at least one vehicle per fire
            
            sendReply(cfp, ACLMessage.PROPOSE, proposal);
            logger.debug("sent proposal for fire at (" + fireCoord + ")");
        }
    }
    
    private class HandleAcceptProposal extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public HandleAcceptProposal(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage accept = blockingReceive(mt);
            if (accept == null) return;
            
            final Coordinate fireCoord;
            try {
                fireCoord = extractMessageContent(HandleFireProposalResponse.class, accept, false).getCoordinate();
            } catch (final Exception e) {
                sendReply(accept, ACLMessage.FAILURE, "cannot extract message content");
                return;
            }
            
            logger.info("proposal for fire at (" + fireCoord + ") got accepted");
            
            // save fire
            fires.put(fireCoord.toString(), null);
            
            distributeVehicles();
        }
    }
    
    private class HandleRejectProposal extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public HandleRejectProposal(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage reject = blockingReceive(mt);
            if (reject == null) return;
            
//            logger.info("proposal got rejected");
        }
    }
    
    /**
     * (Re-)distributes the vehicles among the fires. Package scoped for faster access by inner classes.
     */
    void distributeVehicles() {

        if (fires.isEmpty()) return;
        
        // calculate vehicles distribution
        final Map<String, Integer> fireWeights = new HashMap<String, Integer>();
        int sumWeights = 0;
        for (final Entry<String, FireStatus> fire : fires.entrySet()) {
            if (!fireVehicles.containsKey(fire.getKey())) {
                fireVehicles.put(fire.getKey(), 1);
            }
            final int weight = (fire.getValue() == null) ? 1 : getFireWeight(fire.getKey());
            fireWeights.put(fire.getKey(), weight);
            sumWeights += weight;
        }
        // TODO check that exact number of vehicles assigned
        final float addVehiclesPerWeight = (vehicles.size() - fires.size()) / sumWeights;
        boolean fireVehiclesChanged = false;
        for (final Entry<String, Integer> fireWeight : fireWeights.entrySet()) {
            final int numVehicles = 1 + Math.round(fireWeight.getValue() * addVehiclesPerWeight);
            if (fireVehicles.put(fireWeight.getKey(), numVehicles) == numVehicles) {
                continue;
            }
            fireVehiclesChanged = true;
        }
        if (!fireVehiclesChanged) return;
        
        // check how many vehicles already correctly assigned
        // TODO consider current vehicle state (at target -> leave assignment)
        final Map<String, Integer> fireVehiclesToAssign = new HashMap<String, Integer>();
        final Set<AID> okVehicles = new HashSet<AID>();
        for (final Entry<String, Integer> fv : fireVehicles.entrySet()) {
            fireVehiclesToAssign.put(fv.getKey(), fv.getValue());
            for (final Entry<AID, VehicleStatus> vehicle : vehicles.entrySet()) {
                if (vehicle.getValue().getFire() == null || !vehicle.getValue().getFire().equals(fv.getKey())) {
                    // vehicle not assigned to this fire
                    continue;
                }
                okVehicles.add(vehicle.getKey());
                if (fireVehiclesToAssign.put(fv.getKey(), fv.getValue() - 1) == 1) {
                    // all vehicles for this fire
                    break;
                }
            }
        }
        
        // (re-)distribute other vehicles
        // TODO consider current vehicle position (prefer already close)
        for (final Entry<String, Integer> fv : fireVehiclesToAssign.entrySet()) {
            if (fv.getValue() == 0) {
                // no more vehicles to assign
                continue;
            }
            for (final AID vehicleAID : vehicles.keySet()) {
                if (okVehicles.contains(vehicleAID)) {
                    continue;
                }
                sendMessage(ACLMessage.REQUEST, VehicleAgent.SET_TARGET_PROTOCOL, vehicleAID,
                            new SetTargetRequest(Coordinate.fromString(fv.getKey())));
                if (fv.setValue(fv.getValue() - 1) == 1) {
                    // all vehicles for this fire
                    break;
                }
            }
        }
    }
    
    /**
     * Returns the relative weight of a fire for the vehicle distribute. Set in concrete subclasses.
     * 
     * @param fireKey
     * @return the relative weight
     */
    protected abstract int getFireWeight(final String fireKey);
    
    /**
     * Receives the status messages from the vehicle agents of this stationary agent.
     */
    private class HandleVehicleStatus extends CyclicBehaviour {
        
        final MessageTemplate mt;
        
        public HandleVehicleStatus(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage statusMsg = blockingReceive(mt);
            if (statusMsg == null) return;
            
            if (!vehicles.containsKey(statusMsg.getSender())) {
                logger.error("received vehicle status from unknown vehicle");
                return;
            }
            
            try {
                vehicles.put(statusMsg.getSender(),
                             extractMessageContent(VehicleStatusInfo.class, statusMsg, false).getVehicleStatus());
            } catch (final Exception e) {
                return;
            }
            
//            logger.debug("received status from vehicle " + statusMsg.getSender());
        }
    }
    
    /**
     * Receives the fire status messages propagated by the vehicle agents.
     */
    private class HandleFireStatus extends CyclicBehaviour {
        
        final MessageTemplate mt;
        
        public HandleFireStatus(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage statusMsg = blockingReceive(mt);
            if (statusMsg == null) return;
            
            if (!vehicles.containsKey(statusMsg.getSender())) {
                logger.error("received fire status from unknown vehicle");
                return;
            }
            
            FireStatus status;
            try {
                status = extractMessageContent(FireStatusInfo.class, statusMsg, false).getFireStatus();
            } catch (final Exception e) {
                return;
            }
            logger.debug("received status for fire at (" + status.getCoordinate() + ")");
            
            if (status == fires.put(status.getCoordinate().toString(), status)) return;
            
            distributeVehicles();
        }
    }
}
