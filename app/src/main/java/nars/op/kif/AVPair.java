
/* This code is copyrighted by Articulate Software (c) 2003.
It is released under the GNU Public License <http:
Users of this code also consent, by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.

Authors:
Adam Pease
Infosys LTD.
*/

package nars.op.kif;

import java.io.Serializable;

/*****************************************************************
 */
public class AVPair implements Comparable, Serializable {

    public String attribute = "";  
    public String value = "";

    /*****************************************************************
     */
    public AVPair() {

    }

    /*****************************************************************
     */
    public AVPair(String attrib, String val) {

        attribute = attrib;
        value = val;
    }

    /*****************************************************************
     */
    public int compareTo(Object avp) throws ClassCastException {

        if (!"com.articulate.sigma.AVPair".equalsIgnoreCase(avp.getClass().getName()))
            throw new ClassCastException("Error in AVPair.compareTo(): "
                                         + "Class cast exception for argument of class: "
                                         + avp.getClass().getName());
        
        
        return attribute.compareTo(((AVPair) avp).attribute);
    }

    /*****************************************************************
     */
    public String toString() {

        return '[' + attribute + ',' + value + ']';
    }
}
