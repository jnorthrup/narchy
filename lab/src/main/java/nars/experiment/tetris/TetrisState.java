/*
Copyright 2007 Brian Tanner
http://rl-library.googlecode.com/
brian@tannerpages.com
http://brian.tannerpages.com

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package nars.experiment.tetris;

import jcog.Util;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;


public class TetrisState {
    /*Action values*/
    public static final int LEFT = 0; /*Action value for a move left*/
    public static final int RIGHT = 1; /*Action value for a move right*/
    public static final int CW = 2; /*Action value for a clockwise rotation*/
    public static final int CCW = 3; /*Action value for a counter clockwise rotation*/
    public static final int NONE = 4; /*The no-action Action*/
    public static final int FALL = 5; /* fall down */


    public int width;
    public int height;
    public float[] seen;

    private final Random randomGenerator = new Random();

    public boolean running = true;
    public int currentBlockId;/*which block we're using in the block table*/

    public int currentRotation;
    public int currentX;/* where the falling block is currently*/

    public int currentY;
    public float score;/* what is the current_score*/

    public boolean is_game_over;/*have we reached the end state yet*/


    public float[] worldState;/*what the world looks like without the current block*/

    //	/*Hold all the possible bricks that can fall*/
    Vector<TetrisPiece> possibleBlocks = new Vector<>();
    public int time;

    public int timePerFall;
    private int rowsFilled;


    //private double[] worldObservation;



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

        worldState=new float[this.height * this.width];
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
        //eget observation with only the state space

        Arrays.fill(target, -1);

        int x = 0;
        for (double i : worldState) {
            if (monochrome)
                target[x] = i > 0 ? 1.0f : -1.0f;
            else
                target[x] = i > 0 ? (float)i : - 1.0f;
            x++;
        }

        writeCurrentBlock(target, 0.5f);


//            //Set the bit vector value for which block is currently following
//            target[worldState.length + currentBlockId] = 1;

    }


    private void writeCurrentBlock(float[] f, float color) {
        int[][] thisPiece = possibleBlocks.get(currentBlockId).getShape(currentRotation);

        if (color == -1)
            color = currentBlockId + 1;
        for (int y = 0; y < thisPiece[0].length; ++y) {
            for (int x = 0; x < thisPiece.length; ++x) {
                if (thisPiece[x][y] != 0) {
                    //Writing currentBlockId +1 because blocks are 0 indexed, and we want spots to be
                    //0 if they are clear, and >0 if they are not.
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

    /* This code applies the action, but doesn't do the default fall of 1 square */
    public synchronized boolean act(int theAction) {


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
                nextX = currentX - 1;
                break;
            case RIGHT:
                nextX = currentX + 1;
                break;
            case FALL:
                nextY = currentY;

                boolean isInBounds = true;
                boolean isColliding = false;

                //Fall until you hit something then back up once
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

        //Check if the resulting position is legal. If so, accept it.
        //Otherwise, don't change anything
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
     * @param x
     * @param y
     * @return
     */
    private final int i(int x, int y) {
        return y * width + x;
        //assert returnValue >= 0 : " "+y+" * "+worldWidth+" + "+x+" was less than 0.";
        //return returnValue;
    }
//    final int x(int i) {
//        return i % width;
//    }
//    final int y(int i) {
//        return i / width;
//    }

    /**
     * Check if any filled part of the 5x5 block array is either out of bounds
     * or overlapping with something in wordState
     * @param checkX X location of the left side of the 5x5 block array
     * @param checkY Y location of the top of the 5x5 block array
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
                        //First check if a filled in piece of the block is out of bounds!
                        //if the height of this square is negative or the X of
                        //this square is negative, then we're "colliding" with the wall
                        if (checkY + y < 0 || checkX + x < 0) {
                            return true;
                        }

                        //if the height of this square is more than the board size or the X of
                        //this square is more than the board size, then we're "colliding" with the wall
                        if (checkY + y >= height || checkX + x >= width) {
                            return true;
                        }

                        //Otherwise check if it hits another piece
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

                        //This checks to see if x and y are in bounds
                        if ((checkX + x >= 0 && checkX + x < width && checkY + y >= 0 && checkY + y < height)) {
                            //This array location is in bounds
                            //Check if it hits another piece
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
     * @param checkX X location of the left side of the 5x5 block array
     * @param checkY Y location of the top of the 5x5 block array
     * @param checkOrientation Orientation of the block to check
     * @return
     */
    private boolean inBounds(int checkX, int checkY, int checkOrientation) {
        try {
            int[][] thePiece = possibleBlocks.get(currentBlockId).getShape(checkOrientation);

            for (int y = 0; y < thePiece[0].length; ++y) {
                for (int x = 0; x < thePiece.length; ++x) {
                    if (thePiece[x][y] != 0) {
                        //if ! (thisX is non-negative AND thisX is less than width
                        // AND thisY is non-negative AND thisY is less than height)
                        //Through demorgan's law is
                        //if thisX is negative OR thisX is too big or
                        //thisY is negative OR this Y is too big
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

        // Sanity check.  The game piece should always be in bounds.
        if (!inBounds(currentX, currentY, currentRotation)) {
            System.err.println("In GameState.Java the Current Position of the board is Out Of Bounds... Consistency Check Failed");
        }

        //Need to be careful here because can't check nextColliding if not in bounds

        //onSomething means we're basically done with this piece
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
            //fall
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

        //score += getWidth() / 2;

//Colliding checks both bounds and piece/piece collisions.  We really only want the piece to be falling
//If the filled parts of the 5x5 piece are out of bounds.. IE... we want to stop falling when its all on the screen
        boolean hitOnWayIn = false;
        while (!inBounds(currentX, currentY, currentRotation)) {
            //We know its not in bounds, and we're bringing it in.  Let's see if it would have hit anything...
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


        //Start at the bottom, work way up
        for (int y = height - 1; y >= 0; --y) {
            if (isRow(y, true)) {
                removeRow(y);
                numRowsCleared += 1;
                y += 1;
            } else {
                if (!isRow(y,false))
                    rowsFilled++;
            }
        }

        int prevRows = this.rowsFilled;
        this.rowsFilled = rowsFilled;


        if (numRowsCleared > 0) {
            //1 line == 1
            //2 lines == 2
            //3 lines == 4
            //4 lines == 8
            //score = numRowsCleared;
        } else {
            //score = 0;
        }
        //score -= (((float)rowsFilled) / height) * 0.5f; //penalty for height


        int diff = prevRows - rowsFilled;
        if (diff >= height-1)
            score = -1; //no reward for dying
        else
            score = Util.clamp(diff /3f, -1, 1);
    }

    public float height() {
        return (((float)rowsFilled) / height);
    }

    /**
     * Check if a row has been completed at height y.
     * Short circuits, returns false whenever we hit an unfilled spot.
     * @param y
     * @return
     */
    public boolean isRow(int y, boolean filledOrClear) {
        for (int x = 0; x < width; ++x) {
            float s = worldState[i(x, y)];
            if (filledOrClear ? (s==0) : (s != 0)) {
                return false;
            }
        }
        return true;
    }



    /**
     * Dec 13/07.  Radkie + Tanner found 2 bugs here.
     * Bug 1: Top row never gets updated when removing lower rows. So, if there are 
     * pieces in the top row, and we clear something, they will float there.
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

        //Copy each row down one (except the top)
        for (int ty = y; ty > 0; --ty) {
            for (int x = 0; x < width; ++x) {
                int linearIndexTarget = i(x, ty);
                int linearIndexSource = i(x, ty - 1);
                worldState[linearIndexTarget] = worldState[linearIndexSource];
            }
        }

        //Clear the top row
        for (int x = 0; x < width; ++x) {
            int linearIndex = i(x, 0);
            worldState[linearIndex] = 0;
        }

    }

//    public int numEmptyRows() {
//        int t = 0;
//        for (int y = 0; y < getHeight(); y++)
//            if (isRow(y,false))
//                t++;
//        return t;
//    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

//    public int[] getNumberedStateSnapShot() {
//        int[] numberedStateCopy = new int[worldState.length];
//        for (int i = 0; i < worldState.length; i++) {
//            numberedStateCopy[i] = worldState[i];
//        }
//        writeCurrentBlock(numberedStateCopy);
//        return numberedStateCopy;
//
//    }

    public int getCurrentPiece() {
        return currentBlockId;
    }

    /**
     * Utility methd for debuggin
     *
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

    private Random getRandom() {
        return randomGenerator;
    }

    public void next() {
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

    protected void die() {
        reset();
    }

//    /*End of Tetris Helper Functions*/
//
//    public TetrisState(TetrisState stateToCopy) {
//        blockMobile = stateToCopy.blockMobile;
//        currentBlockId = stateToCopy.currentBlockId;
//        currentRotation = stateToCopy.currentRotation;
//        currentX = stateToCopy.currentX;
//        currentY = stateToCopy.currentY;
//        score = stateToCopy.score;
//        is_game_over = stateToCopy.is_game_over;
//        width = stateToCopy.width;
//        height = stateToCopy.height;
//
//        worldState = new float[stateToCopy.worldState.length];
//        System.arraycopy(stateToCopy.worldState, 0, worldState, 0, worldState.length);
//
//        possibleBlocks = new Vector<>();
//        //hopefully nobody modifies the pieces as they go
//        possibleBlocks.addAll(stateToCopy.possibleBlocks.stream().collect(Collectors.toList()));
//
//    }
}

