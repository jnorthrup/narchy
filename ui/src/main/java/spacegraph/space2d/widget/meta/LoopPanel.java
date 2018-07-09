package spacegraph.space2d.widget.meta;

import jcog.exe.InstrumentedLoop;
import jcog.exe.Loop;
import jcog.math.MutableInteger;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.IntSpinner;
import spacegraph.space2d.widget.tab.ButtonSet;

/**
 * control and view statistics of a loop
 */
public class LoopPanel extends Gridding {

    protected final Loop loop;
    private final IntSpinner fpsLabel;
    private final Plot2D cycleTimePlot;
    private MutableInteger fps;

    private volatile boolean pause = false;

    public LoopPanel(Loop loop) {
        this.loop = loop;
        fps = new MutableInteger(Math.round(loop.getFPS()));
        fpsLabel = new IntSpinner(fps, f -> f + "fps", 0, 100);

        if (loop instanceof InstrumentedLoop) {
            InstrumentedLoop iloop = (InstrumentedLoop) loop;
            cycleTimePlot = new Plot2D(128, Plot2D.Line)
                    .add("cycleTime", iloop.cycleTime::getMean)
                    .add("dutyTime", iloop.dutyTime::getMean);
        } else {
            cycleTimePlot = null; 
        }

        set(
                new Gridding(
                        new ButtonSet(ButtonSet.Mode.One,
                                ToggleButton.awesome("play").on((b) -> {
                                    if (b) {
                                        if (pause) {
                                            pause = false;
                                            update();
                                        }

                                    }
                                }), ToggleButton.awesome("pause").on((b) -> {
                            if (b) {

                                if (!pause) {
                                    pause = true;
                                    update(); 
                                }
                            }
                        })
                        ),
                        fpsLabel, 
                        cycleTimePlot
                ));
        update();
    }

    public void update() {
        synchronized (this) {
            if (!pause) {
                int f = fps.intValue();
                int g = Math.round(loop.getFPS());
                if (f > 0) {
                    if (f != g) {
                        loop.runFPS(f);
                        fpsLabel.set(f);
                    }
                } else {
                    fps.set(g);
                    fpsLabel.set(g);
                }
                cycleTimePlot.update();
            } else {
                if (loop.isRunning()) {
                    
                    loop.stop();
                    fpsLabel.set(0);
                }
                
            }
        }

    }
}
