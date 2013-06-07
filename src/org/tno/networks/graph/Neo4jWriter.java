package org.tno.networks.graph;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.rest.graphdb.RestGraphDatabase;


public class Neo4jWriter {

	// TODO how to deal with credentials?
	public static void write(Graph graph, String config) throws Neo4jException{
		
		Neo4jFactory factory = new Neo4jFactory(config);
		
		GraphDatabaseService instance = factory.makeServiceInstance();
		
		Map<String, Node> neonodes = new HashMap<String, Node>();
			
		for(InMemoryGraph.Node n : graph.getNodes()){
			Transaction tx = instance.beginTx();
			try{
				Node nn = instance.createNode();

				nn.setProperty("id", n.getId());
				indexProperty("id", n.getId(), instance, factory, nn);
				for(String key : n.getAttributeNames()){
					indexProperty(key, n.getAttribute(key), instance, factory, nn);
					nn.setProperty(key, n.getAttribute(key));
				}

				neonodes.put(n.getId(), nn);
				tx.success();
			} finally {
				tx.finish();
			}
		}
			
		for(InMemoryGraph.Edge e : graph.getEdges()){
			Transaction tx = instance.beginTx();
			try{
				Node nsrc = neonodes.get(e.getSrc().getId());
				Node ntarget = neonodes.get(e.getTgt().getId());
				
				String relname = (String) e.getAttribute(AttributeName.Interaction.name());
				
				if(relname == null || relname.isEmpty()){
					relname = "unknown";
				}
				
				Relationship rel = nsrc.createRelationshipTo(ntarget, DynamicRelationshipType.withName(relname));
				indexProperty("id", e.getId(), instance, factory, rel);
				
				for(String key : e.getAttributeNames()){
					rel.setProperty(key, e.getAttribute(key));
					indexProperty(key, e.getAttribute(key), instance, factory, rel);
				}
				tx.success();
			} finally {
				tx.finish();
			}
		}

	}

	private static void indexProperty(String key, Object value,GraphDatabaseService instance, Neo4jFactory factory, Node n){
		Index<Node> currentIndex = instance.index().forNodes(factory.lookupNodeIndexName(key));
		currentIndex.add(n, factory.lookupNodeIndexKey(key), value);
	}
	
	private static void indexProperty(String key, Object value,GraphDatabaseService instance, Neo4jFactory factory, Relationship r){
		Index<Relationship> currentIndex = instance.index().forRelationships(factory.lookupEdgeIndexName(key));
		currentIndex.add(r, factory.lookupEdgeIndexKey(key), value);
	}

}
