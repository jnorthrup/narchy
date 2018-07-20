package nars.experiment.checkers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * @author Arjen Hoogesteger
 * @version 0.3
 */
public class Board extends JPanel implements MouseListener, PlayerListener {
    private static final int SQUARE_WIDTH = 60;
    private static final int SQUARE_HEIGHT = 60;
    private Square source;
    private Checkers game;
    private final Square[][] squares;
    private boolean mouseListener;
    private final Player player1;
    private final Player player2;

    public Board(Player player1, Player player2, int WIDTH, int HEIGHT) {
        this(player1, player2, new Checkers(WIDTH, HEIGHT));
    }

    /**
     *
     *
     */
    public Board(Player player1, Player player2, Checkers game) {
        int WIDTH = game.WIDTH;
        int HEIGHT = game.HEIGHT;


        this.player1 = player1;
        this.player1.setBoard(this);
        this.player1.addListener(this);
        this.player2 = player2;
        this.player2.setBoard(this);
        this.player2.addListener(this);


        disableMouseListener();


        ((FlowLayout) getLayout()).setHgap(0);
        ((FlowLayout) getLayout()).setVgap(0);


        setPreferredSize(new Dimension(WIDTH * SQUARE_WIDTH, HEIGHT * SQUARE_HEIGHT));

        squares = (Square[][]) Array.newInstance(Square.class, WIDTH, HEIGHT);
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {

                squares[i][j] = new Square(i % 2 == j % 2 ? new Color(50, 50, 50) : new Color(200, 200, 200), i, j);


                squares[i][j].setPreferredSize(new Dimension(SQUARE_WIDTH, SQUARE_HEIGHT));


                squares[i][j].addMouseListener(this);
            }
        }


        setLayout(new GridLayout(WIDTH, HEIGHT));


        for (int i = HEIGHT - 1; i >= 0; i--) {
            for (int j = 0; j < WIDTH; j++)
                add(squares[j][i]);
        }


        setContext(game);
    }

    /**
     *
     */
    public void enableMouseListener() {
        mouseListener = true;
    }

    /**
     *
     */
    public void disableMouseListener() {
        mouseListener = false;
    }

    /**
     *
     */
    private void setPieces() {
        Piece[][] pieces = game.getPieces();

        for (int i = 0; i < game.WIDTH; i++) {
            for (int j = 0; j < game.HEIGHT; j++) {
                squares[i][j].setPiece(pieces[i][j]);
            }
        }
    }

    /**
     * @param context the board's context
     */
    public void setContext(Checkers context) {
        game = context;
        setPieces();
    }

    /**
     * @return the board's context
     */
    public Checkers getContext() {
        return game;
    }

    @Override
    public void repaint() {
        super.repaint();

        if (squares != null) {
            for (Square[] square : squares) {
                for (Square aSquare : square) aSquare.repaint();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseListener) {
            if (source == null) {

                source = (Square) e.getSource();

                if (source.getCoordinateX() % 2 == source.getCoordinateY() % 2) {
                    source.select();

                    ArrayList<int[]> targets = game.pieceCouldMoveToFrom(source.getCoordinateX(), source.getCoordinateY());
                    for (int[] target : targets)
                        squares[target[0]][target[1]].target();
                } else
                    source = null;
            } else if (source.equals(e.getSource())) {

                ArrayList<int[]> targets = game.pieceCouldMoveToFrom(source.getCoordinateX(), source.getCoordinateY());
                for (int[] target : targets)
                    squares[target[0]][target[1]].detarget();

                source.deselect();
                source = null;
            } else {

                Square destination = (Square) e.getSource();

                if (game.move(source.getCoordinateX(), source.getCoordinateY(), destination.getCoordinateX(), destination.getCoordinateY())) {
                    detargetAllSquares();
                    source.deselect();
                    source = null;


                    setPieces();
                    repaint();
                } else
                    System.out.println("UNABLE TO MOVE [" + source.getCoordinateX() + ", " + source.getCoordinateY() + "] -> [" + destination.getCoordinateX() + ", " + destination.getCoordinateY() + ']');
            }

            if (game.isTurnDark() && player2.hasTurn()) {
                player2.stopTurn();
            } else if (game.isTurnLight() && player1.hasTurn()) {
                player1.stopTurn();
            }
        }
    }

    /**
     * Returns player one of the game.
     *
     * @return player one
     */
    public Player getPlayer1() {
        return player1;
    }

    /**
     * Returns player two of the game.
     *
     * @return player two
     */
    public Player getPlayer2() {
        return player2;
    }

    /**
     *
     */
    private void detargetAllSquares() {
        for (Square[] square : squares) {
            for (Square aSquare : square) aSquare.detarget();
        }
    }

    /**
     *
     */
    public void play() {
        player1.takeTurn();
    }

    public static void main(String[] args) {
        Board b = new Board(new HumanPlayer("Human1"), new HumanPlayer("Human2"), 8, 8);


        JFrame frame = new JFrame("Checkers");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel boardPane = new JPanel(new FlowLayout());
        ((FlowLayout) boardPane.getLayout()).setAlignment(FlowLayout.CENTER);
        boardPane.add(b);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(boardPane, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        b.play();
    }


    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void finishedTurn(Player p) {
        if ((p.equals(player1) && !player2.hasTurn()) || (p.equals(player2) && !player1.hasTurn())) {
            disableMouseListener();

            if (p.equals(player1))
                player2.takeTurn();
            else
                player1.takeTurn();
        }
    }

    public void playWindow() {
        JFrame frame = new JFrame("Checkers");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel boardPane = new JPanel(new BorderLayout());

        boardPane.add(this, BorderLayout.CENTER);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(boardPane, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        play();

    }
}
