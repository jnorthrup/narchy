package nars.experiment.minicraft.top.item;

import nars.experiment.minicraft.top.entity.Furniture;
import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.level.Level;
import nars.experiment.minicraft.top.level.tile.Tile;

public class FurnitureItem extends Item {
    public Furniture furniture;
    public boolean placed;

    public FurnitureItem(Furniture furniture) {
        this.furniture = furniture;
    }

    @Override
    public int getColor() {
        return furniture.col;
    }

    @Override
    public int getSprite() {
        return furniture.sprite + 10 * 32;
    }

    @Override
    public void renderIcon(Screen screen, int x, int y) {
        screen.render(x, y, getSprite(), getColor(), 0);
    }

    @Override
    public void renderInventory(Screen screen, int x, int y) {
        screen.render(x, y, getSprite(), getColor(), 0);
        Font.draw(furniture.name, screen, x + 8, y, Color.get(-1, 555, 555, 555));
    }

    @Override
    public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, int attackDir) {
        if (tile.mayPass(level, xt, yt, furniture)) {
            furniture.x = xt * 16 + 8;
            furniture.y = yt * 16 + 8;
            level.add(furniture);
            placed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean isDepleted() {
        return placed;
    }

    @Override
    public String getName() {
        return furniture.name;
    }
}