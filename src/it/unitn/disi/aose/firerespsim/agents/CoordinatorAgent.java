package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.Proposal;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Coordinator for {@link StationaryAgent}s. Subscribes at the {@link FireMonitorAgent} for fire alerts. Distribute the
 * responsibilities for the fires among the registered stationary agents. No start-up parameters.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public abstract class CoordinatorAgent extends Agent {
    
    /**
     * Ontology type of coordination messages.
     */
    protected static final String COORDINATION_ONT_TYPE = "Coordination";
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Package scoped for faster access by inner classes.
     */
    Agent thisAgent = this;
    
    /**
     * DF Type of this coordinator. Must be set it in the concrete subclasses.
     */
    protected String dfType;
    
    /**
     * Set of the names (GUID) of all stationary agents that are registered to be coordinated by this agent. Package
     * scoped for faster access by inner classes.
     */
    final Set<String> stationaryAgents = new HashSet<String>();
    
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        logger.debug("starting up");
        
        super.setup();
        
        // add behaviors
        final SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new SubscribeToFireAlerts());
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new ReceiveProposals(), new CoordinationService(), new ReceiveProposals(),
            new CheckCoordinationTimeouts(this)}));
        final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        sb.addSubBehaviour(pb);
        addBehaviour(sb);
        
        // register at the DF
        final DFAgentDescription descr = new DFAgentDescription();
        final ServiceDescription sd = new ServiceDescription();
        sd.setName(getName());
        sd.setType(dfType);
        descr.addServices(sd);
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
     * Service for registering to use this agent for fire responsibility coordination.
     */
    class CoordinationService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
                                                                       MessageTemplate.MatchOntology(COORDINATION_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received coordination registration request");
            
            final String agentName = requestMsg.getSender().getName();
            final ACLMessage replyMsg = requestMsg.createReply();
            if (stationaryAgents.contains(agentName)) {
                // already registered
                replyMsg.setPerformative(ACLMessage.DISCONFIRM);
            } else {
                // new agent
                stationaryAgents.add(agentName);
                replyMsg.setPerformative(ACLMessage.CONFIRM);
            }
            send(replyMsg);
            logger.debug("sent coordination registration reply");
        }
    }
    
    /**
     * Subscribes to new fire alerts from the fire monitor agent.
     */
    class SubscribeToFireAlerts extends SimpleBehaviour {
        
        private boolean done = false;
        private final DFAgentDescription fireAlertAD = new DFAgentDescription();
        private AID fireMonitorAID = null;
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.or(
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)),
                                                                     MessageTemplate.MatchOntology(FireMonitorAgent.FIRE_ALERT_ONT_TYPE));
        
        /**
         * Constructor
         */
        public SubscribeToFireAlerts() {

            super();
            
            final ServiceDescription fireAlertSD = new ServiceDescription();
            fireAlertSD.setType(FireMonitorAgent.DF_TYPE);
            fireAlertAD.addServices(fireAlertSD);
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            if (fireMonitorAID == null) {
                DFAgentDescription[] result = null;
                try {
                    result = DFService.search(thisAgent, fireAlertAD);
                } catch (final FIPAException e) {
                    logger.error("error searching for agent with " + FireMonitorAgent.DF_TYPE + " service at DF");
                    e.printStackTrace();
                    return;
                }
                if (result != null && result.length > 0) {
                    fireMonitorAID = result[0].getName();
                } else {
                    logger.debug("no agent with " + FireMonitorAgent.DF_TYPE + " service at DF");
                }
            }
            if (fireMonitorAID == null) {
                logger.error("no fire alert AID");
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
//                    done = true;
                }
                return;
            }
            
            final ACLMessage subscribeMsg = new ACLMessage(ACLMessage.SUBSCRIBE);
            subscribeMsg.addReceiver(fireMonitorAID);
            subscribeMsg.setOntology(FireMonitorAgent.FIRE_ALERT_ONT_TYPE);
            send(subscribeMsg);
            logger.debug("sent fire alert subscribtion request");
            
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            if (replyMsg.getPerformative() == ACLMessage.CONFIRM) {
                logger.info("subscribed for new fire alerts at monitor agent");
            } else {
                logger.error("fire alert subscription was disconfirmed - assume this is because already subscribed");
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
     * Proposals for fires currently coordinated. Package scoped for faster access by inner classes.
     */
    final Map<String, Set<Proposal>> fireProposals = new HashMap<String, Set<Proposal>>();
    /**
     * Start times of CFPs for fires currently coordinated. Package scoped for faster access by inner classes.
     */
    final Map<String, Long> fireCfpStartTimes = new HashMap<String, Long>();
    
    /**
     * Receives fire alerts from the fire monitor agent. If a fire is new, proposals from the stationary agents are
     * requested. The content of the call for proposals message consists is {@link Position#toString()} of the fire
     * position.
     */
    class ReceiveFireAlerts extends CyclicBehaviour {
        
        private final Set<Position> knownFires = new HashSet<Position>();
        
        private final MessageTemplate alertTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology(FireMonitorAgent.FIRE_ALERT_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage alertMsg = blockingReceive(alertTpl);
            if (alertMsg == null) return;
            
            if (alertMsg.getContent() == null) {
                logger.error("alert message has no content");
                return;
            }
            final Position firePosition = Position.fromString(alertMsg.getContent());
            logger.info("received fire alert for position (" + firePosition + ")");
            
            if (knownFires.contains(firePosition)) return; // not new
            knownFires.add(firePosition);
            
            // request proposals from stationary agents
            fireProposals.put(firePosition.toString(), new HashSet<Proposal>());
            fireCfpStartTimes.put(firePosition.toString(), System.currentTimeMillis());
            final ACLMessage cfpMsg = new ACLMessage(ACLMessage.CFP);
            cfpMsg.setOntology(COORDINATION_ONT_TYPE);
            cfpMsg.setContent(firePosition.toString());
            for (final String sa : stationaryAgents) {
                cfpMsg.addReceiver(new AID(sa, true));
            }
            send(cfpMsg);
        }
    }
    
    /**
     * Receives the proposals from the stationary agents. Content of the proposal Messages must be
     * {@link Proposal#toString()}. After all proposals are collected or if the proposal timeout is reached, the
     * responsibility is given to the agent with the best proposal.
     */
    class ReceiveProposals extends CyclicBehaviour {
        
        private final MessageTemplate proposalTpl = MessageTemplate.and(
                                                                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                                                        MessageTemplate.MatchOntology(COORDINATION_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage proposalMsg = blockingReceive(proposalTpl);
            if (proposalMsg == null) return;
            
            // save proposal
            if (proposalMsg.getContent() == null) {
                logger.error("proposal message has no content");
                return;
            }
            final Proposal prop = Proposal.fromString(proposalMsg.getContent());
            if (!fireProposals.containsKey(prop.firePosition.toString())) return; // fire is not currently coordinated
            fireProposals.get(prop.firePosition.toString()).add(prop);
            
            if (fireProposals.get(prop.firePosition).size() == stationaryAgents.size()) {
                // all proposals received
                assignFire(prop.firePosition.toString());
            }
        }
    }
    
    /**
     * Finishes coordination for fires that are waiting too long for proposals.
     */
    class CheckCoordinationTimeouts extends TickerBehaviour {
        
        private static final int WAIT_FOR_PROPOSALS_MS = 50000;
        
        /**
         * @param a
         */
        public CheckCoordinationTimeouts(final Agent a) {

            super(a, 1000);
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            for (final Map.Entry<String, Long> fire : fireCfpStartTimes.entrySet()) {
                if (System.currentTimeMillis() - fire.getValue() > WAIT_FOR_PROPOSALS_MS) {
                    assignFire(fire.getKey());
                }
            }
        }
    }
    
    /**
     * Finish coordination of a fire. Sends a proposal accept message to the agent with the best proposal and proposal
     * reject messages to all other agents. Content of the messages is {@link Position#toString()} of the fire location.
     * Package scoped for faster access by inner classes.
     * 
     * @param fireKey
     */
    void assignFire(final String fireKey) {

        // find best proposal
        Proposal bestProp = null;
        for (final Proposal prop : fireProposals.get(fireKey)) {
            if (bestProp == null || prop.numVehicles > bestProp.numVehicles ||
                (prop.numVehicles == bestProp.numVehicles && prop.distance < bestProp.distance)) {
                bestProp = prop;
            }
        }
        
        if (bestProp == null) {
            
            logger.error("no proposal for fire at " + fireKey + ", will not be handled!");
            
        } else {
            
            // send accept message
            final ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            acceptMsg.setOntology(COORDINATION_ONT_TYPE);
            acceptMsg.setContent(fireKey);
            acceptMsg.addReceiver(new AID(bestProp.agentName, true));
            send(acceptMsg);
            
            // send reject message
            final ACLMessage recjectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            recjectMsg.setOntology(COORDINATION_ONT_TYPE);
            recjectMsg.setContent(fireKey);
            for (final Proposal prop : fireProposals.get(fireKey)) {
                if (prop != bestProp) {
                    recjectMsg.addReceiver(new AID(prop.agentName, true));
                }
            }
            send(recjectMsg);
            
            logger.info("accepted proposal from " + bestProp.agentName + " for fire at " + fireKey);
        }
        
        // remove from temporary maps
        fireProposals.remove(fireKey);
        fireCfpStartTimes.remove(fireKey);
    }
}
