package spacegraph.space2d.widget.chip;

import jcog.event.Off;
import jcog.signal.Tensor;
import jcog.signal.wave2d.RGBBufImgBitmap2D;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.video.VideoSource;
import spacegraph.video.VideoSurface;
import spacegraph.video.WebCam;

import java.util.function.Consumer;

public class WebcamChip extends Bordering {
    private Off on;
    final VideoSource wc = WebCam.the();
    final TypedPort<Tensor> out = new TypedPort<>(Tensor.class);
    final CheckBox enable = new CheckBox("enable");

    {
        enable.on(true);
    }

    @Override
    protected void starting() {
        set(new VideoSurface(wc));
        set(S, new Gridding(enable, LabeledPane.awesome(out, "play")  /*, device select, ... framerate, */));
        on = wc.tensor.on(new Consumer<RGBBufImgBitmap2D>() {
            @Override
            public void accept(RGBBufImgBitmap2D x) {
                if (enable.on() && out.active()) {
                    //out.out(x);
                    out.out(wc.tensor);
                }
            }
        });
        super.starting();
    }

    @Override
    protected void stopping() {
        on.close();
        on = null;
        super.stopping();
    }
}
