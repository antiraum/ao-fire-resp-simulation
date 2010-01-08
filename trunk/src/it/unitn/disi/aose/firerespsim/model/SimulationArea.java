package it.unitn.disi.aose.firerespsim.model;

/**
 * Represents the simulation area.
 * 
 * @author tom
 */
public final class SimulationArea {
    
    /**
     * Width of the simulation area.
     */
    public int width;
    /**
     * Height of the simulation area.
     */
    public int height;
    /**
     * On fire status of the positions on the area. <code>true</code> stands for on fire, <code>false</code> for not on
     * fire.
     */
    private final boolean[][] onFireStates;
    
    /**
     * @param width
     * @param height
     */
    public SimulationArea(final int width, final int height) {

        this.width = width;
        this.height = height;
        
        // initialize on fire statuses
        onFireStates = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                onFireStates[row][col] = false;
            }
        }
    }
    
    /**
     * @param position
     * @return <code>true</code> if on fire, <code>false</code> if not
     */
    public boolean getOnFireState(final Position position) {

        return onFireStates[position.getRow() - 1][position.getCol() - 1];
    }
    
    /**
     * @param position
     * @param state
     */
    public void setOnFireState(final Position position, final boolean state) {

        onFireStates[position.getRow() - 1][position.getCol() - 1] = state;
    }
    
    /**
     * Separator used by {@link #toString()}.
     */
    public static final String FIELD_SEPARATOR = " ";
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return width + FIELD_SEPARATOR + height;
    }
}
