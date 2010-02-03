package it.unitn.disi.aose.firerespsim.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class Subscriber extends SubscriptionInitiator {
    
    private static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    private final String dsKey;
    
    /**
     * @param a
     * @param msg
     * @param store
     * @param dsKey Data store key of the subscription service AID.
     */
    public Subscriber(final Agent a, final ACLMessage msg, final DataStore store, final String dsKey) {

        super(a, msg, store);
        
        this.dsKey = dsKey;
    }
    
    /**
     * @see jade.proto.SubscriptionInitiator#prepareSubscriptions(jade.lang.acl.ACLMessage)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Vector prepareSubscriptions(final ACLMessage subscription) {

        logger.debug("sending subscription request");
        
        subscription.addReceiver((AID) getDataStore().get(dsKey));
        return super.prepareSubscriptions(subscription);
    }
    
    /**
     * @see jade.proto.SubscriptionInitiator#handleAgree(jade.lang.acl.ACLMessage)
     */
    @Override
    protected void handleAgree(final ACLMessage agree) {

        logger.info("subscription request was accepted");
        
        super.handleAgree(agree);
    }
    
    /**
     * @see jade.proto.SubscriptionInitiator#handleRefuse(jade.lang.acl.ACLMessage)
     */
    @Override
    protected void handleRefuse(final ACLMessage refuse) {

        logger.error("subscription request was refused");
        
        super.handleRefuse(refuse);
    }
}
