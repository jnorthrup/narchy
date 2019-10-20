package nars.experiment.minicraft.side.awtgraphics;

import nars.experiment.minicraft.side.Color;
import nars.experiment.minicraft.side.*;
import nars.experiment.minicraft.side.SpriteStore.AwtSprite;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

public class AwtGraphicsHandler extends GraphicsHandler {
    public final Canvas canvas = new Canvas();

    public JFrame container;
    private Cursor myCursor;
    public JPanel panel;
    public BufferedImage buffer;

    @Override
    public void init(SideScrollMinicraft game) {


        container = new JFrame("Minicraft");

        try {
            var ii = new ImageIcon(new URL("file:sprites/other/mouse.png"));
            var im = ii.getImage();
            var tk = canvas.getToolkit();
            myCursor = tk.createCustomCursor(im, new Point(0, 0), "MyCursor");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("myCursor creation failed " + e);
        }


        panel = (JPanel) container.getContentPane();
        panel.setPreferredSize(new Dimension(screenWidth, screenHeight));
        panel.setLayout(null);
        panel.setCursor(myCursor);
        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                var d = e.getComponent().getSize();
                canvas.setBounds(0, 0, d.width, d.height);
                screenWidth = d.width;
                screenHeight = d.height;
            }
        });


        canvas.setBounds(0, 0, screenWidth + 10, screenHeight + 10);
        panel.add(canvas);


        canvas.setIgnoreRepaint(true);


        container.pack();
        container.setResizable(true);
        container.setVisible(true);


        container.addWindowListener(new MyWindowAdapter(game));


        canvas.requestFocus();


        buffer = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        g = (Graphics2D) buffer.getGraphics();
    }

    Graphics2D g;

    @Override
    public void startDrawing() {


        g = (Graphics2D) buffer.getGraphics();
        g.setColor(java.awt.Color.black);
        g.fillRect(0, 0, screenWidth, screenHeight);

    }

    @Override
    public void finishDrawing() {
        g.dispose();


        var cg = canvas.getGraphics();
        cg.drawImage(buffer, 0, 0, null);
        cg.dispose();


    }

    @Override
    public void setColor(Color color) {

        g.setColor(new java.awt.Color(color.R, color.G, color.B, color.A));
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        g.fillRect(x, y, width, height);
    }

    @Override
    public void drawString(String string, int x, int y) {
        g.drawString(string, x, y);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        g.fillOval(x, y, width, height);
    }

    @Override
    public void drawImage(Sprite sprite, int x, int y) {

        var awtSprite = (AwtSprite) sprite;
        if (awtSprite.image == null) {
            var other = (AwtSprite) SpriteStore.get().loadSprite(awtSprite.ref);
            awtSprite.image = other.image;
        }
        g.drawImage(awtSprite.image, x, y, null);
    }

    @Override
    public void drawImage(Sprite sprite, int x, int y, Color tint) {
        var width = sprite.getWidth();
        var height = sprite.getHeight();
        drawImage(sprite, x, y, width, height, tint);
    }

    @Override
    public void drawImage(Sprite sprite, int x, int y, int width, int height) {
        var awtSprite = (AwtSprite) sprite;
        if (awtSprite.image == null) {
            var other = (AwtSprite) SpriteStore.get().loadSprite(awtSprite.ref);
            awtSprite.image = other.image;
        }
        g.drawImage(awtSprite.image, x, y, width, height, null);
    }

    @Override
    public void drawImage(Sprite sprite, int x, int y, int width, int height,
                          Color tint) {
        drawImage(sprite, x, y, width, height);
        var old = g.getColor();
        this.setColor(tint);
        this.fillRect(x, y, width, height);
        g.setColor(old);
    }

    private static class MyWindowAdapter extends WindowAdapter {
        private final SideScrollMinicraft game;

        public MyWindowAdapter(SideScrollMinicraft game) {
            this.game = game;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            game.goToMainMenu();
            SideScrollMinicraft.quit();
        }
    }
}
