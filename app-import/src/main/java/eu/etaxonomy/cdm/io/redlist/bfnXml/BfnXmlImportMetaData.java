/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.ResultWrapper;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;
/**
 *
 * @author a.oppermann
 * @date 04.07.2013
 *
 */
@Component
public class BfnXmlImportMetaData extends BfnXmlImportBase implements ICdmIO<BfnXmlImportState> {
	private static final Logger logger = Logger.getLogger(BfnXmlImportMetaData.class);
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
		logger.warn("start import MetaData...");

		//TODO only dirty quick fix for now
		state.setFirstClassificationName(state.getConfig().getClassificationName());

		ResultWrapper<Boolean> success = ResultWrapper.NewInstance(true);

		BfnXmlImportConfigurator config = state.getConfig();
		Element elDataSet = getDataSetElement(config);
		Namespace bfnNamespace = config.getBfnXmlNamespace();
		//create complete source object
		if(elDataSet.getName().equalsIgnoreCase("DEBExport")){
			sourceFileName = elDataSet.getAttributeValue("source");
			debVersion = elDataSet.getAttributeValue("debversion");
			timeStamp = elDataSet.getAttributeValue("timestamp");

			Reference<?> sourceReference = ReferenceFactory.newGeneric();
			sourceReference.setTitle(sourceFileName);
			TimePeriod parsedTimePeriod = TimePeriodParser.parseString(timeStamp);
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

					String bfnElementName = "METADATEN";
					@SuppressWarnings("unchecked")
                    List<Element> elMetaDataList  = currentElement.getChildren();
					//for each taxonName
					for (Element elMetaData : elMetaDataList){
						if( elMetaData.getAttributeValue("standardname").equalsIgnoreCase("KurzLit_A")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String kurzlitA = children.get(0).getTextNormalize();
							Reference<?> sourceReference = ReferenceFactory.newGeneric();
							sourceReference.setTitle(kurzlitA);
							state.setFirstListSecRef(sourceReference);

						}
						else if( elMetaData.getAttributeValue("standardname").equalsIgnoreCase("Klassifikation_A")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String klassifikation_A = children.get(0).getTextNormalize();
							state.setFirstClassificationName(klassifikation_A);

						}
						else if( elMetaData.getAttributeValue("standardname").equalsIgnoreCase("KurzLit_B")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String kurzlitB = children.get(0).getTextNormalize();
							Reference<?> sourceReference = ReferenceFactory.newGeneric();
							sourceReference.setTitle(kurzlitB);
							state.setSecondListSecRef(sourceReference);
						}
						else if( elMetaData.getAttributeValue("standardname").equalsIgnoreCase("Klassifikation_B")){
							@SuppressWarnings("unchecked")
                            List<Element> children = elMetaData.getChildren();
							String klassifikation_B = children.get(0).getTextNormalize();
							state.setSecondClassificationName(klassifikation_B);

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
		return;

	}

	@Override
	protected boolean isIgnore(BfnXmlImportState state) {
		return ! state.getConfig().isDoMetaData();
	}

}
