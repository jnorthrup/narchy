package spacegraph.widget.text;

import spacegraph.Surface;
import spacegraph.layout.VSplit;
import spacegraph.widget.windo.Widget;

public class LabeledPane extends Widget {

    public LabeledPane(String title, Surface content) {
        super();
        children(new VSplit(new Label(title), content, 0.9f));
    }



}
