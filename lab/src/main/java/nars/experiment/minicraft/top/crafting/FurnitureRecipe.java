package nars.experiment.minicraft.top.crafting;

import nars.experiment.minicraft.top.entity.Furniture;
import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.item.FurnitureItem;

public class FurnitureRecipe extends Recipe {
	private final Class<? extends Furniture> clazz;

	public FurnitureRecipe(Class<? extends Furniture> clazz) throws InstantiationException, IllegalAccessException {
		super(new FurnitureItem(clazz.newInstance()));
		this.clazz = clazz;
	}

	@Override
	public void craft(Player player) {
		try {
			player.inventory.add(0, new FurnitureItem(clazz.newInstance()));
		} catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
