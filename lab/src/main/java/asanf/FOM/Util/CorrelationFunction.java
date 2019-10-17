package asanf.FOM.Util;

/*
 * Interfaccia per le misure di correlazione
 */
@FunctionalInterface
public interface CorrelationFunction {
	
	double calculateCorrelation(double freq_x, double freq_y, double freq_xy);

}
