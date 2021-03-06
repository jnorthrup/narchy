package nars.experiment;

import jcog.Config;
import jcog.Util;
import jcog.exe.Exe;
import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.AbstractBitmap2D;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.game.GameTime;
import nars.game.NAct;
import nars.game.SimpleReward;
import nars.game.action.GoalActionConcept;
import nars.gui.sensor.VectorSensorChart;
import nars.op.java.Opjects;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static nars.Op.BELIEF;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends GameX {

    public static final String TETRIS_FALL_TIME = Config.get2("TETRIS_FALL_TIME", "" + 1f, false);
    public static final String TETRIS_FALL_MIN = Config.get2("TETRIS_FALL_MIN", "" + 1f, false);
    public static final String TETRIS_FALL_MAX = Config.get2("TETRIS_FALL_MAX", "" + 4f, false);
    public static final boolean TETRIS_CAN_FALL = Config.configIs("TETRIS_CAN_FALL", false);
    public static final boolean TETRIS_USE_DENSITY = Config.configIs("TETRIS_USE_DENSITY", true);
    //public static final boolean TETRIS_USE_SCORE = Config.configIs("TETRIS_USE_SCORE", true);
    static final int tetris_width = 8;
    static final int tetris_height = 16;
    //    public static final boolean TETRIS_V2_REWARDS = Config.configIs("TETRIS_V2_REWARDS", true);
    public static AtomicBoolean easy = new AtomicBoolean(Config.configIs("TETRIS_EASY", false));
    public static boolean opjects = true;

    public static int[][] CENTER_5_X_5 = {TetrisPiece.EMPTY_ROW
            , TetrisPiece.EMPTY_ROW
            , TetrisPiece.CENTER
            , TetrisPiece.EMPTY_ROW
            , TetrisPiece.EMPTY_ROW};
    static float FPS = 24f;
    static float thinkPerFrame = 2.0F;
    private final TetrisState state;
    final Bitmap2DSensor<Bitmap2D> gridVision;
    public Bitmap2DSensor<Bitmap2D> pixels;
    public FloatRange timePerFall = new FloatRange(Float.parseFloat(TETRIS_FALL_TIME), Float.parseFloat(TETRIS_FALL_MIN), Float.parseFloat(TETRIS_FALL_MAX));
    int debounceDurs = 3;
    Term tLEFT =
            //$.the("left");
            //$.inh("left", id);
            $.INSTANCE.inh(id, NAct.NEG);
    Term tRIGHT =
            //$.the("right");
            //$.inh("right", id);
            $.INSTANCE.inh(id, NAct.POS);
    Term tROT =
            //$.the("rotate");
            //$.inh("rotate", id);
            $.INSTANCE.inh(id, "rotate");
    Term tFALL =
            //$.the("fall");
            //$.inh("fall", id);
            $.INSTANCE.inh(id, "fall");


    public Tetris(NAR nar) {
        this(nar, tetris_width, tetris_height);
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
                GameTime.fps(FPS),
                //FrameTrigger.durs(1),
                n
        );


        state = opjects ?
                actionsReflect(n) :
                new TetrisState(width, height, timePerFall);

        easy.set(state.easy);

        state.timePerFall = Math.round(this.timePerFall.floatValue());

        Bitmap2D grid = new AbstractBitmap2D(state.width, state.height) {
            @Override
            public float brightness(int x, int y) {
                return state.seen[y * w + x] > (double) 0 ? 1f : 0f;
            }
        };
        gridVision = addSensor(
                pixels = new Bitmap2DSensor<>(
                        n, grid, new IntIntToObjectFunction<Term>() {
                    @Override
                    public Term apply(int x, int y) {
                        return $.INSTANCE.inh(id, $.INSTANCE.p(x, y));
                    }
                }
                ));

        if (TETRIS_USE_DENSITY) {
            reward("density", 1.0F, new FloatSupplier() {
                @Override
                public float asFloat() {

                    long count = 0L;
                    for (double s : state.grid) {
                        if (s > (double) 0) {
                            count++;
                        }
                    }
                    int filled = (int) count;

                    int r = state.rowsFilled;
                    return r > 0 ? ((float) filled) / (float) (r * state.width) : (float) 0;
                }
            });
        }

        actionUnipolar($.INSTANCE.inh(id, "speed"), new FloatToFloatFunction() {
            @Override
            public float valueOf(float s) {
                int fastest = (int) Tetris.this.timePerFall.min, slowest = (int) Tetris.this.timePerFall.max;
                int t = Math.round(Util.lerp(s, (float) slowest, (float) fastest));
                Tetris.this.timePerFall.set((float) t);
                return Util.unlerp((float) t, (float) slowest, (float) fastest); //get the effective frequency after discretizing
            }
        });


        int[] lastRowsFilled = {0};
        SimpleReward lower = reward("lower", 1.0F, new FloatSupplier() {
            @Override
            public float asFloat() {
                int rowsFilled = state.rowsFilled;
                int deltaRows = rowsFilled - lastRowsFilled[0];
                lastRowsFilled[0] = rowsFilled;
                if (deltaRows > 0) {
                    return -1.0F;
                } else if (deltaRows == 0)
                    return
                            Float.NaN;
                    //0.5f;
                else {//if (deltaRows < 0) {
                    if (deltaRows > 5)
                        return -1.0F; //board clear
                    else
                        return (float) +1; //lower due to line
                }
            }
        });


        Exe.runLater(new Runnable() {
                         @Override
                         public void run() {
                             lower.setDefault($.INSTANCE.t(0.5f, n.confDefault(BELIEF) / 3.0F));
                             //lower.addGuard(true,false);
                         }
                     }
        );

        actionPushButtonLR();
        actionPushButtonRotateFall();

        state.reset();

        onFrame(new Runnable() {
            @Override
            public void run() {
                state.timePerFall = Math.round(Tetris.this.timePerFall.floatValue());
                state.next();
            }
        });


    }

    public static void main(String[] args) {


        GameX.Companion.initC(FPS * thinkPerFrame, new Consumer<NAR>() {
            @Override
            public void accept(NAR n) {

                Tetris t = new Tetris(n, tetris_width, tetris_height);
                n.add(t);

                window(new VectorSensorChart(t.gridVision, t).withControls(), 400, 800);

            }
        });


    }

    private TetrisState actionsReflect(NAR nar) {

        Opjects oo = new Opjects(nar.fork((Term) $.INSTANCE.inh(id, "opjects")));
        oo.exeThresh.set(0.51f);
        Opjects.methodExclusions.add("toVector");

        return oo.a("tetris", TetrisState.class, tetris_width, tetris_height, 2);
    }

    void actionPushButtonLR() {
        GoalActionConcept[] lr = actionPushButtonMutex(tLEFT, tRIGHT,
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return b && state.act(TetrisState.actions.LEFT);
                    }
                },
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return b && state.act(TetrisState.actions.RIGHT);
                    }
                }
        );

    }

    void actionPushButtonRotateFall() {

        actionPushButton(tROT, debounce(new BooleanPredicate() {
                                            @Override
                                            public boolean accept(boolean b) {
                                                return b && state.act(TetrisState.actions.CW);
                                            }
                                        }
                , (float) debounceDurs));

        if (TETRIS_CAN_FALL)
            actionPushButton(tFALL,
                    debounce(new BooleanPredicate() {
                                 @Override
                                 public boolean accept(boolean b) {
                                     return b && state.act(TetrisState.actions.FALL);
                                 }
                             }
                            , (float) (debounceDurs * 2))
            );

    }


    void actionsTriState() {


        actionTriState($.INSTANCE.inh(id, "X"), new IntPredicate() {
            @Override
            public boolean test(int i) {
                switch (i) {
                    case -1:
                        return state.act(TetrisState.actions.LEFT);
                    case +1:
                        return state.act(TetrisState.actions.RIGHT);
                    default:
                    case 0:
                        return true;
                }
            }
        });


        actionPushButton(tROT, new Runnable() {
            @Override
            public void run() {
                state.act(TetrisState.actions.CW);
            }
        });


    }

    private float density() {
        long count = 0L;
        for (double s : state.grid) {
            if (s > (double) 0) {
                count++;
            }
        }
        int filled = (int) count;

        int r = state.rowsFilled;
        return r > 0 ? (float) filled / (float) (r * state.width) : (float) 0;
    }

    public static class TetrisPiece {
        public static int[] EMPTY_ROW = {0, 0, 0, 0, 0};
        public static int[] PAIR1 = {0, 0, 1, 1, 0};
        public static int[] PAIR2 = {0, 1, 1, 0, 0};
        public static int[] CENTER = {0, 0, 1, 0, 0};
        public static int[] MIDDLE = {0, 1, 1, 1, 0};
        public static int[] LINE1 = {0, 1, 1, 1, 1};
        public static int[] LEFT1 = {0, 1, 0, 0, 0};
        public static int[] RIGHT1 = {0, 0, 0, 1, 0};
        int[][][] thePiece = new int[4][5][5];
        int currentOrientation;

        public void setShape(int Direction, int[]... rows) {
            thePiece[Direction] = rows;
        }

        public int[][] getShape(int whichOrientation) {
            return thePiece[whichOrientation];
        }

        @Override
        public String toString() {
            StringBuilder shapeBuffer = new StringBuilder();
            for (int i = 0; i < thePiece[currentOrientation].length; i++) {
                for (int j = 0; j < thePiece[currentOrientation][i].length; j++)
                    shapeBuffer.append(' ').append(thePiece[currentOrientation][i][j]);
                shapeBuffer.append('\n');
            }
            return shapeBuffer.toString();
        }
    }

    public static class TetrisState {
        private final Random randomGenerator = new Random();
        public boolean easy = Tetris.easy.getAcquire();
        public int width;
        public int height;
        public double[] seen;
        public boolean running = true;
        public int currentBlockId;/*which block we're using in the block table*/
        public int currentRotation;
        public int currentX;/* where the falling block is currently*/
        public int currentY;
        public float score;/* what is the current_score*/
        public boolean is_game_over;/*have we reached the end state yet*/
        public double[] grid;/*what the world looks like without the current block*/
        public int time;
        public int timePerFall;
        List<TetrisPiece> possibleBlocks = Tetris.easy.get() ? asList(new TetrisPiece[]{new TetrisPiece() {{
            setShape(0, CENTER_5_X_5);
            setShape(1, CENTER_5_X_5);
            setShape(2, CENTER_5_X_5);
            setShape(3, CENTER_5_X_5);
        }},
        }) : List.of(PossibleBlocks.values()).stream().map(new Function<PossibleBlocks, TetrisPiece>() {
            @Override
            public TetrisPiece apply(PossibleBlocks possibleBlocks1) {
                return possibleBlocks1.shape;
            }
        }).collect(toList());//.subList(0, 1);
        //        CopyOnWriteArrayList<TetrisPiece> possibleBlocks = new CopyOnWriteArrayList<>();
        private int rowsFilled;

        public TetrisState(int width, int height, int timePerFall) {
            this.width = width;
            this.height = height;
            this.timePerFall = timePerFall;
            grid = new double[this.height * this.width];
            seen = new double[width * height];

            reset();
        }

        protected void reset() {
            currentX = width / 2 - 1;
            currentY = 0;
            score = (float) 0;
            Arrays.fill(grid, 0);
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

        private void toVector(boolean monochrome, double[] target) {


            Arrays.fill(target, -1);

            int x = 0;
            for (double i : grid) {
                if (monochrome)
                    target[x] = (double) (i > (double) 0 ? 1.0f : -1.0f);
                else
                    target[x] = i > (double) 0 ?   i : -1.0;
                x++;
            }
            writeCurrentBlock(0.5f, target);
        }

        private void writeCurrentBlock(float color, double[] f) {
            int[][] thisPiece = possibleBlocks.get(currentBlockId).thePiece[currentRotation];

            if (color == -1.0F)
                color = (float) (currentBlockId + 1);
            for (int y = 0; y < thisPiece[0].length; ++y)
                for (int x = 0; x < thisPiece.length; ++x)
                    if (thisPiece[x][y] != 0) {


                        int linearIndex = i(currentX + x, currentY + y);
                        f[linearIndex] = (double) color;
                    }

        }

        public boolean gameOver() {
            return is_game_over;
        }

        public boolean act(TetrisState.actions theAction) {
            return act(theAction, true);
        }

        /* This code applies the action, but doesn't do the default fall of 1 square */
        public boolean act(actions theAction, boolean enable) {
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
                        if (nextRotation < 0) nextRotation = 3;
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
                            if (isInBounds) isColliding = colliding(nextX, nextY, nextRotation);
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

                if (inBounds(nextX, nextY, nextRotation)) if (!colliding(nextX, nextY, nextRotation)) {
                    currentRotation = nextRotation;
                    currentX = nextX;
                    currentY = nextY;
                    return true;
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
        private int i(int x, int y) {
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
            boolean result = false;
            int[][] thePiece = possibleBlocks.get(currentBlockId).thePiece[checkOrientation];
            int ll = thePiece.length;
            try {

                for (int y = 0; y < thePiece[0].length && !result; ++y) {
                    int i = checkY + y;
                    boolean b1 = !(i < 0 || i >= height);
                    for (int x = 0; x < ll && !result; ++x) {
                        int i1 = checkX + x;
                        result = (!b1 || i1 < 0 || i1 >= width || grid[i(i1, i)] != (double) 0) && thePiece[x][y] != 0;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::colliding called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
            }
            return result;
        }

        private boolean collidingCheckOnlySpotsInBounds(int checkX, int checkY, int checkOrientation) {
            boolean result = false;
            int[][] thePiece = possibleBlocks.get(currentBlockId).thePiece[checkOrientation];
            int ll = thePiece.length;
            try {

                for (int y = 0; y < thePiece[0].length && !result; ++y) {
                    int i1 = checkY + y;
                    if (i1 >= 0 && i1 < height)
                        for (int x = 0; x < ll && !result; ++x) {
                            int i = checkX + x;
                            result = thePiece[x][y] != 0 && i >= 0 && i < width && grid[i(i, i1)] != (double) 0;
                        }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::collidingCheckOnlySpotsInBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
            }
            return result;
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
            boolean result = false;
            try {
                int[][] thePiece = possibleBlocks.get(currentBlockId).thePiece[checkOrientation];

                boolean finished = false;
                for (int y = 0; !finished&& y < thePiece[0].length; ++y) {
                    int i1 = checkY + y;
                    boolean b = i1 >= 0 && i1 < height;
                    for (int x = 0; !finished&&x < thePiece.length; ++x) {
                        int i = checkX + x;
                        finished =  ((!b || i < 0 || i >= width) && thePiece[x][y] != 0) ;
                    }
                }

                result |= !finished;

            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::inBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning false from inBounds to help save from error.  Not sure if that's wise.");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
            }

            return result;
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

            if (!inBounds(currentX, currentY, currentRotation))
                System.err.println("In GameState.Java the Current Position of the board is Out Of Bounds... Consistency Check Failed");

            gomezAdamsMethod(!nextInBounds());
        }

        /**
         * casey jones you better watch your speed...
         * @param onSomething is the engineer onSomething that has side effects?
         */
        void gomezAdamsMethod(boolean onSomething) {

            //running NextInbounds without nextColliding appears mutually exclusive

            if (onSomething || nextColliding()) {
                running = false;
                writeCurrentBlock(-1.0F, grid);
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
            if (is_game_over) running = false;

            return currentBlockId;
        }

        protected int nextBlock() {
/*            if (easy) return 1; //square
            else */
            return randomGenerator.nextInt(possibleBlocks.size());

        }

        public void checkScore() {
            int numRowsCleared = 0;
            int rowsFilled = 0;


            for (int y = height - 1; y >= 0; --y)
                if (isRow(y, true)) {
                    removeRow(y);
                    numRowsCleared += 1;
                    y += 1;
                } else if (!isRow(y, false))
                    rowsFilled++;

            int prevRows = this.rowsFilled;
            this.rowsFilled = rowsFilled;


            if (numRowsCleared > 0) {


            } else {

            }


            int diff = prevRows - rowsFilled;

            if (diff >= height - 1) score = Float.NaN;
            else score = (float) diff;
        }

        public float height() {
            return (float) rowsFilled / (float) height;
        }

        /**
         * Check if a row has been completed at height y.
         * Short circuits, returns false whenever we hit an unfilled spot.
         *
         * @param y
         * @return
         */
        public boolean isRow(int y, boolean filledOrClear) {
            int bound = width;
            for (int x = 0; x < bound; x++) {
                double s = grid[i(x, y)];
                if (filledOrClear ? s == (double) 0 : s != (double) 0) {
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


            for (int ty = y; ty > 0; --ty)
                for (int x = 0; x < width; ++x) {
                    int linearIndexTarget = i(x, ty);
                    int linearIndexSource = i(x, ty - 1);
                    grid[linearIndexTarget] = grid[linearIndexSource];
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
                for (int j = 0; j < width; j++) System.out.print(grid[i * width + j]);
                System.out.print("\n");
            }
            System.out.println("-------------");


        }

        protected void next() {
            if (running) update();
            else spawnBlock();

            checkScore();

            toVector(false, seen);

            if (gameOver()) die();


        }

        private float score() {
            return score;
        }

        protected void die() {
            reset();
        }

        public enum actions {

            /**
             * Action value for a move left
             */
            LEFT,
            /**
             * Action value for a move right
             */
            RIGHT,
            /**
             * Action value for a clockwise rotation
             */
            CW,
            /**
             * Action value for a counter clockwise rotation
             */
            CCW,
            /**
             * The no-action Action
             */
            NONE,
            /**
             * fall down
             */
            FALL,
        }

        public enum PossibleBlocks {
            Line(new TetrisPiece() {{
                setShape(0, TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LINE1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                setShape(2, TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LINE1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            Square(new TetrisPiece() {{
                setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            Tri(new TetrisPiece() {{

                setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);


                setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR1
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);


                setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);

                setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
            }}),
            SShape(new TetrisPiece() {{

                setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LEFT1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LEFT1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            ZShape(new TetrisPiece() {{

                setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.LEFT1
                        , TetrisPiece.EMPTY_ROW);
                setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR2
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.LEFT1
                        , TetrisPiece.EMPTY_ROW);
                setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR2
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            LShape(new TetrisPiece() {{

                setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW);

                setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.LEFT1
                        , TetrisPiece.EMPTY_ROW);

                setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);

                setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.RIGHT1
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            JShape(new TetrisPiece() {{
                setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.EMPTY_ROW);
                setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LEFT1
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.RIGHT1
                        , TetrisPiece.EMPTY_ROW);
            }});

            public TetrisPiece shape;

            PossibleBlocks(TetrisPiece shape) {
                this.shape = shape;
            }
        }


    }

}