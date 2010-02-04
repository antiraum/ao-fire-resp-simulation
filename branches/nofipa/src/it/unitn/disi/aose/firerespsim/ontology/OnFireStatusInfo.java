package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class OnFireStatusInfo implements Predicate {
    
    private Coordinate coordinate;
    private boolean status;
    
    /**
     * Constructor for bean instantiation.
     */
    public OnFireStatusInfo() {

    // empty
    }
    
    /**
     * @param coordinate
     * @param status
     */
    public OnFireStatusInfo(final Coordinate coordinate, final boolean status) {

        this.coordinate = coordinate;
        this.status = status;
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
    
    /**
     * @return On fire status.
     */
    @Slot(mandatory = true)
    public boolean getStatus() {

        return status;
    }
    
    /**
     * @param status
     */
    public void setStatus(final boolean status) {

        this.status = status;
    }
}