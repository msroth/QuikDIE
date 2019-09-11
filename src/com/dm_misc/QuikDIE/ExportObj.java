package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Export Content Module - ExportObj
 * (c) 2013-2019 MS Roth
 * 
 * ============================================================================
 */

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.dm_misc.dctm.DCTMBasics;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;

public class ExportObj {

    // private members
    private IDfSysObject m_sObj = null;
    private IDfSession m_session = null;
    private String m_exportPath = "";
    private boolean m_hasContent = false;
    private String m_typeName = "";
    private String m_contentType = "";
    private String m_objId = "";
    private String m_objName = "";
    private boolean m_isVirtualDoc = false;
    private String m_DOSExt = "";
    private ArrayList<ExportObj> m_VDChildren = new ArrayList<ExportObj>();
    private ArrayList<String> m_Renditions = new ArrayList<String>();
    private boolean m_hasRenditions = false;
    private boolean m_isParked = false;
    private String m_ACLName;
    private String m_ACLDomain;

    /*
     * Instantiate the ExportObj, set some private data, etc.
     */
    public ExportObj(String objId, String expPath, IDfSession session) {

        try {

            // set session for use in class
            if (session == null) {
                throw new Exception("session is null in Export obj");
            } else {
                m_session = session;
            }

            // get the sysobj
            m_sObj = (IDfSysObject) session.getObject(new DfId(objId));
            if (m_sObj == null) {
                throw new Exception("cannot retrieve sysobject: " + objId);
            }

            // set export path
            if (expPath == null || expPath.equalsIgnoreCase("") || expPath.length() < 3) {
                throw new Exception("export path is blank");
            } else {
                m_exportPath = expPath;
            }

            // v1.5
            // is content parked on BOCS?
            IDfId contId = m_sObj.getContentsId();
            if (contId.isObjectId()) {
	            IDfTypedObject tObj = (IDfTypedObject) session.getObject(contId);
	            if (tObj != null) {
	            	int parked = tObj.getInt("i_parked_state");  
	            	if (parked != 0)
	            		m_isParked = true;
	            }
            }
            
            // set hascontent and content related properties
            if ((m_sObj.getContentSize() > 0) && !isParked()){
                m_hasContent = true;
                m_contentType = m_sObj.getContentType();
                m_DOSExt = m_sObj.getFormat().getDOSExtension();
            } else {
                m_hasContent = false;
            }

            // get misc properties
            m_objId = m_sObj.getObjectId().toString();
            m_objName = m_sObj.getObjectName();
            m_typeName = m_sObj.getTypeName();
            
            // v1.7
            m_ACLName = m_sObj.getACLName();
            m_ACLDomain = m_sObj.getACLDomain();

            // if VD, gather children
            if (m_sObj.isVirtualDocument()) {
                m_isVirtualDoc = true;
                gatherVDChildren();
            }
            
            // v1.6 
            // gather rendition formats to export
            IDfCollection renditionCol = m_sObj.getRenditions("rendition, full_format, full_content_size");
            if (renditionCol != null) {
            	while (renditionCol.next()) {
            		if (renditionCol.getInt("rendition") > 0 && renditionCol.getInt("full_content_size") > 0) {
            			m_Renditions.add(renditionCol.getString("full_format"));
                		m_hasRenditions = true;
            		}
            	}
            	renditionCol.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    /*
     * This method orchestrates the export of the object's content (if it has 
     * any), its metadata, and if it is a virtual document, that of its children
     * and renditions. 
     */
    public int exportContent()  {
    	int count = 1;
    	
    	try {
	        // export folder metadata
	        if (DCTMBasics.isFolder(m_sObj)) {
	            System.out.println("Exporting folder: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            Utils.writeLog("Exporting folder: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            exportDocumentMetadata();
	            return count;
	        }
	
	        // export no content object metadata
	        if (m_sObj.getContentSize() == 0L) {
	            if (isVirtualDoc()) {
	                System.out.println("Exporting virtual doc root, no content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                Utils.writeLog("Exporting virtual doc root, no content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                exportDocumentMetadata();
	                count += exportVirtualDocChildren(); // export VD children if this is a content-less VD root
	            } else {
	                System.out.println("No content, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                Utils.writeLog("No content, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                exportDocumentMetadata();
	            }
	            return count;
	        }
	
	        // v1.5
	        // export BOCS parked metadata
	        if (isParked()) {
	            System.out.println("Content parked, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            Utils.writeLog("Content parked, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            exportDocumentMetadata();
	            return count;
	        }
	        
	        // export content file
	        String file = m_sObj.getFile(m_exportPath + "/" + m_sObj.getObjectId().toString() + "." + getDOSExt());
	        
	        // export virtual doc children
	        if (isVirtualDoc()) {
	            System.out.println("Exporting virtual doc root content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            Utils.writeLog("Exporting virtual doc root content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            exportDocumentMetadata();
	            count += exportVirtualDocChildren();
	        } else {
	        	// export normal content file
	            System.out.println("Exporting content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            Utils.writeLog("Exporting content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            exportDocumentMetadata();
	        }
	        
	        // v1.6
	        // export all renditions associated with an object
	        if (hasRenditions()) {
	        	for (String format : m_Renditions) {
	        		file = m_sObj.getFileEx(m_exportPath + "/" + m_sObj.getObjectId().toString() + ".rendition." + Utils.lookupDosExt(format, m_session), format, 0, false);
	        		System.out.println("\trendition "  + format + ": " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	        		Utils.writeLog("\trendition "  + format + ": " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	        		count++;
	        	}
        	}
	        
    	} catch (Exception e) {
            System.out.println("Error exporting content: " + e.getMessage());
            Utils.writeLog("Error exporting content: " + e.getMessage());
    	}

    	return count;  // return number of objects exported
    }

    
    /*
     * Export the object's metadata to an XML file.
     */
    private String exportDocumentMetadata() throws Exception {
        String filename = "";

        // figure out file suffix and build metadata file name
        if (DCTMBasics.isFolder(m_sObj)) {
            filename = Utils.FOLDER_FILE_EXT;
        } else {
            filename = Utils.METADATA_FILE_EXT;
        }
        filename = m_sObj.getObjectId().toString() + filename;

        // log the export
        System.out.println("\tmetadata file ==> " + filename);
        Utils.writeLog("\tmetadata file ==> " + filename);

        // open output file
        File file = new File(m_exportPath + "/" + filename);
        PrintWriter xmlFile = new PrintWriter(file);

        // output metadata XML file
        try {
            xmlFile.println(Utils.XML_HEADER);
            xmlFile.println(String.format(Utils.XML_OBJECT_OPEN_TEMPLATE, m_sObj.getObjectId().toString(), 
            		m_sObj.getTypeName(), Boolean.toString(hasContent()),  Boolean.toString(isVirtualDoc())));

            // save repo path
            xmlFile.println(String.format(Utils.XML_REPO_PATH_TEMPLATE, Utils.getObjectPath(m_sObj, m_session)));

            // save content file name
            if (hasContent()) {
                xmlFile.println(String.format(Utils.XML_CONTENT_FILE_TEMPLATE, getObjectId() + "." + getDOSExt()));
            }
            
            // get property values
            xmlFile.println(String.format(Utils.XML_PROPERTIES_TEMPLATE, buildAttrExportString()));

            // v1.7
            // get ACL values
            xmlFile.println(String.format(Utils.XML_PERMISSIONS_TEMPLATE, buildAccessExportString()));

            // export VD children
            if (isVirtualDoc()) {
                xmlFile.println(String.format(Utils.XML_VD_CHILDREN_TEMPLATE, buildVDAttrString()));
            }

            // v1.6
            // export rendition formats
            if (hasRenditions()) {
            	xmlFile.println(String.format(Utils.XML_RENDITIONS_TEMPLATE, buildRenditionAttrString()));
            }
            
            // close xml file
            xmlFile.println("</object>");

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            xmlFile.close();
        }

        return filename;
    }

    
    /*
     * Build the XML that contains the object properties.  Note this method
     * ignores many important properties such as i_aspect_id, etc.  Other 
     * properties can be added here by following the pattern.
     */
    private String buildAttrExportString() throws Exception {
        StringBuilder sb = new StringBuilder();
        // v1.7
        // added 'custom' tag to element
        String template = "<" + Utils.XML_PROPERTY_ELEMENT + " name=\"%s\" type=\"%s\" custom=\"%s\">%s</" + Utils.XML_PROPERTY_ELEMENT + ">\n";

        // basic attrs
        // v1.7
        // moved r_object_type to object element
        //sb.append(String.format(template, Utils.ATTR_OBJ_TYPE, "string", "false", m_sObj.getType().getName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_NAME, "string", "false", Utils.cleanXML(m_sObj.getObjectName())));
        sb.append(String.format(template, Utils.ATTR_OBJ_TITLE, "string", "false", Utils.cleanXML(m_sObj.getTitle())));
        sb.append(String.format(template, Utils.ATTR_OBJ_SUBJECT, "string", "false", Utils.cleanXML(m_sObj.getSubject())));
        sb.append(String.format(template, Utils.ATTR_OBJ_ACL_DOMAIN, "string", "false", m_sObj.getACLDomain()));
        sb.append(String.format(template, Utils.ATTR_OBJ_ACL_NAME, "string", "false", m_sObj.getACLName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_OWNER, "string", "false", m_sObj.getOwnerName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_VERSION, "string", "false", m_sObj.getVersionLabels().getImplicitVersionLabel()));
        sb.append(String.format(template, Utils.ATTR_OBJ_CREATOR, "string", "false", m_sObj.getCreatorName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_CREATE_DATE, "string", "false", m_sObj.getCreationDate().asString("yyyy-MM-dd HH:mm:ss")));
        sb.append(String.format(template, Utils.ATTR_OBJ_MODIFIER, "string", "false", m_sObj.getModifier()));
        sb.append(String.format(template, Utils.ATTR_OBJ_MODIFY_DATE, "string", "false", m_sObj.getModifyDate().asString("yyyy-MM-dd HH:mm:ss")));
        
        // get content specific attrs
        if (!DCTMBasics.isFolder(m_sObj)) {
            sb.append(String.format(template, Utils.ATTR_OBJ_CONTENT_TYPE, "string", "false", m_contentType));
	        sb.append(String.format(template, Utils.ATTR_OBJ_CHRONICLE_ID, "string", "false", m_sObj.getChronicleId().toString()));
	        sb.append(String.format(template, Utils.ATTR_OBJ_ANTECEDENT_ID, "string", "false", m_sObj.getAntecedentId().toString()));
        }

        // get type obj
        IDfType typeObj = m_sObj.getType();

        // get custom attrs past the default/inherited set
        if (! Utils.omitTypes(typeObj.getName())) {

            // get custom attrs
            int attrcnt = typeObj.getInt("attr_count") - typeObj.getInt("start_pos");
            if (attrcnt > 0) {
                for (int i = 0; i < attrcnt; i++) {
                    int offset = typeObj.getInt("start_pos") + i;
                    IDfAttr attrObj = m_sObj.getAttr(offset);
                    sb.append(String.format(template, attrObj.getName(), Utils.DATA_TYPES[attrObj.getDataType()], "true", Utils.cleanXML(m_sObj.getString(attrObj.getName()))));
                }
            }
        }
        return sb.toString();
    }

    
    /*
     * v1.7
     * Build the XML that contains the object access permissions.
     */
    private String buildAccessExportString() throws Exception {
        StringBuilder sb = new StringBuilder();
        String template = "<permission accessor_name=\"%s\" accessor_permit=\"%s\" accessor_x_permit=\"%s\" />\n";

        IDfACL aclObj = m_sObj.getACL();
        
        for (int i=0; i < aclObj.getAccessorCount(); i++) {
        	sb.append(String.format(template, Utils.cleanXML(aclObj.getAccessorName(i)), aclObj.getAccessorPermit(i), aclObj.getAccessorXPermitNames(i)));
        }

        return sb.toString();
    }
    
    
    /*
     * Build XML to hold virtual doc children references.
     */
    private String buildVDAttrString() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (ExportObj eObj : m_VDChildren) {
            sb.append("<vd_child>" + eObj.getObjectId() + "</vd_child>\n");
        }
        return sb.toString();
    }
    
    
    /*
     * Build XML to hold rendition formats
     */
    private String buildRenditionAttrString() throws Exception {
    	StringBuilder sb = new StringBuilder();
    	for (String format : m_Renditions) {
    		String dosExt = Utils.lookupDosExt(format, m_session);
    		sb.append("<rendition format=\"" + format + "\">" + m_objId + ".rendition." + dosExt + "</rendition>\n");
    	}
    	return sb.toString();
    }

    
    /*
     * Export each virtual doc's children's content and metadata.  This
     * is sort of recursive if you think about it.
     */
    private int exportVirtualDocChildren() throws Exception {
    	int vCount = 0;
    	
    	System.out.println("--------------------------------------");
    	Utils.writeLog("--------------------------------------");
    	System.out.println("Exporting virtual doc child content...");
    	Utils.writeLog("Exporting virtual doc child content...");

        for (ExportObj eObj : m_VDChildren) {
        	vCount += eObj.exportContent();  // count the number of children exported
        }
        
        System.out.println("End virtual doc export (" + m_objId + ")");
        Utils.writeLog("End virtual doc export (" + m_objId + ")");
        System.out.println("--------------------------------------");
    	Utils.writeLog("--------------------------------------");
    	return vCount;
    }

    
    /*
     * Gather the objects that are children of this virtual doc, and add them
     * to the m_VDChildren ArrayList as ExportObjs.
     */
    private void gatherVDChildren() throws Exception {
        String DQL = "select r_object_id from dm_sysobject in document id('" + getObjectId() + "') descend";
        IDfCollection col = DCTMBasics.runSelectQuery(DQL, getSession());
        while (col.next()) {
            String objId = col.getString("r_object_id");

            // if it isn't itself, add to child array
            if (!objId.equalsIgnoreCase(getObjectId())) {
                m_VDChildren.add(new ExportObj(objId, getExportPath(), getSession()));
            }
        }
        col.close();
    }
    

    // ========== Getter Methods ==========
        
    public boolean hasContent() {
        return m_hasContent;
    }

    public IDfSysObject getSysObject() {
        return m_sObj;
    }

    public String getTypeName() {
        return m_typeName;
    }

    public String getObjectId() {
        return m_objId;
    }

    public String getObjName() {
        return m_objName;
    }

    public boolean isVirtualDoc() {
        return m_isVirtualDoc;
    }

    public IDfSession getSession() {
        return m_session;
    }

    public String getExportPath() {
        return m_exportPath;
    }

    public String getDOSExt() {
        return m_DOSExt;
    }
    
    // v1.5
    public boolean isParked() {
    	return m_isParked;
    }
    
    // v1.6
    public boolean hasRenditions() {
    	return m_hasRenditions;
    }
    
    // v1.7
    public String getACLName() {
    	return m_ACLName;
    }
    
    public String getACLDomain() {
    	return m_ACLDomain;
    }
}

/* ============================================================================
 * <SDG><
 * ============================================================================
 */