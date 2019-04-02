package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.tree.rtree.rect.RectFloat;
import org.slf4j.Logger;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.util.math.Color4f;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * renders a matrix of characters to a texture
 */
public abstract class BitmapTextGrid extends AbstractConsoleSurface {

    private static final Logger logger = Util.logger(BitmapTextGrid.class);

    private final Tex tex = new Tex().mipmap(true);
    final AtomicBoolean invalid = new AtomicBoolean(true);
    private BufferedImage backbuffer = null;
    private Font font;
    private Graphics2D backbufferGraphics;
    private final Color cursorColor = new Color(255, 200, 0, 127);

    private final boolean antialias = true;
    private final boolean quality = false;

    /** pixel scale of each rendered character bitmap */
    static final int DEFAULT_FONT_SCALE = 16;

    protected int cursorCol, cursorRow;
    protected int fontWidth, fontHeight;

    private float alpha = 1f;
    private boolean fillTextBackground = false;

    private static final Font defaultFont;
    static {
        Font f;
        try (InputStream in = BitmapTextGrid.class.getClassLoader()
                .getResourceAsStream("font/CourierPrimeCode.ttf")) {

            ByteArrayInputStream in2 = new ByteArrayInputStream(in.readAllBytes()); //cache completely

            f = Font.createFont(Font.TRUETYPE_FONT, in2);

        } catch (Exception e) {

            f = new Font("monospace", 0, 1);

        }
        logger.info("bitmap font={}", f);

        defaultFont = f;
    }

    protected BitmapTextGrid() {
        font(defaultFont);
        fontSize(DEFAULT_FONT_SCALE);
    }

//    protected BitmapTextGrid(int cols, int rows) {
//        resize(cols, rows);
//    }

    private boolean ensureBufferSize() {

        if (this.cols == 0 || this.rows == 0)
            return false;

        if (this.backbuffer != null && this.backbuffer.getWidth() == this.pixelWidth() && this.backbuffer.getHeight() == this.pixelHeight())
            return true;


        

        BufferedImage newBackbuffer = new BufferedImage(pixelWidth(), pixelHeight(), BufferedImage.TYPE_INT_ARGB);

        newBackbuffer.setAccelerationPriority(1f);

        Graphics2D backbufferGraphics = newBackbuffer.createGraphics();

        if (antialias) {
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        } else {
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        }

        backbufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                quality ? RenderingHints.VALUE_RENDER_QUALITY : RenderingHints.VALUE_RENDER_SPEED);

//        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f);
//        backbufferGraphics.setComposite(composite);
//        backbufferGraphics.clearRect(0,0,pixelWidth(), pixelHeight());

        this.backbufferGraphics = backbufferGraphics;
        this.backbuffer = newBackbuffer;

        clearBackground();

        //backbufferGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        //backbufferGraphics.setBackground(new Color(0,0,0,0.5f));

        //backbufferGraphics.setColor(new Color(0,0,0,0.5f));
        //backbufferGraphics.fillRect(0,0,pixelWidth(), pixelHeight());
        //backbufferGraphics.drawImage(this.backbuffer, 0, 0, null);

        backbufferGraphics.setFont(font);




        return true;
    }

    protected void clearBackground() {
        Arrays.fill(intData(), 0);
    }

    private int[] intData() {
        return ((DataBufferInt)backbuffer.getRaster().getDataBuffer()).getData();
    }

    public void setFillTextBackground(boolean fillTextBackground) {
        this.fillTextBackground = fillTextBackground;
        
    }

    public BitmapTextGrid alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    protected final void paintIt(GL2 gl, ReSurface r) {
        if (invalid.compareAndSet(true, false)) {
            if (ensureBufferSize()) {
                renderText();
                if (!tex.set(backbuffer)) {
                    invalid.set(true); //try again
                }
            }
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
            font(Font.createFont(Font.TRUETYPE_FONT, i).deriveFont(font.getStyle(), font.getSize()));
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

    public synchronized BitmapTextGrid font(Font f) {

        if (!f.equals(this.font)) {
            this.font = f;

            FontRenderContext ctx = this.getFontRenderContext();
            Rectangle2D b = font.getStringBounds("X", ctx);
            this.fontWidth = (int) Math.ceil((float) b.getWidth());
            this.fontHeight = (int) Math.ceil((float) b.getHeight());
            //TODO b.getCenterY()

            if (backbufferGraphics != null)
                backbufferGraphics.setFont(font);

            //this.charAspect = ((float)fontHeight) / fontWidth;
            //layout();
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

    private FontRenderContext getFontRenderContext() {
        return new FontRenderContext(null,
                antialias ?
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                quality ?
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON :
                    RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }


    private int pixelWidth() {
        return fontWidth * cols;
    }

    int pixelHeight() {
        return fontHeight * rows;
    }

    /** render text to texture, invokes redraw method appropriately */
    protected abstract boolean renderText();


    @Deprecated void redraw(VirtualTerminal.BufferLine bufferLine, int column, int row) {
        redraw(bufferLine.getCharacterAt(column), column, row);
    }

    @Deprecated public void redraw(TextCharacter character, int columnIndex, int rowIndex) {
        Color backgroundColor = character.getBackgroundColor().toColor();
        Color foregroundColor = character.getForegroundColor().toColor();
        redraw(character.getCharacter(), columnIndex, rowIndex, foregroundColor, backgroundColor,
                character.isUnderlined(),
                character.isCrossedOut()
        );
    }

    public void redraw(char c, int columnIndex, int rowIndex, Color3f foregroundColor, Color3f backgroundColor) {
        redraw(c, columnIndex, rowIndex, foregroundColor.toAWT(), backgroundColor.toAWT(), false, false);
    }

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

        if (fillTextBackground) {
            g.setColor(backgroundColor);
            g.fillRect(x, y, characterWidth, fontHeight);
        }

        g.setColor(foregroundColor);

        final int descent = font.getSize()/4; //estimate
        if (c != ' ')
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

    public void invalidate() {
        invalid.set(true);
    }
}
