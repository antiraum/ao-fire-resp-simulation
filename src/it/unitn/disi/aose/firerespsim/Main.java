package it.unitn.disi.aose.firerespsim;

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

/**
 * This class starts the emergency response simulation.
 * 
 * @author tom
 */
public final class Main {
    
    private static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    // JADE Runtime
    private static final String JADE_HOST = "localhost";
    private static final int JADE_PORT = 1199;
    
    // Configuration
    private static final int AREA_WIDTH = 4;
    private static final int AREA_HEIGHT = 4;
    private static final int ENVIRONMENT_SPAWN_FIRE_IVAL = 10000;
    private static final int MONITOR_SCAN_AREA_IVAL = 1000;
    private static final int NUMBER_OF_FIRE_BRIGADES = 2;
    private static final int NUMBER_OF_HOSPITALS = 2;
    private static final int FIRE_INCREASE_IVAL = 10000;
    private static final int VEHICLE_MOVE_IVAL = 10000;
    
    /**
     * @param args
     */
    public static void main(final String[] args) {

        logger.info("starting simulation");
        
        final AgentContainer ac = Runtime.instance().createMainContainer(
                                                                         new ProfileImpl(JADE_HOST, JADE_PORT, null,
                                                                                         false));
        
        // start the environment agent
        try {
            final AgentController env = ac.createNewAgent("environment", EnvironmentAgent.class.getName(),
                                                          new Object[] {
                                                              AREA_WIDTH, AREA_HEIGHT, ENVIRONMENT_SPAWN_FIRE_IVAL,
                                                              FIRE_INCREASE_IVAL});
            env.start();
        } catch (final StaleProxyException e) {
            logger.error("couldn't start the environment agent");
            e.printStackTrace();
            return;
        }
        
        // start the monitor agent
        try {
            final AgentController monitor = ac.createNewAgent("fire monitor", FireMonitorAgent.class.getName(),
                                                              new Object[] {MONITOR_SCAN_AREA_IVAL});
            monitor.start();
        } catch (final StaleProxyException e) {
            logger.error("couldn't start the monitor agent");
            e.printStackTrace();
            return;
        }
        
        // start the fire brigade agents
        for (int i = 1; i <= NUMBER_OF_FIRE_BRIGADES; i++) {
            @SuppressWarnings("unused")
            final int row = RandomUtils.nextInt(AREA_HEIGHT - 1) + 1;
            @SuppressWarnings("unused")
            final int col = RandomUtils.nextInt(AREA_WIDTH - 1) + 1;
            // TODO
        }
        
        // start the hospital agents
        for (int i = 1; i <= NUMBER_OF_HOSPITALS; i++) {
            final int row = RandomUtils.nextInt(AREA_HEIGHT - 1) + 1;
            final int col = RandomUtils.nextInt(AREA_WIDTH - 1) + 1;
            try {
                final AgentController hospital = ac.createNewAgent("hospital " + i, HospitalAgent.class.getName(),
                                                                   new Object[] {i, row, col, VEHICLE_MOVE_IVAL});
                hospital.start();
            } catch (final StaleProxyException e) {
                logger.error("couldn't start the " + i + "st hospital agent");
                e.printStackTrace();
                return;
            }
        }
    }
}
