package spacegraph.space2d;

import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.button.PushButton;

public class BorderingTest {

    public static void main(String[] args) {
        SpaceGraph.surfaceWindow(new Bordering()
                
                .south(PushButton.awesome("times"))
                .east(PushButton.awesome("times"))
                .west(PushButton.awesome("times"))
        , 800, 800);
    }
}
