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

package boofcv.examples.stereo;


import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.PruneStructureFromSceneMetric;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic.Intrinsic;
import boofcv.alg.sfm.structure.ThreeViewEstimateMetricScene;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.*;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.*;
import boofcv.visualize.PointCloudViewer;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import jcog.exe.Exe;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.ConvertMatrixData;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.ImageChip;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.windo.GraphEdit;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static boofcv.alg.geo.MultiViewOps.triangulatePoints;


/**
 * In this example three uncalibrated images are used to compute a point cloud. Extrinsic as well as all intrinsic
 * parameters (e.g. focal length and lens distortion) are found. Stereo disparity is computed between two of
 * the three views and the point cloud derived from that. To keep the code (relatively) simple, extra steps which
 * improve convergence have been omitted. See {@link boofcv.alg.sfm.structure.ThreeViewEstimateMetricScene} for
 * a more robust version of what has been presented here. Even with these simplifications this example can be
 * difficult to fully understand.
 * <p>
 * Three images produce a more stable "practical" algorithm when dealing with uncalibrated images.
 * With just two views its impossible to remove all false matches since an image feature can lie any where
 * along an epipolar line in other other view. Even with three views, results are not always stable or 100% accurate
 * due to scene geometry and here the views were captured. In general you want a well textured scene with objects
 * up close and far away, and images taken with translational
 * motion. Pure rotation and planar scenes are impossible to estimate the structure from.
 * <p>
 * Steps:
 * <ol>
 * <li>Feature Detection (e.g. SURF)</li>
 * <li>Two view association</li>
 * <li>Find 3 View Tracks</li>
 * <li>Fit Trifocal tensor using RANSAC</li>
 * <li>Get and refine camera matrices</li>
 * <li>Compute dual absolute quadratic</li>
 * <li>Estimate intrinsic parameters from DAC</li>
 * <li>Estimate metric scene structure</li>
 * <li>Sparse bundle adjustment</li>
 * <li>Rectify two of the images</li>
 * <li>Compute stereo disparity</li>
 * <li>Convert into a point cloud</li>
 * </ol>
 * <p>
 * For a more stable and accurate version this example see {@link ThreeViewEstimateMetricScene}.
 *
 * @author Peter Abeles
 */
public class ExampleTrifocalStereoUncalibrated extends Bordering {

    final TypedPort<BufferedImage> a, b, c;
    BufferedImage ai, bi, ci;
    final AtomicBoolean busy = new AtomicBoolean(false);

    public ExampleTrifocalStereoUncalibrated() {
        a = new TypedPort<>(BufferedImage.class);
        a.on(aa -> {
            ai = aa;
            update();
        });
        b = new TypedPort<>(BufferedImage.class);
        b.on(aa -> {
            bi = aa;
            update();
        });
        c = new TypedPort<>(BufferedImage.class);
        c.on(aa -> {
            ci = aa;
            update();
        });

        set(W, new Gridding(a, b, c));
    }


    protected synchronized void update() {
        if (ai != null && bi != null && ci != null) {
            if (busy.compareAndSet(false, true)) {
                Exe.invokeLater(() -> {
                    System.out.println("updating");
                    try {
                        update(ai, bi, ci);
                    } finally {
                        busy.set(false);
                    }
                });
            } else {
                System.out.println("busy");
            }
        }
    }

    synchronized private void update(BufferedImage buff01, BufferedImage buff02, BufferedImage buff03) {


        Planar<GrayU8> color01 = ConvertBufferedImage.convertFrom(buff01, true, ImageType.pl(3, GrayU8.class));
        Planar<GrayU8> color02 = ConvertBufferedImage.convertFrom(buff02, true, ImageType.pl(3, GrayU8.class));
        Planar<GrayU8> color03 = ConvertBufferedImage.convertFrom(buff03, true, ImageType.pl(3, GrayU8.class));

        GrayU8 image01 = ConvertImage.average(color01, null);
        GrayU8 image02 = ConvertImage.average(color02, null);
        GrayU8 image03 = ConvertImage.average(color03, null);

        // using SURF features. Robust and fairly fast to compute
        DetectDescribePoint<GrayU8, BrightFeature> detDesc = FactoryDetectDescribe.surfStable(
                new ConfigFastHessian(0, 4, 1000,
                        1, 9, 4, 2),
                null, null, GrayU8.class);

        FastQueue<Point2D_F64> locations01 = new FastQueue<>(Point2D_F64.class, true);
        FastQueue<Point2D_F64> locations02 = new FastQueue<>(Point2D_F64.class, true);
        FastQueue<Point2D_F64> locations03 = new FastQueue<>(Point2D_F64.class, true);

        FastQueue<BrightFeature> features01 = UtilFeature.createQueue(detDesc, 100);
        FastQueue<BrightFeature> features02 = UtilFeature.createQueue(detDesc, 100);
        FastQueue<BrightFeature> features03 = UtilFeature.createQueue(detDesc, 100);

        // Converting data formats for the found features into what can be processed by SFM algorithms
        // Notice how the image center is subtracted from the coordinates? In many cases a principle point
        // of zero is assumed. This is a reasonable assumption in almost all modern cameras. Errors in
        // the principle point tend to materialize as translations and are non fatal.

        int width = image01.width, height = image01.height;
        System.out.println("Image Shape " + width + " x " + height);
        double cx = width / 2;
        double cy = height / 2;

        detDesc.detect(image01);
        for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
            Point2D_F64 pixel = detDesc.getLocation(i);
            locations01.grow().set(pixel.x - cx, pixel.y - cy);
            features01.grow().setTo(detDesc.getDescription(i));
        }
        detDesc.detect(image02);
        for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
            Point2D_F64 pixel = detDesc.getLocation(i);
            locations02.grow().set(pixel.x - cx, pixel.y - cy);
            features02.grow().setTo(detDesc.getDescription(i));
        }
        detDesc.detect(image03);
        for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
            Point2D_F64 pixel = detDesc.getLocation(i);
            locations03.grow().set(pixel.x - cx, pixel.y - cy);
            features03.grow().setTo(detDesc.getDescription(i));
        }

        System.out.println("features01.size = " + features01.size);
        System.out.println("features02.size = " + features02.size);
        System.out.println("features03.size = " + features03.size);

        ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class, true);
        AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 0.1, true);

        AssociateThreeByPairs<BrightFeature> associateThree = new AssociateThreeByPairs<>(associate, BrightFeature.class);

        associateThree.setFeaturesA(features01);
        associateThree.setFeaturesB(features02);
        associateThree.setFeaturesC(features03);

        associateThree.associate();

        System.out.println("Total Matched Triples = " + associateThree.getMatches().size);

        ConfigRansac configRansac = new ConfigRansac();
        configRansac.maxIterations = 50;
        configRansac.inlierThreshold = 1;

        ConfigTrifocal configTri = new ConfigTrifocal();
        ConfigTrifocalError configError = new ConfigTrifocalError();
        configError.model = ConfigTrifocalError.Model.REPROJECTION_REFINE;

        Ransac<TrifocalTensor, AssociatedTriple> ransac =
                FactoryMultiViewRobust.trifocalRansac(configTri, configError, configRansac);

        FastQueue<AssociatedTripleIndex> associatedIdx = associateThree.getMatches();
        FastQueue<AssociatedTriple> associated = new FastQueue<>(AssociatedTriple.class, true);
        for (int i = 0; i < associatedIdx.size; i++) {
            AssociatedTripleIndex p = associatedIdx.get(i);
            associated.grow().set(locations01.get(p.a), locations02.get(p.b), locations03.get(p.c));
        }
        ransac.process(associated.toList());

        List<AssociatedTriple> inliers = ransac.getMatchSet();
        TrifocalTensor model = ransac.getModelParameters();
        System.out.println("Remaining after RANSAC " + inliers.size());

//		// Show remaining associations from RANSAC
//		AssociatedTriplePanel triplePanel = new AssociatedTriplePanel();
//		triplePanel.setPixelOffset(cx,cy);
//		triplePanel.setImages(buff01,buff02,buff03);
//		triplePanel.setAssociation(inliers);
//		ShowImages.showWindow(triplePanel,"Associations", true);

        // estimate using all the inliers
        // No need to re-scale the input because the estimator automatically adjusts the input on its own
        configTri.which = EnumTrifocal.ALGEBRAIC_7;
        configTri.converge.maxIterations = 10;
        Estimate1ofTrifocalTensor trifocalEstimator = FactoryMultiView.trifocal_1(configTri);
        if (!trifocalEstimator.process(inliers, model))
            throw new RuntimeException("Estimator failed");
        model.print();

        DMatrixRMaj P1 = CommonOps_DDRM.identity(3, 4);
        DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
        DMatrixRMaj P3 = new DMatrixRMaj(3, 4);
        MultiViewOps.extractCameraMatrices(model, P2, P3);

        // Most of the time this refinement step makes little difference, but in some edges cases it appears
        // to help convergence
//		System.out.println("Refining projective camera matrices");
//		RefineThreeViewProjective refineP23 = FactoryMultiView.threeViewRefine(null);
//		if( !refineP23.process(inliers,P2,P3,P2,P3) )
//			throw new RuntimeException("Can't refine P2 and P3!");


        SelfCalibrationLinearDualQuadratic selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
        selfcalib.addCameraMatrix(P1);
        selfcalib.addCameraMatrix(P2);
        selfcalib.addCameraMatrix(P3);

        List<CameraPinhole> listPinhole = new ArrayList<>();
        GeometricResult result = selfcalib.solve();
        if (GeometricResult.SOLVE_FAILED != result) {
            for (int i = 0; i < 3; i++) {
                Intrinsic c = selfcalib.getSolutions().get(i);
                CameraPinhole p = new CameraPinhole(c.fx, c.fy, 0, 0, 0, width, height);
                listPinhole.add(p);
            }
        } else {
            System.out.println("Self calibration failed!");
            for (int i = 0; i < 3; i++) {
                CameraPinhole p = new CameraPinhole(width / 2, width / 2, 0, 0, 0, width, height);
                listPinhole.add(p);
            }

        }

        // print the initial guess for focal length. Focal length is a crtical and difficult to estimate
        // parameter
        for (int i = 0; i < 3; i++) {
            CameraPinhole r = listPinhole.get(i);
            System.out.println("fx=" + r.fx + " fy=" + r.fy + " skew=" + r.skew);
        }

        System.out.println("Projective to metric");
        // convert camera matrix from projective to metric
        DMatrixRMaj H = new DMatrixRMaj(4, 4); // storage for rectifying homography
        if (!MultiViewOps.absoluteQuadraticToH(selfcalib.getQ(), H))
            throw new RuntimeException("Projective to metric failed");

        DMatrixRMaj K = new DMatrixRMaj(3, 3);
        List<Se3_F64> worldToView = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            worldToView.add(new Se3_F64());
        }

        // ignore K since we already have that
        MultiViewOps.projectiveToMetric(P1, H, worldToView.get(0), K);
        MultiViewOps.projectiveToMetric(P2, H, worldToView.get(1), K);
        MultiViewOps.projectiveToMetric(P3, H, worldToView.get(2), K);

        // scale is arbitrary. Set max translation to 1
        adjustTranslationScale(worldToView);

        // Construct bundle adjustment data structure
        SceneStructureMetric structure = new SceneStructureMetric(false);
        SceneObservations observations = new SceneObservations(3);

        structure.initialize(3, 3, inliers.size());
        for (int i = 0; i < listPinhole.size(); i++) {
            BundlePinholeSimplified bp = new BundlePinholeSimplified();
            bp.f = listPinhole.get(i).fx;
            structure.setCamera(i, false, bp);
            structure.setView(i, i == 0, worldToView.get(i));
            structure.connectViewToCamera(i, i);
        }
        for (int i = 0; i < inliers.size(); i++) {
            AssociatedTriple t = inliers.get(i);

            observations.getView(0).add(i, (float) t.p1.x, (float) t.p1.y);
            observations.getView(1).add(i, (float) t.p2.x, (float) t.p2.y);
            observations.getView(2).add(i, (float) t.p3.x, (float) t.p3.y);

            structure.connectPointToView(i, 0);
            structure.connectPointToView(i, 1);
            structure.connectPointToView(i, 2);
        }

        // Initial estimate for point 3D locations
        triangulatePoints(structure, observations);

        ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
        configLM.dampeningInitial = 1e-3;
        configLM.hessianScaling = false;
        ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
        configSBA.configOptimizer = configLM;

        // Create and configure the bundle adjustment solver
        BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleAdjustmentMetric(configSBA);
        // prints out useful debugging information that lets you know how well it's converging
//		bundleAdjustment.setVerbose(System.out,0);
        bundleAdjustment.configure(1e-4, 1e-4, 10); // convergence criteria

        bundleAdjustment.setParameters(structure, observations);
        bundleAdjustment.optimize(structure);

        // See if the solution is physically possible. If not fix and run bundle adjustment again
        checkBehindCamera(structure, observations, bundleAdjustment);

        // It's very difficult to find the best solution due to the number of local minimum. In the three view
        // case it's often the problem that a small translation is virtually identical to a small rotation.
        // Convergence can be improved by considering that possibility

        // Now that we have a decent solution, prune the worst outliers to improve the fit quality even more
        PruneStructureFromSceneMetric pruner = new PruneStructureFromSceneMetric(structure, observations);
        pruner.pruneObservationsByErrorRank(0.7);
        pruner.pruneViews(10);
        pruner.prunePoints(1);
        bundleAdjustment.setParameters(structure, observations);
        bundleAdjustment.optimize(structure);

        System.out.println("Final Views");
        for (int i = 0; i < 3; i++) {
            BundlePinholeSimplified cp = structure.getCameras()[i].getModel();
            Vector3D_F64 T = structure.getViews()[i].worldToView.T;
            System.out.printf("[ %d ] f = %5.1f T=%s\n", i, cp.f, T.toString());
        }

        System.out.println("\n\nComputing Stereo Disparity");
        BundlePinholeSimplified cp = structure.getCameras()[0].getModel();
        CameraPinholeRadial intrinsic01 = new CameraPinholeRadial();
        intrinsic01.fsetK(cp.f, cp.f, 0, cx, cy, width, height);
        intrinsic01.fsetRadial(cp.k1, cp.k2);

        cp = structure.getCameras()[1].getModel();
        CameraPinholeRadial intrinsic02 = new CameraPinholeRadial();
        intrinsic02.fsetK(cp.f, cp.f, 0, cx, cy, width, height);
        intrinsic02.fsetRadial(cp.k1, cp.k2);

        Se3_F64 leftToRight = structure.views[1].worldToView;

        // TODO dynamic max disparity
        computeStereoCloud(image01, image02, color01, color02, intrinsic01, intrinsic02, leftToRight, 0, 250);

    }

    public static void main(String[] args) {

        GraphEdit<Surface> g = GraphEdit.window(800, 800);

        ExampleTrifocalStereoUncalibrated ets = new ExampleTrifocalStereoUncalibrated();

        g.add(ets).posRel(0.5f, 0.5f, 0.25f, 0.25f);


//		String name = "rock_leaves_";
//		String name = "mono_wall_";
//        String name = "minecraft_cave1_";
//		String name = "minecraft_distant_";
//		String name = "bobcats_";
//		String name = "chicken_";
//		String name = "turkey_";
//		String name = "rockview_";
//		String name = "pebbles_";
//		String name = "books_";
		String name = "skull_";
//		String name = "triflowers_";

        for (int i = 1; i <= 3; i++) {
            g.add(new ImageChip(UtilImageIO.loadImage(UtilIO.pathExample("/home/me/boofcv/data/example/triple/" + name + "0" + i + ".jpg")))).
                    posRel(0.5f, 0.5f, 0.25f, 0.15f);
        }

//        ets.update(
//                UtilImageIO.loadImage(UtilIO.pathExample("/home/me/boofcv/data/example/triple/" + name + "01.jpg")),
//                UtilImageIO.loadImage(UtilIO.pathExample("/home/me/boofcv/data/example/triple/" + name + "02.jpg")),
//                UtilImageIO.loadImage(UtilIO.pathExample("/home/me/boofcv/data/example/triple/" + name + "03.jpg")));

    }

    private static void adjustTranslationScale(List<Se3_F64> worldToView) {
        double maxT = 0;
        for (Se3_F64 p : worldToView) {
            maxT = Math.max(maxT, p.T.norm());
        }
        for (Se3_F64 p : worldToView) {
            p.T.scale(1.0 / maxT);
            p.print();
        }
    }

    // TODO Do this correction without running bundle adjustment again
    private static void checkBehindCamera(SceneStructureMetric structure, SceneObservations observations, BundleAdjustment<SceneStructureMetric> bundleAdjustment) {

        int totalBehind = 0;
        Point3D_F64 X = new Point3D_F64();
        for (int i = 0; i < structure.points.length; i++) {
            structure.points[i].get(X);
            if (X.z < 0)
                totalBehind++;
        }
        structure.views[1].worldToView.T.print();
        if (totalBehind > structure.points.length / 2) {
            System.out.println("Flipping because it's reversed. score = " + bundleAdjustment.getFitScore());
            for (int i = 1; i < structure.views.length; i++) {
                Se3_F64 w2v = structure.views[i].worldToView;
                w2v.set(w2v.invert(null));
            }
            triangulatePoints(structure, observations);

            bundleAdjustment.setParameters(structure, observations);
            bundleAdjustment.optimize(structure);
            System.out.println("  after = " + bundleAdjustment.getFitScore());
        } else {
            System.out.println("Points not behind camera. " + totalBehind + " / " + structure.points.length);
        }
    }

    void computeStereoCloud(GrayU8 distortedLeft, GrayU8 distortedRight,
                            Planar<GrayU8> colorLeft, Planar<GrayU8> colorRight,
                            CameraPinholeRadial intrinsicLeft,
                            CameraPinholeRadial intrinsicRight,
                            Se3_F64 leftToRight,
                            int minDisparity, int maxDisparity) {

//		drawInliers(origLeft, origRight, intrinsic, inliers);
        int width = distortedLeft.width;
        int height = distortedRight.height;

        // Rectify and remove lens distortion for stereo processing
        DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
        DMatrixRMaj rectifiedR = new DMatrixRMaj(3, 3);

        // rectify a colored image
        Planar<GrayU8> rectColorLeft = colorLeft.createSameShape();
        Planar<GrayU8> rectColorRight = colorLeft.createSameShape();
        GrayU8 rectMask = new GrayU8(colorLeft.width, colorLeft.height);

        rectifyImages(colorLeft, colorRight, leftToRight, intrinsicLeft, intrinsicRight,
                rectColorLeft, rectColorRight, rectMask, rectifiedK, rectifiedR);

        if (rectifiedK.get(0, 0) < 0)
            throw new RuntimeException("Egads");

//		System.out.println("Rectified K");
//		rectifiedK.print();

//		System.out.println("Rectified R");
//		rectifiedR.print();

        GrayU8 rectifiedLeft = distortedLeft.createSameShape();
        GrayU8 rectifiedRight = distortedRight.createSameShape();
        ConvertImage.average(rectColorLeft, rectifiedLeft);
        ConvertImage.average(rectColorRight, rectifiedRight);

        // compute disparity
        StereoDisparity<GrayS16, GrayF32> disparityAlg =
                FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
                        minDisparity, maxDisparity, 6, 6,
                        30, 3, 0.05, GrayS16.class);

        // Apply the Laplacian across the image to addAt extra resistance to changes in lighting or camera gain
        GrayS16 derivLeft = new GrayS16(width, height);
        GrayS16 derivRight = new GrayS16(width, height);
        LaplacianEdge.process(rectifiedLeft, derivLeft);
        LaplacianEdge.process(rectifiedRight, derivRight);

        // process and return the results
        disparityAlg.process(derivLeft, derivRight);
        GrayF32 disparity = disparityAlg.getDisparity();
        RectifyImageOps.applyMask(disparity, rectMask, 0);

        // show results
//		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, minDisparity, maxDisparity, 0);

        BufferedImage outLeft = ConvertBufferedImage.convertTo(rectColorLeft, new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), true);
//		BufferedImage outRight = ConvertBufferedImage.convertTo(rectColorRight, new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB),true);

//		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification",true);
//		ShowImages.showWindow(visualized, "Disparity",true);

        showPointCloud(disparity, outLeft, leftToRight, rectifiedK, rectifiedR, minDisparity, maxDisparity);
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
    public static <T extends ImageBase<T>>
    void rectifyImages(T distortedLeft,
                       T distortedRight,
                       Se3_F64 leftToRight,
                       CameraPinholeRadial intrinsicLeft,
                       CameraPinholeRadial intrinsicRight,
                       T rectifiedLeft,
                       T rectifiedRight,
                       GrayU8 rectifiedMask,
                       DMatrixRMaj rectifiedK,
                       DMatrixRMaj rectifiedR) {
        RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

        // original camera calibration matrices
        DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(intrinsicLeft, (DMatrixRMaj) null);
        DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(intrinsicRight, (DMatrixRMaj) null);

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
        FMatrixRMaj rect1_F32 = new FMatrixRMaj(3, 3);
        FMatrixRMaj rect2_F32 = new FMatrixRMaj(3, 3);
        ConvertMatrixData.convert(rect1, rect1_F32);
        ConvertMatrixData.convert(rect2, rect2_F32);

        // Extending the image prevents a harsh edge reducing false matches at the image border
        // SKIP is another option, possibly a tinny bit faster, but has a harsh edge which will need to be filtered
        ImageDistort<T, T> distortLeft =
                RectifyImageOps.rectifyImage(intrinsicLeft, rect1_F32, BorderType.EXTENDED, distortedLeft.getImageType());
        ImageDistort<T, T> distortRight =
                RectifyImageOps.rectifyImage(intrinsicRight, rect2_F32, BorderType.EXTENDED, distortedRight.getImageType());

        distortLeft.apply(distortedLeft, rectifiedLeft, rectifiedMask);
        distortRight.apply(distortedRight, rectifiedRight);
    }

//	/**
//	 * Draw inliers for debugging purposes.  Need to convert from normalized to pixel coordinates.
//	 */
//	public static void drawInliers(BufferedImage left, BufferedImage right, CameraPinholeRadial intrinsic,
//								   List<AssociatedPair> normalized) {
//		Point2Transform2_F64 n_to_p = LensDistortionFactory.narrow(intrinsic).distort_F64(false,true);
//
//		List<AssociatedPair> pixels = new ArrayList<>();
//
//		for (AssociatedPair n : normalized) {
//			AssociatedPair p = new AssociatedPair();
//
//			n_to_p.compute(n.p1.x, n.p1.y, p.p1);
//			n_to_p.compute(n.p2.x, n.p2.y, p.p2);
//
//			pixels.addAt(p);
//		}
//
////		// display the results
////		AssociationPanel panel = new AssociationPanel(20);
////		panel.setAssociation(pixels);
////		panel.setImages(left, right);
////
////		ShowImages.showWindow(panel, "Inlier Features", true);
//	}

    final PointCloudViewer pcv = //VisualizeData.createPointCloudViewer();
            new PointCloudViewerSwing();

    {

        JFrame j = new JFrame();
        j.getContentPane().add(pcv.getComponent());
        j.setSize(800, 800);
        j.setVisible(true);
        //showWindow(pcv.getComponent(), "Point Cloud", true);
    }

    /**
     * Show results as a point cloud
     */
    public void showPointCloud(ImageGray disparity, BufferedImage left,
                               Se3_F64 motion, DMatrixRMaj rectifiedK, DMatrixRMaj rectifiedR,
                               int minDisparity, int maxDisparity) {
        DisparityToColorPointCloud d2c = new DisparityToColorPointCloud();
        double baseline = motion.getT().norm();
        d2c.configure(baseline, rectifiedK, rectifiedR, new DoNothing2Transform2_F64(), minDisparity, maxDisparity);
        d2c.process(disparity, left);

        CameraPinhole rectifiedPinhole = PerspectiveOps.matrixToPinhole(rectifiedK, disparity.width, disparity.height, null);

        // skew the view to make the structure easier to see
        Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.eulerXyz(-baseline * 5, 0, 0, 0, 0.2, 0, null);


        pcv.setCameraHFov(PerspectiveOps.computeHFov(rectifiedPinhole));
        pcv.setCameraToWorld(cameraToWorld);
        pcv.setTranslationStep(baseline / 3);
        pcv.setDotSize(1);
        pcv.setTranslationStep(baseline / 10);

        pcv.clearPoints();
        pcv.addCloud(d2c.getCloud(), d2c.getCloudColor());


    }

    /**
     * <p>
     * Renders a 3D point cloud using a perspective pin hole camera model.
     * </p>
     *
     * <p>
     * Rendering speed is improved by first rendering onto a grid and only accepting the highest
     * (closest to viewing camera) point as being visible.
     * </p>
     *
     * @author Peter Abeles
     */
    static class DisparityToColorPointCloud {
        // distance between the two camera centers
        float baseline;

        // intrinsic camera parameters
        DMatrixRMaj K;
        float focalLengthX;
        float focalLengthY;
        float centerX;
        float centerY;
        FMatrixRMaj rectifiedR = new FMatrixRMaj(3, 3);

        // minimum disparity
        int minDisparity;
        // maximum minus minimum disparity
        int rangeDisparity;

        // How far out it should zoom.
        double range = 1;

        // Storage for point cloud
        GrowQueue_F32 cloudXyz = new GrowQueue_F32();
        GrowQueue_I32 cloudRgb = new GrowQueue_I32();

        // tilt angle in degrees
        public int tiltAngle = 0;
        public double radius = 5;

        // converts from rectified pixels into color image pixels
        Point2Transform2_F64 rectifiedToColor;
        // storage for color image coordinate
        Point2D_F64 colorPt = new Point2D_F64();

        Point3D_F32 p = new Point3D_F32();

        /**
         * Stereo and intrinsic camera parameters
         *
         * @param baseline         Stereo baseline (world units)
         * @param K                Intrinsic camera calibration matrix of rectified camera
         * @param rectifiedToColor Transform from rectified pixels to the color image pixels.
         * @param minDisparity     Minimum disparity that's computed (pixels)
         * @param maxDisparity     Maximum disparity that's computed (pixels)
         */
        public void configure(double baseline,
                              DMatrixRMaj K, DMatrixRMaj rectifiedR,
                              Point2Transform2_F64 rectifiedToColor,
                              int minDisparity, int maxDisparity) {
            this.K = K;
            ConvertMatrixData.convert(rectifiedR, this.rectifiedR);
            this.rectifiedToColor = rectifiedToColor;
            this.baseline = (float) baseline;
            this.focalLengthX = (float) K.get(0, 0);
            this.focalLengthY = (float) K.get(1, 1);
            this.centerX = (float) K.get(0, 2);
            this.centerY = (float) K.get(1, 2);
            this.minDisparity = minDisparity;

            this.rangeDisparity = maxDisparity - minDisparity;
        }

        /**
         * Given the disparity image compute the 3D location of valid points and save pixel colors
         * at that point
         *
         * @param disparity Disparity image
         * @param color     Color image of left camera
         */
        public void process(ImageGray disparity, BufferedImage color) {
            cloudRgb.setMaxSize(disparity.width * disparity.height);
            cloudXyz.setMaxSize(disparity.width * disparity.height * 3);
            cloudRgb.reset();
            cloudXyz.reset();

            if (disparity instanceof GrayU8)
                process((GrayU8) disparity, color);
            else
                process((GrayF32) disparity, color);
        }

        private void process(GrayU8 disparity, BufferedImage color) {

            for (int pixelY = 0; pixelY < disparity.height; pixelY++) {
                int index = disparity.startIndex + disparity.stride * pixelY;

                for (int pixelX = 0; pixelX < disparity.width; pixelX++) {
                    int value = disparity.data[index++] & 0xFF;

                    if (value >= rangeDisparity)
                        continue;

                    value += minDisparity;

                    // The point lies at infinity.
                    if (value == 0)
                        continue;

                    // Note that this will be in the rectified left camera's reference frame.
                    // An additional rotation is needed to put it into the original left camera frame.
                    p.z = baseline * focalLengthX / value;
                    p.x = p.z * (pixelX - centerX) / focalLengthX;
                    p.y = p.z * (pixelY - centerY) / focalLengthY;

                    // Bring it back into left camera frame
                    GeometryMath_F32.multTran(rectifiedR, p, p);

                    cloudRgb.add(getColor(disparity, color, pixelX, pixelY));
                    cloudXyz.add(p.x);
                    cloudXyz.add(p.y);
                    cloudXyz.add(p.z);
                }
            }
        }

        private void process(GrayF32 disparity, BufferedImage color) {

            for (int pixelY = 0; pixelY < disparity.height; pixelY++) {
                int index = disparity.startIndex + disparity.stride * pixelY;

                for (int pixelX = 0; pixelX < disparity.width; pixelX++) {
                    float value = disparity.data[index++];

                    // invalid disparity
                    if (value >= rangeDisparity)
                        continue;

                    value += minDisparity;

                    // The point lies at infinity.
                    if (value == 0)
                        continue;

                    p.z = baseline * focalLengthX / value;
                    p.x = p.z * (pixelX - centerX) / focalLengthX;
                    p.y = p.z * (pixelY - centerY) / focalLengthY;

                    // Bring it back into left camera frame
                    GeometryMath_F32.multTran(rectifiedR, p, p);

                    cloudRgb.add(getColor(disparity, color, pixelX, pixelY));
                    cloudXyz.add(p.x);
                    cloudXyz.add(p.y);
                    cloudXyz.add(p.z);
                }
            }
        }

        private int getColor(ImageBase disparity, BufferedImage color, int x, int y) {
            rectifiedToColor.compute(x, y, colorPt);
            if (BoofMiscOps.checkInside(disparity, colorPt.x, colorPt.y)) {
                return color.getRGB((int) colorPt.x, (int) colorPt.y);
            } else {
                return 0x000000;
            }
        }

        public GrowQueue_F32 getCloud() {
            return cloudXyz;
        }

        public GrowQueue_I32 getCloudColor() {
            return cloudRgb;
        }

    }

}
