package eu.etaxonomy.cdm.io.common;

import java.sql.Connection;

import eu.etaxonomy.cdm.config.AccountStore;

public class TestConnection {


	public static void main(String[] args){

		System.out.println("start");
//		Source source = new Source(Source.SQL_SERVER_TRUSTED, "BGBM-PESISQL", "ERMS_20161007", true);
		String server = "BGBM-PESISQL\\SQLEXPRESS";
		String dbms = Source.SQL_SERVER_TRUSTED;
		String db = "PESI_DW_2025_1";
//		String db = "erms_2025_04_03";
        Source source = new Source(dbms, server, db, true);
        source.setPort(1434);
//		Source source = new Source(Source.SQL_SERVER, "(local)", "PESI_v12", "SELECT DISTINCT AreaId FROM Area");

//  	Source source = new Source(Source.SQL_SERVER, "PESIIMPORT3", "PESI_v122", "SELECT DISTINCT AreaId FROM Area");
		String user = "pesiimport";
		source.setUsername(user);
		String pwd = AccountStore.readOrStorePassword(dbms, server, user, null);
		source.setPassword(pwd);
		System.out.println("connect");
		Connection con = source.getConnection();
		System.out.println(con);
	}
}
