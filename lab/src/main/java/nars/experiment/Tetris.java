package nars.experiment;

import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatRange;
import jcog.signal.wave2d.AbstractBitmap2D;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.NARS;
import nars.agent.BeliefReward;
import nars.agent.GameTime;
import nars.gui.sensor.VectorSensorView;
import nars.op.java.Opjects;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.$.$$;
import static nars.experiment.Tetris.TetrisState.*;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends GameX {

    final static float FPS = 24f;

    private static final int tetris_width = 8;
    private static final int tetris_height = 16;

    private final Bitmap2D grid;

    private final boolean opjects = false;

    private final boolean canFall = false;

    public final FloatRange timePerFall = new FloatRange(1f, 1f, 32f);
    public final AtomicBoolean easy;

    public final Bitmap2DSensor<Bitmap2D> pixels;
    private final TetrisState state;


    public Tetris(NAR nar) {
        this(nar, Tetris.tetris_width, Tetris.tetris_height);
    }

    public Tetris(NAR nar, int width, int height) {
        this(nar, width, height, 1);
    }

    public Tetris(NAR n, int width, int height, int timePerFall) {
        this(Atomic.the("tetris"), n, width, height, timePerFall);
    }

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(Term id, NAR n, int width, int height, int timePerFall) {
        super(id,
                GameTime.fps(24f),
                //FrameTrigger.durs(1),
                n);


        //if a pixel is on, pixels above it should be off
        reward(new BeliefReward($$("(&|,tetris(#x,#yBelow),--tetris(#x,#yAbove),cmp(#yBelow,#yAbove,1))"), this));

        //pixels on same row should be the same color TODO
        //reward(new BeliefReward($$("--xor(tetris(#x1,#y), tetris(#x2,#y))"), this));

//        //pixels left or right from each other should both be on
//        reward(new BeliefReward($$("(&|,tetris(#x1,#y),tetris(#x2,#y),addAt(#x1,1,#x2))"), this));


        state = opjects ?
                actionsReflect() :
                new TetrisState(width, height, timePerFall);

        easy = state.easy;

        state.timePerFall = Math.round(this.timePerFall.floatValue());


        rewardNormalized("score", 0 /* ignore decrease */, 1,
                state::score
                //new FloatFirstOrderDifference(n::time, state::score).nanIfZero()
        );
        rewardNormalized("height", -1, 1, new FloatFirstOrderDifference(nar::time, () ->
                1 - ((float) state.rowsFilled) / state.height
        ));
        rewardNormalized("density", 0, 1, () -> {

            int filled = 0;
            for (float s : state.grid) {
                if (s > 0) {
                    filled++;
                }
            }

            int r = state.rowsFilled;
            return r > 0 ? ((float) filled) / (r * state.width) : 0;
        });

        grid = new AbstractBitmap2D(state.width, state.height) {
            @Override
            public float brightness(int x, int y) {
                return state.seen[y * w + x] > 0 ? 1f : 0f;
            }
        };

        //                .mode((p,v)->{
        //                    float c = n.confDefault(BELIEF);
        //                    return $.t(v, p!=v || v > 0.5f ? c : c/2);
        //                })
        final Term GRID = id; //Atomic.the("grid");
        Bitmap2DSensor<Bitmap2D> c = pixels = new Bitmap2DSensor<>(
                (x, y) -> $.inh($.p(x, y), GRID),
                //(x, y) -> $.p(GRID,$.the(x), $.the(y)),
                grid, n);
        addSensor(c);
        //pixels.resolution(0.05f);

        window(new VectorSensorView(pixels, this).withControls(), 400, 900);


        actionsPushButton();
        //actionsToggle();
        //actionsTriState();

        state.reset();

        onFrame(() -> {
            state.timePerFall = Math.round(this.timePerFall.floatValue());
            state.next();
        });

    }


    public static void main(String[] args) {


        GameX.runRT(n -> {
            //n.freqResolution.setAt(0.02f);


            //new Abbreviation(n, ("z"), 4, 5,  8);

//            PriArrayBag<NLink<Term>> implConc = new PriArrayBag<>(PriMerge.plus, 64);
//            DurService.on(n, (nn) -> {
//                long now = nn.time();
//                implConc.commit();
//                nn.concepts().filter(x -> x.op() == IMPL).forEach(x -> {
//
//                    Truth t = x.beliefs().truth(now, nn);
//                    if (t != null) {
//                        Term pred = x.target().sub(1);
//                        implConc.put(new NLink(pred, t.evi()*0.001f));
//                    }
//                });
//                if (!implConc.isEmpty())
//                    System.out.println(now + ": " + implConc.get(0));
//            });

            return new Tetris(n, Tetris.tetris_width, Tetris.tetris_height);
        },  FPS);

//        int instances = 2;
//        for (int i = 0; i < instances; i++)
//            runRTNet((n)-> {
//
//                    new Arithmeticize.ArithmeticIntroduction(32, n);
//
//                        return new Tetris($.p(Atomic.the("t"), n.self()), n, tetris_width, tetris_height, 1);
//                    },
//                    2, FPS, FPS, 6);

    }

    private TetrisState actionsReflect() {

        Opjects oo = new Opjects(nar);
        oo.exeThresh.set(0.51f);
//        oo.pri.setAt(ScalarValue.EPSILON);

        Opjects.methodExclusions.add("toVector");

        return oo.a("tetris", TetrisState.class, tetris_width, tetris_height, 2);
    }

    void actionsPushButton() {
        final Term LEFT =
                $.the("left");
        //$.inh("left", id);
        final Term RIGHT =
                $.the("right");
        //$.inh("right", id);
        final Term ROT =
                $.the("rotate");
        //$.inh("rotate", id);
        final Term FALL =
                $.the("fall");
        //$.inh("fall", id);

        actionPushButtonMutex(LEFT, RIGHT,
                b -> {
                    if (b) {
                        return state.act(TetrisState.LEFT);
                    } else return false;
                },
                b -> {
                    if (b) {
                        return state.act(TetrisState.RIGHT);
                    } else return false;
                }
        );

        int debounceDurs = 1;
        actionPushButton(ROT, debounce(b -> {
            if (b) {
                return state.act(TetrisState.CW);
            } else return false;
        }, debounceDurs));

        if (canFall)
            actionPushButton(FALL, debounce(b -> {
                if (b) {
                    return state.act(TetrisState.FALL);
                } else return false;
            }, debounceDurs * 2));

    }


    void actionsTriState() {


        actionTriState($.func("X", id), i -> {
            switch (i) {
                case -1:
                    return state.act(LEFT);
                case +1:
                    return state.act(RIGHT);
                default:
                case 0:
                    return true;
            }
        });


        actionPushButton($.func("R", id), () -> state.act(CW));


    }


    public static class OfflineTetris {
        public static void main(String[] args) {


            NAR n = NARS.tmp();
            n.time.dur(4);
            n.freqResolution.set(0.02f);
            n.confResolution.set(0.02f);


            new Tetris(n, Tetris.tetris_width, Tetris.tetris_height, 2);
            n.run(200);


            n.concepts().forEach(c -> {
                System.out.println(c);
                c.tasks().forEach(t -> {
                    System.out.println("\t" + t.toString(true));
                });
                System.out.println();
            });


            n.stats(System.out);
        }
    }


    public static class TetrisPiece {

        int[][][] thePiece = new int[4][5][5];
        int currentOrientation;

        public static TetrisPiece makeSquare() {
            TetrisPiece newPiece = new TetrisPiece();


            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 1, 1, 0};
            int[] row2 = {0, 0, 1, 1, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(0, row0, row1, row2, row3, row4);
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(2, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        public static TetrisPiece makeTri() {
            TetrisPiece newPiece = new TetrisPiece();

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 0, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
            }
            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 1, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(1, row0, row1, row2, row3, row4);
            }

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 0, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }

            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 1, 0, 0};
            int[] row2 = {0, 1, 1, 0, 0};
            int[] row3 = {0, 0, 1, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        public static TetrisPiece makeLine() {
            TetrisPiece newPiece = new TetrisPiece();

            {

                int[] row0 = {0, 0, 1, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }


            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 0, 0, 0};
            int[] row2 = {0, 1, 1, 1, 1};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);
            return newPiece;

        }

        public static TetrisPiece makeSShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 1, 0, 0, 0};
                int[] row2 = {0, 1, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }


            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 1, 1, 0};
            int[] row2 = {0, 1, 1, 0, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);
            return newPiece;

        }

        public static TetrisPiece makeZShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 1, 1, 0, 0};
                int[] row3 = {0, 1, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }


            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 1, 1, 0, 0};
            int[] row2 = {0, 0, 1, 1, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);
            return newPiece;

        }

        public static TetrisPiece makeLShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 1, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
            }
            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 0, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 1, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(1, row0, row1, row2, row3, row4);
            }

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 1, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }

            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 0, 1, 0};
            int[] row2 = {0, 1, 1, 1, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        public static TetrisPiece makeJShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 1, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
            }
            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 1, 0, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 0, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(1, row0, row1, row2, row3, row4);
            }

            {

                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 1, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }

            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 0, 0, 0};
            int[] row2 = {0, 1, 1, 1, 0};
            int[] row3 = {0, 0, 0, 1, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        public void setShape(int Direction, int[] row0, int[] row1, int[] row2, int[] row3, int[] row4) {
            thePiece[Direction][0] = row0;
            thePiece[Direction][1] = row1;
            thePiece[Direction][2] = row2;
            thePiece[Direction][3] = row3;
            thePiece[Direction][4] = row4;
        }

        public int[][] getShape(int whichOrientation) {
            return thePiece[whichOrientation];
        }

        @Override
        public String toString() {
            StringBuilder shapeBuffer = new StringBuilder();
            for (int i = 0; i < thePiece[currentOrientation].length; i++) {
                for (int j = 0; j < thePiece[currentOrientation][i].length; j++) {
                    shapeBuffer.append(' ').append(thePiece[currentOrientation][i][j]);
                }
                shapeBuffer.append('\n');
            }
            return shapeBuffer.toString();

        }
    }

    public static class TetrisState {
        /*Action values*/
        public static final int LEFT = 0; /*Action value for a move left*/
        public static final int RIGHT = 1; /*Action value for a move right*/
        public static final int CW = 2; /*Action value for a clockwise rotation*/
        public static final int CCW = 3; /*Action value for a counter clockwise rotation*/
        public static final int NONE = 4; /*The no-action Action*/
        public static final int FALL = 5; /* fall down */
        private final Random randomGenerator = new Random();
        public int width;
        public int height;
        public float[] seen;
        public boolean running = true;
        public int currentBlockId;/*which block we're using in the block table*/

        public int currentRotation;
        public int currentX;/* where the falling block is currently*/

        public int currentY;
        public float score;/* what is the current_score*/

        public boolean is_game_over;/*have we reached the end state yet*/


        public float[] grid;/*what the world looks like without the current block*/
        public int time;
        public int timePerFall;
        Vector<TetrisPiece> possibleBlocks = new Vector<>();
        private int rowsFilled;

        public final AtomicBoolean easy = new AtomicBoolean(false);


        public TetrisState(int width, int height, int timePerFall) {
            this.width = width;
            this.height = height;
            this.timePerFall = timePerFall;
            possibleBlocks.add(TetrisPiece.makeLine());
            possibleBlocks.add(TetrisPiece.makeSquare());
            possibleBlocks.add(TetrisPiece.makeTri());
            possibleBlocks.add(TetrisPiece.makeSShape());
            possibleBlocks.add(TetrisPiece.makeZShape());
            possibleBlocks.add(TetrisPiece.makeLShape());
            possibleBlocks.add(TetrisPiece.makeJShape());

            grid = new float[this.height * this.width];
            seen = new float[width * height];
            reset();
        }

        protected void reset() {
            currentX = width / 2 - 1;
            currentY = 0;
            score = 0;
            for (int i = 0; i < grid.length; i++) {
                grid[i] = 0;
            }
            currentRotation = 0;
            is_game_over = false;

            running = true;
            restart();

            spawnBlock();
        }

        /**
         * do nothing method for signaling to NAR restart occurred, but not to allow it to trigger an actual restart
         */
        public void restart() {

        }

        private void toVector(boolean monochrome, float[] target) {


            Arrays.fill(target, -1);

            int x = 0;
            for (double i : grid) {
                if (monochrome)
                    target[x] = i > 0 ? 1.0f : -1.0f;
                else
                    target[x] = i > 0 ? (float) i : -1.0f;
                x++;
            }

            writeCurrentBlock(target, 0.5f);


        }


        private void writeCurrentBlock(float[] f, float color) {
            int[][] thisPiece = possibleBlocks.get(currentBlockId).getShape(currentRotation);

            if (color == -1)
                color = currentBlockId + 1;
            for (int y = 0; y < thisPiece[0].length; ++y) {
                for (int x = 0; x < thisPiece.length; ++x) {
                    if (thisPiece[x][y] != 0) {


                        int linearIndex = i(currentX + x, currentY + y);
                        /*if(linearIndex<0){
                            System.err.printf("Bogus linear index %d for %d + %d, %d + %d\n",linearIndex,currentX,x,currentY,y);
                            Thread.dumpStack();
                            System.exit(1);
                        }*/
                        f[linearIndex] = color;
                    }
                }
            }

        }

        public boolean gameOver() {
            return is_game_over;
        }

        public boolean act(int theAction) {
            return act(theAction, true);
        }

        /* This code applies the action, but doesn't do the default fall of 1 square */
        public boolean act(int theAction, boolean enable) {
            synchronized (this) {


                int nextRotation = currentRotation;
                int nextX = currentX;
                int nextY = currentY;

                switch (theAction) {
                    case CW:
                        nextRotation = (currentRotation + 1) % 4;
                        break;
                    case CCW:
                        nextRotation = currentRotation - 1;
                        if (nextRotation < 0) {
                            nextRotation = 3;
                        }
                        break;
                    case LEFT:
                        nextX = enable ? currentX - 1 : currentX;
                        break;
                    case RIGHT:
                        nextX = enable ? currentX + 1 : currentX;
                        break;
                    case FALL:
                        nextY = currentY;

                        boolean isInBounds = true;
                        boolean isColliding = false;


                        while (isInBounds && !isColliding) {
                            nextY++;
                            isInBounds = inBounds(nextX, nextY, nextRotation);
                            if (isInBounds) {
                                isColliding = colliding(nextX, nextY, nextRotation);
                            }
                        }
                        nextY--;
                        break;
                    default:
                        throw new RuntimeException("unknown action");
                }


                return act(nextRotation, nextX, nextY);
            }
        }

        protected boolean act() {
            return act(currentRotation, currentX, currentY);
        }

        protected boolean act(int nextRotation, int nextX, int nextY) {

            synchronized (this) {

                if (inBounds(nextX, nextY, nextRotation)) {
                    if (!colliding(nextX, nextY, nextRotation)) {
                        currentRotation = nextRotation;
                        currentX = nextX;
                        currentY = nextY;
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Calculate the learn array position from (x,y) components based on
         * worldWidth.
         * Package level access so we can use it in tests.
         *
         * @param x
         * @param y
         * @return
         */
        private final int i(int x, int y) {
            return y * width + x;


        }


        /**
         * Check if any filled part of the 5x5 block array is either out of bounds
         * or overlapping with something in wordState
         *
         * @param checkX           X location of the left side of the 5x5 block array
         * @param checkY           Y location of the top of the 5x5 block array
         * @param checkOrientation Orientation of the block to check
         * @return
         */
        private boolean colliding(int checkX, int checkY, int checkOrientation) {
            int[][] thePiece = possibleBlocks.get(currentBlockId).getShape(checkOrientation);
            int ll = thePiece.length;
            try {

                for (int y = 0; y < thePiece[0].length; ++y) {
                    for (int x = 0; x < ll; ++x) {
                        if (thePiece[x][y] != 0) {


                            if (checkY + y < 0 || checkX + x < 0) {
                                return true;
                            }


                            if (checkY + y >= height || checkX + x >= width) {
                                return true;
                            }


                            int linearArrayIndex = i(checkX + x, checkY + y);
                            if (grid[linearArrayIndex] != 0) {
                                return true;
                            }
                        }
                    }
                }
                return false;

            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::colliding called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
                return true;
            }
        }

        private boolean collidingCheckOnlySpotsInBounds(int checkX, int checkY, int checkOrientation) {
            int[][] thePiece = possibleBlocks.get(currentBlockId).getShape(checkOrientation);
            int ll = thePiece.length;
            try {

                for (int y = 0; y < thePiece[0].length; ++y) {
                    for (int x = 0; x < ll; ++x) {
                        if (thePiece[x][y] != 0) {


                            if (checkX + x >= 0 && checkX + x < width && checkY + y >= 0 && checkY + y < height) {


                                int linearArrayIndex = i(checkX + x, checkY + y);
                                if (grid[linearArrayIndex] != 0) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;

            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::collidingCheckOnlySpotsInBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
                return true;
            }
        }

        /**
         * This function checks every filled part of the 5x5 block array and sees if
         * that piece is in bounds if the entire block is sitting at (checkX,checkY)
         * on the board.
         *
         * @param checkX           X location of the left side of the 5x5 block array
         * @param checkY           Y location of the top of the 5x5 block array
         * @param checkOrientation Orientation of the block to check
         * @return
         */
        private boolean inBounds(int checkX, int checkY, int checkOrientation) {
            try {
                int[][] thePiece = possibleBlocks.get(currentBlockId).getShape(checkOrientation);

                for (int y = 0; y < thePiece[0].length; ++y) {
                    for (int x = 0; x < thePiece.length; ++x) {
                        if (thePiece[x][y] != 0) {


                            if (!(checkX + x >= 0 && checkX + x < width && checkY + y >= 0 && checkY + y < height)) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::inBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning false from inBounds to help save from error.  Not sure if that's wise.");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
                return false;
            }

        }

        public boolean nextInBounds() {
            return inBounds(currentX, currentY + 1, currentRotation);
        }

        public boolean nextColliding() {
            return colliding(currentX, currentY + 1, currentRotation);
        }

        /*Ok, at this point, they've just taken their action.  We now need to make them fall 1 spot, and check if the game is over, etc */
        private void update() {
            act();
            time++;


            if (!inBounds(currentX, currentY, currentRotation)) {
                System.err.println("In GameState.Java the Current Position of the board is Out Of Bounds... Consistency Check Failed");
            }


            boolean onSomething = false;
            if (!nextInBounds()) {
                onSomething = true;
            }
            if (!onSomething) {
                if (nextColliding()) {
                    onSomething = true;
                }
            }

            if (onSomething) {
                running = false;
                writeCurrentBlock(grid, -1);
            } else {

                if (time % timePerFall == 0)
                    currentY += 1;
            }

        }

        public int spawnBlock() {
            running = true;

            currentBlockId = nextBlock();

            currentRotation = 0;
            currentX = width / 2 - 2;
            currentY = -4;


            boolean hitOnWayIn = false;
            while (!inBounds(currentX, currentY, currentRotation)) {

                hitOnWayIn = collidingCheckOnlySpotsInBounds(currentX, currentY, currentRotation);
                currentY++;
            }
            is_game_over = colliding(currentX, currentY, currentRotation) || hitOnWayIn;
            if (is_game_over) {
                running = false;
            }

            return currentBlockId;
        }

        protected int nextBlock() {
            if (easy.get()) {
                return 1; //square
            } else {
                return randomGenerator.nextInt(possibleBlocks.size());
            }

        }

        public void checkScore() {
            int numRowsCleared = 0;
            int rowsFilled = 0;


            for (int y = height - 1; y >= 0; --y) {
                if (isRow(y, true)) {
                    removeRow(y);
                    numRowsCleared += 1;
                    y += 1;
                } else {
                    if (!isRow(y, false))
                        rowsFilled++;
                }
            }

            int prevRows = this.rowsFilled;
            this.rowsFilled = rowsFilled;


            if (numRowsCleared > 0) {


            } else {

            }


            int diff = prevRows - rowsFilled;

            if (diff >= height - 1) {

                score = Float.NaN;

            } else {


                score = diff;
            }
        }

        public float height() {
            return (float) rowsFilled / height;
        }

        /**
         * Check if a row has been completed at height y.
         * Short circuits, returns false whenever we hit an unfilled spot.
         *
         * @param y
         * @return
         */
        public boolean isRow(int y, boolean filledOrClear) {
            for (int x = 0; x < width; ++x) {
                float s = grid[i(x, y)];
                if (filledOrClear ? s == 0 : s != 0) {
                    return false;
                }
            }
            return true;
        }


        /**
         * Dec 13/07.  Radkie + Tanner found 2 bugs here.
         * Bug 1: Top row never gets updated when removing lower rows. So, if there are
         * pieces in the top row, and we clear something, they will float there.
         *
         * @param y
         */
        void removeRow(int y) {
            if (!isRow(y, true)) {
                System.err.println("In GameState.java remove_row you have tried to remove a row which is not complete. Failed to remove row");
                return;
            }

            for (int x = 0; x < width; ++x) {
                int linearIndex = i(x, y);
                grid[linearIndex] = 0;
            }


            for (int ty = y; ty > 0; --ty) {
                for (int x = 0; x < width; ++x) {
                    int linearIndexTarget = i(x, ty);
                    int linearIndexSource = i(x, ty - 1);
                    grid[linearIndexTarget] = grid[linearIndexSource];
                }
            }


            for (int x = 0; x < width; ++x) {
                int linearIndex = i(x, 0);
                grid[linearIndex] = 0;
            }

        }


        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }


        public int getCurrentPiece() {
            return currentBlockId;
        }

        /**
         * Utility methd for debuggin
         */
        public void printState() {
            int index = 0;
            for (int i = 0; i < height - 1; i++) {
                for (int j = 0; j < width; j++) {
                    System.out.print(grid[i * width + j]);
                }
                System.out.print("\n");
            }
            System.out.println("-------------");


        }


        protected void next() {
            if (running) {
                update();
            } else {
                spawnBlock();
            }

            checkScore();

            toVector(false, seen);

            if (gameOver()) {
                die();
            }


        }

        private float score() {
            return score;
        }

        protected void die() {
            reset();
        }


    }
}



































            /*view.plot2 = new GridSurface(HORIZONTAL,
                

                conceptLinePlot(nar, t.actions, (c) -> {
                    try {
                        return nar.concept(c).goals().truth(nar.time()).freq();
                    } catch (NullPointerException npe) {
                        return 0.5f;
                    }
                }, 256)
            );*/




























































































































