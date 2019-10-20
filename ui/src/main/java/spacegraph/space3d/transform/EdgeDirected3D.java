package spacegraph.space3d.transform;

import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.Broadphase;
import spacegraph.space3d.widget.EDraw;
import spacegraph.space3d.widget.SpaceWidget;

import java.util.List;

public class EdgeDirected3D extends ForceDirected3D {

    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        var a = condense.floatValue();

        for (var c : objects) {
            var A = ((Spatial) c.data());


            if (A instanceof SpaceWidget) {
                for (EDraw<?> e : ((SpaceWidget<?>) A).edges()) {
                    var attraction = e.attraction;
                    if (attraction > 0) {
                        var B = e.tgt();

                        if ((B.body != null)) {

                            attract(c, B.body, a * attraction, e.attractionDist);
                        }
                    }

                }
            }


        }

        super.solve(b, objects, timeStep);
    }
}
