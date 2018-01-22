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
import org.jetbrains.annotations.Nullable;
import spacegraph.SurfaceBase;

import java.io.OutputStream;
import java.util.TreeSet;


public class ConsoleTerminal extends BitmapConsoleSurface/*ConsoleSurface*/ {

    public final VirtualTerminal term;
    private VirtualTerminalListener listener;

    public ConsoleTerminal(int cols, int rows) {
        this(new DefaultVirtualTerminal(new TerminalSize(cols, rows)));
    }


    public ConsoleTerminal(VirtualTerminal t) {

        this.term = t;
        //resize(term.getTerminalSize().getColumns(), term.getTerminalSize().getRows());
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
    public void start(@Nullable SurfaceBase parent) {
        synchronized (this) {
            super.start(parent);

            term.addVirtualTerminalListener(listener = new VirtualTerminalListener() {


                @Override
                public void onFlush() {
                    needUpdate.set(true);
                }

                @Override
                public void onBell() {

                }

                @Override
                public void onClose() {
                }

                @Override
                public void onResized(Terminal terminal, TerminalSize terminalSize) {
                    //render();
                }
            });

            needUpdate.set(true);
            //term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw

        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            term.close();
            term.removeVirtualTerminalListener(listener);
            listener = null;
            super.stop();
        }
    }


    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {

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

                //TODO other control keys

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

    //
//    private static final ImmutableCharSet TYPED_KEYS_TO_IGNORE = CharSets.immutable.of('\n', '\t', '\r', '\b', '\u001b', '\u007f');
//
//    private boolean cursorIsVisible;
//    private boolean enableInput;


//    private final boolean blinkOn;

    //    private TerminalPosition lastDrawnCursorPosition;





//    synchronized void onCreated() {
//        this.enableInput = true;
//    }
//
//    synchronized void onDestroyed() {
//        this.enableInput = false;
//    }


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

    @Override
    protected boolean updateBackBuffer() {
        final TerminalPosition cursorPosition = term.getCursorBufferPosition();
        final TerminalSize viewportSize = term.getTerminalSize();
        int firstVisibleRowIndex = 0 / fontHeight;
        int lastVisibleRowIndex = (this.pixelHeight()) / fontHeight;


        int cols = viewportSize.getColumns();
        int lastCol = this.cursorCol;
        cursorCol = cursorPosition.getColumn();
        int lastRow = this.cursorRow;
        cursorRow = cursorPosition.getRow();

        boolean allDirty;
        if (term instanceof DefaultVirtualTerminal) {
            allDirty = ((DefaultVirtualTerminal)term).isWholeBufferDirtyThenReset();
        } else {
            allDirty = true;
        }

        if (allDirty) {
            term.forEachLine(firstVisibleRowIndex, lastVisibleRowIndex, (row, bufferLine) -> {

                for (int column = 0; column < cols; ++column) {
                    redraw(bufferLine, column, row);
                }

//                if (needUpdate.get()) {
//                    actuallyDirty = true; //TODO
//                    return; //update in next frame
//                }

            });
        } else {
            if (lastCol!=this.cursorCol || lastRow!=this.cursorRow) {
                redraw(lastCol, lastRow);
                redraw(cursorCol, cursorRow);
            }
            TreeSet<TerminalPosition> dirty = ((DefaultVirtualTerminal) term).getAndResetDirtyCells();
            if (!dirty.isEmpty()) {
                for (TerminalPosition e: dirty) {
                    redraw(e.getColumn(), e.getRow());
//                    if (needUpdate.get())
//                        return false; //update in next frame
                }
            }

        }

        return !needUpdate.get();
    }

//    public int cursorX() {
//        return term.getCursorPosition().getColumn();
//    }
//
//    public int cursorY() {
//        return term.getCursorPosition().getRow();
//    }
//
//    public int[] getCursorPos() {
//        TerminalPosition p = term.getCursorPosition();
//        int[] cursorPos = new int[2];
//        cursorPos[0] = p.getColumn();
//        cursorPos[1] = p.getRow();
//        return cursorPos;
//    }

    @Override
    public TextCharacter charAt(int col, int row) {
        return term.getCharacter(col, row);
    }

    @Override
    public void resize(int cols, int rows) {
        super.resize(cols, rows);
        term.setTerminalSize(new TerminalSize(cols, rows));
    }


    protected void redraw(int column, int row) {
        redraw(term.getBufferCharacter(column, row), column, row);
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
