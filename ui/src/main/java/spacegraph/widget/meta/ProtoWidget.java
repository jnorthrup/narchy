package spacegraph.widget.meta;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.Surface;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.container.Gridding;
import spacegraph.container.MutableContainer;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.meter.WebCam;
import spacegraph.widget.tab.TabPane;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.FloatPort;
import spacegraph.widget.windo.LabeledPort;
import spacegraph.widget.windo.Widget;

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
        add("Plot", TODO, "See");
        add("Color", TODO, "See");

        add("Audio", TODO, "Hear");
        add("Tonify", TODO, "Hear");


    }};

    public ProtoWidget() {
        this(LIBRARY);
    }

    public ProtoWidget(WidgetLibrary library) {
        super();

        Map<String,Supplier<Surface>> categories = new HashMap();
        categories.put("...", OmniBox::new);
        library.byTag.asMap().forEach((t,v)->{
            Surface[] fields = v.stream()
                    .map(x -> becoming(x.getOne(), x.getTwo()))
                    .toArray(Surface[]::new);
            categories.put(t, ()->new Gridding( fields ) );
        });

        content(new TabPane(categories));

    }


    PushButton becoming(String label, Supplier<Surface> replacement) {
        return new PushButton(label,
                () -> ((MutableContainer) parent).replace(ProtoWidget.this,
                        replacement.get()));
    }

}
