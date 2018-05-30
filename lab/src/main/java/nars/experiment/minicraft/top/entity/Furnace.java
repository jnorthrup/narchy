package nars.experiment.minicraft.top.entity;

import nars.experiment.minicraft.top.crafting.Crafting;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.screen.CraftingMenu;

public class Furnace extends Furniture {
	public Furnace() {
		super("Furnace");
		col = Color.get(-1, 000, 222, 333);
		sprite = 3;
		xr = 3;
		yr = 2;
	}

	@Override
    public boolean use(Player player, int attackDir) {
		player.game.setMenu(new CraftingMenu(Crafting.furnaceRecipes, player));
		return true;
	}
}