package nars;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.IOSafeTerminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;
import com.googlecode.lanterna.terminal.swing.*;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import jcog.Texts;
import nars.gui.Vis;
import org.jetbrains.annotations.Nullable;
import spacegraph.Scale;
import spacegraph.Surface;
import spacegraph.container.EmptySurface;
import spacegraph.container.Gridding;
import spacegraph.input.Finger;
import spacegraph.widget.console.ConsoleTerminal;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.meta.OmniBox;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.LabeledPane;
import spacegraph.widget.windo.Widget;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static spacegraph.render.JoglPhysics.window;

public class Shell {


    public static final float INITIAL_FPS = 25f;

    public static void main(String[] argv) {


        if (argv.length == 0) {
            System.out.println("Usage:");
            System.out.println("  gui\t\tstart gui");
            System.out.println("  telnet <port>\t\tstart telnet server on given port");
            System.out.println("  \"<narsese>\"\t\texecute narsese command"); //daemonize?
            System.out.println("Reading narsese from stdin..\n"); //daemonize?
            narseseStdin();

            //System.out.println("  js \"<javascript>\"\t\texecute NARjs code");
        } else {
            switch (argv[0]) {
                case "gui":
                    NAR ui = NARchy.ui();
                    ui.startFPS(INITIAL_FPS);
                    ui.runLater(() -> {
                        window(
                                Vis.top(ui)
                                //((Grid)Vis.reflect(ui.services)).aspect(0.25f)
                                , 1024, 800);
                    });

                    break;
                case "telnet":
                    int port = Texts.i(argv[1]);

                    NAR n = NARchy.core();
                    new Thread(() -> {
                        try {
                            new TextUI(n, port);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    n.startFPS(INITIAL_FPS);

                    break;

                default:

                    if (argv.length > 1) {
                        narsese(() -> argv[1]);
                    } else {
                        //stdin
                        narseseStdin();
                    }

                    break;
//                case "js":
//                    break;
            }
        }


    }

    static final Supplier<String> stdin = () -> {
        try {
            byte[] b = System.in.readAllBytes();
            return new String(b);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    };

    /**
     * TODO make stream/iterable
     */
    private static void narsese(Supplier<String> s) {
        NAR n = NARchy.ui();
        String in = s.get();
        if (in != null) {
            try {
                n.input(in);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
        }
        n.start();
    }

    /**
     * TODO make stream/iterable
     */
    private static void narseseStdin() {

        LineNumberReader lr = new LineNumberReader(new InputStreamReader(System.in));

        NAR n = NARchy.ui();
        n.start();
        while (true) {
            try {
                String l = lr.readLine();
                if (l == null)
                    break; //EOF
                n.input(l);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                break;
            }
        }
    }

    public static final float TERMINAL_DISPLAY_FPS = 8f;

    public Shell(NAR nar) {


        shellGL(nar);
        //shellSwing(nar);

    }

    public static class ConsoleWidget extends Widget {

        private final ConsoleTerminal console;
        AtomicBoolean menuShown = new AtomicBoolean(false);

        public ConsoleWidget(VirtualTerminal term) {


            Surface menu = new Scale(new LabeledPane("Text Scale", new Gridding(
                    new XYSlider()
            )), 0.5f);

            this.console = new ConsoleTerminal(term) {

                float charAspect = 1.6f;

                //target rows to cols ratio
                //float areaAspect = (80f/25f)/(charAspect);

                float scale = 80f;

                Consumer<Finger> pressable = Finger.clicked(0, () -> {
                    if (menuShown.compareAndSet(false, true)) {
                        content(menu);
                    } else if (menuShown.compareAndSet(true, false)) {
                        content(new EmptySurface());
                    }
                });


                @Override
                public void touch(@Nullable Finger finger) {
                    super.touch(finger);
                    pressable.accept(finger);
                }


                @Override
                public void doLayout(int dtMS) {

                    text.doLayout(dtMS);

                    float cc, rr;
                    float boundsAspect = text.h() / text.w();
                    if (boundsAspect >= 1) {
                        //taller
                        cc = scale / boundsAspect;
                        rr = cc / charAspect;
                        //System.out.println(bounds + " taller: " + cc + "x" + rr);
                    } else {
                        //wider
                        cc = scale;
                        rr = cc * (boundsAspect / charAspect);
                        //System.out.println(bounds + "  wider: " + cc + "x" + rr);
                    }

                    resize(Math.max(2, Math.round(cc)), Math.max(2, Math.round(rr)));

                }

            };


            content(console);
        }
    }

    public void shellGL(NAR nar) {

        //window(Vis.top(nar), 1024, 1024);
        //window(new AutoSurface<>(nar), 700, 600);

        window(new OmniBox(), 600, 200);
        window(new AutoSurface<>(nar.services).aspect(0.1f), 800, 800);
    }


    public void shellSwing(NAR nar) {

        //DefaultTerminalFactory tf = new DefaultTerminalFactory();
        //tf.setForceTextTerminal(true);
//        try {
//          //Terminal tt = tf.createTerminal();
//            //Terminal tt = tf.createTerminal();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        MySwingTerminalFrame tt = new MySwingTerminalFrame(
                "",
                null,
                null,
                //null
                new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.EVERYTHING_BUT_SYMBOLS, new Font("Monospaced", Font.PLAIN, 28)),
                null,
                EnumSet.of(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode).toArray(new TerminalEmulatorAutoCloseTrigger[1]));
        tt.setSize(800, 800);
        tt.setVisible(true);


        new TextUI(nar, tt, TERMINAL_DISPLAY_FPS);
    }


    /**
     * the original SwingTerminalFrame avoids any means of reasonable configuration :(
     * <p>
     * whats so scary about 'public final' FFS
     */
    public static class MySwingTerminalFrame extends JFrame implements IOSafeTerminal {
        public final SwingTerminal swingTerminal;
        public final EnumSet<TerminalEmulatorAutoCloseTrigger> autoCloseTriggers;
        private boolean disposed;

        /**
         * Creates a new SwingTerminalFrame with an optional list of auto-close triggers
         *
         * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
         */
        @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
        public MySwingTerminalFrame(TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
            this("SwingTerminalFrame", autoCloseTriggers);
        }

        /**
         * Creates a new SwingTerminalFrame with a specific title and an optional list of auto-close triggers
         *
         * @param title             Title to use for the window
         * @param autoCloseTriggers What to trigger automatic disposal of the JFrame
         */
        @SuppressWarnings("WeakerAccess")
        public MySwingTerminalFrame(String title, TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) throws HeadlessException {
            this(title, new SwingTerminal(), autoCloseTriggers);
        }

        /**
         * Creates a new SwingTerminalFrame using a specified title and a series of swing terminal configuration objects
         *
         * @param title               What title to use for the window
         * @param deviceConfiguration Device configuration for the embedded SwingTerminal
         * @param fontConfiguration   Font configuration for the embedded SwingTerminal
         * @param colorConfiguration  Color configuration for the embedded SwingTerminal
         * @param autoCloseTriggers   What to trigger automatic disposal of the JFrame
         */
        public MySwingTerminalFrame(String title,
                                    TerminalEmulatorDeviceConfiguration deviceConfiguration,
                                    SwingTerminalFontConfiguration fontConfiguration,
                                    TerminalEmulatorColorConfiguration colorConfiguration,
                                    TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
            this(title, null, deviceConfiguration, fontConfiguration, colorConfiguration, autoCloseTriggers);
        }

        /**
         * Creates a new SwingTerminalFrame using a specified title and a series of swing terminal configuration objects
         *
         * @param title               What title to use for the window
         * @param terminalSize        Initial size of the terminal, in rows and columns. If null, it will default to 80x25.
         * @param deviceConfiguration Device configuration for the embedded SwingTerminal
         * @param fontConfiguration   Font configuration for the embedded SwingTerminal
         * @param colorConfiguration  Color configuration for the embedded SwingTerminal
         * @param autoCloseTriggers   What to trigger automatic disposal of the JFrame
         */
        public MySwingTerminalFrame(String title,
                                    TerminalSize terminalSize,
                                    TerminalEmulatorDeviceConfiguration deviceConfiguration,
                                    SwingTerminalFontConfiguration fontConfiguration,
                                    TerminalEmulatorColorConfiguration colorConfiguration,
                                    TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
            this(title,
                    new SwingTerminal(terminalSize, deviceConfiguration, fontConfiguration, colorConfiguration),
                    autoCloseTriggers);
        }

        private MySwingTerminalFrame(String title, SwingTerminal swingTerminal, TerminalEmulatorAutoCloseTrigger... autoCloseTriggers) {
            super(title != null ? title : "SwingTerminalFrame");
            this.swingTerminal = swingTerminal;
            this.autoCloseTriggers = EnumSet.copyOf(Arrays.asList(autoCloseTriggers));
            this.disposed = false;

            swingTerminal.setIgnoreRepaint(true);
            setContentPane(swingTerminal);
//            getContentPane().setLayout(new BorderLayout());
//            getContentPane().add(swingTerminal, BorderLayout.CENTER);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setBackground(Color.BLACK); //This will reduce white flicker when resizing the window
            pack();

            //Put input focus on the terminal component by default
            swingTerminal.requestFocusInWindow();
        }

        /**
         * Returns the auto-close triggers used by the SwingTerminalFrame
         *
         * @return Current auto-close trigger
         */
        public Set<TerminalEmulatorAutoCloseTrigger> getAutoCloseTrigger() {
            return EnumSet.copyOf(autoCloseTriggers);
        }

        /**
         * Sets the auto-close trigger to use on this terminal. This will reset any previous triggers. If called with
         * {@code null}, all triggers are cleared.
         *
         * @param autoCloseTrigger Auto-close trigger to use on this terminal, or {@code null} to clear all existing triggers
         * @return Itself
         */
        public MySwingTerminalFrame setAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger autoCloseTrigger) {
            this.autoCloseTriggers.clear();
            if (autoCloseTrigger != null) {
                this.autoCloseTriggers.add(autoCloseTrigger);
            }
            return this;
        }

        /**
         * Adds an auto-close trigger to use on this terminal.
         *
         * @param autoCloseTrigger Auto-close trigger to add to this terminal
         * @return Itself
         */
        public MySwingTerminalFrame addAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger autoCloseTrigger) {
            if (autoCloseTrigger != null) {
                this.autoCloseTriggers.add(autoCloseTrigger);
            }
            return this;
        }

        @Override
        public void dispose() {
            super.dispose();
            disposed = true;
        }

        @Override
        public void close() {
            dispose();
        }

        /**
         * Takes a KeyStroke and puts it on the input queue of the terminal emulator. This way you can insert synthetic
         * input events to be processed as if they came from the user typing on the keyboard.
         *
         * @param keyStroke Key stroke input event to put on the queue
         */
        public void addInput(KeyStroke keyStroke) {
            swingTerminal.addInput(keyStroke);
        }

        ///////////
        // Delegate all Terminal interface implementations to SwingTerminal
        ///////////
        @Override
        public KeyStroke pollInput() {
            if (disposed) {
                return new KeyStroke(KeyType.EOF);
            }
            KeyStroke keyStroke = swingTerminal.pollInput();
            if (autoCloseTriggers.contains(TerminalEmulatorAutoCloseTrigger.CloseOnEscape) &&
                    keyStroke != null &&
                    keyStroke.getKeyType() == KeyType.Escape) {
                dispose();
            }
            return keyStroke;
        }

        @Override
        public KeyStroke readInput() {
            return swingTerminal.readInput();
        }

        @Override
        public void enterPrivateMode() {
            swingTerminal.enterPrivateMode();
        }

        @Override
        public void exitPrivateMode() {
            swingTerminal.exitPrivateMode();
            if (autoCloseTriggers.contains(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode)) {
                dispose();
            }
        }

        @Override
        public void clearScreen() {
            swingTerminal.clearScreen();
        }

        @Override
        public void setCursorPosition(int x, int y) {
            swingTerminal.setCursorPosition(x, y);
        }

        @Override
        public void setCursorPosition(TerminalPosition position) {
            swingTerminal.setCursorPosition(position);
        }

        @Override
        public TerminalPosition getCursorPosition() {
            return swingTerminal.getCursorPosition();
        }

        @Override
        public void setCursorVisible(boolean visible) {
            swingTerminal.setCursorVisible(visible);
        }

        @Override
        public void putCharacter(char c) {
            swingTerminal.putCharacter(c);
        }

        @Override
        public TextGraphics newTextGraphics() {
            return swingTerminal.newTextGraphics();
        }

        @Override
        public void enableSGR(SGR sgr) {
            swingTerminal.enableSGR(sgr);
        }

        @Override
        public void disableSGR(SGR sgr) {
            swingTerminal.disableSGR(sgr);
        }

        @Override
        public void resetColorAndSGR() {
            swingTerminal.resetColorAndSGR();
        }

        @Override
        public void setForegroundColor(TextColor color) {
            swingTerminal.setForegroundColor(color);
        }

        @Override
        public void setBackgroundColor(TextColor color) {
            swingTerminal.setBackgroundColor(color);
        }

        @Override
        public TerminalSize getTerminalSize() {
            return swingTerminal.getTerminalSize();
        }

        @Override
        public byte[] enquireTerminal(int timeout, TimeUnit timeoutUnit) {
            return swingTerminal.enquireTerminal(timeout, timeoutUnit);
        }

        @Override
        public void bell() {
            swingTerminal.bell();
        }

        @Override
        public void flush() {
            swingTerminal.flush();
        }

        @Override
        public void addResizeListener(TerminalResizeListener listener) {
            swingTerminal.addResizeListener(listener);
        }

        @Override
        public void removeResizeListener(TerminalResizeListener listener) {
            swingTerminal.removeResizeListener(listener);
        }
    }

}
