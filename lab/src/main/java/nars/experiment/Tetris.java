package nars.experiment;

import jcog.math.FloatRange;
import jcog.signal.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.NAgentX;
import nars.op.java.Opjects;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.util.TimeAware;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import static nars.experiment.Tetris.TetrisState.*;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends NAgentX implements Bitmap2D {

    private static final int tetris_width = 8;
    private static final int tetris_height = 16;
    static boolean easy;
    public final FloatRange timePerFall = new FloatRange(2f, 1f, 32f);
    private final Bitmap2DSensor<Bitmap2D> pixels;
    private TetrisState state;

    public Tetris(NAR nar) {
        this(nar, Tetris.tetris_width, Tetris.tetris_height);
    }

    public Tetris(NAR nar, int width, int height) {
        this(nar, width, height, 1);
    }

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(NAR nar, int width, int height, int timePerFall) {
        super("tetris", nar);

        state = new TetrisState(width, height, timePerFall) {
            @Override
            protected int nextBlock() {


                if (easy) {

                    return 1;

                } else {
                    return super.nextBlock();
                }
            }


        };



        addCamera(
                pixels = new Bitmap2DSensor<>(
                        (x, y) -> $.p(id, $.the(x), $.the(y))
                        , this, nar)

        );


        actionsReflect();

        actionsToggle();

        state.reset();


    }

    public static void main(String[] args) {


        TimeAware nn = NAgentX.runRT((n) -> {
            Tetris a = null;


            a = new Tetris(n, Tetris.tetris_width, Tetris.tetris_height);


            //Param.ETERNALIZE_FORGOTTEN_TEMPORALS = true;
            n.freqResolution.set(0.05f);
            //n.confResolution.set(0.01f);
            //n.dtDither.set(5); //for fine-grain Opjects timing


            return a;
        }, 40f);






















































































        /*
        nar.onCycle((n)->{
            FloatSummaryReusableStatistics inputPri = new FloatSummaryReusableStatistics();
            FloatSummaryReusableStatistics derivPri = new FloatSummaryReusableStatistics();
            FloatSummaryReusableStatistics otherPri = new FloatSummaryReusableStatistics();
            n.tasks.forEach(t -> {
                float tp = t.pri();
                if (tp != tp)
                    return;
                if (t.isInput()) {
                    inputPri.accept(tp);
                } else if (t instanceof DerivedTask) {
                    derivPri.accept(tp);
                } else {
                    otherPri.accept(tp);
                }
            });

            System.out.println("input=" + inputPri);
            System.out.println("deriv=" + derivPri);
            System.out.println("other=" + otherPri);
            System.out.println();
        });
        */


    }

    private void actionsReflect() {

        Opjects oo = new Opjects(nar);
        oo.exeThresh.set(0.51f);

        Opjects.methodExclusions.add("toVector");

        state = oo.a("tetris", TetrisState.class, tetris_width, tetris_height, 2);

    }

    void actionsToggle() {
        final Term LEFT = $.the("left");
        final Term RIGHT = $.the("right");
        final Term ROT = $.the("rotate");

        actionPushButtonMutex(LEFT, RIGHT,
                (b) -> state.act(TetrisState.LEFT, b),
                (b) -> state.act(TetrisState.RIGHT, b));

        actionPushButton(ROT, () -> state.act(CW));

    }

    void actionsTriState() {


        actionTriState($.func("X", id), (i) -> {
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

    @Override
    public int width() {
        return state.width;
    }

    @Override
    public int height() {
        return state.height;
    }

    @Override
    public float brightness(int xx, int yy) {
        int index = yy * state.width + xx;
        return state.seen[index] > 0 ? 1f : 0f;
    }

    @Override
    public float act() {

        state.timePerFall = Math.round(timePerFall.floatValue());
        return state.next();

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


        public float[] worldState;/*what the world looks like without the current block*/
        public int time;
        public int timePerFall;
        Vector<TetrisPiece> possibleBlocks = new Vector<>();
        private int rowsFilled;


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

            worldState = new float[this.height * this.width];
            seen = new float[width * height];
            reset();
        }

        public void reset() {
            currentX = width / 2 - 1;
            currentY = 0;
            score = 0;
            for (int i = 0; i < worldState.length; i++) {
                worldState[i] = 0;
            }
            currentRotation = 0;
            is_game_over = false;

            spawnBlock();
            running = true;

        }

        private void toVector(boolean monochrome, float[] target) {


            Arrays.fill(target, -1);

            int x = 0;
            for (double i: worldState) {
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
        public synchronized boolean act(int theAction, boolean enable) {


            int nextRotation = currentRotation;
            int nextX = currentX;
            int nextY = currentY;

            switch (theAction) {
                case CW:
                    nextRotation = (currentRotation + 1) % 4;
                    break;
                case CCW:
                    nextRotation = (currentRotation - 1);
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

        protected boolean act() {
            return act(currentRotation, currentX, currentY);
        }

        protected boolean act(int nextRotation, int nextX, int nextY) {


            if (inBounds(nextX, nextY, nextRotation)) {
                if (!colliding(nextX, nextY, nextRotation)) {
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
                            if (worldState[linearArrayIndex] != 0) {
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


                            if ((checkX + x >= 0 && checkX + x < width && checkY + y >= 0 && checkY + y < height)) {


                                int linearArrayIndex = i(checkX + x, checkY + y);
                                if (worldState[linearArrayIndex] != 0) {
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
                writeCurrentBlock(worldState, -1);
            } else {

                if (time % timePerFall == 0)
                    currentY += 1;
            }

        }

        public int spawnBlock() {
            running = true;

            currentBlockId = nextBlock();

            currentRotation = 0;
            currentX = (width / 2) - 2;
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
            return randomGenerator.nextInt(possibleBlocks.size());
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
            return (((float) rowsFilled) / height);
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
                float s = worldState[i(x, y)];
                if (filledOrClear ? (s == 0) : (s != 0)) {
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
                worldState[linearIndex] = 0;
            }


            for (int ty = y; ty > 0; --ty) {
                for (int x = 0; x < width; ++x) {
                    int linearIndexTarget = i(x, ty);
                    int linearIndexSource = i(x, ty - 1);
                    worldState[linearIndexTarget] = worldState[linearIndexSource];
                }
            }


            for (int x = 0; x < width; ++x) {
                int linearIndex = i(x, 0);
                worldState[linearIndex] = 0;
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
                    System.out.print(worldState[i * width + j]);
                }
                System.out.print("\n");
            }
            System.out.println("-------------");


        }


        protected float next() {
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

            return score();

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




























































































































