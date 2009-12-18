package it.unitn.disi.aose.firerespsim;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class FireMonitorAgent extends Agent {
    
    private static final DFAgentDescription areaDimensionsAD = new DFAgentDescription();
    private static final ServiceDescription areaDimensionsSD = new ServiceDescription();
    private AID areaDimensionsAID = null;
    private static final DFAgentDescription fireStatusAD = new DFAgentDescription();
    private static final ServiceDescription fireStatusSD = new ServiceDescription();
    private AID fireStatusAID = null;
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

        super.setup();
        
        areaDimensionsSD.setType("AreaDimensions");
        areaDimensionsAD.addServices(areaDimensionsSD);
        fireStatusSD.setType("FireStatus");
        fireStatusAD.addServices(fireStatusSD);
        
        // add behaviors
        addBehaviour(new GetAreaDimensions());
        addBehaviour(new ScanArea());
//        addBehaviour(new FireAlertService()); // TODO
        
        // register at DF service
        final DFAgentDescription descr = new DFAgentDescription();
        final ServiceDescription sd = new ServiceDescription();
        sd.setName("FireAlert");
        sd.setType("FireMonitor");
        descr.addServices(sd);
        try {
            DFService.register(this, descr);
        } catch (final FIPAException e) {
            System.out.println("Cannot register fire monitor agent.");
            e.printStackTrace();
            doDelete();
        }
        System.out.println("registered " + getName() + " at DF");
    }
    
    /**
     * Package scoped for faster access by inner classes.
     * 
     * @return
     */
    AID getAreaDimensionsAID() {

        if (areaDimensionsAID != null) return areaDimensionsAID;
        
        DFAgentDescription[] result = null;
        try {
            result = DFService.search(this, areaDimensionsAD);
        } catch (final FIPAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return areaDimensionsAID = (result != null && result.length > 0) ? result[0].getName() : null;
    }
    
    /**
     * Package scoped for faster access by inner classes.
     * 
     * @return
     */
    AID getFireStatusAID() {

        if (fireStatusAID != null) return fireStatusAID;
        
        DFAgentDescription[] result = null;
        try {
            result = DFService.search(this, fireStatusAD);
        } catch (final FIPAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return fireStatusAID = (result != null && result.length > 0) ? result[0].getName() : null;
    }
    
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaWidth = 0;
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaHeight = 0;
    
    /**
     * @author tom
     */
    class GetAreaDimensions extends OneShotBehaviour {
        
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology("AreaDimensions"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            System.out.println("GetAreaDimensions.action()");
            
            AID aid = null;
            if ((aid = getAreaDimensionsAID()) == null) {
                System.out.println("no AreaDimensions AID");
                return;
            }
            
            final ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(aid);
            requestMsg.setOntology("AreaDimensions");
            requestMsg.setContent(areaColumn + " " + areaRow);
            send(requestMsg);
            System.out.println("sent AreaDimensions request");
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            System.out.println("received AreaDimensions reply");
            final String[] areaDimensions = replyMsg.getContent().split(" ");
            areaWidth = Integer.parseInt(areaDimensions[0]);
            areaHeight = Integer.parseInt(areaDimensions[1]);
            doDelete();
        }
    }
    
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaColumn = 1;
    /**
     * Package scoped for faster access by inner classes.
     */
    int areaRow = 1;
    
    /**
     * @author tom
     */
    class ScanArea extends CyclicBehaviour {
        
        private final MessageTemplate replyTpl = MessageTemplate.and(
                                                                     MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                     MessageTemplate.MatchOntology("FireStatus"));
        
        /**
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {

            System.out.println("ScanArea.action()");
            
            if (areaWidth == 0 || areaHeight == 0) {
                // area dimensions not yet set
                System.out.println("area dimensions not yet set");
                return;
            }
            
            AID aid = null;
            if ((aid = getFireStatusAID()) == null) {
                System.out.println("no FireStatus AID");
                return;
            }
            
            // get fire status for current position
            final ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(aid);
            requestMsg.setOntology("FireStatus");
            requestMsg.setContent(areaColumn + " " + areaRow);
            send(requestMsg);
            System.out.println("sent FireStatus request");
            final ACLMessage replyMsg = blockingReceive(replyTpl);
            System.out.println("received FireStatus reply");
            if (Boolean.parseBoolean(replyMsg.getContent())) {
                // position is on fire
                System.out.println("current position (" + areaColumn + ", " + areaRow + ") is on fire");
                // TODO tell registered agents
            } else {
                // position is not on fire
                System.out.println("current position (" + areaColumn + ", " + areaRow + ") is not on fire");
            }
            
            // move to next position
            if (areaColumn == areaWidth) {
                // to new row
                areaColumn = 1;
                if (areaRow == areaHeight) {
                    // to first row
                    areaRow = 1;
                } else {
                    // to next row
                    areaRow++;
                }
            } else {
                // to next column
                areaColumn++;
            }
            System.out.println("moved to position (" + areaColumn + ", " + areaRow + ")");
        }
        
    }
}
