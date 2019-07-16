package spacegraph.space2d.widget.text;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Stacking;
import spacegraph.video.ImageTexture;

public class LabeledPane extends Splitting {

    public LabeledPane(Surface label, Surface content) {
        super(label, 0.9f, content);
    }

    public static Surface awesome(Surface x, String icon) {
        return new Stacking(x, ImageTexture.awesome(icon).view(1));
    }

    public static Surface the(String label, Surface content) {
        assert(content!=null);

        if (label.isEmpty())
            return content;

        //.trim() ?

        return new LabeledPane(new BitmapLabel(label), content);
    }

}
