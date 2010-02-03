package it.unitn.disi.aose.firerespsim.agents;

/**
 * Coordinator for the {@link FireBrigadeAgent}s.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireBrigadeCoordinatorAgent extends CoordinatorAgent {
    
    /**
     * DF service type.
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
