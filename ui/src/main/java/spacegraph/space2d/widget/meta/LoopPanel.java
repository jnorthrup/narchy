package spacegraph.space2d.widget.meta;

import jcog.Util;
import jcog.exe.InstrumentedLoop;
import jcog.exe.Loop;
import jcog.math.MutableInteger;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.IntSpinner;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * control and view statistics of a loop
 */
public class LoopPanel extends Gridding {

    protected final Loop loop;
    private final IntSpinner fpsLabel;
    private final Plot2D cycleTimePlot;
    private final Plot2D heapPlot;
    private final MutableInteger fps;

    private volatile boolean pause = false;

    public LoopPanel(Loop loop) {
        this.loop = loop;
        fps = new MutableInteger(Math.round(loop.getFPS()));
        fpsLabel = new IntSpinner(fps, f -> f + "fps", 0, 100);

        if (loop instanceof InstrumentedLoop) {
            InstrumentedLoop iloop = (InstrumentedLoop) loop;
            cycleTimePlot = new Plot2D(128, Plot2D.Line)
                    .add("cycleTime", iloop.cycleTime::asFloat)
                    .add("dutyTime", iloop.dutyTime::asFloat)
            ;
        } else {
            cycleTimePlot = null; 
        }

        heapPlot = new Plot2D(128, Plot2D.Line)
                .add("heap", Util::memoryUsed, 0, 1);

        set(
                        //new ButtonSet(ButtonSet.Mode.One,
//                                ToggleButton.awesome("play").on((b) -> {
//                                    if (b) {
//                                        if (pause) {
//                                            pause = false;
//                                            update();
//                                        }
//
//                                    }
//                                }), ToggleButton.awesome("pause").on((b) -> {
//                            if (b) {
//
//                                if (!pause) {
//                                    pause = true;
//                                    update();
//                                }
//                            }
//                        })
//                        ),
                                new CheckBox("On").on(loop.isRunning()).on((o)->{
                                    //synchronized(loop) {
                                        if (o) {
                                            pause = false;
                                            loop.setFPS(fps.intValue());
                                            update(); //HACK shouldnt be needed
                                        } else {
                                            pause = true;
                                            loop.stop();
                                            update();  //HACK maybe necessary
                                        }
                                    //}
                                }
                                //)
                        ),
                        fpsLabel, 
                        cycleTimePlot,
                        heapPlot
                );
    }

    final AtomicBoolean busy = new AtomicBoolean(false); //HACK
    public void update() {
        if (busy.compareAndSet(false, true)) {
            if (!pause) {
                int f = fps.intValue();
                int g = Math.round(loop.getFPS());
                if (f > 0) {
                    if (f != g) {
                        loop.setFPS(f);
                        fpsLabel.set(f);
                    }
                } else {
                    fps.set(g);
                    fpsLabel.set(g);
                }
                cycleTimePlot.commit();
                heapPlot.commit();
            } else {
                if (loop.isRunning()) {

                    loop.stop();
                    fpsLabel.set(0);
                }

            }

            busy.set(false);
        }

    }
}
