package it.unitn.disi.aose.firerespsim;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.log4j.Logger;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class EnvironmentAgent extends Agent {
    
    /**
     * Package scoped for faster access by inner classes.
     */
    static Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    private static final int DEFAULT_AREA_WIDTH = 20;
    private static final int DEFAULT_AREA_HEIGHT = 20;
    private static final int DEFAULT_NUMBER_OF_HOSPITALS = 3;
    private static final int DEFAULT_NUMBER_OF_FIRE_BRIGADES = 3;
    
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaWidth = 0;
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaHeight = 0;
    /**
     * Package scoped for faster access by inner classes.
     */
    boolean[][] fireStatuses;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        logger.debug("setup start");
        
        super.setup();
        
        // initialization parameters
        int numberOfHospitals = 0;
        final int numberOfFireBrigades = 0;
        
        final Object[] params = getArguments();
        if (params != null) {
            // TODO let user control initialization parameters
        }
        
        if (areaWidth == 0) {
            areaWidth = DEFAULT_AREA_WIDTH;
        }
        if (areaHeight == 0) {
            areaHeight = DEFAULT_AREA_HEIGHT;
        }
        if (numberOfHospitals == 0) {
            numberOfHospitals = DEFAULT_NUMBER_OF_HOSPITALS;
        }
        if (numberOfFireBrigades == 0) {
            numberOfHospitals = DEFAULT_NUMBER_OF_FIRE_BRIGADES;
        }
        
        // initialize fire statuses
        fireStatuses = new boolean[areaHeight][areaWidth];
        for (int row = 0; row < areaHeight; row++) {
            for (int col = 0; col < areaWidth; col++) {
                fireStatuses[row][col] = false;
            }
        }
        
        // simulation initialization
        // TODO spawn agents
        
        // register at DF service
        final DFAgentDescription descr = new DFAgentDescription();
        final ServiceDescription areaDimensionsSD = new ServiceDescription();
        areaDimensionsSD.setName(getName());
        areaDimensionsSD.setType("AreaDimensions");
        descr.addServices(areaDimensionsSD);
        final ServiceDescription fireStatusSD = new ServiceDescription();
        fireStatusSD.setName(getName());
        fireStatusSD.setType("FireStatus");
        descr.addServices(fireStatusSD);
        try {
            DFService.register(this, descr);
        } catch (final FIPAException e) {
            logger.error("cannot register at DF");
            e.printStackTrace();
            doDelete();
        }
        
        addBehaviour(new AreaDimensionsService());
        addBehaviour(new FireStatusService());
        
        logger.debug("setup end");
    }
    
    /**
     * @author tom
     */
    class AreaDimensionsService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology("AreaDimensions"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            logger.debug("action start");
            
            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.info("received AreaDimensions request");
            
            // send area dimensions
            final ACLMessage replyMsg = new ACLMessage(ACLMessage.INFORM);
            replyMsg.setContent(areaWidth + " " + areaHeight);
            replyMsg.setOntology("AreaDimensions");
            replyMsg.addReceiver(requestMsg.getSender());
            send(replyMsg);
            
            logger.debug("action end");
        }
    }
    
    /**
     * @author tom
     */
    class FireStatusService extends CyclicBehaviour {
        
        private final MessageTemplate requestTpl = MessageTemplate.and(
                                                                       MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                                       MessageTemplate.MatchOntology("FireStatus"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            logger.debug("action start");
            
            final ACLMessage requestMsg = blockingReceive(requestTpl);
            if (requestMsg == null) return;
            
            logger.info("received FireStatus request");
            
            // get requested position
            final String[] position = requestMsg.getContent().split(" ");
            final int row = Integer.parseInt(position[0]);
            final int col = Integer.parseInt(position[1]);
            
            // send fire status
            final ACLMessage replyMsg = new ACLMessage(ACLMessage.INFORM);
            replyMsg.setContent(Boolean.toString(fireStatuses[row][col]));
            replyMsg.setOntology("FireStatus");
            replyMsg.addReceiver(requestMsg.getSender());
            send(replyMsg);
            
            logger.debug("action end");
        }
    }
}
