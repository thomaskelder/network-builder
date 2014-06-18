package org.edgeleap.networks.args;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;

public interface APathways {
	@Option(shortName = "p", description = "The path(s) to the GPML files (will look for *.gpml recursively).")
	public List<File> getPathway();
}