/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.term.TermType;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraRegionImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -361321245993093847L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraRegionImport.class);

	protected static final String NAMESPACE = "Region";

	private static final String pluralString = "regions";
	private static final String dbTableName = "cv2_Controlled_vocabulary_for_Mexican_States";

	public MexicoEfloraRegionImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdRegion "
		        + " FROM " + dbTableName
		        + " ORDER BY IdRegion ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
        String sqlSelect = " SELECT * ";
        String sqlFrom = " FROM " + dbTableName;
 		String sqlWhere = " WHERE ( IdRegion IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    String label = "Mexican regions";
	    OrderedTermVocabulary<NamedArea> voc = OrderedTermVocabulary.NewOrderedInstance(TermType.NamedArea, NamedArea.class, label, label, null, null);
	    getVocabularyService().save(voc);

	    boolean success = true ;

        Set<NamedArea> areasToSave = new HashSet<>();

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){
			    int idRegion = rs.getInt("IdRegion");

			    String nombreRegion = rs.getString("NombreRegion");
				String clave = rs.getString("ClaveRegion");
			    String abbrev = rs.getString("Abreviado");

			    try {
			        NamedArea area = NamedArea.NewInstance(nombreRegion, nombreRegion, abbrev, Language.SPANISH_CASTILIAN());
			        area.setIdInVocabulary(clave);
			        voc.addTerm(area);
			        state.getAreaMap().put(idRegion, area);

					partitioner.startDoSave();
					areasToSave.add(area);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create region for id " + idRegion + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		getTermService().save(areasToSave);
		return success;
	}

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		return result;
	}

	@Override
	protected String getTableName() {
		return dbTableName;
	}

	@Override
	public String getPluralString() {
		return pluralString;
	}

    @Override
    protected boolean doCheck(MexicoEfloraImportState state){
        return true;
    }

	@Override
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}