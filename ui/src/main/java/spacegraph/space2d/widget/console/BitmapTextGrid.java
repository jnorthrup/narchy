package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
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

    private final Tex tex = new Tex();
    final AtomicBoolean needUpdate = new AtomicBoolean(true);
    private BufferedImage backbuffer = null;
    private Font font;
    private Graphics2D backbufferGraphics;
    private Color cursorColor = new Color(255, 200, 0, 127);

    private boolean antialias = true;
    private boolean quality = true;

    protected int cursorCol, cursorRow;
    private int fontWidth;
    protected int fontHeight;
    //private float charAspect;

    private float alpha = 1f;
    private boolean fillTextBackground = true;

    private static final Font defaultFont;
    static {
        Font f;
        try (InputStream in = BitmapTextGrid.class.getClassLoader().getResourceAsStream("font/CourierPrimeCode.ttf")) {

            f = Font.createFont(Font.TRUETYPE_FONT, in);


        } catch (Exception e) {

            f = new Font("monospace", 0, 28);

        }

        defaultFont = f;
    }

    protected BitmapTextGrid() {

        font(defaultFont);
        fontSize(28);


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
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        }
        if (quality) {
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

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
        Arrays.fill(((DataBufferInt)backbuffer.getRaster().getDataBuffer()).getData(), 0);
    }


    public void setFillTextBackground(boolean fillTextBackground) {
        this.fillTextBackground = fillTextBackground;
        
    }

    public BitmapTextGrid alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    protected void paintIt(GL2 gl) {
        if (needUpdate.compareAndSet(true, false)) {
            if (ensureBufferSize()) {
                updateBackBuffer();
                if (!tex.update(backbuffer)) {
                    needUpdate.set(true); //try again
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
        return new FontRenderContext(null, antialias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
    }


    private int pixelWidth() {
        return fontWidth * cols;
    }

    int pixelHeight() {
        return fontHeight * rows;
    }

    protected abstract boolean updateBackBuffer();


    void redraw(VirtualTerminal.BufferLine bufferLine, int column, int row) {
        redraw(bufferLine.getCharacterAt(column), column, row);
    }

    public void redraw(TextCharacter textCharacter, int column, int row) {
        redraw(backbufferGraphics, textCharacter, column, row,
                fontWidth, fontHeight, fontWidth
        );
    }

    private void redraw(Graphics g, TextCharacter character, int columnIndex, int rowIndex, int fontWidth, int fontHeight, int characterWidth) {
        if (g == null)
            return;

        int x = columnIndex * fontWidth;
        int y = rowIndex * fontHeight;

        if (fillTextBackground) {
            Color backgroundColor = character.getBackgroundColor().toColor();
            g.setColor(backgroundColor);
            
            g.fillRect(x, y, characterWidth, fontHeight);
        } else {
        }

        Color foregroundColor = character.getForegroundColor().toColor();
        g.setColor(foregroundColor);

        
        
        final int descent = 8;
        char c = character.getCharacter();
        if (c != ' ')
            g.drawChars(new char[]{c}, 0, 1, x, y + fontHeight + 1 - descent);


        if (character.isCrossedOut()) {
            int lineStartY = y + fontHeight / 2;
            int lineEndX = x + characterWidth;
            g.drawLine(x, lineStartY, lineEndX, lineStartY);
        }

        if (character.isUnderlined()) {
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

    protected void invalidate() {
        needUpdate.set(true);
    }
}
