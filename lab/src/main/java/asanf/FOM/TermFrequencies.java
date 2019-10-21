package asanf.FOM;


import asanf.FOM.Util.DTMatrix;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Classe che conserva sia le frequenze singole che quelle congiunte 
 * @param <E> Classe degli oggetti di cui si vuole conservare la frequenza
 */
public class TermFrequencies<E> extends DTMatrix<E> implements Iterable<E> {

	/**
	 * Costruttore base
	 */
	public TermFrequencies(){
		super();
	}

	/**
	 * Costruttore
	 * @param size Grandezza iniziale della matrice delle frequenze
	 */
	public TermFrequencies(int size){
		super(size);
		finalValues = false;
	}


	/**
	 * Aggiunge un occorrenza del termine passato per parametro
	 * @param term Termine da aggiornare
	 * @return true se è stato possibile aumentare il conteggio, 
	 * false altrimenti
	 */
	public boolean addOccurrence(E term){

		if(finalValues)
			return false;

		add(term, 1.0);
		
		return true;

	}

	/**
	 * Aggiunge un occorrenza per una coppia di termini
	 * @param firstTerm primo termine della coppia
	 * @param secondTerm secondo termine della coppia
	 * @return true se è stato possibile aumentare il conteggio,
	 *  false altrimenti
	 */
	public boolean addOccurrence(E firstTerm, E secondTerm){
		
		if(finalValues)
			return false;

		add(firstTerm, secondTerm, 1.0);
		return true;
	}

	/**
	 * Metodo che segnala la fine del conteggio dei termini
	 * e calcola quindi le frequenze relative
	 * @return false se il metodo è già stato chiamato in precedenza 
	 */
	public boolean computeFrequencies(){
		if(finalValues)
			return false;

		normalizeBy((double) totWindows);
		finalValues = true;
		return true;
	}
	
	/**
	 * Aumenta il totale di finestre di testo
	 * @return true se è stato possibile modificare il valore, false altrimenti
	 */
	public boolean augmentWindows(){
		if(finalValues)
			return false;
		
		totWindows += 1;
		return true;
	}

	/**
	 * Restituisce la frequenza relativa di un termine
	 * @param term Termine di cui si vuole conoscere la frequenza
	 * @return Frequenza relativa del termine, -1 se il conteggio non è ancora 
	 * finito, -2 se il termine non esiste
	 */
	public double getFrequency(E term){
		
		if(!finalValues)
			return -2.0;
		return getValue(term);
	}


	/**
	 * Restituisce la frequenza relativa di una coppia di termini  
	 * @param firstTerm primo termine della coppia
	 * @param secondTerm secondo termine della coppia 
	 * @return frequenza relativa della coppia, -1 se il conteggio non è ancora 
	 * finito, -2 se uno dei termini non esiste
	 */
	public double getFrequency(E firstTerm, E secondTerm){
		
		if(!finalValues)
			return -2.0;
		return getValue(firstTerm, secondTerm);
	}
	
	public int getTotWindows(){
		return totWindows;
	}
	
	
	public boolean filterTerms(double lower, double upper){

		if(!finalValues)
			return false;

		List<E> toRemove = new ArrayList();
		terms.forEachKeyValue(new ObjectIntProcedure<E>() {
            @Override
            public void value(E term, int i) {
                double f = values[i][i];
                if ((f < lower || f > upper)) {

                    toRemove.add(term);
                }
            }
        });
        ObjectIntHashMap<E> eObjectIntHashMap = terms;
		for (E e : toRemove) {
			eObjectIntHashMap.remove(e);
		}
		terms.compact();

		return true;
	}
	
	@Override
	public Iterator<E> iterator() {
		return getTerms().iterator();
	}
	
	public Collection<E> getTerms(){
		return terms.keySet();
	}


	private int totWindows; 				
	private boolean finalValues;			
	
}
