package nars.experiment.minicraft.top.entity;

import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.item.ResourceItem;
import nars.experiment.minicraft.top.item.resource.Resource;

public class Slime extends Mob {
    private int xa;
    private int ya;
    private int jumpTime;
    private final int lvl;

    public Slime(int lvl) {
        this.lvl = lvl;
        x = random.nextInt(64 * 16);
        y = random.nextInt(64 * 16);
        health = maxHealth = lvl * lvl * 5;
    }

    @Override
    public void tick() {
        super.tick();

        var speed = 1;
        if (!move(xa * speed, ya * speed) || random.nextInt(40) == 0) {
            if (jumpTime <= -10) {
                xa = (random.nextInt(3) - 1);
                ya = (random.nextInt(3) - 1);

                if (level.player != null) {
                    var xd = level.player.x - x;
                    var yd = level.player.y - y;
                    if (xd * xd + yd * yd < 50 * 50) {
                        if (xd < 0) xa = -1;
                        if (xd > 0) xa = +1;
                        if (yd < 0) ya = -1;
                        if (yd > 0) ya = +1;
                    }

                }

                if (xa != 0 || ya != 0) jumpTime = 10;
            }
        }

        jumpTime--;
        if (jumpTime == 0) {
            xa = ya = 0;
        }
    }

    @Override
    protected void die() {
        super.die();

        var count = random.nextInt(2) + 1;
        for (var i = 0; i < count; i++) {
            level.add(new ItemEntity(new ResourceItem(Resource.slime), x + random.nextInt(11) - 5, y + random.nextInt(11) - 5));
        }

        if (level.player != null) {
            level.player.score += 25 * lvl;
        }

    }

    @Override
    public void render(Screen screen) {
        var xt = 0;

        var xo = x - 8;
        var yo = y - 11;

        if (jumpTime > 0) {
            xt += 2;
            yo -= 4;
        }

        var col = Color.get(-1, 10, 252, 555);
        if (lvl == 2) col = Color.get(-1, 100, 522, 555);
        if (lvl == 3) col = Color.get(-1, 111, 444, 555);
        if (lvl == 4) col = Color.get(-1, 000, 111, 224);

        if (hurtTime > 0) {
            col = Color.get(-1, 555, 555, 555);
        }

        var yt = 18;
        screen.render(xo + 0, yo + 0, xt + yt * 32, col, 0);
        screen.render(xo + 8, yo + 0, xt + 1 + yt * 32, col, 0);
        screen.render(xo + 0, yo + 8, xt + (yt + 1) * 32, col, 0);
        screen.render(xo + 8, yo + 8, xt + 1 + (yt + 1) * 32, col, 0);
    }

    @Override
    protected void touchedBy(Entity entity) {
        if (entity instanceof Player) {
            entity.hurt(this, lvl, dir);
        }
    }
}