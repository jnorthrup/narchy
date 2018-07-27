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
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.meter.Plot2D;
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


        WidgetLibrary add(String name, Supplier<Surface> s, String... tags) {
            byName.put(name, s);
            for (String t : tags) {
                byTag.put(t, pair(name, s));
            }
            return this;
        }
    }

    

    private static final Supplier<Surface> TODO = () -> new Label("TODO");
    private static final WidgetLibrary LIBRARY  = new WidgetLibrary() {{

        add("Keyboard", TODO, "Hardware");
        add("Mouse", TODO, "Hardware");
        add("Gamepad", TODO, "Hardware");
        add("WebCam", () -> new WebCam.WebCamSurface(WebCam.the()), "Hardware");
        add("Microphone", ()->{
            {
                WaveCapture au = new WaveCapture(new AudioSource(20));
                au.setFPS(20f);
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

    private ProtoWidget(WidgetLibrary library) {
        super();

        Map<String,Supplier<Surface>> categories = new HashMap<>();
        categories.put("...", OmniBox::new);
        library.byTag.asMap().forEach((t,v)->{
            Surface[] fields = v.stream()
                    .map(x -> becoming(x.getOne(), x.getTwo()))
                    .toArray(Surface[]::new);
            categories.put(t, ()->new Gridding( fields ) );
        });

        content(new TabPane(categories, (l)->{
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
                return ToggleButton.awesome(icon );
            } else {
                return new CheckBox(l);
            }
        }));

    }


    private PushButton becoming(String label, Supplier<Surface> replacement) {
        return new PushButton(label,
                () -> ((MutableContainer) parent).replace(ProtoWidget.this,
                        replacement.get()));
    }

    static class PlotChip extends Gridding {
        final Port in;
        private final Plot2D plot;
        double nextValue = Double.NaN;

        PlotChip() {
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
    static class Cluster2DChip extends Gridding {

        private final Port in;
        private final Surface display;

        Autoencoder ae;
        NeuralGasNet g;
        class Config {
            final IntRange clusters = new IntRange(16, 2, 32);

            synchronized void reset(int dim) {
                if (ae == null || ae.inputs()!=dim) {
                    g = new NeuralGasNet(dim, clusters.intValue(), Centroid.DistanceFunction::distanceCartesianManhattan);
                    ae = new Autoencoder(dim, 2, new XoRoShiRo128PlusRandom(1));
                }
            }
        }

        final Config config = new Config();

        Cluster2DChip() {
            super();

            config.reset(2);

            in = new Port().on((float[] x)->{
                synchronized (g) {
                    config.reset(x.length);
                    g.put(Util.toDouble(x));
                }
            });
            display = new Surface() {

                @Override
                protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                    Draw.bounds(bounds, gl, this::paint);
                }

                void paint(GL2 gl) {
                    synchronized (g) {
                        NeuralGasNet g = Cluster2DChip.this.g;






                        float cw = 0.1f;
                        float ch = 0.1f;
                        for (Centroid c : g.centroids) {
                            float a = (float) (1.0 / (1 + c.localError()));
                            ae.put(Util.toFloat(c.getDataRef()), a * 0.05f, 0.001f, 0, false);
                            float x = 
                                    0.5f*(1+ae.y[0]);


                            float y = 
                                    0.5f*(1+ae.y[1]);







                            Draw.colorHash(gl, c.id, a);
                            Draw.rect(gl, x-cw/2, y-ch/2, cw, ch);
                        }
                    }
                }

            };
            set(in, new ObjectSurface(config), display);
        }
    }
}
