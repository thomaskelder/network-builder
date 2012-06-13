package org.tno.networks.args;

import uk.co.flamingpenguin.jewel.cli.Option;

public interface AHelp {
	@Option(helpRequest = true, shortName = "h")
	boolean getHelp();
}