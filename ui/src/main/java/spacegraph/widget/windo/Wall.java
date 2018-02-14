package spacegraph.widget.windo;

import org.jbox2d.dynamics.Dynamics2D;
import spacegraph.Ortho;
import spacegraph.Scale;
import spacegraph.Surface;
import spacegraph.SurfaceBase;
import spacegraph.layout.Stacking;
import spacegraph.math.v2;
import spacegraph.phys.util.Animated;

/**
 * a wall (virtual surface) contains zero or more windows;
 * anchor region for Windo's to populate
 * <p>
 * TODO move active window to top of child stack
 */
public class Wall extends Stacking implements Animated {

    private final Dynamics2D w;

    //final ContinuousConstraintSolver model = new ContinuousConstraintSolver();

    public Wall() {

        clipTouchBounds = false;

        w = new Dynamics2D(new v2(0,0));
        w.setParticleRadius(0.2f);
        w.setParticleDensity(1.0f);
        w.setContinuousPhysics(true);
        w.setAllowSleep(true);
    }

    @Override
    public void start(SurfaceBase parent) {
        synchronized (this) {
            super.start(parent);
            ((Ortho)root()).onUpdate(this);
        }
    }

    @Override
    public boolean animate(float dt) {
        w.step(dt, 2, 2);
        return true;
    }

    @Override
    public void stop() {
        synchronized (this) {
            super.stop();
        }
    }

    //    public static class P2 {
//        final DoubleVar x, y;
//
//        public P2(String id) {
//            this.x = new DoubleVar(id + ".x");
//            this.y = new DoubleVar(id + ".y");
//        }
//    }

    //protected final Map<String, CRectFloat2D> rects = new ConcurrentHashMap();




    @Override
    public void doLayout(int dtMS) {
        //super.doLayout();

        //model.update();

        for (Surface c : children())
            c.layout();
    }

    public Windo addWindo() {
        Windo w = new Windo();
        add(w);
        return w;
    }

    public Windo addWindo(Surface content) {
        Windo w = addWindo();
        w.children(new Scale(content, 1f - Windo.resizeBorder));
        return w;
    }



}
//    public class CRectFloat2D {
//
//        final String id;
//        final Map<String, ContinuousConstraint> constraints = new LinkedHashMap();
//        private final DoubleVar X, Y, W, H;
//
//        public CRectFloat2D(String id) {
//            this(id, 0, 0, 1, 1);
//        }
//
//        public CRectFloat2D(String id, float x1, float y1, float x2, float y2) {
//            //super(x1, y1, x2, y2);
//            this.id = id;
//            this.X = new DoubleVar(id + ".x");
//            this.Y = new DoubleVar(id + ".y");
//            this.W = new DoubleVar(id + ".w");
//            this.H = new DoubleVar(id + ".h");
//            set(x1, y1, x2, y2);
//            rects.put(id, this);
//        }
//
//        public void set(RectFloat2D r) {
//            set(r.x, r.y, r.right(), r.bottom());
//        }
//
//        public void set(float x1, float y1, float x2, float y2) {
//            X.value(0.5f*(x1+x2));
//            Y.value(0.5f*(y1+y2));
//            W.value(x2-x1);
//            H.value(y2-y1);
//
//            layout(); //TODO only trigger layout if significantly changed
//        }
//
//        public void delete() {
//            if (rects.remove(id) == this) {
//                synchronized (constraints) {
//                    constraints.values().forEach(model::remove);
//                    constraints.clear();
//                }
//            }
//        }
//
//        public void remove(String id) {
//            synchronized (constraints) {
//                ContinuousConstraint previous = constraints.remove(id);
//                if (previous != null)
//                    model.remove(previous);
//            }
//        }
//
//        public void add(String id, ContinuousConstraint c) {
//            synchronized (constraints) {
//                ContinuousConstraint previous = constraints.put(id, c);
//                if (previous != null)
//                    model.remove(previous);
//                model.add(c);
//            }
//        }
//
//    }
//
//    public class CSurface extends Stacking {
//
//        private final CRectFloat2D cbounds;
//
//        public CSurface(String id) {
//            super();
//            this.cbounds = new CRectFloat2D(id);
//        }
////
////        /** affects internal from external action */
////        @Override public Surface pos(RectFloat2D r) {
////            cbounds.set(r);
////            //super.pos(r);
////            layout();
////            return null;
////        }
//
//        /** affects external from internal action
//         * @param dtMS*/
//        @Override public void doLayout(int dtMS) {
//
//            float xx = cbounds.X.floatValue();
//            float yy = cbounds.Y.floatValue();
//            float ww = cbounds.W.floatValue();
//            float hh = cbounds.H.floatValue();
//            super.pos(xx - ww / 2, yy - hh / 2, xx + ww / 2, yy + hh / 2);
//
//            super.doLayout(dtMS);
//        }
//    }

//    public CSurface newCurface(String id) {
//        return new CSurface(id);
//    }

//    public P2 varPoint(String id) {
//        return new P2(id);
//    }
