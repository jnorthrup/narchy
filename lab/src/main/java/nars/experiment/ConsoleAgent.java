package nars.experiment;

import com.googlecode.lanterna.TextCharacter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import jcog.math.FloatRange;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.attention.PriNode;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.term.atom.Atomic;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.console.VectorTextGrid;

import java.util.Arrays;

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
                $.p(id,Atomic.the("should")),
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
                $.p(id,Atomic.the("is")),
                R.W(), R.H(), alphabet) {

            final FloatRange moveThresh = new FloatRange(0.51f, 0, 1f);
            final FloatRange writeThresh = new FloatRange(0.51f, 0, 1f);

            {

                Term id = ConsoleAgent.this.id;
                actionPushButtonMutex($.inh(id,$.$$("left")), $.inh(id,$.$$("right")), ()->Left(), ()->Right(), moveThresh::floatValue);
                actionPushButtonMutex($.inh(id,$.$$("up")), $.inh(id,$.$$("down")), ()->Up(), ()->Down(), moveThresh::floatValue);
                for (char c : alphabet) {
                    actionPushButton($.inh(id, $.p(WRITE, $.the(c))),
                            writeThresh::floatValue, () -> write(c));
                }
            }

        };

        reward("similar", 1f, () -> {

            float s = similarity(R.chars, W.chars);
            return s;
//            float d = s - prevSim;
//            prevSim = s;
//            if (d == 0)
//                return s == 0 ? +1 : Float.NaN;
//            if (d < 0)
//                return -1;
//            else
//                return +1;
            //return d==0 ? Float.NaN : Util.tanhFast(d);
        });

        noise = Loop.of(()->{
            R.c[0] = random().nextInt(R.cols);
            R.c[1] = random().nextInt(R.rows);
            R.write(alphabet[random().nextInt(alphabet.length)]);
        }).setFPS(0.05f);

    }

    public static void main(String[] args) {


        GameX.runRT((n) -> {
            ConsoleAgent a = new ConsoleAgent(3, 3, n);
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


            PriNode charAttn = new PriNode(id);

            nar().control.parent(charAttn, new PriNode[]{attnSensor});

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    Term XY = $.p($.the(x), $.the(y));
                    PriNode xy = new PriNode(XY);

                    nar().control.parent(xy, new PriNode[]{charAttn});

                    for (int i = 0, alphabetLength = alphabet.length; i < alphabetLength; i++) {
                        char a = alphabet[i];
                        Term xya =
                                $.inh(id, $.p($.the(a), XY));
                                //$.funcImg((Atomic)id, $.the(a), XY);
                        int xx = x;
                        int yy = y;

                        nar().control.parent((charMatrix[x][y][i] = sense(xya,
                                () -> chars[xx][yy] == a)).attn, new PriNode[]{xy});

                    }
                }
            }
            c[0] = 0;
            c[1] = 0;
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
            float cc = 0.5f; //nar.concepts.pri(charMatrix[col][row].target, 0);
            if (cc == cc) {
                gl.glColor4f(cc, cc, cc, 0.95f);
                return true;
            }
            return false;
        }

        public void Left() {
            c[0] = Math.max(0, c[0] - 1);
        }

        public void Up() {
            c[1] = Math.max(0, c[1] - 1);
        }

        public void Down() {
            c[1] = Math.min(rows() - 1, c[1] + 1);
        }

        public int rows() {
            return chars[0].length;
        }

        public void Right() {
            c[0] = Math.min(cols() - 1, c[0] + 1);
        }

        public int cols() {
            return chars.length;
        }

        public void write(char value) {
            int cx = this.c[0];
            int cy = this.c[1];
            char prev = chars[cx][cy];

            chars[cx][cy] = value;
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
