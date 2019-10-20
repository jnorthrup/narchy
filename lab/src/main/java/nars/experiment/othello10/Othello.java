package nars.experiment.othello10;/*
 * �쐬��: 2004/12/17
 *
 */

import javax.swing.*;
import java.awt.*;


public class Othello extends JFrame {
    public Othello() {

        setResizable(false);

        var contentPane = getContentPane();


        var infoPanel = new InfoPanel();
        contentPane.add(infoPanel, BorderLayout.NORTH);

        var mainPanel = new MainPanel(infoPanel);
        contentPane.add(mainPanel, BorderLayout.CENTER);

        pack();
    }

    public static void main(String[] args) {
        var frame = new Othello();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}