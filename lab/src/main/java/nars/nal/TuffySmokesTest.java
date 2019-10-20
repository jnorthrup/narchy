package nars.nal;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;

/**
 * Created by me on 1/28/16.
 */
public class TuffySmokesTest {

    static NAR n;

    static void print() throws Narsese.NarseseException {
        for (String name : new String[]{"Edward", "Anna", "Bob", "Frank", "Gary", "Helen"}) {
            String t = "<" + name + " --> Cancer>";

            Concept c = n.conceptualize(t);

            System.err.print(System.identityHashCode(c) + " ");

            if (c == null) {
                System.err.println(t + " unknown" + " ");
            } else {












            }
            
        }
    }

    static void input(NAR n) throws Narsese.NarseseException {
        n.input("<(Anna&Bob) --> Friends>. %1.00;0.99%");
        n.input("<(Anna&Edward) --> Friends>. %1.00;0.99%");
        n.input("<(Anna&Frank) --> Friends>. %1.00;0.99%");
        n.input("<(Edward&Frank) --> Friends>. %1.00;0.99%");
        n.input("<(Gary&Helen) --> Friends>. %1.00;0.99%");
        n.input("<(Gary&Frank) --> Friends>. %0.00;0.99%"); 

        n.input("<Anna --> Smokes>. %1.00;0.99%");
        n.input("<Edward --> Smokes>. %1.00;0.99%");
        n.input("<Gary --> Smokes>. %0.00;0.99%");  
        n.input("<Frank --> Smokes>. %0.00;0.99%"); 
        n.input("<Helen --> Smokes>. %0.00;0.99%"); 
        n.input("<Bob --> Smokes>. %0.00;0.99%"); 
    }

    static void axioms(NAR n) throws Narsese.NarseseException {

        
        

        

        
        
        n.input("(<$1 --> Smokes> ==> <$1 --> Cancer>). %1.00;0.90%");
        
        

        

        
        
        
        
        n.input("(< --(($1&$2) --> Friends)  ==> (($2-->Smokes) && (--,($1-->Smokes)))). %1.00;0.90%");


        
        
        
        

        
        

        
        
        
        

        
        
        
        

        
        
        
        

        
        
        
        

        

    }

    
    
    public static void main(String[] args) throws Narsese.NarseseException {

        

        
        n = new NARS().get();
        
        

        
        n.logPriMin(System.out, 0f);

        

        
        System.err.println();

        axioms(n);
        input(n);



        n.run(250);  print();
        n.run(250);  print();
        n.run(250);  print();

        

        

        
        
        
        

        

        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        


		/*
        http:
		0.75	Cancer(Edward)
		0.65	Cancer(Anna)
		0.50	Cancer(Bob)
		0.45	Cancer(Frank)
		 */
    }
}
