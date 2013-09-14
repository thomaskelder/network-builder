network-builder
===============

Small java tool to generate GML and XGMML networks from several sources.

org.tno.networks.StringToNetwork
--------------------------------

Convert flatfiles from [STRING](http://string-db.org/) or [STITCH](http://stitch.embl.de/).

Requires the following data files:
 * The *.actions.detailed.xx.tsv.gz that can be downloaded from STRING or STITCH.
 * The [bridgedb](http://www.bridgedb.org) identifier mappers for the species you wish to convert. You can specify multiple mappers (e.g. for genes and metabolites)
 * Ensembl gene to protein identifier mappings as text file. These can be exported using BioMART. Files for ensembl 60-64 can be found in the data directory. You can specify multiple files to allow conversion of deprecated protein identifiers still used in STRING / STITCH

An example of how to run the script, this command extracts all non-textmining interactions with a score >400 for mouse and writes it to both GML and XGMML files:

```
  java -cp build-network.jar org.tno.networks.StringToNetwork --idm idmapper-pgdb:~/data/bridgedb/Mm_Derby_20110603.bridge
  --ds L --ens data/string-stitch/mart_export_64.txt data/string-stitch/mart_export_63.txt 
  data/string-stitch/mart_export_62.txt data/string-stitch/mart_export_61.txt data/string-stitch/mart_export_60.txt 
  --species "Mus musculus" --excludeSources NLP --in ~/data/string/protein.actions.detailed.v9.0.txt 
  --out string.9.mouse.400noNLP
```

org.tno.networks.PathwaysToNetwork
--------------------------------

Convert GPML files ([WikiPathways](http://www.wikipathways.org), converted KEGG) to networks.

Requires the following data files:
 * A folder with GPML files to combine in a network
 * The [bridgedb](http://www.bridgedb.org) identifier mappers for the species you wish to convert. You can specify multiple mappers (e.g. for genes and metabolites)

Explanation of the parameters:
```
  java -cp build-network.jar org.tno.networks.PathwaysToNetwork
```

Example:
```
  java -cp build-network.jar org.tno.networks.PathwaysToNetwork --out wikipathways_20130814.gml --idm idmapper-pgdb:/path/to/bridgedb/Hs_Derby_20110601.bridge idmapper-pgdb:/path/to/bridgedb/hmdb_metabolites_20130627.bridge --ds L Ch --p /path/to/wikipathways-gpml/ -ii IN_GROUP IN_SAME_COMPONENT -t wikipathways_network --excludeUnmapped
```

org.tno.networks.TFeToNetwork
--------------------------------

Create a network with transcription factor targets from [TFe](http://burgundy.cmmt.ubc.ca/tfe/).
