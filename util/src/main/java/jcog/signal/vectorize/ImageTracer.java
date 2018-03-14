
package jcog.signal.vectorize;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import static jcog.signal.vectorize.SVGUtils.svgpathstring;
import static jcog.signal.vectorize.SVGUtils.tosvgcolorstr;


public class ImageTracer {

    static final String versionnumber = "1.1.3";

    public ImageTracer() {
    }


    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("ERROR: there's no input filename. Basic usage: \r\n\r\njava -jar ImageTracer.jar <filename>" +
                        "\r\n\r\nor\r\n\r\njava -jar ImageTracer.jar help");


                //System.out.println("Starting anyway with default value for testing purposes.");
                //saveString("output.svg",imageToSVG("input.jpg",new HashMap<String,Float>()));


            } else if (arraycontains(args, "help") > -1) {
                System.out.println("Example usage:\r\n\r\njava -jar ImageTracer.jar <filename> outfilename test.svg " +
                        "ltres 1 qtres 1 pathomit 1 numberofcolors 128 colorquantcycles 15 " +
                        "scale 1 roundcoords 1 lcpr 0 qcpr 0 desc 1 viewbox 0  blurradius 0 blurdelta 20 \r\n" +
                        "\r\nOnly <filename> is mandatory, if some of the other optional parameters are missing, they will be set to these defaults. " +
                        "\r\nWarning: if outfilename is not specified, then <filename>.svg will be overwritten." +
                        "\r\nSee https://github.com/jankovicsandras/imagetracerjava for details. \r\nThis is version " + versionnumber);
            } else {

                // Parameter parsing
                String outfilename = args[0] + ".svg";
                HashMap<String, Float> options = new HashMap<>();
                String[] parameternames = {
                        "ltres", "qtres", "pathomit", "numberofcolors", "colorquantcycles", "scale", "roundcoords", "lcpr", "qcpr", "desc", "viewbox", "outfilename", "blurammount"};
                int j = -1;
                float f = -1;
                for (String parametername : parameternames) {
                    j = arraycontains(args, parametername);
                    if (j > -1) {
                        if (parametername == "outfilename") {
                            if (j < (args.length - 1)) {
                                outfilename = args[j + 1];
                            }
                        } else {
                            f = parsenext(args, j);
                            if (f > -1) {
                                options.put(parametername, f);
                            }
                        }
                    }
                }// End of parameternames loop

                // Loading image, tracing, rendering SVG, saving SVG file
                File file = new File(outfilename);
                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(imageToSVG(args[0], options));
                bw.close();

            }// End of parameter parsing and processing

        } catch (Exception e) {
            e.printStackTrace();
        }
    }// End of main()


    private static int arraycontains(String[] arr, String str) {
        for (int j = 0; j < arr.length; j++) {
            if (arr[j].toLowerCase().equals(str)) {
                return j;
            }
        }
        return -1;
    }


    private static float parsenext(String[] arr, int i) {
        if (i < (arr.length - 1)) {
            try {
                return Float.parseFloat(arr[i + 1]);
            } catch (Exception e) {
            }
        }
        return -1;
    }


    // Container for the color-indexed image before and tracedata after vectorizing
    public static class IndexedImage {
        public final int width;
        public final int height;
        public final int[][] array; // array[x][y] of palette colors
        public final byte[][] palette;// array[palettelength][4] RGBA color palette
        public ArrayList<ArrayList<ArrayList<Double[]>>> layers;// tracedata

        public IndexedImage(int[][] marray, byte[][] mpalette) {
            array = marray;
            palette = mpalette;
            width = marray[0].length - 2;
            height = marray.length - 2;// Color quantization adds +2 to the original width and height
        }

        protected void render(HashMap<String, Float> options, float w, float h, BiConsumer<ArrayList<Double[]>, byte[]> path) {
            // creating Z-index
            TreeMap<Double, int[]> zindex = new TreeMap<>();
            double label;

            // Layer loop
            for (int k = 0; k < this.layers.size(); k++) {

                // Path loop
                ArrayList<ArrayList<Double[]>> lk = this.layers.get(k);

                for (int pcnt = 0; pcnt < lk.size(); pcnt++) {

                    // Label (Z-index key) is the startpoint of the path, linearized
                    Double[] lkp0 = lk.get(pcnt).get(0);

                    label = (lkp0[2] * w) + lkp0[1];
                    int finalPcnt = pcnt;
                    int finalK = k;
                    // Adding layer and path number to list
                    zindex.computeIfAbsent(label, (l)-> new int[] {finalK, finalPcnt} );
                }

            }

            // Sorting Z-index is not required, TreeMap is sorted automatically

            // Drawing
            // Z-index loop
            for (Map.Entry<Double, int[]> entry : zindex.entrySet()) {
                int[] v = entry.getValue();
                path.accept(this.layers.get(v[0]).get(v[1]), this.palette[v[0]]);
            }

        }

        // Converting tracedata to an SVG string, paths are drawn according to a Z-index
        // the optional lcpr and qcpr are linear and quadratic control point radiuses
        public String toSVG(HashMap<String, Float> options) {
            // SVG start
            float w = (int) (this.width * options.get("scale")), h = (int) (this.height * options.get("scale"));

            String viewboxorviewport = options.get("viewbox") != 0 ? "viewBox=\"0 0 " + w + " " + h + "\" " : "width=\"" + w + "\" height=\"" + h + "\" ";
            StringBuilder svgstr = new StringBuilder("<svg " + viewboxorviewport + "version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" ");
            if (options.get("desc") != 0) {
                svgstr.append("desc=\"Created with ImageTracer.java version ").append(ImageTracer.versionnumber).append("\" ");
            }
            svgstr.append(">");

            render(options, w, h, (path, color)->{

//                if (options.get("desc") != 0) {
//                    thisdesc = "desc=\"l " + v[0] + " p " + v[1] + "\" ";
//                } else {
//                    thisdesc = "";
//                }
                svgpathstring(svgstr,
                        "",
                        path,
                        tosvgcolorstr(color),
                        options);
            });



            // SVG End
            svgstr.append("</svg>");

            return svgstr.toString();

        }
    }


    // The bitshift method in loadImageData creates signed bytes where -1 -> 255 unsigned ; -128 -> 128 unsigned ;
    // 127 -> 127 unsigned ; 0 -> 0 unsigned ; These will be converted to -128 (representing 0 unsigned) ...
    // 127 (representing 255 unsigned) and tosvgcolorstr will add +128 to create RGB values 0..255
    private static byte bytetrans(byte b) {
        if (b < 0) {
            return (byte) (b + 128);
        } else {
            return (byte) (b - 128);
        }
    }

    private static byte[][] getPalette(BufferedImage image, HashMap<String, Float> options) {
        int numberofcolors = options.get("numberofcolors").intValue();
        int[][] pixels = new int[image.getWidth()][image.getHeight()];

        for (int i = 0; i < image.getWidth(); i++)
            for (int j = 0; j < image.getHeight(); j++) {
                pixels[i][j] = image.getRGB(i, j);
            }
        int[] palette = Quantize.quantizeImage(pixels, numberofcolors);
        byte[][] bytepalette = new byte[numberofcolors][4];

        for (int i = 0; i < palette.length; i++) {
            Color c = new Color(palette[i]);
            bytepalette[i][0] = (byte) c.getRed();
            bytepalette[i][1] = (byte) c.getGreen();
            bytepalette[i][2] = (byte) c.getBlue();
            bytepalette[i][3] = 0;
        }
        return bytepalette;
    }

    ////////////////////////////////////////////////////////////
    //
    //  User friendly functions
    //
    ////////////////////////////////////////////////////////////

    // Loading an image from a file, tracing when loaded, then returning the SVG String
    private static String imageToSVG(String filename, HashMap<String, Float> options) throws Exception {


        //System.out.println(options.toString());

        HashMap<String, Float> o = checkoptions(options);

        return new ImageData(ImageIO.read(new File(filename)))
                .trace(o)
                .toSVG(o);
    }// End of imageToSVG()


//	// Loading an image from a file, tracing when loaded, then returning IndexedImage with tracedata in layers
//	public IndexedImage imageToTracedata (String filename, HashMap<String,Float> options, byte [][] palette) throws Exception{
//		options = checkoptions(options);
//		ImageData imgd = loadImageData(filename, options);
//		return imagedataToTracedata(imgd,options,palette);
//	}// End of imageToTracedata()
//	public IndexedImage imageToTracedata (BufferedImage image, HashMap<String,Float> options, byte [][] palette) throws Exception{
//		options = checkoptions(options);
//		ImageData imgd = loadImageData(image);
//		return imagedataToTracedata(imgd,options,palette);
//	}// End of imageToTracedata()




    // creating options object, setting defaults for missing values
    private static HashMap<String, Float> checkoptions(HashMap<String, Float> options) {
        if (options == null) {
            options = new HashMap<>();
        }
        // Tracing
        if (!options.containsKey("ltres")) {
            options.put("ltres", 10.0f);
        }
        if (!options.containsKey("qtres")) {
            options.put("qtres", 10.0f);
        }
        if (!options.containsKey("pathomit")) {
            options.put("pathomit", 1.0f);
        }
        // Color quantization
        if (!options.containsKey("numberofcolors")) {
            options.put("numberofcolors", 128.0f);
        }
        if (!options.containsKey("colorquantcycles")) {
            options.put("colorquantcycles", 15.0f);
        }
        // SVG rendering
        if (!options.containsKey("scale")) {
            options.put("scale", 1.0f);
        }
        if (!options.containsKey("roundcoords")) {
            options.put("roundcoords", 1.0f);
        }
        if (!options.containsKey("lcpr")) {
            options.put("lcpr", 0.0f);
        }
        if (!options.containsKey("qcpr")) {
            options.put("qcpr", 0.0f);
        }
        if (!options.containsKey("desc")) {
            options.put("desc", 1.0f);
        }
        if (!options.containsKey("viewbox")) {
            options.put("viewbox", 0.0f);
        }
        // Blur
        if (!options.containsKey("blurradius")) {
            options.put("blurradius", 5.0f);
        }
        if (!options.containsKey("blurdelta")) {
            options.put("blurdelta", 50.0f);
        }

        return options;
    }// End of checkoptions()


    // https://developer.mozilla.org/en-US/docs/Web/API/ImageData
    public static class ImageData {
        public final int width;
        public final int height;
        public final byte[] data; // raw byte data: R G B A R G B A ...
        @Deprecated private final BufferedImage image;

        public ImageData(int mwidth, int mheight, byte[] mdata) {
            width = mwidth;
            height = mheight;
            data = mdata;
            this.image = null;
        }

        public ImageData(BufferedImage image) {
            this.image = image;
            this.width = image.getWidth();
            this.height = image.getHeight();
            int[] rawdata = image.getRGB(0, 0, width, height, null, 0, width);
            byte[] data = new byte[rawdata.length * 4];
            for (int i = 0; i < rawdata.length; i++) {
                int r = rawdata[i];
                data[(i * 4) + 3] = bytetrans((byte) (r >>> 24));
                data[(i * 4)] = bytetrans((byte) (r >>> 16));
                data[(i * 4) + 1] = bytetrans((byte) (r >>> 8));
                data[(i * 4) + 2] = bytetrans((byte) r);
            }
            this.data = data;

        }

        public IndexedImage trace(HashMap<String, Float> options) {

            byte[][] palette = getPalette(image, options);

            // 1. Color quantization
            IndexedImage ii = VectorizingUtils.colorquantization(this, palette, options);
            // 2. Layer separation and edge detection
            int[][][] rawlayers = VectorizingUtils.layering(ii);
            // 3. Batch pathscan
            ArrayList<ArrayList<ArrayList<Integer[]>>> bps = VectorizingUtils.batchpathscan(rawlayers, (int) (Math.floor(options.get("pathomit"))));
            // 4. Batch interpollation
            ArrayList<ArrayList<ArrayList<Double[]>>> bis = VectorizingUtils.batchinternodes(bps);
            // 5. Batch tracing
            ii.layers = VectorizingUtils.batchtracelayers(bis, options.get("ltres"), options.get("qtres"));
            return ii;

        }
    }
}// End of ImageTracer class
