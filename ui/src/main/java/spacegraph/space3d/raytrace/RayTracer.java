package spacegraph.space3d.raytrace;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatAveragedWindow;
import jcog.math.FloatNormalized;
import jcog.math.vv3;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave2d.Bitmap2D;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

final class RayTracer extends JPanel {

    /** regauge this as a fraction of the screen dimension */
    public static final float grainMax = 0.01f;
    float grainMin;
    private int superSampling = 1;

    double fps = 30;

    /** update rate (not alpha chanel) */
    float alpha = 0.75f;

    double CAMERA_EPSILON = 0.001;

    /** recursion depth of each grain */
    private int grainDepthMax = 0;

    private final float subPixelScatter = 0.01f;

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
        int sw = 4, sh = 4;
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

            r.forEach(x -> x.render(subSceneTimeNS));
        }
    }

    private static RayTracer raytracer() {
        Input input;
        RayTracer rayTracer;

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.setSize(640, 480);

        input = new Input();
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
        rayTracer = new RayTracer(scene, input);




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

        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
    }

    /** returns true if camera changed */
    private boolean updateCamera() {
        if (!input.newSize.equals(size)) {
            size = input.newSize;
            resize();
        }

        vv3 cameraPos = scene.camera.position.clone(), cameraDir = scene.camera.direction.clone();
        scene.camera.rotate(input.getDeltaMouseX() / 2.0, -input.getDeltaMouseY() / 2.0);
        scene.camera.move(input.getKeyboardVector());

        return !cameraPos.equals(scene.camera.position, CAMERA_EPSILON) || !cameraDir.equals(scene.camera.direction, CAMERA_EPSILON);
    }


    class Renderer {

        int window = 8;
        int iter = 16;

        @Deprecated private float eee;

        FloatAveragedWindow e = new FloatAveragedWindow(window, 0.5f, 0.5f).mode(FloatAveragedWindow.Mode.Mean);
        FloatNormalized ee = new FloatNormalized(()->eee, 0, 0.000001f);

        {
            ee.relax(0.1f);
        }

        final float x1, y1, x2, y2;
        float grain = grainMax;

        Renderer(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private void render(long sceneTimeNS) {


            boolean moving;
            long start = System.nanoTime();


            long timeUsedNS = 0;

            float WHmax = Math.max(W, H);

            double ePixelAverage;
            do {




                grain = Math.max(grainMin, (float) Util.lerp(Math.pow(e.max(), 1f), grainMin, grainMax));

                float grainPixels = Math.max(1,grain * WHmax);

                //System.out.println(grain + " " +  grainPixels);

                float alphaEffective =
                        alpha;
                          //this.alpha / Math.max(1,grainPixels*grainPixels);

                ePixelAverage = renderStochastic(
                        x1*W,y1*H,x2*W,y2*H,
                        alphaEffective,
                        grainPixels, iter);
                eee = ((float) ePixelAverage);
                e.next(ee.asFloat());


            } while (System.nanoTime() - start <= sceneTimeNS);
//            System.out.println("eAvg = " + ePixelAverage + " " + ee.toString());

            repaint();

//        System.out.println(Texts.timeStr(timeUsedNS) );
//        boolean renderComplete = (pixelsRemain == 0);
        }
    }


    final Random random = new XoRoShiRo128PlusRandom(1);

    private double renderStochastic(float x1, float y1, float x2, float y2, float alpha, float grainPixels, int iterPixels) {

        float a = random.nextFloat() + 0.5f; //0.5..1.5
        float pw = grainPixels / a;
        float ph = (grainPixels*grainPixels)/pw; //grainPixels * a;
        float W = (x2-x1) - pw, H = (y2-y1) - ph;

        int iterGrains = Math.round(((float)iterPixels) / (grainPixels*grainPixels));

        double eTotal = 0;
        for (int i = 0; i < iterGrains; i++) {

//            int xy = random.nextInt() & ~(1 << 31);
//            int x = (xy & Short.MAX_VALUE) % W;
//            int y = (xy >> 16) % H;
            float x = random.nextFloat() * (W-1);
            float y = random.nextFloat() * (H-1);

            double e = renderRecurse(alpha, Math.min(grainDepthMax, (int) Math.floor(Math.log(grainPixels*grainPixels)/Math.log(2))),
                    x + x1, y + y1, x + pw + x1, y+ph + y1);
            eTotal += e;
        }
        double eAvg = eTotal / (iterPixels * (pw*ph));
        return eAvg;
    }


    private void resize() {
        BufferedImage newImage = new BufferedImage((int) size.getWidth(), (int) size.getHeight(), BufferedImage.TYPE_INT_RGB);
        newImage.setAccelerationPriority(1f);


        image = newImage;

        W = image.getWidth();
        H = image.getHeight();
        A = size.getWidth() / size.getHeight();

        pixelCache = ((DataBufferInt) (newImage.getRaster().getDataBuffer())).getData();

        grainMin = 1f / (Math.max(W, H) * superSampling);

    }


    private void render(int depth) {



        final int d = depth - 1;

        render(d, 0, 0, W, H);


    }

    double renderRecurse(float alpha, int depth, float x1, float y1, float x2, float y2) {
        if (depth == 0) {
            return render(alpha, x1, y1, x2, y2);
        } else {
            float mx = (x2 - x1) / 2f + x1;
            float my = (y2 - y1) / 2f + y1;
            depth--;
            return renderRecurse(alpha, depth, x1, y1, mx, my) +
                   renderRecurse(alpha, depth, mx, y1, x2, my) +
                   renderRecurse(alpha, depth, x1, my, mx, y2) +
                   renderRecurse(alpha, depth, mx, my, x2, y2);
        }
    }

    /** returns sum of pixel errors */
    private double render(float alpha, float fx1, float fy1, float fx2, float fy2) {
        int x1 = Math.round(fx1); int y1 = Math.round(fy1); int x2 = Math.round(fx2); int y2 = Math.round(fy2);
        if (x2 <= x1 || y2 <= y1)
            return 0;

        double mx = (fx1 + fx2)/2f+ (0.5f + (random.nextFloat()-0.5f) * subPixelScatter*(fx2-fx1));
        double my = (fy1 + fy2)/2f+ (0.5f + (random.nextFloat()-0.5f) * subPixelScatter*(fy2-fy1));

        int color = color(mx, my);


        int ww = x2 - x1;
        int W = this.W;
        int[] pixelCache = this.pixelCache;

        long delta = 0;
        for (int y = y1; y < y2; y++) {
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
        }
        return delta / (3.0 * 256);
    }

    private int blend(int current, int next, float alpha) {
        if (alpha >= 1 - (1/256f))
            return next;

        //TODO
        float cr = Bitmap2D.decode8bRed(current), nr = Bitmap2D.decode8bRed(next);
        float cg = Bitmap2D.decode8bGreen(current), ng = Bitmap2D.decode8bGreen(next);
        float cb = Bitmap2D.decode8bBlue(current), nb = Bitmap2D.decode8bBlue(next);
        return Bitmap2D.encodeRGB8b(
                Util.lerp(alpha, cr, nr),
                Util.lerp(alpha, cg, ng),
                Util.lerp(alpha, cb, nb)
        );
    }


    /** in 8-bit color components, ie. white -> black = 3 * 256 difference */
    int colorDelta(int a, int b) {
        if (a == b) return 0;
        return
            Math.abs( Bitmap2D.iDecode8bRed(a) - Bitmap2D.iDecode8bRed(b)) +
            Math.abs( Bitmap2D.iDecode8bGreen(a) - Bitmap2D.iDecode8bGreen(b)) +
            Math.abs( Bitmap2D.iDecode8bBlue(a) - Bitmap2D.iDecode8bBlue(b));
    }


    private int color(double x, double y) {
        Ray3 ray = scene.camera.ray(
                (x) / W,
                1 - (y) / H,
                A
        );
        return scene.rayColor(ray);
    }

    public static final class Light {
        public final vv3 position;
        public final int color;

        public Light(vv3 position, int color) {
            this.position = position;
            this.color = color;
        }
    }
}
