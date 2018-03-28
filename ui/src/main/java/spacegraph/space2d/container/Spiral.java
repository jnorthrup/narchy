package spacegraph.space2d.container;

import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.SpaceTransform;
import spacegraph.space3d.Spatial;

/**
 * Created by me on 6/21/16.
 */
public class Spiral<X> implements SpaceTransform<X> {

    float nodeSpeed = 0.3f;
    private int order;

    float baseRad = 40f;
    float angleRate = 0.5f;

    @Override
    public void update(Iterable<Spatial<X>> g, float dt) {
        this.order = 0;
        g.forEach(this::update);
    }


    protected void update(Spatial v) {
        //TODO abstract
        //int hash = v.hash;
        //int vol = v.key.volume();

        //float ni = n / (float) Math.E;
        //final float bn = 1f;

        //float p = v.pri;

        //float nodeSpeed = (this.nodeSpeed / (1f + v.pri));

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


        //1f/(1f+v.lag) * (baseRad/2f);
                //v.budget.qua() * (baseRad + rad)
                //v.tp[2] = act*10f;
                //nodeSpeed);

    }

}
