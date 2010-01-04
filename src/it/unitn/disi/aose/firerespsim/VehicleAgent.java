package it.unitn.disi.aose.firerespsim;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * This is the super class for the fire engine and ambulance agents.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public abstract class VehicleAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Defaults for start-up arguments.
     */
    private static final int DEFAULT_MOVE_IVAL = 10000;
    
    /**
     * Current row on the simulation area. Package scoped for faster access by inner classes.
     */
    int row = 0;
    /**
     * Current column on the simulation area. Package scoped for faster access by inner classes.
     */
    int col = 0;
    /**
     * Move target row on the simulation area. Package scoped for faster access by inner classes.
     */
    int targetRow = 0;
    /**
     * Move target column on the simulation area. Package scoped for faster access by inner classes.
     */
    int targetCol = 0;
    
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
        
        // read start-up arguments
        final Object[] params = getArguments();
        if (params == null || params.length < 2) {
            logger.error("start-up arguments row and column needed");
            doDelete();
            return;
        }
        row = (Integer) params[0];
        col = (Integer) params[1];
        final int vehicleMoveIval = (params.length > 2) ? (Integer) params[2] : DEFAULT_MOVE_IVAL;
        
        // add behaviors
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {new Move(this, vehicleMoveIval)}));
        final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        addBehaviour(pb);
    }
    
    /**
     * @see jade.core.Agent#takeDown()
     */
    @Override
    protected void takeDown() {

        logger.debug("takeDown");
        
        for (final Behaviour b : threadedBehaviours) {
            if (b != null) {
                tbf.getThread(b).interrupt();
            }
        }
        
        super.takeDown();
    }
    
    /**
     * Moves to {@link #targetRow} and {@link #targetCol} if different from {@link #row} and {@link #col}.
     */
    class Move extends TickerBehaviour {
        
        /**
         * @param a
         * @param period
         */
        public Move(final Agent a, final long period) {

            super(a, period);
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            if (row == targetRow && col == targetCol) return;
            if (row != targetRow) {
                row += (row > targetRow) ? -1 : 1;
            }
            if (col != targetCol) {
                col += (col > targetCol) ? -1 : 1;
            }
            if (row == targetRow && col == targetCol) {
                logger.info("arrived at target position (" + row + ", " + col + ")");
            } else {
                logger.debug("moved to (" + row + ", " + col + ")");
            }
        }
        
    }
}
