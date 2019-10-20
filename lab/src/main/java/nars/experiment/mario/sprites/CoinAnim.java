package nars.experiment.mario.sprites;

import nars.experiment.mario.Art;


public class CoinAnim extends Sprite {
    private int life = 10;

    public CoinAnim(int xTile, int yTile) {
        sheet = Art.level;
        wPic = hPic = 16;

        x = (float) (xTile * 16);
        y = (float) (yTile * 16 - 16);
        xa = (float) 0;
        ya = -6f;
        xPic = 0;
        yPic = 2;
    }

    @Override
    public void move() {
        if (life-- < 0) {
            Sprite.spriteContext.removeSprite(this);
            for (int xx = 0; xx < 2; xx++)
                for (int yy = 0; yy < 2; yy++)
                    Sprite.spriteContext.addSprite(new Sparkle((int) x + xx * 8 + (int) (Math.random() * 8.0), (int) y + yy * 8 + (int) (Math.random() * 8.0), (float) 0, (float) 0, 0, 2, 5));
        }

        xPic = life & 3;

        x += xa;
        y += ya;
        ya += 1.0F;
    }
}