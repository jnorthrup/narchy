package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.SurfaceBase;

import java.io.IOException;
import java.util.List;

/** SpaceGraph surface for displaying and interacting with a TextGUI system */
public class ConsoleGUI extends ConsoleTerminal {

    MultiWindowTextGUI gui;
    TerminalScreen screen = null;
    On updates = null;
    BasicWindow window = null;

    public ConsoleGUI(int cols, int rows) {
        this(new DefaultVirtualTerminal(new TerminalSize(cols, rows)));
    }

    public ConsoleGUI(DefaultVirtualTerminal term) {
        super(term);
    }

    protected void init(BasicWindow window) {

    }

    static final Theme DARK = SimpleTheme.makeTheme(
            true,
            TextColor.ANSI.WHITE, //baseForeground,
            new TextColor.RGB(40,40,40), //baseBackground,
            TextColor.ANSI.WHITE, // editableForeground,
            TextColor.ANSI.BLUE, //editableBackground,
            TextColor.ANSI.WHITE, //selectedForeground,
            new TextColor.RGB(55,75,55), //selectedBackground,
            new TextColor.RGB(15,15,15));

    @Override
    public void start(@Nullable SurfaceBase parent) {
        synchronized (this) {
            super.start(parent);
//                thr = new Thread(this);
//                thr.setDaemon(true);
//                thr.start();
            try {

                screen = new TerminalScreen(term);
                screen.startScreen();


                gui = new MultiWindowTextGUI(MyStupidGUIThread::new, screen);





                //TODO try to avoid wrapping it in Window

                window = new BasicWindow();
                window.setPosition(new TerminalPosition(0, 0));

                TerminalSize size = term.getTerminalSize();
                window.setSize(new TerminalSize(size.getColumns() , size.getRows() ));

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
    }

    @Override
    public void stop() {
        synchronized (this) {
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
                updates.off();
                updates = null;
            }

            super.stop();
        }
    }

    /** TODO throttle the different update processes if necessary */
    static class MyStupidGUIThread extends AbstractTextGUIThread {

        MyStupidGUIThread(TextGUI textGUI) {
            super(textGUI);
        }

        @Override
        public synchronized boolean processEventsAndUpdate() throws IOException {

            textGUI.processInput();

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
        super.resize(cols, rows);

        if (screen!=null) {
            screen.doResizeIfNecessary();
            if (gui!=null) {
                Window win = gui.getActiveWindow();
                if (win!=null) {
                    win.setSize(new TerminalSize(cols, rows));
                }
            }
            needUpdate.set(true);
        }
    }
}
