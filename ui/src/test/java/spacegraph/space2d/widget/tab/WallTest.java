package spacegraph.space2d.widget.tab;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.windo.Wall;

public class WallTest {

    public static class TestWallDebugger1 {

        public static void main(String[] args) {

            Wall w = new Wall();
            SpaceGraph.window(
                    new Bordering(w).borderSize(Bordering.S, 0.25f).south(w.debugger()), 1000, 900);

            w.add(new PushButton("X")).pos(RectFloat2D.XYXY(10, 10, 200, 200));
            //Windo ww = w.add(new PushButton("Y"), 200, 300f);
            //System.out.println(ww);

        }
    }

}
