package it.unitn.disi.aose.firerespsim.agents;

/**
 * This agent simulates a hospital.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class HospitalAgent extends StationaryAgent {
    
    /**
     * Constructor.
     */
    public HospitalAgent() {

        super();
        
        vehicleAgentClass = AmbulanceAgent.class.getName();
        vehicleName = "ambulance";
        coordinatorDfType = HospitalCoordinatorAgent.DF_TYPE;
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.StationaryAgent#getFireWeight(java.lang.String)
     */
    @Override
    protected int getFireWeight(final String fireKey) {

        return fires.get(fireKey).getCasualties();
    }
}
