package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireAlert implements Predicate {
    
    private Coordinate fireCoordinate;
    
    /**
     * Constructor for bean instantiation.
     */
    public FireAlert() {

    // empty
    }
    
    /**
     * @param fireCoordinate
     */
    public FireAlert(final Coordinate fireCoordinate) {

        this.fireCoordinate = fireCoordinate;
    }
    
    /**
     * @return Coordinate of the fire.
     */
    @Slot(mandatory = true)
    public Coordinate getFireCoordinate() {

        return fireCoordinate;
    }
    
    /**
     * @param fireCoordinate
     */
    public void setFireCoordinate(final Coordinate fireCoordinate) {

        this.fireCoordinate = fireCoordinate;
    }
}
