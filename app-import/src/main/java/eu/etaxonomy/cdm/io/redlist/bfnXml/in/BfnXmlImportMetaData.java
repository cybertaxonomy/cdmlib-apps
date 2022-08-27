/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.Element;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.ResultWrapper;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.oppermann
 * @since 04.07.2013
 */
@Component
public class BfnXmlImportMetaData extends BfnXmlImportBase implements ICdmIO<BfnXmlImportState> {

    private static final long serialVersionUID = 4180700081829559594L;
    private static final Logger logger = LogManager.getLogger();

    private String sourceFileName;
	private String debVersion;
	private String timeStamp;

	public BfnXmlImportMetaData(){
		super();
	}

	@Override
	public boolean doCheck(BfnXmlImportState state){
		boolean result = true;
		//TODO needs to be implemented
		return result;
	}

	@Override
	public void doInvoke(BfnXmlImportState state){
		logger.info("start import MetaData...");

		//TODO only dirty quick fix for now
		state.setFirstClassificationName(state.getConfig().getClassificationName());

		ResultWrapper<Boolean> success = ResultWrapper.NewInstance(true);

		BfnXmlImportConfigurator config = state.getConfig();

		state.setFirstListSecRef(null);

		Element elDataSet = getDataSetElement(config);

		//create complete source object
		if(elDataSet.getName().equalsIgnoreCase(BfnXmlConstants.EL_DEB_EXPORT)){
			sourceFileName = elDataSet.getAttributeValue("source");
			if (sourceFileName.equals("rldb_print.xls")){
			    sourceFileName = retrieveFileName(config.getSource().toString());
			}
			debVersion = elDataSet.getAttributeValue("debversion");
			timeStamp = elDataSet.getAttributeValue("timestamp");

			Reference sourceReference = ReferenceFactory.newGeneric();
			sourceReference.setTitle(sourceFileName);
			VerbatimTimePeriod parsedTimePeriod = TimePeriodParser.parseStringVerbatim(timeStamp);
			sourceReference.setDatePublished(parsedTimePeriod);
			state.setCompleteSourceRef(sourceReference);
		}

		List<?> contentXML = elDataSet.getContent();
		Element currentElement = null;
		for(Object object:contentXML){

			if(object instanceof Element){
				currentElement = (Element)object;

				if(currentElement.getName().equalsIgnoreCase("METADATEN")){

					TransactionStatus tx = startTransaction();

					@SuppressWarnings("unchecked")
                    List<Element> elMetaDataList  = currentElement.getChildren();
					//for each taxonName
					for (Element elMetaData : elMetaDataList){
						if( elMetaData.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("KurzLit_A")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String kurzlitA = children.get(0).getTextNormalize();
							Reference secReference = ReferenceFactory.newGeneric();
							secReference.setTitle(kurzlitA);
							state.setFirstListSecRef(secReference);

						}
						else if( elMetaData.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("Klassifikation_A")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String klassifikation_A = children.get(0).getTextNormalize();
							state.setFirstClassificationName(klassifikation_A);

						}
						else if( elMetaData.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("KurzLit_B")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String kurzlitB = children.get(0).getTextNormalize();
							Reference secReference = ReferenceFactory.newGeneric();
							secReference.setTitle(kurzlitB);
							state.setSecondListSecRef(secReference);
						}
						else if( elMetaData.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("Klassifikation_B")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String classification_B = children.get(0).getTextNormalize();
							state.setSecondClassificationName(classification_B);

						}

					}


					logger.warn("end import MetaData ...");
					commitTransaction(tx);

					if (!success.getValue()){
						state.setUnsuccessfull();
					}
					//FIXME: Only take the first RoteListeData Features

					return;
				}
			}
		}
		if (state.getFirstListSecRef() == null){
		    //usage of sourceRefUuid is maybe not 100% correct here as we use it for sec reference
		    Reference secReference = getReferenceService().load(state.getConfig().getSourceRefUuid(), Arrays.asList("authorship.teamMembers"));
//            Reference secReference = ReferenceFactory.newGeneric();
//            secReference.setTitle(state.getFirstClassificationName());
            state.setFirstListSecRef(secReference);
            state.setCurrentSecundumRef(secReference);
        }
		return;

	}

	/**
     * @param string
     * @return
     */
    private String retrieveFileName(String uri) {
        String[] splits = uri.split("/");
        return splits[splits.length - 1 ];
    }

    @Override
	protected boolean isIgnore(BfnXmlImportState state) {
		return ! state.getConfig().isDoMetaData();
	}

}
