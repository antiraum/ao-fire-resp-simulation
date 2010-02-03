package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * @author Thomas Hess (139467) / Musawar Saeed (140053)
 */
@SuppressWarnings("serial")
public final class AreaDimensionsInfo implements Predicate {
    
    private AreaDimensions areaDimensions;
    
    /**
     * Constructor for bean instantiation.
     */
    public AreaDimensionsInfo() {

    // empty
    }
    
    /**
     * @param areaDimensions
     */
    public AreaDimensionsInfo(final AreaDimensions areaDimensions) {

        this.areaDimensions = areaDimensions;
    }
    
    /**
     * @return Dimensions of the simulation area.
     */
    @Slot(mandatory = true)
    public AreaDimensions getAreaDimensions() {

        return areaDimensions;
    }
    
    /**
     * @param areaDimensions
     */
    public void setAreaDimensions(final AreaDimensions areaDimensions) {

        this.areaDimensions = areaDimensions;
    }
}
