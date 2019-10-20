package jcog.signal.wave2d.vectorize;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VectorizingUtils {

	
	
	
	
	
	

	
	
	public static ImageTracer.IndexedImage colorquantization (ImageTracer.ImageData imgd, byte [][] palette, HashMap<String,Float> options){
		
		
		if( options.get("blurradius") > (float) 0){ imgd = SelectiveBlur.blur( imgd, options.get("blurradius"), options.get("blurdelta") ); }

        int cycles = (int)Math.floor(options.get("colorquantcycles"));

        int[][] arr = new int[imgd.height+2][imgd.width+2];
		for(int j = 0; j<(imgd.height+2); j++){ arr[j][0] = -1; arr[j][imgd.width+1 ] = -1; }
		for(int i = 0; i<(imgd.width+2) ; i++){ arr[0][i] = -1; arr[imgd.height+1][i] = -1; }

        int idx=0;


        byte[][] original_palette_backup = palette;
        long[][] paletteacc = new long[palette.length][5];

		
		for(int cnt = 0; cnt<cycles; cnt++){

			
			if(cnt>0){
				
				
				for(int k = 0; k<palette.length; k++){
					
					if(paletteacc[k][3]> 0L){
						palette[k][0] = (byte) (-128L + (paletteacc[k][0] / paletteacc[k][4]));
						palette[k][1] = (byte) (-128L + (paletteacc[k][1] / paletteacc[k][4]));
						palette[k][2] = (byte) (-128L + (paletteacc[k][2] / paletteacc[k][4]));
						palette[k][3] = (byte) (-128L + (paletteacc[k][3] / paletteacc[k][4]));
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

			
			for(int i = 0; i<palette.length; i++){
				paletteacc[i][0]= 0L;
				paletteacc[i][1]= 0L;
				paletteacc[i][2]= 0L;
				paletteacc[i][3]= 0L;
				paletteacc[i][4]= 0L;
			}

			
			for(int j = 0; j<imgd.height; j++){
				for(int i = 0; i<imgd.width; i++){

					idx = ((j*imgd.width)+i)*4;


                    int cdl = 256 + 256 + 256 + 256;
                    int ci = 0;
                    for(int k = 0; k<original_palette_backup.length; k++){


                        int c1 = Math.abs((int) original_palette_backup[k][0] - (int) imgd.data[idx]);
                        int c2 = Math.abs((int) original_palette_backup[k][1] - (int) imgd.data[idx + 1]);
                        int c3 = Math.abs((int) original_palette_backup[k][2] - (int) imgd.data[idx + 2]);
                        int c4 = Math.abs((int) original_palette_backup[k][3] - (int) imgd.data[idx + 3]);
                        int cd = c1 + c2 + c3 + (c4 * 4);


                        if(cd<cdl){ cdl = cd; ci = k; }

					}


					paletteacc[ci][0] = paletteacc[ci][0] + (long) 128 + (int) imgd.data[idx];
					paletteacc[ci][1] = paletteacc[ci][1] + (long) 128 + (int) imgd.data[idx + 1];
					paletteacc[ci][2] = paletteacc[ci][2] + (long) 128 + (int) imgd.data[idx + 2];
					paletteacc[ci][3] = paletteacc[ci][3] + (long) 128 + (int) imgd.data[idx + 3];
					paletteacc[ci][4]++;

					arr[j+1][i+1] = ci;
				}
			}

		}

		return new ImageTracer.IndexedImage(arr, original_palette_backup);
	}

	
	
	
	
	
	
	public static int[][][] layering (ImageTracer.IndexedImage ii){
		
		int val=0, aw = ii.array[0].length, ah = ii.array.length;

        int[][][] layers = new int[ii.palette.length][ah][aw];

		
		for(int j = 1; j<(ah-1); j++){
			for(int i = 1; i<(aw-1); i++){

				
				val = ii.array[j][i];


                int n1 = ii.array[j - 1][i - 1] == val ? 1 : 0;
                int n2 = ii.array[j - 1][i] == val ? 1 : 0;
                int n3 = ii.array[j - 1][i + 1] == val ? 1 : 0;
                int n4 = ii.array[j][i - 1] == val ? 1 : 0;
                int n5 = ii.array[j][i + 1] == val ? 1 : 0;
                int n6 = ii.array[j + 1][i - 1] == val ? 1 : 0;
                int n7 = ii.array[j + 1][i] == val ? 1 : 0;
                int n8 = ii.array[j + 1][i + 1] == val ? 1 : 0;


                layers[val][j+1][i+1] = 1 + (n5 * 2) + (n8 * 4) + (n7 * 8) ;
				if(n4==0){ layers[val][j+1][i  ] = 2 + (n7 * 4) + (n6 * 8); }
				if(n2==0){ layers[val][j  ][i+1] = (n3 * 2) + (n5 * 4) + 8; }
				if(n1==0){ layers[val][j  ][i  ] = (n2 * 2) + 4 + (n4 * 8); }

			}
		}

		return layers;
	}


	
	static final byte [] pathscan_dir_lookup = {(byte) 0, (byte) 0, (byte) 3, (byte) 0, (byte) 1, (byte) 0, (byte) 3, (byte) 0, (byte) 0, (byte) 3, (byte) 3, (byte) 1, (byte) 0, (byte) 3, (byte) 0, (byte) 0};
	static final boolean [] pathscan_holepath_lookup = {false,false,false,false, false,false,false,true, false,false,false,true, false,true,true,false };
	
	static final byte [][][] pathscan_combined_lookup = {
			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}},
			{{(byte) 0, (byte) 1, (byte) 0, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 2, (byte) -1, (byte) 0}},
			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 1, (byte) 0, (byte) -1}, {(byte) 0, (byte) 0, (byte) 1, (byte) 0}},
			{{(byte) 0, (byte) 0, (byte) 1, (byte) 0}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 2, (byte) -1, (byte) 0}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}},

			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 0, (byte) 1, (byte) 0}, {(byte) 0, (byte) 3, (byte) 0, (byte) 1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}},
			{{(byte) 13, (byte) 3, (byte) 0, (byte) 1}, {(byte) 13, (byte) 2, (byte) -1, (byte) 0}, {(byte) 7, (byte) 1, (byte) 0, (byte) -1}, {(byte) 7, (byte) 0, (byte) 1, (byte) 0}},
			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 1, (byte) 0, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 3, (byte) 0, (byte) 1}},
			{{(byte) 0, (byte) 3, (byte) 0, (byte) 1}, {(byte) 0, (byte) 2, (byte) -1, (byte) 0}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}},

			{{(byte) 0, (byte) 3, (byte) 0, (byte) 1}, {(byte) 0, (byte) 2, (byte) -1, (byte) 0}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}},
			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 1, (byte) 0, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 3, (byte) 0, (byte) 1}},
			{{(byte) 11, (byte) 1, (byte) 0, (byte) -1}, {(byte) 14, (byte) 0, (byte) 1, (byte) 0}, {(byte) 14, (byte) 3, (byte) 0, (byte) 1}, {(byte) 11, (byte) 2, (byte) -1, (byte) 0}},
			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 0, (byte) 1, (byte) 0}, {(byte) 0, (byte) 3, (byte) 0, (byte) 1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}},

			{{(byte) 0, (byte) 0, (byte) 1, (byte) 0}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 2, (byte) -1, (byte) 0}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}},
			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 1, (byte) 0, (byte) -1}, {(byte) 0, (byte) 0, (byte) 1, (byte) 0}},
			{{(byte) 0, (byte) 1, (byte) 0, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) 0, (byte) 2, (byte) -1, (byte) 0}},
			{{(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}, {(byte) -1, (byte) -1, (byte) -1, (byte) -1}}
	};


	
    
	
	
	
	
	
	
	
	public static ArrayList<ArrayList<Integer[]>> pathscan (int [][] arr,float pathomit){
        ArrayList<ArrayList<Integer[]>> paths = new ArrayList<ArrayList<Integer[]>>();
        int px=0,py=0,w=arr[0].length,h=arr.length,dir=0;
		boolean pathfinished=true, holepath = false;

        for(int j = 0; j<h; j++){
			for(int i = 0; i<w; i++){
				if((arr[j][i]!=0)&&(arr[j][i]!=15)){

					
					px = i; py = j;
                    ArrayList<Integer[]> thispath;
                    paths.add(thispath = new ArrayList<>());
					pathfinished = false;

					
					dir = (int) pathscan_dir_lookup[arr[py][px]]; holepath = pathscan_holepath_lookup[ arr[py][px] ];

					
					while(!pathfinished){

						
						thispath.add(new Integer[] { px-1, py-1, arr[py][px]});


                        byte[] lookuprow = pathscan_combined_lookup[arr[py][px]][dir];
                        arr[py][px] = (int) lookuprow[0]; dir = (int) lookuprow[1];
						px = px + (int) lookuprow[2];
						py = py + (int) lookuprow[3];

						
						if(((px-1)==thispath.get(0)[0])&&((py-1)==thispath.get(0)[1])){
							pathfinished = true;
							
							if( (holepath) || ((float) thispath.size() <pathomit) ){
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
		ArrayList<ArrayList<ArrayList<Integer[]>>> bpaths = new ArrayList<>();
		for (int[][] layer : layers) {
			ArrayList<ArrayList<Integer[]>> pathscan = pathscan(layer, pathomit);
			bpaths.add(pathscan);
		}
		return bpaths;
	}


	
	public static ArrayList<ArrayList<Double[]>> internodes (List<ArrayList<Integer[]>> paths){
        ArrayList<ArrayList<Double[]>> ins = new ArrayList<ArrayList<Double[]>>();
        Double[] nextpoint = new Double[2];
        int palen=0,nextidx=0,nextidx2=0;


		for (ArrayList<Integer[]> path : paths) {
			ins.add(new ArrayList<>());
            ArrayList<Double[]> thisinp = ins.get(ins.size() - 1);
            palen = path.size();

			for (int pcnt = 0; pcnt < palen; pcnt++) {


				nextidx = (pcnt + 1) % palen;
				nextidx2 = (pcnt + 2) % palen;
				thisinp.add(new Double[3]);
                Double[] thispoint = thisinp.get(thisinp.size() - 1);
                Integer[] pp1 = path.get(pcnt);
                Integer[] pp2 = path.get(nextidx);
                Integer[] pp3 = path.get(nextidx2);
                thispoint[0] = (double) (pp1[0] + pp2[0]) / 2.0;
				thispoint[1] = (double) (pp1[1] + pp2[1]) / 2.0;
				nextpoint[0] = (double) (pp2[0] + pp3[0]) / 2.0;
				nextpoint[1] = (double) (pp2[1] + pp3[1]) / 2.0;


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
		ArrayList<ArrayList<ArrayList<Double[]>>> binternodes = new ArrayList<>();
		for (ArrayList<ArrayList<Integer[]>> bpath : bpaths) {
			ArrayList<ArrayList<Double[]>> internodes = internodes(bpath);
			binternodes.add(internodes);
		}
		return binternodes;
	}


	

	
	
	
	
	
	
	

	
	
	
	
	
	
	

	public static ArrayList<Double[]> tracepath (ArrayList<Double[]> path, float ltreshold, float qtreshold){
		int pcnt=0, seqend=0;
        ArrayList<Double[]> smp = new ArrayList<Double[]>();

        int pathlength = path.size();

		while(pcnt<pathlength){

            double segtype1 = path.get(pcnt)[2];
			seqend=pcnt+1;
			double segtype2 = -1.0;
			while(
					((path.get(seqend)[2]==segtype1) || (path.get(seqend)[2]==segtype2) || (segtype2== -1.0))
					&& (seqend<(pathlength-1))){
				if((path.get(seqend)[2]!=segtype1) && (segtype2== -1.0)){ segtype2 = path.get(seqend)[2];}
				seqend++;
			}
			if(seqend==(pathlength-1)){ seqend = 0; }

			
			smp.addAll(fitseq(path,ltreshold,qtreshold,pcnt,seqend));
			

			
			if(seqend>0){ pcnt = seqend; }else{ pcnt = pathlength; }

		}

		return smp;

	}


	
	
	public static ArrayList<Double[]> fitseq (ArrayList<Double[]> path, float ltreshold, float qtreshold, int seqstart, int seqend){
        ArrayList<Double[]> segment = new ArrayList<Double[]>();
        int pathlength = path.size();

		
		if((seqend>pathlength)||(seqend<0)){return segment;}

        double tl = (double) (seqend - seqstart); if(tl< (double) 0){
			tl = tl + (double) pathlength; }
		double vx = (path.get(seqend)[0]-path.get(seqstart)[0]) / tl,
				vy = (path.get(seqend)[1]-path.get(seqstart)[1]) / tl;


        int pcnt = (seqstart+1)%pathlength;
        double errorval = (double) 0;
        double dist2;
        double py;
        double px;
        boolean curvepass = true;
        int errorpoint = seqstart;
        while(pcnt != seqend){
            double pl = (double) (pcnt - seqstart);
            if(pl< (double) 0){
				pl = pl + (double) pathlength; }
			px = path.get(seqstart)[0] + (vx * pl); py = path.get(seqstart)[1] + (vy * pl);
			dist2 = ((path.get(pcnt)[0]-px)*(path.get(pcnt)[0]-px)) + ((path.get(pcnt)[1]-py)*(path.get(pcnt)[1]-py));
			if(dist2> (double) ltreshold){curvepass=false;}
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


        int fitpoint = errorpoint; curvepass = true; errorval = (double) 0;

		
		
		double t= (double) (fitpoint - seqstart) /tl, t1=(1.0-t)*(1.0-t), t2=2.0*(1.0-t)*t, t3=t*t;
		double cpx = (((t1*path.get(seqstart)[0]) + (t3*path.get(seqend)[0])) - path.get(fitpoint)[0])/-t2 ,
				cpy = (((t1*path.get(seqstart)[1]) + (t3*path.get(seqend)[1])) - path.get(fitpoint)[1])/-t2 ;

		
		pcnt = seqstart+1;
		while(pcnt != seqend){

			t= (double) (pcnt - seqstart) /tl; t1=(1.0-t)*(1.0-t); t2=2.0*(1.0-t)*t; t3=t*t;
			px = (t1 * path.get(seqstart)[0]) + (t2 * cpx) + (t3 * path.get(seqend)[0]);
			py = (t1 * path.get(seqstart)[1]) + (t2 * cpy) + (t3 * path.get(seqend)[1]);

			dist2 = ((path.get(pcnt)[0]-px)*(path.get(pcnt)[0]-px)) + ((path.get(pcnt)[1]-py)*(path.get(pcnt)[1]-py));

			if(dist2> (double) qtreshold){curvepass=false;}
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


        int splitpoint = (fitpoint + errorpoint)/2;

		
		segment = fitseq(path,ltreshold,qtreshold,seqstart,splitpoint);
		segment.addAll(fitseq(path,ltreshold,qtreshold,splitpoint,seqend));
		return segment;

	}


	
	public static ArrayList<ArrayList<Double[]>> batchtracepaths (ArrayList<ArrayList<Double[]>> internodepaths, float ltres,float qtres){
		ArrayList<ArrayList<Double[]>> btracedpaths = new ArrayList<>();
		for (ArrayList<Double[]> internodepath : internodepaths) {
			ArrayList<Double[]> tracepath = tracepath(internodepath, ltres, qtres);
			btracedpaths.add(tracepath);
		}
		return btracedpaths;
	}


	
	public static ArrayList<ArrayList<ArrayList<Double[]>>> batchtracelayers (ArrayList<ArrayList<ArrayList<Double[]>>> binternodes, float ltres, float qtres){
		ArrayList<ArrayList<ArrayList<Double[]>>> btbis = new ArrayList<>();
		for (ArrayList<ArrayList<Double[]>> binternode : binternodes) {
			ArrayList<ArrayList<Double[]>> batchtracepaths = batchtracepaths(binternode, ltres, qtres);
			btbis.add(batchtracepaths);
		}
		return btbis;
	}

	
}
