package nars.experiment;

import jcog.Config;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import jcog.signal.wave2d.AbstractBitmap2D;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.game.GameTime;
import nars.game.NAct;
import nars.game.action.GoalActionConcept;
import nars.gui.sensor.VectorSensorChart;
import nars.op.java.Opjects;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends GameX {

    static final float FPS = 24f;

    private static final int tetris_width = 8;
    private static final int tetris_height = 16;
    private final boolean canFall;


    private final Bitmap2D grid;


    public final FloatRange timePerFall = new FloatRange(1f, 1f, 8f);
    public final AtomicBoolean easy;

    public final Bitmap2DSensor<Bitmap2D> pixels;
    private final TetrisState state;
    private final Bitmap2DSensor<Bitmap2D> gridVision;


    public Tetris(final NAR nar) {
        this(nar, Tetris.tetris_width, Tetris.tetris_height);
    }

    public Tetris(final NAR nar, final int width, final int height) {
        this(nar, width, height, 1);
    }

    public Tetris(final NAR n, final int width, final int height, final int timePerFall) {
        this(Atomic.the("tetris"), n, width, height, timePerFall);
    }

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(final Term id, final NAR n, final int width, final int height, final int timePerFall) {
        super(id,
                GameTime.fps(FPS),
                //FrameTrigger.durs(1),
                n
        );


        final var opjects = true;
        this.state = opjects ?
                this.actionsReflect(n) :
                new TetrisState(width, height, timePerFall);

        this.easy = this.state.easy;

        this.state.timePerFall = Math.round(this.timePerFall.floatValue());

        this.grid = new AbstractBitmap2D(this.state.width, this.state.height) {
            @Override
            public float brightness(final int x, final int y) {
                return Tetris.this.state.seen[y * this.w + x] > 0 ? 1f : 0f;
            }
        };
        this.pixels = new Bitmap2DSensor<>(
                (x, y) -> $.inh(id, $.p(x, y)),
                //(x, y) -> $.p(GRID,$.the(x), $.the(y)),
                this.grid, /*0,*/ n);
        this.gridVision = this.addSensor(this.pixels);


        this.rewardNormalized("score", 0, ScalarValue.EPSILON, //0 /* ignore decrease */, 1,
                this.state::score
                //new FloatFirstOrderDifference(n::time, state::score).nanIfZero()
        );
        this.rewardNormalized("density", 0, ScalarValue.EPSILON, () -> {

            var filled = 0;
            for (final var s : this.state.grid) if (s > 0) filled++;

            final var r = this.state.rowsFilled;
            return r > 0 ? ((float) filled) / (r * this.state.width) : 0;
        });


        this.actionPushButtonLR();
        this.actionPushButtonRotateFall();

        this.state.reset();

        this.onFrame(() -> {
            this.state.timePerFall = Math.round(this.timePerFall.floatValue());
            this.state.next();
        });

        this.canFall = Config.configIs("TETRIS_CANFALL", false);
    }


    public static void main(final String[] args) {


        GameX.runRT(n -> {

            final var t = new Tetris(n, Tetris.tetris_width, Tetris.tetris_height);
            n.add(t);

            window(new VectorSensorChart(t.gridVision, t), 500, 300);

        }, FPS * 2);


    }

    private TetrisState actionsReflect(final NAR nar) {

        final var oo = new Opjects(nar.fork((Term) $.inh(this.id, "opjects")));
        oo.exeThresh.set(0.51f);

        Opjects.methodExclusions.add("toVector");

        return oo.a("tetris", TetrisState.class, tetris_width, tetris_height, 2);
    }

    final Term tLEFT =
            $.inh(this.id, NAct.NEG);
    final Term tRIGHT =
            $.inh(this.id, NAct.POS);
    final Term tROT =
            $.inh(this.id, "rotate");
    final Term tFALL =
            $.inh(this.id, "fall");

    void actionPushButtonLR() {
        final var lr = this.actionPushButtonMutex(this.tLEFT, this.tRIGHT,
                b -> b && this.state.act(TetrisState.actions.LEFT),
                b -> b && this.state.act(TetrisState.actions.RIGHT)
        );
    }

    void actionPushButtonLR_proportional() {
    }

    void actionPushButtonRotateFall() {

        final var debounceDurs = 2;
        //actionPushButton(ROT, debounce(b -> b && state.act(TetrisState.CW), debounceDurs));
        this.actionPushButton(this.tROT, b -> b && this.state.act(TetrisState.actions.CW));

        if (this.canFall) this.actionPushButton(this.tFALL, this.debounce(b -> b && this.state.act(TetrisState.actions.FALL), debounceDurs * 2));

    }


    void actionsTriState() {


        this.actionTriState($.inh(this.id, "X"), i -> {
            switch (i) {
                case -1:
                    return this.state.act(TetrisState.actions.LEFT);
                case +1:
                    return this.state.act(TetrisState.actions.RIGHT);
                default:
                case 0:
                    return true;
            }
        });


        this.actionPushButton(this.tROT, () -> this.state.act(TetrisState.actions.CW));


    }

    public static class TetrisPiece {
        public static final int[] EMPTY_ROW = {0, 0, 0, 0, 0};
        public static final int[] PAIR1 = {0, 0, 1, 1, 0};
        public static final int[] PAIR2 = {0, 1, 1, 0, 0};
        public static final int[] CENTER = {0, 0, 1, 0, 0};
        public static final int[] MIDDLE = {0, 1, 1, 1, 0};
        public static final int[] LINE1 = {0, 1, 1, 1, 1};
        public static final int[] LEFT1 = {0, 1, 0, 0, 0};
        public static final int[] RIGHT1 = {0, 0, 0, 1, 0};
        int[][][] thePiece = new int[4][5][5];
        int currentOrientation;

        public void setShape(final int Direction, final int[]... rows) {
            this.thePiece[Direction] = rows;
        }

        @Override
        public String toString() {
            final var shapeBuffer = new StringBuilder();
            for (var i = 0; i < this.thePiece[this.currentOrientation].length; i++) {
                for (var j = 0; j < this.thePiece[this.currentOrientation][i].length; j++)
                    shapeBuffer.append(' ').append(this.thePiece[this.currentOrientation][i][j]);
                shapeBuffer.append('\n');
            }
            return shapeBuffer.toString();
        }
    }

    public static class TetrisState {
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

        private final Random randomGenerator = new Random();
        public int width;
        public int height;
        public float[] seen;
        public boolean running = true;
        public int currentBlockId;/*which block we're using in the block table*/

        public AtomicInteger currentRotation = new AtomicInteger();
        public final AtomicInteger currentX = new AtomicInteger();/* where the falling block is currently*/

        public final AtomicInteger currentY = new AtomicInteger();
        public float score;/* what is the current_score*/

        public boolean is_game_over;/*have we reached the end state yet*/


        public float[] grid;/*what the world looks like without the current block*/
        public int time;
        public int timePerFall;
        //        CopyOnWriteArrayList<TetrisPiece> possibleBlocks = new CopyOnWriteArrayList<>();
        private int rowsFilled;

        public final AtomicBoolean easy = new AtomicBoolean(false);

        public enum PossibleBlocks {
            Line(new TetrisPiece() {{
                this.setShape(0, TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LINE1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(2, TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LINE1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            Square(new TetrisPiece() {{
                this.setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            Tri(new TetrisPiece() {{

                this.setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);


                this.setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR1
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);


                this.setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);

                this.setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
            }}),
            SShape(new TetrisPiece() {{

                this.setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LEFT1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LEFT1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.PAIR2
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            ZShape(new TetrisPiece() {{

                this.setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.LEFT1
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR2
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.LEFT1
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR2
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            LShape(new TetrisPiece() {{

                this.setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR1
                        , TetrisPiece.EMPTY_ROW);

                this.setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.LEFT1
                        , TetrisPiece.EMPTY_ROW);

                this.setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR2
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);

                this.setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.RIGHT1
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
            }}),
            JShape(new TetrisPiece() {{
                this.setShape(0, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.PAIR2
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(1, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.LEFT1
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(2, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.PAIR1
                        , TetrisPiece.CENTER
                        , TetrisPiece.CENTER
                        , TetrisPiece.EMPTY_ROW);
                this.setShape(3, TetrisPiece.EMPTY_ROW
                        , TetrisPiece.EMPTY_ROW
                        , TetrisPiece.MIDDLE
                        , TetrisPiece.RIGHT1
                        , TetrisPiece.EMPTY_ROW);
            }});

            public TetrisPiece shape;

            PossibleBlocks(final TetrisPiece shape) {
                this.shape = shape;
            }
        }

        List<PossibleBlocks> possibleBlocks = Arrays.asList(PossibleBlocks.values());

        public TetrisState(final int width, final int height, final int timePerFall) {
            this.width = width;
            this.height = height;
            this.timePerFall = timePerFall;

            this.grid = new float[this.height * this.width];
            this.seen = new float[width * height];
            this.reset();
        }

        protected void reset() {
            this.currentX.set(this.width / 2 - 1);
            this.currentY.set(0);
            this.score = 0;
            for (var i = 0; i < this.grid.length; i++) this.grid[i] = 0;
            this.currentRotation.set(0);
            this.is_game_over = false;

            this.running = true;
            this.restart();

            this.spawnBlock();
        }

        /**
         * do nothing method for signaling to NAR restart occurred, but not to allow it to trigger an actual restart
         */
        public void restart() {

        }

        private void toVector(final boolean monochrome, final float[] target) {


            Arrays.fill(target, -1);

            var x = 0;
            for (final double i : this.grid) {
                if (monochrome)
                    target[x] = i > 0 ? 1.0f : -1.0f;
                else
                    target[x] = i > 0 ? (float) i : -1.0f;
                x++;
            }

            this.writeCurrentBlock(target, 0.5f);


        }


        private void writeCurrentBlock(final float[] f, final float color) {
            var color1 = color;
            final var thisPiece = PossibleBlocks.values()[this.currentBlockId].shape.thePiece[this.currentRotation.get()];

            if (color1 == -1)
                color1 = this.currentBlockId + 1;
            for (var y = 0; y < thisPiece[0].length; ++y)
                for (var x = 0; x < thisPiece.length; ++x)
                    if (thisPiece[x][y] != 0) {


                        final var linearIndex = this.i(this.currentX.get() + x, this.currentY.get() + y);
                        /*if(linearIndex<0){
                            System.err.printf("Bogus linear index %d for %d + %d, %d + %d\n",linearIndex,currentX,x,currentY,y);
                            Thread.dumpStack();
                            System.exit(1);
                        }*/
                        f[linearIndex] = color1;
                    }

        }

        public boolean gameOver() {
            return this.is_game_over;
        }

        public boolean act(final TetrisState.actions theAction) {
            return this.act(theAction, true);
        }

        /* This code applies the action, but doesn't do the default fall of 1 square */
        public boolean act(final actions theAction, final boolean enable) {
            /*synchronized (this)*/
            final var nextRotation = this.currentRotation;
            var nextX = this.currentX.get();
            var nextY = this.currentY.get();

            switch (theAction) {
                case CW:
                    nextRotation.set((this.currentRotation.get() + 1) % 4);
                    break;
                case CCW:
                    nextRotation.set(this.currentRotation.get() - 1);
                    if (nextRotation.get() < 0) nextRotation.set(3);
                    break;
                case LEFT:
                    nextX = enable ? this.currentX.get() - 1 : this.currentX.get();
                    break;
                case RIGHT:
                    nextX = enable ? this.currentX.get() + 1 : this.currentX.get();
                    break;
                case FALL:
                    nextY = this.currentY.get();

                    var isInBounds = true;
                    var isColliding = false;


                    while (isInBounds && !isColliding) {
                        nextY++;
                        isInBounds = this.inBounds(nextX, nextY, nextRotation);
                        if (isInBounds) isColliding = this.colliding(nextX, nextY, nextRotation);
                    }
                    nextY--;
                    break;
                default:
                    throw new RuntimeException("unknown action");
            }
            return this.act(nextRotation, nextX, nextY);
        }

        protected boolean act() {
            return this.act(this.currentRotation, this.currentX.get(), this.currentY.get());
        }

        protected boolean act(final AtomicInteger nextRotation, final int nextX, final int nextY) {
            var result = false;

            /*synchronized (this)*/

            if (this.inBounds(nextX, nextY, nextRotation) && !this.colliding(nextX, nextY, nextRotation)) {
                this.currentRotation.set(nextRotation.getAcquire());
                this.currentX.set(nextX);
                this.currentY.set(nextY);
                result = true;
            }

            return result;
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
        private final int i(final int x, final int y) {
            return y * this.width + x;


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
        private boolean colliding(final int checkX, final int checkY, final AtomicInteger checkOrientation) {
            final var thePiece = this.possibleBlocks.get(this.currentBlockId).shape.thePiece[checkOrientation.getAcquire()];
            final var ll = thePiece.length;
            try {

                for (var y = 0; y < thePiece[0].length; ++y)
                    for (var x = 0; x < ll; ++x)
                        if (thePiece[x][y] != 0) {
                            if (checkY + y < 0 || checkX + x < 0 || checkY + y >= this.height || checkX + x >= this.width)
                                return true;
                            final var linearArrayIndex = this.i(checkX + x, checkY + y);
                            if (this.grid[linearArrayIndex] != 0) return true;
                        }
                return false;

            } catch (final ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::colliding called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                this.is_game_over = true;
                return true;
            }
        }

        private boolean collidingCheckOnlySpotsInBounds(final int checkX, final int checkY, final AtomicInteger checkOrientation) {
            final var thePiece = this.possibleBlocks.get(this.currentBlockId).shape.thePiece[checkOrientation.getAcquire()];
            final var ll = thePiece.length;
            try {

                return IntStream.range(0, thePiece[0].length).anyMatch(y -> IntStream.range(0, ll).anyMatch(x -> thePiece[x][y] != 0 &&
                        checkX + x >= 0 &&
                        checkX + x < this.width &&
                        checkY + y >= 0 &&
                        checkY + y < this.height &&
                        this.grid[this.i(checkX + x, checkY + y)] != 0));

            } catch (final ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::collidingCheckOnlySpotsInBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                this.is_game_over = true;
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
        private boolean inBounds(final int checkX, final int checkY, final AtomicInteger checkOrientation) {
            try {
                final var thePiece = this.possibleBlocks.get(this.currentBlockId).shape.thePiece[checkOrientation.getAcquire()];
                return IntStream.range(0, thePiece[0].length).noneMatch(y -> IntStream.range(0, thePiece.length).anyMatch(x -> thePiece[x][y] != 0 && !(checkX + x >= 0 && checkX + x < this.width && checkY + y >= 0 && checkY + y < this.height)));
            } catch (final ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::inBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning false from inBounds to help save from error.  Not sure if that's wise.");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                this.is_game_over = true;
                return false;
            }

        }

        public boolean nextInBounds() {
            return this.inBounds(this.currentX.get(), this.currentY.get() + 1, this.currentRotation);
        }

        public boolean nextColliding() {
            return this.colliding(this.currentX.get(), this.currentY.get() + 1, this.currentRotation);
        }

        /*Ok, at this point, they've just taken their action.  We now need to make them fall 1 spot, and check if the game is over, etc */
        private void update() {
            this.act();
            this.time++;
            if (!this.inBounds(this.currentX.get(), this.currentY.get(), this.currentRotation))
                System.err.println("In GameState.Java the Current Position of the board is Out Of Bounds... Consistency Check Failed");
            final var onSomething = !this.nextInBounds() || this.nextColliding();//onSomething = true;
            if (onSomething) {
                this.running = false;
                this.writeCurrentBlock(this.grid, -1);
            } else if (this.time % this.timePerFall == 0)
                this.currentY.addAndGet(1);
        }

        public int spawnBlock() {
            this.running = true;
            this.currentBlockId = this.nextBlock();
            this.currentRotation.set(0);
            this.currentX.set(this.width / 2 - 2);
            this.currentY.set(-4);
            var hitOnWayIn = false;
            while (!this.inBounds(this.currentX.get(), this.currentY.get(), this.currentRotation)) {
                hitOnWayIn = this.collidingCheckOnlySpotsInBounds(this.currentX.get(), this.currentY.get(), this.currentRotation);
                this.currentY.getAndIncrement();
            }
            this.is_game_over = this.colliding(this.currentX.get(), this.currentY.get(), this.currentRotation) || hitOnWayIn;
            if (this.is_game_over) this.running = false;

            return this.currentBlockId;
        }

        protected int nextBlock() {
            return this.easy.get() ? 1 : this.randomGenerator.nextInt(this.possibleBlocks.size()); //square

        }

        public void checkScore() {
            var numRowsCleared = 0;
            var rowsFilled = 0;


            for (var y = this.height - 1; y >= 0; --y)
                if (this.isRow(y, true)) {
                    this.removeRow(y);
                    numRowsCleared += 1;
                    y += 1;
                } else if (!this.isRow(y, false))
                    rowsFilled++;

            final var prevRows = this.rowsFilled;
            this.rowsFilled = rowsFilled;
            final var diff = prevRows - rowsFilled;

            this.score = diff >= this.height - 1 ? Float.NaN : diff;
        }

        public float height() {
            return (float) this.rowsFilled / this.height;
        }

        /**
         * Check if a row has been completed at height y.
         * Short circuits, returns false whenever we hit an unfilled spot.
         */
        public boolean isRow(final int y, final boolean filledOrClear) {
            var result = true;
            for (var x = 0; x < this.width; ++x) {
                final var s = this.grid[this.i(x, y)];
                if (filledOrClear ? s == 0 : s != 0) {
                    result = false;
                    break;
                }
            }
            return result;
        }


        /**
         * Dec 13/07.  Radkie + Tanner found 2 bugs here.
         * Bug 1: Top row never gets updated when removing lower rows. So, if there are
         * pieces in the top row, and we clear something, they will float there.
         *
         * @param y
         */
        void removeRow(final int y) {
            if (this.isRow(y, true)) {
                for (var x = 0; x < this.width; ++x) {
                    final var linearIndex = this.i(x, y);
                    this.grid[linearIndex] = 0;
                }


                for (var ty = y; ty > 0; --ty)
                    for (var x = 0; x < this.width; ++x) {
                        final var linearIndexTarget = this.i(x, ty);
                        final var linearIndexSource = this.i(x, ty - 1);
                        this.grid[linearIndexTarget] = this.grid[linearIndexSource];
                    }


                for (var x = 0; x < this.width; ++x) {
                    final var linearIndex = this.i(x, 0);
                    this.grid[linearIndex] = 0;
                }
            } else
                System.err.println("In GameState.java remove_row you have tried to remove a row which is not complete. Failed to remove row");

        }


        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }


        public int getCurrentPiece() {
            return this.currentBlockId;
        }

        /**
         * Utility methd for debuggin
         */
        public void printState() {
            final var index = 0;
            for (var i = 0; i < this.height - 1; i++) {
                for (var j = 0; j < this.width; j++) System.out.print(this.grid[i * this.width + j]);
                System.out.print("\n");
            }
            System.out.println("-------------");
        }


        protected void next() {
            if (this.running) this.update();
            else this.spawnBlock();
            this.checkScore();
            this.toVector(false, this.seen);
            if (this.gameOver()) this.die();
        }

        private float score() {
            return this.score;
        }

        protected void die() {
            this.reset();
        }
    }
}