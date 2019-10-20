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




package jake2.render;

import jake2.Defines;
import jake2.game.cplane_t;
import jake2.qcommon.qfiles;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.Arrays;

public class model_t implements Cloneable {
	
	public String name = "";

	public int registration_sequence;

	
	public int type;
	public int numframes;

	public int flags;

	
	
	
	public float[] mins = {(float) 0, (float) 0, (float) 0};
    public float[] maxs = {(float) 0, (float) 0, (float) 0};
	public float radius;

	
	
	
	public boolean clipbox;
    public float[] clipmins = {(float) 0, (float) 0, (float) 0};
    public float[] clipmaxs = {(float) 0, (float) 0, (float) 0};

	
	
	
	public int firstmodelsurface;
    public int nummodelsurfaces;
	public int lightmap; 

	public int numsubmodels;
    public mmodel_t[] submodels;

	public int numplanes;
    public cplane_t[] planes;

	public int numleafs;
    public mleaf_t[] leafs;

	public int numvertexes;
    public mvertex_t[] vertexes;

	public int numedges;
    public medge_t[] edges;

	public int numnodes;
	public int firstnode;
    public mnode_t[] nodes;

	public int numtexinfo;
    public mtexinfo_t[] texinfo;

	public int numsurfaces;
    public msurface_t[] surfaces;

	public int numsurfedges;
    public int[] surfedges;

	public int nummarksurfaces;
    public msurface_t[] marksurfaces;

	public qfiles.dvis_t vis;

    public byte[] lightdata;

	
	
	public final image_t[] skins = new image_t[Defines.MAX_MD2SKINS];

	public int extradatasize;

	
	public Object extradata;
	
	public void clear() {
		name = "";
		registration_sequence = 0;

		
		type = 0;
		numframes = 0;
		flags = 0;

		
		
		
		Math3D.VectorClear(mins);
		Math3D.VectorClear(maxs);
		radius = (float) 0;

		
		
		
		clipbox = false;
		Math3D.VectorClear(clipmins);
		Math3D.VectorClear(clipmaxs);

		
		
		
		firstmodelsurface = nummodelsurfaces = 0;
		lightmap = 0; 

		numsubmodels = 0;
		submodels = null;

		numplanes = 0;
		planes = null;

		numleafs = 0; 
		leafs = null;

		numvertexes = 0;
		vertexes = null;

		numedges = 0;
		edges = null;

		numnodes = 0;
		firstnode = 0;
		nodes = null;

		numtexinfo = 0;
		texinfo = null;

		numsurfaces = 0;
		surfaces = null;

		numsurfedges = 0;
		surfedges = null;

		nummarksurfaces = 0;
		marksurfaces = null;

		vis = null;

		lightdata = null;

		
		
		Arrays.fill(skins, null);

		extradatasize = 0;
		
		extradata = null;
	}
	
	
	public model_t copy() {
		model_t theClone = null;
		try
		{
			theClone = (model_t)super.clone();
			theClone.mins = Lib.clone(this.mins);
			theClone.maxs = Lib.clone(this.maxs);
			theClone.clipmins = Lib.clone(this.clipmins);
			theClone.clipmaxs = Lib.clone(this.clipmaxs);
			
		}
		catch (CloneNotSupportedException e)
		{
		}
		return theClone;
	}
}
