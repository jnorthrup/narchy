package spacegraph.space2d.widget.meta;

import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;

import static spacegraph.SpaceGraph.window;

class MetaFrameTest {

    public static void main(String[] args) {

        window(new Scale(new MetaFrame(new PushButton("x")), 0.5f), 800, 800);
    }
}