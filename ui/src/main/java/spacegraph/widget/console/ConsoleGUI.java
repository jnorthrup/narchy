package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/** SpaceGraph surface for displaying and interacting with a TextGUI system */
public class ConsoleGUI extends ConsoleTerminal {

    public MultiWindowTextGUI gui;
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

    @Override
    public void start(@Nullable Surface parent) {
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

}
