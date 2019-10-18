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


        float angleRate = 0.5f;
        float angle = o * angleRate;
        float baseRad = 40f;
        float r = baseRad + o * angleRate * 1.6f /* ~phi */ ;
        SimpleSpatial vv = (SimpleSpatial) v;
        vv.body.clearForces();
        vv.body.setLinearVelocity(0,0,0);
        float nodeSpeed = 0.3f;
        vv.move(
            (float) (Math.sin(angle) * r),
            (float) (Math.cos(angle) * r),
            0,
                nodeSpeed
        );


        
                
                
                

    }

}
