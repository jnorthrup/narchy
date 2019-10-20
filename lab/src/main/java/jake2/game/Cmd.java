/*
 * Cmd.java
 * Copyright (C) 2003
 * 
 * $Id: Cmd.java,v 1.18 2006-01-21 21:53:32 salomo Exp $
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
package jake2.game;

import jake2.Defines;
import jake2.Globals;
import jake2.game.monsters.M_Player;
import jake2.qcommon.*;
import jake2.server.SV_GAME;
import jake2.util.Lib;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cmd
 */
public final class Cmd {
    static final xcommand_t List_f = new xcommand_t() {
        @Override
        public void execute() {
            cmd_function_t cmd = Cmd.cmd_functions;
            int i = 0;

            while (cmd != null) {
                Com.Printf(cmd.name + '\n');
                i++;
                cmd = cmd.next;
            }
            Com.Printf(i + " commands\n");
        }
    };

    static final xcommand_t Exec_f = new xcommand_t() {
        @Override
        public void execute() {
            if (Cmd.Argc() != 2) {
                Com.Printf("exec <filename> : execute a script file\n");
                return;
            }

            byte[] f = FS.LoadFile(Cmd.Argv(1));
            if (f == null) {
                Com.Printf("couldn't exec " + Cmd.Argv(1) + '\n');
                return;
            }
            Com.Printf("execing " + Cmd.Argv(1) + '\n');

            Cbuf.InsertText(new String(f));

            FS.FreeFile(f);
        }
    };

    static final xcommand_t Echo_f = new xcommand_t() {
        @Override
        public void execute() {
            for (int i = 1; i < Cmd.Argc(); i++) {
                Com.Printf(Cmd.Argv(i) + ' ');
            }
            Com.Printf("'\n");
        }
    };

    static final xcommand_t Alias_f = new xcommand_t() {
        @Override
        public void execute() {
            cmdalias_t a = null;
            if (Cmd.Argc() == 1) {
                Com.Printf("Current alias commands:\n");
                for (a = Globals.cmd_alias; a != null; a = a.next) {
                    Com.Printf(a.name + " : " + a.value);
                }
                return;
            }

            String s = Cmd.Argv(1);
            if (s.length() > Defines.MAX_ALIAS_NAME) {
                Com.Printf("Alias name is too long\n");
                return;
            }

            
            for (a = Globals.cmd_alias; a != null; a = a.next) {
                if (s.equalsIgnoreCase(a.name)) {
                    a.value = null;
                    break;
                }
            }

            if (a == null) {
                a = new cmdalias_t();
                a.next = Globals.cmd_alias;
                Globals.cmd_alias = a;
            }
            a.name = s;


            String cmd = "";
            int c = Cmd.Argc();
            for (int i = 2; i < c; i++) {
                cmd += Cmd.Argv(i);
                if (i != (c - 1))
                    cmd += ' ';
            }
            cmd += '\n';

            a.value = cmd;
        }
    };

    public static final xcommand_t Wait_f = new xcommand_t() {
        @Override
        public void execute() {
            Globals.cmd_wait = true;
        }
    };

    public static cmd_function_t cmd_functions;

    public static int cmd_argc;

    public static final String[] cmd_argv = new String[Defines.MAX_STRING_TOKENS];

    public static String cmd_args;

    public static final int ALIAS_LOOP_COUNT = 16;

    /**
     * Register our commands.
     */
    public static void Init() {

        Cmd.AddCommand("exec", Exec_f);
        Cmd.AddCommand("echo", Echo_f);
        Cmd.AddCommand("cmdlist", List_f);
        Cmd.AddCommand("alias", Alias_f);
        Cmd.AddCommand("wait", Wait_f);
    }

    private static final char[] expanded = new char[Defines.MAX_STRING_CHARS];

    private static final char[] temporary = new char[Defines.MAX_STRING_CHARS];

    public static final Comparator PlayerSort = (o1, o2) -> {
        int anum = (Integer) o1;
        int bnum = (Integer) o2;

        int anum1 = GameBase.game.clients[anum].ps.stats[Defines.STAT_FRAGS];
        int bnum1 = GameBase.game.clients[bnum].ps.stats[Defines.STAT_FRAGS];

        return Integer.compare(anum1, bnum1);
    };

    /** 
     * Cmd_MacroExpandString.
     */
    public static char[] MacroExpandString(char[] text, int len) {

        char[] scan = text;

        if (len >= Defines.MAX_STRING_CHARS) {
            Com.Printf("Line exceeded " + Defines.MAX_STRING_CHARS
                    + " chars, discarded.\n");
            return null;
        }

        int count = 0;

        boolean inquote = false;
        for (int i = 0; i < len; i++) {
            if (scan[i] == '"')
                inquote = !inquote;

            if (inquote)
                continue; 

            if (scan[i] != '$')
                continue;


            Com.ParseHelp ph = new Com.ParseHelp(text, i + 1);
            String token = Com.Parse(ph);

            if (ph.data == null)
                continue;

            token = Cvar.VariableString(token);

            int j = token.length();

            len += j;

            if (len >= Defines.MAX_STRING_CHARS) {
                Com.Printf("Expanded line exceeded " + Defines.MAX_STRING_CHARS
                        + " chars, discarded.\n");
                return null;
            }

            System.arraycopy(scan, 0, temporary, 0, i);
            System.arraycopy(token.toCharArray(), 0, temporary, i, token.length());
            System.arraycopy(ph.data, ph.index, temporary, i + j, len - ph.index - j);

            System.arraycopy(temporary, 0, expanded, 0, 0);
            scan = expanded;
            i--;
            if (++count == 100) {
                Com.Printf("Macro expansion loop, discarded.\n");
                return null;
            }
        }

        if (inquote) {
            Com.Printf("Line has unmatched quote, discarded.\n");
            return null;
        }

        return scan;
    }

    /**
     * Cmd_TokenizeString
     * 
     * Parses the given string into command line tokens. $Cvars will be expanded
     * unless they are in a quoted token.
     */
    public static void TokenizeString(char[] text, boolean macroExpand) {

        cmd_argc = 0;
        cmd_args = "";

        int len = Lib.strlen(text);

        
        if (macroExpand)
            text = MacroExpandString(text, len);

        if (text == null)
            return;

        len = Lib.strlen(text);

        Com.ParseHelp ph = new Com.ParseHelp(text);

        while (true) {


            char c = ph.skipwhitestoeol();

            if (c == '\n') { 
                c = ph.nextchar();
                break;
            }

            if (c == 0)
                return;

            
            if (cmd_argc == 1) {
                cmd_args = new String(text, ph.index, len - ph.index);
                cmd_args.trim();
            }

            String com_token = Com.Parse(ph);

            if (ph.data == null)
                return;

            if (cmd_argc < Defines.MAX_STRING_TOKENS) {
                cmd_argv[cmd_argc] = com_token;
                cmd_argc++;
            }
        }
    }

    public static void AddCommand(String cmd_name, xcommand_t function) {


        if ((Cvar.VariableString(cmd_name)).length() > 0) {
            Com.Printf("Cmd_AddCommand: " + cmd_name
                    + " already defined as a var\n");
            return;
        }


        cmd_function_t cmd;
        for (cmd = cmd_functions; cmd != null; cmd = cmd.next) {
            if (cmd_name.equals(cmd.name)) {
                Com
                        .Printf("Cmd_AddCommand: " + cmd_name
                                + " already defined\n");
                return;
            }
        }

        cmd = new cmd_function_t();
        cmd.name = cmd_name;

        cmd.function = function;
        cmd.next = cmd_functions;
        cmd_functions = cmd;
    }

    /**
     * Cmd_RemoveCommand 
     */
    public static void RemoveCommand(String cmd_name) {
        cmd_function_t cmd;

        cmd_function_t back = cmd = cmd_functions;

        while (true) {

            if (cmd == null) {
                Com.Printf("Cmd_RemoveCommand: " + cmd_name + " not added\n");
                return;
            }
            if (0 == Lib.strcmp(cmd_name, cmd.name)) {
                if (cmd == cmd_functions)
                    cmd_functions = cmd.next;
                else
                    back.next = cmd.next;
                return;
            }
            back = cmd;
            cmd = cmd.next;
        }
    }

    /** 
     * Cmd_Exists 
     */
    public static boolean Exists(String cmd_name) {

        for (cmd_function_t cmd = cmd_functions; cmd != null; cmd = cmd.next) {
            if (cmd.name.equals(cmd_name))
                return true;
        }

        return false;
    }

    public static int Argc() {
        return cmd_argc;
    }

    public static String Argv(int i) {
        if (i < 0 || i >= cmd_argc)
            return "";
        return cmd_argv[i];
    }

    public static String Args() {
        return cmd_args;
    }

    /**
     * Cmd_ExecuteString
     * 
     * A complete command line has been parsed, so try to execute it 
     * FIXME: lookupnoadd the token to speed search? 
     */
    public static void ExecuteString(String text) {

        TokenizeString(text.toCharArray(), true);

        
        if (Argc() == 0)
            return; 

        
        for (cmd_function_t cmd = cmd_functions; cmd != null; cmd = cmd.next) {
            if (cmd_argv[0].equalsIgnoreCase(cmd.name)) {
                if (null == cmd.function) { 
                    Cmd.ExecuteString("cmd " + text);
                } else {
                    cmd.function.execute();
                }
                return;
            }
        }

        
        for (cmdalias_t a = Globals.cmd_alias; a != null; a = a.next) {

            if (cmd_argv[0].equalsIgnoreCase(a.name)) {

                if (++Globals.alias_count == ALIAS_LOOP_COUNT) {
                    Com.Printf("ALIAS_LOOP_COUNT\n");
                    return;
                }
                Cbuf.InsertText(a.value);
                return;
            }
        }

        
        if (Cvar.Command())
            return;

        
        Cmd.ForwardToServer();
    }

    /**
     * Cmd_Give_f
     * 
     * Give items to a client.
     */
    public static void Give_f(edict_t ent) {

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            SV_GAME.PF_cprintfhigh(ent,
            	"You must run the server with '+setAt cheats 1' to enable this command.\n");
            return;
        }

        String name = Cmd.Args();

        boolean give_all = 0 == Lib.Q_stricmp(name, "all");

        if (give_all || 0 == Lib.Q_stricmp(Cmd.Argv(1), "health")) {
            if (Cmd.Argc() == 3)
                ent.health = Lib.atoi(Cmd.Argv(2));
            else
                ent.health = ent.max_health;
            if (!give_all)
                return;
        }

        int i;
        gitem_t it;
        if (give_all || 0 == Lib.Q_stricmp(name, "weapons")) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                it = GameItemList.itemlist[i];
                if (null == it.pickup)
                    continue;
                if (0 == (it.flags & Defines.IT_WEAPON))
                    continue;
                ent.client.pers.inventory[i] += 1;
            }
            if (!give_all)
                return;
        }

        if (give_all || 0 == Lib.Q_stricmp(name, "ammo")) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                it = GameItemList.itemlist[i];
                if (null == it.pickup)
                    continue;
                if (0 == (it.flags & Defines.IT_AMMO))
                    continue;
                GameItems.Add_Ammo(ent, it, 1000);
            }
            if (!give_all)
                return;
        }

        if (give_all || Lib.Q_stricmp(name, "armor") == 0) {

            it = GameItems.FindItem("Jacket Armor");
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0;

            it = GameItems.FindItem("Combat Armor");
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0;

            it = GameItems.FindItem("Body Armor");
            gitem_armor_t info = it.info;
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = info.max_count;

            if (!give_all)
                return;
        }

        edict_t it_ent;
        if (give_all || Lib.Q_stricmp(name, "Power Shield") == 0) {
            it = GameItems.FindItem("Power Shield");
            it_ent = GameUtil.G_Spawn();
            it_ent.classname = it.classname;
            GameItems.SpawnItem(it_ent, it);
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null);
            if (it_ent.inuse)
                GameUtil.G_FreeEdict(it_ent);

            if (!give_all)
                return;
        }

        if (give_all) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                it = GameItemList.itemlist[i];
                if (it.pickup != null)
                    continue;
                if ((it.flags & (Defines.IT_ARMOR | Defines.IT_WEAPON | Defines.IT_AMMO)) != 0)
                    continue;
                ent.client.pers.inventory[i] = 1;
            }
            return;
        }

        it = GameItems.FindItem(name);
        if (it == null) {
            name = Cmd.Argv(1);
            it = GameItems.FindItem(name);
            if (it == null) {
                SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH, "unknown item\n");
                return;
            }
        }

        if (it.pickup == null) {
            SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH, "non-pickup item\n");
            return;
        }

        int index = GameItems.ITEM_INDEX(it);

        if ((it.flags & Defines.IT_AMMO) != 0) {
            if (Cmd.Argc() == 3)
                ent.client.pers.inventory[index] = Lib.atoi(Cmd.Argv(2));
            else
                ent.client.pers.inventory[index] += it.quantity;
        } else {
            it_ent = GameUtil.G_Spawn();
            it_ent.classname = it.classname;
            GameItems.SpawnItem(it_ent, it);
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null);
            if (it_ent.inuse)
                GameUtil.G_FreeEdict(it_ent);
        }
    }

    /** 
     * Cmd_God_f
     * 
     * Sets client to godmode
     * 
     * argv(0) god
     */
    public static void God_f(edict_t ent) {

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            SV_GAME.PF_cprintfhigh(ent,
            		"You must run the server with '+setAt cheats 1' to enable this command.\n");
            return;
        }

        ent.flags ^= Defines.FL_GODMODE;
        String msg;
        if (0 == (ent.flags & Defines.FL_GODMODE))
            msg = "godmode OFF\n";
        else
            msg = "godmode ON\n";

        SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH, msg);
    }

    /** 
     * Cmd_Notarget_f
     * 
     * Sets client to notarget
     * 
     * argv(0) notarget.
     */
    public static void Notarget_f(edict_t ent) {

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            SV_GAME.PF_cprintfhigh(ent, 
            	"You must run the server with '+setAt cheats 1' to enable this command.\n");
            return;
        }

        ent.flags ^= Defines.FL_NOTARGET;
        String msg;
        if (0 == (ent.flags & Defines.FL_NOTARGET))
            msg = "notarget OFF\n";
        else
            msg = "notarget ON\n";

        SV_GAME.PF_cprintfhigh(ent, msg);
    }

    /**
     * Cmd_Noclip_f
     * 
     * argv(0) noclip.
     */
    public static void Noclip_f(edict_t ent) {

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            SV_GAME.PF_cprintfhigh(ent, 
            	"You must run the server with '+setAt cheats 1' to enable this command.\n");
            return;
        }

        String msg;
        if (ent.movetype == Defines.MOVETYPE_NOCLIP) {
            ent.movetype = Defines.MOVETYPE_WALK;
            msg = "noclip OFF\n";
        } else {
            ent.movetype = Defines.MOVETYPE_NOCLIP;
            msg = "noclip ON\n";
        }

        SV_GAME.PF_cprintfhigh(ent, msg);
    }

    /**
     * Cmd_Use_f
     * 
     * Use an inventory item.
     */
    public static void Use_f(edict_t ent) {

        String s = Cmd.Args();

        gitem_t it = GameItems.FindItem(s);
        Com.dprintln("using:" + s);
        if (it == null) {
            SV_GAME.PF_cprintfhigh(ent, "unknown item: " + s + '\n');
            return;
        }
        if (it.use == null) {
            SV_GAME.PF_cprintfhigh(ent, "Item is not usable.\n");
            return;
        }
        int index = GameItems.ITEM_INDEX(it);
        if (0 == ent.client.pers.inventory[index]) {
            SV_GAME.PF_cprintfhigh(ent, "Out of item: " + s + '\n');
            return;
        }

        it.use.use(ent, it);
    }

    /**
     * Cmd_Drop_f
     * 
     * Drop an inventory item.
     */
    public static void Drop_f(edict_t ent) {

        String s = Cmd.Args();
        gitem_t it = GameItems.FindItem(s);
        if (it == null) {
            SV_GAME.PF_cprintfhigh(ent, "unknown item: " + s + '\n');
            return;
        }
        if (it.drop == null) {
            SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH,
                    "Item is not dropable.\n");
            return;
        }
        int index = GameItems.ITEM_INDEX(it);
        if (0 == ent.client.pers.inventory[index]) {
            SV_GAME.PF_cprintfhigh(ent, "Out of item: " + s + '\n');
            return;
        }

        it.drop.drop(ent, it);
    }

    /**
     * Cmd_Inven_f.
     */
    public static void Inven_f(edict_t ent) {

        gclient_t cl = ent.client;

        cl.showscores = false;
        cl.showhelp = false;

        if (cl.showinventory) {
            cl.showinventory = false;
            return;
        }

        cl.showinventory = true;

        game_import_t.WriteByte(Defines.svc_inventory);
        for (int i = 0; i < Defines.MAX_ITEMS; i++) {
            game_import_t.WriteShort(cl.pers.inventory[i]);
        }
        game_import_t.unicast(ent, true);
    }

    /**
     * Cmd_InvUse_f.
     */
    public static void InvUse_f(edict_t ent) {

        Cmd.ValidateSelectedItem(ent);

        if (ent.client.pers.selected_item == -1) {
            SV_GAME.PF_cprintfhigh(ent, "No item to use.\n");
            return;
        }

        gitem_t it = GameItemList.itemlist[ent.client.pers.selected_item];
        if (it.use == null) {
            SV_GAME.PF_cprintfhigh(ent, "Item is not usable.\n");
            return;
        }
        it.use.use(ent, it);
    }

    /**
     * Cmd_WeapPrev_f.
     */
    public static void WeapPrev_f(edict_t ent) {

        gclient_t cl = ent.client;

        if (cl.pers.weapon == null)
            return;

        int selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon);

        
        for (int i = 1; i <= Defines.MAX_ITEMS; i++) {
            int index = (selected_weapon + i) % Defines.MAX_ITEMS;
            if (0 == cl.pers.inventory[index])
                continue;

            gitem_t it = GameItemList.itemlist[index];
            if (it.use == null)
                continue;

            if (0 == (it.flags & Defines.IT_WEAPON))
                continue;
            it.use.use(ent, it);
            if (cl.pers.weapon == it)
                return; 
        }
    }

    /**
     * Cmd_WeapNext_f.
     */
    public static void WeapNext_f(edict_t ent) {

        gclient_t cl = ent.client;

        if (null == cl.pers.weapon)
            return;

        int selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon);

        
        for (int i = 1; i <= Defines.MAX_ITEMS; i++) {
            int index = (selected_weapon + Defines.MAX_ITEMS - i)
                    % Defines.MAX_ITEMS;

            if (index == 0)
                index++;
            if (0 == cl.pers.inventory[index])
                continue;
            gitem_t it = GameItemList.itemlist[index];
            if (null == it.use)
                continue;
            if (0 == (it.flags & Defines.IT_WEAPON))
                continue;
            it.use.use(ent, it);
            if (cl.pers.weapon == it)
                return; 
        }
    }

    /** 
     * Cmd_WeapLast_f.
     */
    public static void WeapLast_f(edict_t ent) {

        gclient_t cl = ent.client;

        if (null == cl.pers.weapon || null == cl.pers.lastweapon)
            return;

        int index = GameItems.ITEM_INDEX(cl.pers.lastweapon);
        if (0 == cl.pers.inventory[index])
            return;
        gitem_t it = GameItemList.itemlist[index];
        if (null == it.use)
            return;
        if (0 == (it.flags & Defines.IT_WEAPON))
            return;
        it.use.use(ent, it);
    }

    /**
     * Cmd_InvDrop_f 
     */
    public static void InvDrop_f(edict_t ent) {

        Cmd.ValidateSelectedItem(ent);

        if (ent.client.pers.selected_item == -1) {
            SV_GAME.PF_cprintfhigh(ent, "No item to drop.\n");
            return;
        }

        gitem_t it = GameItemList.itemlist[ent.client.pers.selected_item];
        if (it.drop == null) {
            SV_GAME.PF_cprintfhigh(ent, "Item is not dropable.\n");
            return;
        }
        it.drop.drop(ent, it);
    }

    /** 
     * Cmd_Score_f
     * 
     * Display the scoreboard.
     * 
     */
    public static void Score_f(edict_t ent) {
        ent.client.showinventory = false;
        ent.client.showhelp = false;

        if (0 == GameBase.deathmatch.value && 0 == GameBase.coop.value)
            return;

        if (ent.client.showscores) {
            ent.client.showscores = false;
            return;
        }

        ent.client.showscores = true;
        PlayerHud.DeathmatchScoreboard(ent);
    }

    /**
     * Cmd_Help_f
     * 
     * Display the current help message. 
     *
     */
    public static void Help_f(edict_t ent) {
        
        if (GameBase.deathmatch.value != 0) {
            Score_f(ent);
            return;
        }

        ent.client.showinventory = false;
        ent.client.showscores = false;

        if (ent.client.showhelp
                && (ent.client.pers.game_helpchanged == GameBase.game.helpchanged)) {
            ent.client.showhelp = false;
            return;
        }

        ent.client.showhelp = true;
        ent.client.pers.helpchanged = 0;
        PlayerHud.HelpComputer(ent);
    }

    /**
     * Cmd_Kill_f
     */
    public static void Kill_f(edict_t ent) {
        if ((GameBase.level.time - ent.client.respawn_time) < 5)
            return;
        ent.flags &= ~Defines.FL_GODMODE;
        ent.health = 0;
        GameBase.meansOfDeath = Defines.MOD_SUICIDE;
        PlayerClient.player_die.die(ent, ent, ent, 100000, Globals.vec3_origin);
    }

    /**
     * Cmd_PutAway_f
     */
    public static void PutAway_f(edict_t ent) {
        ent.client.showscores = false;
        ent.client.showhelp = false;
        ent.client.showinventory = false;
    }

    /**
     * Cmd_Players_f
     */
    public static void Players_f(edict_t ent) {
        int i;

        Integer[] index = new Integer[256];

        int count = 0;
        for (i = 0; i < GameBase.maxclients.value; i++) {
            if (GameBase.game.clients[i].pers.connected) {
                index[count] = i;
                count++;
            }
        }

        
        Arrays.sort(index, 0, count - 1, Cmd.PlayerSort);


        String large = "";

        for (i = 0; i < count; i++) {
            String small = GameBase.game.clients[index[i]].ps.stats[Defines.STAT_FRAGS]
                    + " "
                    + GameBase.game.clients[index[i]].pers.netname
                    + '\n';

            if (small.length() + large.length() > 1024 - 100) {
                
                large += "...\n";
                break;
            }
            large += small;
        }

        SV_GAME.PF_cprintfhigh(ent, large + '\n' + count + " players\n");
    }

    /**
     * Cmd_Wave_f
     */
    public static void Wave_f(edict_t ent) {

        int i = Lib.atoi(Cmd.Argv(1));

        
        if ((ent.client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0)
            return;

        if (ent.client.anim_priority > Defines.ANIM_WAVE)
            return;

        ent.client.anim_priority = Defines.ANIM_WAVE;

        switch (i) {
        case 0:
            SV_GAME.PF_cprintfhigh(ent, "flipoff\n");
            ent.s.frame = M_Player.FRAME_flip01 - 1;
            ent.client.anim_end = M_Player.FRAME_flip12;
            break;
        case 1:
            SV_GAME.PF_cprintfhigh(ent, "salute\n");
            ent.s.frame = M_Player.FRAME_salute01 - 1;
            ent.client.anim_end = M_Player.FRAME_salute11;
            break;
        case 2:
            SV_GAME.PF_cprintfhigh(ent, "taunt\n");
            ent.s.frame = M_Player.FRAME_taunt01 - 1;
            ent.client.anim_end = M_Player.FRAME_taunt17;
            break;
        case 3:
            SV_GAME.PF_cprintfhigh(ent, "wave\n");
            ent.s.frame = M_Player.FRAME_wave01 - 1;
            ent.client.anim_end = M_Player.FRAME_wave11;
            break;
        case 4:
        default:
            SV_GAME.PF_cprintfhigh(ent, "point\n");
            ent.s.frame = M_Player.FRAME_point01 - 1;
            ent.client.anim_end = M_Player.FRAME_point12;
            break;
        }
    }

    /**
     * Command to print the players own position.
     */
    public static void ShowPosition_f(edict_t ent) {
        SV_GAME.PF_cprintfhigh(ent, "pos=" + Lib.vtofsbeaty(ent.s.origin) + '\n');
    }

    /**
     * Cmd_Say_f
     */
    public static void Say_f(edict_t ent, boolean team, boolean arg0) {

        if (Cmd.Argc() < 2 && !arg0)
            return;

        if (0 == ((int) (GameBase.dmflags.value) & (Defines.DF_MODELTEAMS | Defines.DF_SKINTEAMS)))
            team = false;

        String text;
        if (team)
            text = '(' + ent.client.pers.netname + "): ";
        else
            text = ent.client.pers.netname + ": ";

        if (arg0) {
            text += Cmd.Argv(0);
            text += " ";
            text += Cmd.Args();
        } else {
            if (Cmd.Args().startsWith("\""))
                text += Cmd.Args().substring(1, Cmd.Args().length() - 1);
            else
                text += Cmd.Args();
        }

        
        if (text.length() > 150)
            
            text = text.substring(0, 150);

        text += "\n";

        if (GameBase.flood_msgs.value != 0) {
            gclient_t cl = ent.client;

            if (GameBase.level.time < cl.flood_locktill) {
                SV_GAME.PF_cprintfhigh(ent, "You can't talk for "
                                        + (int) (cl.flood_locktill - GameBase.level.time)
                                        + " more seconds\n");
                return;
            }
            int i = (int) (cl.flood_whenhead - GameBase.flood_msgs.value + 1);
            if (i < 0)
                i = (10) + i;
            if (cl.flood_when[i] != 0
                    && GameBase.level.time - cl.flood_when[i] < GameBase.flood_persecond.value) {
                cl.flood_locktill = GameBase.level.time + GameBase.flood_waitdelay.value;
                SV_GAME.PF_cprintf(ent, Defines.PRINT_CHAT,
                        "Flood protection:  You can't talk for "
                                + (int) GameBase.flood_waitdelay.value
                                + " seconds.\n");
                return;
            }

            cl.flood_whenhead = (cl.flood_whenhead + 1) % 10;
            cl.flood_when[cl.flood_whenhead] = GameBase.level.time;
        }

        if (Globals.dedicated.value != 0)
            SV_GAME.PF_cprintf(null, Defines.PRINT_CHAT, text);

        for (int j = 1; j <= GameBase.game.maxclients; j++) {
            edict_t other = GameBase.g_edicts[j];
            if (!other.inuse)
                continue;
            if (other.client == null)
                continue;
            if (team) {
                if (!GameUtil.OnSameTeam(ent, other))
                    continue;
            }
            SV_GAME.PF_cprintf(other, Defines.PRINT_CHAT, text);
        }

    }

    /**
     * Returns the playerlist. TODO: The list is badly formatted at the moment.
     */
    public static void PlayerList_f(edict_t ent) {


        String text = "";

        for (int i = 0; i < GameBase.maxclients.value; i++) {
            edict_t e2 = GameBase.g_edicts[1 + i];
            if (!e2.inuse)
                continue;

            String st = String.valueOf((GameBase.level.framenum - e2.client.resp.enterframe)
                    / 600) + ':' + ((GameBase.level.framenum - e2.client.resp.enterframe) % 600)
                    / 10 + ' ' + e2.client.ping + ' ' + e2.client.resp.score + ' ' + e2.client.pers.netname + ' ' + (e2.client.resp.spectator ? " (spectator)" : "") + '\n';

            if (text.length() + st.length() > 1024 - 50) {
                text += "And more...\n";
                SV_GAME.PF_cprintfhigh(ent, text);
                return;
            }
            text += st;
        }
        SV_GAME.PF_cprintfhigh(ent, text);
    }

    /**
     * Adds the current command line as a clc_stringcmd to the client message.
     * things like godmode, noclip, etc, are commands directed to the server, so
     * when they are typed in at the console, they will need to be forwarded.
     */
    public static void ForwardToServer() {

        String cmd = Cmd.Argv(0);
        if (Globals.cls.state <= Defines.ca_connected || cmd.charAt(0) == '-'
                || cmd.charAt(0) == '+') {
            Com.Printf("Unknown command \"" + cmd + "\"\n");
            return;
        }

        MSG.WriteByte(Globals.cls.netchan.message, Defines.clc_stringcmd);
        SZ.Print(Globals.cls.netchan.message, cmd);
        if (Cmd.Argc() > 1) {
            SZ.Print(Globals.cls.netchan.message, " ");
            SZ.Print(Globals.cls.netchan.message, Cmd.Args());
        }
    }

    /**
     * Cmd_CompleteCommand.
     *
     * TODO: revisit Vector syncronization replaced with lockless COW Array here
     * */
    public static List  CompleteCommand(String partial) {
        List<String> cmds = Stream.iterate(cmd_functions, Objects::nonNull, cmd -> cmd.next).filter(cmd -> cmd.name.startsWith(partial)).map(cmd -> cmd.name).collect(Collectors.toList());

        Stream.iterate(Globals.cmd_alias, Objects::nonNull, a -> a.next).filter(a -> a.name.startsWith(partial)).map(a -> a.name).forEach(cmds::add);
        return new CopyOnWriteArrayList( cmds);
    }

    /**
     * Processes the commands the player enters in the quake console.
     */
    public static void ClientCommand(edict_t ent) {

        if (ent.client == null)
            return;

        String cmd = game_import_t.argv(0).toLowerCase();
    
        if ("players".equals(cmd)) {
            Players_f(ent);
            return;
        }
        if ("say".equals(cmd)) {
            Say_f(ent, false, false);
            return;
        }
        if ("say_team".equals(cmd)) {
            Say_f(ent, true, false);
            return;
        }
        if ("score".equals(cmd)) {
            Score_f(ent);
            return;
        }
        if ("help".equals(cmd)) {
            Help_f(ent);
            return;
        }
    
        if (GameBase.level.intermissiontime != 0)
            return;

        switch (cmd) {
            case "use":
                Use_f(ent);
                break;
            case "drop":
                Drop_f(ent);
                break;
            case "give":
                Give_f(ent);
                break;
            case "god":
                God_f(ent);
                break;
            case "notarget":
                Notarget_f(ent);
                break;
            case "noclip":
                Noclip_f(ent);
                break;
            case "inven":
                Inven_f(ent);
                break;
            case "invnext":
                GameItems.SelectNextItem(ent, -1);
                break;
            case "invprev":
                GameItems.SelectPrevItem(ent, -1);
                break;
            case "invnextw":
                GameItems.SelectNextItem(ent, Defines.IT_WEAPON);
                break;
            case "invprevw":
                GameItems.SelectPrevItem(ent, Defines.IT_WEAPON);
                break;
            case "invnextp":
                GameItems.SelectNextItem(ent, Defines.IT_POWERUP);
                break;
            case "invprevp":
                GameItems.SelectPrevItem(ent, Defines.IT_POWERUP);
                break;
            case "invuse":
                InvUse_f(ent);
                break;
            case "invdrop":
                InvDrop_f(ent);
                break;
            case "weapprev":
                WeapPrev_f(ent);
                break;
            case "weapnext":
                WeapNext_f(ent);
                break;
            case "weaplast":
                WeapLast_f(ent);
                break;
            case "kill":
                Kill_f(ent);
                break;
            case "putaway":
                PutAway_f(ent);
                break;
            case "wave":
                Wave_f(ent);
                break;
            case "playerlist":
                PlayerList_f(ent);
                break;
            case "showposition":
                ShowPosition_f(ent);
                break;
            default:
                
                Say_f(ent, false, true);
                break;
        }
    }

    public static void ValidateSelectedItem(edict_t ent) {
        gclient_t cl = ent.client;
    
        if (cl.pers.inventory[cl.pers.selected_item] != 0)
            return; 
    
        GameItems.SelectNextItem(ent, -1);
    }
}