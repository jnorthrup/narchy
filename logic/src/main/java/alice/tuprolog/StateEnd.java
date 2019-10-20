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

import jcog.data.list.FasterList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_LIST;

/**
 * @author Alex Benini
 * <p>
 * End state of demostration.
 */
public class StateEnd extends State {

    public final int endState;
    Struct goal;
    List<Var> vars;
    private int setOfCounter;

    /**
     * Constructor
     *
     * @param end Terminal state of computation
     */
    public StateEnd(int end) {
        endState = end;
    }

    @Override
    State run(PrologSolve e) {
        var gv = e.goalVars.size();
        if (gv > 0) {
            vars = new FasterList<>(gv);
            goal = (Struct) e.startGoal.copyResult(e.goalVars, vars);

            if ((this.endState == PrologRun.TRUE || this.endState == PrologRun.TRUE_CP) &&
                    (e.run.prolog.relinkVar()))
                relinkVar(e);

        } else {
            vars = EMPTY_LIST;
            goal = e.startGoal;
        }

        return null;
    }

    private static Term solve(Term a1, Object[] a, Term initGoalBag) {

        while (a1 instanceof Struct && ((Struct) a1).subs() > 0) {

            var a10 = ((Struct) a1).sub(0);
            if (a10 instanceof Var) {


                initGoalBag = findVarName(a10, a, a1, 0);


            } else if (a10 instanceof Struct) {
                var a100 = ((Struct) a10).sub(0);


                a10 = solve(a100, a, a10);
                ((Struct) a1).setSub(0, a10);

            }
            Term a11 = null;
            if (((Struct) a1).subs() > 1)
                a11 = ((Struct) a1).sub(1);
            a1 = a11;
        }
        if (a1 instanceof Var) {

            initGoalBag = findVarName(a1, a, initGoalBag, 0);
        }
        return initGoalBag;
    }

    private static Term findVarName(Term link, Object[] a, Term initGoalBag, int pos) {
        var findName = false;

        while (link instanceof Var && !findName) {

            var y = 0;
            while (!findName && y < a.length) {
                Term gVar = (Var) a[y];
                while (!findName && gVar instanceof Var) {

                    if (gVar == link || ((Var) gVar).name().equals(((Var) link).name())) {


                        ((Struct) initGoalBag).setSub(pos, new Var(((Var) a[y]).name()));
                        findName = true;
                    }
                    gVar = ((Var) gVar).link();
                }
                y++;
            }
            link = ((Var) link).link();
        }
        return initGoalBag;
    }


    private void relinkVar(PrologSolve e) {
        var pParent = e.run.prolog;


        var bag = e.run.getBagOFres();
        var initBag = pParent.getBagOFbag();



        /* itero nel goal per cercare una eventuale struttura che deve fare match con la
         * result bag ESEMPIO setof(X,member(X,[V,U,f(U),f(V)]),[a,b,f(b),f(a)]).
         */
        var tgoal = pParent.getBagOFgoal();
        var a = (e.goalVars).toArray();


        var query = e.query;


        if (";".equals(((Struct) query).name())) {
            var query_temp = (Struct) ((Struct) query).sub(0);
            if ("setof".equals(query_temp.name()) && setOfCounter == 0) {
                query = query_temp;
                this.setOfCounter++;
            } else {
                query_temp = (Struct) ((Struct) query).sub(1);
                if ("setof".equals(query_temp.name()))
                    query = query_temp;
            }
        }

        if (((Struct) query).subs() > 2 && ((Struct) query).sub(2) instanceof Struct) {

            var findSamePredicateIndicator = false;
            var find = false;
            Term initGoalBag = null;

            Prolog p = null;

            while (tgoal instanceof Var && ((Var) tgoal).link() != null) {
                tgoal = ((Var) tgoal).link();

                if (tgoal instanceof Struct) {
                    tgoal = ((Struct) tgoal).sub(1);

                    if (p == null) p = new Prolog();

                    if (tgoal.unify(p, ((Var) initBag).link())) {

                        initGoalBag = tgoal;
                        find = true;
                        findSamePredicateIndicator = true;
                        break;
                    } else if (((Var) initBag).link() instanceof Struct) {
                        var s = (Struct) ((Var) initBag).link();

                        if (tgoal instanceof Struct && s.key().compareTo(((Struct) tgoal).key()) == 0) {

                            findSamePredicateIndicator = true;
                            find = true;
                            initGoalBag = tgoal;
                        }
                    }

                    if (find || findSamePredicateIndicator && initGoalBag instanceof Struct) {

                        var a0 = ((Struct) initGoalBag).sub(0);
                        var a1 = ((Struct) initGoalBag).sub(1);
                        if (a0 instanceof Var) {


                            initGoalBag = findVarName(a0, a, initGoalBag, 0);

                        }
                        a1 = solve(a1, a, a1);
                        ((Struct) initGoalBag).setSub(1, a1);
                    }
                }
            }


            if (initGoalBag != null) {

                var initGoalBagList = new ArrayList<Term>();
                var initGoalBagTemp = (Struct) initGoalBag;
                while (initGoalBagTemp.subs() > 0) {
                    var t1 = initGoalBagTemp.sub(0);
                    initGoalBagList.add(t1);
                    var t2 = initGoalBagTemp.sub(1);
                    if (t2 instanceof Struct) {
                        initGoalBagTemp = (Struct) t2;
                    }
                }


                var initGoalBagListOrdered = new ArrayList<Term>();
                if ("setof".equals(((Struct) query).name())) {
                    var initGoalBagListVar = initGoalBagList.stream().filter(anInitGoalBagList -> anInitGoalBagList instanceof Var).map(anInitGoalBagList -> ((Var) anInitGoalBagList).name()).collect(Collectors.toCollection(ArrayList::new));

                    List<Term> left = new ArrayList<>();
                    left.add(initGoalBagList.get(0));
                    List<Term> right = new ArrayList<>();
                    List<Term> right_temp = new ArrayList<>();

                    List<Term> left_temp = new ArrayList<>();
                    for (var m = 1; m < initGoalBagList.size(); m++) {
                        var k = 0;
                        for (k = 0; k < left.size(); k++) {
                            if (initGoalBagList.get(m).isGreaterRelink(left.get(k), initGoalBagListVar)) {

                                left_temp.add(left.get(k));
                            } else {

                                left_temp.add(initGoalBagList.get(m));
                                break;
                            }
                        }
                        if (k == left.size())
                            left_temp.add(initGoalBagList.get(m));
                        for (var y = 0; y < left.size(); y++) {

                            var search = false;
                            for (var aLeft_temp : left_temp) {
                                if (aLeft_temp.toString().equals(left.get(y).toString()))
                                    search = true;
                            }
                            if (search) {

                                left.remove(y);
                                y--;
                            } else {

                                right_temp.add(left.get(y));
                                left.remove(y);
                                y--;
                            }
                        }
                        for (var y = 0; y < right.size(); y++) {
                            right_temp.add(right.get(y));
                            right.remove(y);
                            y--;
                        }
                        right.addAll(right_temp);

                        right_temp.clear();
                        left.addAll(left_temp);

                        left_temp.clear();

                    }


                    initGoalBagListOrdered.addAll(left);
                    initGoalBagListOrdered.addAll(right);


                } else initGoalBagListOrdered = initGoalBagList;

                initGoalBagTemp = (Struct) initGoalBag;

                var t = initGoalBagListOrdered.toArray();
                var t1 = Arrays.stream(t).map(item -> (Term) item).toArray(Term[]::new);


                initGoalBag = new Struct(initGoalBagTemp.name(), t1);


                List<Term> initBagList = new ArrayList<>();
                var initBagTemp = (Struct) ((Var) initBag).link();
                while (initBagTemp.subs() > 0) {
                    var t0 = initBagTemp.sub(0);
                    initBagList.add(t0);
                    var t2 = initBagTemp.sub(1);
                    if (t2 instanceof Struct) {
                        initBagTemp = (Struct) t2;
                    }
                }

                var tNoOrd = initBagList.toArray();
                var termNoOrd = Arrays.stream(tNoOrd).map(o -> (Term) o).toArray(Term[]::new);


                initBag = new Struct(initGoalBagTemp.name(), termNoOrd);
            }


            if (findSamePredicateIndicator) {

                if (p == null) p = new Prolog();

                if (!(find && initGoalBag.unify(p, initBag))) {

                    e.nextState = PrologRun.END_FALSE;

                    var prologRun = pParent.run;
                    var ss = prologRun.sinfo != null ? prologRun.sinfo.setOfSolution : null;
                    var s = ss != null ? ss + "\n\nfalse." : "null\n\nfalse.";
                    pParent.endFalse(s);

                    return;
                }
            }
        }
        /*
         * STEP1: dalla struttura risultato bagof (bag = (c.getEngineMan()).getBagOFres())
         * estraggo la lista di tutte le variabili
         * memorizzate nell'ArrayList<String> lSolVar
         * lSolVar = [H_e2301, H_e2302, H_e2303, H_e2304, H_e2305, H_e2306, H_e2307, H_e2308]
         */

        var lSolVar = new ArrayList<String>();

        /*NB lSolVar ha lunghezza multipla di lGoal var, se ho pi soluzioni si ripete
         * servirebbe esempio con 2 bag */
        var l_temp = new ArrayList<String>();
        for (var i = 0; i < bag.size(); i++) {
            var resVar = (Var) bag.get(i);

            var t = resVar.link();

            if (t != null) {
                if (t instanceof Struct) {
                    var t1 = ((Struct) t);


                    l_temp.clear();
                    l_temp = findVar(t1, l_temp);
                    for (var w = l_temp.size() - 1; w >= 0; w--) {
                        lSolVar.add(l_temp.get(w));
                    }
                } else if (t instanceof Var) {
                    while (t instanceof Var) {
                        resVar = (Var) t;

                        t = resVar.link();

                    }
                    lSolVar.add(resVar.name());
                    bag.set(i, resVar);
                }
            } else lSolVar.add(resVar.name());
        }

        /*
         * STEP2: dalla struttura goal bagof (goalBO = (Var)(c.getEngineMan()).getBagOFgoal())
         * estraggo la lista di tutte le variabili
         * memorizzate nell'ArrayList<String> lgoalBOVar
         * lgoalBOVar = [Z_e0, X_e73, Y_e74, V_e59, WithRespectTo_e31, U_e588, V_e59, H_e562, X_e73, Y_e74, F_e900]
         */

        var goalBO = (Var) pParent.getBagOFgoal();

        var lgoalBOVar = new ArrayList<String>();
        var goalBOvalue = goalBO.link();
        if (goalBOvalue instanceof Struct) {
            var t1 = ((Struct) goalBOvalue);

            l_temp.clear();
            l_temp = findVar(t1, l_temp);
            for (var w = l_temp.size() - 1; w >= 0; w--) {
                lgoalBOVar.add(l_temp.get(w));
            }
        }


        /*
         * STEP3: prendere il set di variabili libere della bagof
         * fare il match con le variabili del goal in modo da avere i nomi del goal esterno
         * questo elenco ci servirˆ per eliminare le variabili in pi che abbiamo in lgoalBOVar
         * ovvero tutte le variabili associate al template
         * lGoalVar [Y_e74, U_e588, V_e59, X_e73, Y_e74, U_e588, F_e900]
         * mette quindi in lGoalVar le variabili che compaiono in goalVars e sono anche libere
         * per la bagof c.getEngineMan().getBagOFvarSet()
         */

        var v = (Var) pParent.getBagOFvarSet();
        var varList = (Struct) v.link();
        List<String> lGoalVar = new ArrayList<>();


        if (varList != null)
            for (java.util.Iterator<? extends Term> it = varList.listIterator(); it.hasNext(); ) {


                var var = it.next();
                for (var anA : a) {
                    var vv = (Var) anA;
                    var vLink = vv.link();
                    if (vLink != null && vLink.isEqual(var)/*&& !(var.toString().startsWith("_"))*/) {

                        lGoalVar.add(vv.name());
                    }
                }
            }


        /*
         * STEP4: pulisco lgoalBOVar lasciando solo i nomi che compaiono effettivamente in
         * lGoalVar (che  la rappresentazione con nomi esterni delle variabili libere nel
         * goal della bagof
         */
        lgoalBOVar.retainAll(lGoalVar);

        if (lGoalVar.size() > lgoalBOVar.size()) {

            for (var h = 0; h < lGoalVar.size(); h++)
                if (h >= lgoalBOVar.size()) {

                    lgoalBOVar.add(lGoalVar.get(h));
                }
        }
        /*
         * STEP5: sostituisco le variabili nel risultato (sia in goals che vars)
         * a) cerco l'indice della variabile in lSolVar
         * b) sostituisco con quella di stesso indice in lgoalBOVar
         */
        var goalSolution = new Var();

        if (!lSolVar.isEmpty() && !lgoalBOVar.isEmpty() && !varList.isGround() && !goalBO.isGround()) {
            String bagVarName = null;
            for (var i = 0; i < bag.size(); i++) {

                var resVar = (Var) bag.get(i);

                var t = resVar.link();
                if (t == null) {

                    t = resVar;
                }


                bagVarName = null;
                for (var anA : a) {
                    var vv = (Var) anA;
                    var vv_link = structValue(vv, i);

                    if (vv_link.isEqual(t)) {


                        if (bagVarName == null) {
                            bagVarName = vv.getOriginalName();
                            goalSolution = vv;
                        }


                        if (vv_link.link() != null && vv_link.link() instanceof Struct) {
                            var s = substituteVar((Struct) vv_link.link(), lSolVar, lgoalBOVar);

                        } else {
                            var index = lSolVar.indexOf(resVar.name());


                            setStructValue(vv, i, new Var(lgoalBOVar.get(index)));

                        }
                    }
                }

            }

            for (var j = 0; j < vars.size(); j++) {
                var vv = vars.get(j);
                var on = vv.getOriginalName();
                if (on.equals(bagVarName)) {
                    var solVar = varValue2(goalSolution);

                    solVar.setName(on);
                    solVar.rename(0, 0);

                    vars.set(j, solVar);
                    break;
                }
            }
        }

        /*
         * STEP6: gestisco caso particolare SETOF in cui non stampa la soluzione
         */
        var bagString = pParent.getBagOFresString();
        var i = 0;
        var s = "";

        var bs = bagString.size();
        for (var m = 0; m < bs; m++) {
            var bagResString = bag.get(m).toString();
            var var = false;
            if (bag.get(m) instanceof Var && ((Var) bag.get(m)).link() != null && (((Var) bag.get(m)).link() instanceof Struct) && !((Var) bag.get(m)).link().isAtom())
                var = true;

            if (var && bagResString.length() != bagString.get(m).length()) {

                var st = new StringTokenizer(bagString.get(m));
                var st1 = new StringTokenizer(bagResString);
                while (st.hasMoreTokens()) {
                    var t1 = st.nextToken(" /(),;");

                    var t2 = st1.nextToken(" /(),;");

                    if (t1.compareTo(t2) != 0 && !t2.contains("_")) {

                        s = s + lGoalVar.get(i) + '=' + t2 + ' ';

                        pParent.setSetOfSolution(s);
                        i++;
                    }
                }
            }
        }


        pParent.relinkVar(false);
        pParent.setBagOFres(null);
        pParent.setBagOFgoal(null);
        pParent.setBagOFvarSet(null);
        pParent.setBagOFbag(null);
    }


    public static Var varValue2(Var v) {
        Term l;
        while ((l = v.link()) != null) {

            if (l instanceof Var)
                v = (Var) l;
            else
                break;
        }
        return v;
    }

    public static Var structValue(Var v, int i) {
        structValue:
        while (true) {
            var vStruct = new Var();
            Term l;
            while ((l = v.link()) != null) {

                if (l instanceof Var) {

                    v = (Var) l;
                } else if (l instanceof Struct) {
                    var s = ((Struct) l);


                    while (i > 0) {
                        var s1 = s.sub(1);

                        if (s1 instanceof Struct) {
                            s = (Struct) s1;
                        } else if (s1 instanceof Var) {
                            vStruct = ((Var) s1);
                            if (vStruct.link() != null) {
                                i--;
                                v = vStruct;
                                continue structValue;
                            }
                            return vStruct;
                        }
                        i--;
                    }
                    vStruct = ((Var) s.sub(0));
                    break;
                } else break;
            }

            return vStruct;
        }
    }

    public static void setStructValue(Var v, int i, Var v1) {
        Term l;
        while ((l = v.link()) != null) {

            if (l instanceof Var) {

                v = (Var) l;
            } else if (l instanceof Struct) {
                var s = ((Struct) l);


                while (i > 0) {

                    var s1 = s.sub(1);
                    if (s1 instanceof Struct)
                        s = (Struct) s1;
                    else if (s1 instanceof Var) {
                        v = (Var) s1;
                        s = ((Struct) v.link());
                    }
                    i--;
                }
                s.setSub(0, v1);
                break;
            } else break;
        }

    }


    public static ArrayList<String> findVar(Struct s, ArrayList<String> l) {
        var allVar = l;
        if (allVar == null) allVar = new ArrayList();
        if (s.subs() > 0) {
            var t = s.sub(0);
            if (s.subs() > 1) {
                var tt = s.sub(1);

                if (tt instanceof Var) {
                    allVar.add(((Var) tt).name());
                } else if (tt instanceof Struct) {
                    findVar((Struct) tt, allVar);
                }
            }
            if (t instanceof Var) {
                allVar.add(((Var) t).name());
            } else if (t instanceof Struct) {
                findVar((Struct) t, allVar);
            }
        }
        return allVar;
    }

    public static Struct substituteVar(Struct s, ArrayList<String> lSol, ArrayList<String> lgoal) {
        var t = s.sub(0);

        Term tt = null;
        if (s.subs() > 1)
            tt = s.sub(1);

        if (tt instanceof Var) {
            var index = lSol.indexOf(((Var) tt).name());

            s.setSub(1, new Var(lgoal.get(index)));
            if (t instanceof Var) {
                var index1 = lSol.indexOf(((Var) t).name());

                s.setSub(0, new Var(lgoal.get(index1)));
            }
            if (t instanceof Struct && ((Struct) t).subs() > 0) {


                var s1 = substituteVar((Struct) t, lSol, lgoal);

                s.setSub(0, s1);
            }
        } else {
            if (t instanceof Var) {
                var index1 = lSol.indexOf(((Var) t).name());

                s.setSub(0, new Var(lgoal.get(index1)));
            }
            if (t instanceof Struct) {

                var s1 = substituteVar((Struct) t, lSol, lgoal);

                s.setSub(0, s1);
            }
        }

        return s;
    }


    public String toString() {
        switch (endState) {
            case PrologRun.FALSE:
                return "FALSE";
            case PrologRun.TRUE:
                return "TRUE";
            case PrologRun.TRUE_CP:
                return "TRUE_CP";
            default:
                return "HALT";
        }
    }

}