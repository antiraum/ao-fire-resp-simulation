package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class VehiclePositionInfo implements Predicate {
    
    private Coordinate vehicleCoordinate;
    
    /**
     * Constructor for bean instantiation.
     */
    public VehiclePositionInfo() {

    // empty
    }
    
    /**
     * @param vehicleCoordinate
     */
    public VehiclePositionInfo(final Coordinate vehicleCoordinate) {

        this.vehicleCoordinate = vehicleCoordinate;
    }
    
    /**
     * @return Coordinate of the vehicle.
     */
    @Slot(mandatory = true)
    public Coordinate getVehicleCoordinate() {

        return vehicleCoordinate;
    }
    
    /**
     * @param vehicleCoordinate
     */
    public void setVehicleCoordinate(final Coordinate vehicleCoordinate) {

        this.vehicleCoordinate = vehicleCoordinate;
    }
}
