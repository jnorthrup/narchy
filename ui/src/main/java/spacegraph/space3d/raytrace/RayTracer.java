package spacegraph.space3d.raytrace;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatAveragedWindow;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave2d.Bitmap2D;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

final class RayTracer extends JPanel {

    /** regauge this as a fraction of the screen dimension */
    public final FloatRange grainMax = new FloatRange(0.1f, 0.001f, 0.5f);

    /** accepts value < 1 and >= 1 */
    public final FloatRange superSampling = new FloatRange(1f, 0.25f, 4f);

    private transient float grainMin;


    double fps = 30;

    /** update rate (not alpha chanel) */
    float alpha = 0.85f;

    double CAMERA_EPSILON = 0.001;

    private final Scene scene;
    private final Input input;
    private BufferedImage image;
    private int[] pixelCache;


    private Dimension size;
    private int W, H;
    private double A;



    private RayTracer(Scene scene, Input input) {
        setIgnoreRepaint(true);

        this.scene = scene;
        this.input = input;
        setSize(input.newSize);
    }

    public static void main(String[] args) {
        RayTracer tracer = raytracer();
        tracer.run();
    }

    protected void run() {
        FasterList<Renderer> r = new FasterList();
        int sw = 8, sh = 8;
        float dw = 1f/sw, dh = 1f/sh;
        for (int i = 0; i < sw; i++) {
            for (int j= 0;j < sh; j++) {
                r.add(new Renderer(i * dw, j * dh, (i+1)*dw, (j+1)*dh));
            }
        }

        long sceneTimeNS = Math.round((1.0/fps)*1E9);
        long subSceneTimeNS = sceneTimeNS / r.size();

        while (true) {

            updateCamera();


            //r.parallelStream().forEach(x ->
            r.forEach(x ->
                x.render(subSceneTimeNS)
            );
            repaint();
        }
    }

    private static RayTracer raytracer() {

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.setSize(640, 480);

        Input input = new Input();
        input.newSize = frame.getSize();


        Scene scene = null;
        try {
            String defaults = "camera:\n" +
                    "    position: (4, 4, 4)\n" +
                    "    direction: (-1, -1, -1)\n" +
                    "    fov: 90\n" +
                    "    size: 0\n" +
                    "cube:\n" +
                    "    position: (0, 0, 0)\n" +
                    "    sideLength: 10\n" +
                    "    surface: diffuse\n" +
                    "    texture: texture2.jpg\n" +
                    "sphere:\n" +
                    "    position: (2, 0, 0)\n" +
                    "    radius: 1\n" +
                    "    surface: specular\n" +
                    "sphere:\n" +
                    "    position: (-2, 0, 0)\n" +
                    "    radius: 1\n" +
                    "    surface: specular\n" +
                    "sphere:\n" +
                    "    position: (0, 2, 0)\n" +
                    "    radius: 1\n" +
                    "    surface: specular\n" +
                    "sphere:\n" +
                    "    position: (0, -2, 0)\n" +
                    "    radius: 1\n" +
                    "    surface: specular\n" +
                    "sphere:\n" +
                    "    position: (0, 0, -2)\n" +
                    "    radius: 1\n" +
                    "    surface: specular\n" +
                    "sphere:\n" +
                    "    position: (0, 0, 2)\n" +
                    "    radius: 1\n" +
                    "    surface: specular\n" +
                    "sphere:\n" +
                    "    position: (0, 0, 0)\n" +
                    "    radius: 1\n" +
                    "    surface: specular\n" +
                    "light:\n" +
                    "    position: (0, -2, 4)\n" +
                    "    color: ff0000\n" +
                    "light:\n" +
                    "    position: (0, 0, 4)\n" +
                    "    color: 00ff00\n" +
                    "light:\n" +
                    "    position: (0, 2, 4)\n" +
                    "    color: 0000ff\n";


            scene = new Scene(
                    defaults

            );
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        RayTracer rayTracer = new RayTracer(scene, input);

        frame.add(rayTracer);
        frame.setVisible(true);

        frame.addKeyListener(input);
        frame.addMouseListener(input);
        frame.addMouseMotionListener(input);
        frame.addComponentListener(input);

        return rayTracer;
    }

    @Override
    public void paint(Graphics g) {
        if (image != null)
            g.drawImage(image, 0, 0, this);
    }

    /** returns true if camera changed */
    private boolean updateCamera() {
        if (!input.newSize.equals(size)) {
            size = input.newSize;
            resize();
        }

        grainMin = 1f / (Math.max(W, H) * superSampling.floatValue());

        return scene.camera.update(input, CAMERA_EPSILON);
    }


    class Renderer {
        final Random random = new XoRoShiRo128PlusRandom();

        int window = 16;
        int iterPixels;

        @Deprecated private float eee;

        FloatAveragedWindow e = new FloatAveragedWindow(window, 0.5f, 0.5f).mode(FloatAveragedWindow.Mode.Mean);
        FloatNormalized ee = new FloatNormalized(()->eee, 0, 0.000001f) {

            @Override
            public float asFloat() {
                float MIN_ERROR = 1f / (3*256);

                normalizer.min = MIN_ERROR; //HACK
                float v = super.asFloat();
                normalizer.min = MIN_ERROR; //HACK
                return v;
            }

        };


        final float x1, y1, x2, y2;

        Renderer(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private void render(long sceneTimeNS) {
            float iterCoverage = 0.03f;
            float statRelax = 0.004f;  //determines a passive refresh rate, or fundamental duration

            //TODO trigger total reset when bitmap resized
            ee.relax(statRelax);

            long start = System.nanoTime();


            iterPixels = (int)Math.ceil(iterCoverage * ((x2-x1)*(y2-y1)*W*H));


            double ePixelAverage;
            float grainMax = RayTracer.this.grainMax.floatValue();
            do {

                float ff = e.valueOf(ee.asFloat());

                float fff =
                        //f;
                        (float) Math.pow(
                                //Util.sigmoid((ff - 0.5f) * 2)
                                ff
                                , 2f);


                float grain = Util.lerp(fff, grainMin, grainMax);
                float alphaEffective =
                    alpha;
                    //(float) (alpha * Util.lerp(Math.sqrt(fff), 0.5f, 1f));
                    //this.alpha / Math.max(1,grainPixels*grainPixels);

                float grainPixels = Math.max(1, (float) Math.sqrt( grain * W * H));

                int i = iterPixels;
                if (i > 0) {
                    ePixelAverage = renderStochastic(
                            x1 * W, y1 * H, x2 * W, y2 * H,
                            alphaEffective,
                            grainPixels, i);
                    eee = ((float) ePixelAverage);
                } else
                    eee = 0;


            } while (System.nanoTime() - start <= sceneTimeNS);

        }
        /** returns average pixel error */
        private double renderStochastic(float x1, float y1, float x2, float y2, float alpha, float grainPixels, int iterPixels) {

            float a = random.nextFloat() + 0.5f; //0.5..1.5
            float pw = (float) (Math.sqrt(grainPixels) / a);
            float ph = (grainPixels)/pw; //grainPixels * a;
            float W = Math.max(1,(x2-x1) - pw), H = Math.max(1, (y2-y1) - ph);


            int SW = RayTracer.this.W, SH = RayTracer.this.H;
            double eAvgSum = 0;
            int renderedPixels = 0;
            while (iterPixels > renderedPixels) {

//            int xy = random.nextInt() & ~(1 << 31);
//            int x = (xy & Short.MAX_VALUE) % W;
//            int y = (xy >> 16) % H;
                float x = x1 + random.nextFloat() * W;
                float y = y1 + random.nextFloat() * H;

                float sx1 = Util.clamp(x, 0, SW - 1);
                float sy1 = Util.clamp(y, 0, SH - 1);
                float sx2 = Util.clamp(x + pw, 0, SW - 1);
                float sy2 = Util.clamp(y + ph, 0, SH - 1);


                /** recursion depth of each grain */
                int grainDepthMax = 0;
                double e = renderRecurse(alpha,
                    Math.min(grainDepthMax, (int) Math.floor(Math.log(grainPixels*grainPixels)/Math.log(2))),
                    sx1, sy1, sx2, sy2
                );
                int renderedArea = Math.max(1, (int) ((sy2 - sy1) * (sx2 - sx1)));
                renderedPixels += renderedArea;
                eAvgSum += e * renderedArea;
            }
            return eAvgSum / renderedPixels;
        }


        double renderRecurse(float alpha, int depth, float x1, float y1, float x2, float y2) {
            if (depth == 0) {
                return render(alpha, x1, y1, x2, y2);
            } else {
                float mx = (x2 - x1) / 2f + x1;
                float my = (y2 - y1) / 2f + y1;
                depth--;
                return
                    renderRecurse(alpha, depth, x1, y1, mx, my) +
                    renderRecurse(alpha, depth, mx, y1, x2, my) +
                    renderRecurse(alpha, depth, x1, my, mx, y2) +
                    renderRecurse(alpha, depth, mx, my, x2, y2);
            }
        }


        /** returns sum of pixel errors */
        private double render(float alpha, float fx1, float fy1, float fx2, float fy2) {
            int x1 = Math.round(fx1); int y1 = Math.round(fy1); int x2 = Math.round(fx2); int y2 = Math.round(fy2);
            if (x2 < x1 || y2 < y1)
                return 0;

            double mx = (fx1 + fx2)/2f;
            double my = (fy1 + fy2)/2f;

            float subPixelScatter = 0f;
            if(subPixelScatter >0) {
                mx+= subPixelScatter * (0.5f + (random.nextFloat() - 0.5f) * (fx2 - fx1));
                my+= subPixelScatter * (0.5f + (random.nextFloat() - 0.5f) * (fy2 - fy1));
            }

            int color = color(mx, my);


            int ww = Math.max(1,x2 - x1);
            int W = RayTracer.this.W;
            int[] pixelCache = RayTracer.this.pixelCache;

            long delta = 0;
            int pixels = 0;
            for (int y = y1; y < y2+1; y++) {
                int start = y * W + x1;
                //TODO blend
                //Arrays.fill(pixelCache, start, start + ww, color);
                for (int i = start; i < start + ww; i++) {
                    int current = pixelCache[i];
                    int d = colorDelta(current, color);
                    if (d == 0)
                        continue;

                    delta += d;
                    pixelCache[i] = blend(current, color, alpha);
                }
                pixels+=ww;
            }
            return delta / (3.0 * 256 * pixels);
        }
    }





    private void resize() {
        BufferedImage newImage = new BufferedImage((int) size.getWidth(), (int) size.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        newImage.setAccelerationPriority(1f);

        image = newImage;

        W = image.getWidth();
        H = image.getHeight();
        A = size.getWidth() / size.getHeight();

        pixelCache = ((DataBufferInt) (newImage.getRaster().getDataBuffer())).getData();
    }


//    private void render(int depth) {
//
//
//
//        final int d = depth - 1;
//
//        render(d, 0, 0, W, H);
//
//
//    }



    private static int blend(int current, int next, float alpha) {
        if (alpha >= 1 - (1/256f))
            return next;

        //TODO
        float cr = Bitmap2D.decode8bRed(current), nr = Bitmap2D.decode8bRed(next);
        float cg = Bitmap2D.decode8bGreen(current), ng = Bitmap2D.decode8bGreen(next);
        float cb = Bitmap2D.decode8bBlue(current), nb = Bitmap2D.decode8bBlue(next);
        return Bitmap2D.encodeRGB8b(
                Util.lerpSafe(alpha, cr, nr),
                Util.lerpSafe(alpha, cg, ng),
                Util.lerpSafe(alpha, cb, nb)
        );
    }


    /** in 8-bit color components, ie. white -> black = 3 * 256 difference */
    static int colorDelta(int a, int b) {
        if (a == b) return 0;
        return
            Math.abs( Bitmap2D.iDecode8bRed(a) - Bitmap2D.iDecode8bRed(b)) +
            Math.abs( Bitmap2D.iDecode8bGreen(a) - Bitmap2D.iDecode8bGreen(b)) +
            Math.abs( Bitmap2D.iDecode8bBlue(a) - Bitmap2D.iDecode8bBlue(b));
    }

    private int color(double x, double y) {
        return scene.rayColor(scene.camera.ray(
                x / W,
                1 - y / H,
                A
        ));
    }

}
