package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Vehicle;
import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.ontology.PutOutRequest;
import jade.lang.acl.ACLMessage;

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

    // nothing
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#continuousAction()
     */
    @Override
    void continuousAction() {

        if (vehicle.getState() != Vehicle.STATE_AT_TARGET || vehicle.fire == null) return;
        putOutFire();
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#handleFireStatus(it.unitn.disi.aose.firerespsim.ontology.FireStatus)
     */
    @Override
    void handleFireStatus(final FireStatus status) {

        if (status.getIntensity() > 0) return;
        logger.info("fire is put out, returning to fire brigade");
        vehicle.fire = null;
        setTarget(vehicle.home);
    }
    
    /**
     * Sends a put out request to a fire at the current position. Only call this if at the fire. Package scoped for
     * faster access by inner classes.
     */
    void putOutFire() {

        sendMessage(ACLMessage.REQUEST, FireAgent.PUT_OUT_PROTOCOL, getFireAID(),
                    new PutOutRequest(vehicle.position.getCoordinate()));
    }
}
