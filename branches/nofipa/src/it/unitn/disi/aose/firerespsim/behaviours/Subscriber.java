package it.unitn.disi.aose.firerespsim.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.log4j.Logger;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class Subscriber extends OneShotBehaviour {
    
    private static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    private final ACLMessage msg;
    private final MessageTemplate mt;
    private final String dsKey;
    
    /**
     * @param a
     * @param msg
     * @param mt
     * @param dsKey Data store key of the subscription service AID.
     */
    public Subscriber(final Agent a, final ACLMessage msg, final MessageTemplate mt, final String dsKey) {

        super(a);
        this.msg = msg;
        this.mt = mt;
        this.dsKey = dsKey;
    }
    
    /**
     * @see jade.core.behaviours.Behaviour#action()
     */
    @Override
    public void action() {

//        logger.debug("sending subscription request");
        
        msg.addReceiver((AID) getDataStore().get(dsKey));
        myAgent.send(msg);
        
        final ACLMessage response = myAgent.blockingReceive(mt);
        
        if (response.getPerformative() == ACLMessage.AGREE) {
            logger.info("subscription request was accepted");
        } else {
//            logger.error("subscription request was refused");
        }
    }
}
