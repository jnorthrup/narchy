package spacegraph.widget.text;

import spacegraph.Surface;
import spacegraph.layout.Splitting;
import spacegraph.widget.windo.Widget;

public class LabeledPane extends Widget {

    public LabeledPane(String title, Surface content) {
        super();
        children(new Splitting(new Label(title), content, 0.9f));
    }



}
