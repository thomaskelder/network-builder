package org.tno.networks.graph;

import java.util.HashMap;
import java.util.Map;

public class Neo4jIndexer {

	private String nodeDefaultIndex;
	private String edgeDefaultIndex;
	
	private Map<String, String> nodeIndexNames = new HashMap<String,String>();
	private Map<String, String> nodeIndexKeys = new HashMap<String, String>();
	
	private Map<String, String> edgeIndexNames = new HashMap<String,String>();
	private Map<String, String> edgeIndexKeys = new HashMap<String, String>();

	public Neo4jIndexer(String nodeDefault, String edgeDefault){
		nodeDefaultIndex = nodeDefault;
		edgeDefault = edgeDefaultIndex;
	}
	
	public void addNodePropertyMapping(String propertyName, String indexName, String indexKey){
		nodeIndexNames.put(propertyName, indexName);
		nodeIndexKeys.put(propertyName, indexKey);
	}
	
	public void addEdgePropertyMapping(String propertyName, String indexName, String indexKey){
		edgeIndexNames.put(propertyName, indexName);
		edgeIndexKeys.put(propertyName, indexKey);
	}
	
	public void setNodeDefaultIndex(String nodeDefaultIndex) {
		this.nodeDefaultIndex = nodeDefaultIndex;
	}

	public void setEdgeDefaultIndex(String edgeDefaultIndex) {
		this.edgeDefaultIndex = edgeDefaultIndex;
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
	
}
