/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */



package jake2.server;

import jake2.Defines;
import jake2.Globals;
import jake2.game.*;
import jake2.qcommon.*;
import jake2.sys.NET;
import jake2.sys.Timer;
import jake2.util.Lib;

import java.io.IOException;
import java.util.stream.IntStream;

public class SV_MAIN {

	/** Addess of group servers.*/ 
    public static final netadr_t[] master_adr = new netadr_t[Defines.MAX_MASTERS];
                                                                            
                                                                            
                                                                            
    static {
        for (int i = 0; i < Defines.MAX_MASTERS; i++) {
            master_adr[i] = new netadr_t();
        }
    }

    public static client_t sv_client; 

    public static cvar_t sv_paused;

    public static cvar_t sv_timedemo;

    public static cvar_t sv_enforcetime;

    public static cvar_t timeout; 

    public static cvar_t zombietime; 
                                     

    public static cvar_t rcon_password; 

    public static cvar_t allow_download;

    public static cvar_t allow_download_players;

    public static cvar_t allow_download_models;

    public static cvar_t allow_download_sounds;

    public static cvar_t allow_download_maps;

    public static cvar_t sv_airaccelerate;

    public static cvar_t sv_noreload; 
                                      

    public static cvar_t maxclients; 

    public static cvar_t sv_showclamp;

    public static cvar_t hostname;

    public static cvar_t public_server; 

    public static cvar_t sv_reconnect_limit; 
                                             

    /**
     * Send a message to the master every few minutes to let it know we are
     * alive, and log information.
     */
    public static final int HEARTBEAT_SECONDS = 300;

    /**
     * Called when the player is totally leaving the server, either willingly or
     * unwillingly. This is NOT called if the entire server is quiting or
     * crashing.
     */
    public static void SV_DropClient(client_t drop) {
        
        MSG.WriteByte(drop.netchan.message, Defines.svc_disconnect);

        if (drop.state == Defines.cs_spawned) {
            
            
            PlayerClient.ClientDisconnect(drop.edict);
        }

        if (drop.download != null) {
            FS.FreeFile(drop.download);
            drop.download = null;
        }

        drop.state = Defines.cs_zombie; 
        drop.name = "";
    }

    
    /* ==============================================================================
     * 
     * CONNECTIONLESS COMMANDS
     * 
     * ==============================================================================*/
     
    /**
     * Builds the string that is sent as heartbeats and status replies.
     */
    public static String SV_StatusString() {

        String status = Cvar.Serverinfo() + '\n';

        for (int i = 0; i < SV_MAIN.maxclients.value; i++) {
            client_t cl = SV_INIT.svs.clients[i];
            if (cl.state == Defines.cs_connected
                    || cl.state == Defines.cs_spawned) {
                String player = String.valueOf(cl.edict.client.ps.stats[Defines.STAT_FRAGS]) + ' ' + cl.ping + '"' + cl.name + "\"\n";

                int playerLength = player.length();
                int statusLength = status.length();

                if (statusLength + playerLength >= 1024)
                    break; 

                status += player;
            }
        }

        return status;
    }

    /**
     * Responds with all the info that qplug or qspy can see
     */
    public static void SVC_Status() {
        Netchan.OutOfBandPrint(Defines.NS_SERVER, Globals.net_from, "print\n"
                + SV_StatusString());
    }

    /**
     *  SVC_Ack
     */
    public static void SVC_Ack() {
        Com.Printf("Ping acknowledge from " + NET.AdrToString(Globals.net_from)
                + '\n');
    }

    /**
     * SVC_Info, responds with short info for broadcast scans The second parameter should
     * be the current protocol version number.
     */
    public static void SVC_Info() {

        if (SV_MAIN.maxclients.value == 1)
            return;

        int version = Lib.atoi(Cmd.Argv(1));

        String string;
        if (version != Defines.PROTOCOL_VERSION)
            string = SV_MAIN.hostname.string + ": wrong version\n";
        else {
            int count = 0;
            for (int i = 0; i < SV_MAIN.maxclients.value; i++)
                if (SV_INIT.svs.clients[i].state >= Defines.cs_connected)
                    count++;

            string = SV_MAIN.hostname.string + ' ' + SV_INIT.sv.name + ' '
                    + count + '/' + (int) SV_MAIN.maxclients.value + '\n';
        }

        Netchan.OutOfBandPrint(Defines.NS_SERVER, Globals.net_from, "info\n"
                + string);
    }

    /**
     * SVC_Ping, Just responds with an acknowledgement.
     */
    public static void SVC_Ping() {
        Netchan.OutOfBandPrint(Defines.NS_SERVER, Globals.net_from, "ack");
    }

    /** 
     * Returns a challenge number that can be used in a subsequent
     * client_connect command. We do this to prevent denial of service attacks
     * that flood the server with invalid connection IPs. With a challenge, they
     * must give a valid IP address.
     */
    public static void SVC_GetChallenge() {
        int i;

        int oldest = 0;
        int oldestTime = 0x7fffffff;

        
        for (i = 0; i < Defines.MAX_CHALLENGES; i++) {
            if (NET.CompareBaseAdr(Globals.net_from,
                    SV_INIT.svs.challenges[i].adr))
                break;
            if (SV_INIT.svs.challenges[i].time < oldestTime) {
                oldestTime = SV_INIT.svs.challenges[i].time;
                oldest = i;
            }
        }

        if (i == Defines.MAX_CHALLENGES) {
            
            SV_INIT.svs.challenges[oldest].challenge = Lib.rand() & 0x7fff;
            SV_INIT.svs.challenges[oldest].adr = Globals.net_from;
            SV_INIT.svs.challenges[oldest].time = Globals.curtime;
            i = oldest;
        }

        
        Netchan.OutOfBandPrint(Defines.NS_SERVER, Globals.net_from,
                "challenge " + SV_INIT.svs.challenges[i].challenge);
    }

    /**
     * A connection request that did not come from the master.
     */
    public static void SVC_DirectConnect() {

        netadr_t adr = Globals.net_from;

        Com.DPrintf("SVC_DirectConnect ()\n");

        int version = Lib.atoi(Cmd.Argv(1));
        if (version != Defines.PROTOCOL_VERSION) {
            Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                    "print\nServer is version " + Globals.VERSION + '\n');
            Com.DPrintf("    rejected connect from version " + version + '\n');
            return;
        }

        int qport = Lib.atoi(Cmd.Argv(2));
        int challenge = Lib.atoi(Cmd.Argv(3));
        String userinfo = Cmd.Argv(4);


        userinfo = Info.Info_SetValueForKey(userinfo, "ip", NET.AdrToString(Globals.net_from));

        
        if (SV_INIT.sv.attractloop) {
            if (!NET.IsLocalAddress(adr)) {
                Com.Printf("Remote connect in attract loop.  Ignored.\n");
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                        "print\nConnection refused.\n");
                return;
            }
        }


        int i;
        if (!NET.IsLocalAddress(adr)) {
            for (i = 0; i < Defines.MAX_CHALLENGES; i++) {
                if (NET.CompareBaseAdr(Globals.net_from,
                        SV_INIT.svs.challenges[i].adr)) {
                    if (challenge == SV_INIT.svs.challenges[i].challenge)
                        break; 
                    Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                            "print\nBad challenge.\n");
                    return;
                }
            }
            if (i == Defines.MAX_CHALLENGES) {
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                        "print\nNo challenge for address.\n");
                return;
            }
        }


        client_t cl;
        for (i = 0; i < SV_MAIN.maxclients.value; i++) {
            cl = SV_INIT.svs.clients[i];

            if (cl.state == Defines.cs_free)
                continue;
            if (NET.CompareBaseAdr(adr, cl.netchan.remote_address)
                    && (cl.netchan.qport == qport || adr.port == cl.netchan.remote_address.port)) {
                if (!NET.IsLocalAddress(adr)
                        && (SV_INIT.svs.realtime - cl.lastconnect) < ((int) SV_MAIN.sv_reconnect_limit.value * 1000)) {
                    Com.DPrintf(NET.AdrToString(adr)
                            + ":reconnect rejected : too soon\n");
                    return;
                }
                Com.Printf(NET.AdrToString(adr) + ":reconnect\n");

                gotnewcl(i, challenge, userinfo, adr, qport);
                return;
            }
        }


        int index = -1;
        for (i = 0; i < SV_MAIN.maxclients.value; i++) {
            cl = SV_INIT.svs.clients[i];
            if (cl.state == Defines.cs_free) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                    "print\nServer is full.\n");
            Com.DPrintf("Rejected a connection.\n");
            return;
        }
        gotnewcl(index, challenge, userinfo, adr, qport);
    }

    /**
     * Initializes player structures after successfull connection.
     */
    public static void gotnewcl(int i, int challenge, String userinfo,
            netadr_t adr, int qport) {
        
        
        

        SV_MAIN.sv_client = SV_INIT.svs.clients[i];

        int edictnum = i + 1;

        edict_t ent = GameBase.g_edicts[edictnum];
        SV_INIT.svs.clients[i].edict = ent;
        
        
        SV_INIT.svs.clients[i].challenge = challenge;
        
        

        
        
        if (!(PlayerClient.ClientConnect(ent, userinfo))) {
            if (Info.Info_ValueForKey(userinfo, "rejmsg") != null)
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr, "print\n"
                        + Info.Info_ValueForKey(userinfo, "rejmsg")
                        + "\nConnection refused.\n");
            else
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                        "print\nConnection refused.\n");
            Com.DPrintf("Game rejected a connection.\n");
            return;
        }

        
        SV_INIT.svs.clients[i].userinfo = userinfo;
        SV_UserinfoChanged(SV_INIT.svs.clients[i]);

        
        Netchan.OutOfBandPrint(Defines.NS_SERVER, adr, "client_connect");

        Netchan.Setup(Defines.NS_SERVER, SV_INIT.svs.clients[i].netchan, adr, qport);

        SV_INIT.svs.clients[i].state = Defines.cs_connected;

        SZ.Init(SV_INIT.svs.clients[i].datagram,
                SV_INIT.svs.clients[i].datagram_buf,
                SV_INIT.svs.clients[i].datagram_buf.length);
        
        SV_INIT.svs.clients[i].datagram.allowoverflow = true;
        SV_INIT.svs.clients[i].lastmessage = SV_INIT.svs.realtime; 
        SV_INIT.svs.clients[i].lastconnect = SV_INIT.svs.realtime;
        Com.DPrintf("new client added.\n");
    }

    
    /** 
     * Checks if the rcon password is corect.
     */
    public static int Rcon_Validate() {
        if (0 == SV_MAIN.rcon_password.string.length())
            return 0;

        if (0 != Lib.strcmp(Cmd.Argv(1), SV_MAIN.rcon_password.string))
            return 0;

        return 1;
    }

    /**
     * A client issued an rcon command. Shift down the remaining args Redirect
     * all printfs fromt hte server to the client.
     */
    public static void SVC_RemoteCommand() {

        int i = Rcon_Validate();

        String msg = Lib.CtoJava(Globals.net_message.data, 4, 1024);

        if (i == 0)
            Com.Printf("Bad rcon from " + NET.AdrToString(Globals.net_from)
                    + ":\n" + msg + '\n');
        else
            Com.Printf("Rcon from " + NET.AdrToString(Globals.net_from) + ":\n"
                    + msg + '\n');

        Com.BeginRedirect(Defines.RD_PACKET, SV_SEND.sv_outputbuf,
                Defines.SV_OUTPUTBUF_LENGTH, new Com.RD_Flusher() {
                    @Override
                    public void rd_flush(int target, StringBuffer buffer) {
                        SV_SEND.SV_FlushRedirect(target, Lib.stringToBytes(buffer.toString()));
                    }
                });

        if (0 == Rcon_Validate()) {
            Com.Printf("Bad rcon_password.\n");
        } else {
            String remaining = "";

            for (i = 2; i < Cmd.Argc(); i++) {
                remaining += Cmd.Argv(i);
                remaining += " ";
            }

            Cmd.ExecuteString(remaining);
        }

        Com.EndRedirect();
    }

    /**
     * A connectionless packet has four leading 0xff characters to distinguish
     * it from a game channel. Clients that are in the game can still send
     * connectionless packets. It is used also by rcon commands.
     */
    public static void SV_ConnectionlessPacket() {

        MSG.BeginReading(Globals.net_message);
        MSG.ReadLong(Globals.net_message);

        String s = MSG.ReadStringLine(Globals.net_message);

        Cmd.TokenizeString(s.toCharArray(), false);

        String c = Cmd.Argv(0);
        
        
        
        

        if (0 == Lib.strcmp(c, "ping"))
            SVC_Ping();
        else if (0 == Lib.strcmp(c, "ack"))
            SVC_Ack();
        else if (0 == Lib.strcmp(c, "status"))
            SVC_Status();
        else if (0 == Lib.strcmp(c, "info"))
            SVC_Info();
        else if (0 == Lib.strcmp(c, "getchallenge"))
            SVC_GetChallenge();
        else if (0 == Lib.strcmp(c, "connect"))
            SVC_DirectConnect();
        else if (0 == Lib.strcmp(c, "rcon"))
            SVC_RemoteCommand();
        else {
            Com.Printf("bad connectionless packet from "
                    + NET.AdrToString(Globals.net_from) + '\n');
            Com.Printf('[' + s + "]\n");
            Com.Printf(Lib.hexDump(Globals.net_message.data, 128, false));
        }
    }

    /**
     * Updates the cl.ping variables.
     */
    public static void SV_CalcPings() {

        for (int i = 0; i < SV_MAIN.maxclients.value; i++) {
            client_t cl = SV_INIT.svs.clients[i];
            if (cl.state != Defines.cs_spawned)
                continue;

            int total = 0;
            int count = 0;
            for (int j = 0; j < Defines.LATENCY_COUNTS; j++) {
                if (cl.frame_latency[j] > 0) {
                    count++;
                    total += cl.frame_latency[j];
                }
            }
            if (0 == count)
                cl.ping = 0;
            else
                cl.ping = total / count;

            
            cl.edict.client.ping = cl.ping;
        }
    }

    /**
     * Every few frames, gives all clients an allotment of milliseconds for
     * their command moves. If they exceed it, assume cheating.
     */
    public static void SV_GiveMsec() {

        if ((SV_INIT.sv.framenum & 15) != 0)
            return;

        for (int i = 0; i < SV_MAIN.maxclients.value; i++) {
            client_t cl = SV_INIT.svs.clients[i];
            if (cl.state == Defines.cs_free)
                continue;

            cl.commandMsec = 1800; 
        }
    }

    /**
     * Reads packets from the network or loopback.
     */
    public static void SV_ReadPackets() {
        int qport = 0;

        while (NET.GetPacket(Defines.NS_SERVER, Globals.net_from,
                Globals.net_message)) {


            boolean b = true;
            for (int v : new int[]{0, 1, 2, 3}) {
                if ((Globals.net_message.data[v] != -1)) {
                    b = false;
                    break;
                }
            }
            if (b) {
                SV_ConnectionlessPacket();
                continue;
            }

            
            
            MSG.BeginReading(Globals.net_message);
            MSG.ReadLong(Globals.net_message); 
            MSG.ReadLong(Globals.net_message); 
            qport = MSG.ReadShort(Globals.net_message) & 0xffff;


            int i;
            for (i = 0; i < SV_MAIN.maxclients.value; i++) {
                client_t cl = SV_INIT.svs.clients[i];
                if (cl.state == Defines.cs_free)
                    continue;
                if (!NET.CompareBaseAdr(Globals.net_from,
                        cl.netchan.remote_address))
                    continue;
                if (cl.netchan.qport != qport)
                    continue;
                if (cl.netchan.remote_address.port != Globals.net_from.port) {
                    Com.Printf("SV_ReadPackets: fixing up a translated port\n");
                    cl.netchan.remote_address.port = Globals.net_from.port;
                }

                if (Netchan.Process(cl.netchan, Globals.net_message)) {
                    
                    if (cl.state != Defines.cs_zombie) {
                        cl.lastmessage = SV_INIT.svs.realtime; 
                        SV_USER.SV_ExecuteClientMessage(cl);
                    }
                }
                break;
            }

            if (i != SV_MAIN.maxclients.value)
                continue;
        }
    }

    /**
     * If a packet has not been received from a client for timeout.value
     * seconds, drop the conneciton. Server frames are used instead of realtime
     * to avoid dropping the local client while debugging.
     * 
     * When a client is normally dropped, the client_t goes into a zombie state
     * for a few seconds to make sure any final reliable message gets resent if
     * necessary.
     */
    public static void SV_CheckTimeouts() {

        int droppoint = (int) (SV_INIT.svs.realtime - 1000 * SV_MAIN.timeout.value);
        int zombiepoint = (int) (SV_INIT.svs.realtime - 1000 * SV_MAIN.zombietime.value);

        for (int i = 0; i < SV_MAIN.maxclients.value; i++) {
            client_t cl = SV_INIT.svs.clients[i];

            if (cl.lastmessage > SV_INIT.svs.realtime)
                cl.lastmessage = SV_INIT.svs.realtime;

            if (cl.state == Defines.cs_zombie && cl.lastmessage < zombiepoint) {
                cl.state = Defines.cs_free; 
                continue;
            }
            if ((cl.state == Defines.cs_connected || cl.state == Defines.cs_spawned)
                    && cl.lastmessage < droppoint) {
                SV_SEND.SV_BroadcastPrintf(Defines.PRINT_HIGH, cl.name
                        + " timed out\n");
                SV_DropClient(cl);
                cl.state = Defines.cs_free; 
            }
        }
    }

    /**
     * SV_PrepWorldFrame
     * 
     * This has to be done before the world logic, because player processing
     * happens outside RunWorldFrame.
     */
    public static void SV_PrepWorldFrame() {

        for (int i = 0; i < GameBase.num_edicts; i++) {
            edict_t ent = GameBase.g_edicts[i];

            ent.s.event = 0;
        }

    }

    /**
     * SV_RunGameFrame.
     */
    public static void SV_RunGameFrame() {
        if (Globals.host_speeds.value != 0)
            Globals.time_before_game = Timer.Milliseconds();

        
        
        
        
        SV_INIT.sv.framenum++;
        SV_INIT.sv.time = SV_INIT.sv.framenum * 100;

        
        if (0 == SV_MAIN.sv_paused.value || SV_MAIN.maxclients.value > 1) {
            GameBase.G_RunFrame();

            
            if (SV_INIT.sv.time < SV_INIT.svs.realtime) {
                if (SV_MAIN.sv_showclamp.value != 0)
                    Com.Printf("sv highclamp\n");
                SV_INIT.svs.realtime = SV_INIT.sv.time;
            }
        }

        if (Globals.host_speeds.value != 0)
            Globals.time_after_game = Timer.Milliseconds();

    }

    /**
     * SV_Frame.
     */
    public static void SV_Frame(long msec) {
        Globals.time_before_game = Globals.time_after_game = 0;

        
        if (!SV_INIT.svs.initialized)
            return;

        SV_INIT.svs.realtime += msec;

        
        Lib.rand();

        
        SV_CheckTimeouts();

        
        SV_ReadPackets();

        
        

        
        if (0 == SV_MAIN.sv_timedemo.value
                && SV_INIT.svs.realtime < SV_INIT.sv.time) {
            
            if (SV_INIT.sv.time - SV_INIT.svs.realtime > 100) {
                if (SV_MAIN.sv_showclamp.value != 0)
                    Com.Printf("sv lowclamp\n");
                SV_INIT.svs.realtime = SV_INIT.sv.time - 100;
            }
            NET.Sleep(SV_INIT.sv.time - SV_INIT.svs.realtime);
            return;
        }

        
        SV_CalcPings();

        
        SV_GiveMsec();

        
        SV_RunGameFrame();

        
        SV_SEND.SV_SendClientMessages();

        
        SV_ENTS.SV_RecordDemoMessage();

        
        Master_Heartbeat();

        
        SV_PrepWorldFrame();

    }

    public static void Master_Heartbeat() {


        if (Globals.dedicated == null || 0 == Globals.dedicated.value)
            return; 

        
        if (null == SV_MAIN.public_server || 0 == SV_MAIN.public_server.value)
            return; 

        
        if (SV_INIT.svs.last_heartbeat > SV_INIT.svs.realtime)
            SV_INIT.svs.last_heartbeat = SV_INIT.svs.realtime;

        if (SV_INIT.svs.realtime - SV_INIT.svs.last_heartbeat < SV_MAIN.HEARTBEAT_SECONDS * 1000)
            return; 

        SV_INIT.svs.last_heartbeat = SV_INIT.svs.realtime;


        String string = SV_StatusString();

        
        for (int i = 0; i < Defines.MAX_MASTERS; i++)
            if (SV_MAIN.master_adr[i].port != 0) {
                Com.Printf("Sending heartbeat to "
                        + NET.AdrToString(SV_MAIN.master_adr[i]) + '\n');
                Netchan.OutOfBandPrint(Defines.NS_SERVER,
                        SV_MAIN.master_adr[i], "heartbeat\n" + string);
            }
    }
    

    /**
     * Master_Shutdown, Informs all masters that this server is going down.
     */
    public static void Master_Shutdown() {


        if (null == Globals.dedicated || 0 == Globals.dedicated.value)
            return; 

        
        if (null == SV_MAIN.public_server || 0 == SV_MAIN.public_server.value)
            return; 

        
        for (int i = 0; i < Defines.MAX_MASTERS; i++)
            if (SV_MAIN.master_adr[i].port != 0) {
                if (i > 0)
                    Com.Printf("Sending heartbeat to "
                            + NET.AdrToString(SV_MAIN.master_adr[i]) + '\n');
                Netchan.OutOfBandPrint(Defines.NS_SERVER,
                        SV_MAIN.master_adr[i], "shutdown");
            }
    }
    

    /**
     * Pull specific info from a newly changed userinfo string into a more C
     * freindly form.
     */
    public static void SV_UserinfoChanged(client_t cl) {


        PlayerClient.ClientUserinfoChanged(cl.edict, cl.userinfo);

        
        cl.name = Info.Info_ValueForKey(cl.userinfo, "name");


        String val = Info.Info_ValueForKey(cl.userinfo, "rate");
        if (val.length() > 0) {
            int i = Lib.atoi(val);
            cl.rate = i;
            if (cl.rate < 100)
                cl.rate = 100;
            if (cl.rate > 15000)
                cl.rate = 15000;
        } else
            cl.rate = 5000;

        
        val = Info.Info_ValueForKey(cl.userinfo, "msg");
        if (val.length() > 0) {
            cl.messagelevel = Lib.atoi(val);
        }

    }

    /**
     * Only called at quake2.exe startup, not for each game
     */
    public static void SV_Init() {
        SV_CCMDS.SV_InitOperatorCommands(); 

        SV_MAIN.rcon_password = Cvar.Get("rcon_password", "", 0);
        Cvar.Get("skill", "1", 0);
        Cvar.Get("deathmatch", "0", Defines.CVAR_LATCH);
        Cvar.Get("coop", "0", Defines.CVAR_LATCH);
        Cvar.Get("dmflags", "" + Defines.DF_INSTANT_ITEMS,
                Defines.CVAR_SERVERINFO);
        Cvar.Get("fraglimit", "0", Defines.CVAR_SERVERINFO);
        Cvar.Get("timelimit", "0", Defines.CVAR_SERVERINFO);
        Cvar.Get("cheats", "0", Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        Cvar.Get("protocol", "" + Defines.PROTOCOL_VERSION,
                Defines.CVAR_SERVERINFO | Defines.CVAR_NOSET);

        SV_MAIN.maxclients = Cvar.Get("maxclients", "1",
                Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        SV_MAIN.hostname = Cvar.Get("hostname", "noname",
                Defines.CVAR_SERVERINFO | Defines.CVAR_ARCHIVE);
        SV_MAIN.timeout = Cvar.Get("timeout", "125", 0);
        SV_MAIN.zombietime = Cvar.Get("zombietime", "2", 0);
        SV_MAIN.sv_showclamp = Cvar.Get("showclamp", "0", 0);
        SV_MAIN.sv_paused = Cvar.Get("paused", "0", 0);
        SV_MAIN.sv_timedemo = Cvar.Get("timedemo", "0", 0);
        SV_MAIN.sv_enforcetime = Cvar.Get("sv_enforcetime", "0", 0);

        SV_MAIN.allow_download = Cvar.Get("allow_download", "1",
                Defines.CVAR_ARCHIVE);
        SV_MAIN.allow_download_players = Cvar.Get("allow_download_players",
                "0", Defines.CVAR_ARCHIVE);
        SV_MAIN.allow_download_models = Cvar.Get("allow_download_models", "1",
                Defines.CVAR_ARCHIVE);
        SV_MAIN.allow_download_sounds = Cvar.Get("allow_download_sounds", "1",
                Defines.CVAR_ARCHIVE);
        SV_MAIN.allow_download_maps = Cvar.Get("allow_download_maps", "1",
                Defines.CVAR_ARCHIVE);

        SV_MAIN.sv_noreload = Cvar.Get("sv_noreload", "0", 0);
        SV_MAIN.sv_airaccelerate = Cvar.Get("sv_airaccelerate", "0",
                Defines.CVAR_LATCH);
        SV_MAIN.public_server = Cvar.Get("public", "0", 0);
        SV_MAIN.sv_reconnect_limit = Cvar.Get("sv_reconnect_limit", "3",
                Defines.CVAR_ARCHIVE);

        SZ.Init(Globals.net_message, Globals.net_message_buffer,
                Globals.net_message_buffer.length);
    }

    /**
     * Used by SV_Shutdown to send a final message to all connected clients
     * before the server goes down. The messages are sent immediately, not just
     * stuck on the outgoing message list, because the server is going to
     * totally exit after returning from this function.
     */
    public static void SV_FinalMessage(String message, boolean reconnect) {

        SZ.Clear(Globals.net_message);
        MSG.WriteByte(Globals.net_message, Defines.svc_print);
        MSG.WriteByte(Globals.net_message, Defines.PRINT_HIGH);
        MSG.WriteString(Globals.net_message, message);

        if (reconnect)
            MSG.WriteByte(Globals.net_message, Defines.svc_reconnect);
        else
            MSG.WriteByte(Globals.net_message, Defines.svc_disconnect);


        client_t cl;
        int i;
        for (i = 0; i < SV_INIT.svs.clients.length; i++) {
            cl = SV_INIT.svs.clients[i];
            if (cl.state >= Defines.cs_connected)
                Netchan.Transmit(cl.netchan, Globals.net_message.cursize,
                        Globals.net_message.data);
        }
        for (i = 0; i < SV_INIT.svs.clients.length; i++) {
            cl = SV_INIT.svs.clients[i];
            if (cl.state >= Defines.cs_connected)
                Netchan.Transmit(cl.netchan, Globals.net_message.cursize,
                        Globals.net_message.data);
        }
    }

    /**
     * Called when each game quits, before Sys_Quit or Sys_Error.
     */
    public static void SV_Shutdown(String finalmsg, boolean reconnect) {
        if (SV_INIT.svs.clients != null)
            SV_FinalMessage(finalmsg, reconnect);

        Master_Shutdown();

        SV_GAME.SV_ShutdownGameProgs();

        
        if (SV_INIT.sv.demofile != null)
            try {
                SV_INIT.sv.demofile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        SV_INIT.sv = new server_t();

        Globals.server_state = SV_INIT.sv.state;

        if (SV_INIT.svs.demofile != null)
            try {
                SV_INIT.svs.demofile.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        SV_INIT.svs = new server_static_t();
    }
}