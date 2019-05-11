package spacegraph.input.finger.impl;

import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.LazySurface;
import spacegraph.video.OrthoSurfaceGraph;
import spacegraph.video.VideoSource;
import spacegraph.video.VideoSurface;
import spacegraph.video.WebCam;

import static spacegraph.SpaceGraph.window;

public class WebcamStereoTest {
    public static void main(String[] args) {

        VideoSource[] ab = WebCam.theFirst(2);

        OrthoSurfaceGraph g = window(new LazySurface(() -> {
            return new Gridding(
                    new VideoSurface(ab[0]),
                    new VideoSurface(ab[1])
            );
        }), 1400, 800);

    }

}
