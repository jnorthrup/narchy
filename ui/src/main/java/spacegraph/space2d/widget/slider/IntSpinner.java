package spacegraph.space2d.widget.slider;

import jcog.Util;
import jcog.math.MutableInteger;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.windo.Widget;

import java.util.function.IntFunction;

public class IntSpinner extends Widget {

    private final int min;
    private final int max;
    private final VectorLabel label;
    private final MutableInteger i;
    private final IntFunction<String> labeller;

    public IntSpinner(MutableInteger i, IntFunction<String> labeller, int min, int max) {
        this.min = min;
        this.max = max;
        this.i = i;
        this.labeller = labeller;
        content(
            new Splitting(
                label = new VectorLabel(),
                new Gridding(Gridding.HORIZONTAL,
                    new PushButton("+", ()-> update(+1)),
                    new PushButton("-", ()-> update(-1))
            ), 0.2f)
        );
        update(0);
    }

    private void update(int delta) {
        synchronized (i) {
            set(i.intValue() + delta);
        }
    }

    public void set(int nextValue) {
        synchronized (i) {
            nextValue = Util.clamp(nextValue, min, max);
            label.text(labeller.apply(nextValue));
            i.set(nextValue);
        }
    }

}
