package nars;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.IOSafeTerminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;
import com.googlecode.lanterna.terminal.swing.*;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.jogamp.newt.event.KeyEvent;
import jcog.Texts;
import jcog.util.ArrayUtil;
import nars.web.NARWeb;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.console.ConsoleTerminal;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Shell {

    public static final float INITIAL_FPS = 25f;
    public static final float TERMINAL_DISPLAY_FPS = 8f;
    static final Supplier<String> stdin = () -> {
        try {
            byte[] b = System.in.readAllBytes();
            return new String(b);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    };

    public static void main(String[] args) throws IOException {


        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("  gui\t\tstart gui");
            System.out.println("  telnet <port>\t\tstart telnet server on given port");
            System.out.println("  http <port>\t\tstart http server on given port");
            System.out.println("  \"<narsese>\"\t\texecute narsese command"); 
            
            narseseStdin();

            
        } else {

            args = ArrayUtil.subarray(args, 1, args.length);

            switch (args[0]) {
                case "web":
                    NARWeb.Single.main(args);
                    break;

                case "gui":
                    GUI.main(args);
                    break;
                case "telnet":
                    int port = Texts.i(args[0]);

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

                    if (args.length > 1) {
                        String[] finalArgs = args;
                        narsese(() -> finalArgs[1]);
                    } else {
                        
                        narseseStdin();
                    }

                    break;


            }
        }


    }

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
        n.startFPS(10f);
        while (true) {
            try {
                String l = lr.readLine();
                if (l == null)
                    break; 
                n.input(l);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                break;
            }
        }
    }

    public void shellSwing(NAR nar) {

        
        







        MySwingTerminalFrame tt = new MySwingTerminalFrame(
                "",
                null,
                null,
                
                new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.EVERYTHING_BUT_SYMBOLS, new Font("Monospaced", Font.PLAIN, 28)),
                null,
                EnumSet.of(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode).toArray(new TerminalEmulatorAutoCloseTrigger[1]));
        tt.setSize(800, 800);
        tt.setVisible(true);


        new TextUI(nar, tt, TERMINAL_DISPLAY_FPS);
    }


    public static class TestConsoleWidget {
        public static void main(String[] args) throws IOException {









            PipedOutputStream termIn;
            PipedOutputStream termKeys = new PipedOutputStream();
            PipedInputStream termOut = new PipedInputStream(termKeys);
            DefaultVirtualTerminal term = new DefaultVirtualTerminal(new TerminalSize(80, 40));
            ConsoleTerminal termView;

//
//            JShell js = JShell.builder()
//                    .in(termOut)
//
//                    .build();


            SpaceGraph.window(termView = new ConsoleTerminal(term) {
                @Override
                public boolean key(KeyEvent e, boolean pressedOrReleased) {

                        
//                        js.eval(String.valueOf(e.getKeyChar()));




                    return super.key(e, pressedOrReleased);
                }
            },1000,800);

            termView.output().write('x');
            termView.output().flush();
        }


        }

//        public static class ConsoleWidget extends Widget {
//
//            private final ConsoleTerminal console;
//            AtomicBoolean menuShown = new AtomicBoolean(false);
//
//            public ConsoleWidget(VirtualTerminal target) {
//
//
//                Surface menu = new Scale(new LabeledPane("Text Scale", new Gridding(
//                        new XYSlider()
//                )), 0.5f);
//
//                this.console = new ConsoleTerminal(target) {
//
//                    float charAspect = 1.6f;
//
//
//
//
//                    float scale = 80f;
//
//                    Predicate<Finger> pressable = Finger.clicked(0, () -> {
//                        if (menuShown.compareAndSet(false, true)) {
//                            setAt(menu);
//                        } else if (menuShown.compareAndSet(true, false)) {
//                            setAt(new EmptySurface());
//                        }
//                    });
//
//
//                    @Override
//                    public void onFinger(@Nullable Finger finger) {
//                        super.onFinger(finger);
//                        pressable.test(finger);
//                    }
//
//
//                    @Override
//                    public void doLayout(int dtMS) {
//
////                        text.doLayout(dtMS);
//
//                        float cc, rr;
//                        float boundsAspect = text.h() / text.w();
//                        if (boundsAspect >= 1) {
//
//                            cc = scale / boundsAspect;
//                            rr = cc / charAspect;
//
//                        } else {
//
//                            cc = scale;
//                            rr = cc * (boundsAspect / charAspect);
//
//                        }
//
//                        resize(Math.max(2, Math.round(cc)), Math.max(2, Math.round(rr)));
//
//                    }
//
//                };
//
//
//                setAt(console);
//            }
//        }

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


                setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                setBackground(Color.BLACK);
                pack();

                
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
            public void addInput(com.googlecode.lanterna.input.KeyStroke keyStroke) {
                swingTerminal.addInput(keyStroke);
            }

            
            
            
            @Override
            public com.googlecode.lanterna.input.KeyStroke pollInput() {
                if (disposed) {
                    return new com.googlecode.lanterna.input.KeyStroke(KeyType.EOF);
                }
                com.googlecode.lanterna.input.KeyStroke keyStroke = swingTerminal.pollInput();
                if (autoCloseTriggers.contains(TerminalEmulatorAutoCloseTrigger.CloseOnEscape) &&
                        keyStroke != null &&
                        keyStroke.getKeyType() == KeyType.Escape) {
                    dispose();
                }
                return keyStroke;
            }

            @Override
            public com.googlecode.lanterna.input.KeyStroke readInput() {
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
            public TerminalPosition getCursorPosition() {
                return swingTerminal.getCursorPosition();
            }

            @Override
            public void setCursorPosition(TerminalPosition position) {
                swingTerminal.setCursorPosition(position);
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
