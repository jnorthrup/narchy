package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.video.Draw;

import java.util.Map;

import static spacegraph.SpaceGraph.window;

class ScrollGridTest {

    static class ScrollGridTest1 {
        public static void main(String[] args) {

            GridModel<String> model = new GridModel<>() {

                @Override
                public String get(int x, int y) {
                    return x + "," + y;
                }

                @Override
                public int cellsX() {
                    return 64;
                }

                @Override
                public int cellsY() {
                    return 64;
                }
            };

            ScrollGrid<String> grid = new ScrollGrid<>(model,
                    (x, y, s) -> {
                        if (Math.random() < 0.5f) {
                            Surface p = new PushButton(s) {
                                @Override
                                protected void paintWidget(GL2 gl, RectFloat2D bounds) {
                                    Draw.colorHash(gl, x ^ y, 0.2f, 0.3f, 0.85f);
                                    Draw.rect(bounds, gl);
                                }
                            };
                            return new Widget(p);
                        } else {
                            return new VectorLabel(s);
                        }
                    }, 8, 4);


            grid.setScrollBar(true, true, false);
            grid.setScrollBar(false, false, true);

            window(grid, 1024, 800);
        }
    }
    static class ListTest1 {
        public static void main(String[] args) {
            String[] list = {"a", "b", "c", "d", "e", "f"};
            ScrollGrid.GridRenderer<String> builder = (x, y, n) -> {
                System.out.println("constructing: " + n);
                return new CheckBox(n);
            } ;
            SpaceGraph.window(
                    ScrollGrid.array(builder, list)
                    , 800, 800);
        }

    }

    static class MapTest1 {
        public static void main(String[] args) {
            SpaceGraph.window(
                new ScrollGrid<>(
                        new KeyValueModel(
                            Map.of("wtf", "ok", "sdfj", "xcv", "sdf", "fdfs")
                        ),
                        (x, y, n)->
                            x == 0 ? new VectorLabel(n.toString()) : new CheckBox(n.toString()))
                , 800, 800);
        }
    }

}