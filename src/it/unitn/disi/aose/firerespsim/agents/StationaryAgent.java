package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.behaviours.FindAgent;
import it.unitn.disi.aose.firerespsim.behaviours.Subscriber;
import it.unitn.disi.aose.firerespsim.model.SimulationArea;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.ontology.FireStatusInfo;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireCFP;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireProposal;
import it.unitn.disi.aose.firerespsim.ontology.SetTargetRequest;
import it.unitn.disi.aose.firerespsim.ontology.VehicleStatus;
import it.unitn.disi.aose.firerespsim.ontology.VehicleStatusInfo;
import it.unitn.disi.aose.firerespsim.util.AgentUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.DataStore;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetResponder;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import org.apache.commons.lang.math.RandomUtils;

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
    final Map<Coordinate, FireStatus> fires = new HashMap<Coordinate, FireStatus>();
    /**
     * Fire vehicle distribution. Package scoped for faster access by inner classes.
     */
    final Map<Coordinate, Integer> fireVehicles = new HashMap<Coordinate, Integer>();
    
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
        final int numVehicles = RandomUtils.nextInt(4) + 1; // between 1 and 5
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
        final Subscriber coordSubscriber = new Subscriber(this, coordSubsMsg, ds, coordinatorAIDKey);
        final MessageTemplate cfpTpl = createMessageTemplate(null, CoordinatorAgent.COORDINATION_PROTOCOL,
                                                             ACLMessage.CFP);
        final ReceiveCFP receiveCFP = new ReceiveCFP(this, cfpTpl, position);
        final MessageTemplate vehicleStatusTpl = createMessageTemplate(null, VehicleAgent.VEHICLE_STATUS_PROTOCOL,
                                                                       ACLMessage.INFORM);
        final ReceiveVehicleStatus receiveVehicleStatus = new ReceiveVehicleStatus(this, vehicleStatusTpl);
        final MessageTemplate fireStatusTpl = createMessageTemplate(null, FireAgent.FIRE_STATUS_PROTOCOL,
                                                                    ACLMessage.INFORM);
        final ReceiveFireStatus receiveFireStatus = new ReceiveFireStatus(this, fireStatusTpl);
        
        // add behaviors
        sequentialBehaviours.add(findCoordinator);
        parallelBehaviours.addAll(Arrays.asList(coordSubscriber, receiveCFP, receiveVehicleStatus, receiveFireStatus));
        addBehaviours();
    }
    
    /**
     * Receive calls for proposal from the coordinator agent. Sends out the a proposal.
     */
    private class ReceiveCFP extends ContractNetResponder {
        
        private final Coordinate position;
        
        /**
         * @param a
         * @param mt
         * @param position
         */
        public ReceiveCFP(final Agent a, final MessageTemplate mt, final Coordinate position) {

            super(a, mt);
            this.position = position;
        }
        
        /**
         * @see jade.proto.ContractNetResponder#handleCfp(jade.lang.acl.ACLMessage)
         */
        @Override
        protected ACLMessage handleCfp(final ACLMessage cfp) throws RefuseException, FailureException,
                NotUnderstoodException {

            final Coordinate fireCoord = getFireCoordinate(cfp);
            logger.debug("received CFP for fire at (" + fireCoord + ")");
            
            // create proposal
            final HandleFireProposal proposal = new HandleFireProposal(SimulationArea.getDistance(position, fireCoord),
                                                                       vehicles.size() - fires.size()); // need at least one vehicle per fire
            
            return createReply(cfp, ACLMessage.PROPOSE, proposal);
        }
        
        /**
         * @see jade.proto.ContractNetResponder#handleAcceptProposal(jade.lang.acl.ACLMessage, jade.lang.acl.ACLMessage,
         *      jade.lang.acl.ACLMessage)
         */
        @Override
        protected ACLMessage handleAcceptProposal(final ACLMessage cfp, final ACLMessage propose,
                                                  final ACLMessage accept) throws FailureException {

            final Coordinate fireCoord = getFireCoordinate(cfp);
            
            logger.info("proposal for fire at (" + fireCoord + ") got accepted");
            
            // save fire
            fires.put(fireCoord, null);
            
            // (re-)distribute vehicles
            if (distributeVehicles == null) {
                distributeVehicles = new DistributeVehicles(myAgent);
            } else {
                distributeVehicles.reset();
            }
            addParallelBehaviour(distributeVehicles);
            
            return null;
        }
        
        /**
         * @see jade.proto.SSContractNetResponder#handleRejectProposal(jade.lang.acl.ACLMessage,
         *      jade.lang.acl.ACLMessage, jade.lang.acl.ACLMessage)
         */
        @Override
        protected void handleRejectProposal(final ACLMessage cfp, final ACLMessage propose, final ACLMessage reject) {

            logger.info("proposal got rejected");
        }
        
        /**
         * @param cfp
         * @return Coordinate of the fire.
         * @throws FailureException
         */
        private Coordinate getFireCoordinate(final ACLMessage cfp) throws FailureException {

            try {
                return extractMessageContent(HandleFireCFP.class, cfp, false).getCoordinate();
            } catch (final Exception e) {
                throw new FailureException("cannot extract cfp content");
            }
        }
    }
    
    /**
     * Instance of {@link DistributeVehicles} that gets re-used.
     */
    DistributeVehicles distributeVehicles = null;
    
    /**
     * Assigns the {@link #vehicles} to the {@link #fires}. For each fire one vehicle is assigned. Additional vehicles
     * are distributed along the fires based on the fire intensities.
     */
    private class DistributeVehicles extends AchieveREInitiator {
        
        /**
         * @param a
         */
        public DistributeVehicles(final Agent a) {

            super(a, createMessage(ACLMessage.REQUEST, VehicleAgent.SET_TARGET_PROTOCOL));
        }
        
        /**
         * @see jade.proto.AchieveREInitiator#prepareRequests(jade.lang.acl.ACLMessage)
         */
        @SuppressWarnings("unchecked")
        @Override
        protected Vector prepareRequests(final ACLMessage request) {

            if (fires.isEmpty()) return null;
            
            // calculate vehicles distribution
            final Map<Coordinate, Integer> fireWeights = new HashMap<Coordinate, Integer>();
            int sumWeights = 0;
            for (final Entry<Coordinate, FireStatus> fire : fires.entrySet()) {
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
            for (final Entry<Coordinate, Integer> fireWeight : fireWeights.entrySet()) {
                final int numVehicles = 1 + Math.round(fireWeight.getValue() * addVehiclesPerWeight);
                if (fireVehicles.put(fireWeight.getKey(), numVehicles) == numVehicles) {
                    continue;
                }
                fireVehiclesChanged = true;
            }
            if (!fireVehiclesChanged) return null;
            
            // check how many vehicles already correctly assigned
            // TODO consider current vehicle state (at target -> leave assignment)
            final Map<Coordinate, Integer> fireVehiclesToAssign = new HashMap<Coordinate, Integer>();
            final Set<AID> okVehicles = new HashSet<AID>();
            for (final Entry<Coordinate, Integer> fv : fireVehicles.entrySet()) {
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
            
            final Vector<ACLMessage> requests = new Vector<ACLMessage>();
            
            // (re-)distribute other vehicles
            // TODO consider current vehicle position (prefer already close)
            for (final Entry<Coordinate, Integer> fv : fireVehiclesToAssign.entrySet()) {
                if (fv.getValue() == 0) {
                    // no more vehicles to assign
                    continue;
                }
                for (final AID vehicleAID : vehicles.keySet()) {
                    if (okVehicles.contains(vehicleAID)) {
                        continue;
                    }
                    final ACLMessage thisRequest = copyMessage(request);
                    thisRequest.setSender(vehicleAID);
                    fillMessage(thisRequest, new SetTargetRequest(fv.getKey()));
                    requests.add(thisRequest);
                    if (fv.setValue(fv.getValue() - 1) == 1) {
                        // all vehicles for this fire
                        break;
                    }
                }
            }
            
            return requests;
        }
    }
    
    /**
     * Returns the relative weight of a fire for the vehicle distribute. Set in concrete subclasses.
     * 
     * @param fireCoord
     * @return the relative weight
     */
    protected abstract int getFireWeight(final Coordinate fireCoord);
    
    /**
     * Receives the status messages from the vehicle agents of this stationary agent.
     */
    private class ReceiveVehicleStatus extends CyclicBehaviour {
        
        final MessageTemplate mt;
        
        public ReceiveVehicleStatus(final Agent a, final MessageTemplate mt) {

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
    private class ReceiveFireStatus extends CyclicBehaviour {
        
        final MessageTemplate mt;
        
        public ReceiveFireStatus(final Agent a, final MessageTemplate mt) {

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
            fires.put(status.getCoordinate(), status);
            
            logger.debug("received status for fire at (" + status.getCoordinate() + ")");
            
            // (re-)distribute vehicles
            if (distributeVehicles == null) {
                distributeVehicles = new DistributeVehicles(myAgent);
            } else {
                distributeVehicles.reset();
            }
            addParallelBehaviour(distributeVehicles);
        }
    }
}
