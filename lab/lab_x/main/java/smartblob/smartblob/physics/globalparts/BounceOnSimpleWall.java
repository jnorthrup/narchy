/** Ben F Rayfield offers this software opensource GNU GPL 2+ */
package smartblob.smartblob.physics.globalparts;


import smartblob.Smartblob;
import smartblob.blobs.layeredzigzag.CornerData;
import smartblob.blobs.layeredzigzag.LayeredZigzag;
import smartblob.smartblob.physics.GlobalChangeSpeed;
import smartblob.smartblob.physics.SmartblobSim;

public class BounceOnSimpleWall implements GlobalChangeSpeed {
	
	
	
	public float position;
	
	public final boolean verticalInsteadOfHorizontal;
	
	public final boolean maxInsteadOfMin;
	
	public BounceOnSimpleWall(float position, boolean verticalInsteadOfHorizontal, boolean maxInsteadOfMin){
		this.position = position;
		this.verticalInsteadOfHorizontal = verticalInsteadOfHorizontal;
		this.maxInsteadOfMin = maxInsteadOfMin;
	}
	
	public void globalChangeSpeed(SmartblobSim sim, float secondsSinceLastCall){
		Smartblob blobArray[];
		synchronized(sim.smartblobs){
			blobArray = sim.smartblobs.toArray(new Smartblob[0]);
		}
		for(Smartblob blob : blobArray){
			Rectangle r = blob.boundingRectangle();
			if(anyPartIsPastThisWall(r)){
				if(blob instanceof LayeredZigzag){
					bounceSomePartsOnWall((LayeredZigzag)blob);
				}else{
					System.out.println(Smartblob.class.getName()+" type unknown: "+blob.getClass().getName());
				}
			}
		}
	}
	
	public boolean anyPartIsPastThisWall(Rectangle r){
		if(verticalInsteadOfHorizontal){
			if(maxInsteadOfMin){ 
				return r.y+r.height <= position;
			}else{ 
				return position <= r.y+r.height;
			}
		}else{
			if(maxInsteadOfMin){ 
				return r.x+r.width <= position;
			}else{ 
				return position <= r.x+r.width;
			}
		}
	}
	
	public void bounceSomePartsOnWall(LayeredZigzag z){
		final float position = this.position;
		for(CornerData layerOfCorners[] : z.corners){
			for(CornerData cd : layerOfCorners){
				if(verticalInsteadOfHorizontal){
					if(maxInsteadOfMin){ 
						if(position < cd.y){
							cd.speedY = -Math.abs(cd.speedY);
							
							
							
						}
					}else{ 
						if(cd.y < position){
							cd.speedY = Math.abs(cd.speedY);
							
							
							
						}
					}
				}else{
					if(maxInsteadOfMin){ 
						if(position < cd.x){
							cd.speedX = -Math.abs(cd.speedX);
							
							
							
						}
					}else{ 
						if(cd.x < position){
							cd.speedX = Math.abs(cd.speedX);
							
							
							
						}
					}
				}
			}
		}
	}

}
