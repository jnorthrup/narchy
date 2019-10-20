package jcog.signal.wave2d.vectorize;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class VectorizingUtils {

	
	
	
	
	
	

	
	
	public static ImageTracer.IndexedImage colorquantization (ImageTracer.ImageData imgd, byte [][] palette, HashMap<String,Float> options){
		
		
		if( options.get("blurradius") > 0 ){ imgd = SelectiveBlur.blur( imgd, options.get("blurradius"), options.get("blurdelta") ); }

		var cycles = (int)Math.floor(options.get("colorquantcycles"));

		var arr = new int[imgd.height+2][imgd.width+2];
		for(var j = 0; j<(imgd.height+2); j++){ arr[j][0] = -1; arr[j][imgd.width+1 ] = -1; }
		for(var i = 0; i<(imgd.width+2) ; i++){ arr[0][i] = -1; arr[imgd.height+1][i] = -1; }

		var idx=0;


		var original_palette_backup = palette;
		var paletteacc = new long[palette.length][5];

		
		for(var cnt = 0; cnt<cycles; cnt++){

			
			if(cnt>0){
				
				
				for(var k = 0; k<palette.length; k++){
					
					if(paletteacc[k][3]>0){
						palette[k][0] = (byte) (-128 + (paletteacc[k][0] / paletteacc[k][4]));
						palette[k][1] = (byte) (-128 + (paletteacc[k][1] / paletteacc[k][4]));
						palette[k][2] = (byte) (-128 + (paletteacc[k][2] / paletteacc[k][4]));
						palette[k][3] = (byte) (-128 + (paletteacc[k][3] / paletteacc[k][4]));
					}
					

					/*
					if( (ratio<minratio) && (cnt<(cycles-1)) ){
						palette[k][0] = (byte) (-128+Math.floor(Math.random()*255));
						palette[k][1] = (byte) (-128+Math.floor(Math.random()*255));
						palette[k][2] = (byte) (-128+Math.floor(Math.random()*255));
						palette[k][3] = (byte) (-128+Math.floor(Math.random()*255));
					}*/

				}
			}

			
			for(var i = 0; i<palette.length; i++){
				paletteacc[i][0]=0;
				paletteacc[i][1]=0;
				paletteacc[i][2]=0;
				paletteacc[i][3]=0;
				paletteacc[i][4]=0;
			}

			
			for(var j = 0; j<imgd.height; j++){
				for(var i = 0; i<imgd.width; i++){

					idx = ((j*imgd.width)+i)*4;


					var cdl = 256 + 256 + 256 + 256;
					var ci = 0;
                    for(var k = 0; k<original_palette_backup.length; k++){


						var c1 = Math.abs(original_palette_backup[k][0] - imgd.data[idx]);
						var c2 = Math.abs(original_palette_backup[k][1] - imgd.data[idx + 1]);
						var c3 = Math.abs(original_palette_backup[k][2] - imgd.data[idx + 2]);
						var c4 = Math.abs(original_palette_backup[k][3] - imgd.data[idx + 3]);
						var cd = c1 + c2 + c3 + (c4 * 4);


                        if(cd<cdl){ cdl = cd; ci = k; }

					}

					
					paletteacc[ci][0] += 128+imgd.data[idx];
					paletteacc[ci][1] += 128+imgd.data[idx+1];
					paletteacc[ci][2] += 128+imgd.data[idx+2];
					paletteacc[ci][3] += 128+imgd.data[idx+3];
					paletteacc[ci][4]++;

					arr[j+1][i+1] = ci;
				}
			}

		}

		return new ImageTracer.IndexedImage(arr, original_palette_backup);
	}

	
	
	
	
	
	
	public static int[][][] layering (ImageTracer.IndexedImage ii){
		
		int val=0, aw = ii.array[0].length, ah = ii.array.length;

		var layers = new int[ii.palette.length][ah][aw];

		
		for(var j = 1; j<(ah-1); j++){
			for(var i = 1; i<(aw-1); i++){

				
				val = ii.array[j][i];


				var n1 = ii.array[j - 1][i - 1] == val ? 1 : 0;
				var n2 = ii.array[j - 1][i] == val ? 1 : 0;
				var n3 = ii.array[j - 1][i + 1] == val ? 1 : 0;
				var n4 = ii.array[j][i - 1] == val ? 1 : 0;
				var n5 = ii.array[j][i + 1] == val ? 1 : 0;
				var n6 = ii.array[j + 1][i - 1] == val ? 1 : 0;
				var n7 = ii.array[j + 1][i] == val ? 1 : 0;
				var n8 = ii.array[j + 1][i + 1] == val ? 1 : 0;


                layers[val][j+1][i+1] = 1 + (n5 * 2) + (n8 * 4) + (n7 * 8) ;
				if(n4==0){ layers[val][j+1][i  ] = 2 + (n7 * 4) + (n6 * 8); }
				if(n2==0){ layers[val][j  ][i+1] = (n3 * 2) + (n5 * 4) + 8; }
				if(n1==0){ layers[val][j  ][i  ] = (n2 * 2) + 4 + (n4 * 8); }

			}
		}

		return layers;
	}


	
	static final byte [] pathscan_dir_lookup = {0,0,3,0, 1,0,3,0, 0,3,3,1, 0,3,0,0};
	static final boolean [] pathscan_holepath_lookup = {false,false,false,false, false,false,false,true, false,false,false,true, false,true,true,false };
	
	static final byte [][][] pathscan_combined_lookup = {
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}},
			{{ 0, 1, 0,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 2,-1, 0}},
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 1, 0,-1}, { 0, 0, 1, 0}},
			{{ 0, 0, 1, 0}, {-1,-1,-1,-1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}},

			{{-1,-1,-1,-1}, { 0, 0, 1, 0}, { 0, 3, 0, 1}, {-1,-1,-1,-1}},
			{{13, 3, 0, 1}, {13, 2,-1, 0}, { 7, 1, 0,-1}, { 7, 0, 1, 0}},
			{{-1,-1,-1,-1}, { 0, 1, 0,-1}, {-1,-1,-1,-1}, { 0, 3, 0, 1}},
			{{ 0, 3, 0, 1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}, {-1,-1,-1,-1}},

			{{ 0, 3, 0, 1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}, {-1,-1,-1,-1}},
			{{-1,-1,-1,-1}, { 0, 1, 0,-1}, {-1,-1,-1,-1}, { 0, 3, 0, 1}},
			{{11, 1, 0,-1}, {14, 0, 1, 0}, {14, 3, 0, 1}, {11, 2,-1, 0}},
			{{-1,-1,-1,-1}, { 0, 0, 1, 0}, { 0, 3, 0, 1}, {-1,-1,-1,-1}},

			{{ 0, 0, 1, 0}, {-1,-1,-1,-1}, { 0, 2,-1, 0}, {-1,-1,-1,-1}},
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 1, 0,-1}, { 0, 0, 1, 0}},
			{{ 0, 1, 0,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, { 0, 2,-1, 0}},
			{{-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}, {-1,-1,-1,-1}}
	};


	
    
	
	
	
	
	
	
	
	public static ArrayList<ArrayList<Integer[]>> pathscan (int [][] arr,float pathomit){
		var paths = new ArrayList<ArrayList<Integer[]>>();
        int px=0,py=0,w=arr[0].length,h=arr.length,dir=0;
		boolean pathfinished=true, holepath = false;

        for(var j = 0; j<h; j++){
			for(var i = 0; i<w; i++){
				if((arr[j][i]!=0)&&(arr[j][i]!=15)){

					
					px = i; py = j;
                    ArrayList<Integer[]> thispath;
                    paths.add(thispath = new ArrayList<>());
					pathfinished = false;

					
					dir = pathscan_dir_lookup[ arr[py][px] ]; holepath = pathscan_holepath_lookup[ arr[py][px] ];

					
					while(!pathfinished){

						
						thispath.add(new Integer[] { px-1, py-1, arr[py][px]});


						var lookuprow = pathscan_combined_lookup[arr[py][px]][dir];
                        arr[py][px] = lookuprow[0]; dir = lookuprow[1]; px += lookuprow[2]; py += lookuprow[3];

						
						if(((px-1)==thispath.get(0)[0])&&((py-1)==thispath.get(0)[1])){
							pathfinished = true;
							
							if( (holepath) || (thispath.size()<pathomit) ){
								paths.remove(thispath);
							}
						}

					}

				}

			}
		}

		return paths;
	}


	
	public static ArrayList<ArrayList<ArrayList<Integer[]>>> batchpathscan (int [][][] layers, float pathomit){
		var bpaths = Arrays.stream(layers).map(layer -> pathscan(layer, pathomit)).collect(Collectors.toCollection(ArrayList::new));
		return bpaths;
	}


	
	public static ArrayList<ArrayList<Double[]>> internodes (List<ArrayList<Integer[]>> paths){
		var ins = new ArrayList<ArrayList<Double[]>>();
		var nextpoint = new Double[2];
        int palen=0,nextidx=0,nextidx2=0;


		for (var path : paths) {
			ins.add(new ArrayList<>());
			var thisinp = ins.get(ins.size() - 1);
            palen = path.size();

			for (var pcnt = 0; pcnt < palen; pcnt++) {


				nextidx = (pcnt + 1) % palen;
				nextidx2 = (pcnt + 2) % palen;
				thisinp.add(new Double[3]);
				var thispoint = thisinp.get(thisinp.size() - 1);
				var pp1 = path.get(pcnt);
				var pp2 = path.get(nextidx);
				var pp3 = path.get(nextidx2);
                thispoint[0] = (pp1[0] + pp2[0]) / 2.0;
				thispoint[1] = (pp1[1] + pp2[1]) / 2.0;
				nextpoint[0] = (pp2[0] + pp3[0]) / 2.0;
				nextpoint[1] = (pp2[1] + pp3[1]) / 2.0;


				if (thispoint[0] < nextpoint[0]) {
					if (thispoint[1] < nextpoint[1]) {
						thispoint[2] = 1.0;
					} else if (thispoint[1] > nextpoint[1]) {
						thispoint[2] = 7.0;
					} else {
						thispoint[2] = 0.0;
					}
				} else if (thispoint[0] > nextpoint[0]) {
					if (thispoint[1] < nextpoint[1]) {
						thispoint[2] = 3.0;
					} else if (thispoint[1] > nextpoint[1]) {
						thispoint[2] = 5.0;
					} else {
						thispoint[2] = 4.0;
					}
				} else {
					if (thispoint[1] < nextpoint[1]) {
						thispoint[2] = 2.0;
					} else if (thispoint[1] > nextpoint[1]) {
						thispoint[2] = 6.0;
					} else {
						thispoint[2] = 8.0;
					}
				}

			}
		}
		return ins;
	}


	
	static ArrayList<ArrayList<ArrayList<Double[]>>> batchinternodes (ArrayList<ArrayList<ArrayList<Integer[]>>> bpaths){
		var binternodes = bpaths.stream().map(VectorizingUtils::internodes).collect(Collectors.toCollection(ArrayList::new));
		return binternodes;
	}


	

	
	
	
	
	
	
	

	
	
	
	
	
	
	

	public static ArrayList<Double[]> tracepath (ArrayList<Double[]> path, float ltreshold, float qtreshold){
		int pcnt=0, seqend=0;
		var smp = new ArrayList<Double[]>();

		var pathlength = path.size();

		while(pcnt<pathlength){

            double segtype1 = path.get(pcnt)[2];
			seqend=pcnt+1;
			double segtype2 = -1;
			while(
					((path.get(seqend)[2]==segtype1) || (path.get(seqend)[2]==segtype2) || (segtype2==-1))
					&& (seqend<(pathlength-1))){
				if((path.get(seqend)[2]!=segtype1) && (segtype2==-1)){ segtype2 = path.get(seqend)[2];}
				seqend++;
			}
			if(seqend==(pathlength-1)){ seqend = 0; }

			
			smp.addAll(fitseq(path,ltreshold,qtreshold,pcnt,seqend));
			

			
			if(seqend>0){ pcnt = seqend; }else{ pcnt = pathlength; }

		}

		return smp;

	}


	
	
	public static ArrayList<Double[]> fitseq (ArrayList<Double[]> path, float ltreshold, float qtreshold, int seqstart, int seqend){
		var segment = new ArrayList<Double[]>();
		var pathlength = path.size();

		
		if((seqend>pathlength)||(seqend<0)){return segment;}

        double tl = (seqend-seqstart); if(tl<0){ tl += pathlength; }
		double vx = (path.get(seqend)[0]-path.get(seqstart)[0]) / tl,
				vy = (path.get(seqend)[1]-path.get(seqstart)[1]) / tl;


		var pcnt = (seqstart+1)%pathlength;
        double errorval = 0;
        double dist2;
        double py;
        double px;
		var curvepass = true;
		var errorpoint = seqstart;
        while(pcnt != seqend){
            double pl = pcnt - seqstart;
            if(pl<0){ pl += pathlength; }
			px = path.get(seqstart)[0] + (vx * pl); py = path.get(seqstart)[1] + (vy * pl);
			dist2 = ((path.get(pcnt)[0]-px)*(path.get(pcnt)[0]-px)) + ((path.get(pcnt)[1]-py)*(path.get(pcnt)[1]-py));
			if(dist2>ltreshold){curvepass=false;}
			if(dist2>errorval){ errorpoint=pcnt; errorval=dist2; }
			pcnt = (pcnt+1)%pathlength;
		}


        Double[] thissegment;
        if(curvepass){
			segment.add(new Double[7]);
			thissegment = segment.get(segment.size()-1);
			thissegment[0] = 1.0;
			thissegment[1] = path.get(seqstart)[0];
			thissegment[2] = path.get(seqstart)[1];
			thissegment[3] = path.get(seqend)[0];
			thissegment[4] = path.get(seqend)[1];
			thissegment[5] = 0.0;
			thissegment[6] = 0.0;
			return segment;
		}


		var fitpoint = errorpoint; curvepass = true; errorval = 0;

		
		
		double t=(fitpoint-seqstart)/tl, t1=(1.0-t)*(1.0-t), t2=2.0*(1.0-t)*t, t3=t*t;
		double cpx = (((t1*path.get(seqstart)[0]) + (t3*path.get(seqend)[0])) - path.get(fitpoint)[0])/-t2 ,
				cpy = (((t1*path.get(seqstart)[1]) + (t3*path.get(seqend)[1])) - path.get(fitpoint)[1])/-t2 ;

		
		pcnt = seqstart+1;
		while(pcnt != seqend){

			t=(pcnt-seqstart)/tl; t1=(1.0-t)*(1.0-t); t2=2.0*(1.0-t)*t; t3=t*t;
			px = (t1 * path.get(seqstart)[0]) + (t2 * cpx) + (t3 * path.get(seqend)[0]);
			py = (t1 * path.get(seqstart)[1]) + (t2 * cpy) + (t3 * path.get(seqend)[1]);

			dist2 = ((path.get(pcnt)[0]-px)*(path.get(pcnt)[0]-px)) + ((path.get(pcnt)[1]-py)*(path.get(pcnt)[1]-py));

			if(dist2>qtreshold){curvepass=false;}
			if(dist2>errorval){ errorpoint=pcnt; errorval=dist2; }
			pcnt = (pcnt+1)%pathlength;
		}

		
		if(curvepass){
			segment.add(new Double[7]);
			thissegment = segment.get(segment.size()-1);
			thissegment[0] = 2.0;
			thissegment[1] = path.get(seqstart)[0];
			thissegment[2] = path.get(seqstart)[1];
			thissegment[3] = cpx;
			thissegment[4] = cpy;
			thissegment[5] = path.get(seqend)[0];
			thissegment[6] = path.get(seqend)[1];
			return segment;
		}


		var splitpoint = (fitpoint + errorpoint)/2;

		
		segment = fitseq(path,ltreshold,qtreshold,seqstart,splitpoint);
		segment.addAll(fitseq(path,ltreshold,qtreshold,splitpoint,seqend));
		return segment;

	}


	
	public static ArrayList<ArrayList<Double[]>> batchtracepaths (ArrayList<ArrayList<Double[]>> internodepaths, float ltres,float qtres){
		var btracedpaths = internodepaths.stream().map(internodepath -> tracepath(internodepath, ltres, qtres)).collect(Collectors.toCollection(ArrayList::new));
		return btracedpaths;
	}


	
	public static ArrayList<ArrayList<ArrayList<Double[]>>> batchtracelayers (ArrayList<ArrayList<ArrayList<Double[]>>> binternodes, float ltres, float qtres){
		var btbis = binternodes.stream().map(binternode -> batchtracepaths(binternode, ltres, qtres)).collect(Collectors.toCollection(ArrayList::new));
		return btbis;
	}

	
}
