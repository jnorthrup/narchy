package nars.gui;

import nars.term.Term;
import spacegraph.Surface;
import spacegraph.widget.tab.TabPane;
import spacegraph.widget.text.Label;

import java.util.Map;

public class ConceptSurface extends TabPane {
    public ConceptSurface(Term id) {
        super(Map.of(
            "id", ()->new Label(id.toString()),
            "links", ()->new Label("TODO")
        ));
    }
}
