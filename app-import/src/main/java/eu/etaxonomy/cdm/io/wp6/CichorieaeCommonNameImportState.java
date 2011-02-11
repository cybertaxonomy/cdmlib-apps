package eu.etaxonomy.cdm.io.wp6;

import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportState;

public class CichorieaeCommonNameImportState extends ExcelImportState {
	CommonNameRow row;
	
	public CichorieaeCommonNameImportState(ExcelImportConfiguratorBase config) {
		super(config);
	}

	public void setCommonNameRow(CommonNameRow row) {
		this.row = row;
	}
	
	public CommonNameRow getCommonNameRow(){
		return row;
	}

}
