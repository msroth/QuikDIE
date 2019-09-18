package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Import Content Module - ImportObj
 * (c) 2013-2019 MS Roth
 * 
 * ============================================================================
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.documentum.fc.client.DfACL;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfVirtualDocument;
import com.documentum.fc.common.DfId;
import com.sun.jmx.mbeanserver.Util;

public class ImportObj {

	// private members
	private boolean m_state = false;
	private File m_metadataFile = null;
	private File m_contentFile = null;
	private Properties m_importObjProps = new Properties();
	private Properties m_importObjPermissions = new Properties();
	private Properties m_importObjVDChildren = new Properties();
	private Properties m_importObjRenditions = new Properties();
	private IDfSession m_session = null;
	private String m_objId = null;
	private IDfSysObject m_sObj = null;
	private String m_errorMsg = "";

	private ArrayList<String> m_baseAttrs = new ArrayList<String>();
	private ArrayList<String> m_customAttrs = new ArrayList<String>();


	public ImportObj(String metadatafile, IDfSession session) {

		try {
			// set session for use in class
			if (session == null)
				throw new Exception ("session is null");
			else
				m_session = session;
			
			// read metadata file and load member properties
			m_metadataFile = new File(metadatafile);
			readMetadataFile();
				
			// check that obj type exists -- custom types should have already been created
			if (!Utils.checkTypeExists(getImportObjProperty(Utils.ATTR_OBJ_TYPE), m_session)) {
				setErrorMsg("type " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " does not exist in target Docbase");
				throw new Exception ("type " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " does not exist in target Docbase");
			}

			// check that ACL exits -- ACLs should have already been created
			if ((getImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN) != "") || (getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME) != "")) {
				if (!Utils.checkACLExists(getImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN), getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME), session)) {
					setErrorMsg("acl " + getImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN) + ":" + getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME) + " does not exist in target Docbase");
					throw new Exception ("acl " + getImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN) + ":" + getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME) + " does not exist in target Docbase");
				}
			}
			
			// create object
			m_sObj = (IDfSysObject) session.newObject(getImportObjProperty(Utils.ATTR_OBJ_TYPE));
			if (m_sObj != null)
				m_objId = m_sObj.getObjectId().toString();
			else {
				setErrorMsg("could not create " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " object for " + m_contentFile.getAbsolutePath());
				throw new Exception ("could not create " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " object for " + m_contentFile.getAbsolutePath());
			}
			
			// set attrs on new repo object
			setImportObjAttrs();
			
			// link
			linkObj();
			
			// save
			m_sObj.save();
			
			// find and set content file
			if (hasContent()) {
				m_contentFile = findContentFile();
				
				if (m_contentFile != null) {
					m_sObj.setContentType(getImportObjProperty(Utils.ATTR_OBJ_CONTENT_TYPE));
					m_sObj.setFile(m_contentFile.getAbsolutePath());
				}
				m_sObj.save();
			}
			
			
			m_state = true;

		} catch (Exception e) {
			e.printStackTrace();
			setErrorMsg(e.getMessage());
		}
	}


	private File findContentFile() {
		File file = null;


		String path = Utils.getConfigProperty(Utils.IMPORT_FILES_PATH_KEY);
		String filename = path + "/" + getImportObjProperty(Utils.XML_CONTENT_FILE_ELEMENT);
		//System.out.println("\tcontent file is: " + filename);
		file = new File(filename);
		if (file.exists())
			return file;
		else {
			Utils.writeLog("WARN:  could not find content file " + filename + " for content object " + m_objId);
			setErrorMsg("WARN:  could not find content file " + filename + " for content object " + m_objId);
			return null;
		}
	}

	private void readMetadataFile() {

		// SAX
		// File metadataFile = new File(m_metadatafile);
		SAXBuilder builder = new SAXBuilder();
		Document Doc = null;
		
		try {
			Doc = builder.build(m_metadataFile);
			Element root = Doc.getRootElement();

			// get obj id
			setImportObjProperty(Utils.ATTR_OBJ_ID, root.getAttributeValue(Utils.ATTR_OBJ_ID));
			
			// get obj type
			setImportObjProperty(Utils.ATTR_OBJ_TYPE, root.getAttributeValue(Utils.ATTR_OBJ_TYPE));
						
			// get has content value
			setImportObjProperty(Utils.ATTR_OBJ_HAS_CONTENT, root.getAttributeValue(Utils.ATTR_OBJ_HAS_CONTENT));

			// get is_virtdoc
			setImportObjProperty(Utils.ATTR_OBJ_VIRTUAL_DOC, root.getAttributeValue(Utils.ATTR_OBJ_VIRTUAL_DOC));
			
			// loop over child elements
			List<Element> xml_elements = root.getChildren();
			for(int i=0 ; i < xml_elements.size(); i++) {
				Element element = xml_elements.get(i);

				// get repo_path
				if (element.getName().equalsIgnoreCase(Utils.XML_REPO_PATH_ELEMENT)) {
					setImportObjProperty(Utils.XML_REPO_PATH_ELEMENT, element.getValue());
				}

				// get content file
				if (element.getName().equalsIgnoreCase(Utils.XML_CONTENT_FILE_ELEMENT)) {
					setImportObjProperty(Utils.XML_CONTENT_FILE_ELEMENT, element.getValue());
				}

				// loop over all properties
				if (element.getName().equalsIgnoreCase(Utils.XML_PROPERTIES_ELEMENT)) {
					List<Element> properties = element.getChildren();
					for(int j=0 ; j < properties.size(); j++) {
						
						Element property = properties.get(j);
						String attr = property.getAttributeValue(Utils.XML_ATTR_NAME);
						boolean is_custom = Boolean.valueOf(property.getAttributeValue(Utils.XML_ATTR_CUSTOM));
						String value = property.getValue();
						
						// filter out system generated acls.
						if (attr.equalsIgnoreCase(Utils.ATTR_OBJ_ACL_NAME) && value.startsWith("dm_")) {
							setImportObjProperty(Utils.ATTR_OBJ_ACL_NAME,"");
							setImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN,"");
						} else {
							// save the properties to the object
							setImportObjProperty(attr,value);
						}
						
						// build list of base and custom attrs
						if (is_custom) {
							m_customAttrs.add(attr);
						} else {
							if (!Utils.skipAttrs(attr)) {
								// keep acl_name out if it is blank
								if (getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME) != "") {
									m_baseAttrs.add(attr);
								}
							}
						}
					}
				}

				// loop over permissions
				if (element.getName().equalsIgnoreCase(Utils.XML_PERMISSIONS_ELEMENT)) {
					List<Element> permissions = element.getChildren();
					for(int j=0 ; j < permissions.size(); j++) {
						
						Element permission = permissions.get(j);
						String accessor = permission.getAttributeValue(Utils.XML_ATTR_ACCESSOR);
						String permit = permission.getAttributeValue(Utils.XML_ATTR_ACCESSOR_PERMIT);
						String xpermit = permission.getAttributeValue(Utils.XML_ATTR_ACCESSOR_XPERMIT);
						// save the permissions to properties
						setImportObjPermissions(accessor, permit + ":" + xpermit);
					}
				}
				
				// loop over vd children
				if (element.getName().equalsIgnoreCase(Utils.XML_VD_CHILDREN_ELEMENT)) {
					List<Element> vd_children = element.getChildren();
					for(int j=0 ; j < vd_children.size(); j++) {
						
						Element vd_child = vd_children.get(j);
						String value = vd_child.getValue();
						// save vd children to properties
						setImportObjVDChildren(value, value);
					}
				}
				
				// loop over renditions
				if (element.getName().equalsIgnoreCase(Utils.XML_RENDITIONS_ELEMENT)) {
					List<Element> renditions = element.getChildren();
					for(int j=0 ; j < renditions.size(); j++) {
						
						Element rendition = renditions.get(j);
						String format = rendition.getAttributeValue(Utils.XML_ATTR_FORMAT);
						String value = rendition.getValue();
						// save renditions to properties
						setImportObjRenditions(format, value);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	private void setImportObjAttrs() throws Exception {
		
		// set base attrs
		for (String attr : m_baseAttrs) {
			// omit certain attrs that can't be set
			m_sObj.setString(attr, getImportObjProperty(attr));
		}
		
		// set custom attrs
		for (String attr : m_customAttrs) {
			m_sObj.setString(attr, getImportObjProperty(attr));
		}
		
		
		// set permissions if no acl
		if (getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME).isBlank()) {

			Set<String> keys = m_importObjPermissions.stringPropertyNames();
			for (String key : keys) {
				String[] permissions = m_importObjPermissions.getProperty(key).split(":");
				int permit = Integer.valueOf(m_importObjPermissions.getProperty(key).split(":")[0]);
				String x_permit = null;
				if (permissions.length == 2) {
					x_permit = m_importObjPermissions.getProperty(key).split(":")[1].trim();
				} 
						
				m_sObj.grant(key, permit, x_permit);
			}
		} else {
			// check that acl exists - it should
			if (Utils.checkACLExists(getImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN), getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME), m_session)) {
				m_sObj.setString(Utils.ATTR_OBJ_ACL_DOMAIN, getImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN));
				m_sObj.setString(Utils.ATTR_OBJ_ACL_NAME, getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME));
			} else {
				// noop - just create a system ACL
			}
			
		}
			
	}
		

	private void linkObj() throws Exception {
		
		String path = Utils.getConfigProperty(Utils.IMPORT_REPO_PATH_KEY) + getImportObjProperty(Utils.XML_REPO_PATH_ELEMENT);
		
		// validate path exists (or create it)
		Utils.validateRepoPath(path, m_session, true);
		
		// do link
		m_sObj.link(path);	
		
	}
	

//	private void importVDChildren() {
//
//		// make this obj a VD
//		try {
//			m_sObj.checkout();
//			m_sObj.setIsVirtualDocument(true);
//			IDfVirtualDocument vDoc = m_sObj.asVirtualDocument("CURRENT", false);
//
//    	
//			// Import and add children
//			Set<String> keys = m_importObjVDChildren.stringPropertyNames();
//			for (String key : keys) {
//				String xmlFile = Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY) + "\\" + key	+ Utils.METADATA_FILE_EXT;
//				File metadataFile = new File(xmlFile);
//
//				// SAX
//				// open the vd child metadata file to get filename
//				SAXBuilder builder = new SAXBuilder();
//				Document Doc = null;
//				String filename = "";
//
//				Doc = builder.build(metadataFile);
//				Element root = Doc.getRootElement();
//
//				// loop over child elements
//				List<Element> xml_elements = root.getChildren();
//				for (int i = 0; i < xml_elements.size(); i++) {
//					Element element = xml_elements.get(i);
//
//					// get content file
//					if (element.getName().equalsIgnoreCase(Utils.XML_CONTENT_FILE_ELEMENT)) {
//						filename = element.getValue();
//					}
//				}
//
//				// recursive sort of thing importing using the ImportObj class
//				ImportObj iObj = new ImportObj(Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY) + "\\" + filename, m_session);
//				vDoc.addNode(vDoc.getRootNode(), null, iObj.m_sObj.getChronicleId(), null, false, false);
//				System.out.println("\tAttatched virtual document child: " + filename);
//				metadataFile = null;
//		    	
//			}
//
//	    	
//	    	// save VD root
//			m_sObj.checkin(false, "");
//			
//		} catch (Exception e) {
//			System.out.println("\tERROR: attaching virtual document child: " + e.toString());
//			return;
//		}
//	}
	
	
	// ----- Getters/Setters -----
	
	
	public Properties getVDChildren() {
		return m_importObjVDChildren;
	}
	
	public Properties getRenditions() {
		return m_importObjRenditions;
	}
	
	private String getImportObjProperty(String key) {
		if (m_importObjProps.containsKey(key))
			return m_importObjProps.getProperty(key);
		else
			return null;
	}
	
	private String getImportObjPermissions(String key) {
		if (m_importObjPermissions.containsKey(key))
			return m_importObjPermissions.getProperty(key);
		else
			return null;
	}
	
	private String getImportObjVDChildren(String key) {
		if (m_importObjVDChildren.containsKey(key))
			return m_importObjVDChildren.getProperty(key);
		else
			return null;
	}
	
	private String getImportObjRenditions(String key) {
		if (m_importObjRenditions.containsKey(key))
			return m_importObjRenditions.getProperty(key);
		else
			return null;
	}

	private void setImportObjProperty(String key, String value) {
		m_importObjProps.setProperty(key, value);
	}

	private void setImportObjPermissions(String key, String value) {
		m_importObjPermissions.setProperty(key, value);
	}
	
	private void setImportObjVDChildren(String key, String value) {
		m_importObjVDChildren.setProperty(key, value);
	}
	
	private void setImportObjRenditions(String key, String value) {
		m_importObjRenditions.setProperty(key, value);
	}
	
	public boolean success() {
		return m_state;
	}

	public File metadataFile() {
		return m_metadataFile;
	}

	public File contentFile() {
		return m_contentFile;
	}

	public boolean hasContent() {
		return Boolean.valueOf(getImportObjProperty(Utils.ATTR_OBJ_HAS_CONTENT));
	}

	public String getObjectId() {
		return m_objId;
	}

	public String getOrigObjId() {
		return getImportObjProperty(Utils.ATTR_OBJ_ID);
	}
	
	public String getImportFileName() {
		return m_metadataFile.getName();
	}

	public IDfSysObject getSysObject() {
		return m_sObj;
	}

	public String getErrorMsg() {
		return m_errorMsg;
	}
	
	private void setErrorMsg(String msg) {
		m_errorMsg = msg;
	}
	
	
	
}
