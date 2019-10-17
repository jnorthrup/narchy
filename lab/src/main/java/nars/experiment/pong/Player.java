package nars.experiment.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

public abstract class Player {

    public abstract void computePosition(PongModel pong);

    public void bounce() {
        points++;
    }

    public void score() {
        points++;
    }

    public void ouch() {
        points--;
    }


    public void moveTo(int y, PongModel pong) {
        position = y;
        clip(pong);
    }

    public void move(int dy, PongModel pong) {
        position += dy;
        clip(pong);
    }

    public void clip(PongModel pong) {
        int maxPos = pong.getHeight() - PongModel.PADDLE_HEIGHT;
        int minPos = PongModel.PADDLE_HEIGHT;
        if (position > maxPos)
            position = maxPos;
        if (position < minPos)
            position = minPos;
    }


    public static final class CPU_EASY extends Player {

        float noise = 10f;

        @Override
        public void computePosition(PongModel pong) {

            pong.movePlayer(this, pong.ball_y + (int) (Math.random() * noise));
        }
    }

    public static final class CPU_HARD extends Player {

        @Override
        public void computePosition(PongModel pong) {
            pong.movePlayer(this, destination);
        }
    }

    public int position;
    public int destination;
    public float points;


}
