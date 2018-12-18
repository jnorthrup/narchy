package spacegraph.space2d.widget.meta;

import spacegraph.space2d.widget.button.PushButton;

import static spacegraph.SpaceGraph.window;

class MetaFrameTest {

    public static void main(String[] args) {
        window(new MetaFrame(new PushButton("x")), 800, 800);
    }
}