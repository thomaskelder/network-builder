package org.edgeleap.networks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.edgeleap.networks.args.AHelp;
import org.edgeleap.networks.args.AIDMapper;
import org.edgeleap.networks.args.DIDMapper;
import org.edgeleap.networks.graph.AttributeName;
import org.edgeleap.networks.graph.GmlWriter;
import org.edgeleap.networks.graph.Graph;
import org.edgeleap.networks.graph.Graph.Edge;
import org.edgeleap.networks.graph.Graph.Node;
import org.edgeleap.networks.graph.InMemoryGraph;
import org.edgeleap.networks.graph.Neo4jException;
import org.edgeleap.networks.graph.Neo4jWriter;
import org.edgeleap.networks.graph.XGMMLWriter;
import org.pathvisio.core.util.Utils;

import uk.co.flamingpenguin.jewel.cli.CliFactory;
import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * Import interactions from stitch / string
 *
 */
public class StringToNetwork {
	private final static Logger log = Logger.getLogger(StringToNetwork.class.getName());
	
	static final Pattern zeroPattern = Pattern.compile("^0*");
	static Map<Organism, String> species2taxid = new HashMap<Organism, String>();
	
	static {
		BioDataSource.init();
		species2taxid.put(Organism.MusMusculus, "10090");
		species2taxid.put(Organism.HomoSapiens, "9606");
		species2taxid.put(Organism.RattusNorvegicus, "10116");
	}
	
	int minScore;
	
	Set<String> excludeSources = new HashSet<String>();
	
	DataSource ensDs;
	DataSource[] targetDs;
	
	Organism organism;
	String taxId;
	
	IDMapper idm;
	
	Map<String, String> protein2gene;
	
	public StringToNetwork(Organism org, IDMapper idm, Map<String, String> protein2gene) {
		organism = org;
		taxId = species2taxid.get(organism);
		ensDs = BioDataSource.ENSEMBL;
		targetDs = new DataSource[] { ensDs, BioDataSource.CHEBI };
		
		this.idm = idm;
		this.protein2gene = protein2gene;
	}
	
	public void setTargetDs(DataSource[] targetDs) {
		this.targetDs = targetDs;
	}
	
	public void setMinScore(int minScore) {
		this.minScore = minScore;
	}
	
	public void setExcludeSources(Collection<String> exclude) {
		excludeSources.clear();
		excludeSources.addAll(exclude);
		excludeSources.remove("");
	}
	
	private static String getFromArrayBounds(String[] array, int index, String empty) {
		if(array.length < (index-1)) {
			return array[index];
		} else {
			return empty;
		}
	}
	
	public InMemoryGraph readInteractions(File inFile) throws IDMapperException, IOException {
		InMemoryGraph graph = new InMemoryGraph();
		graph.setDirected(true);
		graph.setTitle("STRING: " + inFile.getName());
		
		Set<Set<Xref>> directedEdges = new HashSet<Set<Xref>>();
		
		//Read the STITCH interactions
		BufferedReader in = new BufferedReader(new FileReader(inFile));
		String line = in.readLine(); //Skip header
		//int srcNull = 0;
		
		
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t", 8);
			String a = cols[0];
			String b = cols[1];
			boolean directed = "1".equals(cols[4]);
			
			double score = Double.parseDouble(cols[5]);
			
			if(score < minScore) {
				continue;
			}
			
			Set<Xref> xas = getXref(a);
			Set<Xref> xbs = getXref(b);

			if(xas == null) continue;
			if(xbs == null) continue;
			
			//Add the interactions
			for(Xref xa : xas) {
				Node na = graph.addNode("" + xa);
				for(Xref xb : xbs) {
					Node nb = graph.addNode("" + xb);
					String type = cols[2];
					//Source can be in col 6 or 7
					String source = getFromArrayBounds(cols, 7, null);
					if(source == null || "".equals(source)) source = getFromArrayBounds(cols, 6, null);
					if(excludeSources.contains(source)) {
						log.info("Skipping interaction:\n" + line);
						continue;
					}
					
					List<Edge> addedEdges = new ArrayList<Edge>();
					Set<Xref> endpoints = new HashSet<Xref>();
					endpoints.add(xa);
					endpoints.add(xb);
					if(directed || xa.equals(xb)) {
						Edge e = graph.addEdge(xa + "(" + type + "|" + source + ")" + xb, na, nb);
						e.setAttribute(AttributeName.Directed.name(), "true");
						directedEdges.add(endpoints);
					} else {
						//Check if a directed edge with endpoints xa and xb already exists
						//Don't overwrite with less specific undirected edge.
						if(directedEdges.contains(endpoints)) continue;
						
						Edge e1 = graph.addEdge(xa + "(" + type + "|" + source + ")" + xb, na, nb);
						Edge e2 = graph.addEdge(xb + "(" + type + "|" + source + ")" + xa, nb, na);
						addedEdges.add(e1);
						addedEdges.add(e2);
						e1.setAttribute(AttributeName.Directed.name(), "false");
						e2.setAttribute(AttributeName.Directed.name(), "false");
					}
					for(Edge edge : addedEdges) {
						edge.setAttribute(AttributeName.Interaction.name(), type);
						edge.setAttribute(AttributeName.Source.name(), source);
						edge.setAttribute("stitch_string_score", "" + score);
					}
				}
			}
		}
		log.info("Imported " + graph.getNodes().size() + " nodes and " + graph.getEdges().size() + " edges.");
		in.close();
		return graph;
	}
	
	public static void main(String[] args) {
		try {
			Args pargs = CliFactory.parseArguments(Args.class, args);
			DIDMapper didm = new DIDMapper(pargs);
			
			Organism species = Organism.fromLatinName(pargs.getSpecies());
			
			//Create ensembl protein -> gene mappings
			Map<String, String> protein2gene = new HashMap<String, String>();
			for(File f : pargs.getEns()) protein2gene.putAll(readEnsemblMappings(f));
			
			StringToNetwork importer = new StringToNetwork(species, didm.getIDMapper(), protein2gene);
				
			if(pargs.getExcludeSources() != null) {
				importer.setExcludeSources(pargs.getExcludeSources());
				log.info("Excluding sources: " + pargs.getExcludeSources());
			}
			
			importer.setMinScore(pargs.getMinScore());
			importer.setTargetDs(didm.getDataSources());
			InMemoryGraph graph = importer.readInteractions(pargs.getIn());
			
			if(pargs.getOut().getName().endsWith(".gml")) {
				writeGml("" + pargs.getOut(), graph);
			} else if(pargs.getOut().getName().endsWith(".gml")) {
				writeXgmml("" + pargs.getOut(), graph);
			} else if(!pargs.getNeo4jConfig().isEmpty()){
				
				writeNeo4j("" + pargs.getNeo4jConfig(), graph);
				
			} else {
				
				
					writeGml(pargs.getOut() + ".gml", graph);
					writeXgmml(pargs.getOut() + ".xgmml", graph);

			}
		} catch(Exception e) {
			log.log(Level.SEVERE, "Fatal error", e);
			e.printStackTrace();
		}
	}
	
	Set<Xref> getXref(String id) throws IDMapperException {
		//Find out if id is protein or metabolite
		if(id.startsWith("CID")) {
			//Remove CID1 (for "flat" compounds)
			id = id.replace("CID1", "");
			//Remove the CID part
			id = id.replace("CID", "");
			//Remove leading zeros
			id = removeLeadingZeros(id);
			Xref x = new Xref(id, BioDataSource.PUBCHEM_COMPOUND);
			if(!x.getDataSource().equals(targetDs)) {
				return idm.mapID(x, targetDs);
			} else {
				return Utils.setOf(x);
			}
		} else {
			//Id is of the form taxcode.identifier
			if(!id.startsWith(taxId)) return null; //Skip other species
			int dot = id.indexOf('.');
			id = id.substring(dot + 1, id.length());
			//Assume it's an ensembl id for now and complain if not
			if(id.startsWith("ENS")) {
				//Find the gene id for the protein id
				String gid = protein2gene.get(id);
				if(gid != null) {
					Xref x = new Xref(gid, ensDs);
					if(ensDs.equals(targetDs)) {
						return Utils.setOf(x);
					} else {
						return idm.mapID(x, targetDs);
					}
				} else {
					log.warning("Couldn't find ensembl gene for protein " + id);
				}
 			} else {
 				log.warning("Non-ensembl identifier '" + id + "', not sure what to do with it...");
 			}
		}
		return null;
	}
	
	static Map<String, String> readEnsemblMappings(File f) throws IOException {
		Map<String, String> protein2gene = new HashMap<String, String>();
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line = in.readLine(); //Skip header
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t", 2);
			if("".equals(cols[1]) || "".equals(cols[0])) continue;
			
			protein2gene.put(cols[1], cols[0]);
		}
		in.close();
		return protein2gene;
	}
	
	static String removeLeadingZeros(String s) {
	    Matcher m = zeroPattern.matcher(s);
	    return m.replaceAll("");
	}
	
	private static void writeGml(String f, InMemoryGraph g) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(new File(f));
		GmlWriter.write(g, out);
		out.close();
	}
	
	private static void writeXgmml(String f, InMemoryGraph g) throws IOException {
		PrintWriter out = new PrintWriter(new File(f));
		XGMMLWriter.write(g, out);
		out.close();
	}
	
	private static void writeNeo4j(String f, Graph g) throws Neo4jException {
		Neo4jWriter.write(g, f);
	}
	
	private interface Args extends AIDMapper, AHelp {
		@Option(description = "The path to the stitch 'actions.detailed' file.")
		File getIn();
		
		@Option(description = "The file to write the imported network to")
		File getOut();
		
		@Option(defaultValue = "400", description = "The minimum score an interaction should have to be included.")
		int getMinScore();
		
		@Option(description = "The path(s) to the file(s) that contains ensembl gene -> protein annotations (exported from BioMART).")
		List<File> getEns();
		
		@Option(description = "The species to import (latin name, e.g. Mus musculus).")
		String getSpecies();
		
		@Option(defaultValue = "", description = "Sources to exclude.")
		List<String> getExcludeSources();
		
		@Option(description = "neo4j config", defaultValue = "")
		String getNeo4jConfig();
	}
}
