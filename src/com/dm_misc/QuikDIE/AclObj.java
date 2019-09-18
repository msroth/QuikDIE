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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfSession;

public class AclObj {

	private boolean m_state = false;
	private File m_metadataFile = null;
	private IDfSession m_session = null;
	private String m_errorMsg = "";
	
	public AclObj(String metadatafile, IDfSession session) {
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
	

			IDfACL acl = (IDfACL) session.newObject("dm_acl");
			if (acl != null) {
				acl.setObjectName(aclName);
				acl.setDescription(description);
				acl.save();
			}
			
			// loop over values
			IDfPermit permit = new DfPermit();
			if (permit != null) {
				permit.setAccessorName("Bhuwan User");
				permit.setPermitType(IDfPermit.DF_ACCESS_PERMIT);
				permit.setPermitValue(IDfACL.DF_PERMIT_READ_STR);
				acl.grantPermit(permit);
				acl.save();
			}
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
				if (element.getName().equalsIgnoreCase(Utils.XML_ATTRIBUTES_ELEMENT)) {
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
	
	
	private String getAclObjProperty(String key) {
		if (m_typeObjProps.containsKey(key))
			return m_typeObjProps.getProperty(key);
		else
			return null;
	}
	
	
	private void setAclAttrProperty(String key, String value) {
		m_typeAttrsProps.setProperty(key, value);
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
