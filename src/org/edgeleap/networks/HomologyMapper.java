package org.edgeleap.networks;

import java.util.HashSet;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;

public class HomologyMapper {
	IDMapper idmHomology;
	IDMapper idmSource;
	IDMapper idmTarget;
	
	DataSource[] homologySrcDs;

	public HomologyMapper(IDMapper idmHomology, IDMapper idmSource, IDMapper idmTarget) throws IDMapperException {
		this.idmHomology = idmHomology;
		this.idmSource = idmSource;
		this.idmTarget = idmTarget;
		
		homologySrcDs = idmHomology.getCapabilities().getSupportedSrcDataSources().toArray(new DataSource[0]);
	}
	
	/**
	 * Maps xref x by including homology, using the idmHomology IDMapper (containing the mappings between species), 
	 * idmTarget (containing the mappings for the target species) and idmSource (contains mappings for the source species).
	 * First maps x to other datasources using idmSource, then maps those to homologs using idmHomolog, then maps homologs to 
	 * the target datasources defined by dsTarget using idmTarget. 
	 * @throws IDMapperException 
	 */
	public Set<Xref> mapWithHomology(Xref x, DataSource... dsTarget) throws IDMapperException {
		Set<Xref> mapped = new HashSet<Xref>();

		//See if homology mapping is necessary and possible (only if xref doesn't exist in target database)
		if(idmHomology != null && !idmTarget.xrefExists(x)) {
			for(Xref xh : getHomologs(x)) {
				mapped.addAll(idmTarget.mapID(xh, dsTarget));
			}
		} else { //Else, just perform normal mapping without homology
			mapped.addAll(idmTarget.mapID(x, dsTarget));
		}

		return mapped;
	}

	public Set<Xref> getHomologs(Xref x) throws IDMapperException {
		Set<Xref> homologs = new HashSet<Xref>();

		for(Xref xs : idmSource.mapID(x, homologySrcDs)) {
			homologs.addAll(idmHomology.mapID(xs));
		}
		
		return homologs;
	}
}
