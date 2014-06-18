package org.edgeleap.networks.args;

import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;

public interface AHomology {
	@Option(description = "Bridgedb connection strings to idmapper that contains the homology mappings.")
	public List<String> getHomology();
	public boolean isHomology();
	
	@Option(description = "Bridgedb connection strings to idmapper that contains mappings for the homology source species.")
	public List<String> getHomologySource();
	public boolean isHomologySource();
}