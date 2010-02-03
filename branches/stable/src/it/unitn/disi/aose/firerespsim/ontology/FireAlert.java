package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireAlert implements Predicate {
    
    private Coordinate coordinate;
    
    /**
     * Constructor for bean instantiation.
     */
    public FireAlert() {

    // empty
    }
    
    /**
     * @param coordinate
     */
    public FireAlert(final Coordinate coordinate) {

        this.coordinate = coordinate;
    }
    
    /**
     * @return Coordinate of the fire.
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
