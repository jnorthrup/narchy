package spacegraph.input.finger.impl;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.bundle.*;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.geo.selfcalib.EstimatePlaneAtInfinityGivenK;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.*;
import boofcv.gui.d3.DisparityToColorPointCloud;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.*;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.ConvertMatrixData;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.LazySurface;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.video.OrthoSurfaceGraph;
import spacegraph.video.VideoSource;
import spacegraph.video.VideoSurface;
import spacegraph.video.WebCam;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static spacegraph.SpaceGraph.window;

public class WebcamStereoTest {
    public static void main(String[] args) {

        VideoSource[] ab = WebCam.theFirst(2);

        OrthoSurfaceGraph g = window(new LazySurface(() -> new LabeledPane(new Gridding(new PushButton("stereo", ()-> stereo3d(ab[0], ab[1]))),
        new Gridding(
                new VideoSurface(ab[0]),
                new VideoSurface(ab[1])
        ))), 1400, 800);

    }

    static void stereo3d(VideoSource a, VideoSource b) {

        BufferedImage buff01 = ((WebCam)a).webcam.getDevice().getImage();
        BufferedImage buff02 = ((WebCam)b).webcam.getDevice().getImage();
        assert(buff01.getWidth() == buff02.getWidth());
        assert(buff01.getHeight() == buff02.getHeight());

        Planar<GrayU8> color01 = ConvertBufferedImage.convertFrom(buff01,true, ImageType.pl(3,GrayU8.class));
        Planar<GrayU8> color02 = ConvertBufferedImage.convertFrom(buff02,true, ImageType.pl(3,GrayU8.class));

        GrayU8 image01 = ConvertImage.average(color01,null);
        GrayU8 image02 = ConvertImage.average(color02,null);

        // Find a set of point feature matches
        List<AssociatedPair> matches = ExampleFundamentalMatrix.computeMatches(buff01, buff02);

        // Prune matches using the epipolar constraint. use a low threshold to prune more false matches
        List<AssociatedPair> inliers = new ArrayList<>();
        DMatrixRMaj F = ExampleFundamentalMatrix.robustFundamental(matches, inliers, /*0.1*/ 0.05);

        // Perform self calibration using the projective view extracted from F
        // Note that P1 = [I|0]
        System.out.println("Self calibration");
        DMatrixRMaj P2 = MultiViewOps.fundamentalToProjective(F);

        // Take a crude guess at the intrinsic parameters. Bundle adjustment will fix this later.
        int width = buff01.getWidth(), height = buff01.getHeight();
        double fx = width/2;
        double fy = fx;
        double cx = width/2;
        double cy = height/2;

        // Compute a transform from projective to metric by assuming we know the camera's calibration
        EstimatePlaneAtInfinityGivenK estimateV = new EstimatePlaneAtInfinityGivenK();
        estimateV.setCamera1(fx,fy,0,cx,cy);
        estimateV.setCamera2(fx,fy,0,cx,cy);

        Vector3D_F64 v = new Vector3D_F64(); // plane at infinity
        if( !estimateV.estimatePlaneAtInfinity(P2,v))
            throw new RuntimeException("Failed!");

        DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(fx,fy,0,cx,cy);
        DMatrixRMaj H = MultiViewOps.createProjectiveToMetric(K,v.x,v.y,v.z,1,null);
        DMatrixRMaj P2m = new DMatrixRMaj(3,4);
        CommonOps_DDRM.mult(P2,H,P2m);

        // Decompose and get the initial estimate for translation
        DMatrixRMaj tmp = new DMatrixRMaj(3,3);
        Se3_F64 view1_to_view2 = new Se3_F64();
        MultiViewOps.decomposeMetricCamera(P2m,tmp,view1_to_view2);

        //------------------------- Setting up bundle adjustment
        // bundle adjustment will provide a more refined and accurate estimate of these parameters
        System.out.println("Configuring bundle adjustment");

        // Construct bundle adjustment data structure
        SceneStructureMetric structure = new SceneStructureMetric(false);
        SceneObservations observations = new SceneObservations(2);

        // We will assume that the camera has fixed intrinsic parameters
        structure.initialize(1,2,inliers.size());
        BundlePinholeSimplified bp = new BundlePinholeSimplified();
        bp.f = fx;
        structure.setCamera(0,false,bp);

        // The first view is the world coordinate system
        structure.setView(0,true,new Se3_F64());
        structure.connectViewToCamera(0,0);
        // Second view was estimated previously
        structure.setView(1,false,view1_to_view2);
        structure.connectViewToCamera(1,0);

        for (int i = 0; i < inliers.size(); i++) {
            AssociatedPair t = inliers.get(i);

            // substract out the camera center from points. This allows a simple camera model to be used and
            // errors in the this coordinate tend to be non-fatal
            observations.getView(0).add(i,(float)(t.p1.x-cx),(float)(t.p1.y-cy));
            observations.getView(1).add(i,(float)(t.p2.x-cx),(float)(t.p2.y-cy));

            // each point is visible in both of the views
            structure.connectPointToView(i,0);
            structure.connectPointToView(i,1);
        }

        // initial location of points is found through triangulation
        MultiViewOps.triangulatePoints(structure,observations);

        //------------------ Running Bundle Adjustment
        System.out.println("Performing bundle adjustment");
        ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
        configLM.dampeningInitial = 1e-3;
        configLM.hessianScaling = false;
        ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
        configSBA.configOptimizer = configLM;

        // Create and configure the bundle adjustment solver
        BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
        // prints out useful debugging information that lets you know how well it's converging
        bundleAdjustment.setVerbose(System.out,0);
        // Specifies convergence criteria
        bundleAdjustment.configure(1e-6, 1e-6, 100);

        // Scaling improve accuracy of numerical calculations
        ScaleSceneStructure bundleScale = new ScaleSceneStructure();
        bundleScale.applyScale(structure,observations);

        bundleAdjustment.setParameters(structure,observations);
        bundleAdjustment.optimize(structure);

        // Sometimes pruning outliers help improve the solution. In the stereo case the errors are likely
        // to already fatal
        PruneStructureFromSceneMetric pruner = new PruneStructureFromSceneMetric(structure,observations);
        pruner.pruneObservationsByErrorRank(0.85);
        pruner.prunePoints(1);
        bundleAdjustment.setParameters(structure,observations);
        bundleAdjustment.optimize(structure);

        bundleScale.undoScale(structure,observations);

        System.out.println("\nCamera");
        for (int i = 0; i < structure.cameras.size(); i++) {
            System.out.println(structure.cameras.get(i).getModel());
        }
        System.out.println("\n\nworldToView");
        for (int i = 0; i < structure.views.size(); i++) {
            System.out.println(structure.views.get(i).worldToView);
        }

        // display the inlier matches found using the robust estimator
        System.out.println("\n\nComputing Stereo Disparity");
        BundlePinholeSimplified cp = structure.getCameras().get(0).getModel();
        CameraPinholeBrown intrinsic = new CameraPinholeBrown();
        intrinsic.fsetK(cp.f,cp.f,0,cx,cy,width,height);
        intrinsic.fsetRadial(cp.k1,cp.k2);

        Se3_F64 leftToRight = structure.views.get(1).worldToView;

        computeStereoCloud(image01,image02,color01,color02,intrinsic,intrinsic,leftToRight,0,250);
    }

    static void computeStereoCloud( GrayU8 distortedLeft, GrayU8 distortedRight ,
                                           Planar<GrayU8> colorLeft, Planar<GrayU8> colorRight,
                                           CameraPinholeBrown intrinsicLeft ,
                                           CameraPinholeBrown intrinsicRight ,
                                           Se3_F64 leftToRight ,
                                           int minDisparity , int maxDisparity) {

//		drawInliers(origLeft, origRight, intrinsic, inliers);
        int width = distortedLeft.width;
        int height = distortedRight.height;

        // Rectify and remove lens distortion for stereo processing
        DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
        DMatrixRMaj rectifiedR = new DMatrixRMaj(3, 3);

        // rectify a colored image
        Planar<GrayU8> rectColorLeft = colorLeft.createSameShape();
        Planar<GrayU8> rectColorRight = colorLeft.createSameShape();
        GrayU8 rectMask = new GrayU8(colorLeft.width,colorLeft.height);

        rectifyImages(colorLeft, colorRight, leftToRight, intrinsicLeft,intrinsicRight,
                rectColorLeft, rectColorRight,rectMask, rectifiedK,rectifiedR);

        if(rectifiedK.get(0,0) < 0)
            throw new RuntimeException("Egads");

        System.out.println("Rectified K");
        rectifiedK.print();

        System.out.println("Rectified R");
        rectifiedR.print();

        GrayU8 rectifiedLeft = distortedLeft.createSameShape();
        GrayU8 rectifiedRight = distortedRight.createSameShape();
        ConvertImage.average(rectColorLeft,rectifiedLeft);
        ConvertImage.average(rectColorRight,rectifiedRight);

        // compute disparity
        StereoDisparity<GrayS16, GrayF32> disparityAlg =
                FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
                        minDisparity, maxDisparity, 6, 6, 30, 3, 0.05, GrayS16.class);

        // Apply the Laplacian across the image to add extra resistance to changes in lighting or camera gain
        GrayS16 derivLeft = new GrayS16(width,height);
        GrayS16 derivRight = new GrayS16(width,height);
        GImageDerivativeOps.laplace(rectifiedLeft, derivLeft, BorderType.EXTENDED);
        GImageDerivativeOps.laplace(rectifiedRight,derivRight, BorderType.EXTENDED);

        // process and return the results
        disparityAlg.process(derivLeft, derivRight);
        GrayF32 disparity = disparityAlg.getDisparity();
        RectifyImageOps.applyMask(disparity,rectMask,0);

        // show results
        BufferedImage visualized = VisualizeImageData.disparity(disparity, null, minDisparity, maxDisparity, 0);

        BufferedImage outLeft = ConvertBufferedImage.convertTo(rectColorLeft, new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB),true);
        BufferedImage outRight = ConvertBufferedImage.convertTo(rectColorRight, new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB),true);

        ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification",true);
        ShowImages.showWindow(visualized, "Disparity",true);

        showPointCloud(disparity, outLeft, leftToRight, rectifiedK,rectifiedR, minDisparity, maxDisparity);
    }
    /**
     * Show results as a point cloud
     */
    static void showPointCloud(ImageGray disparity, BufferedImage left,
                                      Se3_F64 motion, DMatrixRMaj rectifiedK , DMatrixRMaj rectifiedR,
                                      int minDisparity, int maxDisparity)
    {
        DisparityToColorPointCloud d2c = new DisparityToColorPointCloud();
        double baseline = motion.getT().norm();
        d2c.configure(baseline, rectifiedK, rectifiedR, new DoNothing2Transform2_F64(), minDisparity, maxDisparity);
        d2c.process(disparity,left);

        CameraPinhole rectifiedPinhole = PerspectiveOps.matrixToPinhole(rectifiedK,disparity.width,disparity.height,null);

        // skew the view to make the structure easier to see
        Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.eulerXyz(-baseline*5,0,0,0,0.2,0,null);

        PointCloudViewer pcv = VisualizeData.createPointCloudViewer();
        pcv.setCameraHFov(PerspectiveOps.computeHFov(rectifiedPinhole));
        pcv.setCameraToWorld(cameraToWorld);
        pcv.setTranslationStep(baseline/3);
        pcv.addCloud(d2c.getCloud(),d2c.getCloudColor());
        pcv.setDotSize(1);
        pcv.setTranslationStep(baseline/10);

        pcv.getComponent().setPreferredSize(new Dimension(left.getWidth(), left.getHeight()));
        ShowImages.showWindow(pcv.getComponent(), "Point Cloud", true);
    }
    /**
     * Remove lens distortion and rectify stereo images
     *
     * @param distortedLeft  Input distorted image from left camera.
     * @param distortedRight Input distorted image from right camera.
     * @param leftToRight    Camera motion from left to right
     * @param intrinsicLeft  Intrinsic camera parameters
     * @param rectifiedLeft  Output rectified image for left camera.
     * @param rectifiedRight Output rectified image for right camera.
     * @param rectifiedMask  Mask that indicates invalid pixels in rectified image. 1 = valid, 0 = invalid
     * @param rectifiedK     Output camera calibration matrix for rectified camera
     */
    static <T extends ImageBase<T>> void rectifyImages(T distortedLeft,
                       T distortedRight,
                       Se3_F64 leftToRight,
                       CameraPinholeBrown intrinsicLeft,
                       CameraPinholeBrown intrinsicRight,
                       T rectifiedLeft,
                       T rectifiedRight,
                       GrayU8 rectifiedMask,
                       DMatrixRMaj rectifiedK,
                       DMatrixRMaj rectifiedR) {
        RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

        // original camera calibration matrices
        DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(intrinsicLeft, (DMatrixRMaj)null);
        DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(intrinsicRight, (DMatrixRMaj)null);

        rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);

        // rectification matrix for each image
        DMatrixRMaj rect1 = rectifyAlg.getRect1();
        DMatrixRMaj rect2 = rectifyAlg.getRect2();
        rectifiedR.set(rectifyAlg.getRectifiedRotation());

        // New calibration matrix,
        rectifiedK.set(rectifyAlg.getCalibrationMatrix());

        // Adjust the rectification to make the view area more useful
        RectifyImageOps.fullViewLeft(intrinsicLeft, rect1, rect2, rectifiedK);

        // undistorted and rectify images
        FMatrixRMaj rect1_F32 = new FMatrixRMaj(3,3);
        FMatrixRMaj rect2_F32 = new FMatrixRMaj(3,3);
        ConvertMatrixData.convert(rect1, rect1_F32);
        ConvertMatrixData.convert(rect2, rect2_F32);

        // Extending the image prevents a harsh edge reducing false matches at the image border
        // SKIP is another option, possibly a tinny bit faster, but has a harsh edge which will need to be filtered
        ImageDistort<T,T> distortLeft =
                RectifyImageOps.rectifyImage(intrinsicLeft, rect1_F32, BorderType.EXTENDED, distortedLeft.getImageType());
        ImageDistort<T,T> distortRight =
                RectifyImageOps.rectifyImage(intrinsicRight, rect2_F32, BorderType.EXTENDED, distortedRight.getImageType());

        distortLeft.apply(distortedLeft, rectifiedLeft,rectifiedMask);
        distortRight.apply(distortedRight, rectifiedRight);
    }

    /*
     * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
     *
     * This file is part of BoofCV (http://boofcv.org).
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *   http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */



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
    static class ExampleFundamentalMatrix {

        /**
         * Given a set of noisy observations, compute the Fundamental matrix while removing
         * the noise.
         *
         * @param matches List of associated features between the two images
         * @param inliers List of feature pairs that were determined to not be noise.
         * @return The found fundamental matrix.
         */
        public static DMatrixRMaj robustFundamental( List<AssociatedPair> matches ,
                                                     List<AssociatedPair> inliers , double inlierThreshold ) {

            ConfigRansac configRansac = new ConfigRansac();
            configRansac.inlierThreshold = inlierThreshold;
            configRansac.maxIterations = 1000;
            ConfigFundamental configFundamental = new ConfigFundamental();
            configFundamental.which = EnumFundamental.LINEAR_7;
            configFundamental.numResolve = 2;
            configFundamental.errorModel = ConfigFundamental.ErrorModel.GEOMETRIC;
            // geometric error is the most accurate error metric, but also the slowest to compute. See how the
            // results change if you switch to sampson and how much faster it is. You also should adjust
            // the inlier threshold.

            ModelMatcher<DMatrixRMaj, AssociatedPair> ransac =
                    FactoryMultiViewRobust.fundamentalRansac(configFundamental,configRansac);

            // Estimate the fundamental matrix while removing outliers
            if( !ransac.process(matches) )
                throw new IllegalArgumentException("Failed");

            // save the set of features that were used to compute the fundamental matrix
            inliers.addAll(ransac.getMatchSet());

            // Improve the estimate of the fundamental matrix using non-linear optimization
            DMatrixRMaj F = new DMatrixRMaj(3,3);
            ModelFitter<DMatrixRMaj,AssociatedPair> refine =
                    FactoryMultiView.fundamentalRefine(1e-8, 400, EpipolarError.SAMPSON);
            if( !refine.fitModel(inliers, ransac.getModelParameters(), F) )
                throw new IllegalArgumentException("Failed");

            // Return the solution
            return F;
        }

        /**
         * If the set of associated features are known to be correct, then the fundamental matrix can
         * be computed directly with a lot less code.  The down side is that this technique is very
         * sensitive to noise.
         */
        public static DMatrixRMaj simpleFundamental( List<AssociatedPair> matches ) {
            // Use the 8-point algorithm since it will work with an arbitrary number of points
            Estimate1ofEpipolar estimateF = FactoryMultiView.fundamental_1(EnumFundamental.LINEAR_8, 0);

            DMatrixRMaj F = new DMatrixRMaj(3,3);
            if( !estimateF.process(matches,F) )
                throw new IllegalArgumentException("Failed");

            // while not done here, this initial linear estimate can be refined using non-linear optimization
            // as was done above.
            return F;
        }

        /**
         * Use the associate point feature example to create a list of {@link AssociatedPair} for use in computing the
         * fundamental matrix.
         */
        public static List<AssociatedPair> computeMatches(BufferedImage left , BufferedImage right ) {
            DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                    new ConfigFastHessian(0, 2, 400, 1, 9, 4, 4), null,null, GrayF32.class);
//		DetectDescribePoint detDesc = FactoryDetectDescribe.sift(null,new ConfigSiftDetector(2,0,200,5),null,null);

            ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
            AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 0.1, true);

            ExampleAssociatePoints<GrayF32,BrightFeature> findMatches =
                    new ExampleAssociatePoints<>(detDesc, associate, GrayF32.class);

            findMatches.associate(left,right);

            FastQueue<AssociatedIndex> matchIndexes = associate.getMatches();

            List<AssociatedPair> matches;
            int bound = matchIndexes.size;
            matches = IntStream.range(0, bound).mapToObj(matchIndexes::get).map(a -> new AssociatedPair(findMatches.pointsA.get(a.src), findMatches.pointsB.get(a.dst))).collect(Collectors.toList());

            return matches;
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
    static class ExampleAssociatePoints<T extends ImageGray<T>, TD extends TupleDesc> {

        // algorithm used to detect and describe interest points
        DetectDescribePoint<T, TD> detDesc;
        // Associated descriptions together by minimizing an error metric
        AssociateDescription<TD> associate;

        // location of interest points
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
        public void associate(BufferedImage imageA, BufferedImage imageB) {
            T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
            T inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType);

            // stores the location of detected interest points
            pointsA = new ArrayList<>();
            pointsB = new ArrayList<>();

            // stores the description of detected interest points
            FastQueue<TD> descA = UtilFeature.createQueue(detDesc, 100);
            FastQueue<TD> descB = UtilFeature.createQueue(detDesc, 100);

            // describe each image using interest points
            describeImage(inputA, pointsA, descA);
            describeImage(inputB, pointsB, descB);

            // Associate features between the two images
            associate.setSource(descA);
            associate.setDestination(descB);
            associate.associate();

            // display the results
            AssociationPanel panel = new AssociationPanel(20);
            panel.setAssociation(pointsA, pointsB, associate.getMatches());
            panel.setImages(imageA, imageB);

            ShowImages.showWindow(panel, "Associated Features", true);
        }

        /**
         * Detects features inside the two images and computes descriptions at those points.
         */
        private void describeImage(T input, List<Point2D_F64> points, FastQueue<TD> descs) {
            detDesc.detect(input);

            for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
                points.add(detDesc.getLocation(i).copy());
                descs.grow().setTo(detDesc.getDescription(i));
            }
        }
    }
}
