package nars.experiment.bomberman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * File:         BomberConfigDialog
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

/**
 * This class shows a dialog which allows the user to
 * configure the game.
 */
public class BomberConfigDialog extends JDialog
        implements ActionListener {
    /** temporary key datas used for manipulation */
    private final int[][] keys;
    /** keys being set offset values */
    private final int[] keysBeingSet = {-1, -1};
    /** waiting for key flag */
    private boolean waitingForKey;
    /** the buttons that allow the user to set the keys */
    private final JButton[][] buttons;
    /** the text fields that display the keys */
    private final JTextField[][] keyFields;

    /**
     * Constructs the dialog.
     * @param owner the dialog's owner
     */
    public BomberConfigDialog(JFrame owner) {
        /** call base class constructor */
        super(owner, "Bomberman Keys Configuration", true);

        /** create the temporary key objects */
        keys = new int[4][5];
        /** set the object's data from the currently configurations */
        for (var i = 0; i < 4; i++)
            System.arraycopy(BomberKeyConfig.keys[i], 0, keys[i], 0, 5);

        /** create the panel that holds the config. stuff */
        var centerPanel = new JPanel(new GridLayout(2, 2));
        /** panel for each player's config. stuff */
        var panels = new JPanel[4];
        /** create the text fields array of array */
        keyFields = new JTextField[4][];
        /** create the buttons array */
        buttons = new JButton[4][5];
        for (var i = 0; i < 4; i++) {
            /** create the key fields array */
            keyFields[i] = new JTextField[5];
            /** create the 4 panels */
            setupPanel(i, centerPanel, keyFields[i]);
        }

        /** create the panel to display the help message */
        var helpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        /** setup the border */
        helpPanel.setBorder(BorderFactory.createEtchedBorder());
        /** add a label to it */
        helpPanel.add(new JLabel("Click on the buttons to edit the keys.",
                SwingConstants.CENTER));
        /** add the help panel to the north side of the dialog */
        getContentPane().add(helpPanel, "North");
        /** add the key setup panels to the center */
        getContentPane().add(centerPanel, "Center");
        /** create the panel to hold the buttons */
        var buttonsP = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsP.setBorder(BorderFactory.createEtchedBorder());
        /** create the save configuration button */
        var saveButton = new JButton("Save Configurations");
        saveButton.addActionListener(this);
        buttonsP.add(saveButton);
        /** create the close button */
        var closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        buttonsP.add(closeButton);
        /** add the buttons panel to the south side of the dialog */
        getContentPane().add(buttonsP, "South");

        /** set the dialog so user can't resize it */
        setResizable(false);
        /** minimize the size of the dialog */
        pack();

        var x = owner.getLocation().x + (owner.getSize().width -
                getSize().width) / 2;
        var y = owner.getLocation().y + (owner.getSize().height -
                getSize().height) / 2;

        /** center the dialog relative to the owner */
        setLocation(x, y);
        /** finally, show the dialog */
        show();
    }

    /**
     * Creates a panel for each player.
     * @param pn player number
     * @param m master panel
     * @param fields key fields
     */
    private void setupPanel(int pn, JPanel m, JTextField[] fields) {
        /** create the left and right panels, 5 rows each */
        var left = new JPanel(new GridLayout(5, 1));
        var right = new JPanel(new GridLayout(5, 1));

        /** create the buttons and text fields */
        for (var i = 0; i < 5; i++) {
            /** create the button */
            buttons[pn][i] = new JButton();
            /** create the text field */
            fields[i] = new JTextField(10);
            /** setup the button */
            switch (i) {
                case 0:
                    buttons[pn][i].setText("Up");
                    break;
                case 1:
                    buttons[pn][i].setText("Down");
                    break;
                case 2:
                    buttons[pn][i].setText("Left");
                    break;
                case 3:
                    buttons[pn][i].setText("Right");
                    break;
                case 4:
                    buttons[pn][i].setText("Bomb");
                    break;
            }
            /** add action handler to the button */
            buttons[pn][i].addActionListener(this);
            /** setup the text field from data */
            fields[i].setText(KeyEvent.getKeyText(keys[pn][i]));
            /** user can't edit the text field */
            fields[i].setEditable(false);
            /** add the button to the left side */
            left.add(buttons[pn][i]);
            /** add the text field to the right side */
            right.add(fields[i]);
        }

        /** create the player's panel */
        var p = new JPanel(new GridLayout(1, 2));
        /** set the border */
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.
                createEtchedBorder(), "Player " + (pn + 1) + " Keys Configuration"));
        /** add the buttons and the keys to the panel */
        p.add(left);
        p.add(right);
        /** add the panel to the master panel */
        m.add(p);
    }

    /**
     * Action event handler.
     * @param evt action event info.
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        /** if save configuration button is clicked */
        if ("Save Configurations".equals(evt.getActionCommand())) {
            /** copy new keys back to glocal variables */
            for (var i = 0; i < 4; i++)
                System.arraycopy(keys[i], 0, BomberKeyConfig.keys[i], 0, 5);
            /** write the file */
            BomberKeyConfig.writeFile();
            /** destroy this dialog */
            dispose();
        }
        /** if close button is clicked then destroy the dialog */
        else if ("Close".equals(evt.getActionCommand())) dispose();
        /** if other buttons are clicked */
        else {
            /** find which key setup button is clicked */
            int i = 0, j = 0;
            var found = false;
            for (i = 0; i < 4; ++i) {
                for (j = 0; j < 5; ++j) {
                    /** if key found then exit the loop */
                    if (evt.getSource().equals(buttons[i][j])) found = true;
                    if (found) break;
                }
                if (found) break;
            }
            /** set keys being set indexes */
            keysBeingSet[0] = i;
            keysBeingSet[1] = j;
            /** setup get key loop */
            waitingForKey = true;
            /** create the get key dialog */
            new GetKeyDialog(this, "Press a key to be assigned...", true);
        }
    }

    /**
     * This class gets a new key from the user then set it.
     */
    private class GetKeyDialog extends JDialog {
        /** points to itself */
        private final JDialog me;

        /**
         * Constructs a new dialog.
         * @param owner dialog's owner
         * @param title dialog title
         * @param modal modal or not
         */
        public GetKeyDialog(JDialog owner, String title, boolean modal) {
            /** call base class constructor */
            setTitle(title);
            setModal(modal);
            /** setup pointer to point to itself */
            me = this;

            /** add keyboard event handler */
            addKeyListener(new KeyAdapter() {
                /**
                 * Handles key pressed events.
                 * @param evt keyboard event
                 */
                @Override
                public void keyPressed(KeyEvent evt) {
                    /** if it's waiting for a key */
                    if (waitingForKey) {
                        /** get index of key to set */
                        var i = keysBeingSet[0];
                        var j = keysBeingSet[1];
                        /** get the key pressed */
                        var newKey = evt.getKeyCode();
                        /** key used flag */
                        var keyUsed = false;
                        /** see if the key is used already or not */
                        for (var p = 0; p < 4; ++p) {
                            for (var k = 0; k < 5; ++k) {
                                /** if key is used already */
                                if (keys[p][k] == newKey) {
                                    /** if it isn't the key being set */
                                    if (!(p == i && j == k))
                                    /** set key used flag to true */
                                        keyUsed = true;
                                }
                                /** if key used flag is true, then exit loop */
                                if (keyUsed) break;
                            }
                            /** if key used flag is true, then exit loop */
                            if (keyUsed) break;
                        }
                        /** if key isn't used */
                        if (!keyUsed) {
                            /** copy new key */
                            keys[i][j] = newKey;
                            /** reset the key field */
                            keyFields[i][j].setText(
                                    KeyEvent.getKeyText(keys[i][j]));
                            /** set waiting for key to false */
                            waitingForKey = false;
                            /** destroy the dialog */
                            dispose();
                        }
                        /** if key is used already */
                        else {
                            /** then show an error dialog */
                            /** create the dialog content */
                            var pane = new JOptionPane(
                                    "Key: [" + KeyEvent.getKeyText(newKey) +
                                            "] is used already.  Pick a different key.");
                            /** setup the dialog controls */
                            pane.setOptionType(-JOptionPane.NO_OPTION);
                            pane.setMessageType(JOptionPane.ERROR_MESSAGE);
                            /** create the dialog */
                            var dialog = pane.createDialog(me, "Error");
                            /** set it so user can't resize the dialog */
                            dialog.setResizable(false);
                            /** show the dialog */
                            dialog.show();
                        }
                    }
                }
            });

            /** set the dialog so the user can't resize it */
            setResizable(false);
            /** set dialog size */
            setSize(300, 0);

            var x = owner.getLocation().x + (owner.getSize().width -
                    getSize().width) / 2;
            var y = owner.getLocation().y + (owner.getSize().width -
                    getSize().height) / 2;
            /** center the dialog relative to the owner */
            setLocation(x, y);

            /** finally show the dialog */
            show();
        }
    }
}
