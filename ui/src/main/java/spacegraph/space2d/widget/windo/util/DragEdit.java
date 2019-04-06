package spacegraph.space2d.widget.windo.util;

import jcog.math.v2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.CursorOverlay;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerRenderer;
import spacegraph.space2d.container.ContainerSurface;

import java.util.EnumMap;

public enum DragEdit {
    MOVE,
    RESIZE_N, RESIZE_E, RESIZE_S, RESIZE_W,
    RESIZE_NW,
    RESIZE_SW,
    RESIZE_NE,
    RESIZE_SE,
    ;



    static final EnumMap<DragEdit, FingerRenderer> cursor = new EnumMap(DragEdit.class);
    static final EnumMap<DragEdit, CursorOverlay> hover = new EnumMap(DragEdit.class);

    static {

        cursor.put(DragEdit.RESIZE_NE, new FingerRenderer.PolygonWithArrow(45f));
        cursor.put(DragEdit.RESIZE_SW, new FingerRenderer.PolygonWithArrow(45+180));
        cursor.put(DragEdit.RESIZE_SE, new FingerRenderer.PolygonWithArrow(-45));
        cursor.put(DragEdit.RESIZE_NW, new FingerRenderer.PolygonWithArrow(45+90));
        cursor.put(DragEdit.RESIZE_N, new FingerRenderer.PolygonWithArrow(90));
        cursor.put(DragEdit.RESIZE_S, new FingerRenderer.PolygonWithArrow(-90));
        cursor.put(DragEdit.RESIZE_E, new FingerRenderer.PolygonWithArrow(0));
        cursor.put(DragEdit.RESIZE_W, new FingerRenderer.PolygonWithArrow(180));
        //cursor.put(DragEdit.MOVE, new FingerRenderer.PolygonCrosshairs().angle(45)); //TODO something special


        cursor.forEach((k,v)-> hover.put(k, new CursorOverlayOnWindow(v)));
    }

    @Nullable public static DragEdit mode(v2 p, float margin) {
        DragEdit m = null;
        if (p.x >= 0.5f - margin / 2f && p.x <= 0.5f + margin / 2) {
            if (p.y <= margin) {
                m = DragEdit.RESIZE_S;
            }
            if (m == null && p.y >= 1f - margin) {
                m = DragEdit.RESIZE_N;
            }
        }

        if (m == null && p.y >= 0.5f - margin / 2f && p.y <= 0.5f + margin / 2) {
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
        if (m == null && p.scale(1-margin).inUnit())
            return DragEdit.MOVE;
        return m;

    }

    @Nullable
    public FingerRenderer cursor() {
        return cursor.get(this);
    }

    @Nullable
    public CursorOverlay hover() {
        return hover.get(this);
    }

    private static class CursorOverlayOnWindow extends CursorOverlay {
        public CursorOverlayOnWindow(FingerRenderer v) {
            super(v);
        }

        @Override
        public boolean update(Finger f) {
            return f.touching() instanceof ContainerSurface;
        }
    }
}
