package spacegraph.space3d.raytrace;

import jcog.Util;
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
    public static final int MAX_GRAIN = 8;

    private static int width = 640;
    private static int height = 480;
    double fps = 30;

    /** update rate (not alpha chanel) */
    float alpha = 0.33f;

    double CAMERA_EPSILON = 0.001;

    private float subPixelRayNoise = 0.25f;

    private Scene scene;
    private Input input;
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

//        int pause = 0;
        while (true) {
            tracer.render();

        }
    }

    private static RayTracer raytracer() {
        Input input;
        RayTracer rayTracer;

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.setSize(width, height);

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
        vv3 cameraPos = scene.camera.position.clone(), cameraDir = scene.camera.direction.clone();
        scene.camera.rotate(input.getDeltaMouseX() / 2.0, -input.getDeltaMouseY() / 2.0);
        scene.camera.move(input.getKeyboardVector());

        return !cameraPos.equals(scene.camera.position, CAMERA_EPSILON) || !cameraDir.equals(scene.camera.direction, CAMERA_EPSILON);
    }

    float grain = MAX_GRAIN;

    private void render() {

        long sceneTimeNS = Math.round((1.0/fps)*1E9);

        if (!input.newSize.equals(size)) {
            size = input.newSize;
            resize();
        }

        reset();

        long timeUsedNS = 0;

        if (updateCamera()) {

//                while (!tracer.interrupted) {
//                    Util.pauseSpin(pause++);
//                }
//                pause = 0;

            grain = MAX_GRAIN;
        }

        boolean moving;
        long start = System.nanoTime();



        do {


            double eAvg = renderStochastic( alpha, Math.round(grain), 800);
//            System.out.println("eAvg = " + eAvg);

            if (grain > 1) {
                grain = (float) (grain * Util.lerp(eAvg, 0.997f, 0.9999f));
            } else
                grain = 1;

        } while (System.nanoTime() - start <= sceneTimeNS);

        repaint();

//        System.out.println(Texts.timeStr(timeUsedNS) );
//        boolean renderComplete = (pixelsRemain == 0);
    }

    final Random random = new XoRoShiRo128PlusRandom(1);

    private double renderStochastic(float alpha, int superPixelSize, int iter) {

        int W = this.W - superPixelSize;
        int H = this.H - superPixelSize;

        double eTotal = 0;
        for (int i = 0; i < iter; i++) {

            int xy = random.nextInt() & ~(1 << 31);

            int x = (xy & Short.MAX_VALUE) % W;
            int y = (xy >> 16) % H;

            float e = render(alpha, x, y, x + superPixelSize, y+superPixelSize);
            eTotal += e;
        }
        double eAvg = eTotal / (iter * (superPixelSize*superPixelSize));
        return eAvg;
    }


    private void resize() {
        BufferedImage newImage = new BufferedImage((int) size.getWidth(), (int) size.getHeight(), BufferedImage.TYPE_INT_RGB);
        newImage.setAccelerationPriority(1f);


        image = newImage;
        pixelCache = ((DataBufferInt) (newImage.getRaster().getDataBuffer())).getData();

        grain = MAX_GRAIN;

        reset();
    }


    private void render(int depth) {



        final int d = depth - 1;

        render(d, 0, 0, W, H);


    }

    private void reset() {
        W = image.getWidth();
        H = image.getHeight();
        A = size.getWidth() / size.getHeight();
    }

//    @Deprecated private void render(int depth, int x1, int y1, int x2, int y2) {
//        if (depth == 0) {
//            render(x1, y1, x2, y2);
//        } else {
//            int mx = Math.round((x2 - x1) / 2f + x1);
//            int my = Math.round((y2 - y1) / 2f + y1);
//            depth--;
//            render(depth, x1, y1, mx, my);
//            render(depth, mx, y1, x2, my);
//            render(depth, x1, my, mx, y2);
//            render(depth, mx, my, x2, y2);
//
//        }
//    }

    /** returns sum of pixel errors */
    private float render(float alpha, int x1, int y1, int x2, int y2) {
        if (x2 <= x1 || y2 <= y1)
            return 0;

        double mx = (x2 - x1) / 2.0 + x1 + ((random.nextFloat()-0.5f)*2 * subPixelRayNoise);
        double my = (y2 - y1) / 2.0 + y1 + ((random.nextFloat()-0.5f)*2 * subPixelRayNoise);

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
        return delta / (3.0f * 256);
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
                (x + 0.5) / W,
                1 - (y + 0.5) / H,
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
