package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.FireResponseOntology;
import it.unitn.disi.aose.firerespsim.ontology.CFP;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireAlert;
import it.unitn.disi.aose.firerespsim.ontology.Proposal;
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
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public abstract class CoordinatorAgent extends Agent {
    
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
    final Set<AID> stationaryAgents = new HashSet<AID>();
    
    /**
     * Codec for message content encoding. Package scoped for faster access by inner classes.
     */
    final Codec codec = new SLCodec();
    /**
     * Simulation ontology. Package scoped for faster access by inner classes.
     */
    final Ontology onto = FireResponseOntology.getInstance();
    
    /**
     * Current fire CFPs (key is conversation id). Package scoped for faster access by inner classes.
     */
    final Map<String, Coordinate> fireCFPs = new HashMap<String, Coordinate>();
    /**
     * Start times of current CFPs (key is conversation id). Package scoped for faster access by inner classes.
     */
    final Map<String, Long> fireCFPTimes = new HashMap<String, Long>();
    /**
     * Proposals for fires currently coordinated. Package scoped for faster access by inner classes.
     */
    final Map<Coordinate, Map<AID, Proposal>> fireProposals = new HashMap<Coordinate, Map<AID, Proposal>>();
    
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
        
        // add behaviors
        final SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new SubscribeToFireAlerts());
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new ReceiveFireAlerts(), new ReceiveProposals(), new CoordinationService(), new ReceiveProposals(),
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
                                                                       MessageTemplate.MatchOntology(onto.getName()));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
//            logger.debug("received coordination registration request");
            
            final ACLMessage replyMsg = requestMsg.createReply();
            if (stationaryAgents.contains(requestMsg.getSender())) {
                // already registered
                replyMsg.setPerformative(ACLMessage.REFUSE);
            } else {
                // new agent
                stationaryAgents.add(requestMsg.getSender());
                replyMsg.setPerformative(ACLMessage.AGREE);
            }
            send(replyMsg);
//            logger.debug("sent coordination registration reply");
        }
    }
    
    /**
     * Template for messages from the fire monitor. Package scoped for faster access by inner classes.
     */
    MessageTemplate fireMonitorTpl = null;
    
    /**
     * Subscribes to new fire alerts from the fire monitor agent.
     */
    class SubscribeToFireAlerts extends SimpleBehaviour {
        
        private boolean done = false;
        private final DFAgentDescription fireAlertAD = new DFAgentDescription();
        private AID fireMonitorAID = null;
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.or(
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE)),
                                                                     MessageTemplate.MatchOntology(onto.getName()));
        
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
                    fireMonitorTpl = MessageTemplate.MatchSender(fireMonitorAID);
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
            subscribeMsg.setOntology(onto.getName());
            send(subscribeMsg);
//            logger.debug("sent fire alert subscription request");
            
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            if (replyMsg.getPerformative() == ACLMessage.AGREE) {
                logger.info("subscribed for new fire alerts at monitor");
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
     * Receives fire alerts from the fire monitor agent. If a fire is new, proposals from the stationary agents are
     * requested.
     */
    class ReceiveFireAlerts extends CyclicBehaviour {
        
        private final Set<Coordinate> knownFires = new HashSet<Coordinate>();
        
        private final MessageTemplate alertTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology(onto.getName()));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage alertMsg = blockingReceive(alertTpl);
            if (!fireMonitorTpl.match(alertMsg)) return;
            if (alertMsg == null) return;
            
            ContentElement ce;
            try {
                ce = getContentManager().extractContent(alertMsg);
            } catch (final Exception e) {
                logger.error("error extracting message content");
                return;
            }
            if (!(ce instanceof FireAlert)) {
                logger.error("cfp message has wrong content");
                return;
            }
            final FireAlert alert = (FireAlert) ce;
            final Coordinate fireCoord = alert.getCoordinate();
            logger.debug("received alert for fire at (" + fireCoord + ")");
            
            if (knownFires.contains(fireCoord)) return; // not new
            knownFires.add(fireCoord);
            
            if (stationaryAgents.size() > 0) {
                // request proposals from stationary agents
                final ACLMessage cfpMsg = new ACLMessage(ACLMessage.CFP);
                cfpMsg.setOntology(onto.getName());
                cfpMsg.setLanguage(codec.getName());
                for (final AID aid : stationaryAgents) {
                    cfpMsg.addReceiver(aid);
                }
                try {
                    getContentManager().fillContent(cfpMsg, new CFP(fireCoord));
                    send(cfpMsg);
                    logger.info("sent CFP to stationary agents");
                    fireCFPs.put(cfpMsg.getConversationId(), fireCoord);
                    fireProposals.put(fireCoord, new HashMap<AID, Proposal>());
                    fireCFPTimes.put(cfpMsg.getConversationId(), System.currentTimeMillis());
                } catch (final Exception e) {
                    logger.error("error filling message content");
                }
            } else {
                logger.debug("no stationary agent registered to send CFP to");
            }
        }
    }
    
    /**
     * Receives the proposals from the stationary agents. After all proposals are collected or if the proposal timeout
     * is reached, the responsibility is given to the agent with the best proposal.
     */
    class ReceiveProposals extends CyclicBehaviour {
        
        private final MessageTemplate proposalTpl = MessageTemplate.and(
                                                                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                                                        MessageTemplate.MatchOntology(onto.getName()));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage proposalMsg = blockingReceive(proposalTpl);
            if (proposalMsg == null) return;
            
            if (!fireCFPs.containsKey(proposalMsg.getConversationId())) {
                logger.debug("received proposal for fire that is currently not coordinated");
                return;
            }
            final Coordinate fireCoord = fireCFPs.get(proposalMsg.getConversationId());
            
            // save proposal
            ContentElement ce;
            try {
                ce = getContentManager().extractContent(proposalMsg);
            } catch (final Exception e) {
                logger.error("error extracting message content");
                return;
            }
            if (!(ce instanceof Proposal)) {
                logger.error("proposal message has wrong content");
                return;
            }
            final Proposal prop = (Proposal) ce;
            logger.debug("received proposal (" + prop + ")");
            fireProposals.get(fireCoord).put(proposalMsg.getSender(), prop);
            
            if (fireProposals.get(fireCoord).size() == stationaryAgents.size()) {
                // all proposals received
                logger.debug("received all proposals for fire at (" + fireCoord + ")");
                assignFire(fireCoord, proposalMsg.getConversationId());
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

            for (final Map.Entry<String, Long> cfpTime : fireCFPTimes.entrySet()) {
                if (System.currentTimeMillis() - cfpTime.getValue() > WAIT_FOR_PROPOSALS_MS) {
                    assignFire(fireCFPs.get(cfpTime.getKey()), cfpTime.getKey());
                }
            }
        }
    }
    
    /**
     * Finish coordination of a fire. Sends a proposal accept message to the agent with the best proposal and proposal
     * reject messages to all other agents. Package scoped for faster access by inner classes.
     * 
     * @param fireCoord
     * @param cfpId
     */
    void assignFire(final Coordinate fireCoord, final String cfpId) {

        // find best proposal
        AID bestSA = null;
        Proposal bestProp = null;
        for (final Map.Entry<AID, Proposal> saProp : fireProposals.get(fireCoord).entrySet()) {
            final Proposal prop = saProp.getValue();
            if (bestSA == null || bestProp == null || prop.getNumVehicles() > bestProp.getNumVehicles() ||
                (prop.getNumVehicles() == bestProp.getNumVehicles() && prop.getDistance() < bestProp.getDistance())) {
                bestSA = saProp.getKey();
                bestProp = prop;
            }
        }
        
        if (bestSA == null || bestProp == null) {
            
            logger.error("no proposal for fire at " + fireCoord + ", will not be handled!");
            
        } else {
            
            // send accept message
            final ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            acceptMsg.setConversationId(cfpId);
            acceptMsg.setOntology(onto.getName());
            acceptMsg.addReceiver(bestSA);
            send(acceptMsg);
            
            // send reject message
            final ACLMessage recjectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            acceptMsg.setOntology(onto.getName());
            for (final Map.Entry<AID, Proposal> saProp : fireProposals.get(fireCoord).entrySet()) {
                if (saProp.getKey() != bestSA) {
                    recjectMsg.addReceiver(saProp.getKey());
                }
            }
            send(recjectMsg);
            
            logger.info("accepted proposal (" + bestProp + ")");
        }
        
        // remove from temporary maps
        fireCFPs.remove(cfpId);
        fireCFPTimes.remove(cfpId);
        fireProposals.remove(fireCoord);
    }
}
