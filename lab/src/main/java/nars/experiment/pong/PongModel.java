package nars.experiment.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PongModel extends JPanel implements ActionListener, MouseListener, KeyListener {

    private static final int RADIUS = 15;
    private static final int START_SPEED = 4;
    private static final int ACCELERATION = 110;


    public static final int SPEED = 15;
    public static final int PADDLE_HEIGHT = 40;
    public static final int WIDTH = 15;
    private static final int TOLERANCE = 5;
    private static final int PADDING = 0;

    public final Player player1;
    public final Player player2;

    private boolean new_game = true;

    public int ball_x;
    int ball_y;
    private double ball_x_speed;
    private double ball_y_speed;

    public boolean acceleration;
    private int ball_acceleration_count;

    private boolean mouse_inside;
    private boolean key_up;
    private boolean key_down;
    private static final double minBallYSpeed = 0.1f;


    public PongModel(Player player1, Player player2) {
        super();
        setBackground(new Color(0, 0, 0));

        this.player1 = player1;
        this.player2 = player2;
    }


    private void computeDestination(Player player) {
        if (ball_x_speed > 0)
            player.destination = ball_y + (getWidth() - PADDING - WIDTH - RADIUS - ball_x) * (int) (ball_y_speed) / (int) (ball_x_speed);
        else
            player.destination = ball_y - (ball_x - PADDING - WIDTH - RADIUS) * (int) (ball_y_speed) / (int) (ball_x_speed);

        if (player.destination <= RADIUS)
            player.destination = 2 * PADDING - player.destination;

        if (player.destination > getHeight() - 10) {
            player.destination -= RADIUS;
            if ((player.destination / (getHeight() - 2 * RADIUS)) % 2 == 0)
                player.destination %= (getHeight() - 2 * RADIUS);
            else
                player.destination = getHeight() - 2 * RADIUS - player.destination % (getHeight() - 2 * RADIUS);
            player.destination += RADIUS;
        }
    }


    public void movePlayer(Player player, int destination) {
        int distance = Math.abs(player.position - destination);

        if (distance != 0) {
            int direction = -(player.position - destination) / distance;

            if (distance > SPEED)
                distance = SPEED;

            player.position += direction * distance;

            if (player.position - PADDLE_HEIGHT < 0)
                player.position = PADDLE_HEIGHT;
            if (player.position + PADDLE_HEIGHT > getHeight())
                player.position = getHeight() - PADDLE_HEIGHT;
        }
    }


    float bgFlash;


    @Override
    public void paintComponent(Graphics g) {


        if (new_game) {
            bgFlash = 1f;
            ball_x = getWidth() / 2;
            ball_y = getHeight() / 2;

            double phase = Math.random() * Math.PI / 2 - Math.PI / 4;
            ball_x_speed = (int) (Math.cos(phase) * START_SPEED);
            ball_y_speed = (int) (Math.sin(phase) * START_SPEED);

            ball_acceleration_count = 0;

            /*if (player1.getType() == Player.CPU_HARD || player1.getType() == Player.CPU_EASY)*/
            computeDestination(player1);
            /*if (player2.getType() == Player.CPU_HARD || player2.getType() == Player.CPU_EASY)*/
            computeDestination(player2);

            new_game = false;
        }

        bgFlash *= 0.95f;


        super.paintComponent(g);


        player1.computePosition(this);


        player2.computePosition(this);


        ball_x += ball_x_speed;
        ball_y += ball_y_speed;


        if (acceleration) {
            ball_acceleration_count++;
            if (ball_acceleration_count == ACCELERATION) {
                ball_x_speed += (int) ball_x_speed / Math.hypot((int) ball_x_speed, (int) ball_y_speed) * 2;
                ball_y_speed += (int) ball_y_speed / Math.hypot((int) ball_x_speed, (int) ball_y_speed) * 2;
                ball_acceleration_count = 0;
            }
        }


        int bounceLX = PADDING + WIDTH + RADIUS;

        if (ball_x <= bounceLX) {
            int collision_point = ball_y + (int) (ball_y_speed / ball_x_speed * (PADDING + WIDTH + RADIUS - ball_x));
            int dieLX = RADIUS;
            if (ball_x <= bounceLX && collision_point > player1.position - PADDLE_HEIGHT - TOLERANCE &&
                    collision_point < player1.position + PADDLE_HEIGHT + TOLERANCE) {
                ball_x = 2 * (PADDING + WIDTH + RADIUS) - ball_x;
                player1.bounce();
                ball_x_speed = Math.abs(ball_x_speed);
                ball_y_speed -= Math.sin((double) (player1.position - ball_y) / PADDLE_HEIGHT * Math.PI / 4)
                        * Math.hypot(ball_x_speed, ball_y_speed);

                computeDestination(player2);
            } else if (ball_x <= dieLX) {
                player2.score();
                player1.ouch();
                new_game = true;
            }
        }


        int bounceRX = getWidth() - PADDING - WIDTH - RADIUS;
        int dieRX = getWidth() - RADIUS;
        if (ball_x >= bounceRX) {
            int collision_point = ball_y - (int) (ball_y_speed / ball_x_speed * (ball_x - getWidth() + PADDING + WIDTH + RADIUS));
            if (ball_x >= bounceRX && collision_point > player2.position - PADDLE_HEIGHT - TOLERANCE &&
                    collision_point < player2.position + PADDLE_HEIGHT + TOLERANCE) {
                ball_x = 2 * (bounceRX) - ball_x;
                ball_x_speed = -1 * Math.abs(ball_x_speed);
                player2.bounce();
                ball_y_speed -= Math.sin((double) (player2.position - ball_y) / PADDLE_HEIGHT * Math.PI / 4)
                        * Math.hypot(ball_x_speed, ball_y_speed);

                computeDestination(player1);
            } else if (ball_x >= dieRX) {
                player1.score();
                player2.ouch();
                new_game = true;
            }
        }


        if (ball_y <= RADIUS) {
            ball_y_speed = Math.abs(ball_y_speed);
            ball_y = 2 * RADIUS - ball_y;
        }


        if (ball_y >= getHeight() - RADIUS) {
            ball_y_speed = -1 * Math.abs(ball_y_speed);
            ball_y = 2 * (getHeight() - RADIUS) - ball_y;
        }


        g.setColor(new Color(1f - bgFlash, 1f - bgFlash, 1f - bgFlash));

        g.fillRect(PADDING, player1.position - PADDLE_HEIGHT, WIDTH, PADDLE_HEIGHT * 2);
        g.fillRect(getWidth() - PADDING - WIDTH, player2.position - PADDLE_HEIGHT, WIDTH, PADDLE_HEIGHT * 2);


        g.fillOval(ball_x - RADIUS, ball_y - RADIUS, RADIUS * 2, RADIUS * 2);


        g.drawString(player1.points + " ", getWidth() / 2 - 20, 20);
        g.drawString(player2.points + " ", getWidth() / 2 + 20, 20);


    }


    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }


    @Override
    public void mouseEntered(MouseEvent e) {
        mouse_inside = true;
    }


    @Override
    public void mouseExited(MouseEvent e) {
        mouse_inside = false;
    }


    @Override
    public void mousePressed(MouseEvent e) {
    }


    @Override
    public void mouseReleased(MouseEvent e) {
    }


    @Override
    public void mouseClicked(MouseEvent e) {
    }


    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_UP)
            key_up = true;
        else if (e.getKeyCode() == KeyEvent.VK_DOWN)
            key_down = true;
    }


    @Override
    public void keyReleased(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_UP)
            key_up = false;
        else if (e.getKeyCode() == KeyEvent.VK_DOWN)
            key_down = false;
    }


    @Override
    public void keyTyped(KeyEvent e) {
    }
}
