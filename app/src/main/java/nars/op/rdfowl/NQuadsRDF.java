package nars.op.rdfowl;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nars.$.*;
import static nars.Op.BELIEF;
import static nars.Op.CONJ;
import static nars.Op.IMPL;

/**
 * Created by me on 6/4/15.
 */
public abstract class NQuadsRDF {


    //private static final String RDF_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    //private static String parentTagName = null;

    //private final NAR nar;

    //final float beliefConfidence;
    //private boolean includeDataType = false;

//    public NQuadsRDF(NAR n, float beliefConfidence) throws Exception {
//        this.nar = n;
//        this.beliefConfidence = beliefConfidence;
//    }

    //input(new FileInputStream(nqLoc));

//
//    final static Pattern nQuads = Pattern.compile(
//            //"((?:\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"(?:@\\w+(?:-\\w+)?|\\^\\^<[^>]+>)?)|<[^>]+>|\\_\\:\\w+|\\.)"
//            "(<[^\\s]+>|_:(?:[A-Za-z][A-Za-z0-9\\-_]*))\\s+(<[^\\s]+>)\\s+(<[^\\s]+>|_:(?:[A-Za-z][A-Za-z0-9\\-_]*)|\\\"(?:(?:\\\"|[^\"])*)\\\"(?:@(?:[a-z]+[\\-A-Za-z0-9]*)|\\^\\^<(?:[^>]+)>)?)\\s+(<[^\\s]+>).*"
//    );

    public static void input(@NotNull NAR nar, String input) {
        NxParser p = new NxParser();
        p.parse(Collections.singleton(input));
        input(nar, p);
    }

    public static void input(@NotNull NAR nar, @NotNull InputStream input) {
        //try {
        NxParser p = new NxParser();
        p.parse(input);
        input(nar, p);
        //}
//        catch (Exception pe) {
//            //try turtle parser
//            TurtleParser p = new TurtleParser();
//            p.parse(new InputStreamReader(input), URI.create("_"));
//            input(nar, p);
//        }
    }

    @Deprecated
    public static void input(@NotNull NAR nar, @NotNull Iterable<Node[]> nxp) {
        input(nar, stream(nxp));
    }

    public static Stream<Node[]> stream(@NotNull Iterable<Node[]> nxp) {
        return StreamSupport.stream(nxp.spliterator(), false);
    }

    public static Stream<Node[]> stream(@NotNull InputStream input) {
        //try {
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

        return nxp.map((Node[] nx) -> {
            if (nx.length >= 3) {
                //return inputRaw(

                return inputNALlike(
                        nar,
                        resource(nx[0]),
                        resource(nx[1]),
                        resource(nx[2])
                );

            }
            return null;
        }).filter(Objects::nonNull);
    }

//    public static void input(NAR nar, File input) throws Exception {
//        input(nar, new Scanner(input));
//    }


//    /**
//     * These parsing rules were devised by physically looking at the OWL file
//     * and figuring out what goes where. This should by no means be considered a
//     * generalized way to parse OWL files.
//     *
//     * Parsing rules:
//     *
//     * owl:Class@rdf:ID = entity (1), type=Wine optional:
//     * owl:Class/rdfs:subClassOf@rdf:resource = entity (2), type=Wine (2) --
//     * parent --> (1) if owl:Class/rdfs:subClassOf has no attributes, ignore if
//     * no owl:Class/rdfs:subClassOf entity, ignore it
//     * owl:Class/owl:Restriction/owl:onProperty@rdf:resource related to
//     * owl:Class/owl:Restriction/owl:hasValue@rdf:resource
//     *
//     * Region@rdf:ID = entity, type=Region optional:
//     * Region/locatedIn@rdf:resource=entity (2), type=Region (2) -- parent --
//     * (1) owl:Class/rdfs:subClassOf/owl:Restriction - ignore
//     *
//     * WineBody@rdf:ID = entity, type=WineBody WineColor@rdf:ID = entity,
//     * type=WineColor WineFlavor@rdf:ID = entity, type=WineFlavor
//     * WineSugar@rdf:ID = entity, type=WineSugar Winery@rdf:ID = entity,
//     * type=Winery WineGrape@rdf:ID = entity, type=WineGrape
//     *
//     * Else if no namespace, this must be a wine itself, capture as entity:
//     * ?@rdf:ID = entity, type=Wine all subtags are relations: tagname =
//     * relation_name tag@rdf:resource = target entity
//     */
//    public static void input(NAR nar, Scanner f) throws Exception {
//
//        List<String> items = Global.newArrayList(4);
//
//        f.useDelimiter("\n").forEachRemaining(s -> {
//            s = s.trim();
////            if (s.startsWith("#")) //?
////                return;
////            if (s.startsWith("@")) //@prefix not handled yet?
////                return;
//
//            Matcher m = nQuads.matcher(s);
//            items.clear();
//            while(m.find()) {
//                String t = s.substring(m.start(), m.end());
//                items.add(t);
//            }
//
//            System.out.println(s + " " + m.toMatchResult());
//
//            if (items.size() >= 3) {
//
//
//                Atom subj = resource(items.get(0));
//                Atom pred = resource(items.get(1));
//                Term obj = resourceOrValue(items.get(2));
//                if (subj != null && obj != null && pred != null) {
//                    if (!subj.equals(obj))  { //avoid equal subj & obj, if only namespace differs
//                        //try {
//                            input(nar, subj, pred, obj);
//                        //}
////                        catch (InvalidInputException iie) {
////                            System.err.println(iie);
////                            //iie.printStackTrace();
////                        }
//                    }
//                }
//            }
//        });
//    }

    //TODO interpret Node subclasses in special ways, possibly returning Compounds not only Atom's
    public static Atomic resource(@NotNull Node n) {
        String s = n.getLabel();
        //if (s.startsWith("<") && s.endsWith(">")) {
        //s = s.substring(1, s.length() - 1);

        if (s.contains("#")) {
            String[] a = s.split("#");
            //String p = a[0]; //TODO handle the namespace
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
            return $.quote(s); //HACK
        }

        //}
        //else
        //  return null;
    }
//    public static Term resourceOrValue(String s) {
//        Atom a = resource(s);
//        if (a == null) {
//            //...
//        }
//        return a;
//    }

//
//
//    abstract public static class TagProcessor {
//
//        abstract protected void execute(XMLStreamReader parser);
//    }


    public static Term atom(@NotNull String uri) {
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash != -1)
            uri = uri.substring(lastSlash + 1);

        switch (uri) {
            case "owl#Thing":
                uri = "thing";
                break;
        }

        //return Atom.the(Utf8.toUtf8(name));

        return Atomic.the(uri);

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }

//
//    //TODO make this abstract for inserting the fact in different ways
//    protected void inputClass(Term clas) {
//        inputClassBelief(clas);
//    }

//    private void inputClassBelief(Term clas) {
//        nar.believe(isAClass(clas));
//    }

//    public static Term isAClass(Term clas) {
//        return Instance.make(clas, owlClass);
//    }

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


    @Nullable
    static Term subjObjInh(Term subject, char subjType, char objType, boolean reverse) {
        String a = reverse ? "subj" : "obj";
        String b = reverse ? "obj" : "subj";
        return inh(
                p(v(subjType, a), v(objType, b)),
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
            Term term = /*$.inst*/ $.inh($.p(subject, object), predicate);
            if (term == null)
                throw new NullPointerException();
            Task t = new TaskBuilder(term, BELIEF, $.t(1f, nar.confDefault(BELIEF))).apply(nar);
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

        //http://www.w3.org/TR/owl-ref/

        Term belief = null;


        if (predicatesIgnored.contains(predicate))
            return null;

        if (predicate.equals(type) || predicate.equals(subClassOf) || predicate.equals(subPropertyOf)) {
            if (object.equals(owlClass)) {
                belief = $.inst($.varDep(1), subject); //some instance of the type
            }

            //if (!includeDataType) {
            else if (object.equals(dataTypeProperty) ||
                    object.equals(funcProp) ||
                    object.equals(invFuncProp) ||
                    object.equals(objProperty)
                    ) {
                return null;
            } else {
                //}

                belief = inh(subject, object);
            }

        } else if ((predicate.equals(parentOf))) {
            //.. parentOf is probably redundant
            //System.out.println(subject + " " + predicate + " " + object);
        } else if (predicate.equals(equivalentClass)) {

            belief = equi(
                    inst(varIndep("subj"), subject),
                    inst(varIndep("pred"), object)
            );
        } else if (predicate.equals(isPartOf)) {
            belief = $.instprop(subject, object);
        } else if (predicate.equals(sameAs)) {
            belief = sim(subject, object);
        } else if (predicate.equals(differentFrom)) {
            belief = sim(subject, object).neg();
        } else if (predicate.equals(domain)) {
            // PROPERTY domain CLASS
            //<PROPERTY($subj, $obj) ==> <$subj -{- CLASS>>.


            //Term b = inh(varIndep("subj"), object);
            //belief = conj(subjObjInh(subject, '$', '#', false),b);


            belief = $.impl(
                    $.func(subject, $.varIndep(1), $.varDep(2)),
                    $.inst($.varIndep(1), object)
            );

        } else if (predicate.equals(range)) {
            belief = $.impl(
                    $.func(subject, $.varDep(2), $.varIndep(1)),
                    $.inst($.varIndep(1), object)
            );
            // PROPERTY range CLASS
            //<PROPERTY($subj, $obj) ==> <$obj -{- CLASS>>.

            //Term b = inh(varIndep("obj"), object);
            //belief = conj(subjObjInh(subject, '#', '$', false),b);

//            belief = nar.term(
//                    //"<" + subject + "($subj,$obj) ==> <$obj -{- " + object + ">>"
//                    "(" + subject + "($subj,$obj) && <$obj -{- " + object + ">)"
//            );

        } else if (predicate.equals(equivalentProperty)) {

            belief = sim(subject, object);
        } else if (predicate.equals(inverseOf)) {

            //PREDSUBJ(#subj, #obj) <=> PREDOBJ(#obj, #subj)
            belief = equi(
                    subjObjInh(subject, '$', '$', false),
                    subjObjInh(object, '$', '$', true));

        } else if (predicate.equals(disjointWith)) {
            //System.out.println(subject + " " + predicate + " " + object);

            belief = $.inst($.varDep(1),
                    $.secti(subject, object)
            ).neg();

            //disjoint classes have no common instances:
            //TODO

        } else {
            if (subject != null && object != null && predicate != null) {
                belief =
                        //inst
                        inh(
                                p(subject, object),
                                predicate
                        );
            }
        }

        if (belief instanceof Compound) {
            //System.out.println(subject + " " + predicate + " " + object + " :: " + belief);

            return new TaskBuilder(belief, BELIEF,
                    $.t(1f, nar.confDefault(BELIEF)))
                    .eternal().apply(nar);
        }

        return null;
    }

    public static Term equi(Term x, Term y) {
        // (NOT (x AND y)) AND (NOT ((NOT x) AND (NOT y))) http://www.wolframalpha.com/input/?i=x+xor+y
        //return disj(impl(subj, pred), impl(pred, subj));
        return CONJ.the(
                IMPL.the(x,y),
                IMPL.the(y,x)
        );
    }

    public static Term disjoint(Term x, Term y) {
        // (NOT (x AND y)) AND (NOT ((NOT x) AND (NOT y))) http://www.wolframalpha.com/input/?i=x+xor+y
        //return disj(impl(subj, pred), impl(pred, subj));
        return CONJ.the(
                CONJ.the(x,y).neg(),
                CONJ.the(x.neg(), y.neg()).neg()
        );
    }


    // ======== String manipulation methods ========

    /**
     * Format the XML tag. Takes as input the QName of the tag, and formats it
     * to a namespace:tagname format.
     *
     * @param qname the QName for the tag.
     * @return the formatted QName for the tag.
     */
    @NotNull
    private static String formatTag(@NotNull QName qname) {
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


    final static Logger logger = LoggerFactory.getLogger(NQuadsRDF.class);

    @Deprecated
    public static void input(NAR n, File f) throws FileNotFoundException {
        logger.info("loading {}", f);
        input(n, new BufferedInputStream(new FileInputStream(f)));

    }

    public static Stream<Node[]> stream(File f) throws FileNotFoundException {
        logger.info("loading {}", f);
        return stream(new BufferedInputStream(new FileInputStream(f)));
    }

//    public static void main(String[] args) throws Exception {
//        Default n = new Default(1000,16,3);
//        //Solid d = new Solid(32, 4096,1,3,1,2);
//        //d.setInternalExperience(null).level(7);
//        //d.inputsMaxPerCycle.set(256);
//        //d.setTermLinkBagSize(64);
//
//
//
//        //n.input("schizo(I)!"); //needs nal8
//
//
//
//        new NQuadsRDF(n, "/home/me/Downloads/dbpedia.n4", 0.94f /* conf */) {
//
//            @Override
//            protected void believe(Compound assertion) {
//                float freq = 1.0f;
//
//                //insert with zero priority to bypass main memory go directly to subconcepts
//                n.believe(0.01f, Global.DEFAULT_JUDGMENT_DURABILITY, assertion, Tense.Eternal, freq, beliefConfidence);
//            }
//        };
//
//
//        //new TextOutput(n, System.out).setShowStamp(false).setOutputPriorityMin(0.25f);
//
//        n.frame(1);
//        n.frame(1); //one more to be sure
//
//
///*
//        //n.frame(100);
//        n.believe(0.75f, 0.8f, n.term("<Spacecraft <-> PopulatedPlace>!"),
//                Tense.Eternal, 1.0f, 0.95f);
//        n.believe(0.75f, 0.8f, n.term("(&&,SpaceMission,Work). :|:"),
//                Tense.Eternal, 1.0f, 0.95f);
//        //n.frame(5000);
//*/
//        //n.run();
//    }
}

