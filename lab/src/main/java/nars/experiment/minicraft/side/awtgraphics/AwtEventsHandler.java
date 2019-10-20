package nars.experiment.minicraft.side.awtgraphics;

import nars.experiment.minicraft.side.SideScrollMinicraft;

import java.awt.event.*;


public class AwtEventsHandler {
    final SideScrollMinicraft game;

    public AwtEventsHandler(SideScrollMinicraft game) {
        this.game = game;


    }

    private class MouseWheelInputHander implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            var notches = e.getWheelRotation();
            game.player.inventory.hotbarIdx += notches;
            if (game.player.inventory.hotbarIdx < 0) {
                game.player.inventory.hotbarIdx = 0;
            } else if (game.player.inventory.hotbarIdx > 9) {
                game.player.inventory.hotbarIdx = 9;
            }
        }
    }

    private class MouseMoveInputHander implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent arg0) {
            game.screenMousePos.x = arg0.getX();
            game.screenMousePos.y = arg0.getY();
        }

        @Override
        public void mouseMoved(MouseEvent arg0) {
            game.screenMousePos.x = arg0.getX();
            game.screenMousePos.y = arg0.getY();
        }
    }

    private class MouseInputHander extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent arg0) {
            switch (arg0.getButton()) {
                case MouseEvent.BUTTON1:
                    game.leftClick = true;
                    break;
                case MouseEvent.BUTTON2:
                case MouseEvent.BUTTON3:
                    game.rightClick = true;
                    break;
            }
        }

        @Override
        public void mouseReleased(MouseEvent arg0) {
            switch (arg0.getButton()) {
                case MouseEvent.BUTTON1:
                    game.leftClick = false;
                    break;
                case MouseEvent.BUTTON2:
                case MouseEvent.BUTTON3:
                    game.rightClick = false;
                    break;
            }
        }
    }

    private class KeyInputHandler extends KeyAdapter {
        /**
         * Notification from AWT that a key has been pressed. Note that
         * a key being pressed is equal to being pushed down but *NOT*
         * released. Thats where keyTyped() comes in.
         *
         * @param e The details of the key that was pressed
         */
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_SPACE:
                    game.player.startClimb();
                    break;
                case KeyEvent.VK_A:
                    game.player.startLeft(e.isShiftDown());
                    break;
                case KeyEvent.VK_D:
                    game.player.startRight(e.isShiftDown());
                    break;
            }
        }

        /**
         * Notification from AWT that a key has been released.
         *
         * @param e The details of the key that was released
         */
        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_SPACE:
                    game.player.stopClimb();
                    break;
                case KeyEvent.VK_A:
                    game.player.stopLeft();
                    break;
                case KeyEvent.VK_D:
                    game.player.stopRight();
                    break;
                case KeyEvent.VK_ESCAPE:
                    if (game.player.inventory.isVisible()) {
                        game.player.inventory.setVisible(false);
                    } else {
                        game.goToMainMenu();
                    }
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            switch (e.getKeyChar()) {
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    game.player.setHotbarItem(e.getKeyChar() - '1');
                    break;
                case '0':
                    game.player.setHotbarItem(9);
                    break;
                case 'e':
                    game.player.inventory.setVisible(!game.player.inventory.isVisible());
                    break;
                case '=':
                    game.zoom(1);
                    break;
                case 'p':
                    game.paused = !game.paused;
                    break;


                case 'o':
                    game.zoom(0);
                    break;
                case '-':
                    game.zoom(-1);
                    break;
                case 'f':
                    game.viewFPS = !game.viewFPS;
                    break;
                case 'q':
                    game.tossItem();
                    break;
            }
        }
    }
}
