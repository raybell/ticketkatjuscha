package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class PropertyHandler {
	public static final String PROP_TICKET_NAME_PREFIX = "ticketNamePrefix";
	public static final String PROP_TICKET_GEN_DIR = "ticketGenerationDir";
	public static final String PROP_TICKET_ARCHIVE_DIR = "ticketArchiveDir";
	public static final String PROP_TICKET_TEMPLATE_FILE = "ticketTemplateFile";
	public static final String PROP_MAX_CODE_DIGITS = "maxCodeDigits";
	public static final String PROP_MAX_CHECKCODE_DIGITS = "maxCheckcodeDigits";
	

	private static final String[][] PROP_DEFAULT_VALUES = new String[][]{
		{PROP_TICKET_NAME_PREFIX, "ticket"},
		{PROP_MAX_CODE_DIGITS, "4"},
		{PROP_MAX_CHECKCODE_DIGITS, "3"},
		{PROP_TICKET_GEN_DIR, "latest"},
		{PROP_TICKET_ARCHIVE_DIR, "archive"},
		{PROP_TICKET_TEMPLATE_FILE, "template/template.pdf"}
	};
	
	private static final String PROP_FILENAME = "katjuscha.properties";
	private static final HashMap<String, String> PROP_DEFAULT_HASH;
	static{
		PROP_DEFAULT_HASH = new HashMap<>();
		for (int i = 0; i < PROP_DEFAULT_VALUES.length; i++) {
			PROP_DEFAULT_HASH.put(PROP_DEFAULT_VALUES[i][0], PROP_DEFAULT_VALUES[i][1]);
		}
	}
	private Properties props;
	private static PropertyHandler propHandler;
	
	//******** singleton access **********
	public static PropertyHandler getInstance(){
		if (propHandler == null){
			propHandler = new PropertyHandler();
		}
		return propHandler;
	}
	
	public static void persist(){
		if (propHandler != null){
			propHandler.saveProperties();
		}
	}
	//***********************************
	private PropertyHandler(){
		loadProperties();
	}
	
	private void loadProperties(){
		props = new Properties();
		File propFile = new File(PROP_FILENAME);
		if (propFile.exists()){
			//load the props
			try {
				props.load(new FileInputStream(propFile));
			} catch (IOException e) {
				System.out.println("Warning: got a problem loading the props: " + e.toString());
				e.printStackTrace();
			}
		}else{
			//no props, use default
		}
	}
	
	private void saveProperties(){
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(new File(PROP_FILENAME));
			props.store(out, null);
		} catch (IOException e) {
			System.out.println("Warning: could not store properties: " + e.toString());
			e.printStackTrace();
		}finally{
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				System.err.println("Problem closing prop stream: " + e.toString());
				e.printStackTrace();
			}
		}
	}
	
	private String getAndSetDefault(String propName){
		if (PROP_DEFAULT_HASH.containsKey(propName)){
			String propValue = PROP_DEFAULT_HASH.get(propName);
			props.setProperty(propName, propValue);
			return propValue;
		}else{
			return null;
		}
	}
	
	//**************** instance access ************
	public String getPropertyString(String propName){
		if (props.containsKey(propName)){
			return props.getProperty(propName);
		}else{
			return getAndSetDefault(propName);
		}
	}
	
	public int getPropertyInt(String propName){
		if (props.containsKey(propName)){
			return Integer.parseInt(props.getProperty(propName));
		}else{
			return Integer.parseInt(getAndSetDefault(propName));
		}
	}
	//********************************************
}
