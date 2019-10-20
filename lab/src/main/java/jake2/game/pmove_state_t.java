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

import jake2.qcommon.Com;
import jake2.util.Math3D;

import java.io.IOException;
import java.io.RandomAccessFile;

public class pmove_state_t {
	
	
	
	
	

	public int pm_type;

	public final short[] origin = {(short) 0, (short) 0, (short) 0};
	public final short[] velocity = {(short) 0, (short) 0, (short) 0};
	/** ducked, jump_held, etc. */
	public byte pm_flags;
	/** each unit = 8 ms. */
	public byte pm_time; 
	public short gravity;
	/** add to command angles to get view direction. */
	public final short[] delta_angles = {(short) 0, (short) 0, (short) 0};
	/** changed by spawns, rotating objects, and teleporters.*/
	
	private static final pmove_state_t prototype = new pmove_state_t();
	
	public void clear()
	{
		this.set(prototype);
	}
	 
	public void set(pmove_state_t from) {
		pm_type = from.pm_type;
		Math3D.VectorCopy(from.origin, origin);
		Math3D.VectorCopy(from.velocity, velocity);
		pm_flags = from.pm_flags;
		pm_time = from.pm_time;
		gravity = from.gravity;
		Math3D.VectorCopy(from.delta_angles, delta_angles);
	}
	
	public boolean equals(pmove_state_t p2) {
		return pm_type == p2.pm_type
				&& (int) origin[0] == (int) p2.origin[0]
				&& (int) origin[1] == (int) p2.origin[1]
				&& (int) origin[2] == (int) p2.origin[2]
				&& (int) velocity[0] == (int) p2.velocity[0]
				&& (int) velocity[1] == (int) p2.velocity[1]
				&& (int) velocity[2] == (int) p2.origin[2]
				&& (int) pm_flags == (int) p2.pm_flags
				&& (int) pm_time == (int) p2.pm_time
				&& (int) gravity == (int) gravity
				&& (int) delta_angles[0] == (int) p2.delta_angles[0]
				&& (int) delta_angles[1] == (int) p2.delta_angles[1]
				&& (int) delta_angles[2] == (int) p2.origin[2];

	}

	/** Reads the playermove from the file.*/
	public void load(RandomAccessFile f) throws IOException {

		pm_type = f.readInt();

		origin[0] = f.readShort();
		origin[1] = f.readShort();
		origin[2] = f.readShort();

		velocity[0] = f.readShort();
		velocity[1] = f.readShort();
		velocity[2] = f.readShort();

		pm_flags = f.readByte();
		pm_time = f.readByte();
		gravity = f.readShort();

		f.readShort();

		delta_angles[0] = f.readShort();
		delta_angles[1] = f.readShort();
		delta_angles[2] = f.readShort();

	}
	
	/** Writes the playermove to the file. */
	public void write (RandomAccessFile f) throws IOException {

		f.writeInt(pm_type);

		f.writeShort((int) origin[0]);
		f.writeShort((int) origin[1]);
		f.writeShort((int) origin[2]);

		f.writeShort((int) velocity[0]);
		f.writeShort((int) velocity[1]);
		f.writeShort((int) velocity[2]);

		f.writeByte((int) pm_flags);
		f.writeByte((int) pm_time);
		f.writeShort((int) gravity);

		f.writeShort(0);

		f.writeShort((int) delta_angles[0]);
		f.writeShort((int) delta_angles[1]);
		f.writeShort((int) delta_angles[2]);
	}

	public void dump() {
		Com.Println("pm_type: " + pm_type);

		Com.Println("origin[0]: " + origin[0]);
		Com.Println("origin[1]: " + origin[0]);
		Com.Println("origin[2]: " + origin[0]);

		Com.Println("velocity[0]: " + velocity[0]);
		Com.Println("velocity[1]: " + velocity[1]);
		Com.Println("velocity[2]: " + velocity[2]);

		Com.Println("pmflags: " + pm_flags);
		Com.Println("pmtime: " + pm_time);
		Com.Println("gravity: " + gravity);

		Com.Println("delta-angle[0]: " + delta_angles[0]);
		Com.Println("delta-angle[1]: " + delta_angles[0]);
		Com.Println("delta-angle[2]: " + delta_angles[0]);
	}
}