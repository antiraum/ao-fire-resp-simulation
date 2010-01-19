package it.unitn.disi.aose.firerespsim.model;

import it.unitn.disi.aose.firerespsim.agents.VehicleAgent;
import it.unitn.disi.aose.firerespsim.util.SyncedBoolean;
import it.unitn.disi.aose.firerespsim.util.SyncedInteger;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Represents the status of a {@link VehicleAgent}. Thread-safe.
 * 
 * @author tom
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
    
    private static final List<Integer> allowedStates = Arrays.asList(new Integer[] {
        STATE_IDLE, STATE_TO_TARGET, STATE_AT_TARGET});
    
    /**
     * Id of the vehicle.
     */
    public final int id;
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
     * @param vehicleId
     * @param position
     * @param state
     */
    public Vehicle(final int vehicleId, final Position position, final int state) {

        id = vehicleId;
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
    
    private static final String FIELD_SEPARATOR = " / ";
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {

        return StringUtils.join(new Object[] {id, position, state}, FIELD_SEPARATOR);
    }
    
    /**
     * @param str
     * @return {@link Vehicle}
     */
    public final static Vehicle fromString(final String str) {

        final String[] fields = str.split(FIELD_SEPARATOR);
        if (fields.length != 3) return null;
        return new Vehicle(Integer.parseInt(fields[0]), Position.fromString(fields[1]), Integer.parseInt(fields[2]));
    }
}
