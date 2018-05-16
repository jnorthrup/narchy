package spacegraph.space2d.widget.meta;

import spacegraph.SpaceGraph;

import static org.junit.jupiter.api.Assertions.*;

class OmniBoxTest {

    static class OmniBox_JShell {
        public static void main(String[] args) {
            SpaceGraph.window(new OmniBox(new OmniBox.JShellModel()), 800, 250);
        }
    }
    static class OmniBox_Lucene {
//        public static void main(String[] args) {
//            SpaceGraph.window(new OmniBox(new L()), 800, 250);
//        }
    }
}