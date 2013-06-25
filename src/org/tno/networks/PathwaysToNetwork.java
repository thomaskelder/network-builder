package org.tno.networks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.GroupStyle;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MPoint;
import org.pathvisio.core.util.Relation;
import org.pathvisio.core.view.MIMShapes;
import org.tno.networks.args.AHelp;
import org.tno.networks.args.AHomology;
import org.tno.networks.args.AIDMapper;
import org.tno.networks.args.APathways;
import org.tno.networks.args.DHomology;
import org.tno.networks.args.DIDMapper;
import org.tno.networks.args.DPathways;
import org.tno.networks.graph.AttributeName;
import org.tno.networks.graph.GmlWriter;
import org.tno.networks.graph.Graph;
import org.tno.networks.graph.Graph.Edge;
import org.tno.networks.graph.Graph.Node;
import org.tno.networks.graph.InMemoryGraph;
import org.tno.networks.graph.XGMMLWriter;

import uk.co.flamingpenguin.jewel.cli.CliFactory;
import uk.co.flamingpenguin.jewel.cli.Option;

public class PathwaysToNetwork {
	static {
		MIMShapes.registerShapes();
		BioDataSource.init();
	}
	private final static Logger log = Logger.getLogger(PathwaysToNetwork.class.getName());
	
	static final String SEP = "; ";
	
	Set<String> ignoreLabels = new HashSet<String>(Arrays.asList(new String[] {
		"",
		"?"
	}));
		
	IDMapper idm;
	DataSource[] targetDs;
	Collection<InteractionType> ignoreInteractions = new HashSet<InteractionType>();
	HomologyMapper homology;
	boolean includeUnmapped = true;
	
	public PathwaysToNetwork(IDMapper idm, DataSource[] targetDs) {
		this.idm = idm;
		this.targetDs = targetDs;
	}
	
	public void setHomology(HomologyMapper homology) {
		this.homology = homology;
	}
	
	InMemoryGraph parseInteractions(Iterable<Pathway> pathways) throws IDMapperException {
		log.info("Parsing interactions");
		log.fine("Ignoring interactions: " + ignoreInteractions);
		InMemoryGraph graph = new InMemoryGraph();
		graph.setDirected(true);
		
		for (Pathway p : pathways) {
			addPathway(p, graph);
		}

		return graph;
	}

	public void setIncludeUnmapped(boolean includeUnmapped) {
		this.includeUnmapped = includeUnmapped;
	}
	
	public void setIgnoreInteractions(
			Collection<InteractionType> ignoreInteractions) {
		this.ignoreInteractions = ignoreInteractions;
	}
	
	private static boolean isAllType(Collection<PathwayElement> elms,
			DataNodeType type) {
		boolean isAllType = true;
		for (PathwayElement pe : elms) {
			if (!type.getName().equals(pe.getDataNodeType())) {
				isAllType = false;
				break;
			}
		}
		return isAllType;
	}

	void addPathway(Pathway p, Graph g) throws IDMapperException {
		Set<Edge> addedEdges = new HashSet<Edge>();
		
		Map<String, String> symbols = new HashMap<String, String>();
		
		for (PathwayElement pe : p.getDataObjects()) {
			//Store symbol
			if(pe.getObjectType() == ObjectType.DATANODE && pe.getXref() != null) {
				Set<Xref> mapped = homology == null ? idm.mapID(pe.getXref(), targetDs) : homology.mapWithHomology(pe.getXref(), targetDs);
				for(Xref x : mapped) {
					symbols.put(x.toString(), pe.getTextLabel());
				}
				symbols.put(pe.getXref().toString(), pe.getTextLabel());
			}
			
			//Process groups and complexes
			if(pe.getObjectType() == ObjectType.GROUP) {
				// in_group
				// in_same_component
				InteractionType interaction = pe.getGroupStyle().equals(GroupStyle.COMPLEX) ? 
						InteractionType.IN_SAME_COMPONENT : InteractionType.IN_GROUP;
				
				if(ignoreInteractions.contains(interaction)) break;
				
				addedEdges.addAll(addUndirectedCombinations(
						p.getGroupElements(pe.getGroupId()),
						p.getGroupElements(pe.getGroupId()),
						interaction, pe.getGraphId(), g
				));
			}
			
			//Process relations
			if (isRelation(pe)) {
				Relation r = new Relation(pe);

				TypeFromStyle type = new TypeFromStyle(pe);
				
				Set<PathwayElement> leftright = new HashSet<PathwayElement>(); // Both input and output
				leftright.addAll(r.getLefts());
				leftright.addAll(r.getRights());

				// Convert to simple binary interactions using the
				// PathwayCommons SIF mappings
				// Try to derive meaning from template style
				boolean iIsMetabolite = isAllType(r.getLefts(),
						DataNodeType.METABOLITE);
				boolean oIsMetabolite = isAllType(r.getRights(),
						DataNodeType.METABOLITE);

				
				//Process relations

				// metabolic_catalysis
				// Make sure there is at least one mediator
				// Make sure that input/output are metabolites
				if(r.getMediators().size() > 0) {
					InteractionType interaction = InteractionType.METABOLIC_CATALYSIS;
					
					for (PathwayElement m : r.getMediators()) {
						if(!iIsMetabolite && !oIsMetabolite) {
							interaction = InteractionType.MEDIATES_INTERACTION;
							InteractionType derived = new TypeFromStyle(r.getMediatorLine(m)).getType();
							log.info(pe.getGraphId() + ": " + derived);
							if(derived != null) interaction = derived;
						}
						if(ignoreInteractions.contains(interaction)) continue;
						
						for(PathwayElement io : leftright) {
							addedEdges.addAll(addInteraction(m, io, interaction, true, pe.getGraphId(), g));
						}
					}
				}

				// reacts_with
				if (r.getLefts().size() > 0 && r.getRights().size() > 0 && iIsMetabolite
						&& oIsMetabolite) {
					InteractionType interaction = InteractionType.REACTS_WITH;
					if(ignoreInteractions.contains(interaction)) break;
					
					addedEdges.addAll(addUndirectedCombinations(
							leftright,
							leftright,
							interaction, pe.getGraphId(), g
					));
				}

				// co_control
				if (r.getMediators().size() > 0 && iIsMetabolite && oIsMetabolite) {
					InteractionType interaction = InteractionType.CO_CONTROL;
					if(ignoreInteractions.contains(interaction)) break;
					addedEdges.addAll(addUndirectedCombinations(
							r.getMediators(),
							r.getMediators(),
							interaction, pe.getGraphId(), g
					));
				}

				// interacts_with
				if (r.getLefts().size() > 0 && r.getRights().size() > 0 && !iIsMetabolite
						&& !oIsMetabolite && !type.isTransport()) {
					InteractionType interaction = InteractionType.INTERACTS_WITH;
					//Try to derive more specific type from style
					InteractionType derived = type.getType();
					if(derived != null) interaction = derived;
					
					boolean directed = false;
					Set<PathwayElement> lefts = r.getLefts();
					Set<PathwayElement> rights = r.getRights();
					if(type.isDirectedForward()) {
						directed = true;
					} else if(type.isDirectedBackward()) {
						directed = true;
						lefts = r.getRights();
						rights = r.getLefts();
					}
					if(ignoreInteractions.contains(interaction)) break;
					for(PathwayElement p1 : lefts) {
						for(PathwayElement p2 : rights) {
							addedEdges.addAll(addInteraction(p1, p2, interaction, directed, pe.getGraphId(), g));
						}
					}
				}

				// transport
				if(r.getLefts().size() > 0 && r.getRights().size() > 0 && type.isTransport()) {
					InteractionType interaction = InteractionType.TRANSPORT;
					if(ignoreInteractions.contains(interaction)) break;
					
					for(PathwayElement p1 : r.getLefts()) {
						for(PathwayElement p2 : r.getRights()) {
							//Only apply transport if start and end element are actually the same thing
							boolean equal = 
								(p1.getXref() != null && p1.getXref().equals(p2.getXref())) ||
								p1.getTextLabel().equals(p2.getTextLabel());
							
							if(equal) addedEdges.addAll(addInteraction(p1, p2, interaction, false, pe.getGraphId(), g));
							else {
								log.warning(
										"Ignored invalid transport (start and end not same): " 
										+ p1.getTextLabel() + " -> " + p2.getTextLabel()
										+ ", " + pe.getGraphId() + "@" 
										+ pe.getParent().getMappInfo().getDynamicProperty(DPathways.PROP_PATHWAYID)
								);
							}
						}
					}
				}
				
				// state_change
				if (r.getLefts().size() == 1 && r.getRights().size() == 1 && r.getMediators().size() > 0) {
					InteractionType interaction = InteractionType.STATE_CHANGE;
					if(ignoreInteractions.contains(interaction)) break;
					
					PathwayElement pi = r.getLefts().iterator().next();
					PathwayElement po = r.getRights().iterator().next();
					String idi = validXref(pi.getXref()) ? pi.getXref().toString() : pi.getTextLabel();
					String ido = validXref(po.getXref()) ? po.getXref().toString() : po.getTextLabel();
					
					if (idi.equals(ido)) {
						for(PathwayElement m : r.getMediators()) {
							addedEdges.addAll(addInteraction(m, pi, interaction, true, pe.getGraphId(), g));
						}
					}
				}
				
				// sequential_catalysis
				// TODO: is a bit hard, because need to look at following
				// reactions
			}
		}
		
		String pathwayId = p.getMappInfo().getDynamicProperty(DPathways.PROP_PATHWAYID);
		String pathwayDir = p.getMappInfo().getDynamicProperty(DPathways.PROP_PATHWAYDIR);
		for(Edge e : addedEdges) {
			e.appendAttribute(AttributeName.SourcePathway.name(), pathwayId, SEP);
			e.getSrc().appendAttribute(AttributeName.SourcePathway.name(), pathwayId, SEP);
			e.getSrc().appendAttribute(AttributeName.SourceDir.name(), pathwayDir, SEP);
			e.getSrc().appendAttribute("canonicalName", symbols.get(e.getSrc().getId()), SEP);
			e.getTgt().appendAttribute(AttributeName.SourcePathway.name(), pathwayId, SEP);
			e.getTgt().appendAttribute(AttributeName.SourceDir.name(), pathwayDir, SEP);
			e.getTgt().setAttribute(AttributeName.Label.name(), symbols.get(e.getTgt().getId()));
		}
	}

	private Set<Edge> addUndirectedCombinations(Set<PathwayElement> s1, Set<PathwayElement> s2, InteractionType interaction, String graphId, Graph g) throws IDMapperException {
		Set<String> addedIds = new HashSet<String>();
		Set<Edge> addedEdges = new HashSet<Edge>();
		
		for(PathwayElement e1 : s1) {
			for(PathwayElement e2 : s2) {
				if(e1 == e2) continue;
				//Check if interaction was already added in other direction
				if(addedIds.contains(e2.getGraphId() + "-" + e1.getGraphId())) continue;
				
				addedIds.add(e1.getGraphId() + "-" + e2.getGraphId());
				addedEdges.addAll(addInteraction(e1, e2, interaction, false, graphId, g));
			}
		}
		return addedEdges;
	}
	
	private Set<Edge> addInteraction(PathwayElement p1, PathwayElement p2, InteractionType interaction, boolean directed, String graphId, Graph g) throws IDMapperException {
		Set<Edge> edges = new HashSet<Edge>();
		
		Set<Node> nodes1 = nodesFromElement(g, p1);
		Set<Node> nodes2 = nodesFromElement(g, p2);
		for(Node n1 : nodes1) {
			for(Node n2 : nodes2) {
				Edge e = g.addEdge("" + n1 + interaction + n2, n1, n2);
				edges.add(e);
				e.setAttribute(AttributeName.Interaction.name(), "" + interaction);
				e.setAttribute(AttributeName.Directed.name(), "" + directed);
				e.appendAttribute(AttributeName.GraphId.name(), graphId, SEP);
			}
		}
		return edges;
	}
	
	private Set<Node> nodesFromElement(Graph g, PathwayElement pe) throws IDMapperException {
		Xref x = pe.getXref();
		
		Set<Xref> mapped = new HashSet<Xref>();
		if(homology != null && !idm.xrefExists(x)) {
			Set<Xref> homologs = homology.getHomologs(x);
			for(Xref xh : homologs) mapped.addAll(idm.mapID(xh, targetDs));
		} else {
			mapped.addAll(idm.mapID(x, targetDs));
		}
		
		Set<Node> nodes = new HashSet<Node>();
		
		boolean mappedXref = true;
		boolean hasXref = true;
		Set<DataSource> targetDsSet = new HashSet<DataSource>();
		Collections.addAll(targetDsSet, targetDs);
		
		if(mapped.size() > 0) { //Prefer mapped xrefs
			for(Xref xx : mapped) {
				Node n = g.addNode(xx.toString());
				nodes.add(n);
				n.setAttribute(AttributeName.XrefId.name(), xx.getId());
				n.setAttribute(AttributeName.XrefDatasource.name(), xx.getDataSource().getFullName());
			}
		} else if(validXref(x)) { //If can't be mapped, use unmapped xref
			if(includeUnmapped || targetDsSet.contains(x.getDataSource())) {
				Node n = g.addNode(x.toString());
				nodes.add(n);
				n.setAttribute(AttributeName.XrefId.name(), x.getId());
				n.setAttribute(AttributeName.XrefDatasource.name(), x.getDataSource().getFullName());
				mappedXref = false;
			}
		} else if(pe.getObjectType() != ObjectType.GROUP && includeUnmapped){ //if invalid xref, use text label, but don't add groups
			if(pe.getTextLabel() != null && !ignoreLabels.contains(pe.getTextLabel())) {
				String lbl = pe.getTextLabel().replaceAll("\\s", " ");
				nodes.add(g.addNode(lbl));
				mappedXref = false;
				hasXref = false;
			}
		}
		
		String pathwayId = pe.getParent().getMappInfo().getDynamicProperty(DPathways.PROP_PATHWAYID);
		String pathwayDir = pe.getParent().getMappInfo().getDynamicProperty(DPathways.PROP_PATHWAYDIR);
		
		for(Node n : nodes) {
			n.appendAttribute(AttributeName.GraphId.name(), pe.getGraphId(), SEP);
			
			n.appendAttribute(AttributeName.SourcePathway.name(), pathwayId, SEP);
			n.appendAttribute(AttributeName.SourceDir.name(), pathwayDir, SEP);
			
			String xrefValid = "mapped";
			if(!mappedXref) xrefValid = "unmapped";
			if(!hasXref) xrefValid = "none";
			
			n.setAttribute(AttributeName.XrefStatus.name(), xrefValid);
			n.setAttribute(AttributeName.ObjectType.name(), "" + pe.getObjectType());
		}
		return nodes;
	}
	
	private static boolean validXref(Xref x) {
		return x != null && x.getId() != null && !"".equals(x.getId()) && x.getDataSource() != null;
	}
	
	private static boolean isRelation(PathwayElement pe) {
		if (pe.getObjectType() == ObjectType.LINE) {
			MPoint s = pe.getMStart();
			MPoint e = pe.getMEnd();
			if (s.isLinked() && e.isLinked()) {
				// Objects behind graphrefs should be PathwayElement
				// so not MAnchor
				if (pe.getParent().getElementById(s.getGraphRef()) != null
						&& pe.getParent().getElementById(e.getGraphRef()) != null) {
					return true;
				}
			}
		}
		return false;
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
	
	public static void main(String[] args) {
		try {
			Args pargs = CliFactory.parseArguments(Args.class, args);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs);
			DHomology dhom = new DHomology(pargs);
			
			//Convert pathways
			PathwaysToNetwork ptn = new PathwaysToNetwork(didm.getIDMapper(), didm.getDataSources());
			ptn.setIncludeUnmapped(!pargs.isExcludeUnmapped());
			if(pargs.isIgnoreInteraction()) ptn.setIgnoreInteractions(pargs.getIgnoreInteraction());
			if(pargs.isHomology()) ptn.setHomology(
					new HomologyMapper(dhom.getIDMapperHomology(), dhom.getIDMapperSource(), didm.getIDMapper()));
			InMemoryGraph g = ptn.parseInteractions(dpws.getIterable());
			
			g.setTitle(pargs.isTitle() ? pargs.getTitle() : "untitled");
			
			if(pargs.getOut().getName().endsWith(".gml")) {
				writeGml("" + pargs.getOut(), g);
			} else if(pargs.getOut().getName().endsWith(".gml")) {
				writeXgmml("" + pargs.getOut(), g);
			} else {
				writeGml(pargs.getOut() + ".gml", g);
				writeXgmml(pargs.getOut() + ".xgmml", g);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
	
	private interface Args extends AIDMapper, AHelp, APathways, AHomology {
		@Option(shortName = "o", description = "The name of the output file (.gml or .xgmml).")
		public File getOut();
		
		@Option(shortName = "ii", description = "Do not include the interaction types specified here.")
		public List<InteractionType> getIgnoreInteraction();
		public boolean isIgnoreInteraction();
		
		@Option(shortName = "t", description = "The title of the network.")
		public String getTitle();
		public boolean isTitle();
		
		@Option(description = "Whether to exclude xrefs that can't be mapped to the correct datasource.")
		public boolean isExcludeUnmapped();
	}
}
