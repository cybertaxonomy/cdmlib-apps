package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportConfigurator extends
		DbImportConfiguratorBase<RedListGefaesspflanzenImportState> {

    public static RedListGefaesspflanzenImportConfigurator NewInstance(Source source, ICdmDataSource cdmDestination) {
        return new RedListGefaesspflanzenImportConfigurator(source, cdmDestination, new RedListGefaesspflanzenTransformer());
    }


	protected RedListGefaesspflanzenImportConfigurator(Source source,
			ICdmDataSource destination, IInputTransformer defaultTransformer) {
		super(source, destination, NomenclaturalCode.ICNAFP, defaultTransformer);
	}

	@SuppressWarnings("unchecked")
    @Override
	public RedListGefaesspflanzenImportState getNewState() {
		return new RedListGefaesspflanzenImportState(this);
	}

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                RedListGefaesspflanzenImportRank.class,
                RedListGefaesspflanzenImportFamily.class,
                RedListGefaesspflanzenImportAuthor.class,
                RedListGefaesspflanzenImportNames.class,
                RedListGefaesspflanzenImportClassification.class,
        };
    }
}
