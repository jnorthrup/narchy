package nars.experiment.connect4;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
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
        int winPositions1;
        int winPositions2;
        private int moveCount, invalidCount;
        /**
         * Indicates the utility of the state. 1: win for player 1, 0: win for
         * player 2, 0.5: draw, -1 for all non-terminal states.
         */
        private double utility;

        public ConnectFourState() {
            this(6, 7);
        }

        ConnectFourState(int rows, int cols) {
            this.cols = cols;
            this.rows = rows;
            board = new byte[rows * cols];
            clear();
        }

        public void clear() {
            Arrays.fill(board, (byte) 0);
            utility = -1;
            invalidCount = moveCount = winPositions1 = winPositions2 = 0;
        }

        private double utility() {
            return utility;
        }

        int get(int row, int col) {
            return board[row * cols + col] & 3;
        }

        public int moving() {
            return moveCount % 2 + 1;
        }

        public int moveCount() {
            return moveCount;
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

//        public int analyzePotentialWinPositions(Integer action) {
//            final int[] rowIncr = new int[]{1, 0, 1, 1};
//            final int[] colIncr = new int[]{0, 1, -1, 1};
//            int moveCol = action;
//            int moveRow = freeRow(moveCol);
//
//            int playerNum = moving();
//            int result = 0;
//            for (int i = 0; i < 4; i++) {
//                int rIncr = rowIncr[i];
//                int cIncr = colIncr[i];
//                int posCountSum = 0;
//
//                for (int j = 0; j < 2; j++) {
//                    int rBound = rIncr > 0 ? rows : -1;
//                    int cBound = cIncr > 0 ? cols : -1;
//                    int posCount = 0;
//
//                    int row = moveRow + rIncr;
//                    int col = moveCol + cIncr;
//                    while (row != rBound && col != cBound && posCount < 3) {
//                        int plNum = get(row, col);
//                        if (plNum == 3 - playerNum)
//                            break;
//                        posCount++;
//                        row += rIncr;
//                        col += cIncr;
//                    }
//                    posCountSum += posCount;
//                    rIncr = -rIncr;
//                    cIncr = -cIncr;
//                }
//                if (posCountSum >= 3)
//                    result += posCountSum;
//            }
//            return result;
//        }


//        public List<Integer> getActions() {
//            ConnectFourState state = this;
//            List<Integer> result = new ArrayList<>();
//            for (int i = 0; i < state.cols; i++)
//                if (state.get(0, i) == 0)
//                    result.add(i);
//            return result;
//        }

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

//        public ConnectFourState clone() {
//            ConnectFourState result = null;
//            try {
//                result = (ConnectFourState) super.clone();
//            } catch (CloneNotSupportedException e) {
//                e.printStackTrace();
//            }
//            result.board = board.clone();
//            return result;
//        }

//        @Override
//        public int hashCode() {
//            int result = 0;
//            for (byte aBoard : board) result = result * 7 + aBoard + 1;
//            return result;
//        }

//        @Override
//        public boolean equals(Object obj) {
//            if (obj != null && getClass() == obj.getClass()) {
//                ConnectFourState s = (ConnectFourState) obj;
//                for (int i = 0; i < board.length; i++)
//                    if (board[i] != s.board[i])
//                        return false;
//                return true;
//            }
//            return false;
//        }

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

        public static class Play {

            protected int player;
            private ConnectFourState game;
            private int moving;

            /**
             * TODO not public
             */
            protected void init(ConnectFourState game, int player) {
                this.player = player;
                this.game = game;
            }

            public void moving(int whosMove) {
                synchronized (game) {
                    this.moving = whosMove;
                }
            }

            public boolean drop(int which) {
                synchronized (game) {
                    if (this.moving != player) {
                        notMoving(player);
                        return false;
                    } else {
                        if (game.drop(which, player)) {
                            moving(this.moving == 1 ? 2 : 1);
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }

            /**
             * signal attempted move out of turn
             */
            public void notMoving(int player) {

            }

            public int winner() {
                synchronized (game) {

                    if (game.isTerminal()) {
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
                synchronized (game) {
                    //scan through board by calling its methods providing sight of the board state
                    for (int r = 0; r < game.rows; r++)
                        for (int c = 0; c < game.cols; c++)
                            game.get(r, c);
                }

            }

            public boolean isTerminal() {
                return game.isTerminal();
            }

            public void clear() {
                game.clear();
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
        final JButton clearButton;
        //JButton proposeButton;
        final JLabel statusBar;

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
//            //searchMetrics = null;
//            if (e == null || e.getSource() == clearButton) {
//                game.clear();
//            } else if (!game.isTerminal()) {
////                if (e.getSource() == proposeButton) {
////                    proposeMove();
//                /*} else */
////                if (e.getSource() instanceof GridElement) {
////                    GridElement el = (GridElement) e.getSource();
////                    game.drop(el.col, ..);
////                    // turn
////                }
//            }
            repaint(); // paint all disks!


        }

        /**
         * Uses adversarial search for selecting the next action.
         */
//        private void proposeMove() {
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
//        }

        /**
         * Updates the status bar.
         */
        protected int updateStatus() {
            int won = 0;
            String statusText;
            if (!game.isTerminal()) {
                String toMove = ConnectFourState.players[game.moving()];
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
//                if (searchMetrics != null)
//                    statusText += "    " + searchMetrics;
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
//package nars.experiment.connect4;
//
//import java.util.List;
//
///**
// * Artificial Intelligence A Modern Approach (3rd Edition): page 165.<br>
// * <br>
// * A game can be formally defined as a kind of search problem with the following
// * elements: <br>
// * <ul>
// * <li>S0: The initial state, which specifies how the game is set up at the
// * start.</li>
// * <li>PLAYER(s): Defines which player has the move in a state.</li>
// * <li>ACTIONS(s): Returns the set of legal moves in a state.</li>
// * <li>RESULT(s, a): The transition model, which defines the result of a move.</li>
// * <li>TERMINAL-TEST(s): A terminal test, which is true when the game is over
// * and false TERMINAL STATES otherwise. States where the game has ended are
// * called terminal states.</li>
// * <li>UTILITY(s, p): A utility function (also called an objective function or
// * payoff function), defines the final numeric value for a game that ends in
// * terminal state s for a player p. In chess, the outcome is a win, loss, or
// * draw, with values +1, 0, or 1/2 . Some games have a wider variety of possible
// * outcomes; the payoffs in backgammon range from 0 to +192. A zero-sum game is
// * (confusingly) defined as one where the total payoff to all players is the
// * same for every instance of the game. Chess is zero-sum because every game has
// * payoff of either 0 + 1, 1 + 0 or 1/2 + 1/2 . "Constant-sum" would have been a
// * better term, but zero-sum is traditional and makes sense if you imagine each
// * player is charged an entry fee of 1/2.</li>
// * </ul>
// *
// * @param <S>  Type which is used for states in the game.
// * @param <A> Type which is used for actions in the game.
// * @param <P> Type which is used for players in the game.
// * @author Ruediger Lunde
// */
//public interface Game<S, A, P> {
//
//    S getInitialState();
//
//    P[] getPlayers();
//
//    P getPlayer(S state);
//
//    List<A> getActions(S state);
//
//    S getResult(S state, A action);
//
//    boolean isTerminal(S state);
//
//    double getUtility(S state, P player);
//}
