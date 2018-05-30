package nars.experiment.othello10;/*
 * �쐬��: 2004/12/17
 *
 */

import javax.swing.*;
import java.awt.*;


public class Othello extends JFrame {
    public Othello() {

        setResizable(false);

        Container contentPane = getContentPane();


        InfoPanel infoPanel = new InfoPanel();
        contentPane.add(infoPanel, BorderLayout.NORTH);

        MainPanel mainPanel = new MainPanel(infoPanel);
        contentPane.add(mainPanel, BorderLayout.CENTER);

        pack();
    }

    public static void main(String[] args) {
        Othello frame = new Othello();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}