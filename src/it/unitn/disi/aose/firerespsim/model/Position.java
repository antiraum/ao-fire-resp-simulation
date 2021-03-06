package it.unitn.disi.aose.firerespsim.model;

import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.util.SyncedInteger;

/**
 * Model of a position on the simulation area. Thread-safe.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
public final class Position {
    
    /**
     * Position on the simulation area.
     */
    private final SyncedInteger row;
    /**
     * Column on the simulation area.
     */
    private final SyncedInteger col;
    
    /**
     * @param row
     * @param col
     */
    public Position(final int row, final int col) {

        this.row = new SyncedInteger(row);
        this.col = new SyncedInteger(col);
    }
    
    /**
     * @param coordinate
     */
    public Position(final Coordinate coordinate) {

        this(coordinate.getRow(), coordinate.getCol());
    }
    
    /**
     * @param position
     */
    public void set(final Position position) {

        setRow(position.getRow());
        setCol(position.getCol());
    }
    
    /**
     * @return Position row
     */
    public final int getRow() {

        return row.get();
    }
    
    /**
     * @param row
     */
    public final void setRow(final int row) {

        this.row.set(row);
    }
    
    /**
     * @param amount
     */
    public final void increaseRow(final int amount) {

        row.increase(amount);
    }
    
    /**
     * @param amount
     */
    public final void decreaseRow(final int amount) {

        row.decrease(amount);
    }
    
    /**
     * @return Position column
     */
    public final int getCol() {

        return col.get();
    }
    
    /**
     * @param col
     */
    public final void setCol(final int col) {

        this.col.set(col);
    }
    
    /**
     * @param amount
     */
    public final void increaseCol(final int amount) {

        col.increase(amount);
    }
    
    /**
     * @param amount
     */
    public final void decreaseCol(final int amount) {

        col.decrease(amount);
    }
    
    /**
     * @return {@link Coordinate} with the current position.
     */
    public Coordinate getCoordinate() {

        return new Coordinate(getRow(), getCol());
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return row + " " + col;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public Position clone() {

        return new Position(getRow(), getCol());
    }
}
