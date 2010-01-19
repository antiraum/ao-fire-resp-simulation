package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Fire;
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
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtHome()
     */
    @Override
    void arrivedAtHome() {

        if (hasCasualty) {
            // TODO deliver casualty
            hasCasualty = false;
            vehicle.setAcceptingTarget(true);
            logger.info("delivered casualty");
            if (vehicle.fire == null) {
                setIdle();
            } else {
                logger.info("returning to fire");
                setTarget(vehicle.fire);
            }
        } else {
            setIdle();
        }
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#arrivedAtFire()
     */
    @Override
    void arrivedAtFire() {

        if (vehicle.fire == null) {
            vehicle.fire = vehicle.position.clone();
            sendStatus();
        } else if (!vehicle.fire.equals(vehicle.position)) {
            vehicle.fire.set(vehicle.position);
            sendStatus();
        }
        
        final ACLMessage pickUpMsg = new ACLMessage(ACLMessage.REQUEST);
        pickUpMsg.setOntology(FireAgent.PICK_UP_ONT_TYPE);
        pickUpMsg.setContent(vehicle.position.toString());
        pickUpMsg.addReceiver(new AID(FireAgent.FIRE_AGENT_NAME_PREFIX + vehicle.fire, false));
        send(pickUpMsg);
//        logger.debug("sent pick up casualty request to fire at (" + vehicle.fire + ")");
        
        final ACLMessage replyMsg = blockingReceive(pickUpReplyTpl);
        
        if (replyMsg.getPerformative() == ACLMessage.CONFIRM) {
            logger.info("picked up casualty from fire at (" + vehicle.fire + "), bringing it back to hospital");
            hasCasualty = true;
            vehicle.setAcceptingTarget(false);
            setTarget(vehicle.home);
        } else {
            logger.info("no casualty picked up from fire at (" + vehicle.fire + ")");
        }
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#continuousAction()
     */
    @Override
    void continuousAction() {

    // nothing
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#receivedFireStatus(it.unitn.disi.aose.firerespsim.model.Fire)
     */
    @Override
    void receivedFireStatus(final Fire fire) {

        if (fire.getIntensity() >= 1 || fire.getCasualties() >= 1) return;
        
        logger.info("fire is put out and all casualties have been picked up - returning to hospital");
        vehicle.fire = null;
        setTarget(vehicle.home);
    }
}
