package spacegraph.test;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.widget.port.SupplierPort;
import spacegraph.video.VideoSurface;
import spacegraph.video.WebCam;

public class SignalGraphTest {

    public static void main(String[] args) {

        GraphEdit2D w = GraphEdit2D.window(1024, 1024);

        for (WebCam c : WebCam.theFirst(10)) {
            w.add(new SupplierPort(c.toString(), Surface.class, () -> {
                return new VideoSurface(c);
                ///return new PushButton("cam1");
            })).posRel(0, 0, 0.25f, 0.2f);
        }
//        for (AudioSource a : AudioSource.all()) {
//            w.add(new SupplierPort(a.toString(), Surface.class, ()->new SignalView(
//                    new SignalInput().set(a, a.sampleRate(),1)
//            ))).posRel(0, 0, 0.25f, 0.2f);
//        }
    }
}
