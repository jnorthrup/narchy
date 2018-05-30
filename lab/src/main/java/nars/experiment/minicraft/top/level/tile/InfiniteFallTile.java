package nars.experiment.minicraft.top.level.tile;

import nars.experiment.minicraft.top.entity.AirWizard;
import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.level.Level;

public class InfiniteFallTile extends Tile {
	public InfiniteFallTile(int id) {
		super(id);
	}

	@Override
	public boolean mayPass(Level level, int x, int y, Entity e) {
		return e instanceof AirWizard;
	}
}
