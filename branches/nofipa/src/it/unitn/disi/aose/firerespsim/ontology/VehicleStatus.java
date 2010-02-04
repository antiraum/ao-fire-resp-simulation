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
    private boolean acceptingTarget;
    
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
     * @param acceptingTarget
     */
    public VehicleStatus(final Coordinate position, final int state, final Coordinate fire,
                         final boolean acceptingTarget) {

        this.position = position;
        this.state = state;
        this.fire = fire;
        this.acceptingTarget = acceptingTarget;
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
    
    /**
     * @return If vehicle is accepting set target requests.
     */
    public boolean isAcceptingTarget() {

        return acceptingTarget;
    }
    
    /**
     * @param acceptingTarget
     */
    public void setAcceptingTarget(final boolean acceptingTarget) {

        this.acceptingTarget = acceptingTarget;
    }
}
