package nars.experiment.othello10;/*
 * Created on 2004/12/22
 *
 */

/**
 * �I�Z����AI�B
 *
 * @author mori
 */
public class AI {

    private static final int SEARCH_LEVEL = 7;

    private final MainPanel panel;

    private static final int valueOfPlace[][] = {
            {120, -20, 20, 5, 5, 20, -20, 120},
            {-20, -40, -5, -5, -5, -5, -40, -20},
            {20, -5, 15, 3, 3, 15, -5, 20},
            {5, -5, 3, 3, 3, 3, -5, 5},
            {5, -5, 3, 3, 3, 3, -5, 5},
            {20, -5, 15, 3, 3, 15, -5, 20},
            {-20, -40, -5, -5, -5, -5, -40, -20},
            {120, -20, 20, 5, 5, 20, -20, 120}
    };

    /**
     * �R���X�g���N�^�B���C���p�l���ւ̎Q�Ƃ�ۑ��B
     *
     * @param panel ���C���p�l���ւ̎Q�ƁB
     */
    public AI(MainPanel panel) {
        this.panel = panel;
    }

    /**
     * �R���s���[�^�̎�����肷��B
     */
    public void compute() {


        int temp = alphaBeta(true, SEARCH_LEVEL, Integer.MIN_VALUE, Integer.MAX_VALUE);


        int x = temp % MainPanel.MASU;
        int y = temp / MainPanel.MASU;


        Undo undo = new Undo(x, y);

        panel.putDownStone(x, y, false);

        panel.reverse(undo, false);

        if (panel.endGame()) return;

        panel.nextTurn();

        if (panel.countCanPutDownStone() == 0) {
            System.out.println("Player PASS!");
            panel.nextTurn();
            compute();
        }
    }

    /**
     * Min-Max�@�B�őP���T������B�łꏊ��T�������Ŏ��ۂɂ͑ł��Ȃ��B
     *
     * @param flag  AI�̎�Ԃ̂Ƃ�true�A�v���C���[�̎�Ԃ̂Ƃ�false�B
     * @param level ��ǂ݂̎萔�B
     * @return �q�m�[�h�ł͔Ֆʂ̕]���l�B���[�g�m�[�h�ł͍ő�]���l�����ꏊ�ibestX + bestY * MAS�j�B
     */
    private int minMax(boolean flag, int level) {

        int value;

        int childValue;

        int bestX = 0;
        int bestY = 0;


        if (level == 0) {
            return valueBoard();
        }

        if (flag) {

            value = Integer.MIN_VALUE;
        } else {

            value = Integer.MAX_VALUE;
        }


        if (panel.countCanPutDownStone() == 0) {
            return valueBoard();
        }


        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {
                if (panel.canPutDown(x, y)) {
                    Undo undo = new Undo(x, y);

                    panel.putDownStone(x, y, true);

                    panel.reverse(undo, true);

                    panel.nextTurn();


                    childValue = minMax(!flag, level - 1);

                    if (flag) {

                        if (childValue > value) {
                            value = childValue;
                            bestX = x;
                            bestY = y;
                        }
                    } else {

                        if (childValue < value) {
                            value = childValue;
                            bestX = x;
                            bestY = y;
                        }
                    }

                    panel.undoBoard(undo);
                }
            }
        }

        if (level == SEARCH_LEVEL) {

            return bestX + bestY * MainPanel.MASU;
        } else {

            return value;
        }
    }

    /**
     * ��-���@�B�őP���T������B�łꏊ��T�������Ŏ��ۂɂ͑ł��Ȃ��B
     *
     * @param flag  AI�̎�Ԃ̂Ƃ�true�A�v���C���[�̎�Ԃ̂Ƃ�false�B
     * @param level ��ǂ݂̎萔�B
     * @param alpha ���l�B���̃m�[�h�̕]���l�͕K�����l�ȏ�ƂȂ�B
     * @param beta  ���l�B���̃m�[�h�̕]���l�͕K�����l�ȉ��ƂȂ�B
     * @return �q�m�[�h�ł͔Ֆʂ̕]���l�B���[�g�m�[�h�ł͍ő�]���l�����ꏊ�ibestX + bestY * MAS�j�B
     */
    private int alphaBeta(boolean flag, int level, int alpha, int beta) {

        int value;

        int childValue;

        int bestX = 0;
        int bestY = 0;


        if (level == 0) {
            return valueBoard();
        }

        if (flag) {

            value = Integer.MIN_VALUE;
        } else {

            value = Integer.MAX_VALUE;
        }


        if (panel.countCanPutDownStone() == 0) {
            return valueBoard();
        }


        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {
                if (panel.canPutDown(x, y)) {
                    Undo undo = new Undo(x, y);

                    panel.putDownStone(x, y, true);

                    panel.reverse(undo, true);

                    panel.nextTurn();


                    childValue = alphaBeta(!flag, level - 1, alpha, beta);

                    if (flag) {

                        if (childValue > value) {
                            value = childValue;

                            alpha = value;
                            bestX = x;
                            bestY = y;
                        }


                        if (value > beta) {


                            panel.undoBoard(undo);
                            return value;
                        }
                    } else {

                        if (childValue < value) {
                            value = childValue;

                            beta = value;
                            bestX = x;
                            bestY = y;
                        }


                        if (value < alpha) {


                            panel.undoBoard(undo);
                            return value;
                        }
                    }

                    panel.undoBoard(undo);
                }
            }
        }

        if (level == SEARCH_LEVEL) {

            return bestX + bestY * MainPanel.MASU;
        } else {

            return value;
        }
    }

    /**
     * �]���֐��B�Ֆʂ�]�����ĕ]���l��Ԃ��B�Ֆʂ̏ꏊ�̉��l�����ɂ���B
     *
     * @return �Ֆʂ̕]���l�B
     */
    private int valueBoard() {
        int value = 0;

        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {

                value += panel.getBoard(x, y) * valueOfPlace[y][x];
            }
        }


        return -value;
    }
}