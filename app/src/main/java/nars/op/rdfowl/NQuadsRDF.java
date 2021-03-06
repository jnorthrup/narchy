package nars.op.rdfowl;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.NALTask;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.Set;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nars.$.*;
import static nars.Op.*;

/**
 * Created by me on 6/4/15.
 */
public abstract class NQuadsRDF {


    

    

    

    
    






    







    public static void input(@NotNull NAR nar, String input) {
        NxParser p = new NxParser();
        p.parse(Collections.singleton(input));
        input(nar, p);
    }

    public static void input(@NotNull NAR nar, @NotNull InputStream input) {

        NxParser p = new NxParser();
        p.parse(input);
        input(nar, p);
        






    }

    @Deprecated
    public static void input(@NotNull NAR nar, @NotNull Iterable<Node[]> nxp) {
        input(nar, stream(nxp));
    }

    public static Stream<Node[]> stream(@NotNull Iterable<Node[]> nxp) {
        return StreamSupport.stream(nxp.spliterator(), false);
    }

    public static Stream<Node[]> stream(@NotNull InputStream input) {

        NxParser p = new NxParser();
        p.parse(input);
        return stream(p);
    }

    @Deprecated
    public static void input(@NotNull NAR nar, @NotNull Stream<Node[]> nxp) {
        nar.input(stream(nar, nxp));
    }


    public static Stream<Task> stream(@NotNull NAR n, File f) throws FileNotFoundException {
        return NQuadsRDF.stream(n, NQuadsRDF.stream(f));
    }

    public static Stream<Task> stream(@NotNull NAR nar, @NotNull Stream<Node[]> nxp) {

        return nxp.map(new Function<Node[], Task>() {
            @Override
            public Task apply(Node[] nx) {
                if (nx.length >= 3) {


                    return inputNALlike(
                            nar,
                            resource(nx[0]),
                            resource(nx[1]),
                            resource(nx[2])
                    );

                }
                return null;
            }
        }).filter(Objects::nonNull);
    }










































































    
    public static Atomic resource(@NotNull Node n) {
        String s = n.getLabel();
        
        

        if (s.contains("#")) {
            String[] a = s.split("#");
            
            s = a[1];
        } else {
            String[] a = s.split("/");
            if (a.length == 0) return null;
            s = a[a.length - 1];
        }

        if (s.isEmpty()) return null;

        try {
            return Atomic.the(s);
        } catch (Exception e) {
            return $.INSTANCE.quote(s);
        }

        
        
        
    }
















    public static Term atom(@NotNull String uri) {
        int lastSlash = uri.lastIndexOf((int) '/');
        if (lastSlash != -1)
            uri = uri.substring(lastSlash + 1);

        switch (uri) {
            case "owl#Thing":
                uri = "thing";
                break;
        }

        

        return Atomic.the(uri);















        
    }















    public static final Atomic owlClass = Atomic.the("Class");
    static final Atomic parentOf = Atomic.the("parentOf");
    static final Atomic type = Atomic.the("type");
    static final Atomic subClassOf = Atomic.the("subClassOf");
    static final Atomic isPartOf = Atomic.the("isPartOf");
    static final Atomic subPropertyOf = Atomic.the("subPropertyOf");
    static final Atomic equivalentClass = Atomic.the("equivalentClass");
    static final Atomic equivalentProperty = Atomic.the("equivalentProperty");
    static final Atomic inverseOf = Atomic.the("inverseOf");
    static final Atomic disjointWith = Atomic.the("disjointWith");
    static final Atomic domain = Atomic.the("domain");
    static final Atomic range = Atomic.the("range");
    static final Atomic sameAs = Atomic.the("sameAs");
    static final Atomic differentFrom = Atomic.the("differentFrom");

    static final Atomic dataTypeProperty = Atomic.the("DatatypeProperty");
    static final Atomic objProperty = Atomic.the("ObjectProperty");
    static final Atomic funcProp = Atomic.the("FunctionalProperty");
    static final Atomic invFuncProp = Atomic.the("InverseFunctionalProperty");


    static @Nullable Term subjObjInh(Term subject, char subjType, char objType, boolean reverse) {
        String a = reverse ? "subj" : "obj";
        String b = reverse ? "obj" : "subj";
        return INSTANCE.inh(
                INSTANCE.p(INSTANCE.v(subjType, a), INSTANCE.v(objType, b)),
                subject);
    }


    public static final Set<Atom> predicatesIgnored = new HashSet() {{
        add(Atomic.the("comment"));
        add(Atomic.the("isDefinedBy"));
    }};

    public static Task inputRaw(@NotNull NAR nar,
                                @Nullable Atom subject,
                                @NotNull Atom predicate, @NotNull Term object) {

        if (subject == null)
            return null;

        if (predicatesIgnored.contains(predicate))
            return null;

        try {
            Term term = /*$.inst*/ $.INSTANCE.inh($.INSTANCE.p(subject, object), predicate);
            if (term == null)
                throw new NullPointerException();
            Task t = new TaskBuilder(term, BELIEF, $.INSTANCE.t(1f, nar.confDefault(BELIEF))).apply(nar);
            return t;
        } catch (Exception e) {
            logger.error("rdf({}) to task: {}", new Term[]{subject, object, predicate}, e);
            return null;
        }

    }


    /**
     * Saves the relation into the database. Both entities must exist if the
     * relation is to be saved. Takes care of updating relation_types as well.
     */
    public static Task inputNALlike(@NotNull NAR nar,
                                    @Nullable Atomic subject,
                                    @Nullable Atomic predicate, @Nullable Term object) {


        if (predicatesIgnored.contains(predicate))
            return null;

        Term belief = null;
        if (Arrays.asList(type, subClassOf, subPropertyOf).contains(predicate)) {
            if (object.equals(owlClass)) {
                belief = $.INSTANCE.inst($.INSTANCE.varDep(1), subject);
            }

            
            else if (Arrays.asList(dataTypeProperty, funcProp, invFuncProp, objProperty).contains(object)
                    ) {
                return null;
            } else {
                

                belief = INSTANCE.inh(subject, object);
            }

        } else if ((predicate.equals(parentOf))) {
            
            
        } else if (predicate.equals(equivalentClass)) {

            belief = equi(
                    INSTANCE.inst(INSTANCE.varIndep("subj"), subject),
                    INSTANCE.inst(INSTANCE.varIndep("pred"), object)
            );
        } else if (predicate.equals(isPartOf)) {
            belief = $.INSTANCE.instprop(subject, object);
        } else if (predicate.equals(sameAs)) {
            belief = INSTANCE.sim(subject, object);
        } else if (predicate.equals(differentFrom)) {
            belief = INSTANCE.sim(subject, object).neg();
        } else if (predicate.equals(domain)) {
            
            


            
            


            belief = $.INSTANCE.impl(
                    $.INSTANCE.func(subject, $.INSTANCE.varIndep(1), $.INSTANCE.varDep(2)),
                    $.INSTANCE.inst($.INSTANCE.varIndep(1), object)
            );

        } else if (predicate.equals(range)) {
            belief = $.INSTANCE.impl(
                    $.INSTANCE.func(subject, $.INSTANCE.varDep(2), $.INSTANCE.varIndep(1)),
                    $.INSTANCE.inst($.INSTANCE.varIndep(1), object)
            );
            
            

            
            






        } else if (predicate.equals(equivalentProperty)) {

            belief = INSTANCE.sim(subject, object);
        } else if (predicate.equals(inverseOf)) {

            
            belief = equi(
                    subjObjInh(subject, '$', '$', false),
                    subjObjInh(object, '$', '$', true));

        } else if (predicate.equals(disjointWith)) {
            

            belief = $.INSTANCE.inst($.INSTANCE.varDep(1),
                    CONJ.the(subject, object)
            ).neg();

            
            

        } else {
            if (subject != null && object != null && predicate != null) {
                belief =
                        
                        INSTANCE.inh(
                                INSTANCE.p(subject, object),
                                predicate
                        );
            }
        }

        if (belief instanceof Compound) {

            return NALTask.the(belief, BELIEF, $.INSTANCE.t(1f, nar.confDefault(BELIEF)), nar.time(), Tense.ETERNAL, Tense.ETERNAL, nar.evidence())
                    .pri(nar)
                    ;



//                    .eternal().pri(nar).apply(nar);
        }

        return null;
    }

    public static Term equi(Term x, Term y) {
        
        
        return CONJ.the(
                IMPL.the(x,y),
                IMPL.the(y,x)
        );
    }

    public static Term disjoint(Term x, Term y) {
        
        
        return CONJ.the(
                CONJ.the(x,y).neg(),
                CONJ.the(x.neg(), y.neg()).neg()
        );
    }


    

    /**
     * Format the XML tag. Takes as input the QName of the tag, and formats it
     * to a namespace:tagname format.
     *
     * @param qname the QName for the tag.
     * @return the formatted QName for the tag.
     */
    private static @NotNull String formatTag(@NotNull QName qname) {
        String prefix = qname.getPrefix();
        String suffix = qname.getLocalPart();

        suffix = suffix.replace("http://dbpedia.org/ontology/", "");

        return prefix == null || prefix.isEmpty() ? suffix : prefix + ':' + suffix;
    }

    /**
     * Split up Uppercase Camelcased names (like Java classnames or C++ variable
     * names) into English phrases by splitting wherever there is a transition
     * from lowercase to uppercase.
     *
     * @param name the input camel cased name.
     * @return the "english" name.
     */
    private static String getEnglishName(@NotNull String name) {
        StringBuilder englishNameBuilder = new StringBuilder();
        char[] namechars = name.toCharArray();
        for (int i = 0; i < namechars.length; i++) {
            if (i > 0 && Character.isUpperCase(namechars[i])
                    && Character.isLowerCase(namechars[i - 1])) {
                englishNameBuilder.append(' ');
            }
            englishNameBuilder.append(namechars[i]);
        }
        return englishNameBuilder.toString();
    }


    static final Logger logger = LoggerFactory.getLogger(NQuadsRDF.class);

    @Deprecated
    public static void input(NAR n, File f) throws FileNotFoundException {
        logger.info("loading {}", f);
        input(n, new BufferedInputStream(new FileInputStream(f)));

    }

    public static Stream<Node[]> stream(File f) throws FileNotFoundException {
        logger.info("loading {}", f);
        return stream(new BufferedInputStream(new FileInputStream(f)));
    }










































}

