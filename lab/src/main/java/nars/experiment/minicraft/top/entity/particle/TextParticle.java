package nars.experiment.minicraft.top.entity.particle;

import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;

public class TextParticle extends Entity {
    private final String msg;
    private final int col;
    private int time;
    public double xa;
    public double ya;
    public double za;
    public double xx;
    public double yy;
    public double zz;

    public TextParticle(String msg, int x, int y, int col) {
        this.msg = msg;
        this.x = x;
        this.y = y;
        this.col = col;
        xx = x;
        yy = y;
        zz = 2;
        xa = random.nextGaussian() * 0.3;
        ya = random.nextGaussian() * 0.2;
        za = random.nextFloat() * 0.7 + 2;
    }

    @Override
    public void tick() {
        time++;
        if (time > 60) {
            remove();
        }
        xx += xa;
        yy += ya;
        zz += za;
        if (zz < 0) {
            zz = 0;
            za *= -0.5;
            xa *= 0.6;
            ya *= 0.6;
        }
        za -= 0.15;
        x = (int) xx;
        y = (int) yy;
    }

    @Override
    public void render(Screen screen) {

        Font.draw(msg, screen, x - msg.length() * 4 + 1, y - (int) (zz) + 1, Color.get(-1, 0, 0, 0));
        Font.draw(msg, screen, x - msg.length() * 4, y - (int) (zz), col);
    }

}
