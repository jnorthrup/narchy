/*
 * Copyright 2012 Jonathan Leahey
 *
 * This file is part of Minicraft
 *
 * Minicraft is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Minicraft is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Minicraft. If not, see http:
 */

package nars.experiment.minicraft.side;

public class Tool extends Item {
    private static final long serialVersionUID = 1L;

    public enum ToolType {
        Shovel, Pick, Axe
    }

    public enum ToolPower {
        Wood, Stone, Metal, Diamond
    }

    final int totalUses;
    int uses;
    public final ToolType toolType;
    public final ToolPower toolPower;

    public Tool(String ref, int size, int id, String name, int[][] template, int templateCount,
                ToolType toolType, ToolPower toolPower) {
        super(ref, size, id, name, template, templateCount);
        switch (toolPower) {
            case Wood:
                totalUses = 32;
                break;
            case Stone:
                totalUses = 64;
                break;
            case Metal:
                totalUses = 128;
                break;
            default:
                totalUses = 256;
                break;
        }
        this.toolPower = toolPower;
        this.toolType = toolType;
    }

    @Override
    public Tool clone() {
        var t = (Tool) super.clone();
        t.uses = 0;
        return t;
    }
}
