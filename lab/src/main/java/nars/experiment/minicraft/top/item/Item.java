package nars.experiment.minicraft.top.item;

import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.entity.ItemEntity;
import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.level.Level;
import nars.experiment.minicraft.top.level.tile.Tile;
import nars.experiment.minicraft.top.screen.ListItem;

public class Item implements ListItem {
	public int getColor() {
		return 0;
	}

	public int getSprite() {
		return 0;
	}

	public void onTake(ItemEntity itemEntity) {
	}

	@Override
    public void renderInventory(Screen screen, int x, int y) {
	}

	public boolean interact(Player player, Entity entity, int attackDir) {
		return false;
	}

	public void renderIcon(Screen screen, int x, int y) {
	}

	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, int attackDir) {
		return false;
	}
	
	public boolean isDepleted() {
		return false;
	}

	public boolean canAttack() {
		return false;
	}

	public int getAttackDamageBonus(Entity e) {
		return 0;
	}

	public String getName() {
		return "";
	}

	public boolean matches(Item item) {
		return item.getClass() == getClass();
	}
}