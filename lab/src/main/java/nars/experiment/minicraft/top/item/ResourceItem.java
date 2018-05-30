package nars.experiment.minicraft.top.item;

import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.item.resource.Resource;
import nars.experiment.minicraft.top.level.Level;
import nars.experiment.minicraft.top.level.tile.Tile;

public class ResourceItem extends Item {
	public Resource resource;
	public int count = 1;

	public ResourceItem(Resource resource) {
		this.resource = resource;
	}

	public ResourceItem(Resource resource, int count) {
		this.resource = resource;
		this.count = count;
	}

	@Override
	public int getColor() {
		return resource.color;
	}

	@Override
	public int getSprite() {
		return resource.sprite;
	}

	@Override
	public void renderIcon(Screen screen, int x, int y) {
		screen.render(x, y, resource.sprite, resource.color, 0);
	}

	@Override
	public void renderInventory(Screen screen, int x, int y) {
		screen.render(x, y, resource.sprite, resource.color, 0);
		Font.draw(resource.name, screen, x + 32, y, Color.get(-1, 555, 555, 555));
		int cc = count;
		if (cc > 999) cc = 999;
		Font.draw(String.valueOf(cc), screen, x + 8, y, Color.get(-1, 444, 444, 444));
	}

	@Override
	public String getName() {
		return resource.name;
	}

    @Override
	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, int attackDir) {
		if (resource.interactOn(tile, level, xt, yt, player, attackDir)) {
			count--;
			return true;
		}
		return false;
	}

	@Override
	public boolean isDepleted() {
		return count <= 0;
	}

}