package com.ibm.watson.self.constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SelfConfigurationConstants {


	/*          Logging                 */

    private static Logger logger = LogManager.getLogger(SelfConfigurationConstants.class.getName());
	
	private static final String PROPERTIES_NAME = "self.properties";
	private static final Properties SELF_PROPERTIES;
	static {
		InputStream propStream = SelfConfigurationConstants.class.getClassLoader().getResourceAsStream(PROPERTIES_NAME);
		SELF_PROPERTIES = new Properties();
		try {
			SELF_PROPERTIES.load(propStream);
		} catch (IOException e) {
			logger.error("Could not load properties from file!");
			logger.error(Arrays.toString(e.getStackTrace()));
		}
	}
	
	/*			Keys					*/
	
	private static final String HOST_KEY = "host";
	private static final String PORT_KEY = "port";
	private static final String SELFID_KEY = "selfId";
	private static final String ORGID_KEY = "orgId";
	private static final String TOKEN_KEY = "token";
	
	/*			Values				   */
	
	public static final String HOST = SELF_PROPERTIES.getProperty(HOST_KEY);
	public static final String SELF_ID = SELF_PROPERTIES.getProperty(SELFID_KEY);
	public static final String TOKEN = SELF_PROPERTIES.getProperty(TOKEN_KEY);
	public static final String ORG_ID = SELF_PROPERTIES.getProperty(ORGID_KEY);
	
	public static final String PORT = SELF_PROPERTIES.getProperty(PORT_KEY);
}
