package it.unitn.disi.aose.firerespsim;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.log4j.Logger;

/**
 * This agent simulates a hospital. It subscribes to new fire alerts at the fire monitor agent, reasons with the other
 * hospitals registered at the DF about responsibilities, and delegates its ambulance agents to the fires it is
 * responsible for. Start-up parameters are row and column.
 * 
 * @author tom
 */
@SuppressWarnings("serial")
public final class HospitalAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Package scoped for faster access by inner classes.
     */
    Agent thisAgent = this;
    
    /**
     * Row on the simulation area. Package scoped for faster access by inner classes.
     */
    int row = 0;
    /**
     * Column on the simulation area. Package scoped for faster access by inner classes.
     */
    int col = 0;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
        
        // read start-up arguments
        final Object[] params = getArguments();
        if (params == null || params.length < 2) {
            logger.error("start-up arguments row and column needed");
            doDelete();
            return;
        }
        row = (Integer) params[0];
        col = (Integer) params[1];
        
        // TODO create ambulances
        
        // add behaviors
        final SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new SubscribeToFireAlerts());
        sb.addSubBehaviour(new ReceiveFireAlerts());
        addBehaviour(sb);
    }
    
    /**
     * @see jade.core.Agent#takeDown()
     */
    @Override
    protected void takeDown() {

        logger.info("shutting down");
        
        super.takeDown();
    }
    
    /**
     * Subscribes to new fire alerts from the fire monitor agent.
     */
    class SubscribeToFireAlerts extends SimpleBehaviour {
        
        private final static String ONTOLOGY_TYPE = "FireAlert";
        
        private boolean done = false;
        private final DFAgentDescription fireAlertAD = new DFAgentDescription();
        private AID fireMonitorAID = null;
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.or(
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                                                                        MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)),
                                                                     MessageTemplate.MatchOntology(ONTOLOGY_TYPE));
        
        /**
         * Constructor
         */
        public SubscribeToFireAlerts() {

            super();
            
            final ServiceDescription fireAlertSD = new ServiceDescription();
            fireAlertSD.setType(ONTOLOGY_TYPE);
            fireAlertAD.addServices(fireAlertSD);
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            if (fireMonitorAID == null) {
                DFAgentDescription[] result = null;
                try {
                    result = DFService.search(thisAgent, fireAlertAD);
                } catch (final FIPAException e) {
                    logger.error("no agent with FireAlert service at DF");
                    e.printStackTrace();
                    return;
                }
                if (result != null && result.length > 0) {
                    fireMonitorAID = result[0].getName();
                }
            }
            if (fireMonitorAID == null) {
                logger.error("no FireAlert AID");
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
//                    done = true;
                }
                return;
            }
            
            final ACLMessage subscribeMsg = new ACLMessage(ACLMessage.SUBSCRIBE);
            subscribeMsg.addReceiver(fireMonitorAID);
            subscribeMsg.setOntology(ONTOLOGY_TYPE);
            send(subscribeMsg);
            logger.debug("sent FireAlert subscribtion");
            
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            if (replyMsg.getPerformative() == ACLMessage.CONFIRM) {
                logger.info("subscribed for new fire alerts at monitor agent");
            } else {
                logger.error("subscription was disconfirmed - assume this is because already subscribed");
            }
            
            done = true;
        }
        
        /**
         * @see jade.core.behaviours.Behaviour#done()
         */
        @Override
        public boolean done() {

            return done;
        }
        
    }
    
    /**
     * @author tom
     */
    class ReceiveFireAlerts extends CyclicBehaviour {
        
        private final static String ONTOLOGY_TYPE = "FireAlert";
        
        private final MessageTemplate alertTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology(ONTOLOGY_TYPE));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            final ACLMessage alertMsg = blockingReceive(alertTpl);
            if (alertMsg == null) return;
            
            final String[] position = alertMsg.getContent().split(" ");
            final int row = Integer.parseInt(position[0]);
            final int col = Integer.parseInt(position[1]);
            logger.info("received new fire alert at position (" + row + ", " + col + ")");
            
            // TODO talk with other hospitals about it
            
            // TODO do something about the fires responsible for
        }
        
    }
}
