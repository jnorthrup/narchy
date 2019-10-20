package nars.experiment.minicraft.top.level.tile;

import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.level.Level;

public class StairsTile extends Tile {
    private final boolean leadsUp;

    public StairsTile(int id, boolean leadsUp) {
        super(id);
        this.leadsUp = leadsUp;
    }

    @Override
    public void render(Screen screen, Level level, int x, int y) {
        var color = Color.get(level.dirtColor, 000, 333, 444);
        var xt = 0;
        if (leadsUp) xt = 2;
        screen.render(x * 16 + 0, y * 16 + 0, xt + 2 * 32, color, 0);
        screen.render(x * 16 + 8, y * 16 + 0, xt + 1 + 2 * 32, color, 0);
        screen.render(x * 16 + 0, y * 16 + 8, xt + 3 * 32, color, 0);
        screen.render(x * 16 + 8, y * 16 + 8, xt + 1 + 3 * 32, color, 0);
    }
}
