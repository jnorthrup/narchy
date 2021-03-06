package spacegraph.space3d.transform;

import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.SpaceTransform;
import spacegraph.space3d.Spatial;

/**
 * Created by me on 6/21/16.
 */
public class Spiral<X> implements SpaceTransform<X> {

    private int order;

    @Override
    public void update(Iterable<Spatial<X>> g, float dt) {
        this.order = 0;
        for (Spatial<X> xSpatial : g) {
            update(xSpatial);
        }
    }


    private void update(Spatial v) {


        int o = order++;


        SimpleSpatial vv = (SimpleSpatial) v;
        vv.body.clearForces();
        vv.body.setLinearVelocity((float) 0, (float) 0, (float) 0);
        float nodeSpeed = 0.3f;
        /* ~phi */
        float baseRad = 40f;
        float angleRate = 0.5f;
        float r = baseRad + (float) o * angleRate * 1.6f;
        float angle = (float) o * angleRate;
        vv.move(
            (float) (Math.sin((double) angle) * (double) r),
            (float) (Math.cos((double) angle) * (double) r),
                (float) 0,
                nodeSpeed
        );


        
                
                
                

    }

}
