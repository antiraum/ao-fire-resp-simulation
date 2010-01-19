package it.unitn.disi.aose.firerespsim.util;

/**
 * Boolean object with thread synchronization.
 * 
 * @author tom
 */
public final class SyncedBoolean {
    
    private boolean value;
    
    /**
     * @param value Initial value
     */
    public SyncedBoolean(final boolean value) {

        super();
        
        set(value);
    }
    
    /**
     * @return value
     */
    public synchronized boolean get() {

        return value;
    }
    
    /**
     * @param value
     */
    public synchronized void set(final boolean value) {

        this.value = value;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return Boolean.toString(get());
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {

        final SyncedBoolean other = (SyncedBoolean) obj;
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