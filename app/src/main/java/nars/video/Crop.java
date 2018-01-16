//package nars.remote;
//
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.util.function.Supplier;
//
///**
// * Created by me on 3/8/17.
// */
//public class Crop implements Supplier<BufferedImage> {
//
//    private final Supplier<BufferedImage> in;
//    private final Supplier<int[]> rect;
//    private BufferedImage cropped;
//    private Graphics g;
//
//    public Crop(Supplier<BufferedImage> in, Supplier<int[]> rect) {
//        this.in = in;
//        this.rect = rect;
//    }
//
//    @Override
//    public BufferedImage get() {
//        int[] r = rect.get();
//        int x1 = r[0];
//        int y1 = r[1];
//        int x2 = r[2];
//        int y2 = r[3];
//        int w = x2 - x1;
//        int h = y2 - y1;
//
//        if (cropped == null || cropped.getWidth()!=w || cropped.getHeight()!=h) {
//            cropped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//            g = cropped.getGraphics();
//        } else {
//            g.setColor(Color.BLACK);
//            g.fillRect(0, 0, w, h);
//        }
//
//        BufferedImage src = in.get();
//        src.getSubimage(Math.max(x1, 0), Math.max(y1, 0), Math.min(x2-x1, x2 - src.getWidth());
//
//        return cropped;
//    }
//}
