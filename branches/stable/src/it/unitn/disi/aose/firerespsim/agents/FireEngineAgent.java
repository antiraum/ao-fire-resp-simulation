package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Fire;
import it.unitn.disi.aose.firerespsim.model.Vehicle;
import jade.core.AID;
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

        sendPutOutRequest();
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#continuousAction()
     */
    @Override
    void continuousAction() {

        if (vehicle.getState() != Vehicle.STATE_AT_TARGET || vehicle.fire == null) return;
        sendPutOutRequest();
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#receivedFireStatus(it.unitn.disi.aose.firerespsim.model.Fire)
     */
    @Override
    void receivedFireStatus(final Fire fire) {

        if (fire.getIntensity() >= 1) return;
        logger.info("fire is put out, returning to fire brigade");
        vehicle.fire = null;
        setTarget(vehicle.home);
    }
    
    /**
     * Sends a put out request to a fire at the current position. Only call this if at the fire. Package scoped for
     * faster access by inner classes.
     */
    void sendPutOutRequest() {

        final ACLMessage putOutMsg = new ACLMessage(ACLMessage.REQUEST);
        putOutMsg.setOntology(FireAgent.PUT_OUT_ONT_TYPE);
        putOutMsg.setContent(vehicle.position.toString());
        putOutMsg.addReceiver(new AID(FireAgent.FIRE_AGENT_NAME_PREFIX + vehicle.position, false));
        send(putOutMsg);
//        logger.debug("sent put out request to fire at (" + vehicle.position + ")");
    }
}
