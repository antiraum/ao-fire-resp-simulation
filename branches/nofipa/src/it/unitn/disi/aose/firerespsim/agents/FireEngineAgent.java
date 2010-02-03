package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Vehicle;
import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.ontology.PutOutRequest;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

/**
 * This agent simulates an fire engine.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireEngineAgent extends VehicleAgent {
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtHome()
     */
    @Override
    void arrivedAtHome() {

        setIdle();
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtFire()
     */
    @Override
    void arrivedAtFire() {

        putOutFire();
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#doMove()
     */
    @Override
    void doMove() {

        if (vehicle.getState() != Vehicle.STATE_AT_TARGET || vehicle.fire == null) return;
        putOutFire();
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#receivedFireStatus(it.unitn.disi.aose.firerespsim.ontology.FireStatus)
     */
    @Override
    void receivedFireStatus(final FireStatus status) {

        if (status.getIntensity() >= 1) return;
        logger.info("fire is put out, returning to fire brigade");
        vehicle.fire = null;
        setTarget(vehicle.home);
    }
    
    /**
     * Starts a {@link PutOutFire} for a fire at the current position. Only call this if at the fire. Package scoped for
     * faster access by inner classes.
     */
    void putOutFire() {

        final ACLMessage putOutMsg = createMessage(ACLMessage.REQUEST, FireAgent.PUT_OUT_PROTOCOL, getFireAID(),
                                                   new PutOutRequest(vehicle.position.getCoordinate()));
        if (putOutFire == null) {
            putOutFire = new PutOutFire(this, putOutMsg);
        } else {
            putOutFire.reset(putOutMsg);
        }
        addParallelBehaviour(putOutFire);
    }
    
    private PutOutFire putOutFire = null;
    
    private class PutOutFire extends AchieveREInitiator {
        
        /**
         * @param a
         * @param msg
         */
        public PutOutFire(final Agent a, final ACLMessage msg) {

            super(a, msg);
        }
    }
}
