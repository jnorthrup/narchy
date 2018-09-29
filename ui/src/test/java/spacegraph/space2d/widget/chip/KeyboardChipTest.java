package spacegraph.space2d.widget.chip;

import spacegraph.space2d.widget.WallTest;
import spacegraph.space2d.widget.windo.GraphEdit;

import static org.junit.jupiter.api.Assertions.*;

class KeyboardChipTest {

    public static void main(String[] args) {

        GraphEdit w = WallTest.newWallWindow();
        w.add(new Cluster2DChip()).pos(100, 100, 300, 300);
        w.add(new KeyboardChip()).pos(300, 300, 500, 500);


    }
}