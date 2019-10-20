/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package jcog.signal.wave1d;

/**
 * Implements a <a
 * href="http://en.wikipedia.org/wiki/Digital_biquad_filter">BiQuad filter</a>,
 * which can be used for e.g. low pass filtering.
 * 
 * The implementation is a translation of biquad.c from Aubio, Copyright (C)
 * 2003-2009 Paul Brossier <piem@aubio.org>
 * 
 * @author Joren Six
 * @author Paul Brossiers
 */
public class BiQuadFilter {

	private double i1;
	private double i2;
	private double o1;
	private double o2;
	private final double a2;
	private final double a3;
	private final double b1;
	private final double b2;
	private final double b3;

	public BiQuadFilter(double b1, double b2, double b3, double a2, double a3) {
		this.a2 = a2;
		this.a3 = a3;
		this.b1 = b1;
		this.b2 = b2;
		this.b3 = b3;
		this.i1 = 0.0;
		this.i2 = 0.0;
		this.o1 = 0.0;
		this.o2 = 0.0;
	}
	
	public void doFiltering(float[] in, float[] tmp){
        /* mirroring */
        double mir = 2 * in[0];
		  i1 = mir - in[2];
		  i2 = mir - in[1];
		  /* apply filtering */
		  accept(in);
		  /* invert  */
		  for (var j = 0; j < in.length; j++){
		    tmp[in.length-j-1] = in[j];
		  }
		  /* mirror again */
		  mir = 2*tmp[0];
		  i1 = mir - tmp[2];
		  i2 = mir - tmp[1];
		  /* apply filtering */
		  accept(tmp);
		  /* invert back */
		  for (var j = 0; j < in.length; j++){
		    in[j] = tmp[in.length-j-1];
		  }
	}

	private void accept(float[] in) {
		double i1 = this.i1, i2 = this.i2, o2 = this.o2, o1 = this.o1;
		for (var j = 0; j < in.length; j++) {
			double i0 = in[j];
			var o0 = b1 * i0 + b2 * i1 + b3 * i2 - a2 * o1 - a3 * o2;
			in[j] = (float) o0;
			i2 = i1;
			i1 = i0;
			o2 = o1;
			o1 = o0;
		}
		this.i1 = i1; this.i2 = i2; this.o2 = o2; this.o1 = o1;
	}
}
