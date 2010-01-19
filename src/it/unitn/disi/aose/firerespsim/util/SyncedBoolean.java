package it.unitn.disi.aose.firerespsim.util;

/**
 * Boolean object with thread synchronization.
 * 
 * @author tom
 */
public final class SyncedBoolean {
    
    private boolean value;
    
//    private boolean available = false;
    
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

//        boolean needToWait = false;
//        while (available == false) {
//            needToWait = true;
//            System.err.println(this.getClass().getSimpleName() + " waiting to become available");
//            try {
//                wait();
//            } catch (final InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        if (needToWait) {
//            System.err.println(this.getClass().getSimpleName() + " became available");
//        }
//        available = false;
//        notifyAll();
        return value;
    }
    
    /**
     * @param value
     */
    public synchronized void set(final boolean value) {

//        boolean needToWait = false;
//        while (available == true) {
//            needToWait = true;
//            System.err.println(this.getClass().getSimpleName() + " waiting to become un-available");
//            try {
//                wait();
//            } catch (final InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        if (needToWait) {
//            System.err.println(this.getClass().getSimpleName() + " became un-available");
//        }
        this.value = value;
//        available = true;
//        notifyAll();
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