package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminalListener;
import jcog.event.Off;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** SpaceGraph surface for displaying and interacting with a TextGUI system */
public class ConsoleGUI extends ConsoleTerminal {

    private static final int MAX_ROWS = 80;
    private static final int MAX_COLS = 120;

    private MultiWindowTextGUI gui;
    private TerminalScreen screen = null;
    private Off updates = null;
    private BasicWindow window = null;
    private VirtualTerminalListener listener;


    public ConsoleGUI(int cols, int rows) {
        this(new DefaultVirtualTerminal(new TerminalSize(cols, rows)));
    }


    public ConsoleGUI(VirtualTerminal t) {
        super(t);
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
                term.flush();
            }
        };
    }



    void init(BasicWindow window) {

    }



    private static final Theme DARK = SimpleTheme.makeTheme(
            true,
            TextColor.ANSI.WHITE, 
            new TextColor.RGB(40,40,40), 
            TextColor.ANSI.WHITE, 
            TextColor.ANSI.BLUE, 
            TextColor.ANSI.WHITE, 
            new TextColor.RGB(25,25,25), 
            new TextColor.RGB(15,15,15));

    @Override
    protected void starting() {

        super.starting();

        term.addVirtualTerminalListener(listener = new VirtualTerminalListener() {


            @Override
            public void onFlush() {
                text.invalidate();
            }

            @Override
            public void onBell() {

            }

            @Override
            public void onClose() {
            }

            @Override
            public void onResized(Terminal terminal, TerminalSize terminalSize) {
                text.invalidate();
            }
        });

        text.invalidate();


        try {

            screen = new TerminalScreen(term);
            screen.startScreen();


            gui = new MultiWindowTextGUI(MyStupidGUIThread::new, screen);


            window = new BasicWindow();
            window.setPosition(new TerminalPosition(0, 0));

//                TerminalSize size = term.getTerminalSize();
//                window.setSize(new TerminalSize(size.getColumns() , size.getRows() ));

            window.setHints(List.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));

            window.setTheme(DARK);
            window.setEnableDirectionBasedMovements(true);


            gui.addWindow(window);
            gui.setActiveWindow(window);
            gui.setEOFWhenNoWindows(true);

            init(window);

            TextGUIThread guiThread = gui.getGUIThread();
            updates = root().onUpdate((s) -> {
                try {
                    guiThread.processEventsAndUpdate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void stopping() {
        term.removeVirtualTerminalListener(listener);
        listener = null;

        if (gui!=null) {
            gui.removeWindow(window);
            gui = null;
        }

        if (screen != null) {
            try {
                screen.stopScreen();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            screen = null;
        }

        if (updates!=null) {
            updates.close();
            updates = null;
        }


        super.stopping();
    }

    /** TODO throttle the different update processes if necessary */
    static class MyStupidGUIThread extends AbstractTextGUIThread {

        MyStupidGUIThread(TextGUI textGUI) {
            super(textGUI);
        }

        @Override
        public synchronized boolean processEventsAndUpdate() throws IOException {

            try {
                textGUI.processInput();
            } catch (EOFException e) {
                return false;
            }


            Runnable r;
            while ((r = customTasks.poll())!=null) {
                r.run();
            }

            if(textGUI.isPendingUpdate()) {
                textGUI.updateScreen();
                return true;
            }

            return false;
        }

        @Override
        public Thread getThread() {
            return Thread.currentThread();
        }
    }

    @Override
    public void resize(int cols, int rows) {
        cols = Math.min(cols, MAX_COLS);
        rows = Math.min(rows, MAX_ROWS);
        super.resize(cols, rows);

        if (screen!=null) {
            screen.doResizeIfNecessary();
            if (gui!=null) {
                Window win = gui.getActiveWindow();
                if (win!=null) {
                    win.setSize(new TerminalSize(cols, rows));
                }
            }
        }
    }
}
