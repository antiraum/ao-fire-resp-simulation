package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class PutOutRequest implements Predicate {
    
    private Coordinate fireEnginePosition;
    
    /**
     * Constructor for bean instantiation.
     */
    public PutOutRequest() {

    // empty
    }
    
    /**
     * @param fireEnginePosition
     */
    public PutOutRequest(final Coordinate fireEnginePosition) {

        this.fireEnginePosition = fireEnginePosition;
    }
    
    /**
     * @return Target coordinate.
     */
    @Slot(mandatory = true)
    public Coordinate getFireEnginePosition() {

        return fireEnginePosition;
    }
    
    /**
     * @param fireEnginePosition
     */
    public void setFireEnginePosition(final Coordinate fireEnginePosition) {

        this.fireEnginePosition = fireEnginePosition;
    }
    
}
