package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.FireResponseOntology;
import it.unitn.disi.aose.firerespsim.model.Fire;
import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.SimulationArea;
import it.unitn.disi.aose.firerespsim.model.Vehicle;
import it.unitn.disi.aose.firerespsim.ontology.CFP;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.Proposal;
import it.unitn.disi.aose.firerespsim.ontology.SetTargetRequest;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

/**
 * This is the super class for the fire brigade and hospital agents. Handles the communication with the
 * {@link CoordinatorAgent} and the owned {@link VehicleAgent}s and assigns the vehicle agents to the different fires it
 * is responsible for. Concrete subclasses can customize the behavior by implementing the abstract methods. Start-up
 * parameters are an id, the row, column, and the vehicle move interval.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public abstract class StationaryAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Package scoped for faster access by inner classes.
     */
    Agent thisAgent = this;
    
    /**
     * Class of the vehicle agents this agent owns. Must be set in concrete subclasses.
     */
    protected String vehicleAgentClass;
    /**
     * Nickname of the vehicle agents this agent owns. Must be set in concrete subclasses.
     */
    protected String vehicleName;
    /**
     * DF type of the coordinator to use. Must be set in the concrete subclasses.
     */
    protected String coordinatorDfType;
    
    /**
     * Defaults for start-up arguments.
     */
    private static final int DEFAULT_VEHICLE_MOVE_IVAL = 10000;
    
    /**
     * Position on the simulation area. Package scoped for faster access by inner classes.
     */
    Position position;
    
    /**
     * Vehicle agents. Package scoped for faster access by inner classes.
     */
    final Map<Integer, AgentController> vehicleAgents = new HashMap<Integer, AgentController>();
    /**
     * Vehicle statuses. Package scoped for faster access by inner classes.
     */
    final Map<Integer, Vehicle> vehicles = new HashMap<Integer, Vehicle>();
    
    /**
     * Codec for message content encoding. Package scoped for faster access by inner classes.
     */
    final Codec codec = new SLCodec();
    /**
     * Simulation ontology. Package scoped for faster access by inner classes.
     */
    final Ontology onto = FireResponseOntology.getInstance();
    
    /**
     * Fires responsible for. Package scoped for faster access by inner classes.
     */
    final Map<Coordinate, Fire> fires = new HashMap<Coordinate, Fire>();
    /**
     * Vehicle assignments to the {@link #fires}. Package scoped for faster access by inner classes.
     */
    final Map<Coordinate, Set<Integer>> fireAssignments = new HashMap<Coordinate, Set<Integer>>();
    
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
        final Object[] params = getArguments();
        if (params == null || params.length < 3) {
            logger.error("start-up arguments id, row, and column needed");
            doDelete();
            return;
        }
        final String id = (String) params[0];
        position = new Position((Integer) params[1], (Integer) params[2]);
        final int vehicleMoveIval = (params.length > 3) ? (Integer) params[3] : DEFAULT_VEHICLE_MOVE_IVAL;
        
        // create vehicle agents
        final int numVehicles = RandomUtils.nextInt(4) + 1; // between 1 and 5
        for (int i = 0; i < numVehicles; i++) {
            
            final String nickname = vehicleName + " " + id + "-" + i;
            try {
                final AgentController vehicleAC = getContainerController().createNewAgent(
                                                                                          nickname,
                                                                                          vehicleAgentClass,
                                                                                          new Object[] {
                                                                                              i, getName(),
                                                                                              position.getRow(),
                                                                                              position.getCol(),
                                                                                              vehicleMoveIval});
                vehicleAC.start();
                vehicleAgents.put(i, vehicleAC);
                logger.debug("created '" + nickname + "'");
            } catch (final StaleProxyException e) {
                logger.error("couldn't create '" + nickname + "'");
                e.printStackTrace();
            }
            
            vehicles.put(i, null);
        }
        logger.info("created " + numVehicles + " vehicles");
        
        // add behaviors
        final SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new RegisterAtCoordinator());
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new ReceiveCFP(), new ReceiveProposalReply(), new DistributeVehicles(this), new ReceiveVehicleStatus()}));
        final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        sb.addSubBehaviour(pb);
        addBehaviour(sb);
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
     * Template for messages from the coordinator agent. Package scoped for faster access by inner classes.
     */
    MessageTemplate coordinatorTpl = null;
    
    /**
     * Register at {@link CoordinatorAgent}.
     */
    class RegisterAtCoordinator extends SimpleBehaviour {
        
        private boolean done = false;
        private final DFAgentDescription coordinatorAD = new DFAgentDescription();
        private AID coordinatorAID = null;
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.or(
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE)),
                                                                     MessageTemplate.MatchOntology(onto.getName()));
        
        /**
         * Constructor
         */
        public RegisterAtCoordinator() {

            super();
            
            final ServiceDescription coordinatorSD = new ServiceDescription();
            coordinatorSD.setType(coordinatorDfType);
            coordinatorAD.addServices(coordinatorSD);
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            if (coordinatorAID == null) {
                DFAgentDescription[] result = null;
                try {
                    result = DFService.search(thisAgent, coordinatorAD);
                } catch (final FIPAException e) {
                    logger.error("error searching for agent with " + coordinatorDfType + " service at DF");
                    e.printStackTrace();
                    return;
                }
                if (result != null && result.length > 0) {
                    coordinatorAID = result[0].getName();
                    coordinatorTpl = MessageTemplate.MatchSender(coordinatorAID);
                }
            }
            if (coordinatorAID == null) {
                logger.error("no coordinator AID");
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
//                    done = true;
                }
                return;
            }
            
            final ACLMessage subscribeMsg = new ACLMessage(ACLMessage.SUBSCRIBE);
            subscribeMsg.addReceiver(coordinatorAID);
            subscribeMsg.setOntology(onto.getName());
            send(subscribeMsg);
//            logger.debug("sent coordinator registration request");
            
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            if (replyMsg.getPerformative() == ACLMessage.AGREE) {
                logger.info("registered at coordinator");
            } else {
                logger.error("coordinator registration disconfirmed - assume this is because already registered");
            }
            
            done = true;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#done()
         */
        @Override
        public boolean done() {

            return done;
        }
        
    }
    
    /**
     * Fire responsibility proposals sent
     */
    final Map<String, Coordinate> sentProposals = new HashMap<String, Coordinate>();
    
    /**
     * Receive calls for proposal from the coordinator agent. Sends out the a proposal.
     */
    class ReceiveCFP extends CyclicBehaviour {
        
        private final MessageTemplate cfpTpl = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                                                                   MessageTemplate.MatchOntology(onto.getName()));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage cfpMsg = blockingReceive(cfpTpl);
            if (!coordinatorTpl.match(cfpMsg)) return;
            if (cfpMsg == null) return;
            
            ContentElement ce;
            try {
                ce = getContentManager().extractContent(cfpMsg);
            } catch (final Exception e) {
                logger.error("error extracting message content");
                return;
            }
            if (!(ce instanceof CFP)) {
                logger.error("cfp message has wrong content");
                return;
            }
            final CFP cfp = (CFP) ce;
            final Coordinate fireCoord = cfp.getCoordinate();
            logger.debug("received CFP for fire at (" + fireCoord + ")");
            
            // create proposal
            final Proposal proposal = new Proposal(SimulationArea.getDistance(position, fireCoord), vehicles.size() -
                                                                                                    fires.size()); // need at least one vehicle per fire
            
            // send proposal
            final ACLMessage proposalMsg = cfpMsg.createReply();
            proposalMsg.setPerformative(ACLMessage.PROPOSE);
            try {
                getContentManager().fillContent(proposalMsg, proposal);
                send(proposalMsg);
                logger.info("sent proposal (" + proposal + ")");
                sentProposals.put(proposalMsg.getConversationId(), fireCoord);
            } catch (final Exception e) {
                logger.error("error filling message content");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Receives proposal replies from the coordinator agent.
     */
    class ReceiveProposalReply extends CyclicBehaviour {
        
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.or(
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)),
                                                                     MessageTemplate.MatchOntology(onto.getName()));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage replyMsg = blockingReceive(replyTpl);
            if (!coordinatorTpl.match(replyMsg)) return;
            if (replyMsg == null) return;
            
            if (!sentProposals.containsKey(replyMsg.getConversationId())) {
                logger.debug("received reply for unknown proposal");
                return;
            }
            
            final Coordinate fireCoord = sentProposals.get(replyMsg.getConversationId());
            sentProposals.remove(replyMsg.getConversationId());
            
            if (replyMsg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                logger.info("proposal for fire at (" + fireCoord + ") got rejected");
                return;
            }
            
            logger.info("proposal for fire at (" + fireCoord + ") got accepted");
            
            // insert into maps
            fires.put(fireCoord, null);
            fireAssignments.put(fireCoord, new HashSet<Integer>());
            
            // send all idle vehicles to the new fire
            final ACLMessage sendToMsg = new ACLMessage(ACLMessage.REQUEST);
            sendToMsg.setOntology(onto.getName());
            sendToMsg.setLanguage(codec.getName());
            for (final Map.Entry<Integer, Vehicle> vehicleEntry : vehicles.entrySet()) {
                if (vehicleEntry.getValue().getState() == Vehicle.STATE_IDLE) {
                    try {
                        sendToMsg.addReceiver(new AID(vehicleAgents.get(vehicleEntry.getKey()).getName(), true));
                    } catch (final StaleProxyException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                getContentManager().fillContent(sendToMsg, new SetTargetRequest(fireCoord));
                send(sendToMsg);
                logger.info("sent all idle vehicles to the new fire");
            } catch (final Exception e) {
                logger.error("error filling message content");
            }
        }
    }
    
    /**
     * Assigns the {@link #vehicleAgents} to the {@link #fires}. For each fire one vehicle is assigned. Additional
     * vehicles are distributed along the fires based on the fire intensities.
     */
    class DistributeVehicles extends TickerBehaviour {
        
        /**
         * @param a
         */
        public DistributeVehicles(final Agent a) {

            super(a, 1000);
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            final Map<Coordinate, Integer> vehiclesPerFire = new HashMap<Coordinate, Integer>();
            final Map<Coordinate, Integer> fireWeights = new HashMap<Coordinate, Integer>();
            int sumWeights = 0;
            for (final Map.Entry<Coordinate, Fire> fire : fires.entrySet()) {
                vehiclesPerFire.put(fire.getKey(), 1);
                final int weight = (fire.getValue() == null) ? 1 : fire.getValue().getIntensity();
                fireWeights.put(fire.getKey(), weight);
                sumWeights += weight;
            }
            final float addVehiclesPerWeight = (vehicles.size() - fires.size()) / sumWeights;
            for (final Map.Entry<Coordinate, Fire> fire : fires.entrySet()) {
                vehiclesPerFire.put(fire.getKey(), vehiclesPerFire.get(fire.getKey()) +
                                                   Math.round(fire.getValue().getIntensity() * addVehiclesPerWeight));
            }
            
            for (final Fire fire : fires.values()) {
                if (fire == null) {
                    // no info about fire yet
                } else {
                    if (fire.getCasualties() == 0 && fire.getIntensity() == 0) {
                        // TODO
                    }
                }
            }
        }
    }
    
    /**
     * Receives the status messages from the vehicle agents of this stationary agent.
     */
    class ReceiveVehicleStatus extends CyclicBehaviour {
        
        private final MessageTemplate statusTpl = MessageTemplate.and(
                                                                      MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                      MessageTemplate.MatchOntology(VehicleAgent.VEHICLE_STATUS_ONT_TYPE));
        
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
            final Vehicle vehicleStatus = Vehicle.fromString(statusMsg.getContent());
            logger.debug("received status from vehicle " + vehicleStatus.id);
            vehicles.put(vehicleStatus.id, vehicleStatus);
            
            // update assignments
            for (final Map.Entry<Coordinate, Set<Integer>> fireAssignment : fireAssignments.entrySet()) {
                if (vehicleStatus.target != null && fireAssignment.getKey() == vehicleStatus.target.getCoordinate()) {
                    fireAssignment.getValue().add(vehicleStatus.id);
                } else {
                    fireAssignment.getValue().remove(vehicleStatus.id);
                }
            }
        }
    }
    
    /**
     * Receives the fire status messages propagated by the vehicle agents.
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
            final Fire fireStatus = Fire.fromString(statusMsg.getContent());
            logger.info("received status for fire at (" + fireStatus.position + ")");
            
            fires.put(fireStatus.position.getCoordinate(), fireStatus);
        }
    }
}
