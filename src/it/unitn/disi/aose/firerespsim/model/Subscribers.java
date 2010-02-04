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
    
    private final Set<AID> aids = new HashSet<AID>();
    private final Set<ACLMessage> subscriptions = new HashSet<ACLMessage>();
    
    /**
     * @param s
     * @return <code>true</code> if was not registered, <code>false</code> if was
     */
    public boolean register(final ACLMessage s) {

        if (aids.add(s.getSender())) {
            subscriptions.add(s);
            return true;
        }
        return false;
    }
    
    /**
     * @param s
     * @return <code>true</code> if was registered, <code>false</code> if not
     */
    public boolean deregister(final ACLMessage s) {

        if (aids.remove(s.getSender())) {
            subscriptions.remove(s);
            return true;
        }
        return false;
    }
    
    /**
     * @return AIDs of the subscribers.
     */
    public Set<AID> getAIDs() {

        return aids;
    }
    
    /**
     * @return The subscriptions.
     */
    public Set<ACLMessage> getSubscriptions() {

        return subscriptions;
    }
    
    /**
     * @return Number of subscribers.
     */
    public int size() {

        return aids.size();
    }
    
    /**
     * @return <code>true</code> if there are no subscribers, <code>false</code> if there are
     */
    public boolean isEmpty() {

        return aids.isEmpty();
    }
}