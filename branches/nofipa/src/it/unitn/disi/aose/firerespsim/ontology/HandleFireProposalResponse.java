package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class HandleFireProposalResponse implements Predicate {
    
    private Coordinate coordinate;
    
    /**
     * Constructor for bean instantiation.
     */
    public HandleFireProposalResponse() {

    // empty
    }
    
    /**
     * @param coordinate
     */
    public HandleFireProposalResponse(final Coordinate coordinate) {

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
