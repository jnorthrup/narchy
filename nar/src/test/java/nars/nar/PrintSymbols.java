package nars.nar;

import nars.Op;/**
 *
 * @author me
 */


public enum PrintSymbols {
    ;

    public static void main(String[] args) {


        System.out.println("string" + "\t\t" + "rel?" + "\t\t" + "\t\t" + "opener?" + "\t\t" + "closer?");
        int symbols = 0;
        for (Op i : Op.values()) {
            System.out.println(i.str);

            symbols++;
        }
        System.out.println("symbols=" + symbols);
    }
}
