package spacegraph.space2d.widget.meta;

import spacegraph.SpaceGraph;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;

class MetaFrameTest {

    public static void main(String[] args) {

        SpaceGraph.surfaceWindow(new Scale(new MetaFrame(new PushButton("x")), 0.5f), 800, 800);
    }
}