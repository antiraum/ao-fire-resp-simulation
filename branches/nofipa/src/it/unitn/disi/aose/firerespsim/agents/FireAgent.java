package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Fire;
import it.unitn.disi.aose.firerespsim.model.SimulationArea;
import it.unitn.disi.aose.firerespsim.ontology.Coordinate;
import it.unitn.disi.aose.firerespsim.ontology.FireStatusInfo;
import it.unitn.disi.aose.firerespsim.ontology.PickUpCasualtyRequest;
import it.unitn.disi.aose.firerespsim.ontology.PutOutRequest;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.apache.commons.lang.math.RandomUtils;

/**
 * This agent represents a fire. It's intensity increases in intervals. Mobile agents (fire engines and hospitals) next
 * to it can read it's current status (intensity and number of casualties). Fire engines next to it can decrease its
 * intensity. Start-up parameters are the row, column, and the increase interval.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireAgent extends ExtendedAgent {
    
    /**
     * Prefix for the local name of fire agents.
     */
    static final String FIRE_AGENT_NAME_PREFIX = "fire ";
    /**
     * Protocol for fire status messages.
     */
    final static String FIRE_STATUS_PROTOCOL = "FireStatus";
    /**
     * Protocol for put out messages.
     */
    final static String PUT_OUT_PROTOCOL = "PutOut";
    /**
     * Protocol for pick up casualty messages.
     */
    final static String PICK_UP_CASUALTY_PROTOCOL = "PickUpCasualty";
    
    /**
     * Model of the fire. Package scoped for faster access by inner classes.
     */
    Fire fire;
    /**
     * Instance of the {@link Increase} behavior. Package scoped for faster access by inner classes.
     */
    Increase increaseBehaviour;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        params = new LinkedHashMap<String, Object>() {
            
            {
                put("ROW", null);
                put("COLUMN", null);
                put("INCREASE_IVAL", null);
            }
        };
        
        super.setup();
        
        fire = new Fire(new Coordinate((Integer) params.get("ROW"), (Integer) params.get("COLUMN")), 1, 1);
        final int intensityInc = RandomUtils.nextInt(4) + 1; // intensity increase per {@link Increase#onTick()} (from 1 to 5)
        final int casualtiesInc = RandomUtils.nextInt(1) + 1; // casualties increase per {@link Increase#onTick()} (from 1 to 2)
        
        // create behaviors
        final MessageTemplate putOutTpl = createMessageTemplate(null, PUT_OUT_PROTOCOL, ACLMessage.REQUEST);
        final PutOutService putOutService = new PutOutService(this, putOutTpl);
        final MessageTemplate pickUpTpl = createMessageTemplate(null, PICK_UP_CASUALTY_PROTOCOL, ACLMessage.REQUEST);
        final PickUpCasualtyService pickUpService = new PickUpCasualtyService(this, pickUpTpl);
        increaseBehaviour = new Increase(this, (Integer) params.get("INCREASE_IVAL"), intensityInc, casualtiesInc);
        
        // add behaviors
        parallelBehaviours.addAll(Arrays.asList(putOutService, pickUpService, increaseBehaviour));
        addBehaviours();
    }
    
    /**
     * Service for fire engines to reduce the fire intensity. If the intensity reaches 0 the fire is put out and stops
     * increasing. If also all casualties have been picked up the agent deletes itself. The fire engine must be at the
     * fires position to be able to decrease its intensity. If it is a message with the new fire status (
     * {@link Fire#toString()}) is send to the engine agent.
     */
    private class PutOutService extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public PutOutService(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage request = blockingReceive(mt);
            if (request == null) return;
            
            logger.debug("received put out request");
            
            Coordinate engineCoord;
            try {
                engineCoord = extractMessageContent(PutOutRequest.class, request, false).getFireEnginePosition();
            } catch (final Exception e) {
                sendReply(request, ACLMessage.NOT_UNDERSTOOD, "could not read message content");
                return;
            }
            String refuse = null;
            if (!SimulationArea.coordinatesEqual(engineCoord, fire.coordinate)) {
                refuse = "fire engine is too far away";
                logger.debug("fire (" + fire.coordinate + "), engine (" + engineCoord + ")");
            }
            if (fire.getIntensity() < 1) {
                refuse = "fire is already put out";
            }
            if (refuse != null) {
                logger.debug(refuse);
                sendReply(request, ACLMessage.REFUSE, refuse);
                return;
            }
            sendReply(request, ACLMessage.AGREE);
            
            boolean takeDown = false;
            fire.decreaseIntensity(1);
            if (fire.getIntensity() < 1) {
                // fire is put out
                stopParallelBehaviour(increaseBehaviour);
                fire.setIntensity(0);
                if (fire.getCasualties() < 1) {
                    fire.setCasualties(0);
                    takeDown = true;
                }
            }
            logger.info("new intensity: " + fire.getIntensity());
            sendStatus(request.getSender());
            
            if (takeDown) {
                doDelete();
            }
        }
    }
    
    /**
     * Service for ambulances to pick up a casualty. The ambulance must be at the fires position to be able to pick up a
     * casualty. The reply message confirms or disconfirms if a casualty is picked up. If the ambulance is at the fires
     * position, a message with the new fire status ({@link Fire#toString()}) is send to the ambulance agent.
     */
    private class PickUpCasualtyService extends CyclicBehaviour {
        
        private final MessageTemplate mt;
        
        /**
         * @param a
         * @param mt
         */
        public PickUpCasualtyService(final Agent a, final MessageTemplate mt) {

            super(a);
            this.mt = mt;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage request = blockingReceive(mt);
            if (request == null) return;
            
            logger.debug("received pick up casualty request");
            
            Coordinate ambulanceCoord;
            try {
                ambulanceCoord = extractMessageContent(PickUpCasualtyRequest.class, request, false).getAmbulancePosition();
            } catch (final Exception e) {
                sendReply(request, ACLMessage.NOT_UNDERSTOOD, "could not read message content");
                return;
            }
            String refuse = null;
            if (!SimulationArea.coordinatesEqual(ambulanceCoord, fire.coordinate)) {
                refuse = "ambulance is too far away";
            }
            if (fire.getCasualties() < 1) {
                refuse = "no casualty to pick up";
            }
            if (refuse != null) {
                logger.debug(refuse);
                sendReply(request, ACLMessage.REFUSE);
                return;
            }
            sendReply(request, ACLMessage.AGREE);
            
            boolean takeDown = false;
            fire.decreaseCasualties(1);
            if (fire.getCasualties() < 1 && fire.getIntensity() < 1) {
                fire.setIntensity(0);
                fire.setCasualties(0);
                takeDown = true;
            }
            logger.info("new casualty count: " + fire.getCasualties());
            sendStatus(request.getSender());
            
            if (takeDown) {
                doDelete();
            }
        }
    }
    
    /**
     * Sends the current status to a vehicle agent. Call this whenever the status changes as a result of a vehicle agent
     * interaction.
     * 
     * @param aid
     */
    void sendStatus(final AID aid) {

        sendMessage(ACLMessage.INFORM, FIRE_STATUS_PROTOCOL, aid, new FireStatusInfo(fire.getFireStatus()));
    }
    
    /**
     * Increases the fire intensity by {@link #intensityIncrease} and the casualties by {@link #casualtiesIncrease}.
     */
    private class Increase extends TickerBehaviour {
        
        private final int intensityIncrease;
        private final int casualtiesIncrease;
        
        /**
         * @param a
         * @param period
         * @param intensityIncrease
         * @param casualtiesIncrease
         */
        public Increase(final Agent a, final long period, final int intensityIncrease, final int casualtiesIncrease) {

            super(a, period);
            this.intensityIncrease = intensityIncrease;
            this.casualtiesIncrease = casualtiesIncrease;
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            fire.increaseIntensity(intensityIncrease);
            fire.increaseCasualties(casualtiesIncrease);
            logger.info("new intensity: " + fire.getIntensity() + ", casualties: " + fire.getCasualties());
        }
    }
}
