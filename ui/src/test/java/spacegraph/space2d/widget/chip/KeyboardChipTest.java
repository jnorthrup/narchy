package spacegraph.space2d.widget.chip;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.widget.port.IntPort;

class KeyboardChipTest {

    public static void main(String[] args) {

        GraphEdit2D w = GraphEdit2D.window(1000, 1000);
        RectFloat r2 = RectFloat.XYXY((float) 100, (float) 100, (float) 300, (float) 300);
        ((Surface) w.add(new Cluster2DChip())).pos(r2);
        RectFloat r1 = RectFloat.XYXY((float) 300, (float) 300, (float) 500, (float) 500);
        ((Surface) w.add(new KeyboardChip.ArrowKeysChip())).pos(r1);
        RectFloat r = RectFloat.XYXY((float) 300, (float) 300, (float) 500, (float) 500);
        ((Surface) w.add(new IntPort())).pos(r);


    }
}