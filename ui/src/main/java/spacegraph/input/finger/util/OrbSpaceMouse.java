package spacegraph.input.finger.util;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import jcog.math.v3;
import spacegraph.input.finger.Finger;
import spacegraph.space3d.SpaceDisplayGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.ClosestRay;
import spacegraph.space3d.phys.collision.narrow.VoronoiSimplexSolver;
import spacegraph.space3d.phys.constraint.Point2PointConstraint;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.space3d.phys.math.Transform;

import static jcog.math.v3.v;

/**
 * Created by me on 11/20/16.
 */
public class OrbSpaceMouse extends SpaceMouse implements KeyListener {

    private final ClosestRay rayCallback = new ClosestRay(((short) (1 << 7)));

    private int mouseDragPrevX, mouseDragPrevY;
    private int mouseDragDX, mouseDragDY;
    private final v3 gOldPickingPos = v();
    private float gOldPickingDist;

    private TypedConstraint pickConstraint;

    private Body3D pickedBody;
    private Spatial pickedSpatial;
    private Collidable picked;
    private v3 hitPoint;
    private final VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
    private final Finger finger;

    public OrbSpaceMouse(SpaceDisplayGraph3D g, Finger finger) {

        super(g);

        this.finger = finger;
        g.video.addKeyListener(this);
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {

        float y = e.getRotation()[1];
        if (y != 0) {

        }
    }

    private boolean mouseClick(int button, int x, int y) {

        switch (button) {
            case MouseEvent.BUTTON3:
                ClosestRay c = mousePick(x, y);
                if (c.hasHit()) {
                    Collidable co = c.collidable;


                    space.camera(co.transform, co.shape().getBoundingRadius() * 2.5f);
                    return true;

                }
                break;


        }
        return false;
    }

    private void pickConstrain(int button, int state, int x, int y) {


        switch (button) {
            case MouseEvent.BUTTON1:

                if (state == 1) {
                    mouseGrabOn();
                } else {
                    mouseGrabOff();
                }
                break;
            case MouseEvent.BUTTON2:
                break;
            case MouseEvent.BUTTON3:
                break;
        }
    }

    private void mouseGrabOff() {
        if (pickConstraint != null) {
            space.dyn.removeConstraint(pickConstraint);
            pickConstraint = null;

            pickedBody.forceActivationState(Collidable.ACTIVE_TAG);
            pickedBody.setDeactivationTime(0f);
            pickedBody = null;
        }


    }


    private ClosestRay mouseGrabOn() {


        if (pickConstraint == null && pickedBody != null) {
            pickedBody.setActivationState(Collidable.DISABLE_DEACTIVATION);

            Body3D body = pickedBody;
            v3 pickPos = new v3(rayCallback.hitPointWorld);

            Transform tmpTrans = body.transform;
            tmpTrans.inverse();
            v3 localPivot = new v3(pickPos);
            tmpTrans.transform(localPivot);

            Point2PointConstraint p2p = new Point2PointConstraint(body, localPivot);
            p2p.impulseClamp = 3f;


            gOldPickingPos.set(rayCallback.rayToWorld);
            v3 eyePos = new v3(space.camPos);
            v3 tmp = new v3();
            tmp.sub(pickPos, eyePos);
            gOldPickingDist = tmp.length();

            p2p.tau = 0.1f;

            space.dyn.addConstraint(p2p);
            pickConstraint = p2p;


        }

        return rayCallback;

    }


    @Deprecated /* TODO probably rewrite */ private boolean mouseMotionFunc(int px, int py, short[] buttons) {


        ClosestRay cray = mousePick(px, py);


        /*System.out.println(mouseTouch.collisionObject + " touched with " +
            Arrays.toString(buttons) + " at " + mouseTouch.hitPointWorld
        );*/

        Spatial prevPick = pickedSpatial;
        Spatial pickedSpatial = null;

        picked = cray != null ? cray.collidable : null;
        if (picked != null) {
            Object t = picked.data();
            if (t instanceof Spatial) {
                pickedSpatial = ((Spatial) t);
                if (pickedSpatial.onTouch(finger, picked, cray, buttons, space) != null) {


                    clearDrag();

                } else {

                }


            }
        }


        if ((pickConstraint != null) /*|| (directDrag != null)*/) {


        } else {


        }

        if (prevPick != pickedSpatial) {
            if (prevPick != null) {
                prevPick.onUntouch(space.video);
            }
            this.pickedSpatial = pickedSpatial;
        }

        return false;

    }

    private ClosestRay mousePick(int x, int y) {


        float tanFov = (space.top - space.bottom) * 0.5f / space.zNear;
        float fov = 2f * (float) Math.atan(tanFov);

        v3 rayFrom = new v3(space.camPos);
        v3 rayForward = new v3(space.camFwd);

        rayForward.scaled(space.zFar);


        v3 vertical = new v3(space.camUp);

        v3 hor = new v3();

        hor.cross(rayForward, vertical);
        hor.normalize();

        vertical.cross(hor, rayForward);
        vertical.normalize();

        float tanfov = (float) Math.tan(0.5f * fov);
        float ww = space.video.getWidth();
        float hh = space.video.getHeight();

        float aspect = hh / ww;

        hor.scaled(2f * space.zFar * tanfov);
        vertical.scaled(2f * space.zFar * tanfov);

        if (aspect < 1f) {
            hor.scaled(1f / aspect);
        } else {
            vertical.scaled(aspect);
        }

        v3 rayToCenter = new v3();
        rayToCenter.add(rayFrom, rayForward);
        v3 dHor = new v3(hor);
        dHor.scaled(1f / ww);
        v3 dVert = new v3(vertical);
        dVert.scaled(1f / hh);

        v3 tmp1 = new v3();
        v3 tmp2 = new v3();
        tmp1.scale(0.5f, hor);
        tmp2.scale(0.5f, vertical);

        v3 rayTo = new v3();
        rayTo.sub(rayToCenter, tmp1);
        rayTo.add(tmp2);

        tmp1.scale(x, dHor);
        tmp2.scale(y, dVert);

        rayTo.add(tmp1);
        rayTo.sub(tmp2);

        ClosestRay r = new ClosestRay(space.camPos, rayTo);
        space.dyn.rayTest(space.camPos, rayTo, r, simplexSolver);

        if (rayCallback.hasHit()) {
            Body3D body = Body3D.ifDynamic(rayCallback.collidable);
            if (body != null && (!(body.isStaticObject() || body.isKinematicObject()))) {
                pickedBody = body;
                hitPoint = r.hitPointWorld;
            }
        }

        return r;
    }


    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }


    @Deprecated
    public void clearDrag() {
        mouseDragDX = mouseDragDY = 0;
        mouseDragPrevX = mouseDragPrevY = -1;
    }


    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isConsumed())
            return;

        mouseDragDX = mouseDragDY = 0;

        int x = e.getX();
        int y = e.getY();
        if (!mouseMotionFunc(x, y, e.getButtonsDown())) {
            pickConstrain(e.getButton(), 1, x, y);

        }

        e.setConsumed(true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isConsumed())
            return;

        int dragThresh = 1;
        boolean dragging = Math.abs(mouseDragDX) < dragThresh;
        if (dragging && mouseClick(e.getButton(), e.getX(), e.getY())) {

        } else {

            int x = e.getX();
            int y = e.getY();
            if (!mouseMotionFunc(x, y, e.getButtonsDown())) {
                pickConstrain(e.getButton(), 0, x, y);
            }

        }
        if (dragging)
            clearDrag();

        e.setConsumed(true);
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.isConsumed())
            return;

        int x = e.getX();
        int y = e.getY();

        if (mouseDragPrevX >= 0) {
            mouseDragDX = (x) - mouseDragPrevX;
            mouseDragDY = (y) - mouseDragPrevY;
        }

        if (mouseMotionFunc(x, y, e.getButtonsDown())) {
            e.setConsumed(true);
        }

        mouseDragPrevX = x;
        mouseDragPrevY = y;

    }

    @Override
    public void mouseMoved(MouseEvent e) {

        if (mouseMotionFunc(e.getX(), e.getY(), e.getButtonsDown())) {
            e.setConsumed(true);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (pickedSpatial != null) {
            pickedSpatial.onKey(picked, hitPoint, e.getKeyChar(), true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (pickedSpatial != null) {
            pickedSpatial.onKey(picked, hitPoint, e.getKeyChar(), false);
        }
    }
}
