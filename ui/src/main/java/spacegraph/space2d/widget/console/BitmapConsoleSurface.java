package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * renders a matrix of characters to a texture
 */
public abstract class BitmapConsoleSurface extends AbstractConsoleSurface {

    private final Tex tex = new Tex();
    final AtomicBoolean needUpdate = new AtomicBoolean(true);
    private BufferedImage backbuffer;
    private Font font;
    private Graphics2D backbufferGraphics;
    private Color cursorColor = new Color(255, 200, 0, 127);
    private boolean antialias = true;
    private boolean quality = false;
    int cursorCol;
    int cursorRow;
    private int fontWidth;
    int fontHeight;
    private float charAspect;
    int scale = 16;
    private float alpha = 1f;
    private boolean fillTextBackground = true;


    BitmapConsoleSurface() {








        this.backbuffer = null;

        setBitmapFontSize(28);


    }
    private boolean ensureBufferSize() {

        if (this.cols == 0 || this.rows == 0)
            return false;

        if (this.backbuffer != null && this.backbuffer.getWidth() == this.pixelWidth() && this.backbuffer.getHeight() == this.pixelHeight())
            return true;


        

        BufferedImage newBackbuffer = new BufferedImage(pixelWidth(), pixelHeight(), 1);
        Graphics2D backbufferGraphics = newBackbuffer.createGraphics();
        
        
        backbufferGraphics.drawImage(this.backbuffer, 0, 0, null);

        backbufferGraphics.setFont(font);
        if (antialias) { 
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        if (quality) {
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        this.backbufferGraphics = backbufferGraphics;
        this.backbuffer = newBackbuffer;

        return true;
    }


    public void setFillTextBackground(boolean fillTextBackground) {
        this.fillTextBackground = fillTextBackground;
        
    }

    public BitmapConsoleSurface alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }
    private void render() {
        if (needUpdate.compareAndSet(true, false)) {
            if (ensureBufferSize()) {
                updateBackBuffer();
                tex.update(backbuffer);
            }
        }
    }


    @Override
    protected void paintIt(GL2 gl) {
        render();
        tex.paint(gl, bounds, alpha);
    }


    public void setBitmapFontSize(int s) {
        this.font = new Font("Monospaced", Font.PLAIN, s);

        FontRenderContext ctx = this.getFontRenderContext();
        Rectangle2D b = font.getStringBounds("W", ctx);
        this.fontWidth = (int) Math.ceil((float)b.getWidth());
        this.fontHeight = (int) Math.ceil((float)b.getHeight());
        this.charAspect = ((float)fontHeight) / fontWidth;
        layout();
    }






    @Override
    public void doLayout(int dtMS) {
        float va = h()/w(); 
        int r, c;
        if (va <= charAspect) {
            r = scale;
            c = (int) Math.floor(r / va * charAspect);
        } else {
            c = Math.round(scale * charAspect);
            r = Math.round(c * va / charAspect);
        }
        r = Math.max(1, r);
        c = Math.max(2, c);
        resize(c, r);
    }

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

    void redraw(TextCharacter textCharacter, int column, int row) {
        redraw(backbufferGraphics, textCharacter, column, row,
                fontWidth, fontHeight, fontWidth
        );
    }

    private void redraw(Graphics g, TextCharacter character, int columnIndex, int rowIndex, int fontWidth, int fontHeight, int characterWidth) {
        int x = columnIndex * fontWidth;
        int y = rowIndex * fontHeight;

        if (fillTextBackground) {
            Color backgroundColor = character.getBackgroundColor().toColor();
            g.setColor(backgroundColor);
            
            g.fillRect(x, y, characterWidth, fontHeight);
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

    public void setUpdateNecessary() {
        needUpdate.set(true);
    }
}
