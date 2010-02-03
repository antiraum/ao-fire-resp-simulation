package it.unitn.disi.aose.firerespsim.model;

import it.unitn.disi.aose.firerespsim.ontology.VehicleStatus;
import it.unitn.disi.aose.firerespsim.util.SyncedBoolean;
import it.unitn.disi.aose.firerespsim.util.SyncedInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Model of a vehicle. Thread-safe.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
public final class Vehicle {
    
    /**
     * State if vehicle has no target.
     */
    public static final int STATE_IDLE = 0;
    /**
     * State if vehicle is moving to its target.
     */
    public static final int STATE_TO_TARGET = 1;
    /**
     * State if vehicle is at its target.
     */
    public static final int STATE_AT_TARGET = 2;
    
    private static final List<Integer> allowedStates = Arrays.asList(STATE_IDLE, STATE_TO_TARGET, STATE_AT_TARGET);
    
    /**
     * Current position on the simulation area.
     */
    public final Position position;
    /**
     * Current state.
     */
    private final SyncedInteger state;
    /**
     * Position where the vehicle is stationed.
     */
    public Position home;
    /**
     * Position of the fire currently assigned to.
     */
    public Position fire = null;
    /**
     * Current target position on the simulation area.
     */
    public Position target = null;
    /**
     * If currently accepting a new target.
     */
    private final SyncedBoolean acceptingTarget = new SyncedBoolean(true);
    
    /**
     * @param position
     * @param state
     */
    public Vehicle(final Position position, final int state) {

        this.position = position.clone();
        home = position.clone();
        this.state = new SyncedInteger(state);
    }
    
    /**
     * @return Current state ({@link #STATE_IDLE}, {@link #STATE_TO_TARGET}, or {@link #STATE_AT_TARGET}).
     */
    public final int getState() {

        return state.get();
    }
    
    /**
     * @param state {@link #STATE_IDLE}, {@link #STATE_TO_TARGET}, or {@link #STATE_AT_TARGET}
     */
    public final void setState(final int state) {

        if (!allowedStates.contains(state)) return;
        this.state.set(state);
    }
    
    /**
     * @return <code>true</code> if accepting new target, <code>false</code> if not
     */
    public final boolean isAcceptingTarget() {

        return acceptingTarget.get();
    }
    
    /**
     * @param value
     */
    public final void setAcceptingTarget(final boolean value) {

        acceptingTarget.set(value);
    }
    
    /**
     * @return {@link VehicleStatus}
     */
    public VehicleStatus getVehicleStatus() {

        return new VehicleStatus(position.getCoordinate(), state.get(), (fire == null) ? null : fire.getCoordinate());
    }
}
