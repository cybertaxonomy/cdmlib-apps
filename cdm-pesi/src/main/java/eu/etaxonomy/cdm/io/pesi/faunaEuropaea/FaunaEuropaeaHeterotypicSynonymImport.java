/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.ICdmImport;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * This class creates heterotypic synonymy relationships to the accepted taxon for
 * basionym synonyms.
 *
 * @author a.babadshanjan
 * @since 22.09.2009
 */
@Component
public class FaunaEuropaeaHeterotypicSynonymImport
        extends CdmImportBase<FaunaEuropaeaImportConfigurator, FaunaEuropaeaImportState>
        implements ICdmImport<FaunaEuropaeaImportConfigurator, FaunaEuropaeaImportState> {

    private static final long serialVersionUID = -4195183108743873247L;
    private static Logger logger = LogManager.getLogger();

	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state) {
		logger.warn("Checking for heterotypic synonyms for basionyms not yet implemented");
		return false;
	}

	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {

		TransactionStatus txStatus = null;
		List<Synonym> synonymList = null;
		Set<Taxon> taxonSet = null;
		int i = 0;
		int start = 0;

		int limit = state.getConfig().getLimitSave();
		int nbrOfSynonyms = getTaxonService().count(Synonym.class);
		if (logger.isInfoEnabled()) {
			logger.info("Number of synonyms = " + nbrOfSynonyms);
		}
		if(state.getConfig().isDoHeterotypicSynonyms()){
		while (i < nbrOfSynonyms) {

			try {
				if ((i++ % limit) == 0) {

					start = (i == 1) ? 0 : i;
					if (logger.isInfoEnabled()) {
						logger.info("Retrieving synonyms starting from: " + start);
					}
					txStatus = startTransaction();
					synonymList = getTaxonService().list(Synonym.class, limit, start, null, null);
					taxonSet = new HashSet<Taxon>(limit);
				}

				if (((i % limit) == 0 && i != 1 ) || i == nbrOfSynonyms) {

					Set<NameRelationship> nameRelations = null;
					Set<Taxon> taxonBases = null;
					Taxon acceptedTaxon = null;
					TaxonName synonymName = null;
					NameRelationship nameRelation = null;
					TaxonName acceptedName = null;

					for (TaxonBase<?> synonym : synonymList) {
						synonymName = synonym.getName();
						if (synonymName.isGroupsBasionym()) {
							nameRelations = synonymName.getNameRelations();
							if (nameRelations != null && nameRelations.iterator().hasNext()) {
								nameRelation = nameRelations.iterator().next();
								acceptedName = nameRelation.getToName();
								logger.debug("SynonymName: " + synonymName + " titleCache of synonym: "+synonym.getTitleCache() + " name of acceptedTaxon: " + acceptedName.getTitleCache());
								if (logger.isTraceEnabled()) {
									logger.trace("toName: " + acceptedName);
									logger.trace("fromName: " + nameRelation.getFromName());
								}
								taxonBases = acceptedName.getTaxa();
								if (taxonBases != null && taxonBases.iterator().hasNext()) {
								    acceptedTaxon = taxonBases.iterator().next();
									Set <Synonym> synonyms = acceptedTaxon.getSynonyms();
									if (!synonyms.contains(synonym)){
									//TODO: Achtung!!!!! dies wird auch bei homotypischen Synonymen aufgerufen! Dadurch wird ein weiteres Synonym erzeugt
										acceptedTaxon.addHeterotypicSynonymName(synonymName);
										taxonSet.add(acceptedTaxon);
									}
								}
							}
						}
					}

					getTaxonService().save(taxonSet);
					taxonSet = null;
					synonymList = null;
					commitTransaction(txStatus);
					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Transaction committed");
					}
				}

			} catch (Exception e) {
				logger.warn("An exception occurred when creating heterotypic synonym relationship # " + i );
				e.printStackTrace();
			}
		}
		}
		return;
	}

	@Override
	protected boolean isIgnore(FaunaEuropaeaImportState state) {
		return !(state.getConfig().isDoHeterotypicSynonymsForBasionyms());
	}
}
