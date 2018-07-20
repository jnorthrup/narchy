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

public class Item extends Entity implements Cloneable {
    private static final long serialVersionUID = 1L;

    public final int item_id;
    public final String name;
    public final Template template;

    public Item(String ref, int size, int id, String name, int[][] template, int templateCount) {
        super(ref, true, 0, 0, size, size);
        this.template = new Template(template, templateCount);
        this.item_id = id;
        this.name = name;
    }

    @Override
    public Item clone() {
        try {
            return (Item) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

}
