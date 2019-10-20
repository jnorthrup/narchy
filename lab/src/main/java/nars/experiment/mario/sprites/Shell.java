package nars.experiment.mario.sprites;

import nars.experiment.mario.Art;
import nars.experiment.mario.LevelScene;


public class Shell extends Sprite {
    private static final float GROUND_INERTIA = 0.89f;
    private static final float AIR_INERTIA = 0.89f;

    private float runTime;
    private boolean onGround;

    private static final int width = 4;
    int height = 24;

    private final LevelScene world;
    public int facing;

    public boolean avoidCliffs;
    public int anim;

    public boolean dead;
    private int deadTime;
    public boolean carried;


    public Shell(LevelScene world, float x, float y, int type) {
        sheet = Art.enemies;

        this.x = x;
        this.y = y;
        this.world = world;
        xPicO = 8;
        yPicO = 31;

        yPic = type;
        height = 12;
        facing = 0;
        wPic = 16;

        xPic = 4;
        ya = -5.0F;
    }

    @Override
    public boolean fireballCollideCheck(Fireball fireball) {
        if (deadTime != 0) return false;

        float xD = fireball.x - x;
        float yD = fireball.y - y;

        if (xD > -16.0F && xD < 16.0F) {
            if (yD > (float) -height && yD < (float) fireball.height) {
                if (facing != 0) return true;


                xa = (float) (fireball.facing * 2);
                ya = -5.0F;
                if (spriteTemplate != null) spriteTemplate.isDead = true;
                deadTime = 100;
                hPic = -hPic;
                yPicO = -yPicO + 16;
                return true;
            }
        }
        return false;
    }

    @Override
    public void collideCheck() {
        if (carried || dead || deadTime > 0) return;

        float xMarioD = world.mario.x - x;
        float yMarioD = world.mario.y - y;
        float w = 16.0F;
        if (xMarioD > -w && xMarioD < w) {
            if (yMarioD > (float) -height && yMarioD < (float) world.mario.height) {
                if (world.mario.ya > (float) 0 && yMarioD <= (float) 0 && (!world.mario.onGround || !world.mario.wasOnGround)) {
                    world.mario.stomp(this);
                    if (facing != 0) {
                        xa = (float) 0;
                        facing = 0;
                    } else {
                        facing = world.mario.facing;
                    }
                } else {
                    if (facing != 0) {
                        world.mario.getHurt();
                    } else {
                        world.mario.kick(this);
                        facing = world.mario.facing;
                    }
                }
            }
        }
    }

    @Override
    public void move() {
        if (carried) {
            world.checkShellCollide(this);
            return;
        }

        if (deadTime > 0) {
            deadTime--;

            if (deadTime == 0) {
                deadTime = 1;
                for (int i = 0; i < 8; i++) {
                    world.addSprite(new Sparkle((int) ((double) x + Math.random() * 16.0 - 8.0) + 4, (int) ((double) y - Math.random() * 8.0) + 4, (float) (Math.random() * 2.0 - 1.0), (float) Math.random() * -1.0F, 0, 1, 5));
                }
                spriteContext.removeSprite(this);
            }

            x += xa;
            y += ya;
            ya = (float) ((double) ya * 0.95);
            ya += 1.0F;

            return;
        }

        if (facing != 0) anim++;


        if (xa > 2.0F) {
            facing = 1;
        }
        if (xa < -2.0F) {
            facing = -1;
        }

        float sideWaysSpeed = 11f;
        xa = (float) facing * sideWaysSpeed;

        if (facing != 0) {
            world.checkShellCollide(this);
        }

        xFlipPic = facing == -1;

        runTime += (Math.abs(xa)) + 5.0F;

        xPic = (anim / 2) % 4 + 3;


        if (!move(xa, (float) 0)) {


            facing = -facing;
        }
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

        if (blocking && ya == (float) 0 && xa != (float) 0) {
            world.bump(x, y, true);
        }

        return blocking;
    }

    @Override
    public void bumpCheck(int xTile, int yTile) {
        if (x + (float) width > (float) (xTile * 16) && x - (float) width < (float) (xTile * 16 + 16) && yTile == (int) ((y - 1.0F) / 16.0F)) {
            facing = -world.mario.facing;
            ya = -10.0F;
        }
    }

    public void die() {
        dead = true;

        carried = false;

        xa = (float) (-facing * 2);
        ya = -5.0F;
        deadTime = 100;
    }

    @Override
    public boolean shellCollideCheck(Shell shell) {
        if (deadTime != 0) return false;

        float xD = shell.x - x;
        float yD = shell.y - y;

        if (xD > -16.0F && xD < 16.0F) {
            if (yD > (float) -height && yD < (float) shell.height) {


                if (world.mario.carried == shell || world.mario.carried == this) {
                    world.mario.carried = null;
                }

                die();
                shell.die();
                return true;
            }
        }
        return false;
    }


    @Override
    public void release(Mario mario) {
        carried = false;
        facing = mario.facing;
        x = x + (float) facing * 8;
    }
}