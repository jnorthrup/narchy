package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.apache.lucene.util.ThreadInterruptedException;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;


public class TextEdit extends DefaultVirtualTerminal implements Runnable {

    public MultiWindowTextGUI gui;

    public final TextBox textBox;
    private Thread thr;

    public TextEdit(int c, int r) {
        this(c, r, "");
    }

    public TextEdit(int c, int r, String initialContent) {
        super(new TerminalSize(c, r));

        textBox = new TextBox(initialContent, r>1 ? TextBox.Style.MULTI_LINE : TextBox.Style.SINGLE_LINE) {

            {
                setBacklogSize(8); //???
            }

            @Override
            public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
                onKey(keyStroke);

                return super.handleKeyStroke(keyStroke);
            }
        };
    }

    public void start() {
        synchronized (this) {
            assert (thr == null);
            thr = new Thread(this);
            thr.start();
        }
    }

    public void stop() {
        synchronized (this) {
            assert (thr != null);
            thr.interrupt();
            thr = null;
        }
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

    @Deprecated
    public ConsoleTerminal view() {
        return new ConsoleTerminal(this) {
            @Override
            public synchronized void start(@Nullable Surface parent) {
                super.start(parent);
                TextEdit.this.start();
            }

            @Override
            public synchronized void stop() {
                TextEdit.this.stop();
                super.stop();
            }

            @Override
            public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
                /** middle mouse button paste */
                Finger.clicked(2, ()->{
                    paste();
                });
                return super.onTouch(finger, hitPoint, buttons);
            }
        };
    }

    @Override
    public void close() {
        super.close();
        gui.removeWindow(gui.getActiveWindow());
    }


    public void commit() {
        try {
            gui.updateScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        TerminalScreen screen = null;
        try {

            screen = new TerminalScreen(this);
            screen.startScreen();

            gui = new MultiWindowTextGUI(new SameTextGUIThread.Factory(), screen);
            gui.setBlockingIO(false);

            setCursorVisible(true);



//            SimpleTheme st = SimpleTheme.makeTheme(
//                                /*SimpleTheme makeTheme(
//                                    boolean activeIsBold,
//                                     TextColor baseForeground,            TextColor baseBackground,
//                                            TextColor editableForeground,            TextColor editableBackground,
//                                                       TextColor selectedForeground,            TextColor selectedBackground,
//                                                              TextColor guiBackground) {*/
//                    true,
//                    TextColor.ANSI.DEFAULT, TextColor.ANSI.BLACK,
//                    TextColor.ANSI.YELLOW, TextColor.ANSI.BLACK,
//                    TextColor.ANSI.WHITE, TextColor.ANSI.BLUE,
//                    TextColor.ANSI.BLACK
//            );
//            //st.setWindowPostRenderer(null);
//            gui.setTheme(st);


            TerminalSize size = getTerminalSize();

            //TODO try to avoid wrapping it in Window

            final BasicWindow window = new BasicWindow();
            window.setPosition(new TerminalPosition(0, 0));
            window.setSize(new TerminalSize(size.getColumns() - 2, size.getRows() - 2));

            window.setHints(List.of(Window.Hint.FULL_SCREEN));
            window.setEnableDirectionBasedMovements(true);


            //Panel panel = new Panel();
            //panel.setPreferredSize(new TerminalSize(32, 8));
            //panel.set



            textBox.setPreferredSize(new TerminalSize(window.getSize().getColumns() - 2, window.getSize().getRows() - 2));

            textBox.takeFocus();

            window.setComponent(textBox);

            gui.addWindow(window);
            gui.setActiveWindow(window);

            gui.setEOFWhenNoWindows(true);

            gui.waitForWindowToClose(window);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ThreadInterruptedException e) {
            if (screen != null) {
                try {
                    screen.stopScreen();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }


    static Clipboard _clipboard = null;
    private synchronized Clipboard clipboard() {
        if (_clipboard == null) {
            _clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        return _clipboard;
    }

    private void onKey(KeyStroke keyStroke) {
        KeyType kt = keyStroke.getKeyType();

        if (kt == KeyType.Enter) {
            if (keyStroke.isCtrlDown()) {
                //ctrl-enter
                onKeyCtrlEnter();
            } else {
                onKeyEnter();
            }
        }

        if (kt == KeyType.Insert && keyStroke.isShiftDown()) {
            paste();
        }
    }

    /** paste clipboard contents in at cursor location */
    public synchronized void paste() {
        //shift-insert, paste
        try {
            String result = (String) clipboard().getData(DataFlavor.stringFlavor);


            for (char c : result.toCharArray()) //HACK
                addInput(KeyStroke.fromString(String.valueOf(c)));

        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
    }

    protected void onKeyCtrlEnter() {


    }
    protected void onKeyEnter() {

    }
}
