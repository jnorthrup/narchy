package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.newt.event.KeyEvent;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.widget.Widget;

import java.io.OutputStream;
import java.util.TreeSet;


/** TODO request key focus */
public class ConsoleTerminal extends Widget implements Appendable, KeyPressed {

    public final VirtualTerminal term;
    public final MyBitmapTextGrid text = new MyBitmapTextGrid();

    public ConsoleTerminal(int cols, int rows) {
        this(new DefaultVirtualTerminal(new TerminalSize(cols, rows)));
    }


    public ConsoleTerminal(VirtualTerminal t) {
        super();
        term = t;
        set(text);
        resize(term.getTerminalSize().getColumns(), term.getTerminalSize().getRows());
    }


    public OutputStream output() {
        return new OutputStream() {

            @Override
            public void write(int i) {
                append((char) i);
            }

            @Override
            public void flush() {
                //ConsoleTerminal.this.flush();
            }
        };
    }


    @Override
    public boolean key(KeyEvent e, boolean pressedOrReleased) {

        int cc = e.getKeyCode();
        if (pressedOrReleased && cc == 13) {
            term.addInput(new KeyStroke(KeyType.Enter, e.isControlDown(), e.isAltDown(), e.isShiftDown()));
        } else if (pressedOrReleased && cc == 8) {
            term.addInput(new KeyStroke(KeyType.Backspace, e.isControlDown(), e.isAltDown(), e.isShiftDown()));
        } else if (pressedOrReleased && cc == 27) {
            term.addInput(new KeyStroke(KeyType.Escape, e.isControlDown(), e.isAltDown(), e.isShiftDown()));
        } else if (e.isPrintableKey() && !e.isActionKey() && !e.isModifierKey()) {
            char c = e.getKeyChar();
            if (!TerminalTextUtils.isControlCharacter(c) && pressedOrReleased /* release */) {

                term.addInput(

                        new KeyStroke(c, e.isControlDown(), e.isAltDown(), e.isShiftDown())
                );

            } else {
                return false;
            }
        } else if (pressedOrReleased) {
            KeyType c = null;

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
                case KeyEvent.VK_HOME:
                    c = KeyType.Home;
                    break;
                case KeyEvent.VK_END:
                    c = KeyType.End;
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

                    return false;
            }


            term.addInput(
                    new KeyStroke(c, e.isControlDown(), e.isAltDown(), e.isShiftDown())
            );


        } else {

        }

        return true;
    }

    @Override
    protected void stopping() {
        term.close();
        super.stopping();
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

    public class MyBitmapTextGrid extends BitmapTextGrid {

        @Override
        protected void doLayout(int dtMS) {
            TerminalSize ts = term.getTerminalSize();
            if (ts.getColumns() != cols || ts.getRows() != rows) {
                term.setTerminalSize(new TerminalSize(cols, rows));
            }
        }


        @Override
        protected boolean renderText() {


            final TerminalPosition cursorPosition = term.getCursorBufferPosition();


            int firstVisibleRowIndex = 0 / fontHeight;
            int lastVisibleRowIndex = (this.pixelHeight()) / fontHeight;


            final int cols = this.cols;

            int lastCol = this.cursorCol;
            cursorCol = cursorPosition.getColumn();
            int lastRow = this.cursorRow;
            cursorRow = cursorPosition.getRow();

            boolean allDirty;
            if (term instanceof DefaultVirtualTerminal) {
                allDirty = ((DefaultVirtualTerminal) term).isWholeBufferDirtyThenReset();
            } else {
                allDirty = true;
            }

            if (allDirty) {
                term.forEachLine(firstVisibleRowIndex, lastVisibleRowIndex, (row, bufferLine) -> {

                    for (int column = 0; column < cols; ++column) {
                        redraw(bufferLine, column, row);
                    }


                });
            } else {
                if (lastCol != this.cursorCol || lastRow != this.cursorRow) {
                    redraw(lastCol, lastRow);
                    redraw(cursorCol, cursorRow);
                }
                TreeSet<TerminalPosition> dirty = ((DefaultVirtualTerminal) term).getAndResetDirtyCells();
                if (!dirty.isEmpty()) {
                    for (TerminalPosition e : dirty) {
                        redraw(e.getColumn(), e.getRow());


                    }
                }

            }

            return !needUpdate.get();
        }

        void redraw(int column, int row) {
            redraw(term.getBufferCharacter(column, row), column, row);
        }

        public TextCharacter charAt(int col, int row) {
            return term.getCharacter(col, row);
        }


    }

    public void resize(int cols, int rows) {
        term.setTerminalSize(new TerminalSize(cols, rows));
        text.resize(cols, rows);
        layout();
    }


}
