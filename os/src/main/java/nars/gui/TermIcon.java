package nars.gui;

import nars.concept.Concept;
import nars.term.Termed;
import spacegraph.space2d.widget.text.VectorLabel;

/**
 * Created by me on 11/29/16.
 */
public class TermIcon extends VectorLabel {


    public TermIcon(Termed c) {
        super(c.toString());

        
    }

    public static void update(Concept c, long time) {

        var cc = c;
        if (cc == null)
            return;






    }








}
