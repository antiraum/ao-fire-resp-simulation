package it.unitn.disi.aose.firerespsim.util;

/**
 * Integer object with thread synchronization.
 * 
 * @author tom
 */
public final class SyncedInteger {
    
    private int value = 0;
    
    /**
     * @param value Initial value
     */
    public SyncedInteger(final int value) {

        super();
        
        set(value);
    }
    
    /**
     * @return value
     */
    public synchronized int get() {

        return value;
    }
    
    /**
     * @param value
     */
    public synchronized void set(final int value) {

        this.value = value;
    }
    
    /**
     * @param amount
     */
    public synchronized void increase(final int amount) {

        value += amount;
    }
    
    /**
     * @param amount
     */
    public synchronized void decrease(final int amount) {

        value -= amount;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return Integer.toString(get());
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {

        final SyncedInteger other = (SyncedInteger) obj;
        return (other.get() == get()) ? true : false;
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        return super.hashCode();
    }
}