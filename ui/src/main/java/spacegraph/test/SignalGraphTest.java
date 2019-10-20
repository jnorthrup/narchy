package spacegraph.test;

import spacegraph.input.finger.impl.WebcamGestures;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.widget.port.ConstantPort;
import spacegraph.space2d.widget.port.Surplier;
import spacegraph.video.VideoSource;
import spacegraph.video.VideoSurface;
import spacegraph.video.VideoTransform;
import spacegraph.video.WebCam;

import java.util.function.Function;

public class SignalGraphTest {

    public static class VideoTransformPort extends Bordering {

        private final ConstantPort<VideoSource> out;

        private transient VideoTransform y;

        public VideoTransformPort(Function<VideoSource,VideoTransform> t) {
            super();
            var in = new ConstantPort<VideoSource>(VideoSource.class);
            out = new ConstantPort<>(VideoSource.class);
            west(in);
            east(out);
            //in.update((xx)->{
            in.on((x)->{

                if (this.y!=null) {
                    try {
                        y.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    y = null;
                }

                if (x!=null) {
                    this.y = t.apply(x);
                    center(new VideoSurface(y));
                } else {
                    center(new EmptySurface());
                }

                out.out(y);
            });
        }
    }

    public static void main(String[] args) {

        var w = GraphEdit2D.graphWindow(1024, 1024);

        for (var c : WebCam.theFirst(10)) {
            w.add(new Surplier(c.toString(), Surface.class, () -> {
                return Splitting.row(new ConstantPort(c), 0.1f, new VideoSurface(c));
                ///return new PushButton("cam1");
            })).posRel(0, 0, 0.25f, 0.2f);
        }
        w.add(new VideoTransformPort(WebcamGestures.VideoBackgroundRemoval::new)).posRel(0,0, 0.1f, 0.1f);
//        for (AudioSource a : AudioSource.all()) {
//            w.add(new SupplierPort(a.toString(), Surface.class, ()->new SignalView(
//                    new SignalInput().set(a, a.sampleRate(),1)
//            ))).posRel(0, 0, 0.25f, 0.2f);
//        }
    }
}
