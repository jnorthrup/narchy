package spacegraph.slam;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.geo.ConfigEssential;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.wrapper.DynamicWebcamInterface;
import boofcv.io.wrapper.WebcamInterface;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import com.github.sarxos.webcam.Webcam;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import jcog.Util;
import jcog.data.list.FasterList;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a stereo camera system. Additional
 * information on the scene can be optionally extracted from the algorithm if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryStereo {

	public static void main( String args[] ) {





		



		

		
		

		WebcamInterface webcamInterface = new DynamicWebcamInterface();
		webcamInterface.open(null, 640, 480, ImageType.single(GrayU8.class));


		
		PkltConfig configKlt = new PkltConfig();
		configKlt.pyramidScaling = new int[]{1, 2, 4, 8};
		configKlt.templateRadius = 3;

		PointTrackerTwoPass<GrayU8> tracker =
				FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(300, 3, 1),
						GrayU8.class, GrayS16.class);

		
		StereoDisparitySparse<GrayU8> disparity =
				FactoryStereoDisparity.regionSparseWta(0, 150, 3, 3, 50, -1, true, GrayU8.class);

		
		StereoVisualOdometry<GrayU8> visualOdometry = FactoryVisualOdometry.stereoDepth(1.5,120, 2,300,50,true,
				disparity, tracker, GrayU8.class);

		





		GrayU8 left = null, right = null;

		StereoParameters stereoParam = new StereoParameters(
				ExampleStereoTwoViewsOneCamera.intrinsic, ExampleStereoTwoViewsOneCamera.intrinsic,
				new Se3_F64());

		
		List<AssociatedPair> matchedFeatures = new FasterList();

		ImagePanel i = ShowImages.showWindow(Webcam.getDefault().getImage(),"cam");

		while( true ) {
			left = right;
			right = ConvertBufferedImage.convertFrom(Webcam.getDefault().getImage(), (GrayU8)null);
			if (left == null) {
				left = right;
			}

			i.setImageRepaint(ConvertBufferedImage.convertTo(right, null));


			computeMatches(left, right, matchedFeatures);

			

			Se3_F64 cameraMotion = estimateCameraMotion(ExampleStereoTwoViewsOneCamera.intrinsic, matchedFeatures);
			if (cameraMotion == null) {
				
			} else {

				System.out.println("motion: " + cameraMotion.getT());

				visualOdometry.setCalibration(stereoParam);

				if( !visualOdometry.process(left,right) ) {
					
					System.out.println("odom fail");
				} else {

					Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
					Vector3D_F64 T = leftToWorld.getT();

					System.out.printf("Location %8.2f %8.2f %8.2f      inliers %s\n", T.x, T.y, T.z, inlierPercent(visualOdometry));
				}

			}

			Util.sleep(10);
		}
	}

	/**
	 * Use the associate point feature example to create a list of {@link AssociatedPair} for use in computing the
	 * fundamental matrix.
	 */
	static public void computeMatches(GrayU8 left, GrayU8 right, List<AssociatedPair> matchedFeatures) {
		DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(



				new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null,null, GrayU8.class);

		

		ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
		AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 1, true);

		ExampleStereoTwoViewsOneCamera.ExampleAssociatePoints<GrayU8,BrightFeature> findMatches =
				new ExampleStereoTwoViewsOneCamera.ExampleAssociatePoints<>(detDesc, associate, GrayF32.class);

		findMatches.associate(left,right);


		FastQueue<AssociatedIndex> matchIndexes = associate.getMatches();

		matchedFeatures.clear();

		for( int i = 0; i < matchIndexes.size; i++ ) {
			AssociatedIndex a = matchIndexes.get(i);
			matchedFeatures.add(new AssociatedPair(findMatches.pointsA.get(a.src) , findMatches.pointsB.get(a.dst)));
		}

	}


	public static Se3_F64 estimateCameraMotion(CameraPinholeRadial intrinsic, List<AssociatedPair> x)
	{
		ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion =
				FactoryMultiViewRobust.essentialRansac(
						new ConfigEssential(intrinsic),
						new ConfigRansac(200,0.5));

		if (!epipolarMotion.process(x))
			return null;
		

		



		return epipolarMotion.getModelParameters();
	}
	/**
	 * If the algorithm implements AccessPointTracks3D, then count the number of inlier features
	 * and return a string.
	 */
	public static String inlierPercent(StereoVisualOdometry alg) {
		if( !(alg instanceof AccessPointTracks3D))
			return "";

		AccessPointTracks3D access = (AccessPointTracks3D)alg;

		int count = 0;
		int N = access.getAllTracks().size();
		for( int i = 0; i < N; i++ ) {
			if( access.isInlier(i) )
				count++;
		}

		return String.format("%%%5.3f", 100.0 * count / N);
	}
}