package it.unitn.disi.aose.firerespsim.model;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.HashSet;
import java.util.Set;

/**
 * Model of the subscribers to a subscription service.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
public final class Subscribers {
    
    /**
     * Subscriptions
     */
    public final Set<ACLMessage> subscriptions = new HashSet<ACLMessage>();
    
    /**
     * @param s
     * @return <code>true</code> if was not registered, <code>false</code> if was
     */
    public boolean register(final ACLMessage s) {

        return subscriptions.add(s);
    }
    
    /**
     * @param s
     * @return <code>true</code> if was registered, <code>false</code> if not
     */
    public boolean deregister(final ACLMessage s) {

        return subscriptions.remove(s);
    }
    
    /**
     * @return The subscriptions.
     */
    public Set<ACLMessage> getSubscriptions() {

        return subscriptions;
    }
    
    public int size() {

        return subscriptions.size();
    }
    
    public boolean isEmpty() {

        return subscriptions.isEmpty();
    }
    
    public Set<AID> getAIDs() {

        final Set<AID> aids = new HashSet<AID>();
        for (final ACLMessage s : subscriptions) {
            aids.add(s.getSender());
        }
        return aids;
    }
}