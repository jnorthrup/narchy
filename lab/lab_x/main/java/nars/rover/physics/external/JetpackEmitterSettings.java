package nars.rover.obj.util.external;


import nars.rover.obj.util.external.particles.EmitterSettings;
import spacegraph.space2d.phys.common.Vec2;

import static nars.rover.obj.util.external.Jetpack.rng;

public class JetpackEmitterSettings extends EmitterSettings {

		public JetpackEmitterSettings(DynamicEntity attachedTo){
			super.attached = attachedTo;
			super.offset = new Vec2(0.0f, 0.0f);
			super.maxParticles = 150;

			super.particleLifetime = 0.5f;
			super.particleEmissionRate = 0.05f;
			super.particlesPerEmission = 5;
			
			super.xLocationVariance = 0.5f;
			super.yLocationVariance = 0.1f;
			
			super.particleFriction = 10.0f;
			super.particleRestitution = 0.2f;
			super.particleDensity = 0.1f;
			
			super.particleHeight = 0.25f;
			super.particleWidth = 0.25f;
			
			super.particleForce = new Vec2(0.5f, 0.5f);
			
			super.particleRenderer = new Entity.ObjectRenderer2D(){

























				
			};
			
			/*
			 * TODO
			 * offset
			 * particle force
			 * lifetime
			 * 
			 * super.offset = new Vec2(Game.random.nextFloat() * super.xLocationVariance * -0.2f, Game.random.nextFloat() * super.yLocationVariance * 0.02f);
			 */
		}


		@Override
		protected void onCreateParticle() {
			
			super.offset = new Vec2(rng.nextFloat() * super.xLocationVariance * -0.2f, rng.nextFloat() * super.yLocationVariance * 0.02f);
			
			
			if(rng.nextFloat() > 0.9f){
				float low = 0.8f, high = 1.0f;
				float liveLong = rng.nextFloat();
				if(liveLong < low)
					super.particleLifetime = low * 2.0f;
				else if(liveLong > high)
					super.particleLifetime = high * 2.0f;
				else
					super.particleLifetime = liveLong * 2.0f;
			} else {
				super.particleLifetime = rng.nextFloat() * 0.75f;
			}
			
			
			super.particleForce = new Vec2(rng.nextBoolean() ? rng.nextFloat() * -5.0f : rng.nextFloat() * -5.0f, rng.nextFloat() * -50.0f);
		}
}
