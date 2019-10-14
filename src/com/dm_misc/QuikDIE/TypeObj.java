package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Import Content Module - TypeObj
 * (c) 2013-2019 MS Roth
 * 
 * ============================================================================
 */

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.documentum.fc.client.IDfSession;


public class TypeObj {

	private boolean m_state = false;
	private File m_metadataFile = null;
	private IDfSession m_session = null;
	private String m_errorMsg = "";
	private Properties m_typeObjProps = new Properties();
	private Properties m_typeAttrsProps = new Properties();
	
	
	public TypeObj(String metadatafile, IDfSession session) {
		String attrTemplate = "%s %s(%s) %s,";
		
		try {
			// set session for use in class
			if (session == null)
				throw new Exception ("session is null");
			else
				m_session = session;
			
			// read metadata file and load member properties
			m_metadataFile = new File(metadatafile);
			readMetadataFile();
		
			// build query string to create type
			StringBuilder typeQuery = new StringBuilder();
			typeQuery.append("create type \"" + getTypeObjProperty("name") + "\" (");
			
			// build the specific attribute variables for query
			Set<String> attrs = m_typeAttrsProps.stringPropertyNames();
			for (String attr : attrs) {
				String[] attr_values = getTypeAttrProperty(attr).split(":");
				if (attr_values[2].equalsIgnoreCase("false")) {
					attr_values[2] = "";
				} else {
					attr_values[2] = "repeating";
				}
				typeQuery.append(String.format(attrTemplate, attr, attr_values[0], attr_values[1], attr_values[2]));
			}
			
			// clobber the last "," added
			typeQuery.setLength(typeQuery.length() - 1);
			typeQuery.append(") with supertype \"" + getTypeObjProperty(Utils.OBJ_ATTR_SUPER_TYPE) + "\" publish");
			
			// create type via DQL
			boolean rv = Utils.runDQLQueryWithBooleanResult(typeQuery.toString(), m_session);
			
			if (rv == true) {
				m_state = true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setErrorMsg(e.getMessage());
		}
	}
	
	
	private void readMetadataFile() {

		// SAX
		SAXBuilder builder = new SAXBuilder();
		Document Doc = null;

		try {
			Doc = builder.build(m_metadataFile);
			Element root = Doc.getRootElement();

			// get type name
			setTypeObjProperty(Utils.XML_ATTR_NAME, root.getAttributeValue(Utils.XML_ATTR_NAME));
			
			// get super type
			setTypeObjProperty(Utils.XML_ATTR_SUPER_TYPE, root.getAttributeValue(Utils.XML_ATTR_SUPER_TYPE));
						
			// loop over child elements
			List<Element> xml_elements = root.getChildren();
			for(int i=0 ; i < xml_elements.size(); i++) {
				Element element = xml_elements.get(i);

				// find attributes element
				if (element.getName().equalsIgnoreCase(Utils.XML_ELEMENT_ATTRIBUTES)) {
					List<Element> properties = element.getChildren();
					
					// loop over all attributes
					for(int j=0 ; j < properties.size(); j++) {
						
						Element property = properties.get(j);
						String attr = property.getAttributeValue(Utils.XML_ATTR_NAME);
						String attr_type = property.getAttributeValue(Utils.XML_ATTR_TYPE);
						String attr_size = property.getAttributeValue(Utils.XML_ATTR_SIZE);
						boolean is_repeating = Boolean.valueOf(property.getAttributeValue(Utils.XML_ATTR_REPEATING));
						
						// set info in properties
						setTypeAttrProperty(attr, attr_type + ":" + attr_size + ":" + is_repeating);		
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private String getTypeObjProperty(String key) {
		if (m_typeObjProps.containsKey(key))
			return m_typeObjProps.getProperty(key);
		else
			return null;
	}
	
	
	private void setTypeAttrProperty(String key, String value) {
		m_typeAttrsProps.setProperty(key, value);
	}
	
	
	private String getTypeAttrProperty(String key) {
		if (m_typeAttrsProps.containsKey(key))
			return m_typeAttrsProps.getProperty(key);
		else
			return null;
	}
	
	
	private void setTypeObjProperty(String key, String value) {
		m_typeObjProps.setProperty(key, value);
	}
	
	
	public String getErrorMsg() {
		return m_errorMsg;
	}
	
	
	private void setErrorMsg(String msg) {
		m_errorMsg = msg;
	}
	
	
	public boolean success() {
		return m_state;
	}
	
}
