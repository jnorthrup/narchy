//package spacegraph.slam;
//
//import boofcv.abst.fiducial.CalibrationFiducialDetector;
//import boofcv.abst.fiducial.calib.ConfigChessboard;
//import boofcv.alg.distort.LensDistortionNarrowFOV;
//import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
//import boofcv.factory.fiducial.FactoryFiducial;
//import boofcv.gui.MousePauseHelper;
//import boofcv.gui.PanelGridPanel;
//import boofcv.gui.d3.PointCloudViewer;
//import boofcv.gui.image.ImagePanel;
//import boofcv.gui.image.ShowImages;
//import boofcv.io.image.SimpleImageSequence;
//import boofcv.io.wrapper.DefaultMediaManager;
//import boofcv.struct.calib.CameraPinholeRadial;
//import boofcv.struct.image.GrayF32;
//import boofcv.struct.image.ImageType;
//import georegression.geometry.ConvertRotation3D_F64;
//import georegression.struct.point.Point2D_F64;
//import georegression.struct.point.Point3D_F64;
//import georegression.struct.point.Vector3D_F64;
//import georegression.struct.se.Se3_F64;
//import georegression.transform.se.SePointOps_F64;
//import org.ejml.data.DMatrixRMaj;
//
//import java.awt.*;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * The 6-DOF pose of calibration targets can be estimated very accurately[*] once a camera has been calibrated.
// * In this example the high level FiducialDetector interface is used with a chessboard calibration target to
// * process a video sequence. Once the pose of the target is known the location of each calibration point is
// * found in the camera frame and visualized.
// *
// * [*] Accuracy is dependent on a variety of factors. Calibration targets are primarily designed to be viewed up close
// * and their accuracy drops with range, as can be seen in this example.
// *
// * @author Peter Abeles
// */
//public class ExamplePoseOfCalibrationTarget {
//
//	public static void main( String args[] ) {
//
//
//		CameraPinholeRadial intrinsic =
//
//				ExampleStereoTwoViewsOneCamera2.intrinsic;
//		LensDistortionNarrowFOV lensDistortion = new LensDistortionRadialTangential(intrinsic);
//
//
//
//
//
//		SimpleImageSequence<GrayF32> cam = DefaultMediaManager.INSTANCE.openCamera(null, 640, 480, ImageType.single(GrayF32.class));
//
//
//
//		CalibrationFiducialDetector<GrayF32> detector =
//				FactoryFiducial.calibChessboard(new ConfigChessboard(4, 5, 0.03),GrayF32.class);
//
//		detector.setLensDistortion(lensDistortion,intrinsic.width,intrinsic.height);
//
//
//		List<Point2D_F64> calibPts = detector.getCalibrationPoints();
//
//
//		PointCloudViewer viewer = new PointCloudViewer(intrinsic, 0.01);
//
//		DMatrixRMaj rotY = ConvertRotation3D_F64.rotY(-Math.PI/2.0,null);
//		viewer.setWorldToCamera(new Se3_F64(rotY,new Vector3D_F64(0.75,0,1.25)));
//		ImagePanel imagePanel = new ImagePanel(intrinsic.width, intrinsic.height);
//		viewer.setPreferredSize(new Dimension(intrinsic.width,intrinsic.height));
//		PanelGridPanel gui = new PanelGridPanel(1,imagePanel,viewer);
//		gui.setMaximumSize(gui.getPreferredSize());
//		ShowImages.showWindow(gui,"Calibration Target Pose",true);
//
//
//		MousePauseHelper pauseHelper = new MousePauseHelper(gui);
//
//
//		List<Point3D_F64> path = new ArrayList<>();
//
//
//		Se3_F64 targetToCamera = new Se3_F64();
//		while( true /*video.hasNext() */ ) {
//
//
//
//			detector.detect( cam.next());
//
//			if( detector.totalFound() == 1 ) {
//				detector.getFiducialToCamera(0, targetToCamera);
//
//
//				viewer.reset();
//
//				Point3D_F64 center = new Point3D_F64();
//				SePointOps_F64.transform(targetToCamera, center, center);
//				path.addAt(center);
//
//				for (Point3D_F64 p : path) {
//					viewer.addPoint(p.x, p.y, p.z, 0x00FF00);
//				}
//
//				for (int j = 0; j < calibPts.size(); j++) {
//					Point2D_F64 p = calibPts.get(j);
//					Point3D_F64 p3 = new Point3D_F64(p.x, p.y, 0);
//					SePointOps_F64.transform(targetToCamera, p3, p3);
//					viewer.addPoint(p3.x, p3.y, p3.z, 0);
//				}
//			}
//
//			imagePanel.setImage(cam.getGuiImage());
//			viewer.repaint();
//			imagePanel.repaint();
//
//
//
//
//
//		}
//	}
//}