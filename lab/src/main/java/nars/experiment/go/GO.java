package nars.experiment.go;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * https://github.com/mattshin/GO
 */
public class GO extends JPanel implements MouseListener {


    ArrayList<int[][]> boardHist = new ArrayList<>();
    Color bg = new Color(0xA2, 0x64, 0x18);
    public static final int DIM = 9;
    static int[][] board = new int[DIM][DIM];
    int turn;
    static int blackP;
    static int whiteP;

    {
        for (int i = 0; i < DIM; i++) {
            board[i][0] = 3;
            board[0][i] = 3;
            board[DIM - 1][i] = 3;
            board[i][DIM - 1] = 3;
        }
        boardHist.add(board);
    }


    public GO() {
        addMouseListener(this);
    }


    @Override
    public void paintComponent(Graphics g) {

        g.setColor(bg);
        g.fillRect(20, 20, 508, 508);

        g.setColor(Color.BLACK);
        g.drawRect(20, 20, 508, 508);
        for (int i = 40; i <= 510; i += 26) {
            g.drawLine(40, i, 508, i);
            g.drawLine(i, 40, i, 508);
        }

        g.fillOval(114, 114, 8, 8);
        g.fillOval(114, 270, 8, 8);
        g.fillOval(426, 114, 8, 8);
        g.fillOval(426, 270, 8, 8);
        g.fillOval(270, 114, 8, 8);
        g.fillOval(114, 426, 8, 8);
        g.fillOval(270, 270, 8, 8);
        g.fillOval(426, 426, 8, 8);
        g.fillOval(270, 426, 8, 8);

        for (int i = 1; i < DIM - 1; i++) {
            for (int k = 1; k < DIM - 1; k++) {
                switch (boardHist.get(turn)[i][k]) {
                    case 1:
                        g.setColor(Color.BLACK);
                        g.fillOval(4 + i * 26, 4 + k * 26, 20, 20);
                        break;
                    case 2:
                        g.setColor(Color.WHITE);
                        g.fillOval(4 + i * 26, 4 + k * 26, 20, 20);

                        g.setColor(Color.BLACK);
                        g.drawOval(4 + i * 26, 4 + k * 26, 20, 20);
                        break;
                }
            }
        }
        g.setFont(new Font("Times New Roman", 14, 14));
        g.fillRect(40, 580, 80, 30);
        g.drawRoundRect(20, 540, 508, 30, 30, 30);
        g.fillRect(240, 580, 80, 30);
        g.fillRect(440, 580, 80, 30);

        g.setColor(Color.WHITE);
        g.drawString("Reset", 65, 600);
        g.drawString("Pass", 465, 600);
        g.drawString("Back", 265, 600);

        g.setColor(Color.BLACK);
        if (turn % 2 == 1)
            g.drawString("It is currently White's turn", 40, 554);
        else
            g.drawString("It is currently Black's turn", 40, 554);
        g.drawString("Black has captured " + blackP + " stones", 350, 554);
        g.drawString("White has captured " + whiteP + " stones", 350, 568);


    }

    public static JButton reset = new JButton("Reset");
    public static JButton back = new JButton("Back");
    public static JButton pass = new JButton("Pass");
    static JFrame frame = new JFrame("GO");

    public static void main(String[] args) {

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


        GO panel = new GO();
        frame.add(panel);


        frame.setSize(560, 650);
        frame.setVisible(true);
        frame.setResizable(false);


    }

    public void reset() {
        turn = 0;
        board = new int[DIM][DIM];
        for (int i = 0; i < DIM; i++) {
            board[i][0] = 3;
            board[0][i] = 3;
            board[DIM - 1][i] = 3;
            board[i][DIM - 1] = 3;
        }
        boardHist.clear();
        boardHist.add(board);
        blackP = 0;
        whiteP = 0;

    }

    int p;
    boolean kamikaze;

    @Override
    public void mouseReleased(MouseEvent e) {

        int x = e.getX();
        int y = e.getY();
        if (y >= 580 && y <= 610) {
            if (x > 40 && x < 120) {
                reset();
            }
            if (x > 240 && x < 320) {
                turn--;
                if (!kamikaze) {
                    if (turn % 2 == 0)
                        blackP -= p;
                    else
                        whiteP -= p;
                } else {
                    if (turn % 2 == 1)
                        blackP -= p;
                    else
                        whiteP -= p;
                }
                p = 0;

            }
            if (x > 440 && x < 520) {
                boardHist.add(turn, boardHist.get(turn));
                turn++;
            }
        } else {
            x -= 40;
            x = (int) Math.round((double) x / 26.0);
            y -= 40;
            y = (int) Math.round((double) y / 26.0);

            x++;
            y++;
            System.out.print(turn % 2 == 1 ? "W " : "B ");
            System.out.println(x + "," + y);
            board = new int[DIM][DIM];
            for (int r = 0; r < boardHist.get(turn).length; r++) {
                System.arraycopy(boardHist.get(turn)[r], 0, board[r], 0, boardHist.get(turn).length);
            }

            if (x >= 0 && x < DIM - 1 && y >= 0 && y < DIM - 1 && board[x][y] == 0) {
                board[x][y] = 1 + turn % 2;
                p = 0;
                kamikaze = false;

                if (board[x][y + 1] == 2 / board[x][y] && checkSurround(x, y + 1, 2 / board[x][y])) {
                    p = removeGroup(x, y + 1, 2 / board[x][y]);
                    if (turn % 2 == 0)
                        blackP += p;
                    else
                        whiteP += p;
                }
                if (board[x + 1][y] == 2 / board[x][y] && checkSurround(x + 1, y, 2 / board[x][y])) {
                    p = removeGroup(x + 1, y, 2 / board[x][y]);
                    if (turn % 2 == 0)
                        blackP += p;
                    else
                        whiteP += p;
                }
                if (board[x][y - 1] == 2 / board[x][y] && checkSurround(x, y - 1, 2 / board[x][y])) {
                    p = removeGroup(x, y - 1, 2 / board[x][y]);
                    if (turn % 2 == 0)
                        blackP += p;
                    else
                        whiteP += p;
                }
                if (board[x - 1][y] == 2 / board[x][y] && checkSurround(x - 1, y, 2 / board[x][y])) {
                    p = removeGroup(x - 1, y, 2 / board[x][y]);
                    if (turn % 2 == 0)
                        blackP += p;
                    else
                        whiteP += p;
                }

                if (checkSurround(x, y, board[x][y])) {
                    p = removeGroup(x, y, board[x][y]);
                    if (turn % 2 == 1)
                        blackP += p;
                    else
                        whiteP += p;
                    kamikaze = true;
                }

                boolean matches = true;
                for (int i = 0; i < DIM; i++) {
                    if (turn >= 3) {
                        if (!Arrays.toString(boardHist.get(turn - 1)[i]).equals(Arrays.toString(board[i]))) {
                            matches = false;

                        }
                    } else
                        matches = false;
                }
                if (!matches) {
                    turn++;
                    boardHist.add(turn, board);
                } else {
                    System.out.println("KO");
                    if (turn % 2 == 0)
                        blackP -= p;
                    else
                        whiteP -= p;
                }
            }
        }
        System.out.println(turn);
        frame.repaint();
    }

    public static boolean checkSurround(int x, int y, int c) {
        boolean[][] checked = new boolean[DIM][DIM];
        checked[x][y] = true;
        if (board[x][y] == c) {
            return (checkSurround(x + 1, y, c, checked) &&
                    checkSurround(x, y + 1, c, checked) &&
                    checkSurround(x - 1, y, c, checked) &&
                    checkSurround(x, y - 1, c, checked));
        } else return board[x][y] != 0;
    }

    public static boolean checkSurround(int x, int y, int c, boolean[][] checked) {

        if (checked[x][y])
            return true;
        else if (board[x][y] == c) {
            checked[x][y] = true;
            return (checkSurround(x + 1, y, c, checked) &&
                    checkSurround(x, y + 1, c, checked) &&
                    checkSurround(x - 1, y, c, checked) &&
                    checkSurround(x, y - 1, c, checked));
        } else if (board[x][y] == 0)
            return false;
        else {
            checked[x][y] = true;
            return true;
        }
    }

    public static int removeGroup(int x, int y, int c) {
        if (board[x][y] == c) {
            board[x][y] = 0;
            if (board[x][y + 1] != c &&
                    board[x + 1][y] != c &&
                    board[x][y - 1] != c &&
                    board[x - 1][y] != c)
                return 1;
            else {
                return 1 + removeGroup(x - 1, y, c) +
                        removeGroup(x + 1, y, c) +
                        removeGroup(x, y - 1, c) +
                        removeGroup(x, y + 1, c);
            }
        }
        return 0;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }
}

class menuButton extends JButton {

    private static final long serialVersionUID = 1L;
    String name = "";

    public menuButton(String n) {
        super(n);
        name = n;
    }
}
