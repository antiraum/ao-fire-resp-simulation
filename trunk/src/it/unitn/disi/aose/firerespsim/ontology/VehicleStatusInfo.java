package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class VehicleStatusInfo implements Predicate {
    
    private VehicleStatus vehicleStatus;
    
    /**
     * Constructor for bean instantiation.
     */
    public VehicleStatusInfo() {

    // empty
    }
    
    /**
     * @param vehicleStatus
     */
    public VehicleStatusInfo(final VehicleStatus vehicleStatus) {

        this.vehicleStatus = vehicleStatus;
    }
    
    /**
     * @return Status of the vehicle.
     */
    @Slot(mandatory = true)
    public VehicleStatus getVehicleStatus() {

        return vehicleStatus;
    }
    
    /**
     * @param vehicleStatus
     */
    public void setVehicleStatus(final VehicleStatus vehicleStatus) {

        this.vehicleStatus = vehicleStatus;
    }
}
