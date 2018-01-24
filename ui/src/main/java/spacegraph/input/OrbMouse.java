package spacegraph.input;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import org.jetbrains.annotations.Nullable;
import spacegraph.Spatial;
import spacegraph.math.Matrix4f;
import spacegraph.math.SingularMatrixException;
import spacegraph.math.Vector4f;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.collision.narrow.VoronoiSimplexSolver;
import spacegraph.phys.constraint.Point2PointConstraint;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.Transform;
import spacegraph.render.JoglPhysics;

import static spacegraph.math.v3.v;

/**
 * Created by me on 11/20/16.
 */
public class OrbMouse extends SpaceMouse implements KeyListener {

    protected final ClosestRay rayCallback = new ClosestRay(((short) (1 << 7)));
    // constraint for mouse picking
    private int mouseDragPrevX, mouseDragPrevY;
    private int mouseDragDX, mouseDragDY;
    final v3 gOldPickingPos = v();
    float gOldPickingDist;

    protected TypedConstraint pickConstraint;
    //    protected Dynamic directDrag;
    public Dynamic pickedBody; // for deactivation state
    public Spatial pickedSpatial;
    public Collidable picked;
    public v3 hitPoint;
    protected final VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
    public ClosestRay pickRay;
    public final Finger finger = new Finger(null);

    public OrbMouse(JoglPhysics g) {

        super(g);
        g.addKeyListener(this);

    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        //System.out.println("wheel=" + Arrays.toString(e.getRotati on()));
        float y = e.getRotation()[1];
        if (y != 0) {
            //space.setCameraDistance( space.cameraDistance.floatValue() + 0.1f * y );
        }
    }

    private boolean mouseClick(int button, int x, int y) {

        switch (button) {
            case MouseEvent.BUTTON3:
                ClosestRay c = mousePick(x, y);
                if (c.hasHit()) {
                    Collidable co = c.collidable;
                    //System.out.println("zooming to " + co);

                    //TODO compute new azi and ele that match the current viewing angle values by backcomputing the vector delta

                    space.camera(co.transform, co.shape().getBoundingRadius()*2.5f);
                    return true;

                }
                break;

//            case MouseEvent.BUTTON3: {
//                shootBox(rayTo);
//                break;
//            }
//            case MouseEvent.BUTTON2: {
//                // apply an impulse
//
//                v3 rayTo = v(rayTo(x, y));
//                CollisionWorld.ClosestRayResultCallback rayCallback = new CollisionWorld.ClosestRayResultCallback(camPos, rayTo);
//
//                dyn.rayTest(camPos, rayTo, rayCallback);
//                if (rayCallback.hasHit()) {
//                    RigidBody body = RigidBody.upcast(rayCallback.collisionObject);
//                    if (body != null) {
//                        body.setActivationState(CollisionObject.ACTIVE_TAG);
//                        v3 impulse = v(rayTo);
//                        impulse.normalize();
//                        float impulseStrength = 10f;
//                        impulse.scale(impulseStrength);
//                        v3 relPos = v();
//
//                        relPos.sub(rayCallback.hitPointWorld, body.getCenterOfMassPosition(v()));
//                        body.applyImpulse(impulse, relPos);
//                    }
//                }
//
//                break;
//            }
        }
        return false;
    }

    private void pickConstrain(int button, int state, int x, int y) {

        ClosestRay rayCallback = mousePick(x, y);

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

//        if (directDrag != null) {
//            Object u = directDrag.data();
//
//            if (u instanceof SimpleSpatial) {
//                ((SimpleSpatial) u).motionLock(false);
//            }
//
//            directDrag = null;
//        }
    }


    private ClosestRay mouseGrabOn() {
        // add a point to point constraint for picking

        if (pickConstraint == null && pickedBody != null) {
            pickedBody.setActivationState(Collidable.DISABLE_DEACTIVATION);

            Dynamic body = pickedBody;
            v3 pickPos = new v3(rayCallback.hitPointWorld);

            Transform tmpTrans = body.transform;
            tmpTrans.inverse();
            v3 localPivot = new v3(pickPos);
            tmpTrans.transform(localPivot);

            Point2PointConstraint p2p = new Point2PointConstraint(body, localPivot);
            p2p.impulseClamp = 3f;

            // save mouse position for dragging
            gOldPickingPos.set(rayCallback.rayToWorld);
            v3 eyePos = new v3(space.camPos);
            v3 tmp = new v3();
            tmp.sub(pickPos, eyePos);
            gOldPickingDist = tmp.length();
            // very weak constraint for picking
            p2p.tau = 0.1f;

            space.dyn.addConstraint(p2p);
            pickConstraint = p2p;

//                body.setActivationState(Collidable.DISABLE_DEACTIVATION);
//                v3 pickPos = v(rayCallback.hitPointWorld);
//
//                Transform tmpTrans = body.getCenterOfMassTransform(new Transform());
//                tmpTrans.inverse();
//                v3 localPivot = v(pickPos);
//                tmpTrans.transform(localPivot);
//                // save mouse position for dragging
//                gOldPickingPos.set(rayCallback.rayToWorld);
//                v3 eyePos = v(space.camPos);
//                v3 tmp = v();
//                tmp.sub(pickPos, eyePos);
//                gOldPickingDist = tmp.length();
//
//
//                // other exclusions?
//                if (!(body.isStaticObject() || body.isKinematicObject())) {
//                    pickedBody = body;
//
//
//                    Point2PointConstraint p2p = new Point2PointConstraint(body, localPivot);
//                    space.dyn.addConstraint(p2p);
//                    pickConstraint = p2p;
//
//                    // very weak constraint for picking
//                    p2p.tau = 0.02f;
//                } else {
//                    if (directDrag == null) {
//                        directDrag = body;
//
//                    }
//                }


        }

        return rayCallback;
        //}
    }

//    public ClosestRay mousePick(int sx, int sy) {
//        return mousePick(space.rayTo(sx, sy));
//    }
//
//    public ClosestRay mousePick(v3 rayTo) {
//        ClosestRay r = this.rayCallback;
//
//        //v3 camPos = v(rayTo.x, rayTo.y, space.camPos.z); //project directly upward
//
//        v3 camPos = space.camPos;
//
//        return r;
//    }

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
                    //absorbed surface

                    clearDrag();

                } else {
                    //maybe find next closest?
                }


            }
        }

//        } else {
////            if (pickedSpatial!=null) {
////                if (pickedSpatial.onTouch(picked, cray, buttons)!=null) {
////                    clearDrag();
////                    return true;
////                }
////            }
//            pickedSpatial = null;
//        }

        //}

        if ((pickConstraint != null) /*|| (directDrag != null)*/) {

//            // keep it at the same picking distance
//            v3 eyePos = v(space.camPos);
//            v3 dir = v();
//            dir.sub(cray.rayFromWorld, eyePos);
//            dir.normalize();
//            dir.scale(gOldPickingDist);
//
//            v3 newPos = v();
//            newPos.add(eyePos, dir);

//            if (directDrag != null) {
//                //directly move the 'static' object
//
//                Object u = directDrag.data();
//
//                //System.out.println("DRAG: " + directDrag + " " + u + " -> " + newPos);
//
//                if (u instanceof SimpleSpatial) {
//                    ((SimpleSpatial) u).motionLock(true);
//                }
//
////                MotionState mm = directDrag.getMotionState();
////                if (mm instanceof Motion) {
////                    ((Motion) mm).center(newPos);
////                }
//                directDrag.worldTransform.set(newPos);
//
//                return true;
//            } else


//            v3 newRayTo = new v3(mousePick(px, py).rayToWorld); //HACK dont involve a complete ClosestRay
//            v3 eyePos = new v3(space.camPos);
//            v3 dir = new v3();
//            dir.sub(newRayTo, eyePos);
//            dir.normalize();
//            dir.scale(gOldPickingDist);
//
//            v3 newPos = new v3();
//            newPos.add(eyePos, dir);
//
//            // move the constraint pivot
//            Point2PointConstraint p2p = (Point2PointConstraint) pickConstraint;
//            p2p.setPivotB(newPos);
//            return true;


        } else {

//            if (mouseDragPrevX >= 0) {
//
//
//                ///only if ALT key is pressed (Maya style)
//                //            if (m_modifierKeys & BT_ACTIVE_ALT)
//                //            {
//                //                if (m_mouseButtons & 4) {
//                //                    btVector3 hor = getRayTo(0, 0) - getRayTo(1, 0);
//                //                    btVector3 vert = getRayTo(0, 0) - getRayTo(0, 1);
//                //                    btScalar multiplierX = btScalar(0.001);
//                //                    btScalar multiplierY = btScalar(0.001);
//                //                    if (m_ortho) {
//                //                        multiplierX = 1;
//                //                        multiplierY = 1;
//                //                    }
//                //
//                //
//                //                    m_cameraTargetPosition += hor * dx * multiplierX;
//                //                    m_cameraTargetPosition += vert * dy * multiplierY;
//                //                }
//                //            }
//
//                for (short b : buttons) {
//                    switch (b) {
//                        case 1:
////                                ClosestRay m = mousePick(x, y, null);
////                                if (!m.hasHit()) {
////                                    //drag on background space
////                                    float px = mouseDragDX * 0.1f;
////                                    float py = mouseDragDY * 0.1f;
////
////                                    //TODO finish:
////
////                                    //v3 vx = v().cross(camDir, camUp);
////                                    //vx.normalize();
////                                    //System.out.println(px + " " + py + " " + camDir + " x " + camUp + " " + vx);
////
////                                    //camPosTarget.scaleAdd(px, vx);
////                                    //camPosTarget.scaleAdd(py, camUp);
////                                }
//                            return true;
//                        case 3:
//                            //right mouse drag = rotate
//                            //                        nextAzi += dx * btScalar(0.2);
//                            //                        nextAzi = fmodf(nextAzi, btScalar(360.f));
//                            //                        nextEle += dy * btScalar(0.2);
//                            //                        nextEle = fmodf(nextEle, btScalar(180.f));
//                            //space.azi.setValue(space.azi.floatValue() + mouseDragDX * 0.2f );
//                            //nextAzi = fmodf(nextAzi, btScalar(360.f));
//                            //space.ele.setValue(space.ele.floatValue() + mouseDragDY * (0.2f));
//                            //nextEle = fmodf(nextEle, btScalar(180.f));
//                            return true;
//                        case 2:
//                            //middle mouse drag = zoom
//
//                            //space.setCameraDistance( space.cameraDistance.floatValue() - mouseDragDY * 0.5f );
//                            return true;
//                    }
//                }
//            }
//
        }

        if (prevPick != pickedSpatial) {
            if (prevPick != null) {
                prevPick.onUntouch(space);
            }
            this.pickedSpatial = pickedSpatial;
        }

        return false;

    }

    @Nullable
    public ClosestRay mousePick(int x, int y) {
//        float top = 1f;
//        float bottom = -1f;
//        float nearPlane = 1f;
        float tanFov = (space.top - space.bottom) * 0.5f / space.zNear;
        float fov = 2f * (float) Math.atan(tanFov);

        v3 rayFrom = new v3(space.camPos);
        v3 rayForward = new v3(space.camFwd);

        rayForward.scale(space.zFar);

        //v3 rightOffset = new v3();
        v3 vertical = new v3(space.camUp);

        v3 hor = new v3();
        // TODO: check: hor = rayForward.cross(vertical);
        hor.cross(rayForward, vertical);
        hor.normalize();
        // TODO: check: vertical = hor.cross(rayForward);
        vertical.cross(hor, rayForward);
        vertical.normalize();

        float tanfov = (float) Math.tan(0.5f * fov);
        float ww = space.getWidth();
        float hh = space.getHeight();

        float aspect = hh / ww;

        hor.scale(2f * space.zFar * tanfov);
        vertical.scale(2f * space.zFar * tanfov);

        if (aspect < 1f) {
            hor.scale(1f / aspect);
        } else {
            vertical.scale(aspect);
        }

        v3 rayToCenter = new v3();
        rayToCenter.add(rayFrom, rayForward);
        v3 dHor = new v3(hor);
        dHor.scale(1f / ww);
        v3 dVert = new v3(vertical);
        dVert.scale(1f / hh);

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
            Dynamic body = Dynamic.ifDynamic(rayCallback.collidable);
            if (body != null && (!(body.isStaticObject() || body.isKinematicObject()))) {
                pickedBody = body;
                hitPoint = r.hitPointWorld;
            }
        }

        return r;
    }

    private v3 mousePick0(int px, int py) {
        int ww = space.getWidth();
        int hh = space.getHeight();
        if (ww == 0 || hh == 0)
            return null;

        float x = (2.0f * px) / ww - 1.0f;
        float y = 1.0f - (2.0f * py) / hh;
        float z = 1.0f;
        Vector4f ray_eye = new Vector4f(x * 2f, y * 2f, -1.0f, 1.0f);

        //https://capnramses.github.io/opengl/raycasting.html

        Matrix4f viewMatrixInv = new Matrix4f(space.mat4f);

        try {
            viewMatrixInv.invert();
        } catch (SingularMatrixException e) {
            return null;
        }
        viewMatrixInv.transform(ray_eye);
        ray_eye.setZ(-1f);
        ray_eye.setW(1f);

        viewMatrixInv.transform(ray_eye);
        v3 ray_wor = v(ray_eye.x, ray_eye.y, ray_eye.z);
        ray_wor.normalize();
        ray_wor.scale(1000f);


        //if (mouseDragDX == 0) { //if not already dragging somewhere "outside"

        //ray_wor.add(space.camPos);

        v3 from = space.camPos;
        v3 to = ray_wor;
        to.add(from);

        return to;
        //this.pickRay = rayCallback;

//        space.dyn.rayTest(from, to, rayCallback.set(from, to), simplexSolver);
//        return rayCallback;


    }

    @Override
    public void mouseClicked(MouseEvent e) {
        //overriden: called by mouseRelease
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

//        public void mouseExited(MouseEvent e) {
//            clearDrag();
//        }

    @Deprecated
    public void clearDrag() {
        mouseDragDX = mouseDragDY = 0; //clear drag
        mouseDragPrevX = mouseDragPrevY = -1;
    }


    @Override
    public void mousePressed(MouseEvent e) {
        mouseDragDX = mouseDragDY = 0;

        int x = e.getX();
        int y = e.getY();
        if (!mouseMotionFunc(x, y, e.getButtonsDown())) {
            pickConstrain(e.getButton(), 1, x, y);
            e.setConsumed(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
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
    }


    //
    // MouseMotionListener
    //
    @Override
    public void mouseDragged(MouseEvent e) {

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

        e.setConsumed(true);
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
