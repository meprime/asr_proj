package main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyProperties {

	private Properties props;
	
	public final static String PROP_LEXICON_PATH = "lexiconPath";
	public final static String PROP_TRAIN_PATH = "trainPath";
	
	private static MyProperties instance;
	public static MyProperties getInstance() {
		if(instance == null)
			instance = new MyProperties();
		return instance;
	}
	
	private MyProperties() {
		InputStream propsStream = getClass().getResourceAsStream("config.properties");
		props = new Properties();
		try {
			props.load(propsStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getProperty(String propName) {
		return props.getProperty(propName);
	}
	
	public boolean isDebug() {
		return true;
	}
}
