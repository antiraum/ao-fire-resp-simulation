package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Concept;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class VehicleStatus implements Concept {
    
    private Coordinate position;
    private int state;
    private Coordinate fire;
    
    /**
     * Constructor for bean instantiation.
     */
    public VehicleStatus() {

    // empty
    }
    
    /**
     * @param position
     * @param state
     * @param fire
     */
    public VehicleStatus(final Coordinate position, final int state, final Coordinate fire) {

        this.position = position;
        this.state = state;
        this.fire = fire;
    }
    
    /**
     * @return current position
     */
    public Coordinate getPosition() {

        return position;
    }
    
    /**
     * @param position
     */
    public void setPosition(final Coordinate position) {

        this.position = position;
    }
    
    /**
     * @return current state
     */
    public int getState() {

        return state;
    }
    
    /**
     * @param state
     */
    public void setState(final int state) {

        this.state = state;
    }
    
    /**
     * @return current fire
     */
    public Coordinate getFire() {

        return fire;
    }
    
    /**
     * @param fire
     */
    public void setFire(final Coordinate fire) {

        this.fire = fire;
    }
}
