package nars.gui;

import nars.concept.Concept;
import nars.term.Termed;
import spacegraph.space2d.widget.text.VectorLabel;

/**
 * Created by me on 11/29/16.
 */
public class TermIcon extends VectorLabel {

    private Concept _concept;

    

    public TermIcon(Termed c) {
        super(c.toString());

        
    }

    public void update(Concept c, long time) {
        this._concept = c;

        Concept cc = this._concept;
        if (cc == null)
            return;






    }








}
