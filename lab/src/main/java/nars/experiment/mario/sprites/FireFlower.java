package nars.experiment.mario.sprites;

import nars.experiment.mario.Art;
import nars.experiment.mario.LevelScene;


public class FireFlower extends Sprite {
    @SuppressWarnings("unused")
    private static final int width = 4;
    int height = 24;

    private final LevelScene world;
    public int facing;

    public boolean avoidCliffs;
    private int life;

    public FireFlower(LevelScene world, int x, int y) {
        sheet = Art.items;

        this.x = x;
        this.y = y;
        this.world = world;
        xPicO = 8;
        yPicO = 15;

        xPic = 1;
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
        float w = 16;
        if (xMarioD > -w && xMarioD < w) {
            if (yMarioD > -height && yMarioD < world.mario.height) {
                world.mario.getFlower();
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
    }
}