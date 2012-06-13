package org.tno.networks.args;

import java.util.logging.Logger;

import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.bio.BioDataSource;

public class DHomology {
	private final static Logger log = Logger.getLogger(DHomology.class.getName());
	
	private IDMapper idmHomology;
	private IDMapper idmSource;
	
	public DHomology(AHomology ahom) throws IDMapperException,
			ClassNotFoundException {
		loadIDMapper(ahom);
	}

	public IDMapper getIDMapperHomology() {
		return idmHomology;
	}

	public IDMapper getIDMapperSource() {
		return idmSource;
	}
	
	private void loadIDMapper(AHomology ahom) throws IDMapperException,
			ClassNotFoundException {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		Class.forName("org.bridgedb.rdb.IDMapperRdb");
		Class.forName("org.bridgedb.file.IDMapperText");
		BioDataSource.init();

		log.info("Connecting to idmappers for homology mapping");
		if (ahom.isHomology()) {
			IDMapperStack idms = new IDMapperStack();
			idms.setTransitive(true);
			for (String c : ahom.getHomology()) {
				log.info("Connecting to " + c);
				idms.addIDMapper(c);
				idmHomology = idms;
			}
		}
		
		log.info("Connecting to idmappers for homology source species mapping");
		if (ahom.isHomologySource()) {
			IDMapperStack idms = new IDMapperStack();
			idms.setTransitive(true);
			for (String c : ahom.getHomologySource()) {
				log.info("Connecting to " + c);
				idms.addIDMapper(c);
				idmSource = idms;
			}
		}
	}
}