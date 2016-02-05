package iitk.mc.nfcroutingosm.algo;

import iitk.mc.nfcroutingosm.datastruct.GeoNode;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.osmdroid.util.GeoPoint;

import android.support.v4.util.LongSparseArray;
import android.util.Log;
/**
 * Algorithms and helpful links, modified and partly taken from: 
 * 	http://www.peachpit.com/articles/article.aspx?p=101142
 * 	http://snipplr.com/view/90681/a-pathfinding-example/
 * 
 * @author Benjamin Schiller
 * 
 * As estimated distance for the A-Star algorithm the direct distance by air is used.
 *
 */
public class RoutingAlgorithms {

	private LongSparseArray<GeoNode> nodesList;
	private GeoNode start;
	private GeoNode target;
	private GeoNode current;

	public RoutingAlgorithms(LongSparseArray<GeoNode> nodesList, GeoNode current, GeoNode start, GeoNode target){
		this.nodesList = nodesList;
		this.start = start;
		this.target = target;
		this.current = current;
	}



	/**
    A simple priority list, also called a priority queue.
    Objects in the list are ordered by their priority,
    determined by the object's Comparable interface.
    The highest priority item is first in the list.
	 */
	public static class PriorityList extends LinkedList {

		public void add(Comparable object) {
			for (int i=0; i<size(); i++) {
				if (object.compareTo(get(i)) <= 0) {
					add(i, object);
					return;
				}
			}
			addLast(object);
		}
	}


	/**
    Find the path from the start node to the end node. A list
    of AStarNodes is returned, or null if the path is not
    found. 
	 */
	public LinkedList<GeoNode> aStarSearch() {
		
		PriorityList openList = new PriorityList();
		LinkedList<GeoNode> closedList = new LinkedList<GeoNode>();

		start.aStarCostFromStart = 0;
		start.aStarEstimatedCostToGoal =
				start.getEstimatedCost(target);
		start.setPathParent(null);
		openList.add(start);

		while (!openList.isEmpty()) {
			GeoNode node = (GeoNode)openList.removeFirst();
			if (node == target) {
				// construct the path from start to goal
				return constructBFSPath(target);
			}

			Iterator<GeoNode> i = node.getNextNodes().keySet().iterator();
			//      List neighbors = node.getNextNodes().keySet().iterator();
			while (i.hasNext()) { 
				GeoNode neighborNode = (GeoNode)i.next();
				boolean isOpen = openList.contains(neighborNode);
				boolean isClosed =
						closedList.contains(neighborNode);
				float costFromStart = node.aStarCostFromStart +
						node.getCost(neighborNode);

				// check if the neighbor node has not been
				// traversed or if a shorter path to this
				// neighbor node is found.
				if ((!isOpen && !isClosed) ||
						costFromStart < neighborNode.aStarCostFromStart)
				{
					neighborNode.setPathParent(node);
					neighborNode.aStarCostFromStart = costFromStart;
					neighborNode.aStarEstimatedCostToGoal =
							neighborNode.getEstimatedCost(target);
					if (isClosed) {
						closedList.remove(neighborNode);
					}
					if (!isOpen) {
						openList.add(neighborNode);
					}
				}
			}
			closedList.add(node);
		}

		// no path found
		return null;
	}


	public LinkedList<GeoNode> breadthFirstSearch() {
		// list of visited nodes
		LinkedList<GeoNode> closedList = new LinkedList<GeoNode>();

		// list of nodes to visit (sorted)
		LinkedList<GeoNode> openList = new LinkedList<GeoNode>();
		openList.add(start);
		start.setPathParent(null);

		while (!openList.isEmpty()) {
			GeoNode node = (GeoNode)openList.removeFirst();

			if (node == target) {
				// path found!
				return constructBFSPath(target);
			}
			else {
				closedList.add(node);

				// add neighbors to the open list
				Iterator<GeoNode> i = node.getNextNodes().keySet().iterator();
				while (i.hasNext()) {
					GeoNode neighborNode = (GeoNode)i.next();
					if (!closedList.contains(neighborNode) &&
							!openList.contains(neighborNode)) 
					{
						neighborNode.setPathParent(node);
						openList.add(neighborNode);
						//				    Log.d("BLA", ""+neighborNode.getId());
					}
				}
			}
		}
		Log.d("BLA", "NULL");
		// no path found
		LinkedList<GeoNode> result =  new LinkedList<GeoNode>();
		result.add(current);
		return result;
	}

	/**
    Construct the path, not including the start node ???????????? TODO ??
	 */
	private LinkedList<GeoNode> constructBFSPath(GeoNode node) {
		LinkedList<GeoNode> geoNodes = new LinkedList<GeoNode>();
		while (node.getPathParent() != null) {
			geoNodes.addFirst(node);
			node = node.getPathParent();
		}
		geoNodes.addFirst(current);
		return geoNodes;
	}
}
