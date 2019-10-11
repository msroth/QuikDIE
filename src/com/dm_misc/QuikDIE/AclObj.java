package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Import Content Module - TypeObj
 * (c) 2013-2019 MS Roth
 * 
 * ============================================================================
 */


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Attribute;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.Attributes;

import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfSession;

public class AclObj {

	private boolean m_state = false;
	private File m_metadataFile = null;
	private IDfSession m_session = null;
	private String m_errorMsg = "";
	
	private Properties m_AclObjProps = new Properties();
	private Properties m_AclAttrsProps = new Properties();
	private ArrayList<String> m_Permissions = new ArrayList<String>();
	
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
	
			// create new ACL obj
			IDfACL acl = (IDfACL) m_session.newObject("dm_acl");
			if (acl != null) {
				acl.setObjectName(getAclObjProperty(Utils.XML_ATTR_NAME));
				acl.setDomain(getAclObjProperty(Utils.XML_ATTR_DOMAIN));
				acl.setACLClass(Integer.parseInt(getAclObjProperty(Utils.XML_ATTR_CLASS)));
				acl.setDescription(getAclObjProperty("description"));
				acl.save();
				
				
				for (int i=0; i < m_Permissions.size(); i++) {
					String[] permission = m_Permissions.get(i).split(":");
					
					// basic permits
					IDfPermit permit = new DfPermit();
					permit.setAccessorName(permission[0]);
					permit.setPermitType(Integer.parseInt(permission[3]));
					permit.setPermitValue(permission[1]);
					acl.grantPermit(permit);
					
					// extended permits
//					permit = new DfPermit();
//					permit.setAccessorName(permission[0]);
//					permit.setPermitType(IDfPermit.DF_EXTENDED_PERMIT);
//					permit.setPermitValue(permission[2]);
//					acl.grantPermit(permit);
					acl.grant(permission[0], Integer.parseInt(permission[1]),  permission[2]);
				}
				acl.save();
			}
			

			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void readMetadataFile() {

		// SAX
		SAXBuilder builder = new SAXBuilder();
		Document Doc = null;

		try {
			Doc = builder.build(m_metadataFile);
			Element root = Doc.getRootElement();

			// get name
			setAclObjProperty(Utils.XML_ATTR_NAME, root.getAttributeValue(Utils.XML_ATTR_NAME));
			
			// get domain
			setAclObjProperty(Utils.XML_ATTR_DOMAIN, root.getAttributeValue(Utils.XML_ATTR_DOMAIN));
			
//			// get global value
//			setAclObjProperty(Utils.XML_ATTR_GLOBAL, root.getAttributeValue(Utils.XML_ATTR_GLOBAL));
//			
//			// get class value
//			setAclObjProperty(Utils.XML_ATTR_CLASS, root.getAttributeValue(Utils.XML_ATTR_CLASS));
						
			// loop over child elements
			List<Element> xml_elements = root.getChildren();
			for(int i=0 ; i < xml_elements.size(); i++) {
				Element element = xml_elements.get(i);

				// find properties element
				if (element.getName().equalsIgnoreCase("properties")) {
					List<Element> properties = element.getChildren();
					
					// loop over all properties
					for(int j=0 ; j < properties.size(); j++) {
						Element property = properties.get(j);
						List<Attribute> attrs = property.getAttributes();
						setAclObjProperty(attrs.get(0).getValue(), attrs.get(1).getValue());
					}
				}
				
				// get permit info
				if (element.getName().equalsIgnoreCase(Utils.XML_PERMISSIONS_ELEMENT)) {
					List<Element> properties = element.getChildren();
					
					// loop over all attributes
					for(int j=0 ; j < properties.size(); j++) {
						Element property = properties.get(j);
						
						// dm_world:3:CHANGE_LOCATION, EXECUTE_PROCEDURE:0:false
						String perms = property.getAttributeValue("accessor_name");
						perms += ":" + property.getAttributeValue("accessor_permit");
						perms += ":" + property.getAttributeValue("accessor_x_permit");
						// perms += ":" + property.getAttributeValue("accessor_x_permit_value");
						perms += ":" + property.getAttributeValue("permit_type");
						perms += ":" + property.getAttributeValue("is_group");
						m_Permissions.add(perms);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private String getAclObjProperty(String key) {
		if (m_AclObjProps.containsKey(key))
			return m_AclObjProps.getProperty(key);
		else
			return null;
	}
	
	private String getAclAttrProperty(String key) {
		if (m_AclAttrsProps.containsKey(key))
			return m_AclAttrsProps.getProperty(key);
		else
			return null;
	}
	
	private void setAclObjProperty(String key, String value) {
		m_AclObjProps.setProperty(key, value);
	}
	
	
	private void setAclAttrProperty(String key, String value) {
		m_AclAttrsProps.setProperty(key, value);
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
