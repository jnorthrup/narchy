/*
 * Cvar.java
 * Copyright (C) 2003
 *
 * $Id: Cvar.java,v 1.10 2007-05-14 22:29:30 cawe Exp $
 */
/*
 Copyright (C) 1997-2001 Id Software, Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */

package jake2.qcommon;

import jake2.Defines;
import jake2.Globals;
import jake2.game.Cmd;
import jake2.game.Info;
import jake2.game.cvar_t;
import jake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cvar implements console variables. The original code is located in cvar.c
 */
public class Cvar extends Globals {

    /**
     * Set command, sets variables.
     */

    static final xcommand_t Set_f = new xcommand_t() {
        @Override
        public void execute() {
            int flags;

            int c = Cmd.Argc();
            if (c != 3 && c != 4) {
                Com.Printf("usage: setAt <variable> <value> [u / s]\n");
                return;
            }

            if (c == 4) {
                switch (Cmd.Argv(3)) {
                    case "u":
                        flags = Defines.CVAR_USERINFO;
                        break;
                    case "s":
                        flags = Defines.CVAR_SERVERINFO;
                        break;
                    default:
                        Com.Printf("flags can only be 'u' or 's'\n");
                        return;
                }
                FullSet(Cmd.Argv(1), Cmd.Argv(2), flags);
            } else
                Set(Cmd.Argv(1), Cmd.Argv(2));

        }

    };
    /**
     * List command, lists all available commands.
     */
    static final xcommand_t List_f = new xcommand_t() {
        @Override
        public void execute() {
            cvar_t var;

            int i = 0;
            for (var = Globals.cvar_vars; var != null; var = var.next, i++) {
                if ((var.flags & Defines.CVAR_ARCHIVE) != 0)
                    Com.Printf("*");
                else
                    Com.Printf(" ");
                if ((var.flags & Defines.CVAR_USERINFO) != 0)
                    Com.Printf("U");
                else
                    Com.Printf(" ");
                if ((var.flags & Defines.CVAR_SERVERINFO) != 0)
                    Com.Printf("S");
                else
                    Com.Printf(" ");
                if ((var.flags & Defines.CVAR_NOSET) != 0)
                    Com.Printf("-");
                else if ((var.flags & Defines.CVAR_LATCH) != 0)
                    Com.Printf("L");
                else
                    Com.Printf(" ");
                Com.Printf(' ' + var.name + " \"" + var.string + "\"\n");
            }
            Com.Printf(i + " cvars\n");
        }
    };

    /**
     * @param var_name
     * @param var_value
     * @param flags
     * @return
     */
    public static cvar_t Get(String var_name, String var_value, int flags) {

        if ((flags & (Defines.CVAR_USERINFO | Defines.CVAR_SERVERINFO)) != 0) {
            if (!Cvar.InfoValidate(var_name)) {
                Com.Printf("invalid info cvar name\n");
                return null;
            }
        }

        cvar_t var = FindVar(var_name);
        if (var != null) {
            var.flags |= flags;
            return var;
        }

        if (var_value == null)
            return null;

        if ((flags & (Defines.CVAR_USERINFO | Defines.CVAR_SERVERINFO)) != 0) {
            if (!Cvar.InfoValidate(var_value)) {
                Com.Printf("invalid info cvar value\n");
                return null;
            }
        }
        var = new cvar_t();
        var.name = var_name;
        var.string = var_value;
        var.modified = true;
        var.value = Lib.atof(var.string);

        var.next = Globals.cvar_vars;
        Globals.cvar_vars = var;

        var.flags = flags;

        return var;
    }

    static void Init() {
        Cmd.AddCommand("setAt", Cvar.Set_f);
        Cmd.AddCommand("cvarlist", Cvar.List_f);
    }

    public static String VariableString(String var_name) {
        cvar_t var = Cvar.FindVar(var_name);
        return (var == null) ? "" : var.string;
    }

    static cvar_t FindVar(String var_name) {
        cvar_t var;

        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if (var_name.equals(var.name))
                return var;
        }

        return null;
    }

    /**
     * Creates a variable if not found and sets their value, the parsed float value and their flags.
     */
    public static cvar_t FullSet(String var_name, String value, int flags) {

        cvar_t var = FindVar(var_name);
        if (null == var) {
            return Get(var_name, value, flags);
        }

        var.modified = true;

        if ((var.flags & Defines.CVAR_USERINFO) != 0)
            Globals.userinfo_modified = true;

        var.string = value;
        var.value = Lib.atof(var.string);
        var.flags = flags;

        return var;
    }

    /**
     * Sets the value of the variable without forcing.
     */
    public static cvar_t Set(String var_name, String value) {
        return Cvar.Set2(var_name, value, false);
    }

    /**
     * Sets the value of the variable with forcing.
     */
    public static cvar_t ForceSet(String var_name, String value) {
        return Set2(var_name, value, true);
    }

    /**
     * Gereric set function, sets the value of the variable, with forcing its even possible to
     * override the variables write protection.
     */
    static cvar_t Set2(String var_name, String value, boolean force) {

        cvar_t var = FindVar(var_name);
        if (var == null) {

            return Get(var_name, value, 0);
        }

        if ((var.flags & (Defines.CVAR_USERINFO | Defines.CVAR_SERVERINFO)) != 0) {
            if (!Cvar.InfoValidate(value)) {
                Com.Printf("invalid info cvar value\n");
                return var;
            }
        }

        if (!force) {
            if ((var.flags & Defines.CVAR_NOSET) != 0) {
                Com.Printf(var_name + " is write protected.\n");
                return var;
            }

            if ((var.flags & Defines.CVAR_LATCH) != 0) {
                if (var.latched_string != null) {
                    if (value.equals(var.latched_string))
                        return var;
                    var.latched_string = null;
                } else {
                    if (value.equals(var.string))
                        return var;
                }

                if (Globals.server_state != 0) {
                    Com.Printf(var_name + " will be changed for next game.\n");
                    var.latched_string = value;
                } else {
                    var.string = value;
                    var.value = Lib.atof(var.string);
                    if (var.name.equals("game")) {
                        FS.SetGamedir(var.string);
                        FS.ExecAutoexec();
                    }
                }
                return var;
            }
        } else {
            if (var.latched_string != null) {
                var.latched_string = null;
            }
        }

        if (value.equals(var.string))
            return var;

        var.modified = true;

        if ((var.flags & Defines.CVAR_USERINFO) != 0)
            Globals.userinfo_modified = true;

        var.string = value;
        try {
            var.value = Float.parseFloat(var.string);
        } catch (Exception e) {
            var.value = 0.0f;
        }

        return var;
    }

    /**
     * Sets a float value of a variable.
     * <p>
     * The overloading is very important, there was a problem with
     * networt "rate" string --> 10000 became "10000.0" and that wasn't right.
     */
    public static void SetValue(String var_name, int value) {
        Set(var_name, String.valueOf(value));
    }

    public static void SetValue(String var_name, float value) {
        if (value == (int) value) {
            Set(var_name, String.valueOf((int) value));
        } else {
            Set(var_name, String.valueOf(value));
        }
    }

    /**
     * Returns the float value of a variable.
     */
    public static float VariableValue(String var_name) {
        cvar_t var = FindVar(var_name);
        if (var == null)
            return 0;

        return Lib.atof(var.string);
    }

    /**
     * Handles variable inspection and changing from the console.
     */
    public static boolean Command() {


        cvar_t v = FindVar(Cmd.Argv(0));
        if (v == null)
            return false;


        if (Cmd.Argc() == 1) {
            Com.Printf('"' + v.name + "\" is \"" + v.string + "\"\n");
            return true;
        }

        Set(v.name, Cmd.Argv(1));
        return true;
    }

    public static String BitInfo(int bit) {
        cvar_t var;

        String info = "";

        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if ((var.flags & bit) != 0)
                info = Info.Info_SetValueForKey(info, var.name, var.string);
        }
        return info;
    }

    /**
     * Returns an info string containing all the CVAR_SERVERINFO cvars.
     */
    public static String Serverinfo() {
        return Cvar.BitInfo(Defines.CVAR_SERVERINFO);
    }


    /**
     * Any variables with latched values will be updated.
     */
    public static void GetLatchedVars() {
        cvar_t var;

        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if (var.latched_string == null || var.latched_string.length() == 0)
                continue;
            var.string = var.latched_string;
            var.latched_string = null;
            var.value = Lib.atof(var.string);
            if (var.name.equals("game")) {
                FS.SetGamedir(var.string);
                FS.ExecAutoexec();
            }
        }
    }

    /**
     * Returns an info string containing all the CVAR_USERINFO cvars.
     */
    public static String Userinfo() {
        return Cvar.BitInfo(Defines.CVAR_USERINFO);
    }

    /**
     * Appends lines containing \"set vaqriable value\" for all variables
     * with the archive flag set true.
     */

    public static void WriteVariables(String path) {
        cvar_t var;
        String buffer;

        RandomAccessFile f = Lib.fopen(path, "rw");
        if (f == null)
            return;

        try {
            f.seek(f.length());
        } catch (IOException e1) {
            Lib.fclose(f);
            return;
        }
        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if ((var.flags & Defines.CVAR_ARCHIVE) != 0) {
                buffer = "setAt " + var.name + " \"" + var.string + "\"\n";
                try {
                    f.writeBytes(buffer);
                } catch (IOException e) {
                }
            }
        }
        Lib.fclose(f);
    }

    /**
     * Variable typing auto completition.
     */
    public static List CompleteVariable(String partial) {
        return new CopyOnWriteArrayList( Stream.iterate(Globals.cvar_vars, Objects::nonNull, cvar -> cvar.next).filter(cvar -> cvar.name.startsWith(partial)).map(cvar -> cvar.name).collect(Collectors.toList()));
    }

    /**
     * Some characters are invalid for info strings.
     */
    static boolean InfoValidate(String s) {
        if (s.contains("\\"))
            return false;
        if (s.contains("\""))
            return false;
        return !s.contains(";");
    }
}