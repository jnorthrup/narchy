package spacegraph.space3d.transform;

import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.SpaceTransform;
import spacegraph.space3d.Spatial;

/**
 * Created by me on 6/21/16.
 */
public class Spiral<X> implements SpaceTransform<X> {

    private float nodeSpeed = 0.3f;
    private int order;

    private float baseRad = 40f;
    private float angleRate = 0.5f;

    @Override
    public void update(Iterable<Spatial<X>> g, float dt) {
        this.order = 0;
        g.forEach(this::update);
    }


    private void update(Spatial v) {
        
        
        

        
        

        

        

        int o = order++;


        float angle = o * angleRate;
        float r = baseRad + o * angleRate * 1.6f /* ~phi */ ;
        SimpleSpatial vv = (SimpleSpatial) v;
        vv.body.clearForces();
        vv.body.setLinearVelocity(0,0,0);
        vv.move(
            (float) (Math.sin(angle) * r),
            (float) (Math.cos(angle) * r),
            0,
            nodeSpeed
        );


        
                
                
                

    }

}
