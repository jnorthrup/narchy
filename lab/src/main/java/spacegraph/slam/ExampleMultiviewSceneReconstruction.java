/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package spacegraph.slam;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.geo.triangulate.WrapGeometricTriangulation;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.*;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.GrayF32;
import com.jogamp.opengl.GL2;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import jcog.data.bit.MetalBitSet;
import jcog.list.FasterList;
import jcog.signal.Bitmap2D;
import org.HdrHistogram.DoubleHistogram;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.video.Draw;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Demonstration on how to do 3D reconstruction from a set of unordered photos with known intrinsic camera calibration.
 * The code below is still a work in process and is very basic, but still require a solid understanding of
 * structure from motion to understand.  In other words, this is not for beginners and requires good clean set of
 * images to work.
 * <p>
 * One key element it is missing is bundle adjustment to improve the estimated camera location and 3D points.  The
 * current bundle adjustment in BoofCV is too inefficient.   Better noise removal and numerous other improvements
 * are needed before it can compete with commercial equivalents.
 *
 * @author Peter Abeles
 */
public class ExampleMultiviewSceneReconstruction {
    // Detects and describes image interest points
    final DetectDescribePoint<GrayF32, BrightFeature> detDesc =
    //FactoryDetectDescribe.surfFast(null, null, null, GrayF32.class);
        FactoryDetectDescribe.surfStable(null, null, null, GrayF32.class);
    // Converts a point from pixel to normalized image coordinates
    //final DetectDescribePoint<Planar<GrayU8>, BrightFeature> detDesc =
     //       FactoryDetectDescribe.surfColorStable(null, null, null, ImageType.pl(3, GrayU8.class));
    /**
     * SLAM Course - 20 - SLAM
     * Frontends https://youtu.be/Ejw1HBj3Apg?list=PLgnQpQtFTOGQrZ4O5QzbIHgl3b1JHimN_&t=1715
     */
    final int SURF_DIMENSIONS = detDesc.createDescription().size();
    final ThreadLocal<AssociateDescription<TupleDesc_F64>> associate = ThreadLocal.withInitial(()-> {
        //return FactoryAssociation.greedy(
        //FactoryAssociation.scoreEuclidean(BrightFeature.class, true);
        //FactoryAssociation.scoreHamming(BrightFeature.class);
        // , 1, true);

        return FactoryAssociation.kdtree(SURF_DIMENSIONS, SURF_DIMENSIONS * 8);
    });

    final List<Feature3D> featuresAll = new FasterList<>(16 * 1024);
    private final int minMotionFeatures = 10;


    Point2Transform2_F64 pixelToNorm;
    // ratio of matching features to unmatched features for two images to be considered connected
    // TODO dynamically compute this by minimum necessary to maintain a connected graph of all frames, w/ possible centrality heuristic
    // use the frame connection weight to weight the resulting 3d point computation
    double connectPercentile = 50;

    // score ans association algorithm
    double connectThreshold = Double.NaN /* to be computed */;

    // tolerance for inliers in pixels (* planes?)
    double inlierTol = 1f;
    //new WrapPixelDepthLinear()
    //new WrapTwoViewsTriangulateDLT();
    // Triangulates the 3D coordinate of a point from two observations
    TriangulateTwoViewsCalibrated triangulate = //FactoryMultiView.triangulateTwoGeometric();
            new WrapGeometricTriangulation();
    // List of visual features (e.g. SURF) descriptions in each image
    List<FastQueue<BrightFeature>> imageVisualFeatures = new ArrayList<>();
    // List of visual feature locations as normalized image coordinates in each image
    List<FastQueue<Point2D_F64>> imagePixels = new ArrayList<>();
    // Color of the pixel at each feature location
    List<GrowQueue_I32> imageColors = new ArrayList<>();
    // List of 3D features in each image
    List<Map<Long, Feature3D>> imageFeature3D = new ArrayList<>();
    // Transform from world to each camera image
    Se3_F64 motionWorldToCamera[] = new Se3_F64[0];
    // indicates if an image has had its motion estimated yet
    boolean estimatedImage[];
    // List of all 3D features
    // if true the image has been processed.  Estimation could have failed. so this can be true but estimated false
    boolean processedImage[];
    // used to provide initial estimate of the 3D scene
    ModelMatcher<Se3_F64, AssociatedPair> posePnP;
    ModelMatcher<Se3_F64, Point2D3D> motionPnP;
    ModelFitter<Se3_F64, Point2D3D> refinePnP;

    public ExampleMultiviewSceneReconstruction() {
        super();

        new SpaceGraphPhys3D(new PointCloudSpace()).show(800, 800);

    }

    public static void main(String[] args) {

        String directory = UtilIO.pathExample(
                //"sfm/chair"
                "/home/me/BoofCV/data/example/sfm/chair"
                //"/home/me/BoofCV/data/example/calibration/stereo/Bumblebee2_Chess"
        );

        CameraPinholeRadial intrinsic = ExampleStereoTwoViewsOneCamera.intrinsic;
        //CalibrationIO.load(
        //new File(directory, "/intrinsic_DSC-HX5_3648x2736_to_640x480.yaml"));

        List<BufferedImage> images = UtilImageIO.loadImages(directory, ".*jpg");
        //images = images.subList(0, 8);

        ExampleMultiviewSceneReconstruction example = new ExampleMultiviewSceneReconstruction();


        long before = System.currentTimeMillis();
        example.process(intrinsic, images);
        long after = System.currentTimeMillis();

        System.out.println("Elapsed time " + (after - before) / 1000.0 + " (s)");
    }

    /**
     * Process the images and reconstructor the scene as a point cloud using matching interest points between
     * images.
     */
    public void process(CameraPinholeRadial intrinsic, List<BufferedImage> colorImages) {

        pixelToNorm =
                LensDistortionOps.narrow(intrinsic).undistort_F64(true, false);
                //new DoNothing2Transform2_F64();

        posePnP = FactoryMultiViewRobust.essentialRansac(
                new ConfigEssential(intrinsic), new ConfigRansac(2000, inlierTol));

        motionPnP = FactoryMultiViewRobust.pnpRansac(
                new ConfigPnP(intrinsic), new ConfigRansac(2000, inlierTol));

        refinePnP = FactoryMultiView.refinePnP(1e-12, 20000);

        // find features in each image
        detectImageFeatures(colorImages);

        // see which images are the most similar to each o ther
        double[][] matrix = computeConnections();

        printConnectionMatrix(matrix);

        // find the image which is connected to the most other images.  Use that as the origin of the arbitrary
        // coordinate system
        int bestImage = selectMostConnectFrame(colorImages, matrix);

        // Use two images to initialize the scene reconstruction
        initializeReconstruction(colorImages, matrix, bestImage);

        // Process rest of the images and compute 3D coordinates
        List<Integer> seed = new ArrayList<>();
        seed.add(bestImage);
        performReconstruction(seed, -1, matrix);

        // Bundle adjustment would normally be done at this point, but has been omitted since the current
        // implementation is too slow for a large number of points

        // display a point cloud from the 3D features
        System.out.println(featuresAll.size() + " features");

        //gui.setPreferredSize(new Dimension(500, 500));
        //ShowImages.showWindow(gui, "Points");
    }

    /**
     * Initialize the reconstruction by finding the image which is most similar to the "best" image.  Estimate
     * its pose up to a scale factor and create the initial set of 3D features
     */
    private void initializeReconstruction(List<BufferedImage> colorImages, double[][] matrix, int bestImage) {
        // Set all images, but the best one, as not having been estimated yet
        estimatedImage = new boolean[colorImages.size()];
        processedImage = new boolean[colorImages.size()];
        estimatedImage[bestImage] = true;
        processedImage[bestImage] = true;

        // declare stored for found motion of each image
        motionWorldToCamera = new Se3_F64[colorImages.size()];
        for (int i = 0; i < colorImages.size(); i++) {
            motionWorldToCamera[i] = new Se3_F64();
            imageFeature3D.add(new HashMap<>());
        }

        // pick the image most similar to the original image to initialize pose estimation
        int firstChild = findBestFit(matrix, bestImage);
        initialize((short) bestImage, (short) firstChild, matrix[bestImage][firstChild]);
    }

    /**
     * Select the frame which has the most connections to all other frames.  The is probably a good location
     * to start since it will require fewer hops to estimate the motion of other frames
     */
    private int selectMostConnectFrame(List<BufferedImage> colorImages, double[][] matrix) {
        int bestImage = -1;
        int bestCount = 0;
        for (int i = 0; i < colorImages.size(); i++) {
            int count = 0;
            for (int j = 0; j < colorImages.size(); j++) {
                if (matrix[i][j] > connectThreshold) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                bestImage = i;
            }
        }
        return bestImage;
    }

    /**
     * Detect image features in all the images.  Save location, description, and color
     */

    private void detectImageFeatures(List<BufferedImage> colorImages) {
        System.out.println("Detecting Features in each image.  Total " + colorImages.size());
        for (int i = 0; i < colorImages.size(); i++) {
            System.out.print("*");
            BufferedImage colorImage = colorImages.get(i);

            FastQueue<BrightFeature> features = new SurfFeatureQueue(SURF_DIMENSIONS);
            FastQueue<Point2D_F64> pixels = new FastQueue<>(Point2D_F64.class, true);
            GrowQueue_I32 colors = new GrowQueue_I32();
            detectFeatures(colorImage, features, pixels, colors);

            imageVisualFeatures.add(features);
            imagePixels.add(pixels);
            imageColors.add(colors);
        }
        System.out.println();
    }

    /**
     * Compute connectivity matrix based on fraction of matching image features
     */
    private double[][] computeConnections() {

        int features = imageVisualFeatures.size();

        double matrix[][] = new double[features][features];

        DoubleHistogram weights = new DoubleHistogram(5);

        IntStream.range(0, features).mapToObj(
                i -> IntStream.range(i + 1, features).mapToObj(j -> pair(i, j)))
                .flatMap(x -> x).parallel().forEach((IJ) -> {
//        for (int i = 0; i < features; i++) {
//            for (int j = i + 1; j < features; j++) {

            int i = IJ.getOne();
            int j = IJ.getTwo();
            FastQueue<BrightFeature> ii = imageVisualFeatures.get(i);
            FastQueue<BrightFeature> jj = imageVisualFeatures.get(j);

            System.out.println("Associating " + i + "(x" + ii.size() + ") x " + j + "(x" + jj.size() + ")");

            AssociateDescription a = associate.get();
            a.setSource(ii);
            a.setDestination(jj);
            a.associate();

            int matches = a.getMatches().size();
            double ij = matches / (double) ii.size();
            matrix[i][j] = ij;
            double ji = matches / (double) jj.size();
            matrix[j][i] = ji;

            //System.out.println(" = " + matrix[i][j]);

        });

        for (int i = 0; i < features; i++) {
            for (int j = 0; j < features; j++) {
                if (i != j)
                    weights.recordValue(matrix[i][j]);
            }
        }

        connectThreshold = weights.getValueAtPercentile(100 - connectPercentile);

        return matrix;
    }

    /**
     * Prints out which frames are connected to each other
     */
    private void printConnectionMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                if (matrix[i][j] >= connectThreshold)
                    System.out.print("#");
                else
                    System.out.print(".");
            }
            System.out.println();
        }
    }

    /**
     * Detects image features.  Saves their location, description, and pixel color
     */
    private void detectFeatures(BufferedImage colorImage,
                                FastQueue<BrightFeature> features, FastQueue<Point2D_F64> pixels,
                                GrowQueue_I32 colors) {

        GrayF32 image = ConvertBufferedImage.convertFrom(colorImage, (GrayF32) null);
        //Planar<GrayU8> image = ConvertBufferedImage.convertFromPlanar(colorImage, null, true, GrayU8.class);

        features.reset();
        pixels.reset();
        colors.reset();
        detDesc.detect(image);
        int numFeatures = detDesc.getNumberOfFeatures();
        features.growArray(numFeatures);

        for (int i = 0; i < numFeatures; i++) {
            Point2D_F64 p = detDesc.getLocation(i);

            features.grow().set(detDesc.getDescription(i));
            // store pixels are normalized image coordinates
            pixelToNorm.compute(p.x, p.y, pixels.grow());

            colors.add(colorImage.getRGB((int) p.x, (int) p.y));
        }
    }

    /**
     * Finds the frame which is the best match for the given target frame
     */
    private int findBestFit(double matrix[][], int target) {

        // find the image which is the closest fit
        int bestIndex = -1;
        double bestRatio = 0;

        for (int i = 0; i < estimatedImage.length; i++) {
            double ratio = matrix[target][i];
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    /**
     * Initialize the 3D world given these two images.  imageA is assumed to be the origin of the world.
     */
    private void initialize(short imageA, short imageB, double weight) {
        System.out.println("Initializing 3D world using " + imageA + " and " + imageB);

        // Compute the 3D pose and find valid image features
        Se3_F64 motionAtoB = new Se3_F64();
        List<AssociatedIndex> inliers = new ArrayList<>();

        if (!estimateStereoPose(imageA, imageB, motionAtoB, inliers))
            throw new RuntimeException("The first image pair is a bad keyframe!");

        motionWorldToCamera[imageB].set(motionAtoB);
        estimatedImage[imageB] = true;
        processedImage[imageB] = true;

        // create tracks for only those features in the inlier list
        FastQueue<Point2D_F64> pixelsA = imagePixels.get(imageA);
        FastQueue<Point2D_F64> pixelsB = imagePixels.get(imageB);
        Map<Long, Feature3D> tracksA = imageFeature3D.get(imageA);
        Map<Long, Feature3D> tracksB = imageFeature3D.get(imageB);

        GrowQueue_I32 colorsA = imageColors.get(imageA);

        for (int i = 0; i < inliers.size(); i++) {
            AssociatedIndex a = inliers.get(i);

            Feature3D t = new Feature3D();
            t.color = colorsA.get(a.src);
            // compute the 3D coordinate of the feature
            Point2D_F64 pa = pixelsA.get(a.src);
            Point2D_F64 pb = pixelsB.get(a.dst);

            if (!triangulate.triangulate(pa, pb, motionAtoB, t))
                continue;
            // the feature has to be in front of the camera
            if (t.z > 0) {
                featuresAll.add(t);
                tracksA.put(Feature3D.key(imageA, pa), t);
                tracksB.put(Feature3D.key(imageB, pb), t);
            }
        }

        // adjust the scale so that it's not excessively large or small
        normalizeScale(motionWorldToCamera[imageB], tracksA);
    }

    /**
     * Perform a breadth first search through connection graph until the motion to all images has been found
     */
    private void performReconstruction(List<Integer> parents, int childAdd, double matrix[][]) {

        System.out.println("--------- Total Parents " + parents.size());

        List<Integer> children = new ArrayList<>();

        if (childAdd != -1) {
            children.add(childAdd);
        }

        for (int parent : parents) {
            for (int i = 0; i < estimatedImage.length; i++) {
                // see if it is connected to the target and has not had its motion estimated
                double weight = matrix[parent][i];
                if (weight >= connectThreshold && !processedImage[i]) {
                    estimateMotionPnP((short) parent, (short) i, weight);
                    children.add(i);
                }
            }
        }

        if (!children.isEmpty())
            performReconstruction(children, -1, matrix);
    }

    /**
     * Estimate the motion between two images.  Image A is assumed to have known features with 3D coordinates already
     * and image B is an unprocessed image with no 3D features yet.
     */
    private void estimateMotionPnP(short imageA, short imageB, double weight) {
        // Mark image B as processed so that it isn't processed a second time.
        processedImage[imageB] = true;

        System.out.println("Estimating PnP motion between " + imageA + " and " + imageB);

        // initially prune features using essential matrix
        Se3_F64 dummy = new Se3_F64();
        List<AssociatedIndex> inliers = new ArrayList<>();

        if (!estimateStereoPose(imageA, imageB, dummy, inliers))
            throw new RuntimeException("The first image pair is a bad keyframe!");

        FastQueue<Point2D_F64> pixelsA = imagePixels.get(imageA);
        FastQueue<Point2D_F64> pixelsB = imagePixels.get(imageB);
        Map<Long, Feature3D> featuresA = imageFeature3D.get(imageA);
        Map<Long, Feature3D> featuresB = imageFeature3D.get(imageB); // this should be empty

        // create the associated pair for motion estimation
        List<Point2D3D> features = new ArrayList<>();
        List<AssociatedIndex> inputRansac = new ArrayList<>();
        List<AssociatedIndex> unmatched = new ArrayList<>();
        for (int i = 0; i < inliers.size(); i++) {
            AssociatedIndex ii = inliers.get(i);
            Feature3D t = featuresA.get(Feature3D.key(imageA, pixelsA.get(ii.src)));
            if (t != null) {
                Point2D_F64 p = pixelsB.get(ii.dst);
                features.add(new Point2D3D(p, t));
                inputRansac.add(ii);
            } else {
                unmatched.add(ii);
            }
        }

        // make sure there are enough features to estimate motion
        if (features.size() < minMotionFeatures) {
            System.out.println("  Too few features for PnP!!  " + features.size());
            return;
        }

        // estimate the motion between the two images
        if (!motionPnP.process(features))
            throw new RuntimeException("Motion estimation failed");

        // refine the motion estimate using non-linear optimization
        Se3_F64 motionWorldToB = new Se3_F64();
        if (!refinePnP.fitModel(motionPnP.getMatchSet(), motionPnP.getModelParameters(), motionWorldToB))
            throw new RuntimeException("Refine failed!?!?");

        motionWorldToCamera[imageB].set(motionWorldToB);
        estimatedImage[imageB] = true;

        // Add all tracks in the inlier list to the B's list of 3D features
        int N = motionPnP.getMatchSet().size();

        MetalBitSet inlierPnP = MetalBitSet.bits(features.size());
        for (int i = 0; i < N; i++) {
            int index = motionPnP.getInputIndex(i);
            AssociatedIndex ii = inputRansac.get(index);

            // find the track that this was associated with and add it to B
            Feature3D t = featuresA.get(Feature3D.key(imageA, pixelsA.get(ii.src)));
            if (t != null) {
                featuresB.put(Feature3D.key(imageB, pixelsB.get(ii.dst)), t);
                inlierPnP.set(index);
            }
        }

        // Create new tracks for all features which were a member of essential matrix but not used to estimate
        // the motion using PnP.
        Se3_F64 motionBtoWorld = motionWorldToB.invert(null);
        Se3_F64 motionWorldToA = motionWorldToCamera[imageA];
        Se3_F64 motionBtoA = motionBtoWorld.concat(motionWorldToA, null);
        Point3D_F64 pt_in_b = new Point3D_F64();

        int totalAdded = 0;
        GrowQueue_I32 colorsA = imageColors.get(imageA);
        for (AssociatedIndex ii : unmatched) {

            if (!triangulate.triangulate(pixelsB.get(ii.dst), pixelsA.get(ii.src), motionBtoA, pt_in_b))
                continue;

            // the feature has to be in front of the camera
            if (pt_in_b.z > 0) {
                Feature3D t = new Feature3D();

                // transform from B back to world frame
                SePointOps_F64.transform(motionBtoWorld, pt_in_b, t);

                t.color = colorsA.get(ii.src);

                featuresAll.add(t);
                featuresA.put(Feature3D.key(imageA, pixelsA.get(ii.src)), t);
                featuresB.put(Feature3D.key(imageB, pixelsB.get(ii.dst)), t);

                totalAdded++;
            }
        }

        // create new tracks for existing tracks which were not in the inlier set.  Maybe things will work
        // out better if the 3D coordinate is re-triangulated as a new feature
        for (int i = 0; i < features.size(); i++) {
            if (inlierPnP.get(i))
                continue;

            AssociatedIndex ii = inputRansac.get(i);

            if (!triangulate.triangulate(pixelsB.get(ii.dst), pixelsA.get(ii.src), motionBtoA, pt_in_b))
                continue;

            // the feature has to be in front of the camera
            if (pt_in_b.z > 0) {
                Feature3D t = new Feature3D();

                // transform from B back to world frame
                SePointOps_F64.transform(motionBtoWorld, pt_in_b, t);

                // only add this feature to image B since a similar one already exists in A.
                t.color = colorsA.get(ii.src);

                featuresAll.add(t);
                featuresB.put(Feature3D.key(imageB, pixelsB.get(ii.dst)), t);

                totalAdded++;
            }
        }

        System.out.println("  New added " + totalAdded + "  tracksA.size = " + featuresA.size() + "  tracksB.size = " + featuresB.size());
    }

    /**
     * Given two images compute the relative location of each image using the essential matrix.
     */
    protected boolean estimateStereoPose(int imageA, int imageB,
                                         Se3_F64 motionAtoB,
                                         List<AssociatedIndex> inliers) {
        // associate the features together
        AssociateDescription a = associate.get();
        a.setSource(imageVisualFeatures.get(imageA));
        a.setDestination(imageVisualFeatures.get(imageB));
        a.associate();

        FastQueue<AssociatedIndex> matches = a.getMatches();

        // create the associated pair for motion estimation
        FastQueue<Point2D_F64> pixelsA = imagePixels.get(imageA);
        FastQueue<Point2D_F64> pixelsB = imagePixels.get(imageB);
        List<AssociatedPair> pairs = queueToList(matches, (ii)->
                new AssociatedPair(pixelsA.get(ii.src), pixelsB.get(ii.dst)));

        if (!posePnP.process(pairs))
            throw new RuntimeException("Motion estimation failed");

        List<AssociatedPair> inliersEssential = posePnP.getMatchSet();

        motionAtoB.set(posePnP.getModelParameters());

        for (int i = 0; i < inliersEssential.size(); i++) {
            inliers.add(matches.get(posePnP.getInputIndex(i)));
        }

        return true;
    }


    public static <X,Y> List<Y> queueToList(FastQueue<X> q, Function<X,Y> f) {
        int mm = q.size();
        List<Y> l = new FasterList<>(mm);
        for (int i = 0; i < mm; i++) {
            X x = q.get(i);
            l.add(f.apply(x));
        }
        return l;
    }

//	/**
//	 * Given a list of 3D features, find the feature which was observed at the specified frame at the
//	 * specified location.  If no feature is found return null.
//	 */
//	private Feature3D lookupFeature(Map<Long,Feature3D> features, short  frameIndex, Point2D_F64 pixel) {
//		short px = Feature3D.keyCoordinate(pixel.x);
//		short py = Feature3D.keyCoordinate(pixel.y);
//		for (int i = 0; i < features.size(); i++) {
//			Feature3D t = features.get(i);
//			if (t.contains(frameIndex, px, py))
//				return t;
//		}
//		return null;
//	}

    /**
     * Scale can only be estimated up to a scale factor.  Might as well set the distance to 1 since it is
     * less likely to have overflow/underflow issues.  This step is not strictly necessary.
     */
    public void normalizeScale(Se3_F64 transform, Map<Long, Feature3D> features) {

        double T = transform.T.norm();
        double scale = 1.0 / T;

        for (Se3_F64 m : motionWorldToCamera) {
            m.T.timesIP(scale);
        }

        for (Feature3D t : features.values()) {
            t.timesIP(scale);
        }
    }

    public static class Feature3D extends Point3D_F64 {

        //TODO float weight;

        // color of the pixel first found int
        int color;

        static short keyCoordinate(double c) {
            assert (Math.abs(c) <= 1);
            return (short) Math.round(c * (Short.MAX_VALUE - 1));
        }

        static long key(short frame, short px, short py) {
            return (frame << 32) | (px << 16) | py;
        }

//		public boolean contains(short frame, short px, short py) {
//			return obs.contains(key(frame, px, py));
//		}

        private static long key(short frame, Point2D_F64 p) {
            return key(frame, keyCoordinate(p.x), keyCoordinate(p.y));
        }

    }

    public class PointCloudSpace extends SimpleSpatial {

        public PointCloudSpace() {
            super("PointCloudSpace");
        }

        @Override
        public void renderAbsolute(GL2 gl, int dtMS) {

            float s = 64; //spatial scale

            //draw cameras
            for (Se3_F64 c : motionWorldToCamera) {
                gl.glPushMatrix();
                gl.glTranslated(c.T.x * s, c.T.y * s , c.T.z * s);
                gl.glColor3f(1, 0, 0);

                //TODO rotate
                Draw.glut.glutSolidCube(1);
                gl.glPopMatrix();
            }

            float p = 0.5f; //point scale
            for (Feature3D f : featuresAll) {
                int cc = f.color;
                gl.glPushMatrix();
                gl.glTranslated((float) f.x * s, (float) f.y * s, (float) f.z * s);
                gl.glColor4f(Bitmap2D.decodeRed(cc), Bitmap2D.decodeGreen(cc), Bitmap2D.decodeBlue(cc), 0.75f);
                Draw.glut.glutSolidCube(p);
                gl.glPopMatrix();
            }

        }

    }
}
