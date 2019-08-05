package spacegraph.input.finger.util;

import com.jogamp.newt.event.MouseAdapter;
import jcog.TODO;
import jcog.math.v3;
import org.jetbrains.annotations.Nullable;
import spacegraph.space3d.SpaceDisplayGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.ClosestRay;
import spacegraph.space3d.phys.collision.narrow.VoronoiSimplexSolver;

import static java.lang.Math.tan;

/**
 * 3D camera control
 */
public abstract class SpaceMouse extends MouseAdapter {

    final SpaceDisplayGraph3D space;
    protected final ClosestRay rayCallback = new ClosestRay(((short) (1 << 7)));
    public v3 hitPoint;
    private final VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
    public Body3D pickedBody;

    public final v3 target = new v3(), origin = new v3();

    protected SpaceMouse(SpaceDisplayGraph3D g) {
        this.space = g;
    }



    public Spatial pickSpatial(float x, float y) {
        throw new TODO();
    }

    @Nullable
    public Collidable pickCollidable(float x, float y) {
        ClosestRay c = pickRay(x, y);
        if (c.hasHit()) {
            Collidable co = c.collidable;
            return co;
        }

        return null;
    }

    public ClosestRay pickRay0(float x, float y) {
        float ww = space.video.getWidth(), hh = space.video.getHeight();
        float res = Math.min(ww, hh);
        return pickRay0(x/res, y/res, ww, hh);
    }

    public ClosestRay pickRay0(float x, float y, float ww, float hh) {


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

        float tanfov = (float) tan(0.5f * fov);


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
        rayTo.add(rayToCenter, tmp1);
        rayTo.add(tmp2);

        tmp1.scale(x, dHor);
        tmp2.scale(y, dVert);

        rayTo.add(tmp1);
        rayTo.sub(tmp2);

        System.out.println("ray: " + x + "," + y  + "\t => " + rayTo);

        ClosestRay r = new ClosestRay(space.camPos, rayTo);
        space.dyn.rayTest(space.camPos, rayTo, r, simplexSolver);

        if (r.hasHit()) {
            Body3D body = Body3D.ifDynamic(r.collidable);
            if (body != null && (!(body.isStaticObject() || body.isKinematicObject()))) {
                pickedBody = body;
                hitPoint = r.hitPointWorld;
            }
        }

        return r;
    }
    public ClosestRay pickRay(float x, float y) {

        v3 in = space.camFwd.clone();
        in.normalize();

        v3 up = space.camUp.clone();
        up.normalize();

        v3 right = new v3();
        right.cross(space.camFwd, up);
        right.normalize();


        double nearScale = tan((space.fov * Math.PI / 180) ) * space.zNear;
        double vLength = nearScale;
        double hLength = nearScale * (space.right - space.left)/(space.top - space.bottom);


        origin.zero();
        origin.addScaled(in, space.zNear);
        origin.addScaled(right,
            x*(space.right-space.left)
            //(float) (x * hLength)
        );
        origin.addScaled(up,
            y*(space.top-space.bottom)
            //(float) (y * vLength)
        );

        target.set(origin);
        target.scale(space.zFar / space.zNear);

        origin.add(space.camPos);
        target.add(origin);


        //System.out.println(origin + " " + target);

        ClosestRay r = new ClosestRay(origin, target);
        space.dyn.rayTest(origin, target, r, simplexSolver);

        pickedBody = null;
        hitPoint = null;

        if (r.hasHit()) {
//            System.out.println("ray: " + x + "," + y  + "\t => " + target + " " + r.hitPointWorld);
            Body3D body = Body3D.ifDynamic(r.collidable);
            if (body != null && (!(body.isStaticObject() || body.isKinematicObject()))) {
                pickedBody = body;
                hitPoint = r.hitPointWorld;
            }
        }

        return r;
    }

}
