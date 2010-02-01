package it.unitn.disi.aose.firerespsim.model;

import it.unitn.disi.aose.firerespsim.agents.VehicleAgent;
import it.unitn.disi.aose.firerespsim.ontology.AreaDimensions;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;

/**
 * Represents the simulation area.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
public final class SimulationArea {
    
    /**
     * Dimensions of the simulation area.
     */
    public AreaDimensions dimensions;
    /**
     * On fire status of the positions on the area. <code>true</code> stands for on fire, <code>false</code> for not on
     * fire.
     */
    private final boolean[][] onFireStates;
    
    /**
     * @param dimensions
     */
    public SimulationArea(final AreaDimensions dimensions) {

        this.dimensions = dimensions;
        
        // initialize on fire statuses
        onFireStates = new boolean[dimensions.getHeight()][dimensions.getWidth()];
        for (int row = 0; row < dimensions.getHeight(); row++) {
            for (int col = 0; col < dimensions.getWidth(); col++) {
                onFireStates[row][col] = false;
            }
        }
    }
    
    /**
     * @param coordinate
     * @return <code>true</code> if on fire, <code>false</code> if not
     */
    public boolean getOnFireState(final Coordinate coordinate) {

        return onFireStates[coordinate.getRow() - 1][coordinate.getCol() - 1];
    }
    
    /**
     * @param coordinate
     * @param state
     */
    public void setOnFireState(final Coordinate coordinate, final boolean state) {

        onFireStates[coordinate.getRow() - 1][coordinate.getCol() - 1] = state;
    }
    
    /**
     * Calculates the distance of two positions on the simulation area in number of moves necessary for a
     * {@link VehicleAgent} to cross.
     * 
     * @param position1
     * @param position2
     * @return Number of moves to cross.
     */
    public static final int getDistance(final Position position1, final Position position2) {

        return getDistance(position1.getRow(), position1.getCol(), position2.getRow(), position2.getCol());
    }
    
    /**
     * Calculates the distance of two points on the simulation area in number of moves necessary for a
     * {@link VehicleAgent} to cross.
     * 
     * @param row1
     * @param col1
     * @param row2
     * @param col2
     * @return Number of moves to cross.
     */
    public static final int getDistance(final int row1, final int col1, final int row2, final int col2) {

        return Math.max(Math.abs(Math.abs(row1) - Math.abs(row2)), Math.abs(Math.abs(col1) - Math.abs(col2)));
    }
    
    public static final int getDistance(final Coordinate position1, final Coordinate position2) {

        return getDistance(position1.getRow(), position1.getCol(), position2.getRow(), position2.getCol());
    }
    
    public static final int getDistance(final Position position1, final Coordinate position2) {

        return getDistance(position1.getRow(), position1.getCol(), position2.getRow(), position2.getCol());
    }
    
    public static final int getDistance(final Coordinate position1, final Position position2) {

        return getDistance(position1.getRow(), position1.getCol(), position2.getRow(), position2.getCol());
    }
}
