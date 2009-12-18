package it.unitn.disi.aose.firerespsim;

import jade.core.Agent;
import org.apache.log4j.Logger;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
    }
}
