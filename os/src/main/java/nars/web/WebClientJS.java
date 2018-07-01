package nars.web;

import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;

import java.io.File;
import java.util.Random;

/** see: https://github.com/automenta/spimedb/commit/93cc982f6d31cacb9c5d23e29f93d54ac5b9c1a8 */
public class WebClientJS {

    public static void generate() {
        try {
            TeaVMTool tea = new TeaVMTool();
            tea.setMainClass(WebClientJS.class.getName());
            tea.setCacheDirectory(new File("/tmp/tea"));
            tea.setTargetDirectory(new File("/tmp/tea"));

            tea.generate();
        } catch (TeaVMToolException e) {
            e.printStackTrace();
        }
    }

    private HTMLDocument document = HTMLDocument.current();
    private HTMLCanvasElement canvas;
    private CanvasRenderingContext2D graphics;
    private boolean[][] cells;
    private int width;
    private int height;
    private int delay;

    public WebClientJS(int width, int height, int delay) {
        this.width = width;
        this.height = height;
        this.delay = delay;

        canvas = document.createElement("canvas").cast();
        canvas.setWidth(width);
        canvas.setHeight(height);;
        document.getBody().appendChild(canvas);

        graphics = canvas.getContext("2d").cast();

        cells = new boolean[height][width];
    }

    private void start() throws InterruptedException {
        fill();
        display();
        loop();
    }

    private void fill() {
        Random random = new Random();
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                cells[i][j] = random.nextInt(100) >= 50;
            }
        }
    }

    private void loop() throws InterruptedException {
        long time = System.currentTimeMillis();
        while (true) {
            next();
            display();
            time += delay;
            Thread.sleep(Math.max(0, time - System.currentTimeMillis()));
        }
    }

    private void next() {
        boolean[][] nextCells = new boolean[height][width];
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int left = j > 0 ? j - 1 : width - 1;
                int right = j + 1 < width ? j + 1 : 0;
                int top = i > 0 ? i - 1 : height - 1;
                int bottom = i + 1 < height ? i + 1 : 0;

                int neighbours = 0;
                if (cells[top][left]) {
                    neighbours++;
                }
                if (cells[top][j]) {
                    neighbours++;
                }
                if (cells[top][right]) {
                    neighbours++;
                }
                if (cells[i][right]) {
                    neighbours++;
                }
                if (cells[bottom][right]) {
                    neighbours++;
                }
                if (cells[bottom][j]) {
                    neighbours++;
                }
                if (cells[bottom][left]) {
                    neighbours++;
                }
                if (cells[i][left]) {
                    neighbours++;
                }

                if (!cells[i][j]) {
                    if (neighbours == 3) {
                        nextCells[i][j] = true;
                    }
                } else {
                    if (neighbours == 2 || neighbours == 3) {
                        nextCells[i][j] = true;
                    }
                }
            }
        }

        cells = nextCells;
    }

    private void display() {
        ImageData image = graphics.createImageData(width, height);
        int offset = 0;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int value = cells[i][j] ? 0 : 255;
                image.getData().set(offset++, value);
                image.getData().set(offset++, value);
                image.getData().set(offset++, value);
                image.getData().set(offset++, 255);
            }
        }
        graphics.putImageData(image, 0, 0);
    }

    public static void main(String[] args) throws InterruptedException {
        new WebClientJS(300, 300, 200).start();
    }
}
