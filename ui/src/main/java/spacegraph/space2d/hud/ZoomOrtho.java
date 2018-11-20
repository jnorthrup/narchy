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
    public Surface finger(Finger finger) {
        //absorb remaining rotationY
        float zoomRate = 0.5f;

        if (!(finger.touching() instanceof Finger.WheelAbsorb)) {
            float dy = finger.rotationY(true);
            if (dy != 0) {
                v2 xy = cam.screenToWorld(dy < 0 ?
                        finger.posPixel :
                        new v2(w()-finger.posPixel.x, h()-finger.posPixel.y)
                );
                cam.set(xy.x, xy.y, cam.z * (1f + (dy * zoomRate)));
            }
        }

        if (finger.touching() == null) {
            if (finger.tryFingering(fingerWindowMove) || finger.tryFingering(fingerContentPan)) {
                return this;
            }
        }

        return super.finger(finger);
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

    public ZoomOrtho(JoglSpace space, Surface content, Finger finger, NewtKeyboard keyboard) {
        super(space, content, finger, keyboard);
    }


    @Override
    public boolean autosize() {
        return true;
    }






}



























