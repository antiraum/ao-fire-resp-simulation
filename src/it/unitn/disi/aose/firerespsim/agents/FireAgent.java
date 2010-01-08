package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.model.Fire;
import it.unitn.disi.aose.firerespsim.model.Position;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

/**
 * This agent represents a fire. It's intensity increases in intervals. Mobile agents (fire engines and hospitals) next
 * to it can read it's current status (intensity and number of casualties). Fire engines next to it can decrease its
 * intensity. Start-up parameters are the row, column, and the increase interval.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireAgent extends Agent {
    
    /**
     * Prefix for the local name of fire agents.
     */
    static final String FIRE_AGENT_NAME_PREFIX = "fire ";
    /**
     * Ontology type for fire status messages. Package scoped for faster access by inner classes.
     */
    final static String FIRE_STATUS_ONT_TYPE = "FireStatus";
    /**
     * Ontology type for put out messages. Package scoped for faster access by inner classes.
     */
    final static String PUT_OUT_ONT_TYPE = "PutOut";
    /**
     * Ontology type for pick up casualty messages. Package scoped for faster access by inner classes.
     */
    final static String PICK_UP_ONT_TYPE = "PickUpCasualty";
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Status of the fire. Package scoped for faster access by inner classes.
     */
    Fire fire;
    /**
     * Intensity increase per {@link Increase#onTick()}. From 1 to 10. Package scoped for faster access by inner
     * classes.
     */
    int intensityIncrease = 0;
    /**
     * Casualties increase per {@link Increase#onTick()}. From 1 to 3. Package scoped for faster access by inner
     * classes.
     */
    int casualtiesIncrease = 0;
    
    /**
     * Package scoped for faster access by inner classes.
     */
    final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    /**
     * Set of the threaded parallel behaviors. Package scoped for faster access by inner classes.
     */
    final Set<Behaviour> threadedBehaviours = new HashSet<Behaviour>();
    /**
     * Behavior containing all the {@link #threadedBehaviours}. Package scoped for faster access by inner classes.
     */
    final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
    /**
     * Instance of the {@link Increase} behavior. Package scoped for faster access by inner classes.
     */
    Increase increaseBehaviour;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        logger.debug("starting up");
        
        super.setup();
        
        // read start-up arguments
        final Object[] params = getArguments();
        if (params == null || params.length < 3) {
            logger.error("start-up arguments row, column, and increase ival needed");
            doDelete();
            return;
        }
        fire = new Fire(new Position((Integer) params[0], (Integer) params[1]), 0, 0);
        final int increaseIval = (Integer) params[2];
        
        // randomized initialization value
        intensityIncrease = RandomUtils.nextInt(9) + 1;
        casualtiesIncrease = RandomUtils.nextInt(2) + 1;
        
        // add behaviors
        increaseBehaviour = new Increase(this, increaseIval);
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new PutOutService(), new PickUpCasualtyService(), increaseBehaviour}));
        for (final Behaviour b : threadedBehaviours) {
            pb.addSubBehaviour(tbf.wrap(b));
        }
        addBehaviour(pb);
    }
    
    /**
     * @see jade.core.Agent#takeDown()
     */
    @Override
    protected void takeDown() {

        logger.info("shutting down");
        
        for (final Behaviour b : threadedBehaviours) {
            if (b != null) {
                tbf.getThread(b).interrupt();
            }
        }
        
        super.takeDown();
    }
    
    /**
     * Service for fire engines to reduce the fire intensity. If the intensity reaches 0 the fire is put out the agent
     * deletes itself. Content of the request message must be {@link Position#toString()} of the fire engine position.
     * The fire engine must be at the fires position to be able to decrease its intensity. If it is a message with the
     * new fire status ({@link Fire#toString()}) is send to the engine agent.
     */
    class PutOutService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology(PUT_OUT_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received put out request");
            
            // get content
            if (requestMsg.getContent() == null) {
                logger.error("request message has no content");
                return;
            }
            final Position enginePosition = Position.fromString(requestMsg.getContent());
            logger.debug("engine position (" + enginePosition + ")");
            
            boolean takeDown = false;
            if (fire.position.equals(enginePosition)) {
                // fire engine is at the fire and can put out
                if (fire.getIntensity() > 0) {
                    fire.decreaseIntensity(1);
                    if (fire.getIntensity() < 1) {
                        // fire is put out
                        increaseBehaviour.stop();
                        fire.setIntensity(0);
                        tbf.getThread(increaseBehaviour).interrupt();
                        pb.removeSubBehaviour(increaseBehaviour);
                        threadedBehaviours.remove(increaseBehaviour);
                        if (fire.getCasualties() < 1) {
                            fire.setCasualties(0);
                            takeDown = true;
                        }
                    }
                } else {
                    logger.debug("fire is already put out");
                }
                sendStatus(requestMsg.getSender());
            } else {
                logger.debug("fire engine is too far away");
            }
            logger.debug("sent PutOut reply");
            if (takeDown) {
                doDelete();
            }
        }
    }
    
    /**
     * Service for ambulances to pick up a casualty. Content of the request message must be {@link Position#toString()}
     * of the ambulance position. The ambulance must be at the fires position to be able to pick up a casualty. The
     * reply message confirms or disconfirms if a casualty is picked up. If the ambulance is at the fires position, a
     * message with the new fire status ({@link Fire#toString()}) is send to the ambulance agent.
     */
    class PickUpCasualtyService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology(PICK_UP_ONT_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received PickUpCasualty request");
            
            // get content
            if (requestMsg.getContent() == null) {
                logger.error("request message has no content");
                return;
            }
            final Position ambulancePosition = Position.fromString(requestMsg.getContent());
            logger.debug("ambulance position (" + ambulancePosition + ")");
            
            final ACLMessage replyMsg = requestMsg.createReply();
            boolean takeDown = false;
            if (fire.position.equals(ambulancePosition)) {
                // ambulance is at the fire and can pick up a casualty
                if (fire.getCasualties() > 0) {
                    fire.decreaseCasualties(1);
                    replyMsg.setPerformative(ACLMessage.CONFIRM);
                    if (fire.getCasualties() < 1 && fire.getIntensity() < 1) {
                        fire.setIntensity(0);
                        fire.setCasualties(0);
                        takeDown = true;
                    }
                } else {
                    logger.debug("no casualty to pick up");
                    replyMsg.setPerformative(ACLMessage.DISCONFIRM);
                }
                sendStatus(requestMsg.getSender());
            } else {
                logger.debug("ambulance is too far away");
                replyMsg.setPerformative(ACLMessage.DISCONFIRM);
            }
            send(replyMsg);
            logger.debug("sent PickUpCasualty reply");
            if (takeDown) {
                doDelete();
            }
        }
    }
    
    /**
     * Sends the current fire status. Package scoped for faster access by inner classes.
     * 
     * @param receiver
     */
    void sendStatus(final AID receiver) {

        final ACLMessage statusMsg = new ACLMessage(ACLMessage.INFORM);
        statusMsg.setOntology(FIRE_STATUS_ONT_TYPE);
        statusMsg.setContent(fire.toString());
        statusMsg.addReceiver(receiver);
        send(statusMsg);
    }
    
    /**
     * Increases the fire intensity by {@link #intensityIncrease} and the casualties by {@link #casualtiesIncrease}.
     */
    private class Increase extends TickerBehaviour {
        
        /**
         * @param a
         * @param period
         */
        public Increase(final Agent a, final long period) {

            super(a, period);
        }
        
        /**
         * @see jade.core.behaviours.TickerBehaviour#onTick()
         */
        @Override
        protected void onTick() {

            fire.increaseIntensity(intensityIncrease);
            fire.increaseCasualties(casualtiesIncrease);
        }
    }
}
