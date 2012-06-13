package org.tno.networks;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.tno.networks.graph.AttributeHolder;
import org.tno.networks.graph.Graph;
import org.tno.networks.graph.Graph.Edge;
import org.tno.networks.graph.Graph.Node;

public class NetworkIDMapper {
	static {
		BioDataSource.init();
	}
	private final static Logger log = Logger.getLogger(NetworkIDMapper.class.getName());
	
	IDMapper idm;
	DataSource[] targetDs;
	
	public NetworkIDMapper(IDMapper idm, DataSource[] targetDs) {
		this.idm = idm;
		this.targetDs = targetDs;
	}
	
	private Xref xrefFromNode(Node n) {
		return new Xref(
				n.getId().split(":", 2)[1], 
				DataSource.getBySystemCode(n.getId().split(":",2)[0])
		);
	}
	
	public Graph mapIDs(Graph g) throws IDMapperException {
		Graph gm = new Graph();
		copyAttributes(g, gm);
		
		Set<Node> tomap = new HashSet<Node>(g.getNodes());
		
		//Copy edges
		for(Edge e : g.getEdges()) {
			Node src = e.getSrc();
			Node tgt = e.getTgt();
			
			//Assumes the xref is a string in the form syscode:identifier
			Xref xsrc = xrefFromNode(src);
			Xref xtgt = xrefFromNode(tgt);
			
			tomap.remove(src);
			tomap.remove(tgt);
			
			if(isValid(xsrc) && isValid(xtgt)) {
				for(Xref xmsrc : idm.mapID(xsrc, targetDs)) {
					Node msrc = nodeFromMapped(src, xmsrc, gm);
					for(Xref xmtgt : idm.mapID(xtgt, targetDs)) {
						Node mtgt = nodeFromMapped(tgt, xmtgt, gm);
						Edge em = gm.addEdge(xmsrc + e.getId() + xmtgt, msrc, mtgt);
						copyAttributes(e, em);
					}
				}
			}
		}
		
		//Copy remaining nodes
		for(Node n : tomap) {
			Xref x = xrefFromNode(n);
			for(Xref xm : idm.mapID(x, targetDs)) nodeFromMapped(n, xm, gm);
		}
		
		return gm;
	}
	
	private boolean isValid(Xref x) {
		return x != null && x.getId() != null && !"".equals(x.getId()) && x.getDataSource() != null;
	}
	
	private Node nodeFromMapped(Node src, Xref mapped, Graph gm) {
		Node nm = gm.addNode(mapped.toString());
		nm.setAttribute("original_xref", src.getId());
		copyAttributes(src, nm);
		return nm;
	}
	
	private void copyAttributes(AttributeHolder src, AttributeHolder tgt) {
		for(String aname : src.getAttributeNames()) {
			tgt.setAttribute(aname, "" + src.getAttribute(aname));
		}
	}
}
