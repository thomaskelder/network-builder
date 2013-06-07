package org.tno.networks.graph;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	private String nodeDefaultIndex;
	private String edgeDefaultIndex;
	
	private Map<String, String> nodeIndexNames = new HashMap<String,String>();
	private Map<String, String> nodeIndexKeys = new HashMap<String, String>();
	
	private Map<String, String> edgeIndexNames = new HashMap<String,String>();
	private Map<String, String> edgeIndexKeys = new HashMap<String, String>();
	
//	private int instanceCount = 0;
//	private int maxInstances = 1;
	
//	private Set<GraphDatabaseService> instances;
	
	private GraphDatabaseService instance = null;

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
			nodeDefaultIndex = indexing.getChildText("node_default_index");
			edgeDefaultIndex = indexing.getChildText("edge_default_index");
			
			List<Element> node_specific_indices = indexing.getChildren("node_specific_index");
			
			for(Element specific_index : node_specific_indices){
				String propertyName = specific_index.getChildText("property_name");
				String indexName = specific_index.getChildText("index_name");
				String indexKey = specific_index.getChildText("index_key");
				
				nodeIndexNames.put(propertyName, indexName);
				nodeIndexKeys.put(propertyName, indexKey);
			}
			
			List<Element> edge_specific_indices = indexing.getChildren("edge_specific_index");
			
			for(Element specific_index : node_specific_indices){
				String propertyName = specific_index.getChildText("property_name");
				String indexName = specific_index.getChildText("index_name");
				String indexKey = specific_index.getChildText("index_key");
				
				edgeIndexNames.put(propertyName, indexName);
				edgeIndexKeys.put(propertyName, indexKey);
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

	public String lookupNodeIndexName(String propertyName) {
		if(nodeIndexNames.containsKey(propertyName)){
			return nodeIndexNames.get(propertyName);
		} else {
			return nodeDefaultIndex;
		}
	}
	
	public String lookupNodeIndexKey(String propertyName) {
		if(nodeIndexKeys.containsKey(propertyName)){
			return nodeIndexKeys.get(propertyName);
		} else {
			return propertyName;
		}
	}
	
	public String lookupEdgeIndexName(String propertyName) {
		if(edgeIndexNames.containsKey(propertyName)){
			return edgeIndexNames.get(propertyName);
		} else {
			return edgeDefaultIndex;
		}
	}
	
	public String lookupEdgeIndexKey(String propertyName) {
		if(edgeIndexKeys.containsKey(propertyName)){
			return edgeIndexKeys.get(propertyName);
		} else {
			return propertyName;
		}
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
