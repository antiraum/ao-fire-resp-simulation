package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.Vehicle;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.ontology.SetTargetRequest;
import it.unitn.disi.aose.firerespsim.ontology.VehicleStatusInfo;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * This is the super class for the fire engine and ambulance agents. Start-up parameters are id, owner (GUID name of the
 * owning stationary agent), row, column, and the movement interval.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public abstract class VehicleAgent extends ExtendedAgent {
    
    /**
     * Protocol for set target messages.
     */
    final static String SET_TARGET_PROTOCOL = "SetVehicleTarget";
    /**
     * Protocol for vehicle status messages.
     */
    final static String VEHICLE_STATUS_PROTOCOL = "VehicleStatus";
    
    /**
     * AID of the stationary agent owning this vehicle. Package scoped for faster access by inner classes.
     */
    AID owner;
    /**
     * Model of the vehicle. Package scoped for faster access by inner classes.
     */
    Vehicle vehicle;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        params = new LinkedHashMap<String, Object>() {
            
            {
                put("OWNER", null);
                put("ROW", null);
                put("COLUMN", null);
                put("MOVE_IVAL", 10000);
            }
        };
        
        super.setup();
        
        final Position vehiclePosition = new Position((Integer) params.get("ROW"), (Integer) params.get("COLUMN"));
        vehicle = new Vehicle(vehiclePosition, Vehicle.STATE_IDLE);
        owner = new AID((String) params.get("OWNER"), true);
        
        // send status to owner
        sendStatus();
        
        // create behaviors
        final MessageTemplate setTargetReqTpl = createMessageTemplate(owner, SET_TARGET_PROTOCOL, ACLMessage.REQUEST);
        final SetTargetService setTargetService = new SetTargetService(this, setTargetReqTpl);
        final Move move = new Move(this, (Integer) params.get("MOVE_IVAL"));
        final MessageTemplate fireStatusTpl = createMessageTemplate(null, FireAgent.FIRE_STATUS_PROTOCOL,
                                                                    ACLMessage.INFORM);
        final ReceiveFireStatus receiveFireStatus = new ReceiveFireStatus(this, fireStatusTpl);
        
        // add behaviors
        parallelBehaviours.addAll(Arrays.asList(setTargetService, move, receiveFireStatus));
        addBehaviours();
    }
    
    /**
     * Service to send the vehicle to a target.
     */
    private class SetTargetService extends AchieveREResponder {
        
        /**
         * @param a
         * @param mt
         */
        public SetTargetService(final Agent a, final MessageTemplate mt) {

            super(a, mt);
        }
        
        @Override
        protected ACLMessage handleRequest(final ACLMessage request) throws NotUnderstoodException, RefuseException {

            if (!vehicle.isAcceptingTarget()) {
                final String refuse = "currently not accepting set target requests";
                logger.debug(refuse);
                throw new RefuseException(refuse);
            }
            
            Coordinate target;
            try {
                target = extractMessageContent(SetTargetRequest.class, request, false).getTarget();
            } catch (final Exception e) {
                throw new NotUnderstoodException("could not read message content");
            }
            
            logger.debug("received set target request to (" + target + ")");
            
            vehicle.fire = new Position(target);
            setTarget(new Position(target));
            
            return null; // TODO return AGREE
        }
        
        /**
         * @see jade.proto.AchieveREResponder#prepareResultNotification(jade.lang.acl.ACLMessage,
         *      jade.lang.acl.ACLMessage)
         */
        @Override
        protected ACLMessage prepareResultNotification(final ACLMessage request, final ACLMessage response)
                throws FailureException {

            return null;
        }
    }
    
    /**
     * Package scoped for faster access by inner classes.
     * 
     * @param target
     */
    void setTarget(final Position target) {

        if (vehicle.target == null) {
            vehicle.target = target.clone();
        } else {
            vehicle.target.set(target);
        }
        vehicle.setState(Vehicle.STATE_TO_TARGET);
        logger.debug("target set to (" + vehicle.target + ")");
        if (vehicle.position.equals(vehicle.target)) {
            arrivedAtTarget();
        }
        sendStatus();
    }
    
    /**
     * Sets the vehicle to idle.
     */
    protected void setIdle() {

        vehicle.setState(Vehicle.STATE_IDLE);
        vehicle.target = null;
        logger.info("is now idle");
    }
    
    /**
     * Sends the current status to the owning stationary agent. Call this every time {@link #vehicle} is changed.
     * Package scoped for faster access by inner classes.
     */
    void sendStatus() {

        sendMessage(ACLMessage.INFORM, VEHICLE_STATUS_PROTOCOL, owner,
                    new VehicleStatusInfo(vehicle.getVehicleStatus()));
//        logger.debug("sent vehicle status to owner");
    }
    
    /**
     * Moves towards the set target position, if it is different from the current position.
     */
    private class Move extends TickerBehaviour {
        
        /**
         * @param a
         * @param period
         */
        public Move(final Agent a, final long period) {

            super(a, period);
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            if (vehicle.getState() != Vehicle.STATE_TO_TARGET) return;
            final Position oldPosition = vehicle.position.clone();
            String oldPositionStr = "(" + oldPosition.toString() + ")";
            if (oldPosition.equals(vehicle.home)) {
                oldPositionStr = "home";
            } else if (vehicle.fire != null && oldPosition.equals(vehicle.fire)) {
                oldPositionStr = "fire";
            }
            if (vehicle.position.getRow() > vehicle.target.getRow()) {
                vehicle.position.decreaseRow(1);
            } else if (vehicle.position.getRow() < vehicle.target.getRow()) {
                vehicle.position.increaseRow(1);
            }
            if (vehicle.position.getCol() > vehicle.target.getCol()) {
                vehicle.position.decreaseCol(1);
            } else if (vehicle.position.getCol() < vehicle.target.getCol()) {
                vehicle.position.increaseCol(1);
            }
            final Position newPosition = vehicle.position.clone();
            String newPositionStr = "(" + newPosition.toString() + ")";
            if (newPosition.equals(vehicle.home)) {
                newPositionStr = "home";
            } else if (vehicle.fire != null && newPosition.equals(vehicle.fire)) {
                newPositionStr = "fire";
            }
            logger.debug("moved from " + oldPositionStr + " to (" + newPositionStr + ")");
            if (vehicle.position.equals(vehicle.target)) {
                arrivedAtTarget();
            }
            sendStatus();
            doMove();
        }
    }
    
    /**
     * Gets called when arrived at the target position. Package scoped for faster access by inner classes.
     */
    void arrivedAtTarget() {

        vehicle.setState(Vehicle.STATE_AT_TARGET);
        if (vehicle.fire != null && vehicle.position.equals(vehicle.fire)) {
            logger.info("arrived at fire");
            arrivedAtFire();
        } else {
            logger.info("arrived at home");
            arrivedAtHome();
        }
    }
    
    /**
     * Gets called when arrived at home. Package scoped for faster access by inner classes.
     */
    abstract void arrivedAtHome();
    
    /**
     * Gets called when arrived at fire. Package scoped for faster access by inner classes.
     */
    abstract void arrivedAtFire();
    
    /**
     * Gets called every move interval. Package scoped for faster access by inner classes.
     */
    abstract void doMove();
    
    /**
     * Receives the status messages from fire agents.
     */
    private class ReceiveFireStatus extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
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
            
            FireStatus status;
            try {
                status = extractMessageContent(FireStatus.class, statusMsg, false);
            } catch (final Exception e) {
                return;
            }
            logger.info("received status from fire at (" + status.getCoordinate() + ")");
            
            // propagate to stationary agent
            statusMsg.removeReceiver(myAgent.getAID());
            statusMsg.addReceiver(owner);
            send(statusMsg);
            logger.debug("propagated fire status to owner");
            
            receivedFireStatus(status);
        }
    }
    
    /**
     * Gets called when a fire status is received.
     * 
     * @param fire
     */
    abstract void receivedFireStatus(final FireStatus fire);
    
    /**
     * @return AID of the fire currently assigned to.
     */
    protected AID getFireAID() {

        return (vehicle.fire == null) ? null : new AID(FireAgent.FIRE_AGENT_NAME_PREFIX + vehicle.fire, false);
    }
}
