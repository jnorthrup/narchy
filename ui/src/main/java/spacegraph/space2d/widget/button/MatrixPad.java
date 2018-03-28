//package spacegraph.widget.button;
//
//import jcog.list.FasterList;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import spacegraph.Surface;
//import spacegraph.layout.Layout;
//
//import java.util.List;
//
///**
// * grid of buttons or other widgets arranged in a specificaly sized matrix grid
// */
//public class MatrixPad extends Layout {
//
//    private static final Logger logger = LoggerFactory.getLogger(MatrixPad.class);
//
//    private final int w;
//    private final int h;
//    final Surface[][] components;
//
//    public interface MatrixPadBuilder {
//        Surface newComponent(int x, int  y);
//    }
//
//    public MatrixPad(int w, int h, MatrixPadBuilder builder) {
//        super();
//        this.w = w;
//        this.h = h;
//        components = new Surface[w][h];
//        List<Surface> c = new FasterList(w * h);
//        for (int x = 0; x < w; x++) {
//            for (int y = 0; y < h; y++) {
//                c.add(components[x][y] = builder.newComponent(x, y));
//            }
//        }
//        set(c);
//    }
//
//    @Override
//    public void layout() {
//
//        float sx = 1f/w;
//        float sy = 1f/h;
//        for (int x = 0; x < w; x++) {
//            for (int y = 0; y < h; y++) {
//                Surface s = components[x][y];
//                s.scale.set(sx, sy);
//                s.pos.set(x * sx, y * sy);
//            }
//        }
//    }
//
//
//
//}
