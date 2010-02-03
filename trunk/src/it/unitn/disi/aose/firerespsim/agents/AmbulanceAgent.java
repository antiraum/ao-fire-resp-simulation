package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.ontology.PickUpCasualtyRequest;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import java.util.ListIterator;
import java.util.Vector;

/**
 * This agent simulates an ambulance.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class AmbulanceAgent extends VehicleAgent {
    
    /**
     * If currently transporting a casualty. Package scoped for faster access by inner classes.
     */
    boolean hasCasualty = false;
    
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
        
        final ACLMessage pickUpMsg = createMessage(ACLMessage.REQUEST, FireAgent.PICK_UP_CASUALTY_PROTOCOL,
                                                   getFireAID(),
                                                   new PickUpCasualtyRequest(vehicle.position.getCoordinate()));
        if (pickUpCasualty == null) {
            pickUpCasualty = new PickUpCasualty(this, pickUpMsg);
        } else {
            pickUpCasualty.reset(pickUpMsg);
        }
        addParallelBehaviour(pickUpCasualty);
    }
    
    private PickUpCasualty pickUpCasualty = null;
    
    private class PickUpCasualty extends AchieveREInitiator {
        
        /**
         * @param a
         * @param msg
         */
        public PickUpCasualty(final Agent a, final ACLMessage msg) {

            super(a, msg);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        protected void handleAllResponses(final Vector responses) {

            final ListIterator iter = responses.listIterator();
            while (iter.hasNext()) {
                final ACLMessage response = (ACLMessage) iter.next();
                if (response.getPerformative() == ACLMessage.AGREE) {
                    logger.info("picked up casualty from fire at (" + vehicle.fire + "), bringing it back to hospital");
                    hasCasualty = true;
                    vehicle.setAcceptingTarget(false);
                    setTarget(vehicle.home);
                } else {
                    logger.info("no casualty picked up from fire at (" + vehicle.fire + ")");
                }
            }
        }
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#doMove()
     */
    @Override
    void doMove() {

    // nothing
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#receivedFireStatus(it.unitn.disi.aose.firerespsim.ontology.FireStatus)
     */
    @Override
    void receivedFireStatus(final FireStatus fire) {

        if (fire.getIntensity() >= 1 || fire.getCasualties() >= 1) return;
        
        logger.info("fire is put out and all casualties have been picked up - returning to hospital");
        vehicle.fire = null;
        setTarget(vehicle.home);
    }
}
