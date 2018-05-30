package nars.experiment.minicraft.top.entity;

import nars.experiment.minicraft.top.crafting.Crafting;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.screen.CraftingMenu;

public class Oven extends Furniture {
	public Oven() {
		super("Oven");
		col = Color.get(-1, 000, 332, 442);
		sprite = 2;
		xr = 3;
		yr = 2;
	}

	@Override
    public boolean use(Player player, int attackDir) {
		player.game.setMenu(new CraftingMenu(Crafting.ovenRecipes, player));
		return true;
	}
}