package org.tno.networks.graph;

import java.util.Collection;

public interface Graph extends AttributeHolder {

	public void setTitle(String title);

	public String getTitle();

	public boolean isDirected();

	public void setDirected(boolean directed);

	public Node addNode(String id);

	public Edge addEdge(String id, Node src, Node tgt);

	public Node getNode(String id);

	public Collection<Node> getNodes();

	public Collection<Edge> getEdges();
	
	public interface Node extends AttributeHolder{		
		public String getId();
	}
	
	public interface Edge extends AttributeHolder{
		public Node getSrc();
		
		public Node getTgt();
		
		public String getId();
	}
}