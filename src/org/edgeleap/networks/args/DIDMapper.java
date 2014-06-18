package org.edgeleap.networks.args;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.bio.BioDataSource;

public class DIDMapper {
	private final static Logger log = Logger.getLogger(DIDMapper.class.getName());
	
	private IDMapper idm;
	private DataSource[] dataSources;

	public DIDMapper(AIDMapper aidm) throws IDMapperException,
			ClassNotFoundException {
		loadIDMapper(aidm);
	}

	public DataSource[] getDataSources() {
		return dataSources;
	}

	public IDMapper getIDMapper() {
		return idm;
	}

	private void loadIDMapper(AIDMapper aidm) throws IDMapperException,
			ClassNotFoundException {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		Class.forName("org.bridgedb.rdb.IDMapperRdb");
		Class.forName("org.bridgedb.file.IDMapperText");
		BioDataSource.init();

		log.info("Connecting to idmappers");
		if (aidm.isIdm()) {
			IDMapperStack idms = new IDMapperStack();
			idms.setTransitive(false);
			for (String c : aidm.getIdm()) {
				log.info("Connecting to " + c);
				idms.addIDMapper(c);
				idm = idms;
			}
		}

		ArrayList<DataSource> dslist = new ArrayList<DataSource>();
		for (String c : aidm.getDs()) {
			DataSource d = DataSource.getBySystemCode(c);
			if (d == null)
				throw new IllegalArgumentException(
						"Unable to find datasource for system code: " + c);
			dslist.add(d);
		}
		dataSources = dslist.toArray(new DataSource[dslist.size()]);
	}
}