package nars.op.rdfowl;

import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Created by me on 9/13/16.
 */
public class NQuadsRDFTest {


    @Test
    public void test1() {
        final NAR n = NARS.tmp();
        n.log();
        NQuadsRDF.input(n, "<http://example.org/#spiderman> <http://xmlns.com/foaf/0.1/name> \"Человек-паук\"@ru .");
        n.run(1);
        assertTrue(n.concepts.size() > 2);
    }

    @Disabled
    @Test
    public void testSchema1() throws Exception {
        final NAR n = NARS.tmp();
        File output = new File("/tmp/onto.nal");
        PrintStream pout = new PrintStream(new BufferedOutputStream(new FileOutputStream(output), 512 * 1024));

        n.input(
                NQuadsRDF.stream(n, new File(
                        
                        "/home/me/Downloads/nquad"
                )).peek(t -> {
                    pout.println(t.term().toString() + t.punc());
                    
                })
        );

        pout.close();





        n.run(1);
        
        n.log();
        n.input("$0.9$ (Bacteria <-> Pharmacy)?");


        


        n.run(128);




    }

    @Disabled
    @Test
    public void testSchema2() throws Exception {

        final NAR n = NARS.tmp();



        for (String input : new String[] { "/home/me/d/finance/money.orig.n3", "/home/me/d/finance/finance.orig.n3" } ) {
            File output = new File(input + ".nal");
            PrintStream pout = new PrintStream(new BufferedOutputStream(new FileOutputStream(output), 512 * 1024));

            NQuadsRDF.stream(n, new File(
                    input
            )).peek(t -> {
                t.pri(n.priDefault(t.punc()) / 10f);
                pout.println(t + ".");
            }).forEach(x -> {
                n.input(x);
                n.run(1); 
            });

            pout.close();
        }






        /*n.concepts().forEach(Concept::print);
        n.concept($.the("Buyer")).print();*/

        n.clear();
        n.log();
        n.run(1); n.input("({I}-->PhysicalPerson).");
        n.run(1); n.input("({I}-->Seller).");
        n.run(1); n.input("({I}-->ExternalRisk).");
        n.run(1); n.input("({I}-->Service).");
        n.run(1); n.input("({I}-->FinancialInstrument).");
        n.run(1); n.input("({I}-->NonResidentCapitalOwner)!");
        n.run(1); n.input("isReceiverOfPhysicalValue(I,#1)!");
        n.run(1); n.input("--isReceiverOfPhysicalValue(#1,I)!");
        n.run(1); n.input("isReceiverOfObligationValue(I,#1)!");
        n.run(1); n.input("--isReceiverOfObligationValue(#1,I)!");
        n.run(1); n.input("$0.99 (I<->?x)?");
        n.run(2512);

    }
}