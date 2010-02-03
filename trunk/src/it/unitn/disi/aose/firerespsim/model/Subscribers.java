package it.unitn.disi.aose.firerespsim.model;

import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Model of the subscribers to a subscription service.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
public final class Subscribers implements SubscriptionManager {
    
    /**
     * Subscriptions
     */
    public final Set<Subscription> subscriptions = new HashSet<Subscription>();
    
    /**
     * @see jade.proto.SubscriptionResponder.SubscriptionManager#register(jade.proto.SubscriptionResponder.Subscription)
     */
    @Override
    public boolean register(final Subscription s) throws RefuseException, NotUnderstoodException {

        return subscriptions.add(s);
    }
    
    /**
     * @see jade.proto.SubscriptionResponder.SubscriptionManager#deregister(jade.proto.SubscriptionResponder.Subscription)
     */
    @Override
    public boolean deregister(final Subscription s) throws FailureException {

        return subscriptions.remove(s);
    }
    
    /**
     * @return The subscriptions.
     */
    public Set<Subscription> getSubscriptions() {

        return subscriptions;
    }
}