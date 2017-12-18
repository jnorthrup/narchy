package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminalListener;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.impl.factory.primitive.CharSets;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.render.Tex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/14/16.
 */
public class ConsoleTerminal extends AbstractConsoleSurface /*ConsoleSurface*/ {

    final Tex tex = new Tex();

    public final VirtualTerminal term;
    private final int[] cursorPos = new int[2];
    private VirtualTerminalListener listener;

    public ConsoleTerminal(int cols, int rows) {
        this(new DefaultVirtualTerminal(new TerminalSize(cols, rows)));
    }


    public ConsoleTerminal(VirtualTerminal t) {

        this.term = t;
        resize(term.getTerminalSize().getColumns(), term.getTerminalSize().getRows());
    }


    private void render() {
        needFullRedraw.set(true);
        while (needFullRedraw.compareAndSet(true, false)) {
            if (updateBackBuffer()) {
                tex.update(backbuffer);
                needFullRedraw.set(false);
                break;
            }
        }
    }

    @Override
    public Appendable append(CharSequence c) {
        int l = c.length();
        for (int i = 0; i < l; i++) {
            append(c.charAt(i));
        }
        return this;
    }

    @Override
    public Appendable append(char c) {
        term.putCharacter(c);
        return this;
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1) {
        throw new UnsupportedOperationException("TODO");
    }


    public OutputStream output() {
        return new OutputStream() {

            @Override
            public void write(int i) {
                append((char) i);
            }

            @Override
            public void flush() {
                term.flush();
            }
        };
    }

    @Override
    public synchronized void start(@Nullable Surface parent) {
        super.start(parent);

        term.addVirtualTerminalListener(listener = new VirtualTerminalListener() {


            @Override
            public void onFlush() {
                render();
            }

            @Override
            public void onBell() {

            }

            @Override
            public void onClose() {
                onDestroyed();
            }

            @Override
            public void onResized(Terminal terminal, TerminalSize terminalSize) {
                render();
            }
        });

        //term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw

    }

    @Override
    public synchronized void stop() {
        super.stop();

        onDestroyed();


        term.close();
        term.removeVirtualTerminalListener(listener);

    }

    @Override
    public void paintIt(GL2 gl) {

        tex.paint(gl, bounds);
        
        if (needFullRedraw.get()) {
            render();
        }
    }

    @Override
    public int[] getCursorPos() {
        TerminalPosition p = term.getCursorPosition();
        cursorPos[0] = p.getColumn();
        cursorPos[1] = p.getRow();
        return cursorPos;
    }

    public int cursorX() {
        return term.getCursorPosition().getColumn();
    }

    public int cursorY() {
        return term.getCursorPosition().getRow();
    }

    @Override
    public TextCharacter charAt(int col, int row) {
        return term.getCharacter(col, row);
    }

    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {

        //return super.onKey(e, pressed);
        VirtualTerminal eterm = this.term;

        int cc = e.getKeyCode();
        if (pressed && cc == 13) {
            term.addInput(new KeyStroke(KeyType.Enter, e.isControlDown(), e.isAltDown(), e.isShiftDown()));
        } else if (pressed && cc == 8) {
            term.addInput(new KeyStroke(KeyType.Backspace, e.isControlDown(), e.isAltDown(), e.isShiftDown()));
        } else if (pressed && cc == 27) {
            term.addInput(new KeyStroke(KeyType.Escape, e.isControlDown(), e.isAltDown(), e.isShiftDown()));
        } else if (e.isPrintableKey() && !e.isActionKey() && !e.isModifierKey()) {
            char c = e.getKeyChar();
            if (!TerminalTextUtils.isControlCharacter(c) && pressed /* release */) {
                //eterm.gui.getActiveWindow().handleInput(
                term.addInput(
                        //eterm.gui.handleInput(
                        new KeyStroke(c, e.isControlDown(), e.isAltDown(), e.isShiftDown())
                );

            } else {
                return false;
            }
        } else if (pressed) {
            KeyType c = null;
            //System.out.println(" keycode: " + e.getKeyCode());
            switch (e.getKeyCode()) {
                case KeyEvent.VK_BACK_SPACE:
                    c = KeyType.Backspace;
                    break;
                case KeyEvent.VK_ENTER:
                    c = KeyType.Enter;
                    break;

                case KeyEvent.VK_INSERT:
                    c = KeyType.Insert;
                    break;
                case KeyEvent.VK_DELETE:
                    c = KeyType.Delete;
                    break;
                case KeyEvent.VK_LEFT:
                    c = KeyType.ArrowLeft;
                    break;
                case KeyEvent.VK_RIGHT:
                    c = KeyType.ArrowRight;
                    break;
                case KeyEvent.VK_UP:
                    c = KeyType.ArrowUp;
                    break;
                case KeyEvent.VK_DOWN:
                    c = KeyType.ArrowDown;
                    break;

                default:
                    //System.err.println("character not handled: " + e);
                    return false;
            }


            //eterm.gui.handleInput(

            //eterm.gui.getActiveWindow().handleInput(
            term.addInput(
                    new KeyStroke(c, e.isControlDown(), e.isAltDown(), e.isShiftDown())
            );
            //                    KeyEvent.isModifierKey(KeyEvent.VK_CONTROL),
//                    KeyEvent.isModifierKey(KeyEvent.VK_ALT),
//                    KeyEvent.isModifierKey(KeyEvent.VK_SHIFT)
//            ));
        } else {
            //...
        }

        return true;
    }


    private static final ImmutableCharSet TYPED_KEYS_TO_IGNORE = CharSets.immutable.of('\n', '\t', '\r', '\b', '\u001b', '\u007f');

    private boolean cursorIsVisible;
    private boolean enableInput;


    private final boolean blinkOn;

    private final AtomicBoolean needFullRedraw = new AtomicBoolean(true);
    private TerminalPosition lastDrawnCursorPosition;

    private final int lastComponentWidth;
    private final int lastComponentHeight;
    private BufferedImage backbuffer;
    Color cursorColor = Color.ORANGE;


    private int fontWidth;

    private int fontHeight;

    private Font font;
    boolean antialias = true;
    boolean quality = false;

    private Graphics2D backbufferGraphics;

    {


        this.cursorIsVisible = true;
        this.enableInput = false;
        this.lastDrawnCursorPosition = null;
        this.lastComponentHeight = 0;
        this.lastComponentWidth = 0;
        this.backbuffer = null;
        this.blinkOn = true;

        setFontSize(24);


    }

    public void setFontSize(int s) {
        this.font = new Font("Monospaced", 0, s);
        this.fontWidth = getFontWidth(font);
        this.fontHeight = getFontHeight(font);
    }

    private FontRenderContext getFontRenderContext() {
        return new FontRenderContext(null, antialias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
    }

    private int getFontWidth(Font font) {
        return (int) font.getStringBounds("W", this.getFontRenderContext()).getWidth();
    }

    private int getFontHeight(Font font) {
        return (int) font.getStringBounds("W", this.getFontRenderContext()).getHeight();
    }

    protected int getFontHeight() {
        return fontHeight;
    }

    protected int getFontWidth() {
        return fontWidth;
    }


    synchronized void onCreated() {
        this.enableInput = true;
    }

    synchronized void onDestroyed() {
        this.enableInput = false;
    }


    private int getWidth() {
        return fontWidth * cols;
    }

    private int getHeight() {
        return fontHeight * rows;
    }

//    synchronized void paintComponent(Graphics componentGraphics) {
//        int width = this.getWidth();
//        int height = this.getHeight();
////        this.scrollController.updateModel(term.getBufferLineCount() * this.getFontHeight(), height);
//        boolean needToUpdateBackBuffer = this.needFullRedraw;
//        int leftoverWidth;
//        if (width != this.lastComponentWidth || height != this.lastComponentHeight) {
//            int columns = width / this.getFontWidth();
//            leftoverWidth = height / this.getFontHeight();
//            TerminalSize terminalSize = term.getTerminalSize().withColumns(columns).withRows(leftoverWidth);
//            term.setTerminalSize(terminalSize);
//            needToUpdateBackBuffer = true;
//        }
//
////        if (needToUpdateBackBuffer) {
////            this.updateBackBuffer(this.scrollController.getScrollingOffset());
////        }
//
//        this.ensureGraphicBufferHasRightSize();
//        Rectangle clipBounds = componentGraphics.getClipBounds();
//        if (clipBounds == null) {
//            clipBounds = new Rectangle(0, 0, this.getWidth(), this.getHeight());
//        }
//
//        componentGraphics.drawImage(this.backbuffer, clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height, clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height, (ImageObserver) null);
//        leftoverWidth = this.getWidth() % this.getFontWidth();
//        componentGraphics.setColor(Color.BLACK);
//        if (leftoverWidth > 0) {
//            componentGraphics.fillRect(this.getWidth() - leftoverWidth, 0, leftoverWidth, this.getHeight());
//        }
//
//        this.lastComponentWidth = width;
//        this.lastComponentHeight = height;
//        componentGraphics.dispose();
//        this.notifyAll();
//    }

    private boolean updateBackBuffer() {
        final int fontWidth = this.getFontWidth();
        final int fontHeight = this.getFontHeight();
        final TerminalPosition cursorPosition = term.getCursorBufferPosition();
        final TerminalSize viewportSize = term.getTerminalSize();
        int firstVisibleRowIndex = 0 / fontHeight;
        int lastVisibleRowIndex = (this.getHeight()) / fontHeight;
        this.ensureGraphicBufferHasRightSize();


//            final AtomicBoolean foundBlinkingCharacters = new AtomicBoolean(this.deviceConfiguration.isCursorBlinking());
//        this.buildDirtyCellsLookupTable(firstVisibleRowIndex, lastVisibleRowIndex);
        int previousLastVisibleRowIndex;
        Graphics2D graphics;
        int previousFirstVisibleRowIndex;

        int cols = viewportSize.getColumns();
        int cursorCol = cursorPosition.getColumn();
        int cursorRow = cursorPosition.getRow();
        int characterWidth = fontWidth * 1; //(TerminalTextUtils.isCharCJK(textCharacter.getCharacter()) ? 2 : 1);

        term.forEachLine(firstVisibleRowIndex, lastVisibleRowIndex, (row, bufferLine) -> {

            if (needFullRedraw.get())
                return;

            for (int column = 0; column < cols; ++column) {
                TextCharacter textCharacter = bufferLine.getCharacterAt(column);
                boolean atCursorLocation = column == cursorCol && row == cursorRow; //cursorPosition.equals(column, row);

                Color foregroundColor = textCharacter.getForegroundColor().toColor();
                Color backgroundColor = textCharacter.getBackgroundColor().toColor();

                drawCharacter(backbufferGraphics, textCharacter, column, row, foregroundColor, backgroundColor, fontWidth, fontHeight, characterWidth, atCursorLocation);

//                if (TerminalTextUtils.isCharCJK(textCharacter.getCharacter())) {
//                    ++column;
//                }
            }

        });
        if (needFullRedraw.get())
            return false;

        this.lastDrawnCursorPosition = cursorPosition;
        return true;
    }


    private void ensureGraphicBufferHasRightSize() {

        if (this.backbuffer != null && this.backbuffer.getWidth() == this.getWidth() && this.backbuffer.getHeight() == this.getHeight()) {
            return;
        }

        BufferedImage newBackbuffer = new BufferedImage(getWidth(), getHeight(), 1);
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

    private void drawCharacter(Graphics g, TextCharacter character, int columnIndex, int rowIndex, Color foregroundColor, Color backgroundColor, int fontWidth, int fontHeight, int characterWidth, boolean drawCursor) {
        int x = columnIndex * fontWidth;
        int y = rowIndex * fontHeight;
        g.setColor(backgroundColor);
        //g.setClip(x, y, characterWidth, fontHeight);
        g.fillRect(x, y, characterWidth, fontHeight);
        g.setColor(foregroundColor);

        //FontMetrics fontMetrics = g.getFontMetrics();
        //g.drawString(Character.toString(character.getCharacter()), x, y + fontHeight - fontMetrics.getDescent() + 1);
        final int descent = 6;
        char c = character.getCharacter();
        if (c != ' ')
            g.drawChars(new char[]{c}, 0, 1, x, y + fontHeight + 1 - descent);


        int lineStartY;
        int lineEndX;
        if (character.isCrossedOut()) {
            lineStartY = y + fontHeight / 2;
            lineEndX = x + characterWidth;
            g.drawLine(x, lineStartY, lineEndX, lineStartY);
        }

        if (character.isUnderlined()) {
            lineStartY = y + fontHeight - descent + 1;
            lineEndX = x + characterWidth;
            g.drawLine(x, lineStartY, lineEndX, lineStartY);
        }

        if (drawCursor) {
            if (cursorColor == null) {
                g.setColor(foregroundColor);
            } else {
                g.setColor(cursorColor);
            }

//        if (this.deviceConfiguration.getCursorStyle() == CursorStyle.UNDER_BAR) {
//            g.fillRect(x, y + fontHeight - 3, characterWidth, 2);
//        } else if (this.deviceConfiguration.getCursorStyle() == CursorStyle.VERTICAL_BAR) {
            g.fillRect(x, y + 1, characterWidth, fontHeight - 2);
//        }
        }

    }


//        public synchronized void enterPrivateMode() {
//            term.enterPrivateMode();
//            this.clearBackBuffer();
//            this.flush();
//        }
//
//        public synchronized void exitPrivateMode() {
//            term.exitPrivateMode();
//            this.clearBackBuffer();
//            this.flush();
//        }
//
//        public synchronized void clearScreen() {
//            term.clearScreen();
//            this.clearBackBuffer();
//        }

//    private void clearBackBuffer() {
//        if (this.backbuffer != null) {
//            Graphics2D graphics = this.backbuffer.createGraphics();
//            Color backgroundColor = Color.BLACK; //this.colorConfiguration.toAWTColor(ANSI.DEFAULT, false, false);
//            graphics.setColor(backgroundColor);
//            graphics.fillRect(0, 0, this.getWidth(), this.getHeight());
//            graphics.dispose();
//        }
//
//    }

//    public synchronized void setCursorPosition(int x, int y) {
//        this.setCursorPosition(new TerminalPosition(x, y));
//    }

//        public synchronized void setCursorPosition(TerminalPosition position) {
//            if (position.getColumn() < 0) {
//                position = position.withColumn(0);
//            }
//
//            if (position.getRow() < 0) {
//                position = position.withRow(0);
//            }
//
//            term.setCursorPosition(position);
//        }
//
//        public TerminalPosition getCursorPosition() {
//            return term.getCursorPosition();
//        }
//
//    public void setCursorVisible(boolean visible) {
//        this.cursorIsVisible = visible;
//    }

//        public synchronized void putCharacter(char c) {
//            term.putCharacter(c);
//        }
//
//        public TextGraphics newTextGraphics() {
//            return term.newTextGraphics();
//        }

//        public void enableSGR(SGR sgr) {
//            term.enableSGR(sgr);
//        }
//
//        public void disableSGR(SGR sgr) {
//            term.disableSGR(sgr);
//        }
//
//        public void resetColorAndSGR() {
//            term.resetColorAndSGR();
//        }
//
//        public void setForegroundColor(TextColor color) {
//            term.setForegroundColor(color);
//        }
//
//        public void setBackgroundColor(TextColor color) {
//            term.setBackgroundColor(color);
//        }
//
//        public synchronized TerminalSize getTerminalSize() {
//            return term.getTerminalSize();
//        }

//    public byte[] enquireTerminal(int timeout, TimeUnit timeoutUnit) {
//        return this.enquiryString.getBytes();
//    }

    //        public void bell() {
//            if (!this.bellOn) {
//                this.bellOn = true;
//                this.needFullRedraw = true;
//                this.updateBackBuffer(this.scrollController.getScrollingOffset());
//                this.repaint();
//                (new Thread("BellSilencer") {
//                    public void run() {
//                        try {
//                            Thread.sleep(100L);
//                        } catch (InterruptedException var2) {
//                            ;
//                        }
//
//                        bellOn = false;
//                        needFullRedraw = true;
//                        updateBackBuffer(scrollController.getScrollingOffset());
//                        repaint();
//                    }
//                }).start();
//                Toolkit.getDefaultToolkit().beep();
//            }
//        }
//
//    public synchronized void flush() {
//        this.updateBackBuffer(this.scrollController.getScrollingOffset());
//        this.repaint();
//    }

//    public void close() {
//    }

//        public void addResizeListener(TerminalResizeListener listener) {
//            term.addResizeListener(listener);
//        }
//
//        public void removeResizeListener(TerminalResizeListener listener) {
//            term.removeResizeListener(listener);
//        }
//
//        private void pasteClipboardContent() {
//            try {
//                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//                if (systemClipboard != null) {
//                    this.injectStringAsKeyStrokes((String) systemClipboard.getData(DataFlavor.stringFlavor));
//                }
//            } catch (Exception var2) {
//                ;
//            }
//
//        }
//
//        private void pasteSelectionContent() {
//            try {
//                Clipboard systemSelection = Toolkit.getDefaultToolkit().getSystemSelection();
//                if (systemSelection != null) {
//                    this.injectStringAsKeyStrokes((String) systemSelection.getData(DataFlavor.stringFlavor));
//                }
//            } catch (Exception var2) {
//                ;
//            }
//
//        }

//        private void injectStringAsKeyStrokes(String string) {
//            StringReader stringReader = new StringReader(string);
//            InputDecoder inputDecoder = new InputDecoder(stringReader);
//            inputDecoder.addProfile(new DefaultKeyDecodingProfile());
//
//            try {
//                for (KeyStroke keyStroke = inputDecoder.getNextCharacter(false); keyStroke != null && keyStroke.getKeyType() != KeyType.EOF; keyStroke = inputDecoder.getNextCharacter(false)) {
//                    this.keyQueue.add(keyStroke);
//                }
//            } catch (IOException var5) {
//                ;
//            }
//
//        }


}
