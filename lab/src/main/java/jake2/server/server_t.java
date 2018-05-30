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
import jake2.game.cmodel_t;
import jake2.game.entity_state_t;
import jake2.qcommon.sizebuf_t;

import java.io.RandomAccessFile;

public class server_t {

    public server_t() {
        models = new cmodel_t[Defines.MAX_MODELS];
        for (int n = 0; n < Defines.MAX_MODELS; n++)
            models[n] = new cmodel_t();

        for (int n = 0; n < Defines.MAX_EDICTS; n++)
            baselines[n] = new entity_state_t(null);
    }

    int state; 

    boolean attractloop; 
                         

    boolean loadgame; 

    int time; 

    int framenum;

    String name = ""; 

    final cmodel_t[] models;

    final String[] configstrings = new String[Defines.MAX_CONFIGSTRINGS];

    final entity_state_t[] baselines = new entity_state_t[Defines.MAX_EDICTS];

    
    
    final sizebuf_t multicast = new sizebuf_t();

    final byte[] multicast_buf = new byte[Defines.MAX_MSGLEN];

    
    RandomAccessFile demofile;

    boolean timedemo; 
}