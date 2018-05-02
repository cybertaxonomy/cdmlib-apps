/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.caryophyllales;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.log4j.Logger;



/**
 * @author k.luther
 * @since Jun 2015
 */
public class XlsSources {
	@SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(XlsSources.class);


	public static URI normalExplicit(){
		try {
//			URL url = new File(("C:\\localCopy\\eclipse\\cdmlib\\trunk\\app-import\\src\\main\\resources\\excel\\NormalExplicit.xls")).toURL();

			// FIXME what is this????
			URL url = new File("D:\\NormalExplicit.xls").toURI().toURL();

//			URL url = new TcsSources().getClass().getResource("excel/NormalExplicit.xls");
			boolean exists = new File(url.getFile()).exists();
			if (! exists) {
                throw new RuntimeException("File not found: " + url);
            }
			URI uri = url.toURI();
			return uri;
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RuntimeException(e1);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public static String xls_nyctaginaceae(){
		try {
			File file = new File("C:\\Users\\k.luther\\Documents\\Caryophyllales\\Arenaria_ThePlantList.xls");
			return file.toURI().toURL().toString();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

	}


}
