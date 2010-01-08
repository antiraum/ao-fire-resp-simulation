package it.unitn.disi.aose.firerespsim.agents;

/**
 * This agent simulates a hospital.
 * 
 * @author tom
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
}
