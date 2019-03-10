package spacegraph.space2d.widget.chip;

import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.text.LabeledPane;

public class PlotChip extends Bordering {
    final Port in;
    private final Plot2D plot;
    double nextValue = Double.NaN;

    public PlotChip() {
        super();

        this.plot = new Plot2D(256, Plot2D.Line);
        plot.add("x", ()->nextValue);

        set(plot);

        this.in = new Port().on((Object x)->{
            if (x instanceof Number) {
                nextValue = ((Number)x).floatValue();
            } else if (x instanceof Boolean) {
                nextValue = ((Boolean)x) ? 1 : 0;
            } else {
                return;
            }
            plot.commit();
        });
        set(S, LabeledPane.the("in", new Gridding(in)));

    }
}
