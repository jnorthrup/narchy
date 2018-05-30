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
            System.out.println(e);
            System.exit(1);
        }
        rayTracer = new RayTracer(scene, new Dimension(width, height), input);
        
        
        input.addInterruptable(rayTracer::interrupt);

        frame.add(rayTracer);
        frame.setVisible(true);

        
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








        image = newImage;
        pixelCache = ((DataBufferInt)(newImage.getRaster().getDataBuffer())).getData();
        reset();
    }


    private void render(int depth) {
        W = image.getWidth();
        H = image.getHeight();
        A = size.getWidth() / size.getHeight();



        final int d = depth - 1;

        renderDepth(d, 0, 0, W, H);































































    }

    private void reset() {
        pixelsLeft = (image.getWidth() * image.getHeight());
    }

    private void renderDepth(int depth, int x1, int y1, int x2, int y2) {
        
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


        
            int color = getRenderedColor(mx, my);

            pixelsLeft--;

        
        int ww = x2-x1;
        for (int y = y1; y < y2; y++) {
            int start = y * W + x1;
            Arrays.fill(pixelCache, start, start+ww, color);
        }


    }

    
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
