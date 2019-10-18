package nars.experiment.othello10;/*
 * �쐬��: 2004/12/17
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * �I�Z���Ղ̃N���X�B
 *
 * @author mori
 */
public class MainPanel extends JPanel implements MouseListener {

    private static final int GS = 32;

    public static final int MASU = 8;

    private static final int WIDTH = GS * MASU;
    private static final int HEIGHT = WIDTH;

    private static final int BLANK = 0;

    private static final int BLACK_STONE = 1;

    private static final int WHITE_STONE = -1;

    private static final int SLEEP_TIME = 500;

    private static final int END_NUMBER = 60;

    private static final int START = 0;
    private static final int PLAY = 1;
    private static final int YOU_WIN = 2;
    private static final int YOU_LOSE = 3;
    private static final int DRAW = 4;


    private final int[][] board = new int[MASU][MASU];

    private boolean flagForWhite;

    private int putNumber;


    private int gameState;

    private final AI ai;


    private final InfoPanel infoPanel;

    public MainPanel(InfoPanel infoPanel) {

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.infoPanel = infoPanel;


        initBoard();


        ai = new AI(this);

        addMouseListener(this);

        gameState = START;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);


        drawBoard(g);
        switch (gameState) {
            case START:
                drawTextCentering(g, "OTHELLO");
                break;
            case PLAY:

                drawStone(g);

                Counter counter = countStone();

                infoPanel.setBlackLabel(counter.blackCount);
                infoPanel.setWhiteLabel(counter.whiteCount);
                break;
            case YOU_WIN:
                drawStone(g);
                drawTextCentering(g, "YOU WIN");
                break;
            case YOU_LOSE:
                drawStone(g);
                drawTextCentering(g, "YOU LOSE");
                break;
            case DRAW:
                drawStone(g);
                drawTextCentering(g, "DRAW");
                break;
        }

    }

    /**
     * �}�E�X���N���b�N�����Ƃ��B�΂�łB
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        switch (gameState) {
            case START:

                gameState = PLAY;
                break;
            case PLAY:

                int x = e.getX() / GS;
                int y = e.getY() / GS;


                if (canPutDown(x, y)) {

                    Undo undo = new Undo(x, y);

                    putDownStone(x, y, false);

                    reverse(undo, false);

                    endGame();

                    nextTurn();

                    if (countCanPutDownStone() == 0) {
                        System.out.println("AI PASS!");
                        nextTurn();
                        return;
                    } else {

                        ai.compute();
                    }
                }
                break;
            case YOU_WIN:
            case YOU_LOSE:
            case DRAW:

                gameState = START;

                initBoard();
                break;
        }


        repaint();
    }

    /**
     * �Ֆʂ�����������B
     */
    private void initBoard() {
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                board[y][x] = BLANK;
            }
        }

        board[3][3] = board[4][4] = WHITE_STONE;
        board[3][4] = board[4][3] = BLACK_STONE;


        flagForWhite = false;
        putNumber = 0;
    }

    /**
     * �Ֆʂ�`���B
     *
     * @param g �`��I�u�W�F�N�g�B
     */
    private static void drawBoard(Graphics g) {

        g.setColor(new Color(0, 128, 128));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {

                g.setColor(Color.BLACK);
                g.drawRect(x * GS, y * GS, GS, GS);
            }
        }
    }

    /**
     * �΂�`���B
     *
     * @param g �`��I�u�W�F�N�g
     */
    private void drawStone(Graphics g) {
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                switch (board[y][x]) {
                    case BLANK:
                        continue;
                    case BLACK_STONE:
                        g.setColor(Color.BLACK);
                        break;
                    default:
                        g.setColor(Color.WHITE);
                        break;
                }
                g.fillOval(x * GS + 3, y * GS + 3, GS - 6, GS - 6);
            }
        }
    }

    /**
     * �Ֆʂɐ΂�łB
     *
     * @param x           �΂�łꏊ��x���W�B
     * @param y           �΂�łꏊ��y���W�B
     * @param tryAndError �R���s���[�^�̎v�l���������ǂ����B�v�l���͐΂�`�悵�Ȃ��B
     */
    public void putDownStone(int x, int y, boolean tryAndError) {
        int stone;


        if (flagForWhite) {
            stone = WHITE_STONE;
        } else {
            stone = BLACK_STONE;
        }

        board[y][x] = stone;

        if (!tryAndError) {
            putNumber++;


            update(getGraphics());

            sleep();
        }
    }

    /**
     * �΂��łĂ邩�ǂ������ׂ�B
     *
     * @param x �΂�łƂ��Ƃ��Ă���ꏊ��x���W�B
     * @param y �΂�łƂ��Ƃ��Ă���ꏊ��y���W�B
     * @return �΂��łĂ�Ȃ�true�A�łĂȂ��Ȃ�false��Ԃ��B
     */
    public boolean canPutDown(int x, int y) {

        if (x >= MASU || y >= MASU)
            return false;

        if (board[y][x] != BLANK)
            return false;


        if (canPutDown(x, y, 1, 0))
            return true;
        if (canPutDown(x, y, 0, 1))
            return true;
        if (canPutDown(x, y, -1, 0))
            return true;
        if (canPutDown(x, y, 0, -1))
            return true;
        if (canPutDown(x, y, 1, 1))
            return true;
        if (canPutDown(x, y, -1, -1))
            return true;
        if (canPutDown(x, y, 1, -1))
            return true;
        return canPutDown(x, y, -1, 1);


    }

    /**
     * vecX�AvecY�̕����ɂЂ�����Ԃ���΂����邩���ׂ�B
     *
     * @param x    �΂�łƂ��Ƃ��Ă���ꏊ��x���W�B
     * @param y    �΂�łƂ��Ƃ��Ă���ꏊ��y���W�B
     * @param vecX ���ׂ����������x�����x�N�g���B
     * @param vecY ���ׂ����������y�����x�N�g���B
     * @return �΂��łĂ�Ȃ�true�A�łĂȂ��Ȃ�false��Ԃ��B
     */
    private boolean canPutDown(int x, int y, int vecX, int vecY) {
        int putStone;


        if (flagForWhite) {
            putStone = WHITE_STONE;
        } else {
            putStone = BLACK_STONE;
        }


        x += vecX;
        y += vecY;

        if (x < 0 || x >= MASU || y < 0 || y >= MASU)
            return false;

        if (board[y][x] == putStone)
            return false;

        if (board[y][x] == BLANK)
            return false;


        x += vecX;
        y += vecY;

        while (x >= 0 && x < MASU && y >= 0 && y < MASU) {

            if (board[y][x] == BLANK)
                return false;

            if (board[y][x] == putStone) {
                return true;
            }
            x += vecX;
            y += vecY;
        }

        return false;
    }

    /**
     * �΂��Ђ�����Ԃ��B
     *
     * @param x           �΂�ł����ꏊ��x���W�B
     * @param y           �΂�ł����ꏊ��y���W�B
     * @param tryAndError �R���s���[�^�̎v�l���������ǂ����B�v�l���͐΂�`�悵�Ȃ��B
     */
    public void reverse(Undo undo, boolean tryAndError) {

        if (canPutDown(undo.x, undo.y, 1, 0))
            reverse(undo, 1, 0, tryAndError);
        if (canPutDown(undo.x, undo.y, 0, 1))
            reverse(undo, 0, 1, tryAndError);
        if (canPutDown(undo.x, undo.y, -1, 0))
            reverse(undo, -1, 0, tryAndError);
        if (canPutDown(undo.x, undo.y, 0, -1))
            reverse(undo, 0, -1, tryAndError);
        if (canPutDown(undo.x, undo.y, 1, 1))
            reverse(undo, 1, 1, tryAndError);
        if (canPutDown(undo.x, undo.y, -1, -1))
            reverse(undo, -1, -1, tryAndError);
        if (canPutDown(undo.x, undo.y, 1, -1))
            reverse(undo, 1, -1, tryAndError);
        if (canPutDown(undo.x, undo.y, -1, 1))
            reverse(undo, -1, 1, tryAndError);
    }

    /**
     * �΂��Ђ�����Ԃ��B
     *
     * @param x           �΂�ł����ꏊ��x���W�B
     * @param y           �΂�ł����ꏊ��y���W�B
     * @param vecX        �Ђ�����Ԃ������������x�N�g���B
     * @param vecY        �Ђ�����Ԃ������������x�N�g���B
     * @param tryAndError �R���s���[�^�̎v�l���������ǂ����B�v�l���͐΂�`�悵�Ȃ��B
     */
    private void reverse(Undo undo, int vecX, int vecY, boolean tryAndError) {
        int putStone;
        int x = undo.x;
        int y = undo.y;

        if (flagForWhite) {
            putStone = WHITE_STONE;
        } else {
            putStone = BLACK_STONE;
        }


        x += vecX;
        y += vecY;
        while (board[y][x] != putStone) {

            board[y][x] = putStone;

            undo.pos[undo.count++] = new Point(x, y);
            if (!tryAndError) {


                update(getGraphics());

                sleep();
            }
            x += vecX;
            y += vecY;
        }
    }

    /**
     * �I�Z���Ղ�1���O�̏�Ԃɖ߂��B AI�͐΂�ł�����߂����肵�ĔՖʂ�]���ł���B
     *
     * @param undo �Ђ�����Ԃ����΂̏��B
     */
    public void undoBoard(Undo undo) {
        int c = 0;

        while (undo.pos[c] != null) {

            int x = undo.pos[c].x;
            int y = undo.pos[c].y;


            board[y][x] *= -1;
            c++;
        }

        board[undo.y][undo.x] = BLANK;

        nextTurn();
    }

    /**
     * ��Ԃ�ς���B
     */
    public void nextTurn() {

        flagForWhite = !flagForWhite;
    }

    /**
     * �΂��łĂ�ꏊ�̐��𐔂���B
     *
     * @return �΂��łĂ�ꏊ�̐��B
     */
    public int countCanPutDownStone() {
        int count = 0;

        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {
                if (canPutDown(x, y)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * SLEEP_TIME�����x�~������
     */
    private static void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * ��ʂ̒����ɕ������\������
     *
     * @param g �`��I�u�W�F�N�g
     * @param s �`�悵����������
     */
    public static void drawTextCentering(Graphics g, String s) {
        Font f = new Font("SansSerif", Font.BOLD, 20);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.YELLOW);
        g.drawString(s, WIDTH / 2 - fm.stringWidth(s) / 2, HEIGHT / 2
                + fm.getDescent());
    }

    /**
     * �Q�[�����I�����������ׂ�B
     */
    public boolean endGame() {

        if (putNumber == END_NUMBER) {

            Counter counter = countStone();


            if (counter.blackCount > 32) {
                gameState = YOU_WIN;
            } else if (counter.blackCount < 32) {
                gameState = YOU_LOSE;
            } else {
                gameState = DRAW;
            }
            repaint();
            return true;
        }
        return false;
    }

    /**
     * �I�Z���Տ�̐΂̐��𐔂���
     *
     * @return �΂̐����i�[����Counter�I�u�W�F�N�g
     */
    public Counter countStone() {
        Counter counter = new Counter();

        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                if (board[y][x] == BLACK_STONE)
                    counter.blackCount++;
                if (board[y][x] == WHITE_STONE)
                    counter.whiteCount++;
            }
        }

        return counter;
    }

    /**
     * (x,y)�̃{�[�h�̐΂̎�ނ�Ԃ��B
     *
     * @param x X���W�B
     * @param y Y���W�B
     * @return BLANK or BLACK_STONE or WHITE_STONE
     */
    public int getBoard(int x, int y) {
        return board[y][x];
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
}