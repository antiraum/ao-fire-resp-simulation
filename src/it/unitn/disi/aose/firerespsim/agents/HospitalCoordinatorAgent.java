package it.unitn.disi.aose.firerespsim.agents;

/**
 * Coordinator for the {@link HospitalAgent}s.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class HospitalCoordinatorAgent extends CoordinatorAgent {
    
    /**
     * DF type of this agent.
     */
    static final String DF_TYPE = "HospitalCoordinator";
    
    /**
     * Constructor.
     */
    public HospitalCoordinatorAgent() {

        super();
        
        dfType = DF_TYPE;
    }
    
}
