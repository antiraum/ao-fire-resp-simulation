package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class SetTargetRequest implements Predicate {
    
    private Coordinate coordinate;
    
    /**
     * Constructor for bean instantiation.
     */
    public SetTargetRequest() {

    // empty
    }
    
    /**
     * @param coordinate
     */
    public SetTargetRequest(final Coordinate coordinate) {

        this.coordinate = coordinate;
    }
    
    /**
     * @return Target coordinate.
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
