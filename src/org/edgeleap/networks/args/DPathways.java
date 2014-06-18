package org.edgeleap.networks.args;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bridgedb.IDMapperException;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.util.FileUtils;
import org.pathvisio.core.util.PathwayParser.ParseException;
import org.xml.sax.SAXException;

public class DPathways {
	private final static Logger log = Logger.getLogger(DPathways.class
			.getName());

	public static final String PROP_PATHWAYID = "pathwayId";
	public static final String PROP_PATHWAYDIR = "pathwayDir";
	
	private Set<File> files;
	private Map<File, Pathway> pathways;
	
	public DPathways(APathways apws) throws SAXException, ParseException, IDMapperException, ConverterException {
		files = new HashSet<File>();
		for (File f : apws.getPathway()) {
			files.addAll(FileUtils.getFiles(f, "gpml", true));
		}
	}

	public Set<File> getFiles() {
		return files;
	}
	
	public Iterable<Pathway> getIterable() {
		return new Iterable<Pathway>() {
			public Iterator<Pathway> iterator() {
				return new Iterator<Pathway>() {
					Iterator<File> itFiles = files.iterator();
					
					public boolean hasNext() {
						return itFiles.hasNext();
					}
					
					public Pathway next() {
						File f = itFiles.next();
						Pathway p = null;
						try {
							p = loadPathway(f);
						} catch (ConverterException e) {
							log.log(Level.SEVERE, "Unable to load pathway " + f, e);
							throw new RuntimeException("Unable to load pathway " + f, e);
						}
						return p;
					}
					
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
	
	public Map<File, Pathway> getPathways() throws SAXException, ParseException, IDMapperException, ConverterException {
		if(pathways == null) loadPathways();
		return pathways;
	}
	
	private void loadPathways()
			throws SAXException, ParseException, IDMapperException, ConverterException {
		log.info("Reading pathways");
		for(File f : files) {
			Pathway p = loadPathway(f);
			pathways.put(f, p);
		}
	}
	
	private Pathway loadPathway(File f) throws ConverterException {
		log.info("Loading pathway " + f);
		Pathway p = new Pathway();
		p.readFromXml(f, true);
		p.getMappInfo().setDynamicProperty(PROP_PATHWAYID, f.getName());
		p.getMappInfo().setDynamicProperty(PROP_PATHWAYDIR, f.getAbsoluteFile().getParentFile().getName());
		
		if("New Pathway".equals(p.getMappInfo().getMapInfoName())) {
			p.getMappInfo().setMapInfoName(f.getName());
		}
		return p;
	}
}