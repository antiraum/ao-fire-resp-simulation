package it.unitn.disi.aose.firerespsim.agents;

import it.unitn.disi.aose.firerespsim.ontology.FireResponseOntology;
import jade.content.ContentElement;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.log4j.Logger;

/**
 * Super class of all agents. Does some common things and provides various utility methods.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public abstract class ExtendedAgent extends Agent {
    
    private static final Boolean THREADED = true;
    
    /**
     * Response performatives for request interaction.
     */
    protected static final List<Integer> REQUEST_RESPONSE_PERFORMATIVES = Arrays.asList(ACLMessage.AGREE,
                                                                                        ACLMessage.INFORM,
                                                                                        ACLMessage.NOT_UNDERSTOOD,
                                                                                        ACLMessage.REFUSE);
    /**
     * Request performatives for subscription interaction.
     */
    protected static final List<Integer> SUBSCRIBE_REQUEST_PERFORMATIVES = Arrays.asList(ACLMessage.SUBSCRIBE,
                                                                                         ACLMessage.CANCEL);
    /**
     * Response performatives for subscription interaction.
     */
    protected static final List<Integer> SUBSCRIBE_RESPONSE_PERFORMATIVES = Arrays.asList(ACLMessage.AGREE,
                                                                                          ACLMessage.REFUSE);
    
    /**
     * Log4J logger.
     */
    protected static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * Behaviors that are executed in order.
     */
    protected final List<Behaviour> sequentialBehaviours = new ArrayList<Behaviour>();
    /**
     * Behaviors that are executed in parallel after the {@link #sequentialBehaviours} are finished.
     */
    protected final List<Behaviour> parallelBehaviours = new ArrayList<Behaviour>();
    
    /**
     * Agent start-up arguments and their default values. Override in concrete sub classes.
     */
    protected LinkedHashMap<String, Object> params = null;
    
    /**
     * DF service types. Override in concrete sub classes.
     */
    protected String[] dfTypes = null;
    
    private final Codec codec = new SLCodec();
    private final Ontology onto = FireResponseOntology.getInstance();
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
    
    /**
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {

//        logger.debug("starting up");
        
        super.setup();
        
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(onto);
        
        readArguments();
        
        registerAtDF();
    }
    
    /**
     * @see jade.core.Agent#takeDown()
     */
    @Override
    protected void takeDown() {

        logger.info("shutting down");
        
        if (THREADED) {
            for (final Behaviour b : parallelBehaviours) {
                if (b != null) {
                    tbf.getThread(b).interrupt();
                }
            }
        }
        
        super.takeDown();
    }
    
    /**
     * Reads the startup arguments and replace the default parameter values if corresponding arguments are given.
     */
    private void readArguments() {

        if (params == null) return;
        
        final Object[] args = getArguments();
        if (args == null) return;
        
        int argNum = 0;
        for (final Entry<String, Object> arg : params.entrySet()) {
            if (args.length <= argNum) {
                if (arg.getValue() == null) {
                    logger.error("start-up argument " + arg.getKey() + " needed");
                    doDelete();
                    return;
                }
                break;
            }
            arg.setValue(args[argNum]);
            argNum++;
        }
    }
    
    /**
     * Registers the agent's services at the DF.
     */
    private void registerAtDF() {

        if (dfTypes == null || dfTypes.length == 0) return;
        
        // register at the DF
        final DFAgentDescription descr = new DFAgentDescription();
        for (final String dfType : dfTypes) {
            final ServiceDescription sd = new ServiceDescription();
            sd.setName(getName());
            sd.setType(dfType);
            descr.addServices(sd);
        }
        try {
            DFService.register(this, descr);
        } catch (final FIPAException e) {
            logger.error("cannot register at the DF");
            doDelete();
        }
    }
    
    /**
     * Adds the {@link #sequentialBehaviours} and {@link #parallelBehaviours} to the agent.
     */
    protected void addBehaviours() {

        SequentialBehaviour sb = null;
        if (!sequentialBehaviours.isEmpty()) {
            sb = new SequentialBehaviour();
            for (final Behaviour b : sequentialBehaviours) {
                sb.addSubBehaviour(b);
            }
        }
        parallelBehaviours.addAll(parallelBehaviours);
        for (final Behaviour b : parallelBehaviours) {
            pb.addSubBehaviour(THREADED ? tbf.wrap(b) : b);
        }
        if (sb == null) {
            addBehaviour(pb);
        } else {
            sb.addSubBehaviour(pb);
            addBehaviour(sb);
        }
    }
    
    /**
     * Adds a behavior to the {@link #parallelBehaviours} after the initial {@link #addBehaviours()} call.
     * 
     * @param b
     */
    protected void addParallelBehaviour(final Behaviour b) {

        parallelBehaviours.add(b);
        pb.addSubBehaviour(THREADED ? tbf.wrap(b) : b);
    }
    
    /**
     * Stops and removes a behavior from the {@link #parallelBehaviours}.
     * 
     * @param b
     */
    protected void stopParallelBehaviour(final Behaviour b) {

        if (b instanceof TickerBehaviour) {
            ((TickerBehaviour) b).stop();
        }
        if (THREADED) {
            tbf.getThread(b).interrupt();
        }
        pb.removeSubBehaviour(b);
        parallelBehaviours.remove(b);
    }
    
    /**
     * @param performative
     * @param protocol
     * @param recipient
     * @param content
     */
    protected void sendMessage(final int performative, final String protocol, final AID recipient,
                               final Predicate content) {

        send(createMessage(performative, protocol, recipient, content));
    }
    
    /**
     * @param performative
     * @param protocol
     * @param recipients
     * @param content
     */
    protected void sendMessage(final int performative, final String protocol, final List<AID> recipients,
                               final Predicate content) {

        send(createMessage(performative, protocol, recipients, content));
    }
    
    /**
     * @param msg
     * @param performative
     * @param content
     */
    protected void sendReply(final ACLMessage msg, final int performative, final Predicate content) {

        send(createReply(msg, performative, content));
    }
    
    /**
     * @param msg
     * @param performative
     * @param content
     */
    protected void sendReply(final ACLMessage msg, final int performative, final String content) {

        send(createReply(msg, performative, content));
    }
    
    /**
     * @param msg
     * @param performative
     */
    public void sendReply(final ACLMessage msg, final int performative) {

        send(createReply(msg, performative));
    }
    
    /**
     * @param performative
     * @param protocol
     * @return {@link ACLMessage} skeleton.
     */
    protected ACLMessage createMessage(final int performative, final String protocol) {

        return createMessage(performative, protocol, Arrays.asList(new AID[] {}), null);
    }
    
    /**
     * @param performative
     * @param protocol
     * @param recipient
     * @param content
     * @return {@link ACLMessage} ready to send.
     */
    protected ACLMessage createMessage(final int performative, final String protocol, final AID recipient,
                                       final Predicate content) {

        return createMessage(performative, protocol, recipient == null ? null : Arrays.asList(recipient), content);
    }
    
    /**
     * @param performative
     * @param protocol
     * @param recipients
     * @param content
     * @return {@link ACLMessage} ready to send.
     */
    protected ACLMessage createMessage(final int performative, final String protocol, final List<AID> recipients,
                                       final Predicate content) {

        final ACLMessage msg = new ACLMessage(performative);
        msg.setOntology(onto.getName());
        msg.setLanguage(codec.getName());
        msg.setProtocol(protocol);
        if (recipients != null) {
            for (final AID recipient : recipients) {
                msg.addReceiver(recipient);
            }
        }
        fillMessage(msg, content);
        return msg;
    }
    
    /**
     * @param msg
     * @param performative
     * @param content
     * @return {@link ACLMessage} ready to send.
     */
    protected ACLMessage createReply(final ACLMessage msg, final int performative, final Predicate content) {

        final ACLMessage reply = msg.createReply();
        reply.setPerformative(performative);
        fillMessage(reply, content);
        return reply;
    }
    
    /**
     * @param msg
     * @param performative
     * @param content
     * @return {@link ACLMessage} ready to send.
     */
    protected ACLMessage createReply(final ACLMessage msg, final int performative, final String content) {

        final ACLMessage reply = msg.createReply();
        reply.setPerformative(performative);
        reply.setContent(content);
        return reply;
    }
    
    /**
     * @param msg
     * @param performative
     * @return {@link ACLMessage} ready to send.
     */
    protected ACLMessage createReply(final ACLMessage msg, final int performative) {

        final ACLMessage reply = msg.createReply();
        reply.setPerformative(performative);
        return reply;
    }
    
    /**
     * @param msg
     * @return Copy of the message.
     */
    protected ACLMessage copyMessage(final ACLMessage msg) {

        final List<AID> recipients = new ArrayList<AID>();
        final Iterator iter = msg.getAllReceiver();
        while (iter.hasNext()) {
            recipients.add((AID) iter.next());
        }
        final ACLMessage copy = createMessage(msg.getPerformative(), msg.getProtocol(), recipients, null);
        copy.setContent(msg.getContent());
        return copy;
    }
    
    /**
     * @param msg
     * @param content
     */
    protected void fillMessage(final ACLMessage msg, final Predicate content) {

        if (content == null) return;
        try {
            getContentManager().fillContent(msg, content);
        } catch (final Exception e) {
            logger.error("error filling message content");
        }
    }
    
    /**
     * @param sender
     * @param protocol
     * @param performative
     * @return Combined {@link MessageTemplate}.
     */
    protected MessageTemplate createMessageTemplate(final AID sender, final String protocol, final Integer performative) {

        return createMessageTemplate(sender, Arrays.asList(performative), protocol);
    }
    
    /**
     * @param sender
     * @param performatives
     * @param protocol
     * @return Combined {@link MessageTemplate}.
     */
    protected MessageTemplate createMessageTemplate(final AID sender, final List<Integer> performatives,
                                                    final String protocol) {

        final Set<MessageTemplate> andTpl = new HashSet<MessageTemplate>();
        if (sender != null) {
            andTpl.add(MessageTemplate.MatchSender(sender));
        }
        andTpl.add(MessageTemplate.MatchProtocol(protocol));
        MessageTemplate perfTpl = null;
        for (final Integer perf : performatives) {
            if (perfTpl == null) {
                perfTpl = MessageTemplate.MatchPerformative(perf);
            } else {
                perfTpl = MessageTemplate.or(perfTpl, MessageTemplate.MatchPerformative(perf));
            }
        }
        andTpl.add(perfTpl);
        andTpl.add(MessageTemplate.MatchOntology(onto.getName()));
        MessageTemplate tpl = null;
        for (final MessageTemplate at : andTpl) {
            if (tpl == null) {
                tpl = at;
            } else {
                tpl = MessageTemplate.and(tpl, at);
            }
        }
        return tpl;
    }
    
    /**
     * @param <T>
     * @param clazz
     * @param msg
     * @param expectingException
     * @return the extracted message content
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    protected <T> T extractMessageContent(final Class<T> clazz, final ACLMessage msg, final boolean expectingException)
            throws Exception {

        ContentElement ce;
        try {
            ce = getContentManager().extractContent(msg);
        } catch (final Exception e) {
            if (!expectingException) {
                logger.error("error extracting message content");
                e.printStackTrace();
            }
            throw new Exception();
        }
        if (ce.getClass() != clazz) {
            logger.error("reply message has wrong content ('" + clazz.getName() + "' expected)");
            throw new Exception();
        }
        return (T) ce;
    }
}
