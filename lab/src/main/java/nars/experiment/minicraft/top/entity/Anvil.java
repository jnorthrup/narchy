package nars.experiment.minicraft.top.entity;

import nars.experiment.minicraft.top.crafting.Crafting;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.screen.CraftingMenu;

public class Anvil extends Furniture {
	public Anvil() {
		super("Anvil");
		col = Color.get(-1, 000, 111, 222);
		sprite = 0;
		xr = 3;
		yr = 2;
	}

	@Override
    public boolean use(Player player, int attackDir) {
		player.game.setMenu(new CraftingMenu(Crafting.anvilRecipes, player));
		return true;
	}
}