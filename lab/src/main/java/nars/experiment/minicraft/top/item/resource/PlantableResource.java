package nars.experiment.minicraft.top.item.resource;

import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.level.Level;
import nars.experiment.minicraft.top.level.tile.Tile;

import java.util.Arrays;
import java.util.List;

public class PlantableResource extends Resource {
	private final List<Tile> sourceTiles;
	private final Tile targetTile;

	public PlantableResource(String name, int sprite, int color, Tile targetTile, Tile... sourceTiles1) {
		this(name, sprite, color, targetTile, Arrays.asList(sourceTiles1));
	}

	public PlantableResource(String name, int sprite, int color, Tile targetTile, List<Tile> sourceTiles) {
		super(name, sprite, color);
		this.sourceTiles = sourceTiles;
		this.targetTile = targetTile;
	}

	@Override
	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, int attackDir) {
		if (sourceTiles.contains(tile)) {
			level.setTile(xt, yt, targetTile, 0);
			return true;
		}
		return false;
	}
}
