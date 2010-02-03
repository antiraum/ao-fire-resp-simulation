package it.unitn.disi.aose.firerespsim.util;

import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.apache.log4j.Logger;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
public final class AgentUtil {
    
    private static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    /**
     * @param ac
     * @param nickname
     * @param className
     * @param args
     * @return {@link AgentController} if the agent was started, else <code>null</code>
     */
    public static AgentController startAgent(final AgentContainer ac, final String nickname, final String className,
                                             final Object[] args) {

        try {
            final AgentController a = ac.createNewAgent(nickname, className, args);
            a.start();
            return a;
        } catch (final StaleProxyException e) {
            logger.error("couldn't start the '" + nickname + "' agent");
            return null;
        }
    }
}
