package spacegraph.geo;

import jcog.Texts;
import jcog.list.FasterList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
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

import static jcog.Texts.l;

public enum OsmReader { ;


    public static Osm load(String filename) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
//        factory.setXIncludeAware(false);


        DocumentBuilder documentBuilder = factory.newDocumentBuilder();

        InputStream fis = new FileInputStream(filename);

        if (filename.endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        }
        Document document = documentBuilder.parse(fis);


        Osm osm = new Osm();
        osm.clear();


        Collection<Element> relationElements = new FasterList<>();

        NodeList childNodes = document.getDocumentElement().getChildNodes();
        int cl = childNodes.getLength();
        for (int i = 0; i < cl; i++) {
            Node childNode = childNodes.item(i);

            switch (childNode.getNodeName()) {
                case "bounds": {
                    osm.bounds = new OsmBounds((Element) childNode);
                    break;
                }
                case "node": {
                    Element childElement = (Element) childNode;
                    long id = l(childElement.getAttribute("id"));

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
                    osm.nodes.put(id, oo);


                    break;
                }
                case "way": {
                    Element childElement = (Element) childNode;
                    long id = l(childElement.getAttribute("id"));

                    List<OsmNode> refOsmNodes = new FasterList<>();
                    Map<String, String> osmTags = null;

                    NodeList wayChildren = childNode.getChildNodes();
                    int l = wayChildren.getLength();
                    for (int j = 0; j < l; j++) {
                        Node wayChild = wayChildren.item(j);
                        String node = wayChild.getNodeName();
                        if ("nd".equals(node)) {
                            Element wayChildElement = (Element) wayChild;
                            refOsmNodes.add(
                                    osm.nodes.get(l(wayChildElement.getAttribute("ref")))
                            );
                        } else if ("tag".equals(node)) {
                            Element nodeChildElement = (Element) wayChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap<>(1);
                            osmTags.put(nodeChildElement.getAttribute("k"), nodeChildElement.getAttribute("v"));
                        }
                    }

                    OsmWay ow = new OsmWay(id, refOsmNodes, osmTags);
                    osm.ways.put(id, ow);


                    break;
                }
                case "relation":
                    Element childElement = (Element) childNode;
                    long id = l(childElement.getAttribute("id"));

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
                    osm.relations.put(id, or);
                    relationElements.add(childElement);
                    break;
            }
        }


        // Relation 2nd pass
        for (Element relationElement : relationElements) {
            long id = l(relationElement.getAttribute("id"));

            OsmRelation osmRelation = osm.relations.get(id);

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
                    Element r = (Element) relationChild;
                    String type = r.getAttribute("type");
                    long ref = l(r.getAttribute("ref"));
//                    String role = relationChildElement.getAttribute("role");
                    OsmElement member = null;
                    switch (type) {
                        case "node":
                            member = osm.nodes.get(ref);
                            break;
                        case "way":
                            member = osm.ways.get(ref);
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
                            member = osm.relations.get(ref);
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

        //System.out.println("3");
        // Relation 3rd pass: merge multipolygon
        for (Element relationElement : relationElements) {
            long id = l(relationElement.getAttribute("id"));

            OsmRelation osmRelation = osm.relations.get(id);

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
                            //break repeat; // loop again
                        }
                    }
                }
            }
        }

        return osm;
    }

}
