package nars.experiment.connect4;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.NAR;
import nars.Narsese;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static nars.experiment.connect4.C4.dropConcept;


/**
 * Simple graphical Connect Four game application. It demonstrates the Minimax
 * algorithm with alpha-beta pruning, iterative deepening, and action ordering.
 * The implemented action ordering strategy tries to maximize the impact of the
 * chosen action for later game phases.
 *
 * @author Ruediger Lunde
 * from: AIMA-Java
 */
public class ConnectFour {

    public static JFrame constructApplicationFrame(ConnectFourState game) {
        JFrame frame = new JFrame();
        JPanel panel = new ConnectFourPanel(game);
        frame.add(panel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }


    /**
     * A state of the Connect Four game is characterized by a board containing a
     * grid of spaces for disks, the next player to move, and some utility
     * informations. A win position for a player x is an empty space which turns a
     * situation into a win situation for x if he is able to place a disk there.
     *
     * @author Ruediger Lunde
     */
    public static class ConnectFourState {

        private static final String[] players = new String[]{"red", "yellow"};

        public final int cols;
        final int rows;
        /**
         * Uses special bit coding. First bit: disk of player 1, second bit: disk of
         * player 2, third bit: win position for player 1, fourth bit: win position
         * for player 2.
         */
        private final byte[] board;
        private final XoRoShiRo128PlusRandom rng;
        int winPositions1;
        int winPositions2;
        private final AtomicInteger moveCount = new AtomicInteger();
        private volatile int invalidCount;
        /**
         * Indicates the utility of the state. 1: win for player 1, 0: win for
         * player 2, 0.5: draw, -1 for all non-terminal states.
         */
        private double utility;

        public ConnectFourState() {
            this(6, 7);
        }

        ConnectFourState(int rows, int cols) {
            this.rng = new XoRoShiRo128PlusRandom();
            this.cols = cols;
            this.rows = rows;
            board = new byte[rows * cols];
            clear();
        }

        public void clear() {
            Arrays.fill(board, (byte) 0);
            utility = -1;
            moveCount.set(rng.nextBoolean() ? 0 : 1);
            invalidCount = winPositions1 = winPositions2 = 0;
        }

        private double utility() {
            return utility;
        }

        int get(int row, int col) {
            return board[row * cols + col] & 3;
        }

        protected int moving() {
            int m = moveCount.get() % 2 + 1;

            return m;
        }


        public int invalidCount() {
            return invalidCount;
        }

        public synchronized boolean drop(int col, int whoamI) {
            int playerNum = moving();
            if (playerNum != whoamI) {
                invalidMove();
                return false;
            }
            int row = freeRow(col);
            if (row != -1) {
                moveCount.getAndIncrement();
                if (moveCount.get() == board.length)
                    utility = 0.5;
                if (isWinPositionFor(row, col, 1)) {
                    winPositions1--;
                    if (playerNum == 1)
                        utility = 1.0;
                }
                if (isWinPositionFor(row, col, 2)) {
                    winPositions2--;
                    if (playerNum == 2)
                        utility = 0.0;
                }
                set(row, col, playerNum);
                if (utility == -1)
                    analyzeWinPositions(row, col);
                return true;
            } else {
                invalidMove();
                return false;
            }
        }

        void invalidMove() {
            invalidCount++;
        }

        void set(int row, int col, int playerNum) {
            board[row * cols + col] = (byte) playerNum;
        }

        /**
         * Returns the row of the first empty space in the specified column and -1
         * if the column is full.
         */
        private int freeRow(int col) {
            for (int row = rows - 1; row >= 0; row--)
                if (get(row, col) == 0)
                    return row;
            return -1;
        }

        public boolean isWinMoveFor(int col, int playerNum) {
            return isWinPositionFor(freeRow(col), col, playerNum);
        }

        boolean isWinPositionFor(int row, int col, int playerNum) {
            return (board[row * cols + col] & playerNum * 4) > 0;
        }

        private void setWinPositionFor(int row, int col, int playerNum) {
            if (playerNum == 1) {
                if (!isWinPositionFor(row, col, 1))
                    winPositions1++;
            } else if (playerNum == 2) {
                if (!isWinPositionFor(row, col, 2))
                    winPositions2++;
            } else {
                throw new IllegalArgumentException("Wrong player number.");
            }
            board[row * cols + col] |= playerNum * 4;
        }

        /**
         * Assumes a disk at position <code>moveRow</code> and <code>moveCol</code>
         * and analyzes the vicinity with respect to win positions.
         */
        private void analyzeWinPositions(int moveRow, int moveCol) {
            final int[] rowIncr = new int[]{1, 0, 1, 1};
            final int[] colIncr = new int[]{0, 1, -1, 1};
            int playerNum = get(moveRow, moveCol);
            WinPositionInfo[] wInfo = new WinPositionInfo[]{
                    new WinPositionInfo(), new WinPositionInfo()};
            for (int i = 0; i < 4; i++) {
                int rIncr = rowIncr[i];
                int cIncr = colIncr[i];
                int diskCount = 1;

                for (int j = 0; j < 2; j++) {
                    WinPositionInfo wInf = wInfo[j];
                    wInf.clear();
                    int rBound = rIncr > 0 ? rows : -1;
                    int cBound = cIncr > 0 ? cols : -1;

                    int row = moveRow + rIncr;
                    int col = moveCol + cIncr;
                    while (row != rBound && col != cBound) {
                        int plNum = get(row, col);
                        if (plNum == playerNum) {
                            if (wInf.hasData())
                                wInf.diskCount++;
                            else
                                diskCount++;
                        } else if (plNum == 0) {
                            if (!wInf.hasData()) {
                                wInf.row = row;
                                wInf.col = col;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                        row += rIncr;
                        col += cIncr;
                    }
                    rIncr = -rIncr;
                    cIncr = -cIncr;
                }
                for (int j = 0; j < 2; j++) {
                    WinPositionInfo wInf = wInfo[j];
                    if (wInf.hasData() && diskCount + wInf.diskCount >= 3) {
                        setWinPositionFor(wInf.row, wInf.col, playerNum);
                    }
                }
            }
        }


        public boolean isTerminal() {
            return utility() != -1;
        }


        private double getUtility(String player) {
            ConnectFourState state = this;
            double result = state.utility();
            if (result != -1) {
                if (Objects.equals(player, players[1]))
                    result = 1 - result;
            } else {
                throw new IllegalArgumentException("State is not terminal.");
            }
            return result;
        }


        private static class WinPositionInfo {
            int row = -1;
            int col = -1;
            int diskCount;

            void clear() {
                row = -1;
                col = -1;
                diskCount = 0;
            }

            boolean hasData() {
                return row != -1;
            }
        }

        public static class Play {

            protected int player;
            protected ConnectFourState game;


            /**
             * TODO not public
             */
            protected void init(ConnectFourState game, int player) {
                this.player = player;
                this.game = game;
            }

            public void moving(String who, boolean really) {

            }

            public boolean drop(int which) {

                int moving = game.moving();
                if (moving != player) {
                    notMoving(player);
                    return false;
                } else {
                    return game.drop(which, player);
                }

            }

            /**
             * signal attempted move out of turn
             */
            public void notMoving(int player) {

            }

            public int whoWon() {
                synchronized (game) {

                    if (isTerminal()) {
                        if (game.getUtility(ConnectFourState.players[0]) == 1) {
                            return 1;
                        } else if (game.getUtility(ConnectFourState.players[1]) == 1) {
                            return 2;
                        }
                    }

                    return 0;
                }
            }

            public void see() {


                for (int r = 0; r < game.rows; r++)
                    for (int c = 0; c < game.cols; c++) {
                        int x = game.get(r, c);
                        board(r, c, "emt", 0 == x);
                        board(r, c, "red", 1 == x);
                        board(r, c, "yel", 2 == x);
                    }


            }

            public void board(int r, int c, String what, boolean isIt) {

            }

            public boolean isTerminal() {
                return game.isTerminal();
            }

            public void clear() {
                game.clear();
            }

            protected void tryDrop(NAR nar, int which) {
                try {
                    nar.input(dropConcept(which, nar, true, game, player) +
                            "! |"

                    );
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    /**
     * Simple panel to control the game.
     */
    private static class ConnectFourPanel extends JPanel implements ActionListener {
        final JButton clearButton;

        final JLabel statusBar;

        final ConnectFourState game;


        /**
         * Standard constructor.
         */
        ConnectFourPanel(ConnectFourState game) {

            this.game = game;
            setLayout(new BorderLayout());
            setBackground(Color.BLUE);

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.add(Box.createHorizontalGlue());
            clearButton = new JButton("Clear");
            clearButton.addActionListener(this);
            toolBar.add(clearButton);

            add(toolBar, BorderLayout.NORTH);

            int rows = game.rows;
            int cols = game.cols;
            JPanel boardPanel = new JPanel();
            boardPanel.setLayout(new GridLayout(rows, cols, 5, 5));
            boardPanel.setBorder(BorderFactory.createEtchedBorder());
            boardPanel.setBackground(Color.BLUE);
            for (int i = 0; i < rows * cols; i++) {
                GridElement element = new GridElement(i / cols, i % cols);
                boardPanel.add(element);
                element.addActionListener(this);
            }
            add(boardPanel, BorderLayout.CENTER);

            statusBar = new JLabel(" ");
            statusBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(statusBar, BorderLayout.SOUTH);

            updateStatus();
        }

        /**
         * Handles all button events and updates the view.
         */
        @Override
        public void actionPerformed(ActionEvent e) {


            repaint();


        }

        /**
         * Uses adversarial search for selecting the next action.
         */


        /**
         * Updates the status bar.
         */
        protected int updateStatus() {
            int won = 0;
            String statusText;
            if (!game.isTerminal()) {
                String toMove = ConnectFourState.players[game.moving() - 1];
                statusText = "Next move: " + toMove;
                statusBar.setForeground(toMove.equals("red") ? Color.RED
                        : Color.YELLOW);
            } else {
                String winner = null;
                for (int i = 0; i < 2; i++)
                    if (game.getUtility(ConnectFourState.players[i]) == 1) {
                        winner = ConnectFourState.players[i];
                        won = i;
                    }
                if (winner != null)
                    statusText = "Color " + winner
                            + " has won. Congratulations!";
                else
                    statusText = "No winner :-(";
                statusBar.setForeground(Color.WHITE);
            }


            statusBar.setText(statusText);
            return won;
        }

        /**
         * Represents a space within the grid where discs can be placed.
         */
        @SuppressWarnings("serial")
        private class GridElement extends JButton {
            final int row;
            final int col;

            GridElement(int row, int col) {
                this.row = row;
                this.col = col;
                setBackground(Color.BLUE);
            }

            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                int playerNum = game.get(row, col);
                if (playerNum != 0) {
                    drawDisk(g, playerNum);
                }
                for (int pNum = 1; pNum <= 2; pNum++)
                    if (game.isWinPositionFor(row, col, pNum))
                        drawWinSituation(g, pNum);
            }

            /**
             * Fills a simple oval.
             */
            void drawDisk(Graphics g, int playerNum) {
                int size = Math.min(getWidth(), getHeight());
                g.setColor(playerNum == 1 ? Color.RED : Color.YELLOW);
                g.fillOval((getWidth() - size) / 2, (getHeight() - size) / 2,
                        size, size);
            }

            /**
             * Draws a simple oval.
             */
            void drawWinSituation(Graphics g, int playerNum) {
                int size = Math.min(getWidth(), getHeight());
                g.setColor(playerNum == 1 ? Color.RED : Color.YELLOW);
                g.drawOval((getWidth() - size) / 2 + playerNum,
                        (getHeight() - size) / 2 + playerNum, size - 2
                                * playerNum, size - 2 * playerNum);
            }
        }
    }

}



















































