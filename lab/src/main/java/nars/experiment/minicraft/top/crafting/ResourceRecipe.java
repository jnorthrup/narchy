package nars.experiment.minicraft.top.crafting;

import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.item.ResourceItem;
import nars.experiment.minicraft.top.item.resource.Resource;

public class ResourceRecipe extends Recipe {
	private final Resource resource;

	public ResourceRecipe(Resource resource) {
		super(new ResourceItem(resource, 1));
		this.resource = resource;
	}

	@Override
	public void craft(Player player) {
		player.inventory.add(0, new ResourceItem(resource, 1));
	}
}
