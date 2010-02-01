package it.unitn.disi.aose.firerespsim.ontology;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

/**
 * @author tom
 */
@SuppressWarnings("serial")
public final class AreaDimensions implements Concept {
    
    private int width;
    private int height;
    
    /**
     * Constructor for bean instantiation.
     */
    public AreaDimensions() {

    // empty
    }
    
    /**
     * @param width
     * @param height
     */
    public AreaDimensions(final int width, final int height) {

        this.width = width;
        this.height = height;
    }
    
    /**
     * @return With of the simulation area.
     */
    @Slot(mandatory = true)
    public int getWidth() {

        return width;
    }
    
    /**
     * @param width
     */
    public void setWidth(final int width) {

        this.width = width;
    }
    
    /**
     * @return Height of the simulation area.
     */
    @Slot(mandatory = true)
    public int getHeight() {

        return height;
    }
    
    /**
     * @param height
     */
    public void setHeight(final int height) {

        this.height = height;
    }
}
