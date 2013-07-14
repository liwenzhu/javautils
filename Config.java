package com.veivo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

public class Config {

	private static Properties prop;
	
	/* adobe account info */
	private static final String APP_INFO = "adobe.api.getme";
	private static final String ACCOUNT_USERNAME = "adobe.account.username";
	private static final String ACCOUNT_PASS = "adobe.account.password";
	
	/* MySQL Info */
	private static final String MYSQL_DRIVER = "mysql.driver";
	private static final String MYSQL_URL = "mysql.url";
	private static final String MYSQL_USERNAME = "mysql.username";
	private static final String MYSQL_PASS = "mysql.password";
	
	/* API */
	private static final String API_CREATE_APP = "adobe.api.create";
	private static final String API_APP_INFO = "adobe.api.apps.info";
	private static final String API_APP_DOWNLOAD_URL = "adobe.api.apps.download.url";
	
	/* save file */
	private static final String FILES_HOME = "files.home";
	private static final String DOWNLOAD_LOCATION_PATH = "server.package.location";


	static {
		InputStream is = Config.class.getResourceAsStream("/pack.properties");
		try {
			prop = new Properties();
			prop.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Config() {

	}

	private static String getString(String key) {
		return prop.getProperty(key);
	}

	private static String getFormatString(String key, Object... param) {
		return MessageFormat.format(getString(key), param);
	}

	public static String getAppInfoURL() {
		return getString(APP_INFO);
	}

	public static String getUsername() {
		return getString(ACCOUNT_USERNAME);
	}

	public static String getPassword() {
		return getString(ACCOUNT_PASS);
	}

	public static String getMysqlURL() {
		return getString(MYSQL_URL);
	}

	public static String getMysqlUsername() {
		return getString(MYSQL_USERNAME);
	}

	public static String getMysqlPass() {
		return getString(MYSQL_PASS);
	}

	public static String getMysqlDriver() {
		return getString(MYSQL_DRIVER);
	}

	public static String getCompileUrl() {
		return getString(API_CREATE_APP);
	}

	public static String getAppInfo(String id) {
		return getFormatString(API_APP_INFO, id);
	}

	public static String getSaveFileHome(String appid, String devAppid) {
		return getFormatString(FILES_HOME, appid, devAppid);
	}

	public static String getPackageAddress(String string) {
		return getFormatString(API_APP_DOWNLOAD_URL, string);
	}

	public static Object getLocation(String path) {
		// TODO Auto-generated method stub
		return getFormatString(DOWNLOAD_LOCATION_PATH, path);
	}
}
