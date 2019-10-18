package asanf.FOM.Util;


/*
 * Mutual Information
 * metodo che calcola la dipendenza tra due entit
 */
public class PMI implements CorrelationFunction{
	private static final int num = 2048;
	

	
	@Override
    public double calculateCorrelation(double p_x, double p_y, double p_x_y) {
		
		int isZero = Double.compare(p_x_y, 0.0);

        if (isZero == 0)
			return -num;

        int isOne = Double.compare(p_x_y, 1.0);
        if (isOne == 0)
			return num;
		
		double log2 = Math.log(2);
		/*double ex = - p_x * Math.log(p_x)/log2;
		double ey = - p_y * Math.log(p_y)/log2;
		double exy = - p_x_y * Math.log(p_x_y)/log2;
		
		return  ex + ey - exy;*/
		
		return  (Math.log(p_x_y) - Math.log(p_x) - Math.log(p_y))/Math.log(2);
	}
	
	

}
