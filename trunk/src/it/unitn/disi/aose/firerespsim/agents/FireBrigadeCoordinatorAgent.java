package it.unitn.disi.aose.firerespsim.agents;

/**
 * Coordinator for the {@link FireBrigadeAgent}s.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireBrigadeCoordinatorAgent extends CoordinatorAgent {
    
    /**
     * DF type of this agent.
     */
    static final String DF_TYPE = "FireBrigadeCoordinator";
    
    /**
     * Constructor.
     */
    public FireBrigadeCoordinatorAgent() {

        super();
        
        dfType = DF_TYPE;
    }
    
}