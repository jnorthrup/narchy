package nars.experiment.minicraft.top.item;

public class ToolType {
	public static final ToolType shovel = new ToolType("Shvl", 0);
	public static final ToolType hoe = new ToolType("Hoe", 1);
	public static final ToolType sword = new ToolType("Swrd", 2);
	public static final ToolType pickaxe = new ToolType("Pick", 3);
	public static final ToolType axe = new ToolType("Axe", 4);

	public final String name;
	public final int sprite;

	private ToolType(String name, int sprite) {
		this.name = name;
		this.sprite = sprite;
	}
}
