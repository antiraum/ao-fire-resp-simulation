package it.unitn.disi.aose.firerespsim.model;

import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.util.SyncedInteger;

/**
 * Model of a fire. Thread-safe.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
public final class Fire {
    
    /**
     * Fire position.
     */
    public final Coordinate coordinate;
    /**
     * Current fire intensity.
     */
    private final SyncedInteger intensity;
    /**
     * Current number of casualties at the fire.
     */
    private final SyncedInteger casualties;
    
    /**
     * @param coordinate
     * @param intensity
     * @param casualties
     */
    public Fire(final Coordinate coordinate, final int intensity, final int casualties) {

        this.coordinate = coordinate.clone();
        this.intensity = new SyncedInteger(intensity);
        this.casualties = new SyncedInteger(casualties);
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
    
    /**
     * @return {@link FireStatus}
     */
    public FireStatus getFireStatus() {

        return new FireStatus(coordinate, intensity.get(), casualties.get());
    }
}
