package it.unitn.disi.aose.firerespsim.behaviours;

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionResponder;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class SubscriptionService extends SubscriptionResponder {
    
    /**
     * @param a
     * @param mt
     * @param sm
     */
    public SubscriptionService(final Agent a, final MessageTemplate mt, final SubscriptionManager sm) {

        super(a, mt, sm);
    }
    
    /**
     * @see jade.proto.SubscriptionResponder#handleSubscription(jade.lang.acl.ACLMessage)
     */
    @Override
    protected ACLMessage handleSubscription(final ACLMessage subscription) throws NotUnderstoodException,
            RefuseException {

        final ACLMessage reply = subscription.createReply();
        final Subscription subs = createSubscription(subscription);
        reply.setPerformative(mySubscriptionManager.register(subs) ? ACLMessage.AGREE : ACLMessage.REFUSE);
        return reply;
    }
    
    /**
     * @see jade.proto.SubscriptionResponder#handleCancel(jade.lang.acl.ACLMessage)
     */
    @Override
    protected ACLMessage handleCancel(final ACLMessage cancel) throws FailureException {

        final ACLMessage reply = cancel.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        final Subscription subs = getSubscription(cancel);
        if (subs != null) {
            if (mySubscriptionManager.deregister(subs)) {
                reply.setPerformative(ACLMessage.INFORM);
            }
            subs.close();
        }
        return reply;
    }
}