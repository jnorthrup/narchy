/** Ben F Rayfield offers this software opensource GNU GPL 2+ */
package smartblob;

import smartblob.blobs.layeredzigzag.CornerData;
import smartblob.blobs.layeredzigzag.LayeredZigzag;
import smartblob.blobs.layeredzigzag.TriData;
import smartblob.datastruct.Statsys;
import smartblob.smartblob.physics.ChangeSpeed;
import smartblob.smartblob.physics.SmartblobSim;
import smartblob.smartblob.physics.globalparts.BounceOnSimpleWall;
import smartblob.smartblob.physics.globalparts.CollisionsChangeSpeed;
import smartblob.smartblob.physics.globalparts.Gravity;
import smartblob.smartblob.physics.smartblobparts.AccelerateLinearlyFromDistanceConstraints;
import smartblob.smartblob.physics.smartblobparts.Friction;
import smartblob.smartblob.physics.smartblobparts.RelativeMotion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;




public class SmartblobUtil{

	public static final float EPSILON = .000001f;

	private SmartblobUtil(){}
	
	public static List<ChangeSpeed> defaultChangeSpeeds = Collections.unmodifiableList(Arrays.<ChangeSpeed>asList(
		new AccelerateLinearlyFromDistanceConstraints(10f),
		
		
		
		
		
		
			
		
		
		
		new Friction(2f),
		
		
		
			
		
		
		
		new RelativeMotion(10f)
		
			
		
			
		
	));
	
	public static LayeredZigzag simpleSmartblobExample(int layers, int layerSize, float r){
		
		
		
		LayeredZigzag blob = new LayeredZigzag(null, layers, layerSize, 100, 100, r);
		
		
		
		
		blob.mutablePhysics().addAll(defaultChangeSpeeds);
		
		return blob;
	}
	
	public static LayeredZigzag wavegear(
			Statsys brain, double y, double x, double outerMinRadius, double outerMaxRadius,
			int layers, int layerSize, double frequency){
		LayeredZigzag blob = new LayeredZigzag(brain, layers, layerSize, 0, 0, 0);
		for(int layer=0; layer<layers; layer++){
			double fraction = (double)layer/(layers-1);
			boolean layerIsOdd = (layer&1)==1;
			for(int p=0; p<layerSize; p++){
				
				CornerData c = blob.corners[layer][p];
				double minRadius = fraction*outerMinRadius;
				double maxRadius = fraction*outerMaxRadius;
				double radiusRange = maxRadius-minRadius;
				double angle = (double)p/layerSize*2*Math.PI;
				if(layerIsOdd) angle += Math.PI/layerSize;
				double recurseAngle = angle*frequency;
				double radius = minRadius + radiusRange*Math.sin(recurseAngle);
				c.y = (float)(y + radius*Math.sin(angle));
				c.x = (float)(x + radius*Math.cos(angle));
			}
		}
		blob.mutablePhysics().addAll(defaultChangeSpeeds);
		return blob;
	}
	
	/** Add smartblobs to it later */
	public static SmartblobSim newSimWithDefaultOptions(){
		return new SmartblobSim(
			
			
			new Gravity(100),
			
			
			
			new CollisionsChangeSpeed(),
			new BounceOnSimpleWall(0, true, false),
			new BounceOnSimpleWall(0, false, false),
			new BounceOnSimpleWall(500, true, true),
			new BounceOnSimpleWall(500, false, true)
		);
	}
	
	public static void moveAll(SmartblobSim sim, float secondsSinceLastMove){




		for(Smartblob blob : sim.smartblobs){
			blob.onStartUpdatePositions();
			if(blob instanceof LayeredZigzag){
				move((LayeredZigzag)blob, secondsSinceLastMove);
			}else{
				System.out.println(Smartblob.class.getName()+" type unknown: "+blob.getClass().getName());
			}
			blob.onEndUpdatePositions();
		}
	}
	
	public static void move(final LayeredZigzag z, final float secondsSinceLastMove){
		for(final CornerData layerOfCorners[] : z.corners){
			for(final CornerData cd : layerOfCorners){
				cd.y += cd.speedY*secondsSinceLastMove + cd.addToY;
				cd.x += cd.speedX*secondsSinceLastMove + cd.addToX;
				cd.addToY = 0;
				cd.addToX = 0;
			}
		}
	}
	
	
	
	/** Distance from the given point to closest point on the line segment.
	The LineData must be the first LineData in an inward facing TriData,
	which is true of each border line segment of a LayeredZigzag.
	*/
	public static float distanceToOuterLineSegmentSqr(float y, float x, TriData t){
		CornerData cornerA = t.adjacentCorners[0], cornerB = t.adjacentCorners[1];
		float ay = cornerA.y;
		float ax = cornerA.x;
		float by = cornerB.y;
		float bx = cornerB.x;
		
		
		
		float getYX[] = new float[2]; 
		getClosestPointToInfiniteLine(getYX, ay, ax, by, bx, y, x);
		float ly = getYX[0], lx = getYX[1];
		/*if(g != null){ 
			g.setColor(Color.red);
			g.fillRect((int)lx-5, (int)ly-5, 10, 10);
		}*/
		float minX = Math.min(ax,bx)- EPSILON;
		float maxX = Math.max(ax,bx)+ EPSILON;
		boolean lInXOfLine = minX < lx && lx < maxX;
		float minY = Math.min(ay,by)- EPSILON;
		float maxY = Math.max(ay,by)+ EPSILON;
		boolean lInYOfLine = minY < ly && ly < maxY;
		if(lInXOfLine){ 
			float plDy = y-ly, plDx = x-lx;
			return (float)/*Math.sqrt*/(plDy*plDy + plDx*plDx);
			
		}else{ 
			float aDy = y-ay, aDx = x-ax;
			float distToASquared = aDy*aDy + aDx*aDx;
			float bDy = y-by, bDx = x-bx;
			float distToBSquared = bDy*bDy + bDx*bDx;
			return (float)/*Math.sqrt*/(Math.min(distToASquared, distToBSquared));
			
		}
		
		/*
		float getYX[] = new float[2]; 
		float ly = getYX[0], lx = getYX[1];
		getClosestPointToInfiniteLine(getYX, ay, ax, by, bx, y, x);
		float abDy = by-ay, abDx = bx-ax;
		float distanceABSquared = abDy*abDy + abDx*abDx; 
		float alDy = ly-ay, alDx = lx-ax;
		float distanceALSquared = alDy*alDy + alDx*alDx; 
		float blDy = ly-by, blDx = lx-bx;
		float distanceBLSquared = blDy*blDy + blDx*blDx; 
		if(distanceALSquared < distanceBLSquared){ 
			if(distanceABSquared < distanceBLSquared){
				
				
				
				return .3f;
			}
		}else{ 
			if(distanceABSquared < distanceALSquared){
				
				
				
				return .7f;
			}
		}
		return 1f;
		*/
		
		
		/*float aDy = y-ay, aDx = x-ax;
		float distToA = (float)Math.sqrt(aDy*aDy + aDx*aDx);
		float bDy = y-by, bDx = x-bx;
		float distToB = (float)Math.sqrt(bDy*bDy + bDx*bDx);
		float minOfDistToAOrb = Math.min(distToA, distToB);
		*/
		
		/*
		
		float dy = by-ay, dx = bx-ax;
		double twoPointsOnLineDist = Math.sqrt(dy*dy + dx*dx);
		float lineDist = (float)((dy*x - dx*y + bx*ay - by*ax)/twoPointsOnLineDist);
		return lineDist;
		
		*/
		
		
		
		
		
		
	}
	
	/** Returns true if the point is on the side of the line that the nearest
	part of the smartblob is inside. It does not mean in the triangle literally.
	*/
	public static boolean isInsideBorder(float y, float x, TriData closestOuterTri){
		CornerData cornerA = closestOuterTri.adjacentCorners[0], cornerB = closestOuterTri.adjacentCorners[1];
		float y1 = cornerA.y;
		float x1 = cornerA.x;
		float y2 = cornerB.y;
		float x2 = cornerB.x;
		CornerData p2 = closestOuterTri.adjacentCorners[2];
		float ay = y, ax = x;
		float by = p2.y, bx = p2.x;
		
		
		
		
		
		
		float first = (y1-y2)*(ax-x1) + (x2-x1)*(ay-y1);
		float second = (y1-y2)*(bx-x1) + (x2-x1)*(by-y1);
		return 0 < first*second;
	}
	
	public static void getClosestPointToInfiniteLine(final float getYX[], final TriData borderTri, final float y, final float x){
		final CornerData[] bt = borderTri.adjacentCorners;
		CornerData c0 = bt[0], c1 = bt[1];
		getClosestPointToInfiniteLine(getYX, c0.y, c0.x, c1.y, c1.x, y, x);
	}
	
	public static void getClosestPointToInfiniteLine(final float getYX[], final float y1, final float x1, final float y2, final float x2, final float y, final float x){
		
		float xDelta = x2 - x1;
		float yDelta = y2 - y1;
		float u = ((x - x1) * xDelta + (y - y1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);
		getYX[0] = y1 + u * yDelta;
		getYX[1] = x1 + u * xDelta;
	}
	
	/*public static void getClosestPointToLineSegment(float getYX[], float y1, float x1, float y2, float x2, float y, float x){
		float aDy = y-y1, aDx = x-x1;
		float distToA = (float)Math.sqrt(aDy*aDy + aDx*aDx);
		float bDy = y-y2, bDx = x-x2;
		float distToB = (float)Math.sqrt(bDy*bDy + bDx*bDx);
		float minOfDistToAOrb = Math.min(distToA, distToB);
		TODO
	}*/
	
	/** Returns true if any triangle is pointing opposite of the way it should be.
	TODO I'm not sure how to calculate this because smartblob can form into any 2d shape,
	so if its folded does not depend on the angle of the triangle relative to center.
	It may depend on angle relative to near inner triangles.
	*/
	public static boolean isFolded(LayeredZigzag z){
		throw new RuntimeException("TODO");
	}
	
	
	
	
	

}
