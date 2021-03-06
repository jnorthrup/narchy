package spacegraph.space2d.widget.console;


import com.jogamp.opengl.GL2;
import jcog.Log;
import jcog.tree.rtree.rect.RectFloat;
import org.slf4j.Logger;
import spacegraph.space2d.ReSurface;
import spacegraph.util.math.Color4f;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * renders a matrix of characters to a texture
 */
public abstract class BitmapTextGrid extends AbstractConsoleSurface {

    /**
     * pixel scale of each rendered character bitmap
     */
    static final int DEFAULT_FONT_SCALE = 32;
    private static final Logger logger = Log.logger(BitmapTextGrid.class);
    private static volatile Font defaultFont;
    final AtomicBoolean invalidBmp = new AtomicBoolean(false);
    final AtomicBoolean invalidTex = new AtomicBoolean(false);
    private final Tex tex = new Tex().mipmap(true);
    @Deprecated
    private final Color cursorColor = new Color(255, 200, 0, 127);
    protected int cursorCol;
    protected int cursorRow;
    protected int fontWidth;
    protected int fontHeight;
    private BufferedImage backbuffer = null;
    private Font font;
    private Graphics2D backbufferGraphics;
    private float alpha = 1f;

    protected BitmapTextGrid() {
        font(defaultFont());
        fontSize((float) DEFAULT_FONT_SCALE);
    }

    @Override
    public boolean delete() {
        tex.delete();
        return super.delete();
    }

    private static Font defaultFont() {
        if (defaultFont == null) {
            synchronized (BitmapTextGrid.class) {
                if (defaultFont == null) {
                    Font f;
                    try (InputStream in = BitmapTextGrid.class.getClassLoader()
                            .getResourceAsStream(
                                    "font/CourierPrimeCode.ttf"
                                    //"font/ObliviousFont.ttf"
                                    //"font/plasmati.ttf"
                            )) {

                        //in = new ByteArrayInputStream(in.readAllBytes()); //cache completely?

                        f = Font.createFont(Font.TRUETYPE_FONT, in);

                    } catch (Exception e) {

                        f = new Font("monospace", Font.PLAIN, 1);

                    }
                    defaultFont = f;
                    logger.info("bitmap font={}", f);
                }
            }
        }
        return defaultFont;
    }

//    protected BitmapTextGrid(int cols, int rows) {
//        resize(cols, rows);
//    }

    private void allocate() {

        BufferedImage bPrev = this.backbuffer;
        int pw = pixelWidth();
        int ph = pixelHeight();
        if (bPrev != null) {
            if (bPrev.getWidth() == pw && bPrev.getHeight() == ph) return;
        }

        BufferedImage b = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);

        b.setAccelerationPriority(1f);

        Graphics2D next = b.createGraphics();
//        System.out.println(cols + "," + rows + "\t" + b + "\t" + g);

        boolean antialias = false;
        if (antialias) {
            next.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
        } else {
            next.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
            );

        }
        boolean quality = false;
        if (quality)
            next.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
                    //RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT
                    //RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
            );
        else
            next.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
            );

//        next.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, quality ?
//            RenderingHints.VALUE_FRACTIONALMETRICS_ON :
//            RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        next.setRenderingHint(RenderingHints.KEY_RENDERING, quality ?
            RenderingHints.VALUE_RENDER_QUALITY :
            RenderingHints.VALUE_RENDER_SPEED
//          RenderingHints.VALUE_RENDER_DEFAULT
        );


        Graphics2D prev = this.backbufferGraphics;
        if (prev != null)
            prev.dispose();

        this.backbufferGraphics = next;
        this.backbuffer = b;

        next.setFont(font);

    }

    protected void clearBackground() {
        Arrays.fill(intData(), 0);
    }

    private int[] intData() {
        return ((DataBufferInt) backbuffer.getRaster().getDataBuffer()).getData();
    }

    public BitmapTextGrid alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    protected void renderContent(ReSurface r) {

        if (this.cols == 0 || this.rows == 0)
            return;

        if (invalidBmp.weakCompareAndSetAcquire(true, false)) {
            //synchronized (this) {
                allocate();
                renderText();
            //}
            invalidTex.setRelease(true);
        }
        //super.renderContent(r);
    }

    @Override
    protected final void paintIt(GL2 gl, ReSurface r) {
        if (invalidTex.weakCompareAndSetAcquire(true, false)) {
//            try {

                if (tex.gl != null) {
                    if (!tex.set(backbuffer, gl))
                        invalidTex.setRelease(true); //try again later
                } else
                    invalidTex.setRelease(true); //try again

//            } catch (Throwable t) {
//                t.printStackTrace(); //HACK
//            }
        }
        tex.paint(gl, textBounds(), alpha);
    }

    protected RectFloat textBounds() {
        return bounds;
    }

    public BitmapTextGrid font(String fontName) {
        font(new Font(fontName, font.getStyle(), font.getSize()));
        return this;
    }

    public BitmapTextGrid font(InputStream i) {
        try {
            font(Font.createFont(Font.TRUETYPE_FONT, i).deriveFont(font.getStyle(), (float) font.getSize()));
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public BitmapTextGrid fontStyle(int style) {
        font(this.font.deriveFont(style));
        return this;
    }

    public BitmapTextGrid fontSize(float s) {
        return font(this.font.deriveFont(s));
    }

    public BitmapTextGrid font(Font f) {

        if (!f.equals(this.font)) {

            this.font = f;

            //TODO do this once per font
//            FontRenderContext ctx = this.getFontRenderContext();
//            Rectangle2D b = f.getStringBounds("X", ctx);
//            this.fontWidth = (int) Math.ceil((float) b.getWidth());
//            this.fontHeight = (int) Math.ceil((float) b.getHeight());
            this.fontWidth = (int) Math.ceil((double) (font.getSize() / 1.5f));
            this.fontHeight = font.getSize();


            if (backbufferGraphics != null)
                backbufferGraphics.setFont(font);

            //this.charAspect = ((float)fontHeight) / fontWidth;
            //layout();
            invalidate();
        }
        return this;
    }


//    @Override
//    public void doLayout(int dtMS) {
//        float va = h()/w();
//        int r, c;
//        if (va <= charAspect) {
//            r = scale;
//            c = (int) Math.floor(r / va * charAspect);
//        } else {
//            c = Math.round(scale * charAspect);
//            r = Math.round(c * va / charAspect);
//        }
//        r = Math.max(1, r);
//        c = Math.max(2, c);
//        resize(c, r);
//    }

//    private FontRenderContext getFontRenderContext() {
//        return new FontRenderContext(null,
//                antialias ?
//                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
//                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
// );
//    }


    private int pixelWidth() {
        return fontWidth * cols;
    }

    int pixelHeight() {
        return fontHeight * rows;
    }

    /**
     * render text to texture, invokes redraw method appropriately
     */
    protected abstract boolean renderText();




//    public void redraw(char c, int columnIndex, int rowIndex, Color3f foregroundColor, Color3f backgroundColor) {
//        redraw(c, columnIndex, rowIndex, foregroundColor.toAWT(), backgroundColor.toAWT(), false, false);
//    }

    public void redraw(char c, int columnIndex, int rowIndex, Color4f foregroundColor, Color4f backgroundColor) {
        redraw(c, columnIndex, rowIndex, foregroundColor.toAWT(), backgroundColor.toAWT(), false, false);
    }

    public void redraw(char c, int columnIndex, int rowIndex, Color foregroundColor, Color backgroundColor) {
        redraw(c, columnIndex, rowIndex, foregroundColor, backgroundColor, false, false);
    }

    public void redraw(char c, int columnIndex, int rowIndex, Color foregroundColor, Color backgroundColor, boolean underlined, boolean crossedOut) {
        redraw(backbufferGraphics, c, backgroundColor, foregroundColor, underlined, crossedOut, columnIndex, rowIndex, fontWidth, fontHeight, fontWidth);
    }

    private void redraw(Graphics g, char c, Color backgroundColor, Color foregroundColor, boolean underlined, boolean crossedOut, int columnIndex, int rowIndex, int fontWidth, int fontHeight, int characterWidth) {
        if (g == null)
            return;

        int x = columnIndex * fontWidth;
        int y = rowIndex * fontHeight;

        if (backgroundColor.getAlpha()>0) {
            g.setColor(backgroundColor);
            g.fillRect(x, y, characterWidth, fontHeight);
        }

        g.setColor(foregroundColor);

        int descent = font.getSize() / 4; //estimate
        if ((int) c != (int) ' ')
            g.drawChars(new char[]{c}, 0, 1, x, y + fontHeight + 1 - descent);

        if (crossedOut) {
            int lineStartY = y + fontHeight / 2;
            int lineEndX = x + characterWidth;
            g.drawLine(x, lineStartY, lineEndX, lineStartY);
        }

        if (underlined) {
            int lineStartY = y + fontHeight - descent + 1;
            int lineEndX = x + characterWidth;
            g.drawLine(x, lineStartY, lineEndX, lineStartY);
        }

        boolean drawCursor = (columnIndex == cursorCol) && (rowIndex == cursorRow);
        if (drawCursor) {
            g.setColor(cursorColor == null ? foregroundColor : cursorColor);
            g.fillRect(x, y + 1, characterWidth, fontHeight - 2);
        }
    }

    public final void invalidate() {
        invalidBmp.set(true);
    }
}
