package spacegraph.space2d.widget.meta;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.chip.Cluster2DChip;
import spacegraph.space2d.widget.chip.PlotChip;
import spacegraph.space2d.widget.port.FloatRangePort;
import spacegraph.space2d.widget.port.LabeledPort;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.VectorLabel;
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

    

    private static final Supplier<Surface> TODO = () -> new VectorLabel("TODO");
    private static final WidgetLibrary LIBRARY  = new WidgetLibrary() {{

        add("Keyboard", TODO, "Hardware");
        add("Mouse", TODO, "Hardware");
        add("Gamepad", TODO, "Hardware");
        add("WebCam", () -> new WebCam.WebCamSurface(WebCam.the()), "Hardware");
        add("Microphone", ()->{
            {
                WaveCapture au = new WaveCapture(new AudioSource(20), 4f);
                au.setFPS(20f);
                return au.view();
            }
        }, "Hardware");

        add("x[0..1]", ()->new FloatRangePort(0.5f, 0, 1f), "Signal");
        add("Rng", ()->new FloatRangePort(0.5f, 0, 1f), "Signal");
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
        //categories.put("...", OmniBox::new);
        library.byTag.asMap().forEach((t,v)->{
            Surface[] fields = v.stream()
                    .map(x -> becoming(x.getOne(), x.getTwo()))
                    .toArray(Surface[]::new);
            categories.put(t, ()->new Gridding( fields ) );
        });

        set(new TabPane(categories, (l)->{
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
                () -> parent(WizardFrame.class).replace(ProtoWidget.this,
                        replacement.get()));
    }

}
