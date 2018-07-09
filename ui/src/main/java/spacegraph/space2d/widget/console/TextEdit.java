package spacegraph.space2d.widget.console;

import com.google.common.base.Joiner;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.apache.commons.lang3.ArrayUtils;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;


public class TextEdit extends DefaultVirtualTerminal {

    private static final float DEFAULT_ALPHA = 0.9f;

    public final TextBox textBox;

    public TextEdit(int c, int r) {
        this(c, r, "");
    }

    public TextEdit() {
        this("", true);
    }


    private TextEdit(String initialContent, boolean multiline) {
        this(-1, -1, initialContent, multiline);
    }


    public TextEdit(int c, int r, String initialContent) {
        this(c, r, initialContent, r > 1);
    }

    private TextEdit(int c, int r, String initialContent, boolean multiline) {
        super();


        setCursorVisible(true);

        textBox = new TextBox(initialContent, multiline ? TextBox.Style.MULTI_LINE : TextBox.Style.SINGLE_LINE) {

















            @Override
            public Result handleKeyStroke(KeyStroke keyStroke) {

                if (!onKey(keyStroke))
                    return Result.HANDLED;

                String before = getText();

                TerminalPosition beforePos;
                switch (keyStroke.getKeyType()) {
                    case ArrowLeft:
                    case ArrowRight:
                    case ArrowUp:
                    case ArrowDown:
                        beforePos = getCaretPosition();
                        break;
                    default:
                        beforePos = null;
                        break;
                }

                Result r = super.handleKeyStroke(keyStroke);

                if (r == Result.HANDLED) {
                    
                    String after = getText();
                    if (!before.equals(after))
                        textChange(after);
                    else {
                        if (beforePos != null) {
                            TerminalPosition afterPos = getCaretPosition();
                            if (!beforePos.equals(afterPos))
                                cursorChange(after, afterPos);
                        }
                    }
                }

                return r;
            }

            @Override
            public TextBox setText(String next) {
                String prev = getSize().getColumns() == 0 ? null : this.getText();
                if (prev == null || !prev.equals(next)) {
                    synchronized (this) {
                        super.setText(next);
                    }
                    textChange(next);
                }
                return this;
            }

        };

        if (c != -1)
            textBox.setSize(new TerminalSize(c, r));

    }

    /** signals text didnt change but cursor position did */
    protected void cursorChange(String text, TerminalPosition afterPos) {

    }

    protected void textChange(String next) {

    }


    public String text() {
        return textBox.getText();
    }

    public TextEdit text(String s) {
        textBox.setText(s);
        return this;
    }

    public void clear() {
        text("");
    }


    public ConsoleGUI surface() {
        ConsoleGUI g = new ConsoleGUI(this) {


            @Override
            protected void init(BasicWindow window) {
                

                textBox.takeFocus();

                window.setComponent(textBox);
            }

            @Override
            public Surface tryTouch(Finger finger) {
                /** middle mouse button paste */
                Finger.clicked(2, () -> paste());
                return super.tryTouch(finger);
            }


        };
        g.text.alpha(DEFAULT_ALPHA);
        return g;
    }


    private static Clipboard _clipboard = null;
    private static DataFlavor _clipboardEnc;

    private synchronized Clipboard clipboard() {
        if (_clipboard == null) {
            _clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            _clipboardEnc = DataFlavor.stringFlavor;
        }
        return _clipboard;
    }

    /**
     * returns whether to allow the keystroke into the console (true), or whether it was intercepted (false)
     */
    private boolean onKey(KeyStroke keyStroke) {
        KeyType kt = keyStroke.getKeyType();

        if (kt == KeyType.Enter) {
            if (keyStroke.isCtrlDown()) {
                
                onKeyCtrlEnter();
                return false;
            } else {
                onKeyEnter();
            }
        }

        if (kt == KeyType.Insert && keyStroke.isShiftDown()) {
            paste();
            return false;
        }

        return true;
    }

    /**
     * paste clipboard contents in at cursor location
     */
    private synchronized void paste() {
        
        try {

            

            String result = (String) clipboard().getData(_clipboardEnc);
            addLine(result);


        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean limitLines(int limit) {

        synchronized (textBox) {
            

            String tt = textBox.getText();
            String[] lines = tt.split("\n");
            if (lines.length > limit) {
                lines = ArrayUtils.remove(lines, 0);
                textBox.setText(Joiner.on('\n').join(lines));
                return true;
            }

            return false;
        }
    }

    public void addLine(String x) {
        synchronized (textBox) {
            textBox.addLine(x);
        }
        
        
    }

    protected void onKeyCtrlEnter() {


    }

    protected void onKeyEnter() {

    }


}
