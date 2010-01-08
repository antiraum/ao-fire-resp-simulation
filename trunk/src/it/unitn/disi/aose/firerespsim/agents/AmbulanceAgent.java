package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Fire;
import it.unitn.disi.aose.firerespsim.model.Position;
import it.unitn.disi.aose.firerespsim.model.Vehicle;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * This agent simulates an ambulance.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class AmbulanceAgent extends VehicleAgent {
    
    private final MessageTemplate pickUpReplyTpl = MessageTemplate.and(
                                                                       MessageTemplate.or(
                                                                                          MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                                                                          MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)),
                                                                       MessageTemplate.MatchOntology(FireAgent.PICK_UP_ONT_TYPE));
    
    /**
     * If currently transporting a casualty.
     */
    private boolean hasCasualty = false;
    /**
     * Position of the fire currently assigned to.
     */
    private Position firePosition;
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtFire()
     */
    @Override
    void arrivedAtFire() {

        firePosition = vehicle.position;
        final ACLMessage pickUpMsg = new ACLMessage(ACLMessage.REQUEST);
        pickUpMsg.setOntology(FireAgent.PICK_UP_ONT_TYPE);
        pickUpMsg.setContent(vehicle.position.toString());
        pickUpMsg.addReceiver(new AID(FireAgent.FIRE_AGENT_NAME_PREFIX + firePosition, false));
        send(pickUpMsg);
        logger.debug("sent pick up casualty request to fire at (" + firePosition + ")");
        
        final ACLMessage replyMsg = blockingReceive(pickUpReplyTpl);
        if (replyMsg.getPerformative() == ACLMessage.CONFIRM) {
            logger.info("picked up casualty from fire at (" + firePosition + "), bringing it back to hospital");
            hasCasualty = true;
            vehicle.setAcceptingTarget(false);
            vehicle.target.set(vehicle.home);
            vehicle.setState(Vehicle.STATE_TO_TARGET);
        } else {
            logger.info("no casualty picked up from fire at (" + firePosition + ")");
        }
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtHome()
     */
    @Override
    void arrivedAtHome() {

        if (hasCasualty) {
            // TODO deliver casualty
            hasCasualty = false;
            vehicle.setAcceptingTarget(true);
            logger.info("delivered casualty");
            if (firePosition != null) {
                logger.info("returning to fire");
                vehicle.target.set(firePosition);
                vehicle.setState(Vehicle.STATE_TO_TARGET);
            }
        } else {
            logger.info("is idle");
            vehicle.setState(Vehicle.STATE_IDLE);
            vehicle.target = null;
        }
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#receivedFireStatus(it.unitn.disi.aose.firerespsim.model.Fire)
     */
    @Override
    void receivedFireStatus(final Fire fire) {

        if (fire.getIntensity() < 1 && fire.getCasualties() < 1) {
            logger.info("fire is put out and all casualties have been picked up");
            firePosition = null;
            if (!vehicle.target.equals(vehicle.home)) {
                logger.info("returning to hospital");
                vehicle.target.set(vehicle.home);
                vehicle.setState(Vehicle.STATE_TO_TARGET);
                sendStatus();
            }
        }
    }
}
