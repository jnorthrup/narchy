package nars.rover.obj.util.external.particles;


import nars.rover.obj.util.external.BoxEntity;
import spacegraph.space2d.phys.dynamics.BodyDef;
import spacegraph.space2d.phys.dynamics.FixtureDef;

/**
 * A particle.
 * 
 * @author TranquilMarmot
 */
public class Particle extends BoxEntity {
	/** How long the particle stays alive */
	private float timeToLive, timeAlive;
	/** The emitter this particle came from */
	private ParticleEmitter owner;
	
	public Particle(
			float timeToLive, ParticleEmitter owner,
			ObjectRenderer2D renderer, int layer,
			BodyDef bodyDef, float width, float height,
			FixtureDef fixtureDef){
		
		super(renderer, layer, bodyDef, width, height, fixtureDef);
		this.timeToLive = timeToLive;
		this.owner = owner;
	}
	
	@Override
	public void update(float timeStep){
		super.update(timeStep);
		
		timeAlive += timeStep;
		if(timeAlive >= timeToLive) {
			owner.destroy(this);
		}
		
	}
	

	
	public ParticleEmitter getEmitter(){ return this.owner; };
	
}
