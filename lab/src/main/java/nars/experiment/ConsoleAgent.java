package nars.experiment;

import com.googlecode.lanterna.TextCharacter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Task;
import nars.control.channel.CauseChannel;
import nars.gui.NARui;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.signal.Signal;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.console.ConsoleSurface;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * executes a unix shell and perceives the output as a grid of symbols
 * which can be interactively tagged by human, and optionally edited by NARS
 */
public abstract class ConsoleAgent extends NAgentX {

    final BlockingQueue<Task> queue = Util.blockingQueue(16);


    final static int WIDTH = 4;
    final static int HEIGHT = 1;
    final Surface Rlabel = NARui.inputEditor();

    final TestConsole R = new TestConsole(
            Atomic.the("it"),
            true,
            WIDTH, HEIGHT);
    final TestConsole W;


    protected void input(Task t) {
        queue.add(t);
    }


    public ConsoleAgent(NAR nar) {
        super("term", nar);

        
        











        W = new TestConsole(
                nar.self(),
                true,
                R.W(), R.H()).write('a', 'b', ' ');

        
        
        
        SpaceGraph.window(Rlabel, 400, 200);
        SpaceGraph.window(R, 600, 600);
        SpaceGraph.window(W, 600, 600);








        

        CauseChannel<ITask> s = nar.newChannel(this + "_HumanKeys");
        onFrame(() -> {
            
            List<Task> q = $.newArrayList(queue.size());
            Iterator<Task> qq = queue.iterator();
            while (qq.hasNext()) {
                q.add(qq.next());
                qq.remove();
            }

            s.input(q);
        });
    }

    @Override
    protected void run(NAR n, long dt) {
        super.run(n, dt);
        nar.input(Stream.concat(
                W.input(),
                R.input()
        ));
    }

    @Override
    abstract protected float act();

    public static void main(String[] args) {

        NAgentX.runRT((n) -> {
            @NotNull ConsoleAgent a = new ConsoleAgent(n) {
                float prevSim;

                @Override
                protected float act() {
                    
                    float s = similarity(R.chars, W.chars);
                    float d = s - prevSim;
                    prevSim = s;
                    return d;





                }
            };

            a.trace = true;
            return a;
        }, 16f);

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

    private class TestConsole extends ConsoleSurface {

        final char[][] chars;
        final Term terms[][];
        private final boolean read;
        private final Signal[][] beliefs;
        int c[] = new int[2];
        private boolean write;
        


        public TestConsole(Term id, boolean read, int w, int h) {
            super(w, h);
            this.chars = new char[w][h];
            this.terms = new Compound[w][h];
            this.beliefs = new Signal[w][h];

            

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    chars[x][y] = ' ';
                    terms[x][y] = $.p(id, $.the(x), $.the(y));
                    beliefs[x][y] = new Signal(BELIEF, () -> 0.25f);
                    believe((char) 0, x, y);
                }
            }
            c[0] = 0;
            c[1] = 0;
            this.read = read;


        }

        public TestConsole write(char... vocabulary) {
            write = true;
            Atomic agentID = Atomic.the("test");
            actionTriState($.func("cursor", Atomic.the("x"), agentID), (d) -> {
                switch (d) {
                    case -1:
                        Left();
                        break;

                    case +1:
                        Right();
                        break;
                }
            });
            actionTriState($.func("cursor", Atomic.the("y"), agentID), (d) -> {
                switch (d) {
                    case -1:
                        Up();
                        break;
                    case +1:
                        Down();
                        break;

                    
                }
            });
            for (char c : vocabulary) {











                actionToggle($.func(Atomic.the("write"), $.the(String.valueOf(c)), agentID), d -> {
                    if (d) write(c);
                });
            }

            return this;
        }

        @Override
        public int[] getCursorPos() {
            return c;
        }



        

        @Override
        public TextCharacter charAt(int col, int row) {
            char c = chars[col][row];
            return new TextCharacter(c);
        }

        @Override
        public Appendable append(char c) {
            
            return this;
        }

        @Override
        public Appendable append(CharSequence csq) {
            
            return this;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            
            return this;
        }


        @Override
        protected boolean setBackgroundColor(GL2 gl, TextCharacter c, int col, int row) {
            float cc = 1f; 
            if (cc == cc) {
                float s = 0.3f * cc;
                gl.glColor4f(s, 0, 0, 0.95f);
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

        protected void believe(char prev, int cx, int cy) {
            char value = chars[cx][cy];

            if (prev == 0 || (value != prev)) {
                Task prevBelief = beliefs[cx][cy] != null ? beliefs[cx][cy].get() : null;
                if (prevBelief != null) {
                    
                }

                beliefs[cx][cy].set(
                        nar.conceptualize($.inst(terms[cx][cy], $.the(String.valueOf(value)))),
                        $.t(1f, 0.9f),
                        () -> nar.time.nextStamp(),
                        nar.time(), nar.dur(), nar);
            }
        }

        @Override
        protected void doLayout(int dtMS) {
            
        }

        @Override
        public boolean tryKey(KeyEvent e, boolean pressed) {
            if (write) return false; 

            if (pressed) {
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
                    write(c);
                    return true;
                }
            }


            return false;

        }

        public int H() {
            return chars[0].length;
        }

        public int W() {
            return cols();
        }

        public Stream<SignalTask> input() {
            
            return IntStream.range(0, rows() * cols()).mapToObj(i -> {
                int x = i % cols();
                int y = (i - x) / cols();
                return beliefs[x][y].get();
            }).filter(Objects::nonNull);
        }


    }












































}
