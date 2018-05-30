package nars.experiment.minicraft.top.level.tile;

import nars.experiment.minicraft.top.entity.AirWizard;
import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.entity.Mob;
import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.entity.particle.SmashParticle;
import nars.experiment.minicraft.top.entity.particle.TextParticle;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.item.Item;
import nars.experiment.minicraft.top.item.ToolItem;
import nars.experiment.minicraft.top.item.ToolType;
import nars.experiment.minicraft.top.level.Level;

public class CloudCactusTile extends Tile {
	public CloudCactusTile(int id) {
		super(id);
	}

	@Override
	public void render(Screen screen, Level level, int x, int y) {
		int color = Color.get(444, 111, 333, 555);
		screen.render(x * 16 + 0, y * 16 + 0, 17 + 1 * 32, color, 0);
		screen.render(x * 16 + 8, y * 16 + 0, 18 + 1 * 32, color, 0);
		screen.render(x * 16 + 0, y * 16 + 8, 17 + 2 * 32, color, 0);
		screen.render(x * 16 + 8, y * 16 + 8, 18 + 2 * 32, color, 0);
	}

	@Override
	public boolean mayPass(Level level, int x, int y, Entity e) {
        return e instanceof AirWizard;
    }

	@Override
	public void hurt(Level level, int x, int y, Mob source, int dmg, int attackDir) {
		hurt(level, x, y, 0);
	}

	@Override
	public boolean interact(Level level, int xt, int yt, Player player, Item item, int attackDir) {
		if (item instanceof ToolItem) {
			ToolItem tool = (ToolItem) item;
			if (tool.type == ToolType.pickaxe) {
				if (player.payStamina(6 - tool.level)) {
					hurt(level, xt, yt, 1);
					return true;
				}
			}
		}
		return false;
	}

	public static void hurt(Level level, int x, int y, int dmg) {
		int damage = level.getData(x, y) + 1;
		level.add(new SmashParticle(x * 16 + 8, y * 16 + 8));
		level.add(new TextParticle(String.valueOf(dmg), x * 16 + 8, y * 16 + 8, Color.get(-1, 500, 500, 500)));
		if (dmg > 0) {
			if (damage >= 10) {
				level.setTile(x, y, Tile.cloud, 0);
			} else {
				level.setData(x, y, damage);
			}
		}
	}

	@Override
	public void bumpedInto(Level level, int x, int y, Entity entity) {
		if (entity instanceof AirWizard) return;
		entity.hurt(this, x, y, 3);
	}
}