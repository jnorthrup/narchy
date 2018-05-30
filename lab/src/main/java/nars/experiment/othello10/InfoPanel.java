package nars.experiment.othello10;/*
 * �쐬��: 2004/12/18
 *
 */

import javax.swing.*;

/**
 * @author mori
 *  
 */
public class InfoPanel extends JPanel {
    private final JLabel blackLabel;
    private final JLabel whiteLabel;

    public InfoPanel() {
        add(new JLabel("BLACK:"));
        blackLabel = new JLabel("0");
        add(blackLabel);
        add(new JLabel("WHITE:"));
        whiteLabel = new JLabel("0");
        add(whiteLabel);
    }

    /**
     * BLACK���x���ɒl���Z�b�g����B
     * 
     * @param count �Z�b�g���鐔���B
     *  
     */
    public void setBlackLabel(int count) {
        blackLabel.setText(count + "");
    }

    /**
     * WHITE���x���ɒl���Z�b�g����B
     * 
     * @param text �Z�b�g���鐔���B
     *  
     */
    public void setWhiteLabel(int count) {
        whiteLabel.setText(count + "");
    }
}