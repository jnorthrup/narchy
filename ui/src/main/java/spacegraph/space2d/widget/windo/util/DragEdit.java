package spacegraph.space2d.widget.windo.util;

import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerRenderer;
import spacegraph.input.finger.RenderWhileHovering;
import spacegraph.space2d.widget.windo.Windo;

import java.util.EnumMap;

public enum DragEdit {
    MOVE,
    RESIZE_N, RESIZE_E, RESIZE_S, RESIZE_W,
    RESIZE_NW,
    RESIZE_SW,
    RESIZE_NE,
    RESIZE_SE;

    static final EnumMap<DragEdit, FingerRenderer> cursor = new EnumMap(DragEdit.class);
    static final EnumMap<DragEdit, RenderWhileHovering> hover = new EnumMap(DragEdit.class);

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

        cursor.forEach((k,v)-> hover.put(k, new RenderWhileHoveringOnWindow(v)));
    }

    @Nullable
    public FingerRenderer cursor() {
        return cursor.get(this);
    }

    @Nullable
    public RenderWhileHovering hover() {
        return hover.get(this);
    }

    private static class RenderWhileHoveringOnWindow extends RenderWhileHovering {
        public RenderWhileHoveringOnWindow(FingerRenderer v) {
            super(v);
        }

        @Override
        protected boolean update(Finger f) {
            return f.touching() instanceof Windo;
        }
    }
}
