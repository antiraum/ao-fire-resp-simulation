package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class SetTargetRequest implements Predicate {
    
    private Coordinate target;
    
    /**
     * Constructor for bean instantiation.
     */
    public SetTargetRequest() {

    // empty
    }
    
    /**
     * @param target
     */
    public SetTargetRequest(final Coordinate target) {

        this.target = target;
    }
    
    /**
     * @return Target coordinate.
     */
    @Slot(mandatory = true)
    public Coordinate getTarget() {

        return target;
    }
    
    /**
     * @param target
     */
    public void setTarget(final Coordinate target) {

        this.target = target;
    }
    
}
