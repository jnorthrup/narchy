package nars.rover.obj.util.external.particles;


import nars.rover.obj.util.external.DynamicEntity;
import nars.rover.obj.util.external.Entity;
import spacegraph.space2d.phys.common.Vec2;

/**
 * Settings for a particle emitter
 * 
 * @author TranquilMarmot
 */
public abstract class EmitterSettings {
	/** The emitter stays attached to this entity */
	public DynamicEntity attached;
	
	/** Offset from center of attached entity that the emitter sits at */
	public Vec2 offset;
	
	/** Maximum number of particles the emitter can have at one time */
	public int maxParticles;
	
	public float
		/** How long each particle lives */
		particleLifetime,
		/** How long the emitter takes to emit particles, in seconds */
		particleEmissionRate,
		/** How many particles are let out per emission */
		particlesPerEmission,
		
		/** X/Y scatter amount */
		xLocationVariance,
		yLocationVariance,
		
		/** Info for particle in physics world */
		particleFriction,
		particleRestitution,
		particleDensity,
		
		/** Size of particles */
		particleWidth,
		particleHeight;
	
	/** How fast particles come out of the emitter */
	public Vec2 particleForce;
	
	/** Renderer to use for each particle */
	public Entity.ObjectRenderer2D particleRenderer;
	
	/** Called by the emitter after every particle is made, to allow for changing settings */
	protected abstract void onCreateParticle();
}
