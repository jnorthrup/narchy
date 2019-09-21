package spacegraph.space2d.widget;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.widget.chip.Cluster2DChip;
import spacegraph.space2d.widget.chip.NoiseVectorChip;

class Cluster2DChipTest {

    public static void main(String... args) {
        GraphEdit2D w = GraphEdit2D.graphWindow(1000, 1000);
        RectFloat r1 = RectFloat.XYXY((float) 100, (float) 100, (float) 300, (float) 300);
        ((Surface) w.add(new Cluster2DChip())).pos(r1);
        RectFloat r = RectFloat.XYXY((float) 300, (float) 300, (float) 500, (float) 500);
        ((Surface) w.add(new NoiseVectorChip())).pos(r);

    }
}