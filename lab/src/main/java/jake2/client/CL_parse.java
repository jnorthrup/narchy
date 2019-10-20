/*
 * CL_parse.java
 * Copyright (C) 2004
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
package jake2.client;

import jake2.Defines;
import jake2.Globals;
import jake2.game.Cmd;
import jake2.game.entity_state_t;
import jake2.qcommon.*;
import jake2.render.model_t;
import jake2.sound.S;
import jake2.sys.Sys;
import jake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * CL_parse
 */
public class CL_parse {

    

    public static final String[] svc_strings = { "svc_bad", "svc_muzzleflash",
            "svc_muzzlflash2", "svc_temp_entity", "svc_layout",
            "svc_inventory", "svc_nop", "svc_disconnect", "svc_reconnect",
            "svc_sound", "svc_print", "svc_stufftext", "svc_serverdata",
            "svc_configstring", "svc_spawnbaseline", "svc_centerprint",
            "svc_download", "svc_playerinfo", "svc_packetentities",
            "svc_deltapacketentities", "svc_frame" };

    

    public static String DownloadFileName(String fn) {
        return FS.Gamedir() + '/' + fn;
    }

    /**
     * CL_CheckOrDownloadFile returns true if the file exists, 
     * otherwise it attempts to start a
     * download from the server.
     */
    public static boolean CheckOrDownloadFile(String filename) {
        if (filename.contains("..")) {
            Com.Printf("Refusing to download a path with ..\n");
            return true;
        }

        if (FS.FileLength(filename) > 0) {
            
            return true;
        }

        Globals.cls.downloadname = filename;

        
        
        
        Globals.cls.downloadtempname = Com
                .StripExtension(Globals.cls.downloadname);
        Globals.cls.downloadtempname += ".tmp";


        String name = DownloadFileName(Globals.cls.downloadtempname);

        RandomAccessFile fp = Lib.fopen(name, "r+b");

        if (fp != null) { 
            
            
            long len = 0L;

            try {
                len = fp.length();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
            

            Globals.cls.download = fp;

            
            Com.Printf("Resuming " + Globals.cls.downloadname + '\n');
            MSG.WriteByte(Globals.cls.netchan.message, Defines.clc_stringcmd);
            MSG.WriteString(Globals.cls.netchan.message, "download "
                    + Globals.cls.downloadname + ' ' + len);
        } else {
            Com.Printf("Downloading " + Globals.cls.downloadname + '\n');
            MSG.WriteByte(Globals.cls.netchan.message, Defines.clc_stringcmd);
            MSG.WriteString(Globals.cls.netchan.message, "download "
                    + Globals.cls.downloadname);
        }

        Globals.cls.downloadnumber++;

        return false;
    }

    /*
     * =============== CL_Download_f
     * 
     * Request a download from the server ===============
     */
    public static final xcommand_t Download_f = new xcommand_t() {
        @Override
        public void execute() {

            if (Cmd.Argc() != 2) {
                Com.Printf("Usage: download <filename>\n");
                return;
            }

            String filename = Cmd.Argv(1);

            if (filename.contains("..")) {
                Com.Printf("Refusing to download a path with ..\n");
                return;
            }

            if (FS.LoadFile(filename) != null) { 
                
                Com.Printf("File already exists.\n");
                return;
            }

            Globals.cls.downloadname = filename;
            Com.Printf("Downloading " + Globals.cls.downloadname + '\n');

            
            
            
            Globals.cls.downloadtempname = Com
                    .StripExtension(Globals.cls.downloadname);
            Globals.cls.downloadtempname += ".tmp";

            MSG.WriteByte(Globals.cls.netchan.message, Defines.clc_stringcmd);
            MSG.WriteString(Globals.cls.netchan.message, "download "
                    + Globals.cls.downloadname);

            Globals.cls.downloadnumber++;
        }
    };

    /*
     * ====================== CL_RegisterSounds ======================
     */
    public static void RegisterSounds() {
        S.BeginRegistration();
        CL_tent.RegisterTEntSounds();
        for (int i = 1; i < Defines.MAX_SOUNDS; i++) {
            if (Globals.cl.configstrings[Defines.CS_SOUNDS + i] == null
                    || Globals.cl.configstrings[Defines.CS_SOUNDS + i].isEmpty()
                    || "\0"
                            .equals(Globals.cl.configstrings[Defines.CS_SOUNDS + i]))
                break;
            Globals.cl.sound_precache[i] = S
                    .RegisterSound(Globals.cl.configstrings[Defines.CS_SOUNDS
                            + i]);
            Sys.SendKeyEvents(); 
        }
        S.EndRegistration();
    }

    /*
     * ===================== CL_ParseDownload
     * 
     * A download message has been received from the server
     * =====================
     */
    public static void ParseDownload() {

        
        int size = (int) MSG.ReadShort(Globals.net_message);
        int percent = MSG.ReadByte(Globals.net_message);
        if (size == -1) {
            Com.Printf("Server does not have this file.\n");
            if (Globals.cls.download != null) {
                
                try {
                    Globals.cls.download.close();
                } catch (IOException e) {
                }
                Globals.cls.download = null;
            }
            CL.RequestNextDownload();
            return;
        }

        
        if (Globals.cls.download == null) {
            String name = DownloadFileName(Globals.cls.downloadtempname).toLowerCase();

            FS.CreatePath(name);

            Globals.cls.download = Lib.fopen(name, "rw");
            if (Globals.cls.download == null) {
                Globals.net_message.readcount += size;
                Com.Printf("Failed to open " + Globals.cls.downloadtempname
                        + '\n');
                CL.RequestNextDownload();
                return;
            }
        }


        try {
            Globals.cls.download.write(Globals.net_message.data,
                    Globals.net_message.readcount, size);
        } 
        catch (Exception e) {
        }
        Globals.net_message.readcount += size;

        if (percent != 100) {
            
            
            Globals.cls.downloadpercent = percent;
            MSG.WriteByte(Globals.cls.netchan.message, Defines.clc_stringcmd);
            SZ.Print(Globals.cls.netchan.message, "nextdl");
        } else {
            try {
                Globals.cls.download.close();
            } 
            catch (IOException e) {
            }


            String oldn = DownloadFileName(Globals.cls.downloadtempname);
            String newn = DownloadFileName(Globals.cls.downloadname);
            int r = Lib.rename(oldn, newn);
            if (r != 0)
                Com.Printf("failed to rename.\n");

            Globals.cls.download = null;
            Globals.cls.downloadpercent = 0;

            

            CL.RequestNextDownload();
        }
    }

    /*
     * =====================================================================
     * 
     * SERVER CONNECTING MESSAGES
     * 
     * =====================================================================
     */

    /*
     * ================== CL_ParseServerData ==================
     */
    
    public static void ParseServerData() {
        Com.DPrintf("ParseServerData():Serverdata packet received.\n");
        
        
        
        CL.ClearState();
        Globals.cls.state = Defines.ca_connected;


        int i = MSG.ReadLong(Globals.net_message);
        Globals.cls.serverProtocol = i;

        
        if (Globals.server_state != 0 && Defines.PROTOCOL_VERSION == 34) {
        } else if (i != Defines.PROTOCOL_VERSION)
            Com.Error(Defines.ERR_DROP, "Server returned version " + i
                    + ", not " + Defines.PROTOCOL_VERSION);

        Globals.cl.servercount = MSG.ReadLong(Globals.net_message);
        Globals.cl.attractloop = MSG.ReadByte(Globals.net_message) != 0;


        String str = MSG.ReadString(Globals.net_message);
        Globals.cl.gamedir = str;
        Com.dprintln("gamedir=" + str);

        
        if (str.length() > 0
                && (FS.fs_gamedirvar.string == null
                        || FS.fs_gamedirvar.string.length() == 0 || FS.fs_gamedirvar.string
                        .equals(str))
                || (str.length() == 0 && (FS.fs_gamedirvar.string != null || FS.fs_gamedirvar.string
                        .length() == 0)))
            Cvar.Set("game", str);

        
        Globals.cl.playernum = (int) MSG.ReadShort(Globals.net_message);
        Com.dprintln("numplayers=" + Globals.cl.playernum);
        
        str = MSG.ReadString(Globals.net_message);
        Com.dprintln("levelname=" + str);

        if (Globals.cl.playernum == -1) { 
            
            SCR.PlayCinematic(str);
        } else {
            
            
            
            
            Com.Printf("Levelname:" + str + '\n');
            
            Globals.cl.refresh_prepped = false;
        }
    }

    /*
     * ================== CL_ParseBaseline ==================
     */
    public static void ParseBaseline() {
        entity_state_t nullstate = new entity_state_t(null);

        int[] bits = {0};
        int newnum = CL_ents.ParseEntityBits(bits);
        entity_state_t es = Globals.cl_entities[newnum].baseline;
        CL_ents.ParseDelta(nullstate, es, newnum, bits[0]);
    }

    /*
     * ================ CL_LoadClientinfo
     * 
     * ================
     */
    public static void LoadClientinfo(clientinfo_t ci, String s) {


        ci.cinfo = s;
        

        
        ci.name = s;


        int t = s.indexOf((int) '\\');
        

        if (t != -1) {
            ci.name = s.substring(0, t);
            s = s.substring(t + 1);
            
        }

        String weapon_filename;
        String skin_filename;
        String model_filename;
        if (Globals.cl_noskins.value != (float) 0 || s.length() == 0) {

            model_filename = ("players/male/tris.md2");
            weapon_filename = ("players/male/weapon.md2");
            skin_filename = ("players/male/grunt.pcx");
            ci.iconname = ("/players/male/grunt_i.pcx");

            ci.model = Globals.re.RegisterModel(model_filename);

            ci.weaponmodel = new model_t[Defines.MAX_CLIENTWEAPONMODELS];
            ci.weaponmodel[0] = Globals.re.RegisterModel(weapon_filename);
            ci.skin = Globals.re.RegisterSkin(skin_filename);
            ci.icon = Globals.re.RegisterPic(ci.iconname);
            
        } else {


            int pos = s.indexOf((int) '/');

            if (pos == -1)
                pos = s.indexOf((int) '/');
            if (pos == -1) {
                pos = 0;
                Com.Error(Defines.ERR_FATAL, "Invalid model name:" + s);
            }

            String model_name = s.substring(0, pos);


            String skin_name = s.substring(pos + 1);


            model_filename = "players/" + model_name + "/tris.md2";
            ci.model = Globals.re.RegisterModel(model_filename);

            if (ci.model == null) {
                model_name = "male";
                model_filename = "players/male/tris.md2";
                ci.model = Globals.re.RegisterModel(model_filename);
            }

            
            skin_filename = "players/" + model_name + '/' + skin_name + ".pcx";
            ci.skin = Globals.re.RegisterSkin(skin_filename);

            
            
            if (ci.skin == null && !"male".equalsIgnoreCase(model_name)) {
                
                model_name = "male";
                model_filename = "players/male/tris.md2";
                ci.model = Globals.re.RegisterModel(model_filename);

                
                skin_filename = "players/" + model_name + '/' + skin_name
                        + ".pcx";
                ci.skin = Globals.re.RegisterSkin(skin_filename);
            }

            
            
            
            if (ci.skin == null) {
                
                skin_filename = "players/" + model_name + "/grunt.pcx";
                ci.skin = Globals.re.RegisterSkin(skin_filename);
            }

            
            for (int i = 0; i < CL_view.num_cl_weaponmodels; i++) {
                weapon_filename = "players/" + model_name + '/'
                        + CL_view.cl_weaponmodels[i];
                ci.weaponmodel[i] = Globals.re.RegisterModel(weapon_filename);
                if (null == ci.weaponmodel[i] && "cyborg".equals(model_name)) {
                    
                    weapon_filename = "players/male/"
                            + CL_view.cl_weaponmodels[i];
                    ci.weaponmodel[i] = Globals.re
                            .RegisterModel(weapon_filename);
                }
                if ((float) 0 == Globals.cl_vwep.value)
                    break; 
            }

            
            ci.iconname = "/players/" + model_name + '/' + skin_name + "_i.pcx";
            ci.icon = Globals.re.RegisterPic(ci.iconname);
        }

        
        if (ci.skin == null || ci.icon == null || ci.model == null
                || ci.weaponmodel[0] == null) {
            ci.skin = null;
            ci.icon = null;
            ci.model = null;
            ci.weaponmodel[0] = null;
        }
    }

    /*
     * ================ CL_ParseClientinfo
     * 
     * Load the skin, icon, and model for a client ================
     */
    public static void ParseClientinfo(int player) {
        String s = Globals.cl.configstrings[player + Defines.CS_PLAYERSKINS];

        clientinfo_t ci = Globals.cl.clientinfo[player];

        LoadClientinfo(ci, s);
    }

    /*
     * ================ CL_ParseConfigString ================
     */
    public static void ParseConfigString() {
        int i = (int) MSG.ReadShort(Globals.net_message);

        if (i < 0 || i >= Defines.MAX_CONFIGSTRINGS)
            Com.Error(Defines.ERR_DROP, "configstring > MAX_CONFIGSTRINGS");

        String s = MSG.ReadString(Globals.net_message);

        String olds = Globals.cl.configstrings[i];
        Globals.cl.configstrings[i] = s;
        
        

        

        if (i >= Defines.CS_LIGHTS
                && i < Defines.CS_LIGHTS + Defines.MAX_LIGHTSTYLES) {
            
            CL_fx.SetLightstyle(i - Defines.CS_LIGHTS);
            
        } else if (i >= Defines.CS_MODELS && i < Defines.CS_MODELS + Defines.MAX_MODELS) {
            if (Globals.cl.refresh_prepped) {
                Globals.cl.model_draw[i - Defines.CS_MODELS] = Globals.re
                        .RegisterModel(Globals.cl.configstrings[i]);
                if (Globals.cl.configstrings[i].startsWith("*"))
                    Globals.cl.model_clip[i - Defines.CS_MODELS] = CM
                            .InlineModel(Globals.cl.configstrings[i]);
                else
                    Globals.cl.model_clip[i - Defines.CS_MODELS] = null;
            }
        } else if (i >= Defines.CS_SOUNDS
                && i < Defines.CS_SOUNDS + Defines.MAX_MODELS) {
            if (Globals.cl.refresh_prepped)
                Globals.cl.sound_precache[i - Defines.CS_SOUNDS] = S
                        .RegisterSound(Globals.cl.configstrings[i]);
        } else if (i >= Defines.CS_IMAGES
                && i < Defines.CS_IMAGES + Defines.MAX_MODELS) {
            if (Globals.cl.refresh_prepped)
                Globals.cl.image_precache[i - Defines.CS_IMAGES] = Globals.re
                        .RegisterPic(Globals.cl.configstrings[i]);
        } else if (i >= Defines.CS_PLAYERSKINS
                && i < Defines.CS_PLAYERSKINS + Defines.MAX_CLIENTS) {
            if (Globals.cl.refresh_prepped && !olds.equals(s))
                ParseClientinfo(i - Defines.CS_PLAYERSKINS);
        }
    }

    /*
     * =====================================================================
     * 
     * ACTION MESSAGES
     * 
     * =====================================================================
     */

    private static final float[] pos_v = {(float) 0, (float) 0, (float) 0};
    /*
     * ================== CL_ParseStartSoundPacket ==================
     */
    public static void ParseStartSoundPacket() {
        int flags = MSG.ReadByte(Globals.net_message);
        int sound_num = MSG.ReadByte(Globals.net_message);

        float volume;
        if ((flags & Defines.SND_VOLUME) != 0)
            volume = (float) MSG.ReadByte(Globals.net_message) / 255.0f;
        else
            volume = Defines.DEFAULT_SOUND_PACKET_VOLUME;

        float attenuation;
        if ((flags & Defines.SND_ATTENUATION) != 0)
            attenuation = (float) MSG.ReadByte(Globals.net_message) / 64.0f;
        else
            attenuation = Defines.DEFAULT_SOUND_PACKET_ATTENUATION;

        float ofs;
        if ((flags & Defines.SND_OFFSET) != 0)
            ofs = (float) MSG.ReadByte(Globals.net_message) / 1000.0f;
        else
            ofs = (float) 0;

        int channel;
        int ent;
        if ((flags & Defines.SND_ENT) != 0) { 
            channel = (int) MSG.ReadShort(Globals.net_message);
            ent = channel >> 3;
            if (ent > Defines.MAX_EDICTS)
                Com.Error(Defines.ERR_DROP, "CL_ParseStartSoundPacket: ent = "
                        + ent);

            channel &= 7;
        } else {
            ent = 0;
            channel = 0;
        }

        float[] pos;
        if ((flags & Defines.SND_POS) != 0) { 
            MSG.ReadPos(Globals.net_message, pos_v);
            
            pos = pos_v;
        } else
            
            pos = null;

        if (null == Globals.cl.sound_precache[sound_num])
            return;

        S.StartSound(pos, ent, channel, Globals.cl.sound_precache[sound_num],
                volume, attenuation, ofs);
    }

    public static void SHOWNET(String s) {
        if (Globals.cl_shownet.value >= 2.0F)
            Com.Printf(Globals.net_message.readcount - 1 + ":" + s + '\n');
    }

    /*
     * ===================== CL_ParseServerMessage =====================
     */
    public static void ParseServerMessage() {
        
        
        
        
        
        
        

        
        
        
        while (true) {
            if (Globals.net_message.readcount > Globals.net_message.cursize) {
                Com.Error(Defines.ERR_FATAL,
                        "CL_ParseServerMessage: Bad server message:");
                break;
            }

            int cmd = MSG.ReadByte(Globals.net_message);

            if (cmd == -1) {
                SHOWNET("END OF MESSAGE");
                break;
            }

            if (Globals.cl_shownet.value >= 2.0F) {
                if (null == svc_strings[cmd])
                    Com.Printf(Globals.net_message.readcount - 1 + ":BAD CMD "
                            + cmd + '\n');
                else
                    SHOWNET(svc_strings[cmd]);
            }

            
            switch (cmd) {
            default:
                Com.Error(Defines.ERR_DROP,
                        "CL_ParseServerMessage: Illegible server message\n");
                break;

            case Defines.svc_nop:
                
                break;

            case Defines.svc_disconnect:
                Com.Error(Defines.ERR_DISCONNECT, "Server disconnected\n");
                break;

            case Defines.svc_reconnect:
                Com.Printf("Server disconnected, reconnecting\n");
                if (Globals.cls.download != null) {
                    
                    try {
                        Globals.cls.download.close();
                    } catch (IOException e) {
                    }
                    Globals.cls.download = null;
                }
                Globals.cls.state = Defines.ca_connecting;
                Globals.cls.connect_time = -99999.0F;
                
                break;

            case Defines.svc_print:
                int i = MSG.ReadByte(Globals.net_message);
                if (i == Defines.PRINT_CHAT) {
                    S.StartLocalSound("misc/talk.wav");
                    Globals.con.ormask = 128;
                }
                Com.Printf(MSG.ReadString(Globals.net_message));
                Globals.con.ormask = 0;
                break;

            case Defines.svc_centerprint:
                SCR.CenterPrint(MSG.ReadString(Globals.net_message));
                break;

            case Defines.svc_stufftext:
                String s = MSG.ReadString(Globals.net_message);
                Com.DPrintf("stufftext: " + s + '\n');
                Cbuf.AddText(s);
                break;

            case Defines.svc_serverdata:
                Cbuf.Execute(); 
                ParseServerData();
                break;

            case Defines.svc_configstring:
                ParseConfigString();
                break;

            case Defines.svc_sound:
                ParseStartSoundPacket();
                break;

            case Defines.svc_spawnbaseline:
                ParseBaseline();
                break;

            case Defines.svc_temp_entity:
                CL_tent.ParseTEnt();
                break;

            case Defines.svc_muzzleflash:
                CL_fx.ParseMuzzleFlash();
                break;

            case Defines.svc_muzzleflash2:
                CL_fx.ParseMuzzleFlash2();
                break;

            case Defines.svc_download:
                ParseDownload();
                break;

            case Defines.svc_frame:
                CL_ents.ParseFrame();
                break;

            case Defines.svc_inventory:
                CL_inv.ParseInventory();
                break;

            case Defines.svc_layout:
        	Globals.cl.layout = MSG.ReadString(Globals.net_message);
                break;

            case Defines.svc_playerinfo:
            case Defines.svc_packetentities:
            case Defines.svc_deltapacketentities:
                Com.Error(Defines.ERR_DROP, "Out of place frame data");
                break;
            }
        }

        CL_view.AddNetgraph();

        
        
        
        
        if (Globals.cls.demorecording && !Globals.cls.demowaiting)
            CL.WriteDemoMessage();
    }
}