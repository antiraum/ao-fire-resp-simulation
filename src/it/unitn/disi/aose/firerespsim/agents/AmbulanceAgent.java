package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.ontology.FireStatus;
import it.unitn.disi.aose.firerespsim.ontology.PickUpCasualtyRequest;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
        
        final MessageTemplate pickUpResponseTpl = createMessageTemplate(null, REQUEST_RESPONSE_PERFORMATIVES,
                                                                        FireAgent.PICK_UP_CASUALTY_PROTOCOL);
        addParallelBehaviour(new HandlePickUpCasualtyResponse(this, pickUpResponseTpl));
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
        
        sendMessage(ACLMessage.REQUEST, FireAgent.PICK_UP_CASUALTY_PROTOCOL, getFireAID(),
                    new PickUpCasualtyRequest(vehicle.position.getCoordinate()));
    }
    
    private class HandlePickUpCasualtyResponse extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public HandlePickUpCasualtyResponse(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage response = blockingReceive(mt);
            if (response == null) return;
            
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
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#continuousAction()
     */
    @Override
    void continuousAction() {

    // nothing
    }
    
    /**
     * @see it.unitn.disi.aose.firerespsim.agents.VehicleAgent#handleFireStatus(it.unitn.disi.aose.firerespsim.ontology.FireStatus)
     */
    @Override
    void handleFireStatus(final FireStatus fire) {

        if (fire.getIntensity() >= 1 || fire.getCasualties() >= 1) return;
        
        logger.info("fire is put out and all casualties have been picked up - returning to hospital");
        vehicle.fire = null;
        setTarget(vehicle.home);
    }
}
