package spacegraph.space2d.container.layout;

import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.Broadphase;
import spacegraph.space3d.widget.SpaceWidget;

import java.util.List;

public class EdgeDirected3D extends ForceDirected3D {

    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        float a = condense.floatValue();

        for (Collidable c : objects) {
            Spatial A = ((Spatial) c.data());


            if (A instanceof SpaceWidget) {
                ((SpaceWidget<?>) A).edges().forEach(e -> {

                    float attraction = e.attraction;
                    if (attraction > 0) {
                        SimpleSpatial B = e.tgt();

                        if ((B.body != null)) {

                            attract(c, B.body, a * attraction, e.attractionDist);
                        }
                    }

                });
            }


        }

        super.solve(b, objects, timeStep);
    }
}
