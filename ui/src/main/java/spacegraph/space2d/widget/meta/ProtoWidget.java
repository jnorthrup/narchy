package spacegraph.space2d.widget.meta;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.math.IntRange;
import jcog.math.random.XoRoShiRo128PlusRandom;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.IconToggleButton;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.windo.FloatPort;
import spacegraph.space2d.widget.windo.LabeledPort;
import spacegraph.space2d.widget.windo.Port;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.video.Draw;
import spacegraph.video.WebCam;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * widget which can become "anything"
 */
public class ProtoWidget extends Widget {

    public static class WidgetLibrary {

        final Map<String,Supplier<Surface>> byName = new HashMap();
        final Multimap<String,Pair<String,Supplier<Surface>>> byTag = HashMultimap.create();


        public WidgetLibrary add(String name, Supplier<Surface> s, String... tags) {
            byName.put(name, s);
            for (String t : tags) {
                byTag.put(t, pair(name, s));
            }
            return this;
        }
    }

    //Surface palette(Surface... )

    static final Supplier<Surface> TODO = () -> new Label("TODO");
    static final WidgetLibrary LIBRARY  = new WidgetLibrary() {{

        add("Keyboard", TODO, "Hardware");
        add("Mouse", TODO, "Hardware");
        add("Gamepad", TODO, "Hardware");
        add("WebCam", ()->new WebCam().view(), "Hardware");
        add("Microphone", ()->{
            {
                WaveCapture au = new WaveCapture(new AudioSource(20));
                au.runFPS(20f);
                return au.view();
            }
        }, "Hardware");

        add("x[0..1]", ()->new FloatPort(0.5f, 0, 1f), "Signal");
        add("Rng", ()->new FloatPort(0.5f, 0, 1f), "Signal");
        add("Wave", TODO, "Signal");

        add("Split", TODO, "Signal");
        add("Mix", TODO, "Signal");
        add("EQ", TODO, "Signal");

        add("QLearn", TODO, "Control");
        add("PID", TODO, "Control");

        add("Text", LabeledPort::generic, "See");
        add("Plot", PlotChip::new, "See");
        add("Cluster2D", Cluster2DChip::new, "See");
        add("Color", TODO, "See");

        add("Audio", TODO, "Hear");
        add("Sonify", TODO, "Hear");

        add("Geo", TODO, "Reality");
        add("Weather", TODO, "Reality");

        add("Files", TODO, "Connect");
        add("SSH", TODO, "Connect");
        add("RDP", TODO, "Connect");
        add("WWW", TODO, "Connect");

    }};

    public ProtoWidget() {
        this(LIBRARY);
    }

    public ProtoWidget(WidgetLibrary library) {
        super();

        Map<String,Supplier<Surface>> categories = new HashMap<>();
        categories.put("...", OmniBox::new);
        library.byTag.asMap().forEach((t,v)->{
            Surface[] fields = v.stream()
                    .map(x -> becoming(x.getOne(), x.getTwo()))
                    .toArray(Surface[]::new);
            categories.put(t, ()->new Gridding( fields ) );
        });

        content(new TabPane(ButtonSet.Mode.Multi, categories, (l)->{
            String icon;
            switch (l) {
                case "Control":
                    icon = "cogs";
                    break;
                case "Hear":
                    icon = "volume-up";
                    break;

                case "Signal":
                    icon = "sliders";
                    break;
                case "See":
                    icon = "bar-chart-o";
                    break;
                case "Hardware":
                    icon = "wrench";
                    break;
                default:
                    icon = null;
                    break;
            }

            if (icon!=null) {
                return IconToggleButton.awesome(icon );
            } else {
                return new CheckBox(l);
            }
        }));

    }


    PushButton becoming(String label, Supplier<Surface> replacement) {
        return new PushButton(label,
                () -> ((MutableContainer) parent).replace(ProtoWidget.this,
                        replacement.get()));
    }

    public static class PlotChip extends Gridding {
        final Port in;
        private final Plot2D plot;
        double nextValue = Double.NaN;

        public PlotChip() {
            super();

            this.plot = new Plot2D(256, Plot2D.Line);
            plot.add("x", ()->nextValue);

            this.in = new Port().on((Float x)->{
                nextValue = x;
                plot.update();
            });

            set(in, plot);


        }
    }
    public static class Cluster2DChip extends Gridding {

        private final Port in;
        private final Surface display;

        Autoencoder ae;
        NeuralGasNet g;
        class Config {
            public final IntRange clusters = new IntRange(16, 2, 32);

            synchronized void reset(int dim) {
                if (ae == null || ae.inputs()!=dim) {
                    g = new NeuralGasNet(dim, clusters.intValue(), Centroid.DistanceFunction::distanceCartesianManhattan);
                    ae = new Autoencoder(dim, 2, new XoRoShiRo128PlusRandom(1));
                }
            }
        }

        final Config config = new Config();

        public Cluster2DChip() {
            super();

            config.reset(2);

            in = new Port().on((float[] x)->{
                synchronized (g) {
                    config.reset(x.length);
                    g.put(Util.toDouble(x));//Util.toDouble(ae.y));
                }
            });
            display = new Surface() {

                @Override
                protected void paint(GL2 gl, int dtMS) {
                    Draw.bounds(gl, bounds, this::paint);
                }

                protected void paint(GL2 gl) {
                    synchronized (g) {
                        NeuralGasNet g = Cluster2DChip.this.g;
//                        double x1 = Double.POSITIVE_INFINITY;
//                        double x2 = Double.NEGATIVE_INFINITY;
//                        double y1 = Double.POSITIVE_INFINITY;
//                        double y2 = Double.NEGATIVE_INFINITY;

//                        double x1 = 0, y1 = 0, x2 = 1, y2 = 1;
                        float cw = 0.1f;
                        float ch = 0.1f;
                        for (Centroid c : g.centroids) {
                            float a = (float) (1.0 / (1 + c.localError()));
                            ae.put(Util.toFloat(c.getDataRef()), a * 0.05f, 0.001f, 0, false);
                            float x = //c.getEntry(0);
                                    0.5f*(1+ae.y[0]);
//                            x1 = Math.min(x, x1);
//                            x2 = Math.max(x, x2);
                            float y = //c.getEntry(1);
                                    0.5f*(1+ae.y[1]);
//                            y1 = Math.min(y, y1);
//                            y2 = Math.max(y, y2);
//                        }
//                        for (Centroid c : g.centroids) {
//                            float x = (float) Util.normalize(c.getEntry(0), x1, x2);
//                            float y = (float) Util.normalize(c.getEntry(1), y1, y2);

                            Draw.colorHash(gl, c.id, a);
                            Draw.rect(gl, x-cw/2, y-ch/2, cw, ch);
                        }
                    }
                }

            };
            set(in, new AutoSurface(config), display);
        }
    }
}
