/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.persistence.query.MatchMode;
import eu.etaxonomy.cdm.strategy.exceptions.StringNotParsableException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 08.12.2017
 *
 */
@Component
public class GreeceGenusAuthorImport
    extends SimpleExcelTaxonImport<GreeceGenusAuthorImportConfigurator>{

    private static final Logger logger = Logger.getLogger(GreeceGenusAuthorImport.class);

    private static final long serialVersionUID = 1173327042682886814L;
    private ImportDeduplicationHelper<SimpleExcelTaxonImportState> dedupHelper;
    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();


    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<GreeceGenusAuthorImportConfigurator> state) {
        String genus = state.getOriginalRecord().get("Genus");
        String author = state.getOriginalRecord().get("Author");
        List<TaxonName> existingNames = getNameService().findNamesByNameCache(genus, MatchMode.EXACT, null);
        if (existingNames.size() != 1){
            logger.warn("Name '"+genus+"' has not exactly one record, but " + existingNames.size());
        }else{
            TaxonName name = existingNames.iterator().next();
//            TaxonName name = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS()) ;
//            name.setGenusOrUninomial("Genus");
            try {
                parser.parseAuthors(name, author);
                getDedupHelper(state).replaceAuthorNamesAndNomRef(state, name);
                name.addImportSource(null, null, state.getConfig().getSourceReference(), String.valueOf(state.getCurrentLine()));
            } catch (StringNotParsableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
//                throw new RuntimeException(e);
            }
        }

    }

    /**
     * @param state
     * @return
     */
    private ImportDeduplicationHelper<SimpleExcelTaxonImportState> getDedupHelper(SimpleExcelTaxonImportState<GreeceGenusAuthorImportConfigurator> state) {
        if (this.dedupHelper == null){
            dedupHelper = ImportDeduplicationHelper.NewInstance(this, state);
        }
        return this.dedupHelper;
    }

}
