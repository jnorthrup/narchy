package spacegraph.space2d.widget.chip;

import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.port.TypedPort;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public class PlotChip extends TypedPort<Number> {
    private final Plot2D plot;
    double nextValue = Double.NaN;

    public PlotChip() {
        super(Number.class);

        this.plot = new Plot2D(256, Plot2D.Line);
        plot.add("x", new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return nextValue;
            }
        });

        set(plot);

        on(new Consumer<Number>() {
            @Override
            public void accept(Number x) {
                nextValue = (double) x.floatValue();
                if (nextValue == nextValue)
                    plot.commit();
            }
        });

    }
}
