package spacegraph.widget.meta;

import jcog.exe.Loop;
import jcog.math.MutableInteger;
import spacegraph.container.Gridding;
import spacegraph.widget.button.IconToggleButton;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.slider.IntSpinner;
import spacegraph.widget.tab.ButtonSet;
import spacegraph.widget.windo.Widget;

/**
 * control and view statistics of a loop
 */
public class LoopPanel extends Widget {

    private final IntSpinner fpsLabel;
    private final Loop loop;
    private final Plot2D cycleTimePlot;
    MutableInteger fps;

    public LoopPanel(Loop loop) {
        this.loop = loop;
        fps = new MutableInteger(Math.round(loop.getFPS()));
        fpsLabel = new IntSpinner(fps, (f)-> f + "fps", 0, 100);
        cycleTimePlot = new Plot2D(128, Plot2D.Line)
                .add("cycleTime", ()->loop.cycleTime.getMean())
                .add("dutyTime", ()->loop.dutyTime.getMean());

        content(
            new Gridding(
                new ButtonSet(ButtonSet.Mode.One,
                    IconToggleButton.awesome("play").on((b) -> {
                        if (b) {
                            synchronized (loop) {
                                loop.runFPS(fps.intValue());
                                update();
                            }
                        }
                    }), IconToggleButton.awesome("pause").on((b) -> {
                        if (b) {
                            synchronized (loop) {
                                loop.stop();
                                update(); //update because this view wont be updated while paused
                            }
                        }
                    })
                ),
                fpsLabel, //TODO number spin control
                cycleTimePlot
        ));
        update();
    }

    public void update() {
        int f = fps.intValue();
        int g = Math.round(loop.getFPS());
        if (f!=g) {
            loop.runFPS(f);
            fpsLabel.update(0);
        }
        cycleTimePlot.update();
    }
}
