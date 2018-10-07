package spacegraph.space2d.widget.text;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Stacking;
import spacegraph.video.ImageTexture;

public class LabeledPane extends Splitting {

    public LabeledPane(String label, Surface content) {
        this(new VectorLabel(label), content);
    }

    public LabeledPane(Surface label, Surface content) {
        super(label, content, 0.9f);
    }

    public static Surface awesome(Surface x, String icon) {
        return new Stacking(x, ImageTexture.awesome(icon).view(1));
    }
}
