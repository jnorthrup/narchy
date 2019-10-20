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

import java.io.*;
import java.util.ArrayList;

public class SaveLoad {

    public static void doSave(SideScrollMinicraft game) {

        try {
            if (game.world == null) {
                return;
            }

            var fileOut = new FileOutputStream("/tmp/MiniCraft.sav");
            ObjectOutput out = new ObjectOutputStream(fileOut);

            out.writeObject(game.world);
            out.writeObject(game.entities);

            out.close();
            fileOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean doLoad(SideScrollMinicraft game) {

        ObjectInputStream in = null;
        try {
            var f = new File("/tmp/MiniCraft.sav");
            in = new ObjectInputStream(new FileInputStream(f));
        } catch (InvalidClassException ignored) {
            System.err.println("Save file has the wrong version.");
        } catch (FileNotFoundException ignored) {
            System.err.println("Save file does not exist.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (in == null) {
            return false;
        }

        try {
            game.world = (World) in.readObject();
            game.entities = (ArrayList<Entity>) in.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
