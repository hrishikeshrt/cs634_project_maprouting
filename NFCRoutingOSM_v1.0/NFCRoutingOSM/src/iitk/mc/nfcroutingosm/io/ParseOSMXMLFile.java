package iitk.mc.nfcroutingosm.io;

import iitk.mc.nfcroutingosm.datastruct.GeoNode;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osmdroid.util.GeoPoint;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.support.v4.util.LongSparseArray;
import android.util.Log;

/**
 * Parses .OSM-Files from Openstreetmap.org and creates a graph of nodes and ways
 * in form of a linked list.
 * Each Node is given a tag to decide whether its a part of a street and also some location information if available.
 * 
 * TODO: Relation tags are ignored so far in OSM XML (search for "RELTAG" in this class)
 * TODO each file should have a tag. MainActivity can then check if the map was already parsed and wont reparse it

 * @author Benjamin Schiller
 */
/*
 * USEFUL LINKS:
 * http://www.rgagnon.com/javadetails/java-0573.html
 * http://stackoverflow.com/questions/8408504/how-to-parse-a-string-containing-xml-in-java-and-retrieve-the-value-of-the-root
 * http://stackoverflow.com/questions/562160/in-java-how-do-i-parse-xml-as-a-string-instead-of-a-file
 * !!! http://forum.openstreetmap.org/viewtopic.php?pid=213212 !!!
 * https://code.google.com/p/osm-parser/source/browse/trunk/osm-parser/src/br/zuq/osm/parser/OSMParser.java?spec=svn4&r=4
 * 
 */
public class ParseOSMXMLFile {

	private static String TAG = "ParseOSMXMLFile";
	private InputStream in;


	public ParseOSMXMLFile(InputStream in){
		this.in = in;
	}

	public LongSparseArray<GeoNode> parse(){
		//		File sdcard = Environment.getExternalStorageDirectory();
		//		File file = new File(sdcard, "NFCRoutingOSM/OneStreetExample.osm");
		LongSparseArray<GeoNode> nodes = new LongSparseArray<GeoNode>();

		try {
			/* Needed Classes for OSM XML parsing */
			Document doc;
			DocumentBuilder builder;
			NodeList nodesList;

			/* parse inputstream holding .OSM data */
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = builder.parse(in);

			/* get all the child notes of <osm> into nodesList */
			nodesList = doc.getChildNodes().item(0).getChildNodes();

			GeoNode geoNode;

			/* Start iterating over all the chils (nodes, ways, members, ...) */
			for (int i = 0; i < nodesList.getLength(); i++){
				Node n = nodesList.item(i);

				/* Found a note -> save as new GeoNode and put into the LongSparseArray for reference */
				if (n.getNodeName().equals("node")){
					long nodeID = Long.parseLong(n.getAttributes().getNamedItem("id").getNodeValue());
					geoNode = new GeoNode(
							nodeID, 
							Double.parseDouble(n.getAttributes().getNamedItem("lat").getNodeValue()),
							Double.parseDouble(n.getAttributes().getNamedItem("lon").getNodeValue()));
					nodes.put(nodeID, geoNode);
				}
				/* Found a way -> iterate over all <nd>-Tags, which are a list
				 * of nodes. Starting from node i+1, add node i+1 as successor of node i */
				if (n.getNodeName().equals("way")){									
					NodeList wayNodes = n.getChildNodes();
					Node wayNode;
					Node prevWayNode;
					//					Node lastWayNode = n;
					//					int x = 0;
					boolean isStreet = false;

					/* TODO: Maybe check parsing so that those null lines cant exist?
					 * Parsing produces one extra child between each child which is null.
					 * To avoid an NullPointerException the loop must only go to item at index 3. It then
					 * looks at its predecessor at index-2 = 1, which is the last. At index 0 a Null-Item sits. */
					/*
					 * TODO adding the way back for a one-directional route is wrong, which should be considered 
					 */

					String tag = "";
					String name = "";
					for (int j = (wayNodes.getLength()-1); j > 2; j--){
						wayNode = wayNodes.item(j);

						/* Check tag first => street or cycleway? If not, way is discarded. If yes,
						 * GeoNodes are connected to the viewed way */
						if (wayNode.getNodeName().equals("tag")){		
							String value = wayNode.getAttributes().getNamedItem("k").getNodeValue();
							
							/** Problem: Some ways just dont hold TAG information, although they are ways.
							 * Under these circumstances waterways should be just excluded
							 */

							if (value.equals("cables") || value.equals("operator") || value.equals("power")
									|| value.equals("waterway") || value.equals("voltage") || value.equals("building")) isStreet = false;
							else isStreet = true;
							if (value.equals("highway")) tag = "highway";
							if (value.equals("name")) name = wayNode.getAttributes().getNamedItem("v").getNodeValue();
						}
						else if (wayNode.getNodeName().equals("nd") && isStreet){
							/* add nodes so that way in descending order is created */
							prevWayNode = wayNodes.item(j-2);
							geoNode = nodes.get(Long.parseLong(prevWayNode.getAttributes().getNamedItem("ref").getNodeValue()));
							geoNode.addNextNode(nodes.get(Long.parseLong(wayNode.getAttributes().getNamedItem("ref").getNodeValue())));
							/* geoNodes can be part of a lot of ways (crossings). Don't overwrite with an empty String */
							if (geoNode.getTag().equals("")) geoNode.setTag(tag);
							if (geoNode.getName().equals("")) geoNode.setName(name);
							
							
							/* add notes so that way in ascending order is created (NOT FOR ONE-DIRECTIONAL ROADS!!!)  */
							geoNode = nodes.get(Long.parseLong(wayNode.getAttributes().getNamedItem("ref").getNodeValue()));
							geoNode.addNextNode(nodes.get(Long.parseLong(prevWayNode.getAttributes().getNamedItem("ref").getNodeValue())));
							if (geoNode.getTag().equals("")) geoNode.setTag(tag);
							if (geoNode.getName().equals("")) geoNode.setName(name);
							

						}
						else if (wayNode.getNodeName().equals("nd") && !isStreet) break;
					}

				} 
				if (n.getNodeName().equals("relation")){
					//RELTAG
				} 
			}


		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "size: " + nodes.size());

		return nodes;
	}

	/**
	 * Returns the closest GeoNode to the given position, that is included in the 
	 * Array of GeoNodes also provided
	 * @param nodeList LongSparseArray of GeoNodes, parsed in by the ParseOSMXMLFile-Class
	 * @param pos the reference position
	 * @return the closest GeoNode
	 */
	public GeoNode getClosestGeoNode(LongSparseArray<GeoNode> nodeList, GeoPoint pos){

		/* Get the valueSet of the parsed List of Nodes and calculate the closest
		 * GeoNode to the GeoPoint given */
		long key = 0;
		GeoNode node;
		GeoNode closestNode = new GeoNode(0, 0, 0);
		int distance = Integer.MAX_VALUE;
		for (int i = 0; i < nodeList.size(); i++){
			key = nodeList.keyAt(i);
			node = nodeList.get(key);
			
			// otherwise it might be the node of a building and no way can be found
			if (node.getTag().equals("highway")){ 
				if (i == 0){
					closestNode = node;
					distance = closestNode.getGPt().distanceTo(pos);
				}
				if (i > 0 && node.getGPt().distanceTo(pos) < distance){
					closestNode = node;
					distance = node.getGPt().distanceTo(pos);
				}
			}
		}
		return closestNode;
	}

}

/**
 * Snippets of this class
 */
/* Old Code for parsing OSM XML */
//			while ((line = br.readLine()) != null) { 
//				//if (line.startsWith("<")) Log.d(TAG, line + "\n");
//				if (line.startsWith("<?xml") || line.startsWith("<osm") || line.startsWith(" <bounds")); // not important
//				if (line.startsWith(" <node")){
//					//String[] idString = line.split("id=\"[0-9]+\"");
//					node = new Node(id, lat, lon); // regular expressions
//					nodes.put(id, node);
//					id++; //  delete
//				}
//				if (line.startsWith(" <way")){
//					int i = 0;
//					//	Log.d(TAG, "way found");
//					while ((line = br.readLine()) != null && !line.startsWith(" </way")){
//						//	Log.d(TAG, "way while " + i);
//						if (i == 0) lastNode = nodes.get(1); //  regular expressions
//						else {
//							node = nodes.get(1); //  regular expressions
//							lastNode.addNextNode(node);
//							lastNode = node;
//						}
//						i++;
//					}
//				}
//				if (line.startsWith(" <member")){ //
//					// read data, ...
//				}
//			}
//			br.close();
//		} catch (IOException e) {
//
//			e.printStackTrace();
//		}

/* doc builder snippet */
//		try {
//			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//			String line;
//			Node lastNode = new Node(0, 0, 0);
//			Node node = new Node(0, 0, 0); 
//			int id = 0;
//			double lat = 1.0;
//			double lon = 1.0;

/* another doc builder snippet */
// 
//				DocumentBuilderFactory dbf =
//						DocumentBuilderFactory.newInstance();
//				DocumentBuilder db = dbf.newDocumentBuilder();
////				InputSource is = new InputSource();
////				is.setCharacterStream(new StringReader(testString));
//
//				Document doc = db.parse(in);
//				NodeList nodesL = doc.getDocumentElement().getChildNodes();
//				
//			
//				Log.d(TAG, nodesL.item(10).toString());