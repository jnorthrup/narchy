package nars.experiment.mario.sprites;

import nars.experiment.mario.Art;
import nars.experiment.mario.LevelScene;


public class Fireball extends Sprite {
    private static final float GROUND_INERTIA = 0.89f;
    private static final float AIR_INERTIA = 0.89f;

    private float runTime;
    private boolean onGround;

    int height = 24;

    private final LevelScene world;
    public int facing;

    public boolean avoidCliffs;
    public int anim;

    public boolean dead;
    private int deadTime;

    public Fireball(LevelScene world, float x, float y, int facing) {
        sheet = Art.particles;

        this.x = x;
        this.y = y;
        this.world = world;
        xPicO = 4;
        yPicO = 4;

        yPic = 3;
        height = 8;
        this.facing = facing;
        wPic = 8;
        hPic = 8;

        xPic = 4;
        ya = 4.0F;
    }

    @Override
    public void move() {
        if (deadTime > 0) {
            for (int i = 0; i < 8; i++) {
                world.addSprite(new Sparkle((int) ((double) x + Math.random() * 8.0 - 4.0) + 4, (int) ((double) y + Math.random() * 8.0 - 4.0) + 2, (float) Math.random() * 2.0F - 1.0F - (float) facing, (float) Math.random() * 2.0F - 1.0F, 0, 1, 5));
            }
            spriteContext.removeSprite(this);

            return;
        }

        if (facing != 0) anim++;


        if (xa > 2.0F) {
            facing = 1;
        }
        if (xa < -2.0F) {
            facing = -1;
        }

        float sideWaysSpeed = 8f;
        xa = (float) facing * sideWaysSpeed;

        world.checkFireballCollide(this);

        xFlipPic = facing == -1;

        runTime += (Math.abs(xa)) + 5.0F;

        xPic = (anim) % 4;


        if (!move(xa, (float) 0)) {
            die();
        }

        onGround = false;
        move((float) 0, ya);
        if (onGround) ya = -10.0F;

        ya *= 0.95f;
        if (onGround) {
            xa *= GROUND_INERTIA;
        } else {
            xa *= AIR_INERTIA;
        }

        if (!onGround) {
            ya = (float) ((double) ya + 1.5);
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
        int width = 4;
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

    public void die() {
        dead = true;

        xa = (float) (-facing * 2);
        ya = -5.0F;
        deadTime = 100;
    }
}