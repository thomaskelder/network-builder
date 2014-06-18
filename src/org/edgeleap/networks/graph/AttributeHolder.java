package org.edgeleap.networks.graph;

import java.util.Set;

public interface AttributeHolder {

	public void setAttribute(String name, String value);

	public void appendAttribute(String name, String value);

	public void appendAttribute(String name, String value, String sep);

	public Object getAttribute(String name);

	public Set<String> getAttributeNames();

}