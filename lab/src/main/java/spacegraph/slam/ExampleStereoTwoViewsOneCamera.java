/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spacegraph.slam;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.abst.geo.fitting.ModelManagerEpipolarMatrix;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.*;
import boofcv.gui.d3.ColorPoint3D;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.*;
import com.jogamp.opengl.GL2;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.signal.Bitmap2D;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;
import spacegraph.slam.raytrace.RayTracer;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.video.Draw;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Example demonstrating how to use to images taken from a single calibrated camera to create a stereo disparity image,
 * from which a dense 3D point cloud of the scene can be computed.  For this technique to work the camera's motion
 * needs to be approximately tangential to the direction the camera is pointing.  The code below assumes that the first
 * image is to the left of the second image.
 *
 * @author Peter Abeles
 */
public class ExampleStereoTwoViewsOneCamera {

	
	private static final int minDisparity = 15;
	private static final int maxDisparity = 100;
	private final PointCloudTiltPanel gui;
	List<AssociatedPair> matchedCalibrated = new ArrayList<>();
	List<AssociatedPair> matchedFeatures = new ArrayList();
	List<AssociatedPair> inliers = new FasterList<>();

	final static CameraPinholeRadial intrinsic = CalibrationIO.load(new StringReader(
			"# Pinhole camera model with radial and tangential distortion\n" +
					"# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape\n" +
					"# radial = radial distortion, (t1,t2) = tangential distortion\n" +
					"\n" +
					"pinhole:\n" +
					"  fx: 701.0116882676376\n" +
					"  fy: 698.6537946928421\n" +
					"  cx: 308.4551818095542\n" +
					"  cy: 246.84300560315452\n" +
					"  width: 640\n" +
					"  height: 480\n" +
					"  skew: 0.0\n" +
					"model: pinhole_radial_tangential\n" +
					"radial_tangential:\n" +
					"  radial:\n" +
					"  - -0.25559248570886445\n" +
					"  - 0.09997127476560613\n" +
					"  t1: 0.0\n" +
					"  t2: 0.0"));

	private GrayF32 next, prev;
	private GrayU8 distortedNext, distortedPrev;
	private final AssociationPanel assocPanel = new AssociationPanel(20);


	public static void main(String args[]) throws InterruptedException {
		ExampleStereoTwoViewsOneCamera e = new ExampleStereoTwoViewsOneCamera();
		new SpaceGraphPhys3D(new SimpleSpatial(e) {

			@Override
			public void renderAbsolute(GL2 gl, int dtMS) {

					for (ColorPoint3D p : e.gui.view.cloud) {
						int cc = p.rgb;
						gl.glColor3f(Bitmap2D.decodeRed(cc), Bitmap2D.decodeGreen(cc), Bitmap2D.decodeBlue(cc));
						Draw.rect(gl, (float) p.x, -(float) p.y, 2, 2, (float) p.z);
					}

			}


		}).show(800, 800, false);

		RayTracer r = RayTracer.raytracer();

		r.scene.camera.position.x = 4;
		r.scene.camera.position.y = 1;
		r.scene.camera.position.z = 1;

		r.update();
		r.renderProgressively();


		

		r.update();
		if (r.renderProgressively()) {
			
		}
		Thread.sleep(500);
		e.snap(r.image);

		Thread.sleep(100);



				
				


		new Thread(()->{
			while (true) {
				r.scene.camera.position.x = 4 + .15f;
				r.scene.camera.direction.x -= 0.01f;

				r.update();
				if (r.renderProgressively()) {
					
				}

				Util.sleep(500);
				e.snap(r.image);
			}

		}).start();

	}

	public ExampleStereoTwoViewsOneCamera() {
		gui = new PointCloudTiltPanel();
		gui.setPreferredSize(new Dimension(800, 800));
		ShowImages.showWindow(gui, "Point Cloud");

		ShowImages.showWindow(assocPanel, "Inlier Features").setSize(800, 800);

	}

	public synchronized void snap(BufferedImage input) {
		prev = next;
		distortedPrev = distortedNext;

		next = ConvertBufferedImage.convertFrom(input, (GrayF32)null);
		distortedNext = ConvertBufferedImage.convertFrom(input, (GrayU8) null);

		if (prev!=null) {
			update(input);
		}
	}

	protected boolean update(BufferedImage inLeft) {




		
		computeMatches(prev, next);

		
		convertToNormalizedCoordinates(matchedFeatures, intrinsic);

		
		Se3_F64 leftToRight = estimateCameraMotion(intrinsic);
		if (leftToRight == null)
			return false; 

		System.out.println(leftToRight);

		drawInliers(intrinsic, inliers);

		
		DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
		GrayU8 rectifiedLeft =
				distortedPrev.createSameShape();
		GrayU8 rectifiedRight =
				distortedNext.createSameShape();

		rectifyImages(distortedPrev, distortedNext, leftToRight, intrinsic, rectifiedLeft, rectifiedRight, rectifiedK);

		
		StereoDisparity<GrayS16, GrayF32> disparityAlg =
				FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
						minDisparity, maxDisparity,
						5, 5, 20, 1,
						0.1, GrayS16.class);

		
		GrayS16 derivLeft = new GrayS16(rectifiedLeft.width,rectifiedLeft.height);
		LaplacianEdge.process(rectifiedLeft, derivLeft);
		GrayS16 derivRight = new GrayS16(rectifiedRight.width,rectifiedRight.height);
		LaplacianEdge.process(rectifiedRight,derivRight);

		
		disparityAlg.process(derivLeft, derivRight);
		GrayF32 disparity = disparityAlg.getDisparity();

		








		double baseline = leftToRight.getT().norm();

		gui.configure(baseline, rectifiedK, new DoNothing2Transform2_F64(),				minDisparity, maxDisparity);
		gui.process(leftToRight, disparity, inLeft);



		System.out.println("Total found " + matchedCalibrated.size());
		System.out.println("Total Inliers " + inliers.size());

		return true;
	}

	/**
	 * Use the associate point feature example to create a list of {@link AssociatedPair} for use in computing the
	 * fundamental matrix.
	 */
	public void computeMatches( GrayF32 left , GrayF32 right ) {
		DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(
						1, 2, 0, 1, 9, 4, 4),
				null,null, GrayF32.class);
		

		ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
		AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 0.9f, true);

		ExampleAssociatePoints<GrayF32,BrightFeature> findMatches =
				new ExampleAssociatePoints<>(detDesc, associate, GrayF32.class);

		findMatches.associate(left,right);


		FastQueue<AssociatedIndex> matchIndexes = associate.getMatches();

		matchedFeatures.clear();

		for( int i = 0; i < matchIndexes.size; i++ ) {
			AssociatedIndex a = matchIndexes.get(i);
			matchedFeatures.add(new AssociatedPair(findMatches.pointsA.get(a.src) , findMatches.pointsB.get(a.dst)));
		}

	}

	/**
	 * Estimates the camera motion robustly using RANSAC and a set of associated points.
	 *
	 * @param intrinsic   Intrinsic camera parameters
	 * @param matchedNorm set of matched point features in normalized image coordinates
	 * @param inliers     OUTPUT: Set of inlier features from RANSAC
	 * @return Found camera motion.  Note translation has an arbitrary scale
	 */
	public Se3_F64 estimateCameraMotion(CameraPinholeRadial intrinsic)
	{
		ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion =
				FactoryMultiViewRobust.essentialRansac(
						new ConfigEssential(intrinsic),
						new ConfigRansac(200,0.5));

		if (!epipolarMotion.process(matchedCalibrated))
			return null;
			

		
		inliers.clear();
		inliers.addAll(epipolarMotion.getMatchSet());

		return epipolarMotion.getModelParameters();
	}

	/**
	 * Convert a set of associated point features from pixel coordinates into normalized image coordinates.
	 */
	public void convertToNormalizedCoordinates(List<AssociatedPair> matchedFeatures, CameraPinholeRadial intrinsic) {

		Point2Transform2_F64 p_to_n = LensDistortionOps.narrow(intrinsic).undistort_F64(true, false);

		matchedCalibrated.clear();
		for (int i = 0, matchedFeaturesSize = matchedFeatures.size(); i < matchedFeaturesSize; i++) {

			AssociatedPair p = matchedFeatures.get(i);

			AssociatedPair c = new AssociatedPair();

			p_to_n.compute(p.p1.x, p.p1.y, c.p1);
			p_to_n.compute(p.p2.x, p.p2.y, c.p2);

			matchedCalibrated.add(c);
		}

	}

	/**
	 * Remove lens distortion and rectify stereo images
	 *
	 * @param distortedLeft  Input distorted image from left camera.
	 * @param distortedRight Input distorted image from right camera.
	 * @param leftToRight    Camera motion from left to right
	 * @param intrinsic      Intrinsic camera parameters
	 * @param rectifiedLeft  Output rectified image for left camera.
	 * @param rectifiedRight Output rectified image for right camera.
	 * @param rectifiedK     Output camera calibration matrix for rectified camera
	 */
	public static void rectifyImages(GrayU8 distortedLeft,
									 GrayU8 distortedRight,
									 Se3_F64 leftToRight,
									 CameraPinholeRadial intrinsic,
									 GrayU8 rectifiedLeft,
									 GrayU8 rectifiedRight,
									 DMatrixRMaj rectifiedK) {

		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		

		
		DMatrixRMaj K = PerspectiveOps.calibrationMatrix(intrinsic, (DMatrixRMaj)null);

		rectifyAlg.process(K, new Se3_F64(), K, leftToRight);

		
		DMatrixRMaj rect1 = rectifyAlg.getRect1();
		DMatrixRMaj rect2 = rectifyAlg.getRect2();

		
		rectifiedK.set(rectifyAlg.getCalibrationMatrix());

		
		RectifyImageOps.allInsideLeft(intrinsic, rect1, rect2, rectifiedK);

		
		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3,3);
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3,3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageDistort<GrayU8,GrayU8> distortLeft =
				RectifyImageOps.rectifyImage(intrinsic, rect1_F32, BorderType.SKIP, distortedLeft.getImageType());
		ImageDistort<GrayU8,GrayU8> distortRight =
				RectifyImageOps.rectifyImage(intrinsic, rect2_F32, BorderType.SKIP, distortedRight.getImageType());

		distortLeft.apply(distortedLeft, rectifiedLeft);
		distortRight.apply(distortedRight, rectifiedRight);
	}

	/**
	 * Draw inliers for debugging purposes.  Need to convert from normalized to pixel coordinates.
	 */
	public void drawInliers(CameraPinholeRadial intrinsic,
								   List<AssociatedPair> normalized) {
		Point2Transform2_F64 n_to_p = LensDistortionOps.narrow(intrinsic).distort_F64(false,true);

		List<AssociatedPair> pixels = new ArrayList<>(normalized.size());

		for (AssociatedPair n : normalized) {
			AssociatedPair p = new AssociatedPair();

			n_to_p.compute(n.p1.x, n.p1.y, p.p1);
			n_to_p.compute(n.p2.x, n.p2.y, p.p2);

			pixels.add(p);
		}



		

		assocPanel.setAssociation(pixels);
		assocPanel.setImages(ConvertBufferedImage.extractBuffered(distortedPrev), ConvertBufferedImage.extractBuffered(distortedNext));
		assocPanel.repaint();
		assocPanel.setSize(500,100);

	}

	/**
	 * A Fundamental matrix describes the epipolar relationship between two images.  If two points, one from
	 * each image, match, then the inner product around the Fundamental matrix will be zero.  If a fundamental
	 * matrix is known, then information about the scene and its structure can be extracted.
	 *
	 * Below are two examples of how a Fundamental matrix can be computed using different.
	 * The robust technique attempts to find the best fit Fundamental matrix to the data while removing noisy
	 * matches, The simple version just assumes that all the matches are correct.  Similar techniques can be used
	 * to fit various other types of motion or structural models to observations.
	 *
	 * The input image and associated features are displayed in a window.  In another window, inlier features
	 * from robust model fitting are shown.
	 *
	 * @author Peter Abeles
	 */
	public static class ExampleFundamentalMatrix {

		/**
		 * Given a set of noisy observations, compute the Fundamental matrix while removing
		 * the noise.
		 *
		 * @param matches List of associated features between the two images
		 * @param inliers List of feature pairs that were determined to not be noise.
		 * @return The found fundamental matrix.
		 */
		public static DMatrixRMaj robustFundamental( List<AssociatedPair> matches ,
													 List<AssociatedPair> inliers ) {

			
			ModelManager<DMatrixRMaj> managerF = new ModelManagerEpipolarMatrix();
			
			Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumFundamental.LINEAR_7, 2);
			
			GenerateEpipolarMatrix generateF = new GenerateEpipolarMatrix(estimateF);

			
			DistanceFromModelResidual<DMatrixRMaj,AssociatedPair> errorMetric =
					new DistanceFromModelResidual<>(new FundamentalResidualSampson());

			
			ModelMatcher<DMatrixRMaj,AssociatedPair> robustF =
					new Ransac<>(123123, managerF, generateF, errorMetric, 6000, 0.1);

			
			if( !robustF.process(matches) )
				throw new IllegalArgumentException("Failed");

			
			inliers.addAll(robustF.getMatchSet());

			
			DMatrixRMaj F = new DMatrixRMaj(3,3);
			ModelFitter<DMatrixRMaj,AssociatedPair> refine =
					FactoryMultiView.refineFundamental(1e-8, 400, EpipolarError.SAMPSON);
			if( !refine.fitModel(inliers, robustF.getModelParameters(), F) )
				throw new IllegalArgumentException("Failed");

			
			return F;
		}





















































	}

    /**
     * After interest points have been detected in two images the next step is to associate the two
     * sets of images so that the relationship can be found.  This is done by computing descriptors for
     * each detected feature and associating them together.  In the code below abstracted interfaces are
     * used to allow different algorithms to be easily used.  The cost of this abstraction is that detector/descriptor
     * specific information is thrown away, potentially slowing down or degrading performance.
     *
     * @author Peter Abeles
     */
    public static class ExampleAssociatePoints<T extends ImageGray<T>, TD extends TupleDesc> {

        
        DetectDescribePoint<T, TD> detDesc;
        
        AssociateDescription<TD> associate;

        
        public List<Point2D_F64> pointsA;
        public List<Point2D_F64> pointsB;

        Class<T> imageType;

        public ExampleAssociatePoints(DetectDescribePoint<T, TD> detDesc,
                                      AssociateDescription<TD> associate,
                                      Class<T> imageType) {
            this.detDesc = detDesc;
            this.associate = associate;
            this.imageType = imageType;
        }

        /**
         * Detect and associate point features in the two images.  Display the results.
         */
        public void associate( BufferedImage imageA , BufferedImage imageB )
        {
            T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
            T inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType);
			associate(inputA, inputB);


			
			AssociationPanel panel = new AssociationPanel(20);
			panel.setAssociation(pointsA,pointsB,associate.getMatches());
			panel.setImages(imageA,imageB);

			ShowImages.showWindow(panel,"Associated Features",true);
		}

		public void associate(T inputA, T inputB) {
			
			pointsA = new ArrayList<>();
			pointsB = new ArrayList<>();

			
			FastQueue<TD> descA = UtilFeature.createQueue(detDesc,100);
			FastQueue<TD> descB = UtilFeature.createQueue(detDesc,100);

			
			describeImage(inputA,pointsA,descA);
			describeImage(inputB,pointsB,descB);

			
			associate.setSource(descA);
			associate.setDestination(descB);
			associate.associate();

		}

		/**
         * Detects features inside the two images and computes descriptions at those points.
         */
        private void describeImage(T input, List<Point2D_F64> points, FastQueue<TD> descs )
        {
            detDesc.detect(input);

            for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
                points.add( detDesc.getLocation(i).copy() );
                descs.grow().setTo(detDesc.getDescription(i));
            }
        }

        public static void main( String args[] ) {

            Class imageType = GrayF32.class;


            
            DetectDescribePoint detDesc = FactoryDetectDescribe.
                    surfStable(new ConfigFastHessian(1, 2, 300, 1, 9, 4, 4), null,null, imageType);


            ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
            AssociateDescription associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);

            
            ExampleAssociatePoints app = new ExampleAssociatePoints(detDesc,associate,imageType);

            BufferedImage imageA = UtilImageIO.loadImage(UtilIO.pathExample("stitch/kayak_01.jpg"));
            BufferedImage imageB = UtilImageIO.loadImage(UtilIO.pathExample("stitch/kayak_03.jpg"));

            app.associate(imageA,imageB);
        }
    }

	public static class DisparityPointCloudViewer extends JPanel {
		Deque<ColorPoint3D> cloud =
				new ConcurrentLinkedDeque<>();
				

		
		double baseline;

		
		DMatrixRMaj K;
		double focalLengthX;
		double focalLengthY;
		double centerX;
		double centerY;

		
		int minDisparity;
		
		int rangeDisparity;

		
		double range = 10;

		
		double offsetX;
		double offsetY;

		
		
		Pixel data[] = new Pixel[0];

		
		public int tiltAngle = 0;
		public double radius = 5;

		
		Point2Transform2_F64 rectifiedToColor;
		
		Point2D_F64 colorPt = new Point2D_F64();

		/**
		 * Stereo and intrinsic camera parameters
		 * @param baseline Stereo baseline (world units)
		 * @param K Intrinsic camera calibration matrix of rectified camera
		 * @param rectifiedToColor Transform from rectified pixels to the color image pixels.
		 * @param minDisparity Minimum disparity that's computed (pixels)
		 * @param maxDisparity Maximum disparity that's computed (pixels)
		 */
		public void configure(double baseline,
							  DMatrixRMaj K,
							  Point2Transform2_F64 rectifiedToColor,
							  int minDisparity, int maxDisparity) {
			this.K = K;
			this.rectifiedToColor = rectifiedToColor;
			this.baseline = baseline;
			this.focalLengthX = K.get(0,0);
			this.focalLengthY = K.get(1,1);
			this.centerX = K.get(0,2);
			this.centerY = K.get(1,2);
			this.minDisparity = minDisparity;

			this.rangeDisparity = maxDisparity-minDisparity;
		}

		/**
		 * Given the disparity image compute the 3D location of valid points and save pixel colors
		 * at that point
		 *
		 * @param disparity Disparity image
		 * @param color Color image of left camera
		 */
		public void process(ImageGray disparity , BufferedImage color ) {
			if( disparity instanceof GrayU8)
				process((GrayU8)disparity,color);
			else
				process((GrayF32)disparity,color);
		}

		private void process(GrayU8 disparity , BufferedImage color ) {
			throw new TODO();




























		}

		private void process(GrayF32 disparity , BufferedImage color ) {



			for( int y = 0; y < disparity.height; y++ ) {
				int index = disparity.startIndex + disparity.stride*y;

				for( int x = 0; x < disparity.width; x++ ) {
					float value = disparity.data[index++];

					if( value >= rangeDisparity )
						continue;

					value += minDisparity;

					if( value == 0 )
						continue;

					ColorPoint3D p = new ColorPoint3D();
					cloud.addLast(p);

					p.z = baseline*focalLengthX/value;
					p.x = p.z*(x - centerX)/focalLengthX;
					p.y = p.z*(y - centerY)/focalLengthY;

					getColor(disparity, color, x, y, p);
				}
			}
		}

		private void getColor(ImageBase disparity, BufferedImage color, int x, int y, ColorPoint3D p) {
			rectifiedToColor.compute(x,y,colorPt);
			if( BoofMiscOps.checkInside(disparity, colorPt.x, colorPt.y, 0) ) {
				p.rgb = color.getRGB((int)colorPt.x,(int)colorPt.y);
			} else {
				p.rgb = 0x000000;
			}
		}

		@Override
		public synchronized void paintComponent(Graphics g) {
			super.paintComponent(g);

			projectScene();

			int width = getWidth();
			int h = getHeight();

			int r = 2;
			int w = r*2+1;

			Graphics2D g2 = (Graphics2D)g;

			int index = 0;
			for( int y = 0; y < h; y++ ) {
				for( int x = 0; x < width; x++ ) {
					Pixel p = data[index++];
					if( p.rgb == -1 )
						continue;

					g2.setColor(new Color(p.rgb));
					g2.fillRect(x - r, y - r, w, w);
				}
			}
		}

		private void projectScene() {
			int w = getWidth();
			int h = getHeight();

			int N = w*h;

			if( data.length < N ) {
				data = new Pixel[ N ];
				for( int i = 0; i < N; i++ )
					data[i] = new Pixel();
			} else {
				for( int i = 0; i < N; i++ )
					data[i].reset();
			}

			Se3_F64 pose = createWorldToCamera();
			Point3D_F64 cameraPt = new Point3D_F64();
			Point2D_F64 pixel = new Point2D_F64();

			for (ColorPoint3D p : cloud) {

				SePointOps_F64.transform(pose,p,cameraPt);
				pixel.x = cameraPt.x/cameraPt.z;
				pixel.y = cameraPt.y/cameraPt.z;

				GeometryMath_F64.mult(K,pixel,pixel);

				int x = (int)pixel.x;
				int y = (int)pixel.y;

				if( x < 0 || y < 0 || x >= w || y >= h )
					continue;

				Pixel d = data[y*w+x];
				if( d.height > cameraPt.z ) {
					d.height = cameraPt.z;
					d.rgb = p.rgb;
				}
			}
		}

		public Se3_F64 createWorldToCamera() {
			
			double z =  
					baseline*focalLengthX/(minDisparity+rangeDisparity);

			double adjust = baseline/20.0;

			Vector3D_F64 rotPt = new Vector3D_F64(offsetX*adjust,offsetY*adjust,z* range);

			double radians = tiltAngle*Math.PI/180.0;
			DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,radians,0,0,null);

			Se3_F64 a = new Se3_F64(R,rotPt);

			return a;
		}

	}

	/**
	 * Contains information on visible pixels
	 */
	private static class Pixel
	{
		
		public double height;
		
		public int rgb;

		private Pixel() {
			reset();
		}

		public void reset() {
			height = Double.MAX_VALUE;
			rgb = -1;
		}
	}
	public static class PointCloudTiltPanel extends JPanel
			implements ActionListener, ChangeListener, MouseListener, MouseMotionListener
	{
		
		DisparityPointCloudViewer view;

		
		JButton homeButton;
		
		JSpinner rangeSpinner;
		
		JSlider tiltSlider;

		int maxPoints = 5500;

		
		double minRange = 0;
		double maxRange = 30;

		
		int prevX;
		int prevY;

		public PointCloudTiltPanel() {
			super(new BorderLayout());

			addMouseListener(this);
			addMouseMotionListener(this);

			view = new DisparityPointCloudViewer();
			JToolBar toolBar = createToolBar();

			add(toolBar, BorderLayout.PAGE_START);
			add(view, BorderLayout.CENTER);
		}

		private JToolBar createToolBar() {
			JToolBar toolBar = new JToolBar("Controls");

			homeButton = new JButton("Home");
			homeButton.addActionListener(this);

			rangeSpinner = new JSpinner(new SpinnerNumberModel(view.range, minRange, maxRange, 0.2));

			rangeSpinner.addChangeListener(this);
			rangeSpinner.setMaximumSize(rangeSpinner.getPreferredSize());

			tiltSlider = new JSlider(JSlider.HORIZONTAL,
					-120, 120, view.tiltAngle);
			tiltSlider.addChangeListener(this);
			tiltSlider.setMajorTickSpacing(60);
			tiltSlider.setPaintLabels(true);

			toolBar.add(homeButton);
			toolBar.add(new JToolBar.Separator(new Dimension(10,1)));
			toolBar.add(new JLabel("Range:"));
			toolBar.add(rangeSpinner);
			toolBar.add(new JToolBar.Separator(new Dimension(10,1)));
			toolBar.add(new JLabel("Tilt Angle:"));
			toolBar.add(tiltSlider);

			return toolBar;
		}

		/**
		 * Specified intrinsic camera parameters and disparity settings
		 *
		 * @param baseline Stereo baseline
		 * @param K rectified camera calibration matrix
		 */
		public void configure(double baseline,
							  DMatrixRMaj K,
							  Point2Transform2_F64 rectifiedToColor,
							  int minDisparity, int maxDisparity) {
			view.configure(baseline, K, rectifiedToColor, minDisparity, maxDisparity);
		}

		/**
		 * Updates the view, must be called in a GUI thread
		 */
		public void process(Se3_F64 newMotion, ImageGray disparity, BufferedImage color) {

			while (view.cloud.size() > maxPoints)
				view.cloud.removeFirst();

			newMotion = newMotion.invert(null);
			System.out.println(newMotion);

			for (Point3D_F64 p : view.cloud) {
				SePointOps_F64.transform(newMotion, p, p);
			}


			view.process(disparity,color);

			tiltSlider.removeChangeListener(this);
			tiltSlider.setValue(view.tiltAngle);
			tiltSlider.addChangeListener(this);

			repaint();
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			if( e.getSource() == homeButton ) {
				view.offsetX = 0;
				view.offsetY = 0;
				view.tiltAngle = 0;
				view.range = 1;

				tiltSlider.removeChangeListener(this);
				tiltSlider.setValue(view.tiltAngle);
				tiltSlider.addChangeListener(this);

				rangeSpinner.removeChangeListener(this);
				rangeSpinner.setValue(view.range);
				rangeSpinner.addChangeListener(this);
			}
			view.repaint();
		}

		@Override
		public void stateChanged(ChangeEvent e) {

			if( e.getSource() == rangeSpinner) {
				view.range = ((Number) rangeSpinner.getValue()).doubleValue();
			} else if( e.getSource() == tiltSlider ) {
				view.tiltAngle = ((Number)tiltSlider.getValue()).intValue();
			}
			view.repaint();
		}

		@Override
		public synchronized void mouseClicked(MouseEvent e) {

			double range = view.range;
			if( e.isShiftDown())
				range *= 0.75;
			else
				range *= 1.25;

			if( range < minRange) range = minRange;
			if( range > maxRange) range = maxRange;
			rangeSpinner.setValue(range);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			prevX = e.getX();
			prevY = e.getY();
		}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public synchronized void mouseDragged(MouseEvent e) {
			final int deltaX = e.getX()-prevX;
			final int deltaY = e.getY()-prevY;

			view.offsetX += deltaX;
			view.offsetY += deltaY;

			prevX = e.getX();
			prevY = e.getY();

			view.repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {}
	}

}