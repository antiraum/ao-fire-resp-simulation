package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class CFP implements Predicate {
    
    private Coordinate coordinate;
    
    /**
     * Constructor for bean instantiation.
     */
    public CFP() {

    // empty
    }
    
    /**
     * @param coordinate
     */
    public CFP(final Coordinate coordinate) {

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
