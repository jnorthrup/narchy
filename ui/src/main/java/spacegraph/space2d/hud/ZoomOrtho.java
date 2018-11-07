package spacegraph.space2d.hud;

import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMovePixels;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.Surface;
import spacegraph.util.math.v2;
import spacegraph.util.math.v3;
import spacegraph.video.JoglSpace;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    public final static short PAN_BUTTON = 0;
    private final static short MOVE_WINDOW_BUTTON = 1;

    @Override
    protected void starting() {
        super.starting();

        //mouse wheel zoom
        onUpdate((w)->{

            //absorb remaining rotationY
            float zoomRate = 0.5f;

            if (!(finger.touching() instanceof Finger.WheelAbsorb)) {
                float dy = finger.rotationY(false);
                if (dy != 0) {
                    v2 xy = cam.screenToWorld(finger.posPixel);
                    cam.set(xy.x, xy.y, cam.z * (1f + (dy * zoomRate)));
                }
            }
        });
    }

    private final Fingering fingerContentPan = new FingerMovePixels(PAN_BUTTON) {

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
    private final Fingering fingerWindowMove = new FingerMovePixels(MOVE_WINDOW_BUTTON) {


        private final v2 windowStart = new v2();

        @Override
        protected JoglSpace window() {
            return space;
        }

        @Override
        protected boolean startDrag(Finger f) {


            windowStart.set(space.io.getX(), space.io.getY());
            //System.out.println("window start=" + windowStart);
            return super.startDrag(f);
        }

        @Override
        protected v2 pos(Finger finger) {

            return finger.posScreen.clone();
        }

        @Override
        public void move(float dx, float dy) {


            space.io.setPosition(
                    Math.round(windowStartX + dx),
                    Math.round(windowStartY - dy));
        }

    };

    public ZoomOrtho(Surface content, Finger finger, NewtKeyboard keyboard) {
        super(content, finger, keyboard);
    }


    @Override
    public boolean autosize() {
        return true;
    }



    @Override
    protected Surface finger() {

        Surface touchPrev = finger.touching();
        Surface touchNext = super.finger();
        if (touchNext != null && touchNext != touchPrev) {
            debug(this, 1f, () -> "touch(" + touchNext + ')');
        }

        if (touchNext == null) {
            if (finger.tryFingering(fingerWindowMove) || finger.tryFingering(fingerContentPan)) {
                return this;
            }
        }



        return touchNext;
    }


}



























