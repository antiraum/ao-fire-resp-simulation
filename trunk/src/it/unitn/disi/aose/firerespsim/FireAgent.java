package it.unitn.disi.aose.firerespsim;

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
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Row on the simulation area. Package scoped for faster access by inner classes.
     */
    int row = 0;
    /**
     * Column on the simulation area. Package scoped for faster access by inner classes.
     */
    int col = 0;
    /**
     * Fire intensity. From 1 to infinite. Package scoped for faster access by inner classes.
     */
    int intensity = 0;
    /**
     * Intensity increase per {@link Increase#onTick()}. From 1 to 10. Package scoped for faster access by inner
     * classes.
     */
    int intensityIncrease = 0;
    /**
     * Current number of casualties. From 0 to infinite. Package scoped for faster access by inner classes.
     */
    int casualties = 0;
    /**
     * Casualties increase per {@link Increase#onTick()}. From 1 to 3. Package scoped for faster access by inner
     * classes.
     */
    int casualtiesIncrease = 0;
    
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
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

        super.setup();
        
        // read start-up arguments
        final Object[] params = getArguments();
        if (params == null || params.length < 3) {
            logger.error("start-up arguments row, column, and increase ival needed");
            doDelete();
            return;
        }
        row = (Integer) params[0];
        col = (Integer) params[1];
        final int increaseIval = (Integer) params[2];
        
        // randomized initialization value
        intensityIncrease = RandomUtils.nextInt(9) + 1;
        casualtiesIncrease = RandomUtils.nextInt(2) + 1;
        
        // add behaviors
        increaseBehaviour = new Increase(this, increaseIval);
        threadedBehaviours.addAll(Arrays.asList(new Behaviour[] {
            new StatusService(), new PutOutService(), new PickUpCasualtyService(), increaseBehaviour}));
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

        logger.debug("takeDown");
        
        for (final Behaviour b : threadedBehaviours) {
            if (b != null) {
                tbf.getThread(b).interrupt();
            }
        }
        
        super.takeDown();
    }
    
    /**
     * Service that provides the current fire status to fire engines and ambulances. Content of the request message must
     * consist of the fire engine/ambulance row and column separated by a space. Content of the reply message consists
     * of the intensity and number of casualties separated by a space.
     */
    class StatusService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology("Status"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received Status request");
            
            // get content
            if (requestMsg.getContent() == null) {
                logger.error("request message has no content");
                return;
            }
            final String[] requestContent = requestMsg.getContent().split(" ");
            if (requestContent.length != 2) {
                logger.error("request message has wrong format");
                return;
            }
            final int requesterRow = Integer.parseInt(requestContent[0]);
            final int requesterCol = Integer.parseInt(requestContent[1]);
            logger.debug("requester position (" + requesterRow + ", " + requesterCol + ")");
            
            final ACLMessage replyMsg = requestMsg.createReply();
            if (Math.abs(row - requesterRow) <= 1 && Math.abs(col - requesterCol) <= 1) {
                // requester is next to the fire and can get the status
                replyMsg.setPerformative(ACLMessage.INFORM);
                replyMsg.setContent(intensity + " " + casualties);
            } else {
                logger.debug("requester is too far away");
                replyMsg.setPerformative(ACLMessage.REFUSE);
            }
            send(replyMsg);
            logger.debug("sent Status reply");
        }
    }
    
    /**
     * Service for fire engines to reduce the fire intensity. If the intensity reaches 0 the fire is put out the agent
     * deletes itself. Content of the request message must consist of fire engine row, fire engine column, and intensity
     * decrease (0 to 10) separated by spaces. Reply message content is the new intensity.
     */
    class PutOutService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology("PutOut"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.debug("received PutOut request");
            
            // get content
            if (requestMsg.getContent() == null) {
                logger.error("request message has no content");
                return;
            }
            final String[] requestContent = requestMsg.getContent().split(" ");
            if (requestContent.length != 3) {
                logger.error("request message has wrong format");
                return;
            }
            final int engineRow = Integer.parseInt(requestContent[0]);
            final int engineCol = Integer.parseInt(requestContent[1]);
            final int decrease = Math.min(10, Math.max(0, Integer.parseInt(requestContent[2]))); // must be from 0 to 10
            logger.debug("engine position (" + engineRow + ", " + engineCol + "), decrease = " + decrease);
            
            final ACLMessage replyMsg = requestMsg.createReply();
            boolean takeDown = false;
            if (Math.abs(row - engineRow) <= 1 && Math.abs(col - engineCol) <= 1) {
                // fire engine is next to the fire and can put out
                if (intensity > 0) {
                    intensity -= decrease; // TODO thread synchronization
                    replyMsg.setPerformative(ACLMessage.CONFIRM);
                    if (intensity < 1) {
                        // fire is put out
                        increaseBehaviour.stop();
                        pb.removeSubBehaviour(increaseBehaviour);
                        threadedBehaviours.remove(increaseBehaviour);
                        if (casualties < 1) {
                            takeDown = true;
                        }
                    }
                } else {
                    logger.debug("fire is already put out");
                    replyMsg.setPerformative(ACLMessage.DISCONFIRM);
                }
                replyMsg.setContent(intensity + "");
            } else {
                logger.debug("fire engine is too far away");
                replyMsg.setPerformative(ACLMessage.DISCONFIRM);
            }
            send(replyMsg);
            logger.debug("sent PutOut reply");
            if (takeDown) {
                doDelete();
            }
        }
        
    }
    
    /**
     * Service for ambulances to pick up a casualty. Content of the request message must consist of ambulance row and
     * ambulance column separated by a space. Reply message content is the new number of casualties.
     */
    class PickUpCasualtyService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology("PickUpCasualty"));
        
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
            final String[] requestContent = requestMsg.getContent().split(" ");
            if (requestContent.length != 2) {
                logger.error("request message has wrong format");
                return;
            }
            final int ambulanceRow = Integer.parseInt(requestContent[0]);
            final int ambulanceCol = Integer.parseInt(requestContent[1]);
            logger.debug("ambulance position (" + ambulanceRow + ", " + ambulanceCol + ")");
            
            final ACLMessage replyMsg = requestMsg.createReply();
            boolean takeDown = false;
            if (Math.abs(row - ambulanceRow) <= 1 && Math.abs(col - ambulanceCol) <= 1) {
                // ambulance is next to the fire and can pick up a casualty
                if (casualties > 0) {
                    casualties -= 1; // TODO thread synchronization
                    replyMsg.setPerformative(ACLMessage.CONFIRM);
                    if (casualties < 1 && intensity < 1) {
                        takeDown = true;
                    }
                } else {
                    logger.debug("no casualty to pick up");
                    replyMsg.setPerformative(ACLMessage.DISCONFIRM);
                }
                replyMsg.setContent(casualties + "");
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
     * Increases the fire intensity by {@link #intensityIncrease}.
     */
    class Increase extends TickerBehaviour {
        
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

            intensity += intensityIncrease; // TODO thread synchronization
        }
        
    }
}
