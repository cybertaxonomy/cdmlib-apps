/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbExportStateBase;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.mapping.CdmMapperBase;
import eu.etaxonomy.cdm.io.common.mapping.out.DbSequenceMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IDbExportMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IdMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * Does not seem to be in use currently. (2019-09-22)
 * @author e.-m.lee
 * @since 24.02.2010
 */
public class PesiCollectionExportMapping extends PesiExportMapping {

    private static Logger logger = LogManager.getLogger();

	private IdMapper parentMapper;
	private DbSequenceMapper sequenceMapper;
	private String collectionAttributeName;

	public static PesiCollectionExportMapping NewInstance(String tableName, String collectionAttributeName, IdMapper parentMapper){
		return new PesiCollectionExportMapping(tableName, collectionAttributeName, parentMapper, null);
	}

	public static PesiCollectionExportMapping NewInstance(String tableName, String collectionAttributeName, IdMapper parentMapper, String seqenceAttribute){
		return NewInstance(tableName, collectionAttributeName, parentMapper, seqenceAttribute, 0);
	}

	public static PesiCollectionExportMapping NewInstance(String tableName, String collectionAttributeName, IdMapper parentMapper, String seqenceAttribute, int sequenceStart){
		DbSequenceMapper sequenceMapper = DbSequenceMapper.NewInstance(seqenceAttribute, sequenceStart);
		PesiCollectionExportMapping result = new PesiCollectionExportMapping(tableName, collectionAttributeName, parentMapper, sequenceMapper);
		return result;
	}

	private PesiCollectionExportMapping(String tableName, String collectionAttributeName, IdMapper parentMapper, DbSequenceMapper sequenceMapper){
		super(tableName);
		this.parentMapper = parentMapper;
		this.addMapper(parentMapper);
		this.sequenceMapper = sequenceMapper;
		this.addMapper(sequenceMapper);
		this.collectionAttributeName = collectionAttributeName;
	}

	@Override
	public boolean invoke(CdmBase parent) {
		try {
			boolean result = true;
			Collection<CdmBase> collection = getCollection(parent);
			if (this.sequenceMapper != null){
				this.sequenceMapper.reset();
			}
			for(CdmBase collectionObject : collection){
				if (collectionObject == null){
					logger.warn("Collection object was null");
					result = false;
					continue;
				}
				for (CdmMapperBase mapper : this.mapperList){
					if (mapper == this.parentMapper){
						parentMapper.invoke(parent);
					}else if (mapper == this.sequenceMapper){
						this.sequenceMapper.invoke(null);
					}else if (mapper instanceof IDbExportMapper){
						IDbExportMapper<DbExportStateBase<?, PesiTransformer>, PesiTransformer> dbMapper = (IDbExportMapper<DbExportStateBase<?, PesiTransformer>, PesiTransformer>)mapper;
						try {
							result &= dbMapper.invoke(collectionObject);
						} catch (Exception e) {
							result = false;
							logger.error("Error occurred in mapping.invoke");
							e.printStackTrace();
							continue;
						}
					}else{
						logger.warn("mapper is not of type " + IDbExportMapper.class.getSimpleName());
					}
				}
				int count = getPreparedStatement().executeUpdate();
				if (logger.isDebugEnabled()) {
                    logger.debug("Number of rows affected: " + count);
                }
			}
			return result;
		} catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage() + ": " + parent.toString());
			return false;
		}
	}

	private Collection<CdmBase> getCollection(CdmBase cdmBase){
		Object result = ImportHelper.getValue(cdmBase, collectionAttributeName, false, true);
		return (Collection<CdmBase>)result;
	}
}
