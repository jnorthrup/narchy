package spacegraph.space2d.widget.port;

import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit;

import java.util.concurrent.atomic.AtomicInteger;

import static spacegraph.space2d.container.Gridding.col;

/** TODO add both text and spinner methods */
public class IntPort extends TypedPort {

    final AtomicInteger value = new AtomicInteger();

    final TextEdit txt;

    public IntPort() {
        super(Integer.class);

        process(0);

        PushButton incButton = new PushButton("+");
        PushButton decButton = new PushButton("-");

        set(new Splitting(txt = new TextEdit(8, 1), col(incButton, decButton), false, 0.8f));

        incButton.click(()-> set(value.getOpaque()+1));
        decButton.click(()-> set((value.getOpaque()-1))); //TODO fully atomic
        txt.on(this::set);
    }

    public final boolean set(String x) {
        try {
            Integer next = process(Integer.valueOf(x));
            if (next!=null) {
                return set(next);
            }

        } catch (NumberFormatException t) {

        }
        return false;
    }

    public final boolean set(int next) {
        if (value.getAndSet(next)!=next) {
            txt.text(Integer.toString(next));
            out(next);

            return true;
        }
        return false;
    }

    /** returns true if the value is valid and can set the port, override in subclasses to filter input */
    public Integer process(int v) {
        return v;
    }

}
