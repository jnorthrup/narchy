package spacegraph.space2d.widget.windo.util;

import jcog.math.v2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.FingerRenderer;

public enum DragEdit {
    MOVE {
        @Override
        public FingerRenderer cursor() {
            //cursor.put(DragEdit.MOVE, new FingerRenderer.PolygonCrosshairs().angle(45)); //TODO something special
            return null;
        }
    },
    RESIZE_N {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow(90.0F);
        }
    }, RESIZE_E {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow((float) 0);
        }
    }, RESIZE_S {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow(-90.0F);
        }
    }, RESIZE_W {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow(180.0F);
        }
    },
    RESIZE_NW {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow((float) (45 + 90));
        }
    },
    RESIZE_SW {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow((float) (45 + 180));
        }
    },
    RESIZE_NE {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow(45f);
        }
    },
    RESIZE_SE {
        @Override
        public FingerRenderer cursor() {
            return new FingerRenderer.PolygonWithArrow(-45.0F);
        }
    },
    ;


    public static @Nullable DragEdit mode(v2 p, float margin) {
        DragEdit m = null;
        if (p.x >= 0.5f - margin / 2f && p.x <= 0.5f + margin / 2.0F) {
            if (p.y <= margin) {
                m = DragEdit.RESIZE_S;
            }
            if (m == null && p.y >= 1f - margin) {
                m = DragEdit.RESIZE_N;
            }
        }

        if (m == null && p.y >= 0.5f - margin / 2f && p.y <= 0.5f + margin / 2.0F) {
            if (p.x <= margin) {
                m = DragEdit.RESIZE_W;
            }
            if (m == null && p.x >= 1f - margin) {
                m = DragEdit.RESIZE_E;
            }
        }

        if (m == null && p.x <= margin) {
            if (p.y <= margin) {
                m = DragEdit.RESIZE_SW;
            }
            if (m == null && p.y >= 1f - margin) {
                m = DragEdit.RESIZE_NW;
            }
        }

        if (m == null && p.x >= 1f - margin) {

            if (p.y <= margin) {
                m = DragEdit.RESIZE_SE;
            }
            if (m == null && p.y >= 1f - margin) {
                m = DragEdit.RESIZE_NE;
            }
        }
        if (m == null && p.inUnit(1.0F -margin))
            return DragEdit.MOVE;
        return m;

    }

    public abstract FingerRenderer cursor();
}
