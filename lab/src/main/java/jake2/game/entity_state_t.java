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

import jake2.util.Math3D;
import jake2.util.QuakeFile;

import java.io.IOException;

public class entity_state_t implements Cloneable
{
	/** entity_state_t is the information conveyed from the server
		in an update message about entities that the client will
		need to render in some way. */
	public entity_state_t(edict_t ent)
	{
		this.surrounding_ent = ent;
		if (ent != null)
		    number = ent.index;
	}

	/** edict index. TODO: this is critical. The index has to be proper managed. */
	public int number;
	
	public edict_t surrounding_ent;
	public float[] origin = { 0, 0, 0 };
	public float[] angles = { 0, 0, 0 };
	
	/** for lerping. */
	public float[] old_origin = { 0, 0, 0 }; 
	public int modelindex;
	/** weapons, CTF flags, etc. */
	public int modelindex2;
    public int modelindex3;
    public int modelindex4;
	public int frame;
	public int skinnum;
	/** PGM - we're filling it, so it needs to be unsigned. */
	public int effects; 
	public int renderfx;
	public int solid;
	
	
	
	public int sound; 
	public int event; 
	
	

	/** Writes the entity state to the file. */
	public void write(QuakeFile f) throws IOException
	{
		f.writeEdictRef(surrounding_ent);
		f.writeVector(origin);
		f.writeVector(angles);
		f.writeVector(old_origin);
	
		f.writeInt(modelindex); 
		
		f.writeInt(modelindex2);
		f.writeInt(modelindex3);
		f.writeInt(modelindex4);
	
		f.writeInt(frame);	
		f.writeInt(skinnum);
		
		f.writeInt(effects);
		f.writeInt(renderfx);
		f.writeInt(solid);
		
		f.writeInt(sound);
		f.writeInt(event);
		
	}

	/** Reads the entity state from the file. */
	public void read(QuakeFile f) throws IOException
	{
		surrounding_ent = f.readEdictRef();
		origin = f.readVector();
		angles = f.readVector();
		old_origin = f.readVector();
	
		modelindex = f.readInt(); 
		
		modelindex2= f.readInt();
		modelindex3= f.readInt();
		modelindex4= f.readInt();
	
		frame = f.readInt();	
		skinnum = f.readInt();
		
		effects = f.readInt();
		renderfx = f.readInt();
		solid = f.readInt();
		
		sound = f.readInt();
		event = f.readInt();
		

	}


	public entity_state_t getClone()
	{
		entity_state_t out = new entity_state_t(this.surrounding_ent);
		out.set(this);
		return out;
	}

	public void set(entity_state_t from)
	{
		Math3D.VectorCopy(from.origin, origin);
		Math3D.VectorCopy(from.angles, angles);
		Math3D.VectorCopy(from.old_origin, old_origin);

		number = from.number;
		modelindex = from.modelindex;
		modelindex2 = from.modelindex2;
		modelindex3 = from.modelindex3;
		modelindex4 = from.modelindex4;

		frame = from.frame;
		skinnum = from.skinnum;
		effects = from.effects;
		renderfx = from.renderfx;
		solid = from.solid;
		sound = from.sound;
		event = from.event;
	}

	public void clear()
	{
	    
		number = 0;
		surrounding_ent = null;
		Math3D.VectorClear(origin);
		Math3D.VectorClear(angles);
		Math3D.VectorClear(old_origin);
		modelindex = 0;
		modelindex2 = modelindex3 = modelindex4 = 0; 
		frame = 0;
		skinnum = 0;
		effects = 0; 
		renderfx = 0;
		solid = 0;
		sound = 0;
		event = 0;
	}
}