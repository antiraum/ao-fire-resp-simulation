package it.unitn.disi.aose.firerespsim;

import it.unitn.disi.aose.firerespsim.agents.EnvironmentAgent;
import it.unitn.disi.aose.firerespsim.agents.FireBrigadeAgent;
import it.unitn.disi.aose.firerespsim.agents.FireBrigadeCoordinatorAgent;
import it.unitn.disi.aose.firerespsim.agents.FireMonitorAgent;
import it.unitn.disi.aose.firerespsim.agents.HospitalAgent;
import it.unitn.disi.aose.firerespsim.agents.HospitalCoordinatorAgent;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

/**
 * This class launches the JADE runtime and starts the emergency response simulation.
 * 
 * @author tom
 */
public final class Main {
    
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
    
    private static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    private final static AgentContainer ac = Runtime.instance().createMainContainer(
                                                                                    new ProfileImpl(JADE_HOST,
                                                                                                    JADE_PORT, null,
                                                                                                    false));
    
    /**
     * @param args
     */
    public static void main(final String[] args) {

        logger.info("starting simulation");
        
        // start the environment agent
        startAgent("environment", EnvironmentAgent.class.getName(), new Object[] {
            AREA_WIDTH, AREA_HEIGHT, ENVIRONMENT_SPAWN_FIRE_IVAL, FIRE_INCREASE_IVAL});
        
        // start the monitor agent
        startAgent("fire monitor", FireMonitorAgent.class.getName(), new Object[] {MONITOR_SCAN_AREA_IVAL});
        
        // start the fire brigade coordinator
        startAgent("fire brigade coordinator", FireBrigadeCoordinatorAgent.class.getName(), null);
        
        // start the fire brigade agents
        for (int i = 1; i <= NUMBER_OF_FIRE_BRIGADES; i++) {
            final int row = RandomUtils.nextInt(AREA_HEIGHT - 1) + 1;
            final int col = RandomUtils.nextInt(AREA_WIDTH - 1) + 1;
            startAgent("fire brigade " + i, FireBrigadeAgent.class.getName(), new Object[] {
                "fb" + i, row, col, VEHICLE_MOVE_IVAL});
        }
        
        // start the hospital coordinator
        startAgent("hospital coordinator", HospitalCoordinatorAgent.class.getName(), null);
        
        // start the hospital agents
        for (int i = 1; i <= NUMBER_OF_HOSPITALS; i++) {
            final int row = RandomUtils.nextInt(AREA_HEIGHT - 1) + 1;
            final int col = RandomUtils.nextInt(AREA_WIDTH - 1) + 1;
            startAgent("hospital " + i, HospitalAgent.class.getName(), new Object[] {
                "h" + i, row, col, VEHICLE_MOVE_IVAL});
        }
    }
    
    private static void startAgent(final String nickname, final String className, final Object[] args) {

        try {
            final AgentController a = ac.createNewAgent(nickname, className, args);
            a.start();
        } catch (final StaleProxyException e) {
            logger.error("couldn't start the " + nickname + " agent");
            e.printStackTrace();
            return;
        }
    }
}
