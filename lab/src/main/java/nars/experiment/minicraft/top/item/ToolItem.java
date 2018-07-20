package nars.experiment.minicraft.top.item;

import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;

import java.util.Random;

public class ToolItem extends Item {
    private final Random random = new Random();

    public static final int MAX_LEVEL = 5;
    public static final String[] LEVEL_NAMES = {
            "Wood", "Rock", "Iron", "Gold", "Gem"
    };

    public static final int[] LEVEL_COLORS = {
            Color.get(-1, 100, 321, 431),
            Color.get(-1, 100, 321, 111),
            Color.get(-1, 100, 321, 555),
            Color.get(-1, 100, 321, 550),
            Color.get(-1, 100, 321, 055),
    };

    public ToolType type;
    public int level;

    public ToolItem(ToolType type, int level) {
        this.type = type;
        this.level = level;
    }

    @Override
    public int getColor() {
        return LEVEL_COLORS[level];
    }

    @Override
    public int getSprite() {
        return type.sprite + 5 * 32;
    }

    @Override
    public void renderIcon(Screen screen, int x, int y) {
        screen.render(x, y, getSprite(), getColor(), 0);
    }

    @Override
    public void renderInventory(Screen screen, int x, int y) {
        screen.render(x, y, getSprite(), getColor(), 0);
        Font.draw(getName(), screen, x + 8, y, Color.get(-1, 555, 555, 555));
    }

    @Override
    public String getName() {
        return LEVEL_NAMES[level] + " " + type.name;
    }

    @Override
    public boolean canAttack() {
        return true;
    }

    @Override
    public int getAttackDamageBonus(Entity e) {
        if (type == ToolType.axe) {
            return (level + 1) * 2 + random.nextInt(4);
        }
        if (type == ToolType.sword) {
            return (level + 1) * 3 + random.nextInt(2 + level * level * 2);
        }
        return 1;
    }

    @Override
    public boolean matches(Item item) {
        if (item instanceof ToolItem) {
            ToolItem other = (ToolItem) item;
            if (other.type != type) return false;
            return other.level == level;
        }
        return false;
    }
}