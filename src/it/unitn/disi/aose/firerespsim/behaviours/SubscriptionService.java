package it.unitn.disi.aose.firerespsim.behaviours;

import it.unitn.disi.aose.firerespsim.agents.ExtendedAgent;
import it.unitn.disi.aose.firerespsim.model.Subscribers;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class SubscriptionService extends CyclicBehaviour {
    
    private final MessageTemplate mt;
    private final Subscribers sm;
    
    /**
     * @param a
     * @param mt
     * @param sm
     */
    public SubscriptionService(final Agent a, final MessageTemplate mt, final Subscribers sm) {

        super(a);
        this.mt = mt;
        this.sm = sm;
    }
    
    /**
     * @see jade.core.behaviours.Behaviour#action()
     */
    @Override
    public void action() {

        final ACLMessage request = myAgent.blockingReceive(mt);
        if (request == null) return;
        
        int perf;
        if (request.getPerformative() == ACLMessage.SUBSCRIBE) {
            perf = sm.register(request) ? ACLMessage.AGREE : ACLMessage.REFUSE;
        } else {
            perf = sm.deregister(request) ? ACLMessage.INFORM : ACLMessage.FAILURE;
        }
        
        ((ExtendedAgent) myAgent).sendReply(request, perf);
    }
}