package org.tno.networks.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InMemoryAttributeHolder implements AttributeHolder {
	Map<String, String> attributes = new HashMap<String, String>();

	@Override
	public void setAttribute(String name, String value) {
		attributes.put(name, value);
	}

	@Override
	public void appendAttribute(String name, String value) {
		appendAttribute(name, value, "; ");
	}
	
	@Override
	public void appendAttribute(String name, String value, String sep) {
		String curr = attributes.get(name);
		if(curr == null || "".equals(curr)) curr = value;
		else if(!curr.startsWith(value) && !curr.contains(sep + value)) {
			curr += sep + value;
		}
		attributes.put(name, curr);
	}
	
	@Override
	public Object getAttribute(String name) {
		return attributes.get(name); 
	}

	@Override
	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}
}
