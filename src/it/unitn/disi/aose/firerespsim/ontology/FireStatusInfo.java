package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireStatusInfo implements Predicate {
    
    private FireStatus fireStatus;
    
    /**
     * Constructor for bean instantiation.
     */
    public FireStatusInfo() {

    // empty
    }
    
    /**
     * @param fireStatus
     */
    public FireStatusInfo(final FireStatus fireStatus) {

        this.fireStatus = fireStatus;
    }
    
    /**
     * @return Status of the fire.
     */
    @Slot(mandatory = true)
    public FireStatus getFireStatus() {

        return fireStatus;
    }
    
    /**
     * @param fireStatus
     */
    public void setFireStatus(final FireStatus fireStatus) {

        this.fireStatus = fireStatus;
    }
}
