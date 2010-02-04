package it.unitn.disi.aose.firerespsim.agents;

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
}
