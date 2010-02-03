package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.behaviours.FindAgent;
import it.unitn.disi.aose.firerespsim.behaviours.Subscriber;
import it.unitn.disi.aose.firerespsim.behaviours.SubscriptionService;
import it.unitn.disi.aose.firerespsim.model.Subscribers;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireAlert;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireCFP;
import it.unitn.disi.aose.firerespsim.ontology.HandleFireProposal;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.SubscriptionResponder.Subscription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

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
    final Set<Coordinate> knownFires = new HashSet<Coordinate>();
    
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
        final Subscriber fireAlertSubscriber = new Subscriber(this, fireAlertSubsMsg, ds, fireMonitorAIDKey);
        fireAlertSubscriber.registerHandleInform(new HandleFireAlert(this));
        final MessageTemplate coordSubsTpl = createMessageTemplate(null, COORDINATION_PROTOCOL, ACLMessage.SUBSCRIBE);
        final SubscriptionService coordSubsService = new SubscriptionService(this, coordSubsTpl, coordSubscribers);
        
        // add behaviors
        sequentialBehaviours.add(findFireMonitor);
        parallelBehaviours.addAll(Arrays.asList(fireAlertSubscriber, coordSubsService));
        addBehaviours();
    }
    
    /**
     * Handles a fire alert. If a fire is new, starts a {@link CoordinateFire} for it.
     */
    private class HandleFireAlert extends OneShotBehaviour {
        
        /**
         * @param a
         */
        public HandleFireAlert(final Agent a) {

            super(a);
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @SuppressWarnings("unchecked")
        @Override
        public void action() {

            // look for fire alert in data store
            Coordinate fireCoord = null;
            final Iterator iter = getDataStore().values().iterator();
            while (iter.hasNext()) {
                final Object val = iter.next();
                if (!(val instanceof ACLMessage)) {
                    continue;
                }
                try {
                    fireCoord = extractMessageContent(FireAlert.class, (ACLMessage) val, true).getFireCoordinate();
                } catch (final Exception e) {
                    continue;
                }
            }
            if (fireCoord == null) {
                logger.error("cannot find fire alert in data store");
                return;
            }
            logger.debug("received alert for fire at (" + fireCoord + ")");
            
            // add to known fires
            if (knownFires.contains(fireCoord)) return; // not new
            knownFires.add(fireCoord); // TODO remove fires when put out
            
            // start cfp
            final Set<Subscription> coordSubscriptions = coordSubscribers.getSubscriptions();
            if (coordSubscriptions.isEmpty()) {
                logger.error("no stationary agent registered to send CFP to - fire will not be handled!");
                return;
            }
            final List<AID> recipients = new ArrayList<AID>();
            for (final Subscription sub : coordSubscriptions) {
                recipients.add(sub.getMessage().getSender());
            }
            final ACLMessage cfpMsg = createMessage(ACLMessage.CFP, COORDINATION_PROTOCOL, recipients,
                                                    new HandleFireCFP(fireCoord));
            if (coordinateFire == null) {
                coordinateFire = new CoordinateFire(myAgent, cfpMsg);
            } else {
                coordinateFire.reset(cfpMsg);
            }
            addParallelBehaviour(coordinateFire);
        }
    }
    
    /**
     * Instance of {@link CoordinateFire} that gets re-used for every coordination. Package scoped for faster access by
     * inner classes.
     */
    CoordinateFire coordinateFire = null;
    
    /**
     * Coordinates the responsibility for a new fire. Sends a CFP and gives the responsibility to the stationary agent
     * with the best proposal.
     */
    private class CoordinateFire extends ContractNetInitiator {
        
        /**
         * @param a
         * @param cfp
         */
        public CoordinateFire(final Agent a, final ACLMessage cfp) {

            super(a, cfp);
        }
        
        /**
         * @see jade.proto.ContractNetInitiator#handleAllResponses(java.util.Vector, java.util.Vector)
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void handleAllResponses(final Vector responses, final Vector acceptances) {

            // find best proposal
            ACLMessage bestMsg = null;
            HandleFireProposal bestProp = null;
            ListIterator<ACLMessage> iter = responses.listIterator();
            while (iter.hasNext()) {
                final ACLMessage msg = iter.next();
                if (msg.getPerformative() != ACLMessage.PROPOSE) {
                    continue;
                }
                HandleFireProposal prop;
                try {
                    prop = extractMessageContent(HandleFireProposal.class, msg, false);
                } catch (final Exception e) {
                    continue;
                }
                // number of available vehicles is primary criteria, distance to fire is secondary criteria
                if (bestProp == null || prop.getNumVehicles() > bestProp.getNumVehicles() ||
                    (prop.getNumVehicles() == bestProp.getNumVehicles() && prop.getDistance() < bestProp.getDistance())) {
                    bestMsg = msg;
                    bestProp = prop;
                }
            }
            
            if (bestProp == null) {
                logger.error("no proposal for fire, will not be handled!");
                return;
            }
            logger.info("accepted proposal (" + bestProp + ")");
            
            // create replies
            iter = responses.listIterator();
            while (iter.hasNext()) {
                final ACLMessage msg = iter.next();
                if (msg.getPerformative() != ACLMessage.PROPOSE) {
                    continue;
                }
                acceptances.add(createReply(msg, (msg == bestMsg) ? ACLMessage.ACCEPT_PROPOSAL
                                                                 : ACLMessage.REJECT_PROPOSAL, null));
            }
        }
    }
}
