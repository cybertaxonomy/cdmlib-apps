package eu.etaxonomy.cdm.app.caryophyllales;

import eu.etaxonomy.cdm.io.excel.taxa.NormalExplicitRow;
import eu.etaxonomy.cdm.io.excel.taxa.TaxonExcelImportBase;
import eu.etaxonomy.cdm.io.excel.taxa.TaxonExcelImportState;

public class TaxonExcelCaryophyllales extends TaxonExcelImportBase {
    private static final long serialVersionUID = 7516628978483172010L;
    protected static final String AUTHOR_COLUMN = "Authorship";
	protected static final String SPECIES_HYBRID_MARKER = "Species hybrid marker";
	protected static final String NAME_STATUS_COLUMN = "NameStatus";
	protected static final String VERNACULAR_NAME_COLUMN = "VernacularName";
	protected static final String LANGUAGE_COLUMN = "Language";
	protected static final String REFERENCE_COLUMN = "Reference";

	protected static final String PROTOLOGUE_COLUMN = "Protologue";
	protected static final String IMAGE_COLUMN = "Image";
	protected static final String TDWG_COLUMN = "TDWG";
	protected static final String COUNTRY_COLUMN = "Country";

	protected static final String SYNONYM_COLUMN = "Synonym";

	@Override
	protected NormalExplicitRow createDataHolderRow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void analyzeSingleValue(
			eu.etaxonomy.cdm.io.excel.common.ExcelTaxonOrSpecimenImportBase.KeyValue keyValue,
			TaxonExcelImportState state) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void firstPass(TaxonExcelImportState state) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void secondPass(TaxonExcelImportState state) {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean isIgnore(TaxonExcelImportState state) {
		// TODO Auto-generated method stub
		return false;
	}

}
