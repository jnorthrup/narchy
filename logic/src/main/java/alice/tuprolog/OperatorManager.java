/*
 * tuProlog - Copyright (C) 2001-2006  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages Prolog operators.
 *
 * @see Operator
 */
@SuppressWarnings("serial")
public class OperatorManager extends ConcurrentHashMap<String, Operator> /**/ {

    /**
     * lowest operator priority
     */
    public static final int OP_LOW = 1;

    /**
     * highest operator priority
     */
    public static final int OP_HIGH = 1200;

    public OperatorManager() {
        super(128, 0.9f);
    }

    public void addOperator(Operator op) {
        put(op.name + op.type, op);
    }

    public Operator getOperator(String name, String type) {
        return get(name + type);
    }



    /**
     * Creates a new operator. If the operator is already provided,
     * it replaces it with the new one
     */
    public void opNew(String name, String type, int prio) {
        final Operator op = new Operator(name, type, prio);
        if (prio >= OP_LOW && prio <= OP_HIGH)
            addOperator(op);
    }

    /**
     * Returns the priority of an operator (0 if the operator is not defined).
     */
    public int opPrio(String name, String type) {
        Operator o = getOperator(name, type);
        return (o == null) ? 0 : o.prio;
    }
    public int opPrio(String nametype) {
        Operator o = get(nametype);
        return (o == null) ? 0 : o.prio;
    }














    /**
     * Gets the list of the operators currently defined
     *
     * @return the list of the operators
     */
    public Iterable<Operator> operators() {
        return values(); 
    }


/*Castagna 06/2011*/     
    /* Francesco Fabbri		 
     * 16/05/2011		 
     * Clone operation added		 
     */






/**/

}