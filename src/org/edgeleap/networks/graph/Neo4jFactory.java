package org.edgeleap.networks.graph;

import java.io.IOException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.rest.graphdb.RestGraphDatabase;

public class Neo4jFactory {
	
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
	
	private String configName;
	
	private ConnectionType connectionType;
	private String location;
	private String serverConfigFile;
	
	private RelationshipType relationshipTypeAttribute;
		
	private GraphDatabaseService instance = null;
	private Neo4jIndexer indexer = null;

	public Neo4jFactory(String configFile) throws Neo4jException {
		SAXBuilder builder = new SAXBuilder();
		try {
			Document doc =builder.build(configFile);
			
			Element root = doc.getRootElement();
			
			setConfigName(root.getChildText("config_name"));
			
			Element db = root.getChild("db");
			location = db.getChildText("location");
			serverConfigFile = db.getChildText("config_file");
			
			connectionType = ConnectionType.valueOf(db.getChildText("type"));
			
			relationshipTypeAttribute = DynamicRelationshipType.withName(AttributeName.valueOf(root.getChildText("relationship_type_attribute")).name());
			
//			maxInstances = Integer.valueOf(root.getChildText("max_instances"));
//			<max_instances>1</max_instances>
			
			Element indexing = root.getChild("indexing");
			indexer = new Neo4jIndexer(indexing.getChildText("node_default_index"),indexing.getChildText("edge_default_index"));
			
			List<Element> node_specific_indices = indexing.getChildren("node_specific_index");
			
			for(Element specific_index : node_specific_indices){
				String propertyName = specific_index.getChildText("property_name");
				String indexName = specific_index.getChildText("index_name");
				String indexKey = specific_index.getChildText("index_key");
				
				indexer.addNodePropertyMapping(propertyName, indexName, indexKey);
			}
			
			List<Element> edge_specific_indices = indexing.getChildren("edge_specific_index");
			
			for(Element specific_index : node_specific_indices){
				String propertyName = specific_index.getChildText("property_name");
				String indexName = specific_index.getChildText("index_name");
				String indexKey = specific_index.getChildText("index_key");
				
				indexer.addEdgePropertyMapping(propertyName, indexName, indexKey);
			}
			
		} catch (JDOMException | IOException e) {
			throw new Neo4jException(e);
		}
			
	}
	
	public GraphDatabaseService makeServiceInstance() throws Neo4jException {
		
		switch(connectionType){
		case embedded:
			if(serverConfigFile.isEmpty()){
				instance = new GraphDatabaseFactory().newEmbeddedDatabase(location);
			} else {
				instance = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(location).loadPropertiesFromFile(serverConfigFile).newGraphDatabase();
			}
			break;
			
		case rest:
			instance = new RestGraphDatabase(location);
			break;
			
			default:
				throw new Neo4jException("unrecognized connection type!" + connectionType.name());
		
		}
		
		registerShutdownHook(instance);
		
		return instance;
	}
	
	public Neo4jIndexer makeIndexerInstance() throws Neo4jException{
		if(instance == null){
			throw new Neo4jException("instance not ready");
		}
		
		if(indexer == null){
			indexer = new Neo4jIndexer("property", "property");
		}
		
		return indexer;
			
	}
	
	public RelationshipType getRelationshipType(){
		return relationshipTypeAttribute;
	}
	
	public String getConfigName() {
		return configName;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public enum ConnectionType {
		embedded,
		rest
	}
}
