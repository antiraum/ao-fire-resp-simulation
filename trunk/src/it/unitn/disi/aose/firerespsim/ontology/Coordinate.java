package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class Coordinate implements Concept {
    
    private int row;
    private int col;
    
    /**
     * Constructor for bean instantiation.
     */
    public Coordinate() {

    // empty
    }
    
    /**
     * @param row
     * @param col
     */
    public Coordinate(final int row, final int col) {

        super();
        this.row = row;
        this.col = col;
    }
    
    /**
     * @return Row on the simulation area.
     */
    @Slot(mandatory = true)
    public int getRow() {

        return row;
    }
    
    /**
     * @param row
     */
    public void setRow(final int row) {

        this.row = row;
    }
    
    /**
     * @return Column on the simulation area.
     */
    @Slot(mandatory = true)
    public int getCol() {

        return col;
    }
    
    /**
     * @param col
     */
    public void setCol(final int col) {

        this.col = col;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return row + " " + col;
    }
    
}
