package nars.rover.util;

import spacegraph.space2d.phys.callbacks.QueryCallback;
import spacegraph.space2d.phys.collision.AABB;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body;
import spacegraph.space2d.phys.dynamics.Fixture;
import spacegraph.space2d.phys.dynamics.World;

/**
 * http:
 */
public class Explosion {

    public static void applyBlastImpulse(Body body, Vec2 blastCenter, Vec2 applyPoint, float blastPower) {
        Vec2 blastDir = applyPoint.sub( blastCenter );
        float distance = blastDir.normalize();
        
        if ( distance == 0 )
            return;
        float invDistance = 1 / distance;
        float impulseMag = blastPower * invDistance * invDistance;
        body.applyLinearImpulse( blastDir.mul(impulseMag), applyPoint );
    }

    public static void explodeBlastRadius(World world, Vec2 center, float blastRadius, float blastPower) {

        final float m_blastRadiusSq = blastRadius*blastRadius;

        
        QueryCallback queryCallback = new QueryCallback() {
            @Override
            public boolean reportFixture(Fixture fixture) {
                Body body = fixture.getBody();
                Vec2 bodyCom = body.getWorldCenter();

                
                if ((bodyCom.sub(center)).lengthSquared() < m_blastRadiusSq) {
                    applyBlastImpulse(body, center, bodyCom, blastPower);
                    return true;
                }
                return false;
            }
        };

        world.queryAABB(queryCallback, new AABB(
                center.sub(new Vec2(blastRadius, blastRadius)) ,
                center.add(new Vec2(blastRadius, blastRadius))
        ));

    }
















}
