package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class PickUpCasualtyRequest implements Predicate {
    
    private Coordinate ambulancePosition;
    
    /**
     * Constructor for bean instantiation.
     */
    public PickUpCasualtyRequest() {

    // empty
    }
    
    /**
     * @param ambulancePosition
     */
    public PickUpCasualtyRequest(final Coordinate ambulancePosition) {

        this.ambulancePosition = ambulancePosition;
    }
    
    /**
     * @return Target coordinate.
     */
    @Slot(mandatory = true)
    public Coordinate getAmbulancePosition() {

        return ambulancePosition;
    }
    
    /**
     * @param ambulancePosition
     */
    public void setAmbulancePosition(final Coordinate ambulancePosition) {

        this.ambulancePosition = ambulancePosition;
    }
    
}
