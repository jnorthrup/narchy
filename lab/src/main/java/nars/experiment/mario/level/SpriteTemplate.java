package nars.experiment.mario.level;

import nars.experiment.mario.LevelScene;
import nars.experiment.mario.sprites.Enemy;
import nars.experiment.mario.sprites.FlowerEnemy;
import nars.experiment.mario.sprites.Sprite;

public class SpriteTemplate {
    public int lastVisibleTick = -1;
    public Sprite sprite;
    public boolean isDead;
    private final boolean winged;

    private final int type;

    public SpriteTemplate(int type, boolean winged) {
        this.type = type;
        this.winged = winged;
    }

    public void spawn(LevelScene world, int x, int y, int dir) {
        if (isDead) return;

        if (type == Enemy.ENEMY_FLOWER) {
            sprite = new FlowerEnemy(world, x * 16 + 15, y * 16 + 24);
        } else {
            sprite = new Enemy(world, x * 16 + 8, y * 16 + 15, dir, type, winged);
        }
        sprite.spriteTemplate = this;
        world.addSprite(sprite);
    }
}