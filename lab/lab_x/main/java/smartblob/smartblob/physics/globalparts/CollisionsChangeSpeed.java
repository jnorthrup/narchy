/** Ben F Rayfield offers this software opensource GNU GPL 2+ */
package smartblob.smartblob.physics.globalparts;

import smartblob.Smartblob;
import smartblob.SmartblobUtil;
import smartblob.blobs.layeredzigzag.CornerData;
import smartblob.blobs.layeredzigzag.LayeredZigzag;
import smartblob.blobs.layeredzigzag.TriData;
import smartblob.smartblob.physics.GlobalChangeSpeed;
import smartblob.smartblob.physics.SmartblobSim;

/** First all pairs of Smartblob.boundingRectangle() are checked for possible collision.
Then of those which may collide, more detailed checks are done. For all outer point
found to collide past a surface line, speeds of the 2 points on those lines are updated.
Positions are not updated, so this can be done in any order for the same result.
Actually order may affect it a little since speeds are kept the same in magnitude
along certain directions but set away from eachother.
*/
public class CollisionsChangeSpeed implements GlobalChangeSpeed {
	
	public void globalChangeSpeed(SmartblobSim sim, float secondsSinceLastCall){
		Smartblob blobArray[];
		synchronized(sim.smartblobs){
			blobArray = sim.smartblobs.toArray(new Smartblob[0]);
		}
		for(int i=0; i<blobArray.length-1; i++){
			for(int j=i+1; j<blobArray.length; j++){ 
				nextPair(blobArray[i], blobArray[j]);
			}
		}
	}
	
	public void nextPair(Smartblob a, Smartblob b){
		Rectangle ra = a.boundingRectangle();
		Rectangle rb = b.boundingRectangle();
		if(ra.intersects(rb)){
			
			if(a instanceof LayeredZigzag && b instanceof LayeredZigzag){
				rectanglesIntersect((LayeredZigzag)a, (LayeredZigzag)b);
			}else{
				System.out.println("At least 1 of 2 "+Smartblob.class.getName()
					+" type unknown: "+a.getClass().getName()
					+" and "+b.getClass().getName());
			}
		}
	}
	
	
	/** Their bounding rectangles are known to intersect. The smartblobs may intersect. */
	public void rectanglesIntersect(LayeredZigzag a, LayeredZigzag b){
		
		
		
		Rectangle intersect = a.boundingRectangle().intersection(b.boundingRectangle());
		handleIntersectsBetweenPointAndWeightedSumOfLine(intersect, a, b);
		handleIntersectsBetweenPointAndWeightedSumOfLine(intersect, b, a);
	}
	
	/** Rectangle intersect is the intersection of the 2 smartblobs bounding rectangles.
	TODO This puts equal force between point and weightedSum between 2 line ends
	depending on, closest intersection point on the line (or distances to each end?),
	and spreading the opposite force between those 2 ends.
	*/
	protected void handleIntersectsBetweenPointAndWeightedSumOfLine(Rectangle intersect, LayeredZigzag myPoints, LayeredZigzag myLines){
		int lastLayer = myPoints.layers-1;
		float getYX[] = new float[2];
		for(int p=0; p<myPoints.layerSize; p++){
			CornerData point = myPoints.corners[lastLayer][p];
			if(intersect.contains(point.x, point.y)){
				
				TriData t = myLines.findCollision(point.y, point.x);
				if(t != null){ 
					
					
					SmartblobUtil.getClosestPointToInfiniteLine(getYX, t, point.y, point.x);
					
					float vectorY = point.y-getYX[0];
					float vectorX = point.x-getYX[1];
					float vectorLen = (float)Math.sqrt(vectorY*vectorY + vectorX*vectorX);
					if(vectorLen == 0){
						
						continue;
					}
					
					CornerData a = t.adjacentCorners[0], b = t.adjacentCorners[1];
					
					float aDy = getYX[0]-a.y, aDx = getYX[1]-a.x; 
					float distanceA = (float)Math.sqrt(aDy*aDy + aDx*aDx);
					float bDy = getYX[0]-b.y, bDx = getYX[1]-b.x; 
					float distanceB = (float)Math.sqrt(bDy*bDy + bDx*bDx);
					
					
					
					float distanceSum = distanceA+distanceB;
					float fractionLineEndA = distanceA/distanceSum;
					float fractionLineEndB = 1-fractionLineEndA;
					
					
					
					float speedYOfPointOnLine = a.speedY*fractionLineEndA + fractionLineEndB*b.speedY;
					float speedXOfPointOnLine = a.speedX*fractionLineEndA + fractionLineEndB*b.speedX;
					
					float ddy = point.speedY-speedYOfPointOnLine;
					float ddx = point.speedX-speedXOfPointOnLine;
					
					float normVY = vectorY/vectorLen;
					float normVX = vectorX/vectorLen;
					
					
					normVY = -normVY;
					normVX = -normVX;
					
					
					
					
					
					
					float speedDotNorm = ddy*normVY + ddx*normVX;
					
					
					if(speedDotNorm < 0){
						
						
						float partOfSpeedVecY = normVY*speedDotNorm;
						float partOfSpeedVecX = normVX*speedDotNorm;
						float addToSpeedY = -2*partOfSpeedVecY;
						float addToSpeedX = -2*partOfSpeedVecX;
						float addToEachSpeedY = addToSpeedY/2;
						float addToEachSpeedX = addToSpeedX/2;
						
						
						
						point.speedY += addToEachSpeedY;
						a.speedY -= fractionLineEndA*addToEachSpeedY;
						b.speedY -= fractionLineEndB*addToEachSpeedY;
						
						point.speedX += addToEachSpeedX;
						a.speedX -= fractionLineEndA*addToEachSpeedX;
						b.speedX -= fractionLineEndB*addToEachSpeedX;
						
						
						
						
						
					}
					
				}
				/*LineData lineData = myLines.findCollision(point.y, point.x);
				if(lineData != null){ 
					throw new RuntimeException("TODO");
				}
				*/
			}
		}
	}
	
	/** Rectangle intersect is the intersection of the 2 smartblobs bounding rectangles.
	This only bounces the point off the line but doesnt bounce the line oppositely,
	so this is old code but still works unusually well since both smartblobs have such points
	and lines that they bounce, mostly symmetricly, on eachother,
	but it visibly doesnt work as equal and opposite forces too often.
	*/
	protected void handleIntersectsOneWay(Rectangle intersect, LayeredZigzag myPoints, LayeredZigzag myLines){
		int lastLayer = myPoints.layers-1;
		float getYX[] = new float[2];
		for(int p=0; p<myPoints.layerSize; p++){
			CornerData point = myPoints.corners[lastLayer][p];
			if(intersect.contains(point.x, point.y)){
				
				TriData t = myLines.findCollision(point.y, point.x);
				if(t != null){ 
					
					SmartblobUtil.getClosestPointToInfiniteLine(getYX, t, point.y, point.x);
					
					float vectorY = point.y-getYX[0];
					float vectorX = point.x-getYX[1];
					float vectorLen = (float)Math.sqrt(vectorY*vectorY + vectorX*vectorX);
					if(vectorLen == 0){
						
						continue;
					}
					float normVY = vectorY/vectorLen;
					float normVX = vectorX/vectorLen;
					
					
					normVY = -normVY;
					normVX = -normVX;
					
					
					
					
					float speedDotNorm = point.speedY*normVY + point.speedX*normVX;
					
					if(speedDotNorm < 0){
						
						
						float partOfSpeedVecY = normVY*speedDotNorm;
						float partOfSpeedVecX = normVX*speedDotNorm;
						point.speedY -= 2*partOfSpeedVecY;
						point.speedX -= 2*partOfSpeedVecX;
						
						
					}
				}
				/*LineData lineData = myLines.findCollision(point.y, point.x);
				if(lineData != null){ 
					throw new RuntimeException("TODO");
				}
				*/
			}
		}
	}

}
