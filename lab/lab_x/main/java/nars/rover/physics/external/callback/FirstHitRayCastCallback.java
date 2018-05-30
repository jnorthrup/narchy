package nars.rover.obj.util.external.callback;


import nars.rover.obj.util.external.DynamicEntity;
import spacegraph.space2d.phys.callbacks.RayCastCallback;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Fixture;

/**
 * A RayCastCallback that grabs the first hit from a ray cast
 *
 * @author TranquilMarmot
 */
public class FirstHitRayCastCallback implements RayCastCallback {
	/** Pointer to first hit */
	private DynamicEntity hit;

	public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal,
							   float fraction) {
		hit = (DynamicEntity)fixture.getUserData(); 

		
		return 0;
	}

	/**
	 * @return First DynamicEntity hit in RayCast, null if no hits
	 */
	public DynamicEntity getHit(){
		return hit;
	}
}
