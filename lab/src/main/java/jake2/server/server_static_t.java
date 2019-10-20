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
import jake2.game.entity_state_t;
import jake2.qcommon.sizebuf_t;

import java.io.RandomAccessFile;

public class server_static_t {
    public server_static_t() {
        for (var n = 0; n < Defines.MAX_CHALLENGES; n++) {
            challenges[n] = new challenge_t();
        }
    }

    boolean initialized; 

    int realtime; 

    String mapcmd = ""; 

    int spawncount;


    client_t[] clients;

    int num_client_entities; 

    int next_client_entities;

    entity_state_t[] client_entities;

    int last_heartbeat;

    final challenge_t[] challenges = new challenge_t[Defines.MAX_CHALLENGES]; 
                                                                        
                                                                        
                                                                        
                                                                        
                                                                        

    
    RandomAccessFile demofile;

    final sizebuf_t demo_multicast = new sizebuf_t();

    final byte[] demo_multicast_buf = new byte[Defines.MAX_MSGLEN];
}