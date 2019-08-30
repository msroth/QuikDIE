package com.dm_misc.QuikDIE;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;

public class ImportObj {

	private boolean m_state = false;
	private boolean m_useExportRepoAttrs = false;
	private File m_metadataFile = null;
	private File m_contentFile = null;
	private Properties m_importObjProps = new Properties();
	private IDfSession m_session = null;
	private String m_objId = null;
	private IDfSysObject m_sObj = null;
	private String m_errorMsg = "";

	private static final String XML_ATTR_NAME = "name";
	private static final String XML_ATTR_TYPE = "type";
	private static final String XML_PROPERTIES = "properties";
	private static final String XML_REPO_PATH="repo_path";

	private List<String> BASE_ATTRS = Arrays.asList(
			Utils.ATTR_OBJ_NAME, 
			Utils.ATTR_OBJ_TYPE, 
			Utils.ATTR_OBJ_TITLE, 
			Utils.ATTR_OBJ_SUBJECT, 
			Utils.ATTR_OBJ_CONTENT_TYPE);

	private List<String> REPO_SPECIFIC_ATTRS = Arrays.asList(
			Utils.ATTR_OBJ_VERSION,
			Utils.ATTR_OBJ_ACL_DOMAIN,
			Utils.ATTR_OBJ_ACL_NAME,
			Utils.ATTR_OBJ_OWNER,
			Utils.ATTR_OBJ_ID);

	private ArrayList<String> CUSTOM_ATTRS = new ArrayList<String>();

	public ImportObj(String metadatafile, IDfSession session) {

		try {
			// set session for use in class
			if (session == null)
				throw new Exception ("session is null");
			else
				m_session = session;
			
			// check if using export attrs or not
			m_useExportRepoAttrs = Boolean.parseBoolean(Utils.getProperty(Utils.IMPORT_USE_EXPORT_REPO_ATTRS_KEY));
			
			// read meatadata file
			m_metadataFile = new File(metadatafile);
			loadMetadataFile(m_metadataFile);

			// check that obj type exists
			if (!Utils.checkTypeExists(getImportObjProperty(Utils.ATTR_OBJ_TYPE), m_session)) {
				setErrorMsg("type " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " does not exist in target Docbase");
				throw new Exception ("type " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " does not exist in target Docbase");
			}

			// create object
			m_sObj = (IDfSysObject) session.newObject(getImportObjProperty(Utils.ATTR_OBJ_TYPE));
			if (m_sObj != null)
				m_objId = m_sObj.getObjectId().toString();
			else {
				setErrorMsg("could not create " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " object for " + m_contentFile.getAbsolutePath());
				throw new Exception ("could not create " + getImportObjProperty(Utils.ATTR_OBJ_TYPE) + " object for " + m_contentFile.getAbsolutePath());
			}
			
			// set attrs
			setObjAttrs(m_useExportRepoAttrs);
			
			// link
			linkObj();
			
			// find and set content file
			if (hasContent()) {
				m_contentFile = findContentFile();
				m_sObj.setContentType(getImportObjProperty(Utils.ATTR_OBJ_CONTENT_TYPE));
				m_sObj.setFile(m_contentFile.getAbsolutePath());
			}
			
			// save
			m_sObj.save();
			m_state = true;

		} catch (Exception e) {
			e.printStackTrace();
			setErrorMsg(e.getMessage());
		}
		
	}

	private String getImportObjProperty(String key) {
		if (m_importObjProps.containsKey(key))
			return m_importObjProps.getProperty(key);
		else
			return null;
	}

	private void setImportObjProperty(String key, String value) {
		m_importObjProps.setProperty(key, value);
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
		return Boolean.parseBoolean(getImportObjProperty(Utils.ATTR_OBJ_HAS_CONTENT));
	}

	public boolean ignoreExportedRepoProperties() {
		return Boolean.parseBoolean(Utils.getProperty(Utils.IMPORT_USE_EXPORT_REPO_ATTRS_KEY));
	}

	public String getObjectId() {
		return m_objId;
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
	
	private File findContentFile() {
		File file = null;

		String obj_id = getImportObjProperty(Utils.ATTR_OBJ_ID);
		String ext = getImportObjProperty(Utils.ATTR_OBJ_CONTENT_TYPE);
		String path = Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY);
		String filename = path + "/" + obj_id + "." + ext;
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

	private void loadMetadataFile(File metadatafile) {

		/*
	 	<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
		<object r_object_id="09003afd804f2ba8" content="true" virtdoc="true">
		<properties>
		<property name="object_type" type="string">dm_document</property>
		<property name="object_name" type="string">DUMMY_DOCUMENT_temp</property>
		<property name="title" type="string"></property>
		<property name="subject" type="string"></property>
		<property name="acl_domain" type="string">eccsadmin</property>
		<property name="acl_name" type="string">dm_45003afd80008d08</property>
		<property name="owner_name" type="string">eccsadmin</property>
		<property name="version" type="string">1.1</property>
		<property name="a_content_type" type="string">crtext</property>
		</properties>
		<repo_path>/Temp</repo_path>
		<vd_children>
		<vd_child>09003afd80000210</vd_child>
		<vd_child>09003afd804e9914</vd_child>
		</vd_children>
		</object>
		*/



		// SAX
		SAXBuilder builder = new SAXBuilder();
		Document Doc = null;
		try {
			Doc = builder.build(metadatafile);
			Element root = Doc.getRootElement();
			//System.out.println("root=" + root.getName());

			// get obj id
			String attr = root.getAttributeValue(Utils.ATTR_OBJ_ID);
			//System.out.println(ATTR_OBJ_ID + "=" + attr);
			setImportObjProperty(Utils.ATTR_OBJ_ID, attr);

			// get has content value
			attr = root.getAttributeValue(Utils.ATTR_OBJ_HAS_CONTENT);
			//System.out.println(ATTR_OBJ_HAS_CONTENT + "=" + attr);
			setImportObjProperty(Utils.ATTR_OBJ_HAS_CONTENT, attr);

			// loop over child elements (properties and repo_path)
			List<Element> level1 = root.getChildren();
			for(int i=0 ; i < level1.size(); i++) {
				Element element = level1.get(i);
				//System.out.println("element1[" + i + "]=" + element.getName());

				// get repo_path
				if (element.getName().equalsIgnoreCase(XML_REPO_PATH)) {
					attr = element.getValue();
					setImportObjProperty(XML_REPO_PATH, attr);
					//System.out.println(XML_REPO_PATH + "=" + attr);
				}

				// loop over all properties
				if (element.getName().equalsIgnoreCase(XML_PROPERTIES)) {
					List<Element> level2 = element.getChildren();
					for(int j=0 ; j < level2.size(); j++) {
						Element element2 = level2.get(j);
						//System.out.print("element2[" + j + "]=" + element2.getName() + ": ");
						attr = element2.getAttributeValue(XML_ATTR_NAME);
						String type = element2.getAttributeValue(XML_ATTR_TYPE);
						String elementVal = element2.getValue();
						//System.out.println(XML_ATTR_NAME + " = " + attr + " : " + type + " ==> " + elementVal);
						setImportObjProperty(attr,elementVal);
						
						// collect custom attr names
						if(isCustomAttr(attr))
							CUSTOM_ATTRS.add(attr);
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	private void setObjAttrs(boolean useRepoAttrs) throws Exception {
		
		// set generic attrs
		m_sObj.setString(Utils.ATTR_OBJ_NAME, getImportObjProperty(Utils.ATTR_OBJ_NAME));
		m_sObj.setString(Utils.ATTR_OBJ_TITLE, getImportObjProperty(Utils.ATTR_OBJ_TITLE));
		m_sObj.setString(Utils.ATTR_OBJ_SUBJECT, getImportObjProperty(Utils.ATTR_OBJ_SUBJECT));
		
		// set attrs specific to EXPORT repo?
		if (m_useExportRepoAttrs) {
			// version and obj_id cannot be set
			m_sObj.setString(Utils.ATTR_OBJ_ACL_DOMAIN, getImportObjProperty(Utils.ATTR_OBJ_ACL_DOMAIN));
			m_sObj.setString(Utils.ATTR_OBJ_ACL_NAME, getImportObjProperty(Utils.ATTR_OBJ_ACL_NAME));
			m_sObj.setString(Utils.ATTR_OBJ_OWNER, getImportObjProperty(Utils.ATTR_OBJ_OWNER));
		}
		
		// set custom attrs
		for (String k : CUSTOM_ATTRS) {
			m_sObj.setString(k, getImportObjProperty(k));
		}
		
	}
		
	
	private boolean isCustomAttr(String attr) {
		if (!BASE_ATTRS.contains(attr) && !REPO_SPECIFIC_ATTRS.contains(attr))
			return true;
		else
			return false;
	}
	

	private void linkObj() throws Exception {
		
		String path = Utils.getProperty(Utils.IMPORT_REPO_PATH_KEY) + getImportObjProperty(XML_REPO_PATH);
		
		// validate path exists (or create it)
		Utils.validateRepoPath(path, m_session, true);
		
		// do link
		m_sObj.link(path);	
		
	}
	
	
}
