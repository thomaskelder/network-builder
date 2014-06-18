package org.edgeleap.networks.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InMemoryGraph extends InMemoryAttributeHolder implements Graph {
	String title = "";
	boolean directed = false;
	
	Map<String, Node> nodes = new HashMap<String, Node>();
	Map<String, Edge> edges = new HashMap<String, Edge>();

	@Override
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public String getTitle() {
		return title;
	}
	
	@Override
	public boolean isDirected() {
		return directed;
	}
	
	@Override
	public void setDirected(boolean directed) {
		this.directed = directed;
	}
	
	@Override
	public Node addNode(String id) {
		Node n = nodes.get(id);
		if(n == null) { 
			n = new InMemoryNode(id);
			nodes.put(id, n);
		}
		return n;
	}
	
	@Override
	public Edge addEdge(String id, Node src, Node tgt) {
		Edge e = edges.get(id);
		if(e == null) {
			e = new InMemoryEdge(id, src, tgt);
			edges.put(id, e);
		}
		return e;
	}
	
	@Override
	public Node getNode(String id) { return nodes.get(id); }
	
	@Override
	public Collection<Node> getNodes() { return nodes.values(); }

	@Override
	public Collection<Edge> getEdges() { return edges.values(); }
	
	public class InMemoryNode extends InMemoryAttributeHolder implements Node{
		String id;
		
		public InMemoryNode(String id) {
			this.id = id;
		}
		
		public String getId() {
			return id;
		}
		
		public int hashCode() {
			return id.hashCode();
		}
	}
	
	public class InMemoryEdge extends InMemoryAttributeHolder implements Edge {
		String id;
		Node src;
		Node tgt;
		
		public InMemoryEdge(String id, Node src, Node tgt) {
			this.id = id;
			this.src = src;
			this.tgt = tgt;
		}
		
		public Node getSrc() {
			return src;
		}
		
		public Node getTgt() {
			return tgt;
		}
		
		public String getId() {
			return id;
		}
	}
}
