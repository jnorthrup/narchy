package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import spacegraph.Surface;
import spacegraph.input.Finger;

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


    public TextEdit(String initialContent, boolean multiline) {
        this(-1, -1, initialContent, multiline);
    }


    public TextEdit(int c, int r, String initialContent) {
        this(c, r, initialContent, r>1);
    }

    public TextEdit(int c, int r, String initialContent, boolean multiline) {
        super();


        setCursorVisible(true);

        textBox = new TextBox(initialContent, multiline ? TextBox.Style.MULTI_LINE : TextBox.Style.SINGLE_LINE) {

//            {
//                setBacklogSize(8); //???
//            }
//
//            public String getLine(int index) {
//                if (getLineCount() == 0) return "";
//                return super.getLine(index);
//            }
//
//            @Override
//            public String getText() {
//                if (getLineCount() == 0) return "";
//                return super.getText();
//            }


            @Override
            public Result handleKeyStroke(KeyStroke keyStroke) {

                if (!onKey(keyStroke))
                    return Result.HANDLED;

                String before = getText();
                Result r = super.handleKeyStroke(keyStroke);
                if (r == Result.HANDLED) {
                    //HACK
                    String after = getText();
                    if (!before.equals(after))
                        textChange(after);
                }

                return r;
            }

            @Override
            public TextBox setText(String next) {
                String prev = getSize().getColumns()== 0 ? null : this.getText();
                if (prev == null || !prev.equals(next)) {
                    synchronized (this) {
                        super.setText(next);
                    }
                    textChange(next);
                }
                return this;
            }

        };

        if (c!=-1)
            textBox.setSize(new TerminalSize(c, r));

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
                //textBox.setPreferredSize(new TerminalSize(window.getSize().getColumns() - 2, window.getSize().getRows() - 2));

                textBox.takeFocus();

                window.setComponent(textBox);
            }

            @Override
            public Surface onTouch(Finger finger, short[] buttons) {
                /** middle mouse button paste */
                Finger.clicked(2, ()->{
                    paste();
                });
                return super.onTouch(finger, buttons);
            }


        };
        g.alpha(DEFAULT_ALPHA);
        return g;
    }



    static Clipboard _clipboard = null;
    static DataFlavor _clipboardEnc;
    private synchronized Clipboard clipboard() {
        if (_clipboard == null) {
            _clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            _clipboardEnc = DataFlavor.stringFlavor;
        }
        return _clipboard;
    }

    /** returns whether to allow the keystroke into the console (true), or whether it was intercepted (false) */
    private boolean onKey(KeyStroke keyStroke) {
        KeyType kt = keyStroke.getKeyType();

        if (kt == KeyType.Enter) {
            if (keyStroke.isCtrlDown()) {
                //ctrl-enter
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

    /** paste clipboard contents in at cursor location */
    public synchronized void paste() {
        //shift-insert, paste
        try {

            //TODO this probably needs to be run on EDT thread to eliminate the delay involved in reading the clipboard

            String result = (String) clipboard().getData(_clipboardEnc);
            addLine(result);


        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
    }

    public void addLine(String x) {
        textBox.addLine(x);
        //x.chars().forEach(c -> textBox.handleKeyStroke(new KeyStroke((char)c, false, false, false)));
        //flush();
    }

    protected void onKeyCtrlEnter() {


    }

    protected void onKeyEnter() {

    }


}
