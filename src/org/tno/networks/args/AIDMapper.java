package org.tno.networks.args;

import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;

public interface AIDMapper {
	@Option(description = "Bridgedb connection strings to idmappers to use to translate between xrefs of different types.")
	public List<String> getIdm();
	public boolean isIdm();

	@Option(defaultValue = { "L", "Ce" }, description = "The datasource(s) to translate all xrefs to (use system code).")
	public List<String> getDs();
}