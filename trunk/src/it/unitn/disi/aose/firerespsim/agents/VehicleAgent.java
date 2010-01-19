package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Fire;
import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.Vehicle;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * This is the super class for the fire engine and ambulance agents. Start-up parameters are id, owner (GUID name of the
 * owning stationary agent), row, column, and the movement interval.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public abstract class VehicleAgent extends Agent {
    
    /**
     * Ontology type of vehicle status messages. Package scoped for faster access by inner classes.
     */
    final static String VEHICLE_STATUS_ONT_TYPE = "VehicleStatus";
    /**
     * Ontology type of vehicle target messages. Package scoped for faster access by inner classes.
     */
    final static String VEHICLE_TARGET_ONT_TYPE = "VehicleTarget";
    
    /**
     * Defaults for start-up arguments.
     */
    private static final int DEFAULT_MOVE_IVAL = 10000;
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Package scoped for faster access by inner classes.
     */
    Agent thisAgent = this;
    
    /**
     * AID of the stationary agent owning this vehicle. Package scoped for faster access by inner classes.
     */
    AID owner;
    /**
     * Current status of the vehicle of the agent. Package scoped for faster access by inner classes.
     */
    Vehicle vehicle;
    
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
        final Object[] params = getArguments();
        if (params == null || params.length < 4) {
            logger.error("start-up arguments id, owner, row, and column needed");
            doDelete();
            return;
        }
        vehicle = new Vehicle((Integer) params[0], new Position((Integer) params[2], (Integer) params[3]),
                              Vehicle.STATE_IDLE);
        owner = new AID((String) params[1], true);
        final int vehicleMoveIval = (params.length > 4) ? (Integer) params[4] : DEFAULT_MOVE_IVAL;
        
        // status to owner
        sendStatus();
        
        // add behaviors
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new SetTargetService(), new Move(this, vehicleMoveIval), new ReceiveFireStatus()}));
        final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        addBehaviour(pb);
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
     * Service to send the vehicle to a target. Content of the request message must be {@link Position#toString()} of
     * the target position.
     */
    class SetTargetService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology(VEHICLE_TARGET_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            if (!vehicle.isAcceptingTarget()) {
                logger.debug("currently not accepting set-target requests");
                return;
            }
            
            // get target position
            if (requestMsg.getContent() == null) {
                logger.error("request message has no content");
                return;
            }
            final Position newTarget = Position.fromString(requestMsg.getContent());
            
            logger.debug("received set-target request with new target (" + newTarget + ")");
            
            setTarget(newTarget);
        }
    }
    
    /**
     * Package scoped for faster access by inner classes.
     * 
     * @param target
     */
    void setTarget(final Position target) {

        vehicle.target.set(target);
        vehicle.setState(Vehicle.STATE_TO_TARGET);
        logger.debug("target set to (" + vehicle.target + ")");
        if (vehicle.position.equals(vehicle.target)) {
            arrivedAtTarget();
        }
        sendStatus();
    }
    
    /**
     * Sends the current status to the owning stationary agent. Call this every time {@link #vehicle} is changed.
     * Package scoped for faster access by inner classes.
     */
    void sendStatus() {

        final ACLMessage statusMsg = new ACLMessage(ACLMessage.INFORM);
        statusMsg.addReceiver(owner);
        statusMsg.setOntology(VEHICLE_STATUS_ONT_TYPE);
        statusMsg.setContent(vehicle.toString());
        send(statusMsg);
        logger.debug("sent status to owner");
    }
    
    /**
     * Moves towards the set target position, if it is different from the current position.
     */
    class Move extends TickerBehaviour {
        
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
            if (vehicle.position.equals(vehicle.target)) {
                arrivedAtTarget();
            } else {
                logger.debug("moved to (" + vehicle.position.getRow() + ", " + vehicle.position.getCol() + ")");
            }
            sendStatus();
        }
    }
    
    /**
     * Gets called when arrived at the target position. Package scoped for faster access by inner classes.
     */
    void arrivedAtTarget() {

        vehicle.setState(Vehicle.STATE_AT_TARGET);
        if (vehicle.position.equals(vehicle.home)) {
            logger.info("arrived at home");
            arrivedAtHome();
        } else {
            logger.info("arrived at fire");
            arrivedAtFire();
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
     * Receives the status messages from fire agents.
     */
    class ReceiveFireStatus extends CyclicBehaviour {
        
        private final MessageTemplate statusTpl = MessageTemplate.and(
                                                                      MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                      MessageTemplate.MatchOntology(FireAgent.FIRE_STATUS_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage statusMsg = blockingReceive(statusTpl);
            if (statusMsg == null) return;
            
            if (statusMsg.getContent() == null) {
                logger.error("status message has no content");
                return;
            }
            final Fire status = Fire.fromString(statusMsg.getContent());
            logger.info("received status from fire at (" + status.position + ")");
            
            // propagate to stationary agent
            statusMsg.removeReceiver(thisAgent.getAID());
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
    abstract void receivedFireStatus(final Fire fire);
}
