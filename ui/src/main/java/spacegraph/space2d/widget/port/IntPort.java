package spacegraph.space2d.widget.port;

import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.PushButton;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static spacegraph.space2d.container.Gridding.col;

/** TODO add both text and spinner methods */
public class IntPort extends EditablePort<Integer> {

    final AtomicInteger value = new AtomicInteger();


    public IntPort() {
        super(0, Integer.class);

        PushButton incButton = new PushButton("+");
        PushButton decButton = new PushButton("-");
        incButton.click(()-> out(get() +1));
        decButton.click(()-> out((get() -1))); //TODO fully atomic

        set(new Splitting(txt, col(incButton, decButton), false, 0.8f));
    }

    public IntPort(IntConsumer i) {
        this();
        on((I)->{
            if (I != null)
                i.accept(I);
        });
    }
    public int get() {
        return value.getOpaque();
    }


    @Override
    protected boolean change(Integer x) {
        return value.getAndSet(x)!=x;
    }

    @Override
    protected Integer parse(String x) {
        x = x.trim();
        if (x.isEmpty())
            return null;
        else {
            try {
                return Integer.valueOf(x);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

}
