package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class OnFireStatusRequest implements Predicate {
    
    private Coordinate coordinate;
    
    /**
     * Constructor for bean instantiation.
     */
    public OnFireStatusRequest() {

    // empty
    }
    
    /**
     * @param coordinate
     */
    public OnFireStatusRequest(final Coordinate coordinate) {

        this.coordinate = coordinate;
    }
    
    /**
     * @return Coordinate to check on fire status for.
     */
    @Slot(mandatory = true)
    public Coordinate getCoordinate() {

        return coordinate;
    }
    
    /**
     * @param coordinate
     */
    public void setCoordinate(final Coordinate coordinate) {

        this.coordinate = coordinate;
    }
    
}
