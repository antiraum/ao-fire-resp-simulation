package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import org.apache.log4j.Logger;

/**
 * Ontology for the simulation.
 * 
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class FireResponseOntology extends BeanOntology {
    
    private static final Logger logger = Logger.getLogger("it.unitn.disi.aose.firerespsim");
    
    private static final Ontology thisOntology = new FireResponseOntology();
    
    /**
     * @return Singleton instance of this ontology.
     */
    public static Ontology getInstance() {

        return thisOntology;
    }
    
    private FireResponseOntology() {

        super("Fire Response Simulation");
        try {
            add("it.unitn.disi.aose.firerespsim.ontology");
        } catch (final BeanOntologyException e) {
            logger.error("error adding ontology beans");
        }
    }
    
}
