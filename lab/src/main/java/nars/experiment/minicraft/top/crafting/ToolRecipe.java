package nars.experiment.minicraft.top.crafting;

import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.item.ToolItem;
import nars.experiment.minicraft.top.item.ToolType;

public class ToolRecipe extends Recipe {
	private final ToolType type;
	private final int level;

	public ToolRecipe(ToolType type, int level) {
		super(new ToolItem(type, level));
		this.type = type;
		this.level = level;
	}

	@Override
	public void craft(Player player) {
		player.inventory.add(0, new ToolItem(type, level));
	}
}
