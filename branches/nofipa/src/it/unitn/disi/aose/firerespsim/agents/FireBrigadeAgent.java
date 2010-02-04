package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.ontology.FireStatus;

/**
 * This agent simulates a fire brigade.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireBrigadeAgent extends StationaryAgent {
    
    /**
     * Constructor.
     */
    public FireBrigadeAgent() {

        super();
        
        vehicleAgentClass = FireEngineAgent.class.getName();
        vehicleName = "fire engine";
        coordinatorDfType = FireBrigadeCoordinatorAgent.DF_TYPE;
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.StationaryAgent#getFireWeight(java.lang.String)
     */
    @Override
    protected int getFireWeight(final String fireKey) {

        final FireStatus fireStatus = fires.get(fireKey);
        return (fireStatus == null) ? 1 : fireStatus.getIntensity();
    }
}
