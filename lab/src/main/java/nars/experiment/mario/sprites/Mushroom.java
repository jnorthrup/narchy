package nars.experiment.mario.sprites;

import nars.experiment.mario.Art;
import nars.experiment.mario.LevelScene;


public class Mushroom extends Sprite {
    private static final float GROUND_INERTIA = 0.89f;
    private static final float AIR_INERTIA = 0.89f;

    private float runTime;
    private boolean onGround;
    @SuppressWarnings("unused")
    private float xJumpSpeed;
    @SuppressWarnings("unused")
    private float yJumpSpeed;

    private static final int width = 4;
    int height = 24;

    private final LevelScene world;
    public int facing;

    public boolean avoidCliffs;
    private int life;

    public Mushroom(LevelScene world, int x, int y) {
        sheet = Art.items;

        this.x = (float) x;
        this.y = (float) y;
        this.world = world;
        xPicO = 8;
        yPicO = 15;

        yPic = 0;
        height = 12;
        facing = 1;
        wPic = hPic = 16;
        life = 0;
    }

    @Override
    public void collideCheck() {
        float xMarioD = world.mario.x - x;
        float yMarioD = world.mario.y - y;
        float w = 16.0F;
        if (xMarioD > -w && xMarioD < w) {
            if (yMarioD > (float) -height && yMarioD < (float) world.mario.height) {
                world.mario.getMushroom();
                spriteContext.removeSprite(this);
            }
        }
    }

    @Override
    public void move() {
        if (life < 9) {
            layer = 0;
            y--;
            life++;
            return;
        }
        layer = 1;


        if (xa > 2.0F) {
            facing = 1;
        }
        if (xa < -2.0F) {
            facing = -1;
        }

        float sideWaysSpeed = 1.75f;
        xa = (float) facing * sideWaysSpeed;

        boolean mayJump = (onGround);

        xFlipPic = facing == -1;

        runTime += (Math.abs(xa)) + 5.0F;


        if (!move(xa, (float) 0)) facing = -facing;
        onGround = false;
        move((float) 0, ya);

        ya *= 0.85f;
        if (onGround) {
            xa *= GROUND_INERTIA;
        } else {
            xa *= AIR_INERTIA;
        }

        if (!onGround) {
            ya += 2.0F;
        }
    }

    private boolean move(float xa, float ya) {
        while (xa > 8.0F) {
            if (!move(8.0F, (float) 0)) return false;
            xa -= 8.0F;
        }
        while (xa < -8.0F) {
            if (!move(-8.0F, (float) 0)) return false;
            xa += 8.0F;
        }
        while (ya > 8.0F) {
            if (!move((float) 0, 8.0F)) return false;
            ya -= 8.0F;
        }
        while (ya < -8.0F) {
            if (!move((float) 0, -8.0F)) return false;
            ya += 8.0F;
        }

        boolean collide = false;
        if (ya > (float) 0) {
            if (isBlocking(x + xa - (float) width, y + ya, xa, (float) 0)) collide = true;
            else if (isBlocking(x + xa + (float) width, y + ya, xa, (float) 0)) collide = true;
            else if (isBlocking(x + xa - (float) width, y + ya + 1.0F, xa, ya)) collide = true;
            else if (isBlocking(x + xa + (float) width, y + ya + 1.0F, xa, ya)) collide = true;
        }
        if (ya < (float) 0) {
            if (isBlocking(x + xa, y + ya - (float) height, xa, ya)) collide = true;
            else if (collide || isBlocking(x + xa - (float) width, y + ya - (float) height, xa, ya)) collide = true;
            else if (collide || isBlocking(x + xa + (float) width, y + ya - (float) height, xa, ya)) collide = true;
        }
        if (xa > (float) 0) {
            if (isBlocking(x + xa + (float) width, y + ya - (float) height, xa, ya)) collide = true;
            if (isBlocking(x + xa + (float) width, y + ya - (float) (height / 2), xa, ya)) collide = true;
            if (isBlocking(x + xa + (float) width, y + ya, xa, ya)) collide = true;

            if (avoidCliffs && onGround && !world.level.isBlocking((int) ((x + xa + (float) width) / 16.0F), (int) ((y) / 16.0F + 1.0F), xa, 1.0F))
                collide = true;
        }
        if (xa < (float) 0) {
            if (isBlocking(x + xa - (float) width, y + ya - (float) height, xa, ya)) collide = true;
            if (isBlocking(x + xa - (float) width, y + ya - (float) (height / 2), xa, ya)) collide = true;
            if (isBlocking(x + xa - (float) width, y + ya, xa, ya)) collide = true;

            if (avoidCliffs && onGround && !world.level.isBlocking((int) ((x + xa - (float) width) / 16.0F), (int) ((y) / 16.0F + 1.0F), xa, 1.0F))
                collide = true;
        }

        if (collide) {
            if (xa < (float) 0) {
                x = (float) ((int) ((x - (float) width) / 16.0F) * 16 + width);
                this.xa = (float) 0;
            }
            if (xa > (float) 0) {
                x = (float) ((int) ((x + (float) width) / 16.0F + 1.0F) * 16 - width - 1);
                this.xa = (float) 0;
            }
            if (ya < (float) 0) {
                y = (float) ((int) ((y - (float) height) / 16.0F) * 16 + height);
                int jumpTime = 0;
                this.ya = (float) 0;
            }
            if (ya > (float) 0) {
                y = (float) ((int) (y / 16.0F + 1.0F) * 16 - 1);
                onGround = true;
            }
            return false;
        } else {
            x += xa;
            y += ya;
            return true;
        }
    }

    private boolean isBlocking(float _x, float _y, float xa, float ya) {
        int x = (int) (_x / 16.0F);
        int y = (int) (_y / 16.0F);
        if (x == (int) (this.x / 16.0F) && y == (int) (this.y / 16.0F)) return false;

        boolean blocking = world.level.isBlocking(x, y, xa, ya);

        @SuppressWarnings("unused")
        byte block = world.level.getBlock(x, y);

        return blocking;
    }

    @Override
    public void bumpCheck(int xTile, int yTile) {
        if (x + (float) width > (float) (xTile * 16) && x - (float) width < (float) (xTile * 16 + 16) && yTile == (int) ((y - 1.0F) / 16.0F)) {
            facing = -world.mario.facing;
            ya = -10.0F;
        }
    }

}