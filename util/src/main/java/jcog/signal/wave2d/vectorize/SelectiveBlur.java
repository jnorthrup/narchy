package jcog.signal.wave2d.vectorize;


public class SelectiveBlur {

	
	
		static final double[][] gks = { {0.27901,0.44198,0.27901}, {0.135336,0.228569,0.272192,0.228569,0.135336}, {0.086776,0.136394,0.178908,0.195843,0.178908,0.136394,0.086776},
				{0.063327,0.093095,0.122589,0.144599,0.152781,0.144599,0.122589,0.093095,0.063327}, {0.049692,0.069304,0.089767,0.107988,0.120651,0.125194,0.120651,0.107988,0.089767,0.069304,0.049692} };


		
		static ImageTracer.ImageData blur (ImageTracer.ImageData imgd, float rad, float del){
            ImageTracer.ImageData imgd2 = new ImageTracer.ImageData(imgd.width,imgd.height,new byte[imgd.width*imgd.height*4]);


            int radius = (int)Math.floor((double) rad); if(radius<1){ return imgd; } if(radius>5){ radius = 5; }
            int delta = (int)Math.abs(del); if(delta>1024){ delta = 1024; }
            double[] thisgk = gks[radius-1];


            double wacc;
            double aacc;
            double bacc;
            double gacc;
            double racc;
            int idx;
            int k;
            int j;
            int i;
            for(j=0; j < imgd.height; j++ ){
				for( i=0; i < imgd.width; i++ ){

					racc = (double) 0; gacc = (double) 0; bacc = (double) 0; aacc = (double) 0; wacc = (double) 0;
					
					for( k = -radius; k < (radius+1); k++){
						
						if( ((i+k) > 0) && ((i+k) < imgd.width) ){
							idx = ((j*imgd.width)+i+k)*4;
							racc += (double) imgd.data[idx] * thisgk[k+radius];
							gacc += (double) imgd.data[idx + 1] * thisgk[k+radius];
							bacc += (double) imgd.data[idx + 2] * thisgk[k+radius];
							aacc += (double) imgd.data[idx + 3] * thisgk[k+radius];
							wacc += thisgk[k+radius];
						}
					}
					
					idx = ((j*imgd.width)+i)*4;
					imgd2.data[idx  ] = (byte) Math.floor(racc / wacc);
					imgd2.data[idx+1] = (byte) Math.floor(gacc / wacc);
					imgd2.data[idx+2] = (byte) Math.floor(bacc / wacc);
					imgd2.data[idx+3] = (byte) Math.floor(aacc / wacc);

				}
			}


            byte[] himgd = imgd2.data.clone();

			
			for( j=0; j < imgd.height; j++ ){
				for( i=0; i < imgd.width; i++ ){

					racc = (double) 0; gacc = (double) 0; bacc = (double) 0; aacc = (double) 0; wacc = (double) 0;
					
					for( k = -radius; k < (radius+1); k++){
						
						if( ((j+k) > 0) && ((j+k) < imgd.height) ){
							idx = (((j+k)*imgd.width)+i)*4;
							racc += (double) himgd[idx] * thisgk[k+radius];
							gacc += (double) himgd[idx + 1] * thisgk[k+radius];
							bacc += (double) himgd[idx + 2] * thisgk[k+radius];
							aacc += (double) himgd[idx + 3] * thisgk[k+radius];
							wacc += thisgk[k+radius];
						}
					}
					
					idx = ((j*imgd.width)+i)*4;
					imgd2.data[idx  ] = (byte) Math.floor(racc / wacc);
					imgd2.data[idx+1] = (byte) Math.floor(gacc / wacc);
					imgd2.data[idx+2] = (byte) Math.floor(bacc / wacc);
					imgd2.data[idx+3] = (byte) Math.floor(aacc / wacc);

				}
			}

			
			for( j=0; j < imgd.height; j++ ){
				for( i=0; i < imgd.width; i++ ){

					idx = ((j*imgd.width)+i)*4;

                    int d = Math.abs((int) imgd2.data[idx] - (int) imgd.data[idx]) + Math.abs((int) imgd2.data[idx + 1] - (int) imgd.data[idx + 1]) +
                            Math.abs((int) imgd2.data[idx + 2] - (int) imgd.data[idx + 2]) + Math.abs((int) imgd2.data[idx + 3] - (int) imgd.data[idx + 3]);

                    if(d>delta){
						imgd2.data[idx  ] = imgd.data[idx  ];
						imgd2.data[idx+1] = imgd.data[idx+1];
						imgd2.data[idx+2] = imgd.data[idx+2];
						imgd2.data[idx+3] = imgd.data[idx+3];
					}
				}
			}

			return imgd2;

		}
}
