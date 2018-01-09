package spacegraph.geo;

import jcog.list.FasterList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import spacegraph.geo.data.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class OsmReader {

    final Osm osm;

    private String filename;

    OsmReader(String filename) {
        setFilename(filename);
        osm = new Osm();
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    Osm parse() throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setIgnoringComments(true);
//        factory.setIgnoringElementContentWhitespace(true);
//        factory.setXIncludeAware(false);


        DocumentBuilder documentBuilder = factory.newDocumentBuilder();

        InputStream fis = new FileInputStream(filename);

        if (filename.endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        }
        Document document = documentBuilder.parse(fis);


        osm.clear();


        Collection<Element> relationElements = new FasterList<>();


        NodeList childNodes = document.getDocumentElement().getChildNodes();
        int cl = childNodes.getLength();
        //System.out.println("stage A : " + cl + " child nodes");

        Map<String, OsmNode> nodes = new HashMap(128 * 1024);
        Map<String, OsmRelation> relations = new HashMap(64 * 1024);
        Map<String, OsmWay> ways = new HashMap(64 * 1024);

        for (int i = 0; i < cl; i++) {
            Node childNode = childNodes.item(i);

            switch (childNode.getNodeName()) {
                case "bounds": {
                    osm.bounds = new OsmBounds((Element) childNode);
                    break;
                }
                case "node": {
                    Element childElement = (Element) childNode;
                    String id = childElement.getAttribute("id");

                    Map<String, String> osmTags = null;
                    NodeList nodeChildren = ((Element) childNode).getElementsByTagName("tag") /*childNode.getChildNodes()*/;
                    int nnc = nodeChildren.getLength();
                    for (int j = 0; j < nnc; j++) {
                        Node nodeChild = nodeChildren.item(j);
                        /*if ("tag".equals(nodeChild.getNodeName()))*/
                        {
                            Element ne = (Element) nodeChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap(1);
                            osmTags.put(ne.getAttribute("k"), ne.getAttribute("v"));
                        }
                    }

                    OsmNode oo = new OsmNode(id, new GeoCoordinate(childElement), osmTags);
                    nodes.put(id, oo);
                    osm.nodes.add(oo);

                    break;
                }
                case "way": {
                    Element childElement = (Element) childNode;
                    String id = childElement.getAttribute("id");

                    List<OsmNode> refOsmNodes = new FasterList<>();
                    Map<String, String> osmTags = null;

                    NodeList wayChildren = childNode.getChildNodes();
                    int l = wayChildren.getLength();
                    for (int j = 0; j < l; j++) {
                        Node wayChild = wayChildren.item(j);
                        String node = wayChild.getNodeName();
                        if ("nd".equals(node)) {
                            Element wayChildElement = (Element) wayChild;
                            String refId = wayChildElement.getAttribute("ref");
                            refOsmNodes.add(nodes.get(refId));
                        } else if ("tag".equals(node)) {
                            Element nodeChildElement = (Element) wayChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap<>(1);
                            osmTags.put(nodeChildElement.getAttribute("k"), nodeChildElement.getAttribute("v"));
                        }
                    }

                    OsmWay ow = new OsmWay(id, refOsmNodes, osmTags);
                    ways.put(id, ow);
                    osm.ways.add(ow);

                    break;
                }
                case "relation":
                    Element childElement = (Element) childNode;
                    String id = childElement.getAttribute("id");

                    NodeList relationChildren = childElement.getElementsByTagName("tag");
                    Map<String, String> osmTags = null;
                    int l = relationChildren.getLength();
                    for (int j = 0; j < l; j++) {
                        Node relationChild = relationChildren.item(j);
                        /*if ("tag".equals(relationChild.getNodeName()))*/
                        {
                            Element e = (Element) relationChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap<>(1);
                            osmTags.put(e.getAttribute("k"), e.getAttribute("v"));
                        }
                    }

                    OsmRelation or = new OsmRelation(id, null, osmTags);
                    relations.put(id, or);
                    osm.relations.add(or);
                    relationElements.add(childElement);
                    break;
            }
        }

        //System.out.println("2");

        // Relation 2nd pass
        for (Element relationElement : relationElements) {
            String id = relationElement.getAttribute("id");

            OsmRelation osmRelation = relations.get(id);
//            List<OsmElement> osmMembers = new FasterList<>();

            Map<String, String> tags = osmRelation.tags;
            String highway, natural, building, building_part, landuse;
            if (tags.isEmpty()) {
                highway = natural = building = building_part = landuse = null;
            } else {
                highway = tags.get("highway");
                natural = tags.get("natural");
                building = tags.get("building");
                building_part = tags.get("building:part");
                landuse = tags.get("landuse");
            }

            NodeList relationChildren = relationElement.getElementsByTagName("member"); //getChildNodes();
            List<OsmElement> osmMembers = null;
            int l = relationChildren.getLength();
            for (int j = 0; j < l; j++) {
                Node relationChild = relationChildren.item(j);
                /*if ("member".equals(relationChild.getNodeName()))*/
                {
                    Element relationChildElement = (Element) relationChild;
                    String type = relationChildElement.getAttribute("type");
                    String ref = relationChildElement.getAttribute("ref");
//                    String role = relationChildElement.getAttribute("role");
                    OsmElement member = null;
                    switch (type) {
                        case "node":
                            member = nodes.get(ref);
                            break;
                        case "way":
                            member = ways.get(ref);
                            if (member != null) {
                                if (highway != null) {
                                    member.tag("highway", highway);
                                }
                                if (natural != null) {
                                    member.tag("natural", natural);
                                }
                                if (building != null) {
                                    member.tag("building", building);
                                }
                                if (building_part != null) {
                                    member.tag("building:part", building_part);
                                }
                                if (landuse != null) {
                                    member.tag("landuse", landuse);
                                }
                            }
                            break;
                        case "relation":
                            member = relations.get(ref);
                            break;
                    }
                    if (member != null) {
                        if (osmMembers == null)
                            osmMembers = new FasterList();
                        osmMembers.add(member);
                    }
                }
            }

            if (osmMembers != null)
                osmRelation.addChildren(osmMembers);
        }

        //System.out.println("3");
        // Relation 3rd pass: merge multipolygon
        for (Element relationElement : relationElements) {
            String id = relationElement.getAttribute("id");

            OsmRelation osmRelation = relations.get(id);

            Map<String, String> tags = osmRelation.tags;
            String type = tags.get("type");

            if (!"multipolygon".equals(type))
                continue;

            List<? extends OsmElement> oc = osmRelation.children();
            int s = oc.size();
            for (int i = 0; i < s; i++) {
                OsmElement e1 = oc.get(i);

                if (e1 == null || e1.getClass() != OsmWay.class)
                    continue;

                OsmWay w1 = (OsmWay) e1;
                if (w1.isClosed())
                    continue;

                repeat:
                {
                    ListIterator<? extends OsmElement> ii = oc.listIterator(i);
                    while (ii.hasNext()) {
                        OsmElement e2 = ii.next();
                        if (e2 == null || e2.getClass() != OsmWay.class)
                            continue;

                        OsmWay w2 = (OsmWay) e2;

                        if (w1.isFollowedBy(w2)) {
                            w1.addOsmWay(w2);
                            ii.remove();
                            break repeat; // loop again
                        }
                    }
                }
            }
        }

        return osm;
    }

    public static void printOsm(Osm osm) {
        printOsmNodes(osm.nodes);
        printOsmWays(osm.ways);
        printOsmRelations(osm.relations);
    }

    static void printTags(Map<String, String> tags, int indent) {
        for (Map.Entry<String, String> stringStringEntry : tags.entrySet()) {
            String v = stringStringEntry.getValue();
            printIndent(indent);
            System.out.printf("%s=%s\n", stringStringEntry.getKey(), v);
        }
    }

    static void printOsmNodes(Iterable<OsmNode> osmNodes) {
        printOsmNodes(osmNodes, 0);
    }

    static void printOsmNodes(Iterable<OsmNode> osmNodes, int indent) {
        for (OsmNode osmNode : osmNodes) {
            printOsmNode(osmNode, indent);
        }
    }

    static void printOsmNode(OsmNode osmNode, int indent) {
        GeoCoordinate geoCoordinate = osmNode.geoCoordinate;
        printIndent(indent);
        System.out.printf("<node id=%s, lat=%f, lon=%f>\n", osmNode.id, geoCoordinate.latitude, geoCoordinate.longitude);
        printTags(osmNode.tags, indent + 1);
    }

    static void printOsmWays(Iterable<OsmWay> osmWays) {
        printOsmWays(osmWays, 0);
    }

    static void printOsmWays(Iterable<OsmWay> osmWays, int indent) {
        for (OsmWay osmWay : osmWays) {
            printOsmWay(osmWay, indent);
        }
    }

    static void printOsmWay(OsmWay osmWay, int indent) {
        printIndent(indent);
        System.out.printf("<way id=%s>\n", osmWay.id);
        printOsmNodes(osmWay.getOsmNodes(), indent + 1);
        printTags(osmWay.tags, indent + 1);
    }

    static void printOsmRelations(Iterable<OsmRelation> osmRelations) {
        printOsmRelations(osmRelations, 0);
    }

    static void printOsmRelations(Iterable<OsmRelation> osmRelations, int indent) {
        for (OsmRelation osmRelation : osmRelations) {
            printOsmRelation(osmRelation, indent);
        }
    }

    static void printOsmRelation(OsmRelation osmRelation, int indent) {
        printIndent(indent);
        System.out.printf("<relation id=%s>\n", osmRelation.id);
        printOsmElements(osmRelation.children(), indent + 1);
        printTags(osmRelation.tags, indent + 1);
    }

    static void printOsmElements(Iterable<? extends OsmElement> osmElements, int indent) {
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

    static void printIndent(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
    }

    void printNode(int indent, Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
        System.out.print(node.getNodeName());

        NamedNodeMap nodeMap = node.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node attr = nodeMap.item(i);
            System.out.print(", " + attr.getNodeName() + '=' + attr.getNodeValue());
        }
        System.out.println();

        NodeList childNodes = node.getChildNodes();
        int l = childNodes.getLength();
        for (int i = 0; i < l; i++) {
            printNode(indent + 1, childNodes.item(i));
        }
    }
}
