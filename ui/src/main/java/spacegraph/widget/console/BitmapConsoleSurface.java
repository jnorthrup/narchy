package spacegraph.widget.console;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import spacegraph.render.Tex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * renders a matrix of characters to a texture
 */
public abstract class BitmapConsoleSurface extends AbstractConsoleSurface {

    protected final Tex tex = new Tex();
    protected final AtomicBoolean needUpdate = new AtomicBoolean(true);
    protected BufferedImage backbuffer;
    protected Font font;
    protected Graphics2D backbufferGraphics;
    Color cursorColor = Color.ORANGE;
    boolean antialias = true;
    boolean quality = false;
    protected int cursorCol;
    protected int cursorRow;
    protected int fontWidth;
    protected int fontHeight;
    protected float charAspect;
    int scale = 16;

    protected BitmapConsoleSurface() {


//        this.cursorIsVisible = true;
//        this.enableInput = false;
//        this.lastDrawnCursorPosition = null;
//        this.lastComponentHeight = 0;
//        this.lastComponentWidth = 0;
//        this.blinkOn = true;
        this.backbuffer = null;

        setBitmapFontSize(28);


    }
    private void ensureBufferSize() {

        if (this.backbuffer != null && this.backbuffer.getWidth() == this.pixelWidth() && this.backbuffer.getHeight() == this.pixelHeight()) {
            return;
        }

        //System.out.println(cols + " x " + pixelWidth() + " _ " + rows + " x " + pixelHeight());

        BufferedImage newBackbuffer = new BufferedImage(pixelWidth(), pixelHeight(), 1);
        Graphics2D backbufferGraphics = newBackbuffer.createGraphics();
        backbufferGraphics.fillRect(0, 0, newBackbuffer.getWidth(), newBackbuffer.getHeight());
        backbufferGraphics.drawImage(this.backbuffer, 0, 0, null);

        backbufferGraphics.setFont(font);
        if (antialias) { //if (this.isTextAntiAliased()) {
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        if (quality) {
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        this.backbufferGraphics = backbufferGraphics;
        this.backbuffer = newBackbuffer;

    }


    protected void render() {
        if (needUpdate.compareAndSet(true, false)) {
            ensureBufferSize();
            updateBackBuffer();

            tex.update(backbuffer);
        }
    }

    @Override
    public void paintIt(GL2 gl) {
        render();
        tex.paint(gl, bounds);
    }


    public void setBitmapFontSize(int s) {
        this.font = new Font("Monospaced", 0, s);

        FontRenderContext ctx = this.getFontRenderContext();
        Rectangle2D b = font.getStringBounds("W", ctx);
        this.fontWidth = (int) Math.ceil((float)b.getWidth());
        this.fontHeight = (int) Math.ceil((float)b.getHeight());
        this.charAspect = ((float)fontHeight) / fontWidth;
        layout();
    }


    @Override
    public void doLayout(int dtMS) {

        float va = h()/w(); //visual aspect ratio
        int r, c;
        if (va <= charAspect) {
            r = scale;
            c = (int) Math.floor(r / va * charAspect);
        } else {
            c = Math.round(scale * charAspect);
            r = Math.round(c * va / charAspect);
        }
        r = Math.max(3, r);
        c = Math.max(3, c);
        resize(c, r);
        super.doLayout(dtMS);
    }

    private FontRenderContext getFontRenderContext() {
        return new FontRenderContext(null, antialias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
    }


    protected int pixelWidth() {
        return fontWidth * cols;
    }

    protected int pixelHeight() {
        return fontHeight * rows;
    }

    protected abstract boolean updateBackBuffer();


    protected void redraw(VirtualTerminal.BufferLine bufferLine, int column, int row) {
        redraw(bufferLine.getCharacterAt(column), column, row);
    }

    protected void redraw(TextCharacter textCharacter, int column, int row) {
        redraw(backbufferGraphics, textCharacter, column, row,
                fontWidth, fontHeight, fontWidth
        );
    }

    protected void redraw(Graphics g, TextCharacter character, int columnIndex, int rowIndex, int fontWidth, int fontHeight, int characterWidth) {
        int x = columnIndex * fontWidth;
        int y = rowIndex * fontHeight;

        Color foregroundColor = character.getForegroundColor().toColor();
        Color backgroundColor = character.getBackgroundColor().toColor();
        g.setColor(backgroundColor);
        //g.setClip(x, y, characterWidth, fontHeight);
        g.fillRect(x, y, characterWidth, fontHeight);
        g.setColor(foregroundColor);

        //FontMetrics fontMetrics = g.getFontMetrics();
        //g.drawString(Character.toString(character.getCharacter()), x, y + fontHeight - fontMetrics.getDescent() + 1);
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

//        if (this.deviceConfiguration.getCursorStyle() == CursorStyle.UNDER_BAR) {
//            g.fillRect(x, y + fontHeight - 3, characterWidth, 2);
//        } else if (this.deviceConfiguration.getCursorStyle() == CursorStyle.VERTICAL_BAR) {
            g.fillRect(x, y + 1, characterWidth, fontHeight - 2);
//        }
        }

    }
}
