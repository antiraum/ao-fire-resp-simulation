package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Concept;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireStatus implements Concept {
    
    private Coordinate coordinate;
    private int intensity;
    private int casualties;
    
    /**
     * Constructor for bean instantiation.
     */
    public FireStatus() {

    // empty
    }
    
    /**
     * @param coordinate
     * @param intensity
     * @param casualties
     */
    public FireStatus(final Coordinate coordinate, final int intensity, final int casualties) {

        this.coordinate = coordinate;
        this.intensity = intensity;
        this.casualties = casualties;
    }
    
    /**
     * @return Fire position.
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
     * @return fire intensity
     */
    public int getIntensity() {

        return intensity;
    }
    
    /**
     * @param intensity
     */
    public void setIntensity(final int intensity) {

        this.intensity = intensity;
    }
    
    /**
     * @return number of casualties
     */
    public int getCasualties() {

        return casualties;
    }
    
    /**
     * @param casualties
     */
    public void setCasualties(final int casualties) {

        this.casualties = casualties;
    }
}
