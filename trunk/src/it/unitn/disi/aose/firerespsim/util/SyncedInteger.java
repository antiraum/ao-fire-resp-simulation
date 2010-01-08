package it.unitn.disi.aose.firerespsim.util;

/**
 * Integer object with thread synchronization.
 * 
 * @author tom
 */
public final class SyncedInteger {
    
    private int value = 0;
    private boolean available = false;
    
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

        while (available == false) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // pass
            }
        }
        available = false;
        notifyAll();
        return value;
    }
    
    /**
     * @param value
     */
    public synchronized void set(final int value) {

        while (available == true) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // pass
            }
        }
        this.value = value;
        available = true;
        notifyAll();
    }
    
    /**
     * @param amount
     */
    public synchronized void increase(final int amount) {

        while (available == true) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // pass
            }
        }
        value += amount;
        available = true;
        notifyAll();
    }
    
    /**
     * @param amount
     */
    public synchronized void decrease(final int amount) {

        while (available == true) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // pass
            }
        }
        value -= amount;
        available = true;
        notifyAll();
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return Integer.toString(value);
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