package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.behaviours.FindAgent;
import it.unitn.disi.aose.firerespsim.behaviours.Subscriber;
import it.unitn.disi.aose.firerespsim.behaviours.SubscriptionService;
import it.unitn.disi.aose.firerespsim.model.Subscribers;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireAlert;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireCFP;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireProposal;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireProposalResponse;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.DataStore;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Coordinator for {@link StationaryAgent}s. Subscribes at the {@link FireMonitorAgent} for fire alerts. Distribute the
 * responsibilities for the fires among the registered stationary agents. No start-up parameters.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public abstract class CoordinatorAgent extends ExtendedAgent {
    
    /**
     * Protocol for coordination messages.
     */
    public static final String COORDINATION_PROTOCOL = "Coordination";
    /**
     * Coordination DF service type. Must be set it in the concrete subclasses.
     */
    protected String dfType;
    
    /**
     * Package scoped for faster access by inner classes.
     */
    final Subscribers coordSubscribers = new Subscribers();
    /**
     * Currently known fires. Package scoped for faster access by inner classes.
     */
    final Set<String> knownFires = new HashSet<String>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        dfTypes = new String[] {dfType};
        
        super.setup();
        
        // create data store
        final String fireMonitorAIDKey = "FIRE_MONITOR_AID";
        final DataStore ds = new DataStore();
        
        // create behaviors
        final FindAgent findFireMonitor = new FindAgent(this, FireMonitorAgent.FIRE_ALERT_DF_TYPE, fireMonitorAIDKey);
        findFireMonitor.setDataStore(ds);
        final ACLMessage fireAlertSubsMsg = createMessage(ACLMessage.SUBSCRIBE, FireMonitorAgent.FIRE_ALERT_PROTOCOL);
        final MessageTemplate fireAlertSubsTpl = createMessageTemplate(null, SUBSCRIBE_RESPONSE_PERFORMATIVES,
                                                                       FireMonitorAgent.FIRE_ALERT_PROTOCOL);
        final Subscriber fireAlertSubscriber = new Subscriber(this, fireAlertSubsMsg, fireAlertSubsTpl,
                                                              fireMonitorAIDKey);
        fireAlertSubscriber.setDataStore(ds);
        final MessageTemplate fireAlertTpl = createMessageTemplate(null, REQUEST_RESPONSE_PERFORMATIVES,
                                                                   FireMonitorAgent.FIRE_ALERT_PROTOCOL);
        final HandleFireAlert handleFireAlert = new HandleFireAlert(this, fireAlertTpl);
        final MessageTemplate coordSubsTpl = createMessageTemplate(null, COORDINATION_PROTOCOL, ACLMessage.SUBSCRIBE);
        final SubscriptionService coordSubsService = new SubscriptionService(this, coordSubsTpl, coordSubscribers);
        final MessageTemplate proposalTpl = createMessageTemplate(null, COORDINATION_PROTOCOL, ACLMessage.PROPOSE);
        final HandleProposals handleProposals = new HandleProposals(this, proposalTpl);
        
        // add behaviors
        sequentialBehaviours.add(findFireMonitor);
        parallelBehaviours.addAll(Arrays.asList(fireAlertSubscriber, handleFireAlert, coordSubsService, handleProposals));
        addBehaviours();
    }
    
    /**
     * Handles incoming fire alert. If a fire is new, sends a CFP to the registered stationary agents.
     */
    private class HandleFireAlert extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public HandleFireAlert(final Agent a, final MessageTemplate mt) {

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
            
            Coordinate fireCoord;
            try {
                fireCoord = extractMessageContent(FireAlert.class, response, false).getFireCoordinate();
            } catch (final Exception e) {
                return;
            }
            if (knownFires.contains(fireCoord.toString())) return; // not new
                
            logger.debug("received alert for fire at (" + fireCoord + ")");
            knownFires.add(fireCoord.toString());
            // TODO remove fires when put out
            
            // send cfp
            if (coordSubscribers.isEmpty()) {
                logger.error("no stationary agent registered to send CFP to - fire will not be handled!");
                return;
            }
            sendMessage(ACLMessage.CFP, COORDINATION_PROTOCOL, coordSubscribers.getAIDs(), new HandleFireCFP(fireCoord));
            logger.debug("sent CFP for fire at (" + fireCoord + ")");
        }
    }
    
    private class HandleProposals extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public HandleProposals(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        private final Map<String, Set<ProposalInfo>> proposals = new HashMap<String, Set<ProposalInfo>>();
        
        private class ProposalInfo {
            
            public ACLMessage msg;
            public HandleFireProposal proposal;
            
            public ProposalInfo(final ACLMessage msg, final HandleFireProposal proposal) {

                this.msg = msg;
                this.proposal = proposal;
            }
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage proposalMsg = blockingReceive(mt);
            if (proposalMsg == null) return;
            
            HandleFireProposal proposal;
            try {
                proposal = extractMessageContent(HandleFireProposal.class, proposalMsg, false);
            } catch (final Exception e) {
                sendReply(proposalMsg, ACLMessage.FAILURE, "cannot read proposal content");
                return;
            }
            final Coordinate fireCoord = proposal.getCoordinate();
//            logger.debug("received proposal for fire at (" + fireCoord + ")");
            if (!proposals.containsKey(fireCoord.toString())) {
                proposals.put(fireCoord.toString(), new HashSet<ProposalInfo>());
            }
            proposals.get(fireCoord.toString()).add(new ProposalInfo(proposalMsg, proposal));
            
            if (proposals.get(fireCoord.toString()).size() != coordSubscribers.size()) return;
            
            // find best proposal
            ProposalInfo bestPropInfo = null;
            for (final ProposalInfo propInfo : proposals.get(fireCoord.toString())) {
                
                if (bestPropInfo == null) {
                    bestPropInfo = propInfo;
                    continue;
                }
                
                final HandleFireProposal bestProp = bestPropInfo.proposal;
                final HandleFireProposal prop = propInfo.proposal;
                
                // number of available vehicles is primary criteria, distance to fire is secondary criteria
                if (prop.getNumVehicles() > bestProp.getNumVehicles() ||
                    (prop.getNumVehicles() == bestProp.getNumVehicles() && prop.getDistance() < bestProp.getDistance())) {
                    bestPropInfo = propInfo;
                }
            }
            
            if (bestPropInfo == null) {
                logger.error("no proposal for fire, will not be handled!");
                return;
            }
            logger.info("accepted proposal (" + bestPropInfo.proposal + ")");
            
            // send replies
            for (final ProposalInfo propInfo : proposals.get(fireCoord.toString())) {
                sendReply(propInfo.msg, (propInfo == bestPropInfo) ? ACLMessage.ACCEPT_PROPOSAL
                                                                  : ACLMessage.REJECT_PROPOSAL,
                          new HandleFireProposalResponse(fireCoord));
            }
        }
    }
}
