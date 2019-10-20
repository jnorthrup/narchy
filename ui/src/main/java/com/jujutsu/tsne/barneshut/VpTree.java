package com.jujutsu.tsne.barneshut;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VpTree<StorageType> {

	DataPoint [] _items;
	private Node _root;
	private final Distance distance;
	
	VpTree() {
		distance = new EuclideanDistance();
	}

	public VpTree(Distance distance) {
		this.distance = distance;
	}

	public void create(DataPoint [] items) {
		_items = items.clone();
		_root = buildFromPoints(0,items.length);
	}

	public void search(DataPoint target, int k, List<DataPoint> results, List<Double> distances) {


		var heap = new PriorityQueue<HeapItem>(k, (o1, o2) -> -1 * o1.compareTo(o2));


		var tau = Double.MAX_VALUE;
        
        
       _root.search(_root, target, k, heap, tau);
        
        
        results.clear(); 
        distances.clear();
        while(!heap.isEmpty()) {
            results.add(_items[heap.peek().index]);
            distances.add(heap.peek().dist);
            heap.remove();
        }
        
        
        Collections.reverse(results);
        Collections.reverse(distances);
	}

	
    private Node buildFromPoints(int lower, int upper)
	{
		if (upper == lower) {     
			return null;
		}


		var node = createNode();
		node.index = lower;

		if (upper - lower > 1) {


			var i = (int) (ThreadLocalRandom.current().nextDouble() * (upper - lower - 1)) + lower;
			swap(_items, lower, i);


			var median = (upper + lower) / 2;
			nth_element(_items, lower + 1,	median,	upper, new DistanceComparator(_items[lower],distance));

			
			node.threshold = distance(_items[lower], _items[median]);

			
			node.index = lower;
			node.left = buildFromPoints(lower + 1, median);
			node.right = buildFromPoints(median, upper);
		}

		
		return node;
	}
	
	VpTree<StorageType>.Node createNode() {
		return new Node();
	}

	Node getRoot() {
		return _root;
	}
	
	
	static void nth_element(DataPoint [] array, int low, int mid, int high,
							Comparator<DataPoint> distanceComparator) {
		var tmp = new DataPoint[high-low];
        System.arraycopy(array, low, tmp, 0, tmp.length);
		Arrays.sort(tmp, distanceComparator);
        System.arraycopy(tmp, 0, array, low, tmp.length);
	}
	
	static void nth_element(int [] array, int low, int mid, int high) {
		var tmp = new int[high-low];
        System.arraycopy(array, low, tmp, 0, tmp.length);
		Arrays.sort(tmp);
        System.arraycopy(tmp, 0, array, low, tmp.length);
	}

	private double distance(DataPoint dataPoint1, DataPoint dataPoint2) {
		return distance.distance(dataPoint1, dataPoint2);
	}
	private double distanceSq(DataPoint dataPoint1, DataPoint dataPoint2) {
		return distance.distanceSq(dataPoint1, dataPoint2);
	}
	
	private static void swap(DataPoint[] items, int idx1, int idx2) {
		var dp = items[idx1];
		items[idx1] = items[idx2];
		items[idx2] = dp;
	}

	
    static class HeapItem implements Comparable<HeapItem> {
    	final int index;
    	final double dist;
    	HeapItem( int index, double dist) {
    		this.index = index;
    		this.dist = dist; 
    	}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

		@Override
		public int compareTo(HeapItem o) {
			if (this == o) return 0;
    		return Double.compare(dist, o.dist);
		}
    	
    	@Override
    	public String toString() {
    		return "HeapItem (index=" + index + ",dist=" + dist + ')';
    	}
    }

	class Node {
		int index;
		double threshold;
		Node left;
		Node right;
		
		@Override
		public String toString() {
			return "Node(id=" + index + ')';
		}
		
		public Node getLeft() {
			return left;
		}

		public Node getRight() {
			return right;
		}

		
		double search(Node node, DataPoint target, int k, Queue<HeapItem> heap, double _tau)
		{
			if(node == null) return _tau;


			var dist = distance(_items[node.index], target);

			
			if(dist < _tau) {
				if(heap.size() == k) heap.remove();           
				heap.add(new HeapItem(node.index, dist));     
				if(heap.size() == k) _tau = heap.peek().dist; 
			}

			
			if(node.left == null && node.right == null) {
				return _tau;
			}

			
			if(dist < node.threshold) {
				if(dist - _tau <= node.threshold) {         
					_tau = search(node.left, target, k, heap, _tau);
				}

				if(dist + _tau >= node.threshold) {         
					_tau = search(node.right, target, k, heap, _tau);
				}

				
			} else {
				if(dist + _tau >= node.threshold) {         
					_tau = search(node.right, target, k, heap, _tau);
				}

				if (dist - _tau <= node.threshold) {         
					_tau = search(node.left, target, k, heap, _tau);
				}
			}
			return _tau;
		}
	}
}
