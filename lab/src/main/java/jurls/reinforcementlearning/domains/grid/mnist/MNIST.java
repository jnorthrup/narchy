package jurls.reinforcementlearning.domains.grid.mnist;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * This class implements a reader for the MNIST dataset of handwritten digits.
 * The dataset is found at http:
 *
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class MNIST {

    public static class MNISTImage {
        public final byte label;
        public final int[][] image;

        
        public MNISTImage(int w, int h) {
            label = -1;
            image = new int[h][];
            for (var i = 0; i < h; i++) {
                image[i] = new int[w];
            }
        }
        
        public MNISTImage(byte label, int[][] image) {
            assert(label >= 0);
            assert(label < 10);
            this.label = label;
            this.image = image;
        }

        public void toArray(double[] v, double noise) {
            assert(v.length == image.length * image[0].length);
            var c = 0;
            for (var y = 0; y < image.length; y++)
                for (var x = 0; x < image[y].length; x++) {
                    v[c] = (image[x][y])/256.0 +  (Math.random()-0.5)*noise;
                    v[c] = Math.min(1, Math.max(v[c], 0));
                    c++;
                }
        }
        
        public int width() { return image[0].length; }
        public int height() { return image.length; }

        void scrollRight(MNISTImage i, int nextColumn) {
            for (var y = 0; y < height(); y++) {
                int x;
                for (x = 1; x < width(); x++) {
                    image[x-1][y] = image[x][y];
                }
                image[x-1][y] = i.image[nextColumn][y];
            }
        }
        
    }
    
    public final List<MNISTImage> images;

    /**
     * @param args args[0]: label file; args[1]: data file.
     * @throws IOException
     */
    public MNIST(String path, int maxImages, int maxDigit) throws IOException {
        
        images = new ArrayList(maxImages);

        var labelStream = new DataInputStream(new GZIPInputStream(new FileInputStream(path + "/train-labels-idx1-ubyte.gz")));
        var imageStream = new DataInputStream(new GZIPInputStream(new FileInputStream(path + "/train-images-idx3-ubyte.gz")));

        var magicNumber = labelStream.readInt();
        if (magicNumber != 2049) {
            System.err.println("Label file has wrong magic number: " + magicNumber + " (should be 2049)");
            System.exit(0);
        }
        magicNumber = imageStream.readInt();
        if (magicNumber != 2051) {
            System.err.println("Image file has wrong magic number: " + magicNumber + " (should be 2051)");
            System.exit(0);
        }
        var numLabels = labelStream.readInt();
        var numImages = imageStream.readInt();
        var numRows = imageStream.readInt();
        var numCols = imageStream.readInt();
        if (numLabels != numImages) {
            System.err.println("Image file and label file do not contain the same number of entries.");
            System.err.println("  Label file contains: " + numLabels);
            System.err.println("  Image file contains: " + numImages);
            System.exit(0);
        }

        var start = System.currentTimeMillis();
        var numLabelsRead = 0;
        var numImagesRead = 0;
        while (maxImages > 0 && labelStream.available() > 0 && numLabelsRead < numLabels) {
            var label = labelStream.readByte();
            numLabelsRead++;
            var image = new int[numCols][numRows];
            for (var colIdx = 0; colIdx < numCols; colIdx++) {
                for (var rowIdx = 0; rowIdx < numRows; rowIdx++) {
                    image[colIdx][rowIdx] = imageStream.readUnsignedByte();
                }
            }
            numImagesRead++;
            if (label <= maxDigit)            
                images.add(new MNISTImage(label, image));

      
            if (numLabelsRead % 10 == 0) {
                System.out.print(".");
            }
            if ((numLabelsRead % 800) == 0) {
                System.out.print(" " + numLabelsRead + " / " + numLabels);
                var end = System.currentTimeMillis();
                var elapsed = end - start;
                var minutes = elapsed / (1000 * 60);
                var seconds = (elapsed / 1000) - (minutes * 60);
                System.out.println("  " + minutes + " m " + seconds + " s ");
            }
            
            maxImages--;
        }
        
        System.out.println();
        var end = System.currentTimeMillis();
        var elapsed = end - start;
        var minutes = elapsed / (1000 * 60);
        var seconds = (elapsed / 1000) - (minutes * 60);
        System.out.println("Read " + numLabelsRead + " samples in " + minutes + " m " + seconds + " s ");
    }

    public double[][] getImageVectors() {
        var v = new double[images.size()][];
        for (var i = 0; i < images.size(); i++) {
            var m = images.get(i);
            v[i] = new double[m.width() * m.height()];
            m.toArray(v[i], 0);
        }
        return v;
    }
    
    /*public static void main(String[] args) throws IOException {
        new MNIST("/home/me/Downloads",1000);
    }*/
}
