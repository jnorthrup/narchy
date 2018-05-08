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
 * @author mori
 *  
 */
public class MainPanel extends JPanel implements MouseListener {
    // �}�X�̃T�C�Y�iGRID SIZE�j
    private static final int GS = 32;
    // �}�X�̐��B�I�Z����8�~8�}�X�iAI�N���X�Ŏg���̂�public�j
    public static final int MASU = 8;
    // �Ֆʂ̑傫�������C���p�l���̑傫���Ɠ���
    private static final int WIDTH = GS * MASU;
    private static final int HEIGHT = WIDTH;
    // ��
    private static final int BLANK = 0;
    // ����
    private static final int BLACK_STONE = 1;
    // ����
    private static final int WHITE_STONE = -1;
    // ���x�~�̎���
    private static final int SLEEP_TIME = 500;
    // �I�����̐΂̐��i�I�Z����8x8-4=60��ŏI������j
    private static final int END_NUMBER = 60;
    // �Q�[�����
    private static final int START = 0;
    private static final int PLAY = 1;
    private static final int YOU_WIN = 2;
    private static final int YOU_LOSE = 3;
    private static final int DRAW = 4;

    // �Ֆ�
    private final int[][] board = new int[MASU][MASU];
    // ���̔Ԃ�
    private boolean flagForWhite;
    // �ł��ꂽ�΂̐�
    private int putNumber;
    // �΂�ł�
    //private AudioClip kachi;
    // �Q�[�����
    private int gameState;
    // AI
    private final AI ai;

    // ���p�l���ւ̎Q��
    private final InfoPanel infoPanel;

    public MainPanel(InfoPanel infoPanel) {
        // Othello��pack()����Ƃ��ɕK�v
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.infoPanel = infoPanel;

        // �Ֆʂ�����������
        initBoard();
        // �T�E���h�����[�h����
        //kachi = Applet.newAudioClip(getClass().getResource("kachi.wav"));
        // AI���쐬
        ai = new AI(this);
        // �}�E�X������󂯕t����悤�ɂ���
        addMouseListener(this);
        // START��ԁi�^�C�g���\���j
        gameState = START;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // �Ֆʂ�`��
        drawBoard(g);
        switch (gameState) {
            case START :
                drawTextCentering(g, "OTHELLO");
                break;
            case PLAY :
                // �΂�`��
                drawStone(g);
                // �Ֆʂ̐΂̐��𐔂���
                Counter counter = countStone();
                // ���x���ɕ\��
                infoPanel.setBlackLabel(counter.blackCount);
                infoPanel.setWhiteLabel(counter.whiteCount);
                break;
            case YOU_WIN :
                drawStone(g);
                drawTextCentering(g, "YOU WIN");
                break;
            case YOU_LOSE :
                drawStone(g);
                drawTextCentering(g, "YOU LOSE");
                break;
            case DRAW :
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
            case START :
                // START��ʂŃN���b�N���ꂽ��Q�[���J�n
                gameState = PLAY;
                break;
            case PLAY :
                // �ǂ��̃}�X���𒲂ׂ�
                int x = e.getX() / GS;
                int y = e.getY() / GS;

                // (x, y)�ɐ΂��łĂ�ꍇ�����ł�
                if (canPutDown(x, y)) {
                    // �߂���悤�ɋL�^���Ă���
                    Undo undo = new Undo(x, y);
                    // ���̏ꏊ�ɐ΂�ł�
                    putDownStone(x, y, false);
                    // �Ђ�����Ԃ�
                    reverse(undo, false);
                    // �I�����������ׂ�
                    endGame();
                    // ��Ԃ�ς���
                    nextTurn();
                    // AI���p�X�̏ꍇ�͂������
                    if (countCanPutDownStone() == 0) {
                        System.out.println("AI PASS!");
                        nextTurn();
                        return;
                    } else {
                        // �p�X�łȂ�������AI���΂�ł�
                        ai.compute();
                    }
                }
                break;
            case YOU_WIN :
            case YOU_LOSE :
            case DRAW :
                // �Q�[���I�����ɃN���b�N���ꂽ��X�^�[�Ƃ֖߂�
                gameState = START;
                // �Ֆʏ�����
                initBoard();
                break;
        }

        // �ĕ`�悷��
        repaint();
    }

    /**
     * �Ֆʂ�����������B
     *  
     */
    private void initBoard() {
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                board[y][x] = BLANK;
            }
        }
        // �����z�u
        board[3][3] = board[4][4] = WHITE_STONE;
        board[3][4] = board[4][3] = BLACK_STONE;

        // ���Ԃ���n�߂�
        flagForWhite = false;
        putNumber = 0;
    }

    /**
     * �Ֆʂ�`���B
     * 
     * @param g �`��I�u�W�F�N�g�B
     */
    private void drawBoard(Graphics g) {
        // �}�X��h��Ԃ�
        g.setColor(new Color(0, 128, 128));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                // �}�X�g��`�悷��
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
                if (board[y][x] == BLANK) {
                    continue;
                } else if (board[y][x] == BLACK_STONE) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillOval(x * GS + 3, y * GS + 3, GS - 6, GS - 6);
            }
        }
    }

    /**
     * �Ֆʂɐ΂�łB
     * 
     * @param x �΂�łꏊ��x���W�B
     * @param y �΂�łꏊ��y���W�B
     * @param tryAndError �R���s���[�^�̎v�l���������ǂ����B�v�l���͐΂�`�悵�Ȃ��B
     */
    public void putDownStone(int x, int y, boolean tryAndError) {
        int stone;

        // �ǂ����̎�Ԃ����ׂĐ΂̐F�����߂�
        if (flagForWhite) {
            stone = WHITE_STONE;
        } else {
            stone = BLACK_STONE;
        }
        // �΂�ł�
        board[y][x] = stone;
        // �R���s���[�^�̎v�l���łȂ���Ύ��ۂɑł��čĕ`�悷��
        if (!tryAndError) {
            putNumber++;
            // �J�`�b
            //kachi.play();
            // �Ֆʂ��X�V���ꂽ�̂ōĕ`��
            update(getGraphics());
            // ���x�~������i����Ȃ��Ƃ����ɂЂ�����Ԃ����n�܂��Ă��܂��j
            sleep();
        }
    }

    /**
     * �΂��łĂ邩�ǂ������ׂ�B
     * 
     * @param x �΂�łƂ��Ƃ��Ă���ꏊ��x���W�B
     * @param y �΂�łƂ��Ƃ��Ă���ꏊ��y���W�B
     * @return �΂��łĂ�Ȃ�true�A�łĂȂ��Ȃ�false��Ԃ��B
     *  
     */
    public boolean canPutDown(int x, int y) {
        // (x,y)���Ֆʂ̊O��������łĂȂ�
        if (x >= MASU || y >= MASU)
            return false;
        // (x,y)�ɂ��łɐ΂��ł���Ă���łĂȂ�
        if (board[y][x] != BLANK)
            return false;
        // 8�����̂�����ӏ��ł��Ђ�����Ԃ���΂��̏ꏊ�ɑłĂ�
        // �Ђ�����Ԃ��邩�ǂ����͂���1��canPutDown�Œ��ׂ�
        if (canPutDown(x, y, 1, 0))
            return true; // �E
        if (canPutDown(x, y, 0, 1))
            return true; // ��
        if (canPutDown(x, y, -1, 0))
            return true; // ��
        if (canPutDown(x, y, 0, -1))
            return true; // ��
        if (canPutDown(x, y, 1, 1))
            return true; // �E��
        if (canPutDown(x, y, -1, -1))
            return true; // ����
        if (canPutDown(x, y, 1, -1))
            return true; // �E��
        return canPutDown(x, y, -1, 1);

        // �ǂ̕��������߂ȏꍇ�͂����ɂ͑łĂȂ�
    }

    /**
     * vecX�AvecY�̕����ɂЂ�����Ԃ���΂����邩���ׂ�B
     * 
     * @param x �΂�łƂ��Ƃ��Ă���ꏊ��x���W�B
     * @param y �΂�łƂ��Ƃ��Ă���ꏊ��y���W�B
     * @param vecX ���ׂ����������x�����x�N�g���B
     * @param vecY ���ׂ����������y�����x�N�g���B
     * @return �΂��łĂ�Ȃ�true�A�łĂȂ��Ȃ�false��Ԃ��B
     *  
     */
    private boolean canPutDown(int x, int y, int vecX, int vecY) {
        int putStone;

        // �ł΂͂ǂꂩ
        if (flagForWhite) {
            putStone = WHITE_STONE;
        } else {
            putStone = BLACK_STONE;
        }

        // �ׂ̏ꏊ�ցB�ǂׂ̗���(vecX, vecY)�����߂�B
        x += vecX;
        y += vecY;
        // �ՖʊO��������łĂȂ�
        if (x < 0 || x >= MASU || y < 0 || y >= MASU)
            return false;
        // �ׂ������̐΂̏ꍇ�͑łĂȂ�
        if (board[y][x] == putStone)
            return false;
        // �ׂ��󔒂̏ꍇ�͑łĂȂ�
        if (board[y][x] == BLANK)
            return false;

        // ����ɗׂ𒲂ׂĂ���
        x += vecX;
        y += vecY;
        // �ƂȂ�ɐ΂�����ԃ��[�v���܂��
        while (x >= 0 && x < MASU && y >= 0 && y < MASU) {
            // �󔒂�����������łĂȂ��i�͂��߂Ȃ�����j
            if (board[y][x] == BLANK)
                return false;
            // �����̐΂�����΂͂��߂�̂őłĂ�
            if (board[y][x] == putStone) {
                return true;
            }
            x += vecX;
            y += vecY;
        }
        // ����̐΂����Ȃ��ꍇ�͂�����Ֆʂ̊O�ɂłĂ��܂��̂ł���false
        return false;
    }

    /**
     * �΂��Ђ�����Ԃ��B
     * 
     * @param x �΂�ł����ꏊ��x���W�B
     * @param y �΂�ł����ꏊ��y���W�B
     * @param tryAndError �R���s���[�^�̎v�l���������ǂ����B�v�l���͐΂�`�悵�Ȃ��B
     */
    public void reverse(Undo undo, boolean tryAndError) {
        // �Ђ�����Ԃ���΂���������͂��ׂĂЂ�����Ԃ�
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
     * @param x �΂�ł����ꏊ��x���W�B
     * @param y �΂�ł����ꏊ��y���W�B
     * @param vecX �Ђ�����Ԃ������������x�N�g���B
     * @param vecY �Ђ�����Ԃ������������x�N�g���B
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

        // ����̐΂�����ԂЂ�����Ԃ�������
        // (x,y)�ɑłĂ�̂͊m�F�ς݂Ȃ̂ő���̐΂͕K������
        x += vecX;
        y += vecY;
        while (board[y][x] != putStone) {
            // �Ђ�����Ԃ�
            board[y][x] = putStone;
            // �Ђ�����Ԃ����ꏊ���L�^���Ă���
            undo.pos[undo.count++] = new Point(x, y);
            if (!tryAndError) {
                // �J�`�b
                //kachi.play();
                // �Ֆʂ��X�V���ꂽ�̂ōĕ`��
                update(getGraphics());
                // ���x�~������i����Ȃ��ƕ����̐΂���ĂɂЂ�����Ԃ���Ă��܂��j
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
            // �Ђ�����Ԃ����ʒu���擾
            int x = undo.pos[c].x;
            int y = undo.pos[c].y;
            // ���ɖ߂��ɂ�-1��������΂悢
            // ��(1)�͔�(-1)�ɔ��͍��ɂȂ�
            board[y][x] *= -1;
            c++;
        }
        // �΂�łO�ɖ߂�
        board[undo.y][undo.x] = BLANK;
        // ��Ԃ����ɖ߂�
        nextTurn();
    }

    /**
     * ��Ԃ�ς���B
     *  
     */
    public void nextTurn() {
        // ��Ԃ�ς���
        flagForWhite = !flagForWhite;
    }

    /**
     * �΂��łĂ�ꏊ�̐��𐔂���B
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
     *  
     */
    private void sleep() {
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
    public void drawTextCentering(Graphics g, String s) {
        Font f = new Font("SansSerif", Font.BOLD, 20);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.YELLOW);
        g.drawString(s, WIDTH / 2 - fm.stringWidth(s) / 2, HEIGHT / 2
                + fm.getDescent());
    }

    /**
     * �Q�[�����I�����������ׂ�B
     *  
     */
    public boolean endGame() {
        // �ł��ꂽ�΂̐���60�i�S�����܂�����ԁj�ȊO�͉������Ȃ�
        if (putNumber == END_NUMBER) {
            // ���������̐΂𐔂���
            Counter counter;
            counter = countStone();
            // �����ߔ����i64/2=32�j������Ă����珟��
            // �ߔ����ȉ��Ȃ畉��
            // �������Ȃ��������
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
     *  
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