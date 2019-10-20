package nars.experiment.mario.sprites;


import nars.experiment.mario.Art;
import nars.experiment.mario.LevelScene;
import nars.experiment.mario.Scene;
import nars.experiment.mario.level.Level;


public class Mario extends Sprite {
    public static boolean large;
    public static boolean fire;
    /** TODO non-static */
    @Deprecated public static int coins;
    public static int lives = 3;
    public static String levelString = "none";
    private static final int kill_reward_coins = 4;
    private static final int medical_bill = 2;
    private static final int burial_cost = 4;

    public static void resetStatic() {
        large = false;
        fire = false;
        coins = 0;
        lives = 3;
        levelString = "none";
    }

    public static final int KEY_LEFT = 0;
    public static final int KEY_RIGHT = 1;
    public static final int KEY_DOWN = 2;
    public static final int KEY_UP = 3;
    public static final int KEY_JUMP = 4;
    public static final int KEY_SPEED = 5;

    private static final float GROUND_INERTIA = 0.89f;
    private static final float AIR_INERTIA = 0.89f;

    public boolean[] keys;
    private float runTime;
    boolean wasOnGround;
    boolean onGround;
    private boolean mayJump;
    private boolean ducking;
    private boolean sliding;
    public int jumpTime;
    private float xJumpSpeed;
    private float yJumpSpeed;
    private boolean canShoot;

    int width = 4;
    int height = 24;

    private final LevelScene world;
    public int facing;
    private int powerUpTime;

    public int xDeathPos;
    public int yDeathPos;

    public int deathTime;
    public int winTime;
    private int invulnerableTime;

    public Sprite carried;

    public Mario(LevelScene world) {
        this.world = world;
        keys = Scene.keys;
        x = 32.0F;
        y = (float) 0;

        facing = 1;
        setLarge(Mario.large, Mario.fire);
    }

    private boolean lastLarge;
    private boolean lastFire;
    private boolean newLarge;
    private boolean newFire;

    private void blink(boolean on) {
        Mario.large = on ? newLarge : lastLarge;
        Mario.fire = on ? newFire : lastFire;

        if (large) {
            sheet = Art.mario;
            if (fire)
                sheet = Art.fireMario;

            xPicO = 16;
            yPicO = 31;
            wPic = hPic = 32;
        } else {
            sheet = Art.smallMario;

            xPicO = 8;
            yPicO = 15;
            wPic = hPic = 16;
        }

        calcPic();
    }

    void setLarge(boolean large, boolean fire) {
        if (fire) large = true;
        if (!large) fire = false;

        lastLarge = Mario.large;
        lastFire = Mario.fire;

        Mario.large = large;
        Mario.fire = fire;

        newLarge = Mario.large;
        newFire = Mario.fire;

        blink(true);
    }

    @Override
    public void move() {
        if (winTime > 0) {
            winTime++;

            xa = (float) 0;
            ya = (float) 0;
            return;
        }

        if (deathTime > 0) {
            //deathTime--;
            if (deathTime < 11) {
                xa = (float) 0;
                ya = (float) 0;
            } else if (deathTime == 11) {
                ya = -15.0F;
            } else {
                ya += 2.0F;
            }
            x += xa;
            y += ya;
            return;
        }

        if (powerUpTime != 0) {
            if (powerUpTime > 0) {
                powerUpTime--;
                blink(((powerUpTime / 3) & 1) == 0);
            } else {
                powerUpTime++;
                blink(((-powerUpTime / 3) & 1) == 0);
            }

            if (powerUpTime == 0) world.paused = false;

            calcPic();
            return;
        }

        if (invulnerableTime > 0) invulnerableTime--;
        visible = ((invulnerableTime / 2) & 1) == 0;

        wasOnGround = onGround;
        float sideWaysSpeed = keys[KEY_SPEED] ? 1.2f : 0.6f;


        if (onGround) {
            ducking = keys[KEY_DOWN] && large && !keys[KEY_JUMP];
        }

        if (xa > 2.0F) {
            facing = 1;
        }
        if (xa < -2.0F) {
            facing = -1;
        }

        if (keys[KEY_JUMP] && !keys[KEY_DOWN] || (jumpTime < 0 && !onGround && !sliding)) {
            if (jumpTime < 0) {
                xa = xJumpSpeed;
                ya = (float) -jumpTime * yJumpSpeed;
                jumpTime++;
            } else if (onGround && mayJump) {

                xJumpSpeed = (float) 0;
                yJumpSpeed = -1.9f;
                jumpTime = 7;
                ya = (float) jumpTime * yJumpSpeed;
                onGround = false;
                sliding = false;
            } else if (sliding && mayJump) {

                xJumpSpeed = (float) -facing * 6.0f;
                yJumpSpeed = -2.0f;
                jumpTime = -6;
                xa = xJumpSpeed;
                ya = (float) -jumpTime * yJumpSpeed;
                onGround = false;
                sliding = false;
                facing = -facing;
            } else if (jumpTime > 0) {
                xa += xJumpSpeed;
                ya = (float) jumpTime * yJumpSpeed;
                jumpTime--;
            }
        } else {
            jumpTime = 0;
        }

        if (keys[KEY_LEFT] && !ducking && !keys[KEY_RIGHT]) {
            if (facing == 1) sliding = false;
            xa -= sideWaysSpeed;
            if (jumpTime >= 0) facing = -1;
        }

        if (keys[KEY_RIGHT] && !ducking && !keys[KEY_LEFT]) {
            if (facing == -1) sliding = false;
            xa += sideWaysSpeed;
            if (jumpTime >= 0) facing = 1;
        }

        if ((!keys[KEY_LEFT] && !keys[KEY_RIGHT]) || ducking || ya < (float) 0 || onGround) {
            sliding = false;
        }

        if (keys[KEY_SPEED] && canShoot && Mario.fire && world.fireballsOnScreen < 2) {

            world.addSprite(new Fireball(world, x + (float) (facing * 6), y - 20.0F, facing));
        }

        canShoot = !keys[KEY_SPEED];

        mayJump = (onGround || sliding) && !keys[KEY_JUMP];

        xFlipPic = facing == -1;

        runTime += (Math.abs(xa)) + 5.0F;
        if (Math.abs(xa) < 0.5f) {
            runTime = (float) 0;
            xa = (float) 0;
        }

        calcPic();

        if (sliding) {
            for (int i = 0; i < 1; i++) {
                world.addSprite(new Sparkle((int) ((double) x + Math.random() * 4.0 - 2.0) + facing * 8, (int) ((double) y + Math.random() * 4.0) - 24, (float) (Math.random() * 2.0 - 1.0), (float) Math.random() * 1.0F, 0, 1, 5));
            }
            ya *= 0.5f;
        }

        onGround = false;
        move(xa, (float) 0);
        move((float) 0, ya);

        if (y > (float) (world.level.height * 16 + 16)) {
            die();
        }

        if (x < (float) 0) {
            x = (float) 0;
            xa = (float) 0;
        }

        if (x > (float) (world.level.xExit * 16)) {
            win();
        }

        if (x > (float) (world.level.width * 16)) {
            x = (float) (world.level.width * 16);
            xa = (float) 0;
        }

        ya *= 0.85f;
        if (onGround) {
            xa *= GROUND_INERTIA;
        } else {
            xa *= AIR_INERTIA;
        }

        if (!onGround) {
            ya += 3.0F;
        }

        if (carried != null) {
            carried.x = x + (float) (facing * 8);
            carried.y = y - 2.0F;
            if (!keys[KEY_SPEED]) {
                carried.release(this);
                carried = null;
            }
        }
    }

    private void calcPic() {
        int runFrame = 0;

        if (large) {
            runFrame = ((int) (runTime / 20.0F)) % 4;
            if (runFrame == 3) runFrame = 1;
            if (carried == null && Math.abs(xa) > 10.0F) runFrame += 3;
            if (carried != null) runFrame += 10;
            if (!onGround) {
                if (carried != null) runFrame = 12;
                else if (Math.abs(xa) > 10.0F) runFrame = 7;
                else runFrame = 6;
            }
        } else {
            runFrame = ((int) (runTime / 20.0F)) % 2;
            if (carried == null && Math.abs(xa) > 10.0F) runFrame += 2;
            if (carried != null) runFrame += 8;
            if (!onGround) {
                if (carried != null) runFrame = 9;
                else if (Math.abs(xa) > 10.0F) runFrame = 5;
                else runFrame = 4;
            }
        }

        if (onGround && ((facing == -1 && xa > (float) 0) || (facing == 1 && xa < (float) 0))) {
            if (xa > 1.0F || xa < -1.0F) runFrame = large ? 9 : 7;

            if (xa > 3.0F || xa < -3.0F) {
                for (int i = 0; i < 3; i++) {
                    world.addSprite(new Sparkle((int) ((double) x + Math.random() * 8.0 - 4.0), (int) ((double) y + Math.random() * 4.0), (float) (Math.random() * 2.0 - 1.0), (float) Math.random() * -1.0F, 0, 1, 5));
                }
            }
        }

        if (large) {
            if (ducking) runFrame = 14;
            height = ducking ? 12 : 24;
        } else {
            height = 12;
        }

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
            sliding = true;
            if (isBlocking(x + xa + (float) width, y + ya - (float) height, xa, ya)) collide = true;
            else sliding = false;
            if (isBlocking(x + xa + (float) width, y + ya - (float) (height / 2), xa, ya)) collide = true;
            else sliding = false;
            if (isBlocking(x + xa + (float) width, y + ya, xa, ya)) collide = true;
            else sliding = false;
        }
        if (xa < (float) 0) {
            sliding = true;
            if (isBlocking(x + xa - (float) width, y + ya - (float) height, xa, ya)) collide = true;
            else sliding = false;
            if (isBlocking(x + xa - (float) width, y + ya - (float) (height / 2), xa, ya)) collide = true;
            else sliding = false;
            if (isBlocking(x + xa - (float) width, y + ya, xa, ya)) collide = true;
            else sliding = false;
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
                jumpTime = 0;
                this.ya = (float) 0;
            }
            if (ya > (float) 0) {
                y = (float) ((int) ((y - 1.0F) / 16.0F + 1.0F) * 16 - 1);
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

        byte block = world.level.getBlock(x, y);

        if (((int) (Level.TILE_BEHAVIORS[(int) block & 0xff]) & Level.BIT_PICKUPABLE) > 0) {
            Mario.getCoin();

            world.level.setBlock(x, y, (byte) 0);
            for (int xx = 0; xx < 2; xx++)
                for (int yy = 0; yy < 2; yy++)
                    world.addSprite(new Sparkle(x * 16 + xx * 8 + (int) (Math.random() * 8.0), y * 16 + yy * 8 + (int) (Math.random() * 8.0), (float) 0, (float) 0, 0, 2, 5));
        }

        if (blocking && ya < (float) 0) {
            world.bump(x, y, large);
        }

        return blocking;
    }

    public void stomp(Enemy enemy) {
        coins += kill_reward_coins;

        if (deathTime > 0 || world.paused) return;


        float targetY = enemy.y - (float) (enemy.height / 2);
        move((float) 0, targetY - y);


        xJumpSpeed = (float) 0;
        yJumpSpeed = -1.9f;
        jumpTime = 8;
        ya = (float) jumpTime * yJumpSpeed;
        onGround = false;
        sliding = false;
        invulnerableTime = 1;
    }

    public void stomp(Shell shell) {
        if (deathTime > 0 || world.paused) return;

        if (keys[KEY_SPEED] && shell.facing == 0) {
            carried = shell;
            shell.carried = true;
        } else {
            float targetY = shell.y - (float) (shell.height / 2);
            move((float) 0, targetY - y);


            xJumpSpeed = (float) 0;
            yJumpSpeed = -1.9f;
            jumpTime = 8;
            ya = (float) jumpTime * yJumpSpeed;
            onGround = false;
            sliding = false;
            invulnerableTime = 1;
        }
    }

    public void getHurt() {

        coins -= medical_bill;


        if (deathTime > 0 || world.paused) return;
        if (invulnerableTime > 0) return;

        if (large) {
            world.paused = true;
            powerUpTime = -3 * 6;

            if (fire) {
                world.mario.setLarge(true, false);
            } else {
                world.mario.setLarge(false, false);
            }
            invulnerableTime = 32;
        } else {
            die();
        }
    }

    private void win() {
        int retirement_benefit = 8;
        coins += retirement_benefit;

        xDeathPos = (int) x;
        yDeathPos = (int) y;
        world.paused = true;
        winTime = 1;
        Art.stopMusic();

    }

    public void die() {

        xDeathPos = (int) x;
        yDeathPos = (int) y;
        world.paused = true;
        deathTime = 100;
        Art.stopMusic();

    }


    public void getFlower() {
        if (deathTime > 0 || world.paused) return;

        if (!fire) {
            world.paused = true;
            powerUpTime = 3 * 6;

            world.mario.setLarge(true, true);
        } else {
            Mario.getCoin();

        }
    }

    public void getMushroom() {
        if (deathTime > 0 || world.paused) return;

        if (!large) {
            world.paused = true;
            powerUpTime = 3 * 6;

            world.mario.setLarge(true, false);
        } else {
            Mario.getCoin();

        }
    }

    public void kick(Shell shell) {
        if (deathTime > 0 || world.paused) return;

        if (keys[KEY_SPEED]) {
            carried = shell;
            shell.carried = true;
        } else {

            invulnerableTime = 1;
        }
    }

    public void stomp(BulletBill bill) {
        coins += kill_reward_coins;

        if (deathTime > 0 || world.paused) return;

        float targetY = bill.y - (float) (bill.height / 2);
        move((float) 0, targetY - y);


        xJumpSpeed = (float) 0;
        yJumpSpeed = -1.9f;
        jumpTime = 8;
        ya = (float) jumpTime * yJumpSpeed;
        onGround = false;
        sliding = false;
        invulnerableTime = 1;
    }

    public byte getKeyMask() {
        int mask = 0;
        for (int i = 0; i < 7; i++) {
            if (keys[i]) {
                int i1 = (1 << i);
                mask = mask | i1;
            }
        }
        return (byte) mask;
    }

    public void setKeys(byte mask) {
        for (int i = 0; i < 7; i++) {
            keys[i] = ((int) mask & (1 << i)) > 0;
        }
    }

    public static void get1Up() {
        coins += medical_bill;


        lives++;
        if (lives == 99) {
            lives = 99;
        }
    }

    public static void getCoin() {
        coins++;


    }
}