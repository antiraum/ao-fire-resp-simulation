package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class OnFireStatusInfo implements Predicate {
    
    private boolean status;
    
    /**
     * Constructor for bean instantiation.
     */
    public OnFireStatusInfo() {

    // empty
    }
    
    /**
     * @param status
     */
    public OnFireStatusInfo(final boolean status) {

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
}
