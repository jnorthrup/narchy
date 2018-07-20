package nars.experiment.minicraft.side;

/**
 * Created by me on 9/19/16.
 */
public class ToolDefinition extends ItemDefinition {
    final Tool.ToolType type;
    final Tool.ToolPower power;

    public ToolDefinition(String typeString, String powerString, int id, String name, String sprite, int[][] t, int y) {
        super(id, name, sprite, y, t);
        type = Tool.ToolType.valueOf(typeString);
        power = Tool.ToolPower.valueOf(powerString);
    }

    public Tool makeTool(int size) {
        return new Tool(spriteRef, size, item_id, name, recipe, yield, type, power);
    }
}
