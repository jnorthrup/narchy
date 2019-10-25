package nars.gui;

import nars.NAR;
import nars.Narsese;
import nars.gui.concept.ConceptSurface;
import nars.term.Term;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.function.Consumer;

import static nars.$.*;

public class ConceptBrowser extends Bordering {

    private final NAR nar;
    private Term current = null;

    public ConceptBrowser(NAR nar) {
        super();

        this.nar = nar;
        TextEdit edit = new TextEdit(16, 2).onChange(new Consumer<TextEdit>() {
            @Override
            public void accept(TextEdit te) {
                ConceptBrowser.this.update(te.text());
            }
        });

        north(edit.focus());

        edit.text(nar.self().toString());

    }

    private synchronized void update(String termString) {
        Term t;
        try {
            t = INSTANCE.$(termString);
        } catch (Narsese.NarseseException e) {
            center(new VectorLabel(e.getMessage()));
            current = null;
            return;
        }


        if (!t.op().conceptualizable) {
            center(new VectorLabel("invalid concept: " + termString));
            current = null;
        } else {

            if (current!=null && current.equals(t))
                return; //no change

            current = t;
            center(new ConceptSurface(t, nar));
        }

    }
}
