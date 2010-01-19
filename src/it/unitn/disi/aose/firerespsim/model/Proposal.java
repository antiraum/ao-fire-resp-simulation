package it.unitn.disi.aose.firerespsim.model;

import org.apache.commons.lang.StringUtils;

/**
 * Proposal of a stationary agent to deal with a fire.
 * 
 * @author tom
 */
public final class Proposal {
    
    /**
     * Position of the fire the proposal is for.
     */
    public Position firePosition;
    /**
     * Name (GUID) of the proposing stationary agent.
     */
    public String agentName;
    /**
     * Distance in rounds from the fire.
     */
    public int distance;
    /**
     * Number of available vehicle agents.
     */
    public int numVehicles;
    
    /**
     * @param firePosition
     * @param agentName
     * @param distance
     * @param numVehicles
     */
    public Proposal(final Position firePosition, final String agentName, final int distance, final int numVehicles) {

        this.firePosition = firePosition;
        this.agentName = agentName;
        this.distance = distance;
        this.numVehicles = numVehicles;
    }
    
    /**
     * @param firePosition
     * @param agentName
     * @param stationaryAgentPosition
     * @param numVehicles
     */
    public Proposal(final Position firePosition, final String agentName, final Position stationaryAgentPosition,
                    final int numVehicles) {

        this.firePosition = firePosition;
        this.agentName = agentName;
        distance = SimulationArea.getDistance(stationaryAgentPosition, firePosition);
        this.numVehicles = numVehicles;
    }
    
    private static final String FIELD_SEPARATOR = " / ";
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return StringUtils.join(new Object[] {firePosition, agentName, distance, numVehicles}, FIELD_SEPARATOR);
    }
    
    /**
     * @param str
     * @return {@link Proposal}
     */
    public static Proposal fromString(final String str) {

        final String[] fields = str.split(FIELD_SEPARATOR);
        if (fields.length != 4) return null;
        return new Proposal(Position.fromString(fields[0]), fields[1], Integer.parseInt(fields[2]),
                            Integer.parseInt(fields[3]));
    }
}
