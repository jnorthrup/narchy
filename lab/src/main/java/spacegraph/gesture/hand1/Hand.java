//package spacegraph.gesture.hand1;
//
//import org.opencv.core.*;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.imgproc.Moments;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.PrintWriter;
//import java.util.*;
//
//public class Hand {
//
//	public static final int MIN_HAND_AREA = 5000;
//	Mat mat;
//	Point cog = new Point();
//	List<Point> fingers = new ArrayList<Point>();
//	Mat clean;
//	boolean fist;
//
//	private static final int VIDEO_WIDTH = 640;
//	private static final int VIDEO_HEIGHT = 360;
//
//	private PrintWriter out;
//
//	public static class HandProfile {
//
//		List<double[]> colors = new ArrayList<double[]>();
//
//		public HandProfile(Mat mat){
//			//Imgproc.rectangle(mat, r.tl(), r.br(), new Scalar(255, 255, 255), 2);
//			Imgproc.medianBlur(mat, mat, 5);
//
//			for (int i = 0; i < 4; i++){
//				for (int j = 0; j < 4; j++){
//					Point p = new Point(i * (mat.width()-1)/3, j * (mat.height()-1)/3);
//					//System.out.println(p.toString() + " " + Arrays.toString((mat.get((int) p.y, (int) p.x))));
//					colors.add(mat.get((int) p.y, (int) p.x));
//				}
//			}
//
//			for (double[] color: colors)
//			{
//				System.out.print(Arrays.toString(color));
//				System.out.print("*");
//			}
//			System.out.println();
//			//Imgproc.circle(mat, new Point(mat.width() / 2, mat.height() / 2), 20, new Scalar(255));
//		}
//
//		public HandProfile(String input){
//			String[] strs = input.split("\\*");
//			for (String color: strs){
//				if (color.isEmpty()){
//					continue;
//				}
//
//				color = color.substring(1, color.length()-1);
//				String[] values = color.split(",");
//
//				double h = Double.parseDouble(values[0].trim());
//				double s = Double.parseDouble(values[1].trim());
//				double v = Double.parseDouble(values[2].trim());
//
//				double[] hsv = new double[]{h, s, v};
//				colors.add(hsv);
//			}
//
//			for (double[] color: colors)
//			{
//				System.out.print(Arrays.toString(color));
//				System.out.print("*");
//			}
//			System.out.println();
//		}
//
//		public  String toString(){
//			String ret = "";
//			for (double[] color: colors)
//			{
//				ret += Arrays.toString(color) + "*";
//			}
//			return ret;
//		}
//
//		public Mat thresholdHand(Mat mat){
//			Mat sum = null;
//			for(double[] hsv: colors){
//				Mat thresh = new Mat();
//				//System.out.println(Arrays.toString(hsv));
//
//				if (hsv == null){
//					continue;
//				}
//
//				int dA = 20;
//				int dB = 50;
//				int dC = 50;
//				Scalar low = new Scalar(truncate(hsv[0]) - dA, truncate(hsv[1]) - dB, truncate(hsv[2]) - dC);
//				Scalar high = new Scalar(truncate(hsv[0]) + dA, truncate(hsv[1]) + dB, truncate(hsv[2]) + dC);
//				Core.inRange(mat, low, high, thresh);
//				if (sum == null){
//					//System.out.print("a");
//					sum = thresh;
//					//break;
//				}else {
//					//System.out.print("b");
//					Core.bitwise_or(sum, thresh, sum);
//				}
//			}
//
//			return sum;
//		}
//
//		private static double truncate(double x){
//			if (x < 0){
//				return 0;
//			}
//			return x;
//		}
//
//	}
//
//
//
//	static HandProfile p;
//
//	static {
//		// Restore hand profile
//		Scanner in = null;
//		try {
//			in = new Scanner(new File("settings"));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		if (in.hasNextLine()){
//			p = new HandProfile(in.nextLine());
//		}
//	}
//
////	public static void main(String... args)  {
////
//////		Socket echoSocket = new Socket("172.16.98.47", 1999);
//////	    out =
//////	        new PrintWriter(echoSocket.getOutputStream(), true);
//////
////
////
////		System.loadLibrary(Core.NATIVE_LIBRARY_NAME + "");
////
////
////
////
////
////		JFrame frame = new JFrame("Finger Tracking");
////		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
////		frame.setSize(500, 800);
////
//////		VideoPanel panel = new VideoPanel();
//////		frame.setContentPane(panel);
//////		panel.delegate = this;
////
////
////		frame.setVisible(true);
////
////		//p = new HandProfile(ProfilePresets.naturalLight);
////
////		new spacegraph.video.Webcam(800, 600, true) {
////
////			@Override
////			public void webcamImageObtained(WebcamEvent webcamEvent) {
////				BufferedImage wb = webcamEvent.getImage();
////				int w = wb.getWidth();
////				int h = wb.getHeight();
////				Mat mat = new Mat(w, h, CvType.makeType(8,3));
////				int[] pp = new int[w * h * 3];
////				wb.getData().getPixels(0, 0, w, h, pp);
////				mat.put(0, 0, pp);
////
////
////						/*
////						 * Crop image to hand area
////						 */
////				Point a = new Point(VIDEO_WIDTH * 0.46 * 2, VIDEO_HEIGHT * 0.3 * 2);
////				Point b = new Point(VIDEO_WIDTH * 0.6 * 2, VIDEO_HEIGHT * 0.64 * 2);
////
////				Rect roi = new Rect((int) a.x, (int) a.y, (int) (b.x - a.x), (int) (b.y - a.y));
////				Mat cropped = new Mat(mat, roi);
////
////				//Imgproc.rectangle(mat, a, b, new Scalar(0, 0, 255), 5);
////
////				mat = processMat(mat);
////
////				//double[] d = mat.get();
////				//BufferedImage img = MatConverter.convertMatToBufferedImage(mat);
////
////				//panel.setImage(img);
////				//panel.repaint();
////				// break;
////
////
////			}
////		};
//////		VideoCapture webcam = new VideoCapture(0);
//////		if (webcam.isOpened()) {
//////			System.out.println("Found Webcam: " + webcam.toString());
//////		} else {
//////			System.out.println("Connection to webcam failed!");
//////		}
////
////	}
//
//	Mat profileRoi;
//	private Mat processMat(Mat original){
//		Mat mat = original.clone();
//
//
//		Point a = new Point(0, 0);
//		Point b = new Point(VIDEO_WIDTH * 0.3 * 2, VIDEO_HEIGHT * 0.6 * 2);
//
//		Rect roi = new Rect((int) a.x, (int) a.y, (int) (b.x - a.x), (int) (b.y - a.y));
//		mat = new Mat(mat, roi);
//
//		int width = 80;
//		int height = 80;
//		Rect r = new Rect(mat.width()/2-width/2, mat.height()/2-height/2, width, height);
//		Mat clean = mat.clone();
//		//System.out.println(r);
//
//		//Imgproc.medianBlur(mat, mat, 21);
//		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV);
//
//		profileRoi = new Mat(mat.clone(), r);
//		//Imgproc.medianBlur(mat, mat, 5);
//		//Imgproc.rectangle(mat, r.tl(), r.br(), new Scalar(0, 255, 0), 2);
//
//		if (p == null){
//			//p = new HandProfile("[170.0, 26.0, 117.0]*[170.0, 27.0, 114.0]*[170.0, 27.0, 112.0]*[170.0, 28.0, 108.0]*[165.0, 26.0, 120.0]*[168.0, 28.0, 117.0]*[168.0, 27.0, 114.0]*[170.0, 28.0, 111.0]*[164.0, 27.0, 123.0]*[161.0, 24.0, 118.0]*[166.0, 28.0, 118.0]*[166.0, 29.0, 115.0]*[163.0, 24.0, 125.0]*[163.0, 25.0, 121.0]*[164.0, 27.0, 121.0]*[164.0, 28.0, 119.0]*");
//			return mat;
//		}
//
//		Mat segmented =  Segmenter.segment(clean, p);
////		if (true){
////			return segmented;
////		}
//		//return segmented;
//		Hand h = new Hand(segmented, clean);
//		//String info = h.cog + " " + (h.fist ? "true": "false") + " " + h.fingers.size();
//		//System.out.println(info);
//
//		//s.sendToAll(info);
//
//		int xMovement =  (int) ((h.cog.x / segmented.width() - 0.5)*30.0);
//		int yMovement =  (int) ((h.cog.y / segmented.height() - 0.5)*10.0);
//		int fingers = h.fingers.size();
//		String output = String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d\n", 0, 0, 0, 0, 0, fingers, 0, 0, 0);
//
//		System.out.println(output);
//		out.println(output);
//		//s.sendToAll(text);
//
//		return h.clean;
//	}
//
//	boolean first = true;
//	public void mousePressed() throws FileNotFoundException {
//		if (first && p != null){
//
//			p = null;
//			first = false;
//			return;
//		}else if (profileRoi != null){
//			p = new HandProfile(profileRoi);
//			first = true;
//
//			PrintWriter writer = new PrintWriter(new File("settings"));
//			writer.print(p.toString());
//			writer.close();
//		}
//	}
//
//	static class ProfilePresets {
//		public static String naturalLight = "[173.0, 116.0, 90.0]*[173.0, 116.0, 97.0]*[173.0, 100.0, 99.0]*[173.0, 106.0, 94.0]*[172.0, 110.0, 95.0]*[173.0, 110.0, 93.0]*[173.0, 103.0, 92.0]*[172.0, 92.0, 94.0]*[172.0, 106.0, 103.0]*[172.0, 114.0, 96.0]*[172.0, 109.0, 89.0]*[173.0, 99.0, 93.0]*[173.0, 108.0, 102.0]*[173.0, 115.0, 95.0]*[173.0, 106.0, 89.0]*[173.0, 102.0, 90.0]*";
//	}
//
//	static class Segmenter {
//
//		/*
//         * Primary segmentation algorithm using HSV thresholding and morphological operations
//         */
//		public static Mat segment(Mat mat, HandProfile profile) {
//			mat = mat.clone();
//			// Convert the color space from BGR to HSV
//			Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV);
//			Imgproc.medianBlur(mat, mat, 5);
//
//			if (true){
//				//return mat;
//			}
//			//HandProfile profile = new HandProfile(new Mat(mat, r));
//			Mat sum = profile.thresholdHand(mat);
//
//
//			Imgproc.medianBlur(sum, sum, 11);
//			//Imgproc.erode(sum, sum, Mat.ones(10, 10, CvType.CV_8UC1));
//
//			//Imgproc.morphologyEx(sum, sum, Imgproc.MORPH_CLOSE, Mat.ones(5, 5, CvType.CV_8UC1));
//
//			return sum;
//		}
//
//
//
//		/*
//         * Experimental second segmentation algorithm using a histogram and back projection. Doesn't really work
//         */
//		public static void segment2(Mat in, Mat source, Mat mat) {
//
//			//Imgproc.GaussianBlur(in, mat, new Size(31, 31), 0);
//			Imgproc.medianBlur(in, mat, 21);
//			Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV);
//
//			mat = histAndBackproj(mat, source);
//
//			Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, Mat.ones(10, 10, CvType.CV_8UC1));
//			Imgproc.GaussianBlur(mat, mat, new Size(31, 31), 0);
//		}
//
//		private static Mat histAndBackproj(Mat source, Mat sourceHist) {
//			Mat hist = new Mat();
//			int h_bins = 30;
//			int s_bins = 32;
//
//			MatOfInt mHistSize = new MatOfInt(h_bins, s_bins);
//			MatOfFloat mRanges = new MatOfFloat(0, 179, 0, 255);
//			MatOfInt mChannels = new MatOfInt(0, 1);
//
//			boolean accumulate = false;
//			Imgproc.calcHist(Arrays.asList(sourceHist), mChannels, new Mat(), hist, mHistSize, mRanges, accumulate);
//
//			Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX, -1, new Mat());
//
//			Mat backproj = new Mat();
//			Imgproc.calcBackProject(Arrays.asList(source), mChannels, hist, backproj, mRanges, 1);
//
//			return backproj;
//		}
//
//	}
//
////sum.copyTo(mat);
//
//
//
////Mat matC = new Mat();
////Mat matD = new Mat();
////
////Core.inRange(mat, new Scalar(0, 60, 30), new Scalar(10, 120, 255), matC);
////Core.inRange(mat, new Scalar(175, 95, 100), new Scalar(195, 115, 150), matD);
////
////
////// Perform morphological operations
////Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, Mat.ones(2, 2, CvType.CV_8UC1));
////Imgproc.erode(mat, mat, Mat.ones(55, 55, CvType.CV_8UC1));
////
////Core.bitwise_or(matC, matD, mat);
////return mat;
//
//
//	public Hand(Mat mat, Mat clean) {
//		this.mat = mat;
//		this.clean = clean;
//		process();
//	}
//
//	public void process() {
//
//		// Get the contour for the hand
//		MatOfPoint hand = getHandContour();
//
//		if (hand == null){
//			return;
//		}
//
//		// Get center of gravity
//		cog = getCOG(hand);
//		//Imgproc.circle(clean, cog, 30, new Scalar(255, 255, 0));
//
//		//System.out.print("Cog: " + cog.toString());
//		//Imgproc.putText(clean, cog.toString(), new Point(50, 25), 0, 0.6, new Scalar(0, 0, 255), 2);
//
//		// Convert hand to simpler polygon
//		MatOfPoint2f handPoly2f = new MatOfPoint2f();
//		Imgproc.approxPolyDP(new MatOfPoint2f(hand.toArray()), handPoly2f, 15, true);
//		hand = new MatOfPoint(handPoly2f.toArray());
//
//		// Get the convex hull
//		MatOfInt hull = new MatOfInt();
//		Imgproc.convexHull(hand, hull, true);
//
//		// Turn the convex hull from a MatOfInt in to a MatOfPoint
//		MatOfPoint hullContour = new MatOfPoint();
//		List<Point> hullPoints = new ArrayList<Point>();
//
//		for (int j = 0; j < hull.toList().size(); j++) {
//			Point p = hand.toList().get(hull.toList().get(j));
//			//Imgproc.putText(clean, hull.toList().get(j) + "", p, 0, 0.6, new Scalar(0, 255, 0));
//			////System.out.println(hull.toList().get(j));
//			Point pBefore;
//
//			if (j >= 1 ){
//				pBefore = hand.toList().get(hull.toList().get(j-1));
//			}else {
//				pBefore = hand.toList().get(hull.toList().get(hull.toList().size()-1));
//			}
//
//			if (distance(pBefore, p) < 50){
//				continue;
//			}
//
//			hullPoints.add(p);
//		}
//		hullContour.fromList(hullPoints);
//
//
//		for (Point p: hand.toList()){
//			//Imgproc.putText(clean, hand.toList().indexOf(p) + "", p, 0, 0.6, new Scalar(0, 255, 0));
//		}
//
//		double minY = Double.MAX_VALUE;
//		for (Point p: hullPoints){
//			if (p.y < minY){
//				minY = (int) p.y;
//			}
//		}
//
//		double dist = cog.y - minY;
//		////System.out.println(" Dist: " + dist);
//
//		System.out.println(dist);
//		if (dist < 150){
//			//System.out.println("FIST");
//			fist = true;
//			//Imgproc.putText(clean, "FIST", new Point(50, 75), 0, 0.6, new Scalar(255, 0, 0), 2);
//			return;
//		}else {
//			fist = false;
//			//Imgproc.putText(clean, "NO FIST", new Point(50, 75), 0, 0.6, new Scalar(255, 0, 0), 2);
//		}
//
//		Imgproc.drawContours(clean, Arrays.asList(hand), 0, new Scalar(255, 255, 255, 255), 1);
//		Imgproc.drawContours(clean, Arrays.asList(hullContour), 0, new Scalar(255, 0, 0), 2);
//
//		fingers = new ArrayList<Point>();
//		int fingerCount = 0;
//		for (Point p : hullPoints) {
//			if (p.y < cog.y){
//				if (distance(p, cog) < 110){
//					continue;
//				}
//				fingerCount++;
//				fingers.add(p);
//				//Imgproc.circle(clean, p, 5, new Scalar(0, 0, 255), 5);
//			}
//		}
//
//		//System.out.println(fingerCount);
//		//Imgproc.putText(clean, fingerCount + " fingers", new Point(50, 50), 0, 0.6, new Scalar(0, 255, 0), 2);
//	}
//
//	private static double angleBetween(Point center, Point current, Point previous) {
//
//		return Math.toDegrees(Math.atan2(current.x - center.x, current.y - center.y)
//				- Math.atan2(previous.x - center.x, previous.y - center.y));
//	}
//
//	private static Point mid(Point left, Point right) {
//		return new Point((left.x + right.x) / 2, (left.y + right.y) / 2);
//	}
//
//
//	private static double distance(Point a, Point b){
//		return Math.sqrt((a.x - b.x)*(a.x - b.x) + (a.y - b.y)*(a.y - b.y));
//	}
//
//	private static Integer hullNeighbor(int defectIndex, MatOfInt hull, boolean direction) {
//		List<Integer> hullPoints = hull.toList();
//		Collections.sort(hullPoints);
//
//		if (direction) {
//			for (Integer i : hullPoints) {
//				if (i == defectIndex) {
//					return null;
//				}
//				if (i > defectIndex) {
//					return i;
//				}
//			}
//			return 0;
//		} else {
//			int last = 0;
//			for (Integer i : hullPoints) {
//				if (i == defectIndex) {
//					return null;
//				}
//				if (i > defectIndex) {
//					return last;
//				}
//				last = i;
//			}
//			return last;
//		}
//
//	}
//
//	private MatOfPoint getHandContour(){
//		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//		Imgproc.findContours(mat.clone(), contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//
//		MatOfPoint hand = null;
//		for (MatOfPoint contour : contours) {
//			if (hand != null) {
//				if (Imgproc.contourArea(hand) < Imgproc.contourArea(contour) && Imgproc.contourArea(contour) > 4000)
//					hand = contour;
//			} else {
//				hand = contour;
//			}
//		}
//
//		if (hand == null || Imgproc.contourArea(hand) < MIN_HAND_AREA){
//			return null;
//		}
//
//		System.out.println(Imgproc.contourArea(hand));
//
//		return hand;
//	}
//
//	private Point getCOG(Mat contour){
//		Moments p = Imgproc.moments(contour);
//		int x = (int) (p.get_m10() / p.get_m00());
//        int y = (int) (p.get_m01() / p.get_m00());
//
//        return new Point(x, y);
//	}
//}
