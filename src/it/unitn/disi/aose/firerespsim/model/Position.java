package it.unitn.disi.aose.firerespsim.model;

import it.unitn.disi.aose.firerespsim.util.SyncedInteger;

/**
 * Representation of a position on the simulation area. Thread-safe.
 * 
 * @author tom
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
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return row + " " + col;
    }
    
    /**
     * @param str
     * @return {@link Position}
     */
    public static Position fromString(final String str) {

        final String[] fields = str.split(" ");
        // TODO checking
        return new Position(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]));
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {

        final Position other = (Position) obj;
        return (other.row.equals(row) && other.col.equals(col)) ? true : false;
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        return super.hashCode();
    }
}
