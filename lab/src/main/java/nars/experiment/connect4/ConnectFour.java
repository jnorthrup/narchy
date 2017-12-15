package nars.experiment.connect4;

import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.op.java.Opjects;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

//import aima.core.environment.connectfour.ConnectFourAIPlayer;
//import aima.core.environment.connectfour.ConnectFourGame;
//import aima.core.environment.connectfour.ConnectFourState;
//import aima.core.search.adversarial.AdversarialSearch;
//import aima.core.search.adversarial.AlphaBetaSearch;
//import aima.core.search.adversarial.IterativeDeepeningAlphaBetaSearch;
//import aima.core.search.adversarial.MinimaxSearch;
//import aima.core.search.framework.Metrics;

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

    static JFrame constructApplicationFrame(ConnectFourState game) {
        JFrame frame = new JFrame();
        JPanel panel = new ConnectFourPanel(game);
        frame.add(panel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    public static void main(String[] args) {
        NAR n = NARS.tmp();
        n.log();
        Opjects o = new Opjects(n);
        ConnectFourState game = o.a("c", ConnectFourState.class);

        JFrame frame = constructApplicationFrame(game);
        frame.setSize(450, 450);
        frame.setVisible(true);

        while (true) {
            switch (game.moving()) {
                case 1:
                    game.drop(n.random().nextInt(game.cols));
                    break;
                case 2:
                    game.drop(n.random().nextInt(game.cols));
                    break;
            }
            frame.repaint();
            n.run(1);
            Util.sleep(25);
            if (game.isTerminal())
                game.clear();
        }

    }

    /**
     * A state of the Connect Four game is characterized by a board containing a
     * grid of spaces for disks, the next player to move, and some utility
     * informations. A win position for a player x is an empty space which turns a
     * situation into a win situation for x if he is able to place a disk there.
     *
     * @author Ruediger Lunde
     */
    public static class ConnectFourState implements Cloneable {

        private static final String[] players = new String[]{"red", "yellow"};

        public final int cols;
        public final int rows;
        /**
         * Uses special bit coding. First bit: disk of player 1, second bit: disk of
         * player 2, third bit: win position for player 1, fourth bit: win position
         * for player 2.
         */
        private byte[] board;

        private int moveCount;
        /**
         * Indicates the utility of the state. 1: win for player 1, 0: win for
         * player 2, 0.5: draw, -1 for all non-terminal states.
         */
        private double utility;
        public int winPositions1;
        public int winPositions2;

        public ConnectFourState() {
            this(6, 7);
        }

        public ConnectFourState(int rows, int cols) {
            utility = -1;
            this.cols = cols;
            this.rows = rows;
            board = new byte[rows * cols];
        }

        public void clear() {
            Arrays.fill(board, (byte) 0);
            utility = -1;
            moveCount = winPositions1 = winPositions2 = 0;
        }

        private double utility() {
            return utility;
        }

        public int get(int row, int col) {
            return board[row * cols + col] & 3;
        }

        public int moving() {
            return moveCount % 2 + 1;
        }

        public int moveCount() {
            return moveCount;
        }

        public void drop(int col) {
            int playerNum = moving();
            int row = freeRow(col);
            if (row != -1) {
                moveCount++;
                if (moveCount == board.length)
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
            }
        }

        public void set(int row, int col, int playerNum) {
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

        public boolean isWinPositionFor(int row, int col, int playerNum) {
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

        public int analyzePotentialWinPositions(Integer action) {
            final int[] rowIncr = new int[]{1, 0, 1, 1};
            final int[] colIncr = new int[]{0, 1, -1, 1};
            int moveCol = action;
            int moveRow = freeRow(moveCol);

            int playerNum = moving();
            int result = 0;
            for (int i = 0; i < 4; i++) {
                int rIncr = rowIncr[i];
                int cIncr = colIncr[i];
                int posCountSum = 0;

                for (int j = 0; j < 2; j++) {
                    int rBound = rIncr > 0 ? rows : -1;
                    int cBound = cIncr > 0 ? cols : -1;
                    int posCount = 0;

                    int row = moveRow + rIncr;
                    int col = moveCol + cIncr;
                    while (row != rBound && col != cBound && posCount < 3) {
                        int plNum = get(row, col);
                        if (plNum == 3 - playerNum)
                            break;
                        posCount++;
                        row += rIncr;
                        col += cIncr;
                    }
                    posCountSum += posCount;
                    rIncr = -rIncr;
                    cIncr = -cIncr;
                }
                if (posCountSum >= 3)
                    result += posCountSum;
            }
            return result;
        }


        public List<Integer> getActions() {
            ConnectFourState state = this;
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < state.cols; i++)
                if (state.get(0, i) == 0)
                    result.add(i);
            return result;
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

        public ConnectFourState clone() {
            ConnectFourState result = null;
            try {
                result = (ConnectFourState) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            result.board = board.clone();
            return result;
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (byte aBoard : board) result = result * 7 + aBoard + 1;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && getClass() == obj.getClass()) {
                ConnectFourState s = (ConnectFourState) obj;
                for (int i = 0; i < board.length; i++)
                    if (board[i] != s.board[i])
                        return false;
                return true;
            }
            return false;
        }

        // ////////////////////////////////////////////////////////////////////
        // nested classes

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
    }


//    /**
//     * Provides an implementation of the ConnectFour game which can be used for
//     * experiments with the Minimax algorithm.
//     *
//     * @author Ruediger Lunde
//     */
//    public static class ConnectFourGame implements Game<ConnectFourState, Integer, String> {
//        private String[] players = new String[]{"red", "yellow"};
//        private ConnectFourState initialState = new ConnectFourState(6, 7);
//
//        @Override
//        public ConnectFourState getInitialState() {
//            return initialState;
//        }
//
//        @Override
//        public String[] getPlayers() {
//            return players;
//        }
//
//        @Override
//        public String getPlayer(ConnectFourState state) {
//            return getPlayer(state.moving());
//        }
//
//        /**
//         * Returns the player corresponding to the specified player number. For
//         * efficiency reasons, <code>ConnectFourState</code>s use numbers
//         * instead of strings to identify players.
//         */
//        public String getPlayer(int playerNum) {
//            switch (playerNum) {
//                case 1:
//                    return players[0];
//                case 2:
//                    return players[1];
//            }
//            return null;
//        }
//
//        /**
//         * Returns the player number corresponding to the specified player. For
//         * efficiency reasons, <code>ConnectFourState</code>s use numbers instead of
//         * strings to identify players.
//         */
//        public int getPlayerNum(String player) {
//            for (int i = 0; i < players.length; i++)
//                if (Objects.equals(players[i], player))
//                    return i + 1;
//            throw new IllegalArgumentException("Wrong player number.");
//        }
//
//    }


    /**
     * Simple panel to control the game.
     */
    private static class ConnectFourPanel extends JPanel implements ActionListener {
        JButton clearButton;
        //JButton proposeButton;
        JLabel statusBar;

        final ConnectFourState game;
        //Metrics searchMetrics;

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
            //searchMetrics = null;
            if (e == null || e.getSource() == clearButton) {
                game.clear();
            } else if (!game.isTerminal()) {
//                if (e.getSource() == proposeButton) {
//                    proposeMove();
                /*} else */
                if (e.getSource() instanceof GridElement) {
                    GridElement el = (GridElement) e.getSource();
                    game.drop(el.col);
                    // turn
                }
            }
            repaint(); // paint all disks!


        }

        /**
         * Uses adversarial search for selecting the next action.
         */
        private void proposeMove() {
//                Integer action;
//                int time = (timeCombo.getSelectedIndex() + 1) * 5;
//                AdversarialSearch<ConnectFourState, Integer> search;
//                switch (strategyCombo.getSelectedIndex()) {
//                    case 0:
//                        search = MinimaxSearch.createFor(game);
//                        break;
//                    case 1:
//                        search = AlphaBetaSearch.createFor(game);
//                        break;
//                    case 2:
//                        search = IterativeDeepeningAlphaBetaSearch.createFor(game, 0.0,
//                                1.0, time);
//                        break;
//                    case 3:
//                        search = new ConnectFourAIPlayer(game, time);
//                        break;
//                    default:
//                        search = new ConnectFourAIPlayer(game, time);
//                        ((ConnectFourAIPlayer) search).setLogEnabled(true);
//                }
//                action = search.makeDecision(currState);
            //searchMetrics = search.getMetrics();

            //currState = game.getResult(currState, action);
        }

        /**
         * Updates the status bar.
         */
        private void updateStatus() {
            String statusText;
            if (!game.isTerminal()) {
                String toMove = (String) game.players[game.moving()];
                statusText = "Next move: " + toMove;
                statusBar.setForeground(toMove.equals("red") ? Color.RED
                        : Color.YELLOW);
            } else {
                String winner = null;
                for (int i = 0; i < 2; i++)
                    if (game.getUtility(game.players[i]) == 1)
                        winner = game.players[i];
                if (winner != null)
                    statusText = "Color " + winner
                            + " has won. Congratulations!";
                else
                    statusText = "No winner :-(";
                statusBar.setForeground(Color.WHITE);
            }
//                if (searchMetrics != null)
//                    statusText += "    " + searchMetrics;
            statusBar.setText(statusText);
        }

        /**
         * Represents a space within the grid where discs can be placed.
         */
        @SuppressWarnings("serial")
        private class GridElement extends JButton {
            int row;
            int col;

            GridElement(int row, int col) {
                this.row = row;
                this.col = col;
                setBackground(Color.BLUE);
            }

            public void paintComponent(Graphics g) {
                super.paintComponent(g); // should have look and feel of a
                // button...
                int playerNum = game.get(row, col);
                if (playerNum != 0) {
                    drawDisk(g, playerNum); // draw disk on top!
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
