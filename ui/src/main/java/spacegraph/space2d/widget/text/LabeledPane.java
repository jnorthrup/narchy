package spacegraph.space2d.widget.text;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;

public class LabeledPane extends Splitting {

    public LabeledPane(String title, Surface content) {
        super(new VectorLabel(title), content, 0.9f);
    }

}
