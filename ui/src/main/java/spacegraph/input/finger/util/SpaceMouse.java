package spacegraph.input.finger.util;

import com.jogamp.newt.event.MouseAdapter;
import jcog.TODO;
import jcog.math.v3;
import org.jetbrains.annotations.Nullable;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.ClosestRay;
import spacegraph.space3d.phys.collision.narrow.VoronoiSimplexSolver;

/**
 * 3D camera control
 */
public abstract class SpaceMouse extends MouseAdapter {

    final SpaceGraph3D space;
    protected final ClosestRay rayCallback = new ClosestRay(((short) (1 << 7)));
    public v3 hitPoint;
    private final VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
    public Body3D pickedBody;

    public final v3 target = new v3();
    public final v3 origin = new v3();

    protected SpaceMouse(SpaceGraph3D g) {
        this.space = g;
    }



    public static Spatial pickSpatial(float x, float y) {
        throw new TODO();
    }

    public @Nullable Collidable pickCollidable(float x, float y) {
        ClosestRay c = pickRay(x, y);
        if (c.hasHit()) {
            Collidable co = c.collidable;
            return co;
        }

        return null;
    }


    public ClosestRay pickRay(float x, float y) {

        v3 in = space.camFwd.clone();
        in.normalize();

        v3 up = space.camUp.clone();
        up.normalize();

        v3 right = new v3();
        right.cross(space.camFwd, up);
        right.normalize();


//        double nearScale = tan((space.fov * Math.PI / 180) ) * space.zNear;
//        double vLength = nearScale;
//        double hLength = nearScale * (space.right - space.left)/(space.top - space.bottom);


        target.zero();
        target.addScaled(in, space.zNear);
        target.addScaled(right,
            (float) ((double) x *(space.right-space.left))/ 2.0F
            //(float) (x * hLength)
        );
        target.addScaled(up,
            (float) ((double) y *(space.top-space.bottom))/ 2.0F
            //(float) (y * vLength)
        );

        target.scale(space.zFar / space.zNear);

        origin.set(space.camPos);
        target.add(space.camPos);


//        System.out.println(origin + " " + target);

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
