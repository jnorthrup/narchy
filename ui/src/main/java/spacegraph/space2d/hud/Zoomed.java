package spacegraph.space2d.hud;

import jcog.Util;
import jcog.event.Off;
import jcog.math.v2;
import jcog.math.v3;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.*;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.input.finger.impl.NewtMouseFinger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;
import spacegraph.video.JoglDisplay;

import java.util.ArrayDeque;
import java.util.Deque;

import static java.lang.Math.sin;

/**
 * manages a moveable and zoomable view camera for interaction and display of a target virtual surface.
 * <p>
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Zoomed<S extends Surface> extends MutableUnitContainer<S> implements /*Deprecated*/ KeyPressed {

    public final static short PAN_BUTTON = 0;

    /**
     * middle mouse button (wheel when pressed, apart from its roll which are detected in the wheel/wheelabsorber)
     */
    public final static short ZOOM_BUTTON = 2;

    private static final int ZOOM_STACK_MAX = 8;
    private final static float focusAngle = (float) Math.toRadians(45);
    /**
     * current view area, in absolute world coords
     */
    public final v2 scale = new v2(1, 1);
    public final Camera cam;
    /**
     * parent
     */
    public final JoglDisplay space;
    @Deprecated
    private final NewtKeyboard keyboard;
    private final Deque<Surface> zoomStack = new ArrayDeque();

    private final float camZmin = 1;
    private final float zoomMargin = 0.1f;
    private final float wheelZoomRate = 0.6f;

    private final Fingering zoomDrag = new Dragging(ZOOM_BUTTON) {

        final v2 start = new v2();
        float maxIterationChange = 0.25f;
        float rate = 0.4f;

        @Override
        protected boolean ready(Finger f) {
            start.set(f.posPixel);
            return super.ready(f);
        }

        @Override
        protected boolean drag(Finger f) {

            v2 current = f.posPixel;

            float d = start.distanceToY(current) + start.distanceToX(current);

            zoomDelta(Util.clamp((float) Math.pow(d * rate, 1), -maxIterationChange, +maxIterationChange));

            start.set(current); //incremental

            return true;
        }
    };
    private final Fingering contentPan = new FingerMoveWindow(PAN_BUTTON) {

        final float speed = 1f;
        private v3 camStart;

        @Override
        protected JoglDisplay window() {
            return space;
        }

        @Override
        protected boolean ready(Finger f) {
            if (f.fingering() == Fingering.Null) {
                camStart = new v3(cam);
                return super.ready(f);
            } else {
                return false;
            }
        }

        @Override
        public void move(float dx, float dy) {
            cam.set(camStart.x - dx / scale.x * speed,
                    camStart.y - dy / scale.y * speed);
        }

    };

//    float CORNER_RADIUS = 4;
    private float camXmin = -1, camXmax = +1;
    private float camYmin = -1, camYmax = +1;

    public Zoomed(JoglDisplay space, NewtKeyboard keyboard, S content) {
        super(content);

        this.space = space;
        this.keyboard = keyboard;

        this.cam = new Camera();
    }

    @Override
    protected void doLayout(float dtS) {
        if (autosize()) {
            zoomStackReset();
            unzoom();
        }
        super.doLayout(dtS);
    }

    /**
     * full unzoom
     */
    public void unzoom() {
        cam.set(bounds.w / 2f, bounds.h / 2f, camZMax());
    }

    private float camZMax() {
        return targetDepth(Math.min(space.video.getWidth(), space.video.getHeight()));
    }

    @Override
    public final void renderContent(ReSurface render) {

        render.on((gl) -> {

            float zoom = (float) (sin(Math.PI / 2 - focusAngle / 2) / (cam.z * sin(focusAngle / 2)));
            float s = zoom * Math.min(w(), h());

            render.set(cam, scale.set(s,s));
            render.push(cam, scale.set(s, s));

            gl.glPushMatrix();

            gl.glScalef(scale.x, scale.y, 1);
            gl.glTranslatef((w() / 2) / scale.x - cam.x, (h() / 2) / scale.y - cam.y, 0);


        });

        the().tryRender(render);

        render.on((gl)->{
            gl.glPopMatrix();

            render.pop();
        });
    }

    @Override
    public Surface finger(Finger finger) {


        //TODO may need to be a stack


        ((NewtMouseFinger) finger).setTransform(cam::pixelToGlobal); //ENTER ORTHO

        try {

            Surface innerTouched = super.finger(finger);

            if (!(innerTouched instanceof WheelAbsorb)) {
                //wheel zoom: absorb remaining rotationY
                float dy = finger.rotationY(true);
                if (dy != 0) {
                    zoomDeltaTo(finger, dy * wheelZoomRate);
                    zoomStackReset();
                }
            }


            if (innerTouched != null && finger.clickedNow(2 /*right button*/)) {
                /** click-zoom */
                zoomNext(finger, innerTouched);
            }


            if (innerTouched == null) {
                if (finger.tryFingering(zoomDrag)) {
                    zoomStackReset();
                } else if (finger.tryFingering(contentPan)) {
                    zoomStackReset();
                }
                //}
            }

            return innerTouched;
        } finally {
            ((NewtMouseFinger) finger).setTransform(x -> x); //EXIT ORTHO
        }
    }

    private void zoomStackReset() {
        synchronized (zoomStack) {
            zoomStack.clear();
            zoomStack.add(this);
        }
    }

//    private boolean corner(v2 p) {
//        //TODO other 3 corners
//        return (p.x < CORNER_RADIUS && p.y  < CORNER_RADIUS);
//    }


    public Off animate(Animated c) {
        return space.onUpdate(c);
    }

    private Off animate(Runnable c) {
        return space.onUpdate(c);
    }

    @Override
    protected void starting() {

        JoglDisplay s = (JoglDisplay) root();

        s.video.addKeyListener(keyboard);

        animate(cam);

        super.starting();
    }

    @Override
    public Zoomed move(float x, float y) {
        throw new UnsupportedOperationException();
    }


    /**
     * delta in (-1..+1)
     * POSITIVE = ?
     * NEGATIVE = ?
     */
    public void zoomDeltaTo(Finger finger, float deltaPct) {
        if (Math.abs(deltaPct) < Float.MIN_NORMAL)
            return; //no effect

        v2 xy = finger.posGlobal(); /*cam.screenToGlobal(deltaPct < 0 ?
                        finger.posPixel
                        :
                        //TODO fix this zoom-out calculation
                        //finger.posPixel
                        new v2(finger.posPixel.x, finger.posPixel.y)
                //new v2(w()-finger.posPixel.x, h()-finger.posPixel.y)
        )*/
        cam.set(xy.x, xy.y, cam.z * (1f + deltaPct));
    }

    public void zoomDelta(float deltaPct) {
        if (Math.abs(deltaPct) < Float.MIN_NORMAL)
            return; //no effect
        cam.set(cam.x, cam.y, cam.z * (1f + deltaPct));
    }

    private void zoomNext(Finger finger, Surface x) {

        synchronized (zoomStack) {

            int s = zoomStack.size();
            Surface top = zoomStack.peekLast();
            if (top == x) {
                if (s > 1)
                    zoomStack.removeLast(); //POP

                zoom(zoomStack.peekLast().bounds);
            } else {

                if (s + 1 >= ZOOM_STACK_MAX)
                    zoomStack.removeFirst(); //EVICT

                zoomStack.addLast(x); //PUSH

                zoom(x.bounds);
            }
        }
    }


    public void zoom(RectFloat b) {
        zoom(b.cx(), b.cy(), b.w, b.h, zoomMargin);
    }

    /**
     * choose best zoom radius for the target rectangle according to current view aspect ratio
     */
    private float targetDepth(float w, float h, float margin) {
        float d = Math.max(w, h);
//        if (((((float) pw()) / ph()) >= 1) == ((w / h) >= 1))
//            d = h; //limit by height
//        else
//            d = w; //limit by width

        return targetDepth(d * (1 + margin));
    }


    private float targetDepth(float viewDiameter) {
        return (float) ((viewDiameter * sin(Math.PI / 2 - focusAngle / 2)) / sin(focusAngle / 2));
    }

    private void zoom(float x, float y, float sx, float sy, float margin) {
        zoom(x, y, targetDepth(sx, sy, margin));
    }

    public final void zoom(v3 v) {
        zoom(v.x, v.y, v.z);
    }

    public void zoom(float x, float y, float z) {
        cam.set(x, y, z);
    }



    public boolean autosize() {
        return true;
    }

    public Surface overlayZoomBounds(Finger finger) {
        return new Finger.TouchOverlay(finger, cam);
    }


//    /**
//     * called each frame regardless of mouse activity
//     * TODO split this into 2 methods.  one picks the current touchable
//     * and the other method invokes button changes on the result of the first.
//     * this will allow rapid button changes to propagate directly to
//     * the picked surface even in-between pick updates which are invoked
//     * during the update loop.
//     */
//    protected Surface finger() {
//        /** layer specific, separate from Finger */
//        float pmx = finger.posPixel.x, pmy = finger.posPixel.y;
//        float wmx = +cam.x + (-0.5f * w() + pmx) / scale.x;
//        float wmy = +cam.y + (-0.5f * h() + pmy) / scale.y;
//        fingerPos.setAt(wmx, wmy);
//
//        return finger.touching();
//    }


    public class Camera extends AnimVector3f {

        /**
         * TODO atomic
         */
        private final float CAM_RATE = 3f;

        {
            setDirect(0, 0, 1); //(camZmin + camZmax) / 2);
        }

        public Camera() {
            super(1);
        }

//        public v3 snapshot() {
//            return new v3(target.x, target.y, target.z);
//        }

        @Override
        public boolean animate(float dt) {
            //System.out.println(this);
            if (super.animate(dt)) {
                update();
                return true;
            }
            return false;
        }

        protected void update() {
            if (!Zoomed.this.visible())
                return;

            //System.out.println(z);
            float W = bounds.w;
            float H = bounds.h;
            speed.set(Math.max(W, H) * CAM_RATE);

            float visW = W / scale.x / 2, visH = H / scale.y / 2; //TODO optional extra margin
            camXmin = 0 + visW;
            camYmin = 0 + visH;
            camXmax = bounds.w - visW;
            camYmax = bounds.h - visH;
        }

        @Override
        public void setDirect(float x, float y, float z) {
            super.setDirect(camX(x), camY(y), camZ(z));
        }

        public float camZ(float z) {
            return Util.clamp(z, camZmin, Math.max(camZmin, camZMax()));
        }

        public float camY(float y) {
            return Util.clamp(y, camYmin, Math.max(camYmin, camYmax));
        }

        public float camX(float x) {
            return Util.clamp(x, camXmin, Math.max(camXmin, camXmax));
        }

//        public float motionSq() {
//            v3 t = new v3(target);
//            t.add(-x, -y, -z);
//            return t.lengthSquared();
//        }


        public final v2 pixelToGlobal(v2 xy) {
            return pixelToGlobal(xy.x, xy.y);
        }

        public v2 globalToPixel(float gx, float gy) {
            return new v2(
                    ((gx - cam.x) * scale.x) + w() / 2,
                    ((gy - cam.y) * scale.y) + h() / 2
            );
        }

        public v2 pixelToGlobal(float px, float py) {
            v2 g = new v2(
                    ((px - w() / 2) / scale.x + cam.x),
                    ((py - h() / 2) / scale.y + cam.y)
            );

            return g;
        }

        /**
         * immediately get to where its going
         */
        public void complete() {
            setDirect(target.x, target.y, target.z);
        }
//
//    public final RectFloat globalToPixel(Surface t) {
//        return globalToPixel(t.bounds);
//    }
//
//

    }

}
