package it.unitn.disi.aose.firerespsim.model;

import it.unitn.disi.aose.firerespsim.util.SyncedInteger;
import org.apache.commons.lang.StringUtils;

/**
 * Status of a fire. Thread-safe.
 * 
 * @author tom
 */
public final class Fire {
    
    /**
     * Position of the fire on the simulation area.
     */
    public Position position;
    /**
     * Current fire intensity.
     */
    private final SyncedInteger intensity = new SyncedInteger(0);
    /**
     * Current number of casualties at the fire.
     */
    private final SyncedInteger casualties = new SyncedInteger(0);
    
    /**
     * @param position
     * @param intensity
     * @param casualties
     */
    public Fire(final Position position, final int intensity, final int casualties) {

        this.position = position;
        setIntensity(intensity);
        setCasualties(casualties);
    }
    
    /**
     * @return Current fire intensity.
     */
    public final int getIntensity() {

        return intensity.get();
    }
    
    /**
     * @param intensity
     */
    public final void setIntensity(final int intensity) {

        this.intensity.set(intensity);
    }
    
    /**
     * @param amount
     */
    public final void increaseIntensity(final int amount) {

        intensity.increase(amount);
    }
    
    /**
     * @param amount
     */
    public final void decreaseIntensity(final int amount) {

        intensity.decrease(amount);
    }
    
    /**
     * @return Current number of casualties.
     */
    public final int getCasualties() {

        return casualties.get();
    }
    
    /**
     * @param casualties
     */
    public final void setCasualties(final int casualties) {

        this.casualties.set(casualties);
    }
    
    /**
     * @param amount
     */
    public final void increaseCasualties(final int amount) {

        casualties.increase(amount);
    }
    
    /**
     * @param amount
     */
    public final void decreaseCasualties(final int amount) {

        casualties.decrease(amount);
    }
    
    private static final String FIELD_SEPARATOR = " / ";
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return StringUtils.join(new Object[] {position, intensity, casualties}, FIELD_SEPARATOR);
    }
    
    /**
     * @param str
     * @return {@link Fire}
     */
    public static Fire fromString(final String str) {

        final String[] fields = str.split(FIELD_SEPARATOR);
        // TODO checking
        return new Fire(Position.fromString(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
    }
    
}
