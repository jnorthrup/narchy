package nars.experiment;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import jcog.math.FloatRange;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.game.action.GoalActionConcept;
import nars.game.sensor.Signal;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.PreciseTruth;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.console.VectorTextGrid;

import java.util.Arrays;

import static nars.Op.BELIEF;

/**
 * executes a unix shell and perceives the output as a grid of symbols
 * which can be interactively tagged by human, and optionally edited by NARS
 */
public class ConsoleAgent extends GameX {


    static final Atomic WRITE = Atomic.the("write");

    static final char[] alphabet =
            //new char[] { ' ', 'a', 'b' };
            new char[]{' ', 'x'};

    final int WIDTH;
    final int HEIGHT;
    static final float fps = 24f;

    final TestConsole R;
    final TestConsole W;
    float prevSim;

    /** whether to accept manual override input */
    boolean manualOverride = true;

    public final Loop noise;

    public ConsoleAgent(int w, int h, NAR nar) {
        super("target", nar);

        this.WIDTH = w; this.HEIGHT = h;
        R = new TestConsole(
                $.inh(id,"should"),
                WIDTH, HEIGHT, alphabet) {

            @Override
            public Surface finger(Finger finger) {
                if (manualOverride && finger.pressedNow(0))
                    root().keyFocus(this);
                return super.finger(finger);
            }

            @Override
            public boolean key(KeyEvent e, boolean pressed) {

                if (manualOverride && pressed) {
                    if (!e.isPrintableKey()) {
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_DOWN:
                                Down();
                                return true;
                            case KeyEvent.VK_UP:
                                Up();
                                return true;
                            case KeyEvent.VK_LEFT:
                                Left();
                                return true;
                            case KeyEvent.VK_RIGHT:
                                Right();
                                return true;
                        }
                        return false;
                    } else {
                        char c = e.getKeyChar();
                        //TODO restrict alphabet?
                        write(c);
                        return true;
                    }
                }


                return false;

            }


        };


        W = new TestConsole(
                $.inh(id, "is"),
                R.W(), R.H(), alphabet) {

            final FloatRange moveThresh = new FloatRange(0.51f, 0, 1f);
            final FloatRange writeThresh = new FloatRange(0.51f, 0, 1f);

            {

                Term id = ConsoleAgent.this.id;

                GoalActionConcept[] lr = actionPushButtonMutex($.inh(id, "left"), $.inh(id, "right"), this::Left, this::Right, moveThresh::floatValue);
                PreciseTruth OFF = $.t(0, nar.confDefault(BELIEF));
                lr[0].goalDefault(OFF, nar);
                lr[1].goalDefault(OFF, nar);

                if (R.H() > 1) {
                    GoalActionConcept[] ud = actionPushButtonMutex($.inh(id, "up"), $.inh(id, "down"), this::Up, this::Down, moveThresh::floatValue);
                    ud[0].goalDefault(OFF, nar);
                    ud[1].goalDefault(OFF, nar);
                }
                for (char c : alphabet) {
                    Term C = $.inh(id, $.quote(String.valueOf(c)));
                    //actionPushButton(C, writeThresh::floatValue, () -> write(c));
                    GoalActionConcept cc = actionUnipolar(C, (x) -> {
                        if (x > writeThresh.floatValue()) {
                            if (write(c))
                                return 1;
                        }
                        return 0;
                    });
                    cc.goalDefault(OFF, nar);
                }
            }

        };

        reward("similar", 1f, () -> {

            float s = similarity(R.chars, W.chars);
            return s;
        });

        noise = Loop.of(()->{
            R.c[0] = random().nextInt(R.cols);
            R.c[1] = random().nextInt(R.rows);
            R.write(alphabet[random().nextInt(alphabet.length)]);
        }).setFPS(0.5f);

    }

    public static void main(String[] args) {


        GameX.runRT((n) -> {
            ConsoleAgent a = new ConsoleAgent(3, 1, n);
            SpaceGraph.window(new Gridding(a.R, a.W), 800, 400);
            return a;
        }, fps);

    }

    private static float similarity(char[][] a, char[][] b) {
        int total = 0, equal = 0;
        for (int j = 0; j < a[0].length; j++) {
            for (int i = 0; i < a.length; i++) {
                equal += (a[i][j] == b[i][j]) ? 1 : 0;
                total++;
            }
        }
        return (equal) / ((float) total);
    }

    private class TestConsole extends VectorTextGrid implements KeyPressed {

        final char[][] chars;

        private final Signal[][][] charMatrix;
        private final char[] alphabet;
        int[] c = new int[2];


        public TestConsole(Term id, int w, int h, char[] alphabet) {
            super(w, h);

            this.alphabet = alphabet;

            this.chars = new char[w][h];
            for (char[] cc : chars)
                Arrays.fill(cc, alphabet[0]);

            //TODO use Bitmap3D or Tensor something
            this.charMatrix = new Signal[w][h][alphabet.length];


//            PriNode charAttn = new PriNode(id);
//            nar.control.input(charAttn, sensorPri);

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    Term XY = $.p($.the(x), $.the(y));
                    //PriNode xy = new PriNode(XY);
                    //nar.control.input(xy, charAttn);

                    for (int i = 0, alphabetLength = alphabet.length; i < alphabetLength; i++) {
                        char a = alphabet[i];
                        Term xya =
                                $.inh(id, $.p($.quote(String.valueOf(a)), XY));
                                //$.funcImg((Atomic)id, $.the(a), XY);
                        int xx = x;
                        int yy = y;

                        //HACK
                        Signal cm = charMatrix[x][y][i] = sense(xya, () -> chars[xx][yy] == a);
                        //nar.control.input(((ScalarSignal)(cm).pri, xy);

                    }
                }
            }
            c[0] = 0;
            c[1] = 0;
//            what().onTask(t->{
//                System.out.println(t);
//            });
        }

        @Override
        public int[] getCursorPos() {
            return c;
        }


        @Override
        public TextCharacter charAt(int col, int row) {
            TextCharacter t = new TextCharacter(chars[col][row]);
            return t;
        }


        @Override
        protected boolean setBackgroundColor(GL2 gl, TextCharacter c, int col, int row) {
            //nar.beliefTruth(charMatrix[col][row][], nar.time());
            float cc = 0.25f; //nar.concepts.pri(charMatrix[col][row].target, 0);
            if (cc == cc) {
                gl.glColor4f(cc, cc, cc, 0.95f);
                return true;
            }
            return false;
        }

        public boolean Left() {
            int before = c[0];
            c[0] = Math.max(0, c[0] - 1);
            return c[0]!=before;
        }

        public boolean Up() {
            int before = c[1];
            c[1] = Math.max(0, c[1] - 1);
            return c[1]!=before;
        }

        public boolean Down() {
            int before = c[1];
            c[1] = Math.min(rows() - 1, c[1] + 1);
            return c[1]!=before;
        }

        public boolean Right() {
            int before = c[0];
            c[0] = Math.min(cols() - 1, c[0] + 1);
            return c[0]!=before;
        }

        public int rows() {
            return chars[0].length;
        }


        public int cols() {
            return chars.length;
        }

        public boolean write(char value) {
            int cx = this.c[0];
            int cy = this.c[1];
            char prev = chars[cx][cy];
            if (prev!=value) {
                chars[cx][cy] = value;
                return true;
            }
            return false;
        }

//        protected void believe(char c, int x, int y) {
//            chars[x][y] = c;
//            char value = chars[cx][cy];
//
//            if (prev == 0 || (value != prev)) {
//                Task prevBelief = beliefs[cx][cy] != null ? beliefs[cx][cy].get() : null;
//                if (prevBelief != null) {
//
//                }
//
//                beliefs[cx][cy].setAt(
//                        nar.conceptualize($.inst(terms[cx][cy], $.the(String.valueOf(value)))),
//                        $.t(1f, 0.9f),
//                        () -> nar.time.nextStamp(),
//                        nar.time(), nar.dur(), nar);
//            }
//        }

//        @Override
//        protected void doLayout(int dtMS) {
//
//        }


//        @Override
//        public boolean key(com.jogamp.newt.event.KeyEvent e, boolean pressedOrReleased) {
//            return false;
//        }

        public int H() {
            return chars[0].length;
        }

        public int W() {
            return cols();
        }

        @Override
        public void invalidate() {

        }

//        public Stream<SignalTask> input() {
//
//            return IntStream.range(0, rows() * cols()).mapToObj(i -> {
//                int x = i % cols();
//                int y = (i - x) / cols();
//                return beliefs[x][y].get();
//            }).filter(Objects::nonNull);
//        }


    }


}
