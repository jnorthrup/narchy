package nars.rover.util;

import spacegraph.space2d.phys.callbacks.RayCastCallback;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body;
import spacegraph.space2d.phys.dynamics.Fixture;

/**
 * Created by me on 7/19/15.
 */
public class RayCastClosestCallback implements RayCastCallback {


    public boolean m_hit;
    public Vec2 m_point;
    Vec2 m_normal;
    public Body body;

    public void init() {
        m_hit = false;
    }

    public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
        Body body = fixture.getBody();
        
        this.body = body;









        m_hit = true;
        m_point = point;
        m_normal = normal;

        return fraction;
    }

}


