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

    public final TextBox textBox;

    public TextEdit(int c, int r) {
        this(c, r, "");
    }

    public TextEdit(int c, int r, String initialContent) {
        super(new TerminalSize(c, r));

        setCursorVisible(true);

        textBox = new TextBox(initialContent, r>1 ? TextBox.Style.MULTI_LINE : TextBox.Style.SINGLE_LINE) {

//            {
//                setBacklogSize(8); //???
//            }

            @Override
            public Result handleKeyStroke(KeyStroke keyStroke) {
                if (!onKey(keyStroke))
                    return Result.HANDLED;

                return super.handleKeyStroke(keyStroke);
            }
        };
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


    public Surface surface() {
        ConsoleGUI g = new ConsoleGUI(this) {

            @Override
            protected void init(BasicWindow window) {
                textBox.setPreferredSize(new TerminalSize(window.getSize().getColumns() - 2, window.getSize().getRows() - 2));

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
