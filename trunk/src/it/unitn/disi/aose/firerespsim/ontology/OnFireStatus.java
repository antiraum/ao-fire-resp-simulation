package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class OnFireStatus implements Predicate {
    
    private Coordinate coordinate;
    private boolean status;
    
    /**
     * Constructor for bean instantiation.
     */
    public OnFireStatus() {

    // empty
    }
    
    /**
     * @param coordinate
     * @param status
     */
    public OnFireStatus(final Coordinate coordinate, final boolean status) {

        this.coordinate = coordinate;
        this.status = status;
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
    
    /**
     * @return Coordinate of the on fire status.
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
