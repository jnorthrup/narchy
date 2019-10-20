package nars.experiment.mario.sprites;

import nars.experiment.mario.Art;
import nars.experiment.mario.LevelScene;

import java.awt.*;


public class Enemy extends Sprite {
    public static final int ENEMY_RED_KOOPA = 0;
    public static final int ENEMY_GREEN_KOOPA = 1;
    public static final int ENEMY_GOOMBA = 2;
    public static final int ENEMY_SPIKY = 3;
    public static final int ENEMY_FLOWER = 4;

    private static final float GROUND_INERTIA = 0.89f;
    private static final float AIR_INERTIA = 0.89f;

    private float runTime;
    private boolean onGround;
    @SuppressWarnings("unused")
    private float xJumpSpeed;
    @SuppressWarnings("unused")
    private float yJumpSpeed;

    int width = 4;
    int height = 24;

    private final LevelScene world;
    public int facing;
    public int deadTime;
    public boolean flyDeath;

    public boolean avoidCliffs = true;
    private final int type;

    public boolean winged = true;
    private int wingTime;

    public boolean noFireballDeath;

    public Enemy(LevelScene world, int x, int y, int dir, int type, boolean winged) {
        this.type = type;
        sheet = Art.enemies;
        this.winged = winged;

        this.x = (float) x;
        this.y = (float) y;
        this.world = world;
        xPicO = 8;
        yPicO = 31;

        avoidCliffs = type == Enemy.ENEMY_RED_KOOPA;

        noFireballDeath = type == Enemy.ENEMY_SPIKY;

        yPic = type;
        if (yPic > 1) height = 12;
        facing = dir;
        if (facing == 0) facing = 1;
        this.wPic = 16;
    }

    @Override
    public void collideCheck() {
        if (deadTime != 0) {
            return;
        }

        float xMarioD = world.mario.x - x;
        float yMarioD = world.mario.y - y;
        @SuppressWarnings("unused")
        float w = 16.0F;
        if (xMarioD > (float) (-width * 2 - 4) && xMarioD < (float) (width * 2 + 4)) {
            if (yMarioD > (float) -height && yMarioD < (float) world.mario.height) {
                if (type != Enemy.ENEMY_SPIKY && world.mario.ya > (float) 0 && yMarioD <= (float) 0 && (!world.mario.onGround || !world.mario.wasOnGround)) {
                    world.mario.stomp(this);
                    if (winged) {
                        winged = false;
                        ya = (float) 0;
                    } else {
                        this.yPicO = 31 - (32 - 8);
                        hPic = 8;
                        if (spriteTemplate != null) spriteTemplate.isDead = true;
                        deadTime = 10;
                        winged = false;

                        switch (type) {
                            case Enemy.ENEMY_RED_KOOPA:
                                spriteContext.addSprite(new Shell(world, x, y, 0));
                                break;
                            case Enemy.ENEMY_GREEN_KOOPA:
                                spriteContext.addSprite(new Shell(world, x, y, 1));
                                break;
                        }
                    }
                } else {
                    world.mario.getHurt();
                }
            }
        }
    }

    @Override
    public void move() {
        wingTime++;
        if (deadTime > 0) {
            deadTime--;

            if (deadTime == 0) {
                deadTime = 1;
                for (int i = 0; i < 8; i++) {
                    world.addSprite(new Sparkle((int) ((double) x + Math.random() * 16.0 - 8.0) + 4, (int) ((double) y - Math.random() * 8.0) + 4, (float) (Math.random() * 2.0 - 1.0), (float) Math.random() * -1.0F, 0, 1, 5));
                }
                spriteContext.removeSprite(this);
            }

            if (flyDeath) {
                x += xa;
                y += ya;
                ya = (float) ((double) ya * 0.95);
                ya += 1.0F;
            }
            return;
        }


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

        int runFrame = ((int) (runTime / 20.0F)) % 2;

        if (!onGround) {
            runFrame = 1;
        }


        if (!move(xa, (float) 0)) facing = -facing;
        onGround = false;
        move((float) 0, ya);

        ya *= winged ? 0.95f : 0.85f;
        if (onGround) {
            xa *= GROUND_INERTIA;
        } else {
            xa *= AIR_INERTIA;
        }

        if (!onGround) {
            if (winged) {
                ya += 0.6f;
            } else {
                ya += 2.0F;
            }
        } else if (winged) {
            ya = -10.0F;
        }

        if (winged) runFrame = wingTime / 4 % 2;

        xPic = runFrame;
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
    public boolean shellCollideCheck(Shell shell) {
        if (deadTime != 0) return false;

        float xD = shell.x - x;
        float yD = shell.y - y;

        if (xD > -16.0F && xD < 16.0F) {
            if (yD > (float) -height && yD < (float) shell.height) {


                xa = (float) (shell.facing * 2);
                ya = -5.0F;
                flyDeath = true;
                if (spriteTemplate != null) spriteTemplate.isDead = true;
                deadTime = 100;
                winged = false;
                hPic = -hPic;
                yPicO = -yPicO + 16;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean fireballCollideCheck(Fireball fireball) {
        if (deadTime != 0) return false;

        float xD = fireball.x - x;
        float yD = fireball.y - y;

        if (xD > -16.0F && xD < 16.0F) {
            if (yD > (float) -height && yD < (float) fireball.height) {
                if (noFireballDeath) return true;


                xa = (float) (fireball.facing * 2);
                ya = -5.0F;
                flyDeath = true;
                if (spriteTemplate != null) spriteTemplate.isDead = true;
                deadTime = 100;
                winged = false;
                hPic = -hPic;
                yPicO = -yPicO + 16;
                return true;
            }
        }
        return false;
    }

    @Override
    public void bumpCheck(int xTile, int yTile) {
        if (deadTime != 0) return;

        if (x + (float) width > (float) (xTile * 16) && x - (float) width < (float) (xTile * 16 + 16) && yTile == (int) ((y - 1.0F) / 16.0F)) {


            xa = (float) (-world.mario.facing * 2);
            ya = -5.0F;
            flyDeath = true;
            if (spriteTemplate != null) spriteTemplate.isDead = true;
            deadTime = 100;
            winged = false;
            hPic = -hPic;
            yPicO = -yPicO + 16;
        }
    }

    @Override
    public void render(Graphics og, float alpha) {
        if (winged) {
            int xPixel = (int) (xOld + (x - xOld) * alpha) - xPicO;
            int yPixel = (int) (yOld + (y - yOld) * alpha) - yPicO;

            if (type == Enemy.ENEMY_GREEN_KOOPA || type == Enemy.ENEMY_RED_KOOPA) {
            } else {
                xFlipPic = !xFlipPic;
                og.drawImage(sheet[wingTime / 4 % 2][4], xPixel + (xFlipPic ? wPic : 0) + (xFlipPic ? 10 : -10), yPixel + (yFlipPic ? hPic : 0) - 8, xFlipPic ? -wPic : wPic, yFlipPic ? -hPic : hPic, null);
                xFlipPic = !xFlipPic;
            }
        }

        super.render(og, alpha);

        if (winged) {
            int xPixel = (int) (xOld + (x - xOld) * alpha) - xPicO;
            int yPixel = (int) (yOld + (y - yOld) * alpha) - yPicO;

            if (type == Enemy.ENEMY_GREEN_KOOPA || type == Enemy.ENEMY_RED_KOOPA) {
                og.drawImage(sheet[wingTime / 4 % 2][4], xPixel + (xFlipPic ? wPic : 0) + (xFlipPic ? 10 : -10), yPixel + (yFlipPic ? hPic : 0) - 10, xFlipPic ? -wPic : wPic, yFlipPic ? -hPic : hPic, null);
            } else {
                og.drawImage(sheet[wingTime / 4 % 2][4], xPixel + (xFlipPic ? wPic : 0) + (xFlipPic ? 10 : -10), yPixel + (yFlipPic ? hPic : 0) - 8, xFlipPic ? -wPic : wPic, yFlipPic ? -hPic : hPic, null);
            }
        }
    }
}