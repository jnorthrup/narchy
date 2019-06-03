package spacegraph.space2d;

import jcog.constraint.continuous.BoundVar;
import jcog.constraint.continuous.C;
import jcog.constraint.continuous.ContinuousConstraintSolver;
import jcog.constraint.continuous.DoubleVar;
import jcog.math.v2;

/** Untested */
public class SurfaceConstraints {

    final ContinuousConstraintSolver constraints = new ContinuousConstraintSolver();

    public void scale(DoubleVar target, float scale, DoubleVar source) {
        constraints.add(C.equals(target, C.multiply(source, scale)));
    }

    public void scale(Surface target, v2 scale, Surface source) {
        constraints.add(C.equals(new SurfaceVar(target, SurfaceFeature.W),
                C.multiply(new SurfaceVar(source,SurfaceFeature.W) {
                    @Override
                    public void save() {
                    }
                }, scale.x)));
        constraints.add(C.equals(new SurfaceVar(target, SurfaceFeature.H),
                C.multiply(new SurfaceVar(source,SurfaceFeature.H) {
                    @Override
                    public void save() {
                    }
                }, scale.y)));
    }

    public enum SurfaceFeature {
        W {
            @Override
            double get(Surface s) {
                return s.w();
            }

            @Override
            void set(Surface s, double next) {
                s.resize((float) next, s.h());
            }
        },
        H {
            @Override
            double get(Surface s) {
                return s.h();
            }

            @Override
            void set(Surface s, double next) {
                s.resize(s.w(), (float) next);
            }

        };

        abstract double get(Surface s);
        abstract void set(Surface s, double next);
    }

    static class SurfaceVar extends BoundVar<Surface> {

        private final Surface surface;
        private final SurfaceFeature feature;

        public SurfaceVar(Surface sfc, SurfaceFeature feature) {
            super("Surface(" + sfc.id + ")." + feature);
            this.surface = sfc;
            this.feature = feature;
            load();
        }

        @Override
        protected double get() {
            return feature.get(surface);
        }

        @Override
        protected void set(double next) {
            feature.set(surface, next);
        }
    }

    public void update() {
        constraints.vars.keySet().forEach(v -> {
            if (v instanceof BoundVar) {
                ((BoundVar)v).load();
            }
        });
        constraints.update();
        constraints.vars.keySet().forEach(v -> {
            if (v instanceof BoundVar) {
                ((BoundVar)v).save();
            }
        });
    }
}
