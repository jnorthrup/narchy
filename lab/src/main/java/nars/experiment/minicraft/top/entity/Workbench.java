package nars.experiment.minicraft.top.entity;

import nars.experiment.minicraft.top.crafting.Crafting;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.screen.CraftingMenu;

public class Workbench extends Furniture {
	public Workbench() {
		super("Workbench");
		col = Color.get(-1, 100, 321, 431);
		sprite = 4;
		xr = 3;
		yr = 2;
	}

	@Override
    public boolean use(Player player, int attackDir) {
		player.game.setMenu(new CraftingMenu(Crafting.workbenchRecipes, player));
		return true;
	}
}