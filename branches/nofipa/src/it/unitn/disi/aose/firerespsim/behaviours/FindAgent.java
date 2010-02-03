package it.unitn.disi.aose.firerespsim.behaviours;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import org.apache.log4j.Logger;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FindAgent extends SimpleBehaviour {
    
    private static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    private final String dfType;
    private final String dsKey;
    private final DFAgentDescription agentAD = new DFAgentDescription();
    private boolean done = false;
    
    /**
     * @param a Calling agent.
     * @param dfType DF service type of the service agent.
     * @param dsKey Data store key to store service agent AID.
     */
    public FindAgent(final Agent a, final String dfType, final String dsKey) {

        super(a);
        
        this.dfType = dfType;
        this.dsKey = dsKey;
        
        final ServiceDescription agentSD = new ServiceDescription();
        agentSD.setType(dfType);
        agentAD.addServices(agentSD);
    }
    
    /**
     * @see jade.core.behaviours.Behaviour#action()
     */
    @Override
    public void action() {

        DFAgentDescription[] result = null;
        try {
            result = DFService.search(myAgent, agentAD);
        } catch (final FIPAException e) {
            logger.error("error searching for agent with " + dfType + " service at DF");
            return;
        }
        if (result != null && result.length > 0) {
            getDataStore().put(dsKey, result[0].getName());
            logger.info("found agent with " + dfType + " service");
            done = true;
            return;
        }
        logger.debug("no agent with " + dfType + " service at DF");
        
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            done = true;
        }
    }
    
    /**
     * @see jade.core.behaviours.Behaviour#done()
     */
    @Override
    public boolean done() {

        return done;
    }
}