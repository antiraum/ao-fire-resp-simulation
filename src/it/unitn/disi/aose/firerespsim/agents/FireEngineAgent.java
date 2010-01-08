package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Fire;
import it.unitn.disi.aose.firerespsim.model.Vehicle;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**
 * This agent simulates an fire engine.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireEngineAgent extends VehicleAgent {
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtFire()
     */
    @Override
    void arrivedAtFire() {

        final ACLMessage putOutMsg = new ACLMessage(ACLMessage.REQUEST);
        putOutMsg.setOntology(FireAgent.PUT_OUT_ONT_TYPE);
        putOutMsg.setContent(vehicle.position.toString());
        putOutMsg.addReceiver(new AID(FireAgent.FIRE_AGENT_NAME_PREFIX + vehicle.position, false));
        send(putOutMsg);
        logger.debug("sent put out request to fire at (" + vehicle.position + ")");
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtHome()
     */
    @Override
    void arrivedAtHome() {

        logger.info("is idle");
        vehicle.setState(Vehicle.STATE_IDLE);
        vehicle.target = null;
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#receivedFireStatus(it.unitn.disi.aose.firerespsim.model.Fire)
     */
    @Override
    void receivedFireStatus(final Fire fire) {

        if (fire.getIntensity() < 1) {
            logger.info("fire is put out, returning to fire brigade");
            vehicle.target.set(vehicle.home);
            vehicle.setState(Vehicle.STATE_TO_TARGET);
            sendStatus();
        }
    }
}
