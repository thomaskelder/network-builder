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

	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				graphDb.shutdown();
			}
		} );
	}

	// TODO how to deal with credentials?
	public static void write(Graph graph, String location, boolean remote){

		GraphDatabaseService instance = null;

		if(remote){
			instance = new RestGraphDatabase(location);
		} else {
			instance = new GraphDatabaseFactory().newEmbeddedDatabase(location);
		}
		
		registerShutdownHook(instance);

		Index<Node> idIndex = instance.index().forNodes("id");
		Index<Node> propIndex = instance.index().forNodes("property");
	
		Map<String, Node> neonodes = new HashMap<String, Node>();
		
		
		for(Graph.Node n : graph.getNodes()){
			Transaction tx = instance.beginTx();
			try{

				Node nn = instance.createNode();

				nn.setProperty("id", n.getId());
				idIndex.add(nn, "id", n.getId());
				for(String key : n.getAttributeNames()){
					// index them?
					propIndex.add(nn, key, n.getAttribute(key));
					nn.setProperty(key, n.getAttribute(key));
				}

				neonodes.put(n.getId(), nn);
				tx.success();
			} finally {
				tx.finish();
			}
		}
			
		for(Graph.Edge e : graph.getEdges()){
			Transaction tx = instance.beginTx();
			try{
				Node nsrc = neonodes.get(e.getSrc().getId());
				Node ntarget = neonodes.get(e.getTgt().getId());
				
				String relname = (String) e.getAttribute(AttributeName.Interaction.name());
				
				if(relname == null || relname.isEmpty()){
					relname = "unknown";
				}
				
				Relationship rel = nsrc.createRelationshipTo(ntarget, DynamicRelationshipType.withName(relname));
			
				for(String key : e.getAttributeNames()){
					rel.setProperty(key, e.getAttribute(key));
				}
				tx.success();
			} finally {
				tx.finish();
			}
		}

	}

}
