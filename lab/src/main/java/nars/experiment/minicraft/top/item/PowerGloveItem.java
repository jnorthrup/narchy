package nars.experiment.minicraft.top.item;

import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.entity.Furniture;
import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;

public class PowerGloveItem extends Item {
	@Override
    public int getColor() {
		return Color.get(-1, 100, 320, 430);
	}

	@Override
    public int getSprite() {
		return 7 + 4 * 32;
	}

	@Override
    public void renderIcon(Screen screen, int x, int y) {
		screen.render(x, y, getSprite(), getColor(), 0);
	}

	@Override
    public void renderInventory(Screen screen, int x, int y) {
		screen.render(x, y, getSprite(), getColor(), 0);
		Font.draw(getName(), screen, x + 8, y, Color.get(-1, 555, 555, 555));
	}

	@Override
    public String getName() {
		return "Pow glove";
	}

	@Override
    public boolean interact(Player player, Entity entity, int attackDir) {
		if (entity instanceof Furniture) {
			Furniture f = (Furniture) entity;
			f.take(player);
			return true;
		}
		return false;
	}
}