package it.unitn.disi.aose.firerespsim;

import it.unitn.disi.aose.firerespsim.agents.EnvironmentAgent;
import it.unitn.disi.aose.firerespsim.agents.FireBrigadeAgent;
import it.unitn.disi.aose.firerespsim.agents.FireBrigadeCoordinatorAgent;
import it.unitn.disi.aose.firerespsim.agents.FireMonitorAgent;
import it.unitn.disi.aose.firerespsim.agents.HospitalAgent;
import it.unitn.disi.aose.firerespsim.agents.HospitalCoordinatorAgent;
import it.unitn.disi.aose.firerespsim.model.Position;
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
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
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
    private static final int NUMBER_OF_FIRE_BRIGADES = 1;
    private static final int NUMBER_OF_HOSPITALS = 1;
    private static final int FIRE_INCREASE_IVAL = 10000;
    private static final int VEHICLE_MOVE_IVAL = 1000;
    
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
        logger.info("started environment");
        
        // start the monitor agent
        startAgent("fire monitor", FireMonitorAgent.class.getName(), new Object[] {MONITOR_SCAN_AREA_IVAL});
        logger.info("started fire monitor");
        
        // start the fire brigade coordinator
        startAgent("fire brigade coordinator", FireBrigadeCoordinatorAgent.class.getName(), null);
        logger.info("started fire brigade coordinator");
        
        // start the fire brigade agents
        startStationaryAgents(FireBrigadeAgent.class.getName(), NUMBER_OF_FIRE_BRIGADES, "fire brigade", "fb");
        
        // start the hospital coordinator
        startAgent("hospital coordinator", HospitalCoordinatorAgent.class.getName(), null);
        logger.info("started hospital coordinator");
        
        // start the hospital agents
        startStationaryAgents(HospitalAgent.class.getName(), NUMBER_OF_HOSPITALS, "hospital", "h");
    }
    
    private static void startStationaryAgents(final String className, final int num, final String name,
                                              final String shortName) {

        for (int i = 1; i <= num; i++) {
            final String id = name + " " + i;
            final Position position = new Position(RandomUtils.nextInt(AREA_HEIGHT - 1) + 1,
                                                   RandomUtils.nextInt(AREA_WIDTH - 1) + 1);
            startAgent(id, className, new Object[] {
                shortName + i, position.getRow(), position.getCol(), VEHICLE_MOVE_IVAL});
            logger.debug("started '" + id + "' at (" + position + ")");
        }
        logger.info("started " + NUMBER_OF_FIRE_BRIGADES + " " + name + "s");
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
