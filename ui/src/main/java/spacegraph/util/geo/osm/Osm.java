package spacegraph.util.geo.osm;

import it.unimi.dsi.fastutil.longs.Long2ReferenceRBTreeMap;
import jcog.data.list.FasterList;
import jcog.io.bzip2.BZip2InputStream;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static java.lang.Double.parseDouble;
import static jcog.Texts.l;
import static jcog.Texts.n4;

/**
 * Created by unkei on 2017/04/25.
 */
public class Osm {

    /** lon,lat coordinates */
    public RectFloat geoBounds;

    public final Long2ReferenceRBTreeMap<OsmNode> nodes = new Long2ReferenceRBTreeMap();
//    public final LongObjectHashMap<OsmNode> nodes;
    public final LongObjectHashMap<OsmRelation> relations = new LongObjectHashMap();
    public final LongObjectHashMap<OsmWay> ways = new LongObjectHashMap();


    public boolean ready = false;
    public String id;

    /** TODO abstract
     * bzless ~/test.osm.bz2  | fgrep 'k=' | sort > /tmp/x

     * */
    private static final Set<String> filteredKeys = Set.of(
            "source", "tiger:cfcc", "odbl"
    );

    public Osm() {

    }

    @Override
    public String toString() {
        return geoBounds + " (" + nodes.size() + " nodes, " + relations.size() + " relations, " + ways.size() + " ways)";
    }

    public Osm load(String filename) throws SAXException, IOException, ParserConfigurationException {
        InputStream fis = new BufferedInputStream(new FileInputStream(filename), 128 * 1024);

        if (filename.endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        } else if (filename.endsWith(".bz2")) {
            fis = new BZip2InputStream(fis);
        }

        return load(fis);
    }

    public static URL url(String apiURL, double lonMin, double latMin, double lonMax, double latMax)  {
        try {
            return new URL(apiURL + "/api/0.6/map?bbox=" +
                    n4(lonMin) + ',' + n4(latMin) + ',' + n4(lonMax) + ',' + n4(latMax) );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(String apiURL, double lonMin, double latMin, double lonMax, double latMax) throws SAXException, IOException, ParserConfigurationException {
        load(url(apiURL, lonMin, latMin, lonMax, latMax).openStream());
    }

    /** TODO make static so it can customize/optimize the returned instance */
    public Osm load(InputStream fis) throws SAXException, IOException, ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();


        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setCoalescing(true);


        var documentBuilder = factory.newDocumentBuilder();


        var document = documentBuilder.parse(fis);


        Collection<Element> relationElements = new FasterList<>();

        var childNodes = document.getDocumentElement().getChildNodes();
        var cl = childNodes.getLength();
        for (var i = 0; i < cl; i++) {
            var n = childNodes.item(i);

            switch (n.getNodeName()) {
                case "bounds": {
                    var e = ((Element) n);
                    assert(this.geoBounds == null);
                    this.geoBounds = RectFloat.XYXY(
                            (float) parseDouble(e.getAttribute("minlon")),
                            (float) parseDouble(e.getAttribute("minlat")),
                            (float) parseDouble(e.getAttribute("maxlon")),
                            (float) parseDouble(e.getAttribute("maxlat"))
                    );
                    break;
                }
                case "node": {
                    var childElement = (Element) n;
                    var id = l(childElement.getAttribute("id"));

                    Map<String, String> osmTags = null;
                    var nodeChildren = ((Element) n).getElementsByTagName("tag") /*childNode.getChildNodes()*/;
                    var nnc = nodeChildren.getLength();
                    for (var j = 0; j < nnc; j++) {
                        var nodeChild = nodeChildren.item(j);
                        /*if ("tag".equals(nodeChild.getNodeName()))*/
                        {
                            var ne = (Element) nodeChild;
                            var key = ne.getAttribute("k");
                            if (!filteredKeys.contains(key)) {
                                if (osmTags == null)
                                    osmTags = new UnifiedMap(1);
                                osmTags.put(key, ne.getAttribute("v"));
                            }
                        }
                    }


                    var oo = new OsmNode(id, new GeoVec3(childElement), osmTags);
                    this.nodes.put(id, oo);


                    break;
                }
                case "way": {
                    var childElement = (Element) n;
                    var id = l(childElement.getAttribute("id"));

                    List<OsmElement> refOsmNodes = new FasterList<>();
                    Map<String, String> osmTags = null;

                    var wayChildren = n.getChildNodes();
                    var l = wayChildren.getLength();
                    for (var j = 0; j < l; j++) {
                        var wayChild = wayChildren.item(j);
                        var node = wayChild.getNodeName();
                        switch (node) {
                            case "nd":
                                var wayChildElement = (Element) wayChild;
                                refOsmNodes.add(
                                        this.nodes.get(l(wayChildElement.getAttribute("ref")))
                                );
                                break;
                            case "tag":
                                var nodeChildElement = (Element) wayChild;
                                if (osmTags == null)
                                    osmTags = new UnifiedMap<>(1);
                                osmTags.put(nodeChildElement.getAttribute("k"), nodeChildElement.getAttribute("v"));
                                break;
                        }
                    }

                    var ow = new OsmWay(id, refOsmNodes, osmTags);
                    this.ways.put(id, ow);


                    break;
                }
                case "relation":
                    var childElement = (Element) n;
                    var id = l(childElement.getAttribute("id"));

                    var relationChildren = childElement.getElementsByTagName("tag");
                    Map<String, String> osmTags = null;
                    var l = relationChildren.getLength();
                    for (var j = 0; j < l; j++) {
                        var relationChild = relationChildren.item(j);
                        /*if ("tag".equals(relationChild.getNodeName()))*/
                        {
                            var e = (Element) relationChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap<>(1);
                            osmTags.put(e.getAttribute("k"), e.getAttribute("v"));
                        }
                    }

                    var or = new OsmRelation(id, null, osmTags);
                    this.relations.put(id, or);
                    relationElements.add(childElement);
                    break;
            }
        }


        
        for (var relationElement : relationElements) {
            var id = l(relationElement.getAttribute("id"));

            var osmRelation = this.relations.get(id);

            var tags = osmRelation.tags;
//            String highway, natural, building, building_part, landuse;
//            if (tags.isEmpty()) {
//                highway = natural = building = building_part = landuse = null;
//            } else {
//                highway = tags.get("highway");
//                natural = tags.get("natural");
//                building = tags.get("building");
//                building_part = tags.get("building:part");
//                landuse = tags.get("landuse");
//            }

            var relationChildren = relationElement.getElementsByTagName("member");
            List<OsmElement> osmMembers = null;
            var l = relationChildren.getLength();
            for (var j = 0; j < l; j++) {
                var relationChild = relationChildren.item(j);
                /*if ("member".equals(relationChild.getNodeName()))*/
                {
                    var r = (Element) relationChild;
                    var type = r.getAttribute("type");
                    var ref = l(r.getAttribute("ref"));

                    OsmElement member = null;
                    switch (type) {
                        case "node":
                            member = this.nodes.get(ref);
                            break;
                        case "way":
                            member = this.ways.get(ref);
//                            if (member != null) {
//                                if (highway != null) {
//                                    member.tag("highway", highway);
//                                }
//                                if (natural != null) {
//                                    member.tag("natural", natural);
//                                }
//                                if (building != null) {
//                                    member.tag("building", building);
//                                }
//                                if (building_part != null) {
//                                    member.tag("building:part", building_part);
//                                }
//                                if (landuse != null) {
//                                    member.tag("landuse", landuse);
//                                }
//                            }
                            break;
                        case "relation":
                            member = this.relations.get(ref);
                            break;
                    }
                    if (member != null) {
                        if (osmMembers == null)
                            osmMembers = new FasterList(1);
                        osmMembers.add(member);
                    }
                }
            }

            if (osmMembers != null && !osmMembers.isEmpty())
                osmRelation.addChildren(osmMembers);
        }

        
        
        for (var relationElement : relationElements) {
            var id = l(relationElement.getAttribute("id"));

            var osmRelation = this.relations.get(id);

            var tags = osmRelation.tags;
            var type = tags.get("type");

            if (!"multipolygon".equals(type))
                continue;

            List<? extends OsmElement> oc = osmRelation.children;
            for (int i = 0, ocSize = oc.size(); i < ocSize; i++) {
                var e1 = oc.get(i);

                if (e1 == null || e1.getClass() != OsmWay.class)
                    return this;

                var w1 = (OsmWay) e1;
                if (w1.isLoop())
                    return this;

                {
                    ListIterator<? extends OsmElement> ii = osmRelation.children.listIterator(i);
                    while (ii.hasNext()) {
                        var e2 = ii.next();
                        if (e2 == null || e2.getClass() != OsmWay.class)
                            continue;

                        var w2 = (OsmWay) e2;

                        if (w1.isFollowedBy(w2)) {
                            w1.addOsmWay(w2);
                            ii.remove();
                        }
                    }
                }
            }
        }

        return this;
    }


    public boolean isEmpty() {
        return this.nodes.isEmpty() && this.ways.isEmpty() && this.relations.isEmpty();
    }

    public void clear() {
        nodes.clear();
        ways.clear();
        relations.clear();
    }

    public void print() {
        printOsmNodes(nodes.values());
        printOsmWays(ways);
        printOsmRelations(relations);
    }

    private static void printTags(Map<String, String> tags, int indent) {
        for (var stringStringEntry : tags.entrySet()) {
            var v = stringStringEntry.getValue();
            printIndent(indent);
            System.out.printf("%s=%s\n", stringStringEntry.getKey(), v);
        }
    }

    private static void printOsmNodes(Iterable<? extends OsmElement> osmNodes) {
        printOsmNodes(osmNodes, 0);
    }

    private static void printOsmNodes(Iterable<? extends OsmElement> osmNodes, int indent) {
        for (OsmElement osmNode : osmNodes) {
            printOsmNode((OsmNode) osmNode, indent);
        }
    }

    private static void printOsmNode(OsmNode osmNode, int indent) {
        var pos = osmNode.pos;
        printIndent(indent);
        System.out.printf("<node id=%s, lat=%f, lon=%f>\n", osmNode.id, pos.getLatitude(), pos.getLongitude());
        printTags(osmNode.tags, indent + 1);
    }

    private static void printOsmWays(Iterable<OsmWay> osmWays) {
        printOsmWays(osmWays, 0);
    }

    private static void printOsmWays(Iterable<OsmWay> osmWays, int indent) {
        for (var osmWay : osmWays) {
            printOsmWay(osmWay, indent);
        }
    }

    private static void printOsmWay(OsmWay osmWay, int indent) {
        printIndent(indent);
        System.out.printf("<way id=%s>\n", osmWay.id);
        printOsmNodes(osmWay.children, indent + 1);
        printTags(osmWay.tags, indent + 1);
    }

    private static void printOsmRelations(Iterable<OsmRelation> osmRelations) {
        printOsmRelations(osmRelations, 0);
    }

    private static void printOsmRelations(Iterable<OsmRelation> osmRelations, int indent) {
        for (var osmRelation : osmRelations) {
            printOsmRelation(osmRelation, indent);
        }
    }

    private static void printOsmRelation(OsmRelation osmRelation, int indent) {
        printIndent(indent);
        System.out.printf("<relation id=%s>\n", osmRelation.id);
        printOsmElements(osmRelation.children, indent + 1);
        printTags(osmRelation.tags, indent + 1);
    }

    private static void printOsmElements(Iterable<? extends OsmElement> osmElements, int indent) {
        for (OsmElement osmElement : osmElements) {
            if (osmElement.getClass() == OsmNode.class) {
                printOsmNode((OsmNode) osmElement, indent);
            } else if (osmElement.getClass() == OsmWay.class) {
                printOsmWay((OsmWay) osmElement, indent);
            } else if (osmElement.getClass() == OsmRelation.class) {
                printOsmRelation((OsmRelation) osmElement, indent);
            }
        }
    }

    private static void printIndent(int indent) {
        for (var i = 0; i < indent; i++) {
            System.out.print("  ");
        }
    }

    private static void printNode(int indent, Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }

        for (var i = 0; i < indent; i++) {
            System.out.print("  ");
        }
        System.out.print(node.getNodeName());

        var nodeMap = node.getAttributes();
        for (var i = 0; i < nodeMap.getLength(); i++) {
            var attr = nodeMap.item(i);
            System.out.print(", " + attr.getNodeName() + '=' + attr.getNodeValue());
        }
        System.out.println();

        var childNodes = node.getChildNodes();
        var l = childNodes.getLength();
        for (var i = 0; i < l; i++) {
            printNode(indent + 1, childNodes.item(i));
        }
    }



}
