package org.edgeleap.networks;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.edgeleap.networks.args.AHelp;
import org.edgeleap.networks.args.AIDMapper;
import org.edgeleap.networks.args.APathways;
import org.edgeleap.networks.args.DIDMapper;
import org.edgeleap.networks.args.DPathways;
import org.pathvisio.core.data.XrefWithSymbol;
import org.pathvisio.core.util.PathwayParser;
import org.pathvisio.core.util.PathwayParser.ParseException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import uk.co.flamingpenguin.jewel.cli.CliFactory;
import uk.co.flamingpenguin.jewel.cli.Option;

public class PathwaysToSets {
	static {
		BioDataSource.init();
	}
	private final static Logger log = Logger.getLogger(PathwaysToSets.class.getName());
	
	public static Map<String, Set<Xref>> readPathwaySets(Collection<File> gpmlFiles, boolean fullPath, IDMapper idMapper, DataSource... targetDs) throws SAXException, ParseException, IDMapperException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		Set<DataSource> tgts = new HashSet<DataSource>();
		for(DataSource ds : targetDs) tgts.add(ds);

		Map<String, Set<Xref>> sets = new HashMap<String, Set<Xref>>();
		for(File f : gpmlFiles) {
			PathwayParser pp = new PathwayParser(f, xmlReader);
			Set<Xref> xrefs = new HashSet<Xref>();
			for(XrefWithSymbol xs : pp.getGenes()) {
				Xref x = xs.asXref();
				if(tgts.contains(x.getDataSource())) {
					xrefs.add(x);
				} else {
					if(idMapper != null) xrefs.addAll(idMapper.mapID(x, targetDs));
					else throw new IllegalArgumentException(
							"Found xref (" + x + ") that needs to be mapped to target datasources " + tgts + ", but no idMapper specified!"
					);
				}
			}
			sets.put(fullPath ? f.toString() : f.getName(), xrefs);
		}
		return sets;
	}
	
	public static Map<String, String> readPathwayTitles(Collection<File> gpmlFiles, boolean fullPath) throws SAXException, ParseException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		Map<String, String> titles = new HashMap<String, String>();

		for(File f : gpmlFiles) {
			PathwayParser pp = new PathwayParser(f, xmlReader);
			titles.put(fullPath ? f.toString() : f.getName(), pp.getName());
		}
		return titles;
	}
	
	public static void main(String[] args) {
		try {
			Args pargs = CliFactory.parseArguments(Args.class, args);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs);
			
			//Convert pathways to gene sets
			Map<String, Set<Xref>> sets = readPathwaySets(dpws.getFiles(), false, didm.getIDMapper(), didm.getDataSources());
			Map<String, String> titles = readPathwayTitles(dpws.getFiles(), false);
			
			//Write xref lists to txt files
			for(String fn : sets.keySet()) {
				PrintWriter out = new PrintWriter(new File(pargs.getOut(), fn + ".txt"));
				out.println(fn);
				out.println(titles.get(fn));
				for(Xref x : sets.get(fn)) out.println(x);
				out.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
	
	private interface Args extends AIDMapper, AHelp, APathways {
		@Option(shortName = "o", description = "The output path to write the sets to.")
		public File getOut();
	}
}
