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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SignalGraphTest {

    public static class VideoTransformPort extends Bordering {

        private final ConstantPort<VideoSource> out;

        private transient VideoTransform y;

        public VideoTransformPort(Function<VideoSource,VideoTransform> t) {
            super();
            ConstantPort<VideoSource> in = new ConstantPort<VideoSource>(VideoSource.class);
            out = new ConstantPort<>(VideoSource.class);
            west(in);
            east(out);
            //in.update((xx)->{
            in.on(new Consumer<VideoSource>() {
                @Override
                public void accept(VideoSource x) {

                    if (VideoTransformPort.this.y != null) {
                        try {
                            y.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        y = null;
                    }

                    if (x != null) {
                        VideoTransformPort.this.y = t.apply(x);
                        VideoTransformPort.this.center(new VideoSurface(y));
                    } else {
                        VideoTransformPort.this.center(new EmptySurface());
                    }

                    out.out(y);
                }
            });
        }
    }

    public static void main(String[] args) {

        GraphEdit2D w = GraphEdit2D.graphWindow(1024, 1024);

        for (WebCam c : WebCam.theFirst(10)) {
            w.add(new Surplier(c.toString(), Surface.class, new Supplier() {
                @Override
                public Object get() {
                    return Splitting.row(new ConstantPort(c), 0.1f, new VideoSurface(c));
                    ///return new PushButton("cam1");
                }
            })).posRel((float) 0, (float) 0, 0.25f, 0.2f);
        }
        w.add(new VideoTransformPort(WebcamGestures.VideoBackgroundRemoval::new)).posRel((float) 0, (float) 0, 0.1f, 0.1f);
//        for (AudioSource a : AudioSource.all()) {
//            w.add(new SupplierPort(a.toString(), Surface.class, ()->new SignalView(
//                    new SignalInput().set(a, a.sampleRate(),1)
//            ))).posRel(0, 0, 0.25f, 0.2f);
//        }
    }
}
