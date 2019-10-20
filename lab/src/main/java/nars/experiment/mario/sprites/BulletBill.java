package nars.experiment.mario.sprites;

import nars.experiment.mario.Art;
import nars.experiment.mario.LevelScene;


public class BulletBill extends Sprite {
    @SuppressWarnings("unused")
    private static final int width = 4;
    int height = 24;

    private final LevelScene world;
    public int facing;

    public boolean avoidCliffs;
    public int anim;

    public boolean dead;
    private int deadTime;


    public BulletBill(LevelScene world, float x, float y, int dir) {
        sheet = Art.enemies;

        this.x = x;
        this.y = y;
        this.world = world;
        xPicO = 8;
        yPicO = 31;

        height = 12;
        facing = 0;
        wPic = 16;
        yPic = 5;

        xPic = 0;
        ya = -5;
        this.facing = dir;
    }

    @Override
    public void collideCheck() {
        if (dead) return;

        var xMarioD = world.mario.x - x;
        var yMarioD = world.mario.y - y;
        float w = 16;
        if (xMarioD > -w && xMarioD < w) {
            if (yMarioD > -height && yMarioD < world.mario.height) {
                if (world.mario.ya > 0 && yMarioD <= 0 && (!world.mario.onGround || !world.mario.wasOnGround)) {
                    world.mario.stomp(this);
                    dead = true;

                    xa = 0;
                    ya = 1;
                    deadTime = 100;
                } else {
                    world.mario.getHurt();
                }
            }
        }
    }

    @Override
    public void move() {
        if (deadTime > 0) {
            deadTime--;

            if (deadTime == 0) {
                deadTime = 1;
                for (var i = 0; i < 8; i++) {
                    world.addSprite(new Sparkle((int) (x + Math.random() * 16 - 8) + 4, (int) (y - Math.random() * 8) + 4, (float) (Math.random() * 2 - 1), (float) Math.random() * -1, 0, 1, 5));
                }
                spriteContext.removeSprite(this);
            }

            x += xa;
            y += ya;
            ya *= 0.95;
            ya += 1;

            return;
        }

        var sideWaysSpeed = 4f;

        xa = facing * sideWaysSpeed;
        xFlipPic = facing == -1;
        move(xa, 0);
    }

    private boolean move(float xa, float ya) {
        x += xa;
        return true;
    }

    @Override
    public boolean fireballCollideCheck(Fireball fireball) {
        if (deadTime != 0) return false;

        var xD = fireball.x - x;
        var yD = fireball.y - y;

        if (xD > -16 && xD < 16) {
            return yD > -height && yD < fireball.height;
        }
        return false;
    }

    @Override
    public boolean shellCollideCheck(Shell shell) {
        if (deadTime != 0) return false;

        var xD = shell.x - x;
        var yD = shell.y - y;

        if (xD > -16 && xD < 16) {
            if (yD > -height && yD < shell.height) {


                dead = true;

                xa = 0;
                ya = 1;
                deadTime = 100;

                return true;
            }
        }
        return false;
    }
}