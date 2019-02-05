/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
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


import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static alice.tuprolog.PrologPrim.*;


/**
 * Administration of primitive predicates
 *
 * @author Alex Benini
 */
public class PrologPrimitives {

    private static final Set<String> PRIMITIVE_PREDICATES = Set.of(",", "':-'", ":-");
    private final Set<PrologLib> libs;
    private final Map<String, PrologPrim> directives;
    private final Map<String, PrologPrim> predicates;
    private final Map<String, PrologPrim> functors;

    public PrologPrimitives() {
        libs = new CopyOnWriteArraySet<>();
        directives = new ConcurrentHashMap();
        predicates = new ConcurrentHashMap();
        functors = new ConcurrentHashMap();
    }


    void start(Prolog prolog) {
        start(new BuiltIn(prolog));
    }

    void start(PrologLib src) {
        if (!libs.add(src))
            throw new RuntimeException("already loaded: " + src);

        Map<Integer, List<PrologPrim>> sp = src.primitives();
        for (int type : new int[] { DIRECTIVE, PREDICATE, FUNCTOR }) {
            Map<String, PrologPrim> table = table(type);
            sp.get(type).forEach(p-> table.put(p.key, p));
        }
    }


    void stop(PrologLib src) {
        if (!libs.remove(src))
            throw new RuntimeException("not loaded: " + src);

        Map<Integer, List<PrologPrim>> sp = src.primitives();
        for (int type : new int[] { DIRECTIVE, PREDICATE, FUNCTOR }) {
            Map<String, PrologPrim> table = table(type);
            sp.get(type).forEach(p-> table.remove(p.key));
        }
    }


    private Map<String, PrologPrim> table(int type) {
        Map<String, PrologPrim> table;
        switch (type) {
            case DIRECTIVE: table = directives; break;
            case PREDICATE: table = predicates; break;
            case FUNCTOR: table = functors; break;
            default:
                throw new UnsupportedOperationException();
        }
        return table;
    }


    /**
     * Identifies the term passed as argument.
     * <p>
     * This involves identifying structs representing builtin
     * predicates and functors, and setting up related structures and links
     *
     * @return term with the identified built-in directive
     * @parm term the term to be identified
     */
    public Struct identifyDirective(Struct term) {
        identify(term, DIRECTIVE);
        return term;
    }

    public boolean evalAsDirective(Struct d) throws Throwable {
        PrologPrim pd = identifyDirective(d).getPrimitive();
        if (pd != null) {
            try {
                pd.evalAsDirective(d);
                return true;
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        } else
            return false;
    }


    public void identify(Term term, int typeOfPrimitive) {
        if (term instanceof Var)
            term = term.term();

        if (term instanceof Struct)
            identify((Struct) term, typeOfPrimitive);
    }

    public void identify(Struct t, int typeOfPrimitive) {
        if (t.isPrimitive())
            return; //assume already identified

        final int primType = PRIMITIVE_PREDICATES.contains(t.name()) ? PREDICATE : FUNCTOR;
        int arity = t.subs();
        for (int c = 0; c < arity; c++) {
            identify(t.sub(c), primType);
        }

        PrologPrim p = table(typeOfPrimitive).get(t.key());
        if (p!=null)
            t.setPrimitive(p);
    }



//    public static class PrimitiveStruct extends Struct {
//
//        public PrimitiveStruct(String f) {
//            super(f);
//        }
//    }























    /*Castagna 06/2011*/





    /**/
}