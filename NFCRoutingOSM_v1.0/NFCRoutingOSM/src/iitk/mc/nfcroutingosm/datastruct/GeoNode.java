package iitk.mc.nfcroutingosm.datastruct;

import java.util.HashMap;

import org.osmdroid.util.GeoPoint;

/**
 * Represents a Node of a graph in a linkedList created by parsing
 * an OSM XML file
 * 
 *	Alternative distance calculation: http://www.movable-type.co.uk/scripts/latlong.html
 *
 * @author Benjamin Schiller
 *
 */
public class GeoNode {

	private long id;
	private GeoPoint gPt;
	private double lon;
	private double lat;
	private HashMap<GeoNode, Integer> nextNodes;
	private GeoNode pathParent; // for searching only -> will be traversed back to find path
	private String tag = ""; // holds information whether its a highway, etc. If empty, then its no walkable way
	private String name = ""; //holds information about street name
	public float aStarCostFromStart = 0;
	public float aStarEstimatedCostToGoal = 0;

	public GeoNode getPathParent() {
		return pathParent;
	}

	public void setPathParent(GeoNode pathParent) {
		this.pathParent = pathParent;
	}

	/**
	 * Whole costs from start Node to this one
	 * @return
	 */
	public float getCost(){
		return aStarCostFromStart + aStarEstimatedCostToGoal;
	}

	public void setAStartCost(float cost){
		this.aStarCostFromStart = cost;
	}

	public int compareTo(Object other) {
		float thisValue = this.getCost();
		float otherValue = ((GeoNode)other).getCost();

		float v = thisValue - otherValue;
		return (v>0)?1:(v<0)?-1:0; // sign function
	}

	/**
	 * Costs from this particular GeoNode to the given GeoNode
	 * @param node
	 * @return
	 */
	public float getCost(GeoNode node){
		return node.getGPt().distanceTo(this.getGPt());
	}
	
	/**
	 * Costs from this particular GeoNode to the given GeoNode which is most probably
	 * quite far away, since it is the target node
	 * @param node
	 * @return
	 */
	public float getEstimatedCost(GeoNode node){
		return node.getGPt().distanceTo(this.getGPt());
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GeoNode(long id, double lat, double lon) {
		this.id = id;
		this.gPt = new GeoPoint(lat, lon);
		this.lon = lon;
		this.lat = lat;
		this.nextNodes = new HashMap<GeoNode, Integer>();
	}

	public GeoNode(long id, GeoPoint gPt) {
		this.id = id;
		this.gPt = new GeoPoint(lat, lon);
		this.lon = gPt.getLongitude();
		this.lat = gPt.getLatitude();
		this.nextNodes = new HashMap<GeoNode, Integer>();
	}

	public long getId() {
		return id;
	}

	public GeoPoint getGPt() {
		return gPt;
	}



	public void setId(int id) {
		this.id = id;
	}


	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public HashMap<GeoNode, Integer> getNextNodes() {
		return nextNodes;
	}

	public void addNextNode(GeoNode node){
		nextNodes.put(node, this.getGPt().distanceTo(node.getGPt()));
	}




}
