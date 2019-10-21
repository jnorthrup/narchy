package nars.gui;

import nars.NAR;
import nars.util.Timed;
import org.jetbrains.annotations.NotNull;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.meta.WindowToggleButton;
import spacegraph.space2d.widget.text.AbstractLabel;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * for use with ReflectionSurface
 */
public class CycleView implements Runnable {

    public final Timed timed;

    public final AtomicBoolean run;

    public final Runnable step;

    public final AbstractLabel time;

    public final ToggleButton details;

    public CycleView(@NotNull NAR nar) {

        this.timed = nar;


        run = new AtomicBoolean(false);

        step = new Runnable() {
            @Override
            public void run() {
                nar.run(1);
            }
        };

        time = new VectorLabel("");

        details = new WindowToggleButton("Details", nar);

        nar.onCycle(this);

    }

    @Override
    public void run() {
        time.text("@: " + timed.time());
    }

}
