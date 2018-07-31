package jcog.signal.wave2d.vectorize;

import java.util.ArrayList;
import java.util.HashMap;

public class SVGUtils {
	



	
	
	
	
	

	public static float roundtodec(float val, int places){
		float scale = (float) Math.pow(10, places);
		return Math.round(val * scale) / scale;
	}


	
	public static void svgpathstring (StringBuilder sb, String desc, ArrayList<Double[]> segments, String colorstr, HashMap<String,Float> options){
		float scale = options.get("scale"), lcpr = options.get("lcpr"), qcpr = options.get("qcpr");
		int roundcoords = (int) Math.floor(options.get("roundcoords"));
		
		sb.append("<path ").append(desc).append(colorstr).append("d=\"" ).append("M ").append(segments.get(0)[1]*scale).append(' ').append(segments.get(0)[2]*scale).append(' ');

		if( roundcoords == -1 ){
			for(int pcnt=0;pcnt<segments.size();pcnt++){
				if(segments.get(pcnt)[0]==1.0){
					sb.append("L ").append(segments.get(pcnt)[3]*scale).append(' ').append(segments.get(pcnt)[4]*scale).append(' ');
				}else{
					sb.append("Q ").append(segments.get(pcnt)[3]*scale).append(' ').append(segments.get(pcnt)[4]*scale).append(' ').append(segments.get(pcnt)[5]*scale).append(' ').append(segments.get(pcnt)[6]*scale).append(' ');
				}
			}
		}else{
			for(int pcnt=0;pcnt<segments.size();pcnt++){
				if(segments.get(pcnt)[0]==1.0){
					sb.append("L ").append(roundtodec((float)(segments.get(pcnt)[3]*scale),roundcoords)).append(' ')
					.append(roundtodec((float)(segments.get(pcnt)[4]*scale),roundcoords)).append(' ');
				}else{
					sb.append("Q ").append(roundtodec((float)(segments.get(pcnt)[3]*scale),roundcoords)).append(' ')
					.append(roundtodec((float)(segments.get(pcnt)[4]*scale),roundcoords)).append(' ')
					.append(roundtodec((float)(segments.get(pcnt)[5]*scale),roundcoords)).append(' ')
					.append(roundtodec((float)(segments.get(pcnt)[6]*scale),roundcoords)).append(' ');
				}
			}
		}

		sb.append("Z\" />");

		
		for(int pcnt=0;pcnt<segments.size();pcnt++){
			if((lcpr>0)&&(segments.get(pcnt)[0]==1.0)){
				sb.append( "<circle cx=\"").append(segments.get(pcnt)[3]*scale).append("\" cy=\"").append(segments.get(pcnt)[4]*scale).append("\" r=\"").append(lcpr).append("\" fill=\"white\" stroke-width=\"").append(lcpr*0.2).append("\" stroke=\"black\" />");
			}
			if((qcpr>0)&&(segments.get(pcnt)[0]==2.0)){
				sb.append( "<circle cx=\"").append(segments.get(pcnt)[3]*scale).append("\" cy=\"").append(segments.get(pcnt)[4]*scale).append("\" r=\"").append(qcpr).append("\" fill=\"cyan\" stroke-width=\"").append(qcpr*0.2).append("\" stroke=\"black\" />");
				sb.append( "<circle cx=\"").append(segments.get(pcnt)[5]*scale).append("\" cy=\"").append(segments.get(pcnt)[6]*scale).append("\" r=\"").append(qcpr).append("\" fill=\"white\" stroke-width=\"").append(qcpr*0.2).append("\" stroke=\"black\" />");
				sb.append( "<line x1=\"").append(segments.get(pcnt)[1]*scale).append("\" y1=\"").append(segments.get(pcnt)[2]*scale).append("\" x2=\"").append(segments.get(pcnt)[3]*scale).append("\" y2=\"").append(segments.get(pcnt)[4]*scale).append("\" stroke-width=\"").append(qcpr*0.2).append("\" stroke=\"cyan\" />");
				sb.append( "<line x1=\"").append(segments.get(pcnt)[3]*scale).append("\" y1=\"").append(segments.get(pcnt)[4]*scale).append("\" x2=\"").append(segments.get(pcnt)[5]*scale).append("\" y2=\"").append(segments.get(pcnt)[6]*scale).append("\" stroke-width=\"").append(qcpr*0.2).append("\" stroke=\"cyan\" />");
			}
		}

	}


	

	
	


	static String tosvgcolorstr (byte[] c){
		return "fill=\"rgb("+(c[0]+128)+ ',' +(c[1]+128)+ ',' +(c[2]+128)+")\" stroke=\"rgb("+(c[0]+128)+ ',' +(c[1]+128)+ ',' +(c[2]+128)+")\" stroke-width=\"1\" opacity=\""+((c[3]+128)/255.0)+"\" ";
	}


}
