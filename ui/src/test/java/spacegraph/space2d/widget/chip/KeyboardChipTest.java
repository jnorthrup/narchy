package spacegraph.space2d.widget.chip;

import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.widget.port.IntPort;

class KeyboardChipTest {

    public static void main(String[] args) {

        EditGraph2D w = EditGraph2D.window(1000, 1000);
        w.add(new Cluster2DChip()).pos(100, 100, 300, 300);
        w.add(new KeyboardChip.ArrowKeysChip()).pos(300, 300, 500, 500);
        w.add(new IntPort()).pos(300, 300, 500, 500);


    }
}