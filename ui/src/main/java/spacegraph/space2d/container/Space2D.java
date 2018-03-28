//package spacegraph.layout;
//
//import spacegraph.SpaceGraph;
//import spacegraph.Surface;
//import spacegraph.math.v3;
//import spacegraph.widget.Widget;
//
//import java.util.List;
//
///**
// * 2D space of shapes, which can be managed and interacted with
// */
//public class Space2D<S extends Surface> extends Layout<S> {
//
//
//    public float repelSpeed = 0.1f;
//    public float attractSpeed = 2f;
//
//    private final float minRepelDist = 0;
//    private final float maxRepelDist = 1000f;
//    private final float attractDist = 1f;
//
//
//    public Space2D(List<S> content) {
//        super();
//        set(content);
//    }
//
//
////    @Override
////    public void transform(GL2 gl) {
////        super.transform(gl);
////
////        layout();
////    }
//
//    @Override
//    public void layout() {
//
//
//        float sx = 1f; //scaleLocal.x;// * globalScale.x;
//        float sy = 1f; //scaleLocal.y;// * globalScale.y;
//
//        List<? extends S> l = children;
//        int lSize = l.size();
//        float lSizeSqrt = (float)Math.sqrt(lSize);
//        for (int i = 0; i < lSize; i++) {
//            l.get(i).scale((sx*0.75f)/lSizeSqrt, (sy*0.75f)/lSizeSqrt);
//        }
//
//
//        for (int i = 0; i < lSize; i++) {
//            Surface x = l.get(i);
//            for (int j = i + 1; j < lSize; j++) {
//                repel(x, l.get(j), repelSpeed, minRepelDist, maxRepelDist, sx/2f, sy/2f);
//            }
//        }
//    }
//    private static void repel(Surface x, Surface y, float speed, float minDist, float maxDist, float sx, float sy) {
//
//        v3 delta = new v3();
//        v3 xp = x.pos;
//        v3 yp = y.pos;
//        delta.sub(xp, yp);
//
//        float len = delta.normalize();
//        len = Math.max(0, len + (x.radius() + y.radius()));
//
//        if ((len <= minDist) || (len >= maxDist))
//            return;
//
//        delta.scale(((speed * speed) / (1 + len * len)) / 2f);
//
//        //experimental
////            if (len > maxDist) {
////                delta.negate(); //attract
////            }
//
//        xp.add(delta, -sx, -sy, sx, sy);
//        //xp.moveDelta(delta, 0.5f);
//        delta.negate();
//        yp.add(delta, -sx, -sy, sx, sy);
//        //yp.moveDelta(delta, 0.5f);
//
//    }
//
//    public static void main(String[] args) {
//        SpaceGraph.window(new Space2D(Widget.widgetDemo().children()) {
//
//
//        }, 900, 700);
//    }
//}
