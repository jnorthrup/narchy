package spacegraph.space2d.hud;

import jcog.math.v2;
import jcog.math.v3;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMoveWindow;
import spacegraph.input.finger.FingerResizeWindow;
import spacegraph.input.finger.Fingering;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.util.animate.Animated;
import spacegraph.video.JoglSpace;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    public final static short PAN_BUTTON = 0;
    private final static short MOVE_WINDOW_BUTTON = 1;

    private float wheelZoomRate = 0.5f;

    public ZoomOrtho(JoglSpace space, NewtKeyboard keyboard) {
        super(space, keyboard);
    }


    @Override
    public Surface finger(Finger finger) {

        Surface f = super.finger(finger);

        if (!(f instanceof Finger.WheelAbsorb)) {
            //wheel zoom: absorb remaining rotationY
            float dy = finger.rotationY(true);
            if (dy != 0)
                zoomDelta(finger, dy * wheelZoomRate);
        }

//        if (corner(finger.posPixel)) {
//            System.out.println("corner " + menu.bounds);
//        } else {
            if (f != null && finger.clickedNow(2 /*right button*/)) {
                /** auto-zoom */
                zoomNext(f);
            }
//        }





//        if (f!=null && finger.pressedNow(1)) {
//            /** magnify zoom */
//            if (beforeMagnify.get()==null && beforeMagnify.compareAndSet(null, cam.snapshot())) {
//                zoom(f.bounds);
//                cam.complete();
//            }
//
//        } else if (!finger.releasedNow(1)) {
//
//            v3 b;
//            if ((b = beforeMagnify.getAndSet(null))!=null) {
//                //unmagnify zoom (restore)
//                cam.setDirect(b);
//            }
//        }

        if (f==null) {
            if (finger.touching() == null) {
                if (finger.tryFingering(fingerWindowMove) ||
                        finger.tryFingering(fingerWindowResize) ||
                        finger.tryFingering(fingerContentPan)) {
                    return this;
                }
            }
        }

        return f;
    }

    float CORNER_RADIUS = 4;
    private boolean corner(v2 p) {
        //TODO other 3 corners
        return (p.x < CORNER_RADIUS && p.y  < CORNER_RADIUS);
    }


    @Deprecated
    public MutableListContainer hud() {
        return (MutableListContainer) ((MutableListContainer) the()).get(3); //HACK
    }

    private final Fingering fingerContentPan = new FingerMoveWindow(PAN_BUTTON) {

        float speed = 1f;
        private v3 camStart;

        @Override
        protected JoglSpace window() {
            return space;
        }

        @Override
        protected boolean startDrag(Finger f) {
            camStart = new v3(cam);
            return super.startDrag(f);
        }

        @Override
        public void move(float dx, float dy) {
            cam.set(camStart.x - dx / scale.x * speed,
                    camStart.y - dy / scale.y * speed);
        }

    };
    private final Fingering fingerWindowMove = new FingerMoveWindow(MOVE_WINDOW_BUTTON) {


        private final v2 windowStart = new v2();

        @Override
        protected JoglSpace window() {
            return space;
        }

        @Override
        public boolean escapes() {
            return false;
        }

        @Override
        protected boolean startDrag(Finger f) {


            windowStart.set(space.display.getX(), space.display.getY());
            //System.out.println("window start=" + windowStart);
            return super.startDrag(f);
        }


        @Override
        public void move(float dx, float dy) {


            space.display.setPosition(
                    Math.round(windowStartX + dx),
                    Math.round(windowStartY - dy));
        }

    };

    private final Fingering fingerWindowResize = new FingerResizeWindow(space, MOVE_WINDOW_BUTTON) {


        private final v2 windowStart = new v2();

        @Override
        public boolean escapes() {
            return false;
        }

        @Override
        protected boolean startDrag(Finger f) {
            windowStart.set(space.display.getX(), space.display.getY());
            //System.out.println("window start=" + windowStart);
            return super.startDrag(f);
        }

        @Override
        protected v2 pos(Finger finger) {
            return finger.posScreen.clone();
        }


    };



    @Override
    public boolean autosize() {
        return true;
    }


    private class DelayedHover implements Animated {

        private final Finger finger;
        long lastTouchStart;
        final static long hoverDelayMS = 100;
        Surface prevTouch;
        Surface hover;

        public DelayedHover(Finger finger) {
            this.finger = finger;
            lastTouchStart = Long.MIN_VALUE;
            prevTouch = null;
            hover = null;
        }

        @Override
        public boolean animate(float w) {

            Surface nextTouch = finger.touching();
            if (nextTouch!=null)
                nextTouch = (Surface) nextTouch.parent(HudHover.class);

            if (nextTouch != prevTouch) {

                if (hover!=null) {
                    hover.remove();
                    hover = null;
                }

                if (nextTouch instanceof HudHover) {
                    this.lastTouchStart = System.currentTimeMillis();
                    this.prevTouch = nextTouch;
                } else{
                    this.prevTouch = null;
                }
            }

            Surface t = this.prevTouch;
            if (t!=null && hover == null && System.currentTimeMillis() - lastTouchStart > hoverDelayMS) {
                hover = ((HudHover) t).hover(cam.worldToScreen(t), finger);
                if (hover == null)
                    this.prevTouch = null;
                else {
                    hud().add(hover);
                }
            }

            return true;
        }
    }
}



























