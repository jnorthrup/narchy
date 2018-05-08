package spacegraph.slam.raytrace;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public final class RayTracer extends JPanel {
    public static int width = 640;
    public static int height = 480;


    public static void main(String[] args) {
        RayTracer rayTracer = raytracer();
        // Main loop.
        while (true) {
            rayTracer.update();
            if (rayTracer.renderProgressively()) {
                rayTracer.input.waitForInput();
            }
        }
    }

    public static RayTracer raytracer() {
        Input input;
        RayTracer rayTracer;

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.setSize(width, height);

        input = new Input();
        input.newSize = frame.getSize();

        // If there are any command-line arguments, use the first argument
        // as the world definition file. Otherwise default to "default_scene.txt".
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

//            String simple= "camera:\n" +
//                    "    position: (4, 4, 4)\n" +
//                    "    direction: (-1, -1, -1)\n" +
//                    "    fov: 90\n" +
//                    "    size: 0\n" +
//                    "cube:\n" +
//                    "    position: (0, 0, 0)\n" +
//                    "    sideLength: 2\n" +
//                    "    surface: diffuse\n" +
//                    "light:\n" +
//                    "    position: (0, -2, 4)\n" +
//                    "    color: ffffff\n";

            scene = new Scene(
                    defaults
                    //simple
            );
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
        rayTracer = new RayTracer(scene, new Dimension(width, height), input);
        // Add the ray tracer to the list of Interruptable objects that are
        // interrupted on input.
        input.addInterruptable(rayTracer::interrupt);

        frame.add(rayTracer);
        frame.setVisible(true);

        // Set up keyboard, mouse, and window resizing listeners.
        frame.addKeyListener(input);
        frame.addMouseListener(input);
        frame.addMouseMotionListener(input);
        frame.addComponentListener(input);



        return rayTracer;
    }


    public Scene scene;
    public Input input;

    public BufferedImage image;
    public int[] pixelCache;
    private int pixelsLeft;

    private boolean progressiveAbort;

    private Dimension size;
    private int W, H;
    private double A;

    public RayTracer(Scene scene, Dimension size, Input input) {
        setIgnoreRepaint(true);
        this.scene = scene;
        this.input = input;
        size = input.newSize;
        pixelsLeft = 0;
        setSize(size);
    }

    public void interrupt() {
        progressiveAbort = true;
    }

    @Override
    public void paint(Graphics g) {
        //super.paint(g);
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
    }

    public void update() {
        if (!input.newSize.equals(size)) {
            size = input.newSize;
            updateSize();
        }
        scene.camera.rotate(input.getDeltaMouseX() / 2.0, -input.getDeltaMouseY() / 2.0);
        scene.camera.move(input.getKeyboardVector());


    }

    // Returns true if it rendered completely.
    // Returns false if rendering was aborted.
    public boolean renderProgressively() {
        double timeSpent = 0.0;
        double fps = 30;
        int depth = 7;
        progressiveAbort = false;
        boolean moving;
        do {
            long t1 = System.nanoTime();
            render(depth);
            repaint();
            long t2 = System.nanoTime();
            timeSpent += (t2 - t1) / 1.0e9;
            depth++;
            moving = input.moving();
        } while (pixelsLeft > 0 && (timeSpent <= 1 / fps && moving || !progressiveAbort && !moving));
        boolean renderComplete = (pixelsLeft == 0);
        reset();
        return renderComplete;
    }


    private void updateSize() {
        BufferedImage newImage = new BufferedImage((int)size.getWidth(), (int)size.getHeight(), BufferedImage.TYPE_INT_RGB);
        newImage.setAccelerationPriority(1f);

//        if (image != null) {
//            for (int y = 0; y < image.getHeight() && y < newImage.getHeight(); y++) {
//                for (int x = 0; x < image.getWidth() && x < newImage.getWidth(); x++) {
//                    newImage.setRGB(x, y, image.getRGB(x, y));
//                }
//            }
//        }
        image = newImage;
        pixelCache = ((DataBufferInt)(newImage.getRaster().getDataBuffer())).getData();
        reset();
    }


    private void render(int depth) {
        W = image.getWidth();
        H = image.getHeight();
        A = size.getWidth() / size.getHeight();
//        int mx = W / 2;
//
//        int my = H / 2;
        final int d = depth - 1;
//        Graphics2D g = image.createGraphics();
        renderDepth(d, 0, 0, W, H);
//        Thread[] threads = new Thread[]{
//            new Thread() {
//                @Override
//                public void run() {
//                    //Graphics2D g = image.createGraphics();
//                    renderDepth(d, 0, 0, mx, my, 0);
//                    //g.dispose();
//                }
//            },
//            new Thread() {
//                @Override
//                public void run() {
//                    //Graphics2D g = image.createGraphics();
//                    renderDepth(d, mx, 0, W, my, 1);
//                    //g.dispose();
//                }
//            },
//            new Thread() {
//                @Override
//                public void run() {
//                    //Graphics2D g = image.createGraphics();
//                    renderDepth(d, 0, my, mx, H, 2);
//                    //g.dispose();
//                }
//            },
//            new Thread() {
//                @Override
//                public void run() {
//                    //Graphics2D g = image.createGraphics();
//                    renderDepth(d, mx, my, W, H, 3);
//                    //g.dispose();
//                }
//            }
//        };
//        // Sort threads by how much work they were able to do last frame.
//        // This way quadrant threads who didn't get as much work done get
//        // to start first.
//        for (int i = threads.length - 1; i > 0; i--) {
//            for (int j = 0; j < i; j++) {
//                if (threadWork[j] > threadWork[j + 1]) {
//                    int tw = threadWork[j];
//                    threadWork[j] = threadWork[j + 1];
//                    threadWork[j + 1] = tw;
//                    Thread t = threads[j];
//                    threads[j] = threads[j + 1];
//                    threads[j + 1] = t;
//                }
//            }
//        }
//        for (int i = 0; i < threadWork.length; i++) {
//            threadWork[i] = 0;
//        }
//        for (Thread thread : threads) {
//            thread.start();
//        }
//        try {
//            for (Thread thread : threads) {
//                thread.join();
//            }
//        } catch(Exception e) {
//        } finally {
////            g.dispose();
//        }
    }

    private void reset() {
        pixelsLeft = (image.getWidth() * image.getHeight());
    }

    private void renderDepth(int depth, int x1, int y1, int x2, int y2) {
        // I can't remember why I did this, it's to prevent stuttering and tearing or something.
        if (depth > 5 && progressiveAbort) {
            return;
        }
        if (depth == 0) {
            renderRect(x1, y1, x2, y2);
            return;
        }
        int mx = (x2 - x1)/2 + x1;
        int my = (y2 - y1)/2 + y1;
        depth--;
        renderDepth(depth, x1, y1, mx, my);
        renderDepth(depth, mx, y1, x2, my);
        renderDepth(depth, x1, my, mx, y2);
        renderDepth(depth, mx, my, x2, y2);
        if (progressiveAbort) {
        }
    }

    private void renderRect(int x1, int y1, int x2, int y2) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        int mx = (x2 - x1)/2 + x1;
        int my = (y2 - y1)/2 + y1;
//        int offset = my * W + mx;
//        int color = pixelCache[offset];
        //if (color == -1) {
            int color = getRenderedColor(mx, my);
//            pixelCache[offset] = color;
            pixelsLeft--;
//            threadWork[thread]++;
        //}
        int ww = x2-x1;
        for (int y = y1; y < y2; y++) {
            int start = y * W + x1;
            Arrays.fill(pixelCache, start, start+ww, color);
        }
//        g.setColor(new Color(color));
//        g.fillRect(x1, y1, x2 - x1, y2 - y1);
    }

    // Cast a ray into the world from a given pixel location and calculate its resultant color.
    private int getRenderedColor(int x, int y) {
        Ray3 ray = scene.camera.getRay(
            (x + 0.5) / W,
            1 - (y + 0.5)/H,
                A
        );
        return scene.getRayColor(ray);
    }

    public static final class Light {
        public final Vector3 position;
        public final int color;

        public Light(Vector3 position, int color) {
            this.position = position;
            this.color = color;
        }
    }
}
