package spacegraph.space2d.widget.chip;

import spacegraph.space2d.widget.port.IntPort;
import spacegraph.space2d.widget.windo.GraphEdit;

class KeyboardChipTest {

    public static void main(String[] args) {

        GraphEdit w = GraphEdit.window(1000, 1000);
        w.add(new Cluster2DChip()).pos(100, 100, 300, 300);
        w.add(new KeyboardChip.ArrowKeysChip()).pos(300, 300, 500, 500);
        w.add(new IntPort()).pos(300, 300, 500, 500);


    }
}