package nars.experiment.bomberman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * File:         BomberMain.java
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

/**
 * This is the starting point of the game.
 */
public class BomberMain extends JFrame {
    /** relative path for files */
    public static String RP = BomberMenu.class.getResource(".").getPath();
    /** menu object */
    private BomberMenu menu;
    /** game object */
    private BomberGame game;

    /** sound effect player */
    public static BomberSndEffect sndEffectPlayer;
    /** this is used to calculate the dimension of the game */
    public static final int shiftCount = 4;
    /** this is the size of each square in the game */
    public static final int size = 1 << shiftCount;

    static {
        sndEffectPlayer = new BomberSndEffect();
    }

    /**
     * Constructs the main frame.
     */
    public BomberMain() {
        /** addAt window event handler */
        addWindowListener(new WindowAdapter() {
            /**
             * Handles window closing events.
             * @param evt window event
             */
            @Override
            public void windowClosing(WindowEvent evt) {
                /** terminate the program */
                System.exit(0);
            }
        });

        /** addAt keyboard event handler */
        addKeyListener(new KeyAdapter() {
            /**
             * Handles key pressed events.
             * @param evt keyboard event
             */
            @Override
            public void keyPressed(KeyEvent evt) {
                if (menu != null) menu.keyPressed(evt);
                if (game != null) game.keyPressed(evt);
            }

            /**
             * Handles key released events.
             * @param evt keyboard event
             */
            @Override
            public void keyReleased(KeyEvent evt) {
                if (game != null) game.keyReleased(evt);
            }
        });

        /** set the window title */
        setTitle("Bomberman 1.0 by Sammy Leong");


        /** create and addAt the menu to the frame */
        getContentPane().add(menu = new BomberMenu(this));

        /** set the window so that the user can't resize it */
        setResizable(false);
        /** minimize the size of the window */
        pack();

        /** get screen size */
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        int x = (d.width - getSize().width) / 2;
        int y = (d.height - getSize().height) / 2;

        /** center the window on the screen */
        setLocation(x, y);
        /** show the frame */
        show();
        /** make this window the top level window */
        toFront();
    }

    /**
     * Creates a new game.
     * @param players total number of players
     */
    public void newGame(int players) {
        JDialog dialog = new JDialog(this, "Loading Game...", false);
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        dialog.setSize(new Dimension(200, 0));
        dialog.setResizable(false);
        int x = getLocation().x + (getSize().width - 200) / 2;
        int y = getLocation().y + getSize().height / 2;
        dialog.setLocation(x, y);
        /** show the dialog */
        dialog.show();

        /** remove existing panels in the content pane */
        getContentPane().removeAll();
        getLayeredPane().removeAll();
        /** get rid of the menu */
        menu = null;
        /** create the map */
        BomberMap map = new BomberMap(this);

        /** create the game */
        game = new BomberGame(this, map, players);

        /** get rid of loading dialog */
        dialog.dispose();
        /** show the frame */
        show();


    }

    /**
     * Starting ponit of program.
     * @param args arguments
     */
    public static void main(String[] args) {
        BomberMain bomberMain1 = new BomberMain();
    }
}
