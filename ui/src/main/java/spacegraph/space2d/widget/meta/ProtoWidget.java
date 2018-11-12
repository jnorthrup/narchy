package spacegraph.space2d.widget.meta;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jogamp.opengl.GL2;
import jcog.User;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.TensorChain;
import jcog.signal.wave1d.HaarWaveletTensor;
import jcog.signal.wave1d.SlidingDFTTensor;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.chip.*;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.menu.view.GridMenuView;
import spacegraph.space2d.widget.port.*;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * widget which can become "anything"
 */
public class ProtoWidget extends Bordering {

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

        add("Keyboard", KeyboardChip::new, "Input");
        add("ArrowKeys", KeyboardChip.ArrowKeysChip::new, "Input");
        add("QwertyPiano", TODO, "Input");
        add("Mouse", TODO, "Input");
        add("Gamepad", TODO, "Input");

        add("WebCam", () -> {
            return new WebcamChip();
        }, "Video");
        add("Microphone", AudioCaptureChip::new, "Audio");

        add("int", IntPort::new, "Value");
        add("float", FloatPort::new, "Value");
        add("text", TextPort::new, "Value");
        add("float[-1..1]", ()->new FloatRangePort(0.5f, -1, 1f), "Value");
        add("float[0..1]", ()->new FloatRangePort(0.5f, 0, 1f), "Value");

//        add("random float[0..1]", TODO, "Noise");
//        add("random float[-1..+1]", TODO, "Noise");


        add("ColorChoose", TODO, "Video");
        add("Recognizer", TODO, "Video");
        add("Tracker", TODO, "Video");
        add("Snapshotter", TODO, "Video");
        add("Vectorize", TODO, "Video");
        add("ShapeDetect", TODO, "Video");

        add("split", TODO, "Tensor");
        add("concat", ()->new BiFunctionChip<Tensor, Tensor,Tensor>((Tensor a, Tensor b) -> {
            return TensorChain.get(a, b);
        }), "Tensor");
        add("OneHotBit", ()-> new BiFunctionChip<>((Integer signal, Integer range) -> {
            //TODO optimize with a special Tensor impl
            if (signal >= 0 && signal < range) {
                float[] x = new float[range];
                x[signal] = 1;
                return new ArrayTensor(x);
            } else {
                return null;
            }
        }), "Tensor");
        add("HaarWavelet", ()->new FunctionChip<>((Tensor t)->{
            return new HaarWaveletTensor(t, 64);
        }).buffered(), "Tensor");
        add("SlidingDFT", ()->new FunctionChip<>((Tensor t)->{
            return new SlidingDFTTensor(t, 64);
        }).buffered(), "Tensor");


        add("mix", TODO, "Audio");
        add("EQ", TODO, "Audio");
        add("play", TODO, "Audio");
        add("sonify", TODO, "Audio");

        add("MLP", TODO, "Control");
        add("QLearn", TODO, "Control");
        add("PID", TODO, "Control");

        add("Text", LabeledPort::generic, "Meter");
        add("Plot", PlotChip::new, "Meter");
        add("MatrixView", ()-> new MatrixViewChip(), "Meter");
        add("Cluster2D", Cluster2DChip::new, "Meter");





        add("Geo", TODO, "Reality");
        add("Weather", TODO, "Reality");

        add("File", TODO, "Data"); //and directory too
        add("CSV", TODO, "Data");
        add("ARFF", TODO, "Data");
        add("SQL", TODO, "Data");
        add("SSH", TODO, "Data");
        add("RDP", TODO, "Data");
        add("HTTP", TODO, "Data");
        add("FTP", TODO, "Data");

        add("Shell", TODO, "Data");

    }};

    public ProtoWidget() {
        this(LIBRARY);
    }

    private ProtoWidget(WidgetLibrary library) {
        super();

        Map<String,Supplier<Surface>> categories = new TreeMap<>();
        //categories.put("...", OmniBox::new);
        library.byTag.asMap().forEach((t,v)->{
            Surface[] fields = v.stream()
                    .map(x -> becoming(x.getOne(), x.getTwo()))
                    .toArray(Surface[]::new);
            categories.put(t, () -> new Widget(new LabeledPane(new VectorLabel(t) {
                {  textColor.set(1,1,1,1);   }

                @Override
                protected void paintIt(GL2 gl, SurfaceRender r) {
                    gl.glColor3f(0,0, 0);
                    Draw.rect(bounds, gl);
                }
            }, new Gridding( fields )) ));
        });

        set(new TabMenu(categories, new GridMenuView(), (l)->{
            String icon;
            switch (l) {
                case "Control":
                    icon = "cogs";
                    break;
                case "Audio":
                    icon = "volume-up";
                    break;

                case "Signal":
                    icon = "sliders";
                    break;
                case "Data":
                    icon = "bar-chart-o";
                    break;
                case "Reality":
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

        User u = new User();
        library.byName.forEach((t,v)->{
           u.put(t, t);
        });

        set(N, new OmniBox(new OmniBox.LuceneQueryModel(u) {

        }));

    }


    private PushButton becoming(String label, Supplier<Surface> replacement) {
        return new PushButton(label,
                () -> parent(WizardFrame.class).replace(ProtoWidget.this,
                        replacement.get()));
    }

}
