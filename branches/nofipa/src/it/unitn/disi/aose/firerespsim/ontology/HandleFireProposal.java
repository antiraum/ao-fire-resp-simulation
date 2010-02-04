package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class HandleFireProposal implements Predicate {
    
    private Coordinate coordinate;
    private int distance;
    private int numVehicles;
    
    /**
     * Constructor for bean instantiation.
     */
    public HandleFireProposal() {

    // empty
    }
    
    /**
     * @param coordinate
     * @param distance
     * @param numVehicles
     */
    public HandleFireProposal(final Coordinate coordinate, final int distance, final int numVehicles) {

        this.coordinate = coordinate;
        this.distance = distance;
        this.numVehicles = numVehicles;
    }
    
    /**
     * @return Distance to the fire.
     */
    @Slot(mandatory = true)
    public int getDistance() {

        return distance;
    }
    
    /**
     * @return Coordinate of the fire.
     */
    public Coordinate getCoordinate() {

        return coordinate;
    }
    
    /**
     * @param coordinate
     */
    public void setCoordinate(final Coordinate coordinate) {

        this.coordinate = coordinate;
    }
    
    /**
     * @param distance
     */
    public void setDistance(final int distance) {

        this.distance = distance;
    }
    
    /**
     * @return Number of available vehicles.
     */
    @Slot(mandatory = true)
    public int getNumVehicles() {

        return numVehicles;
    }
    
    /**
     * @param numVehicles
     */
    public void setNumVehicles(final int numVehicles) {

        this.numVehicles = numVehicles;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return "distance: " + distance + ", #vehicles: " + numVehicles;
    }
}
