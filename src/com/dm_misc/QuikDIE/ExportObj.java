package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Export Content Module - ExportObj
 * (c) 2013-2015 MS Roth
 * 
 * ============================================================================
 */

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.dm_misc.dctm.DCTMBasics;
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
    private boolean m_isParked = false;

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

            // if VD, gather children
            if (m_sObj.isVirtualDocument()) {
                m_isVirtualDoc = true;
                gatherVDChildren();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * This method orchestrates the export of the object's content (if it has 
     * any), its metadata, and if it is a virtual document, that of its children. 
     */
    public boolean exportContent()  {
    	boolean rv = true;
    	
    	try {
	        // folder
	        if (DCTMBasics.isFolder(m_sObj)) {
	            System.out.println("Exporting folder: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            Utils.writeLog("Exporting folder: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            exportDocumentMetadata();
	            return rv;
	        }
	
	        // no content
	        if (m_sObj.getContentSize() == 0L) {
	            if (isVirtualDoc()) {
	                System.out.println("Exporting virtual doc root, no content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                Utils.writeLog("Exporting virtual doc root, no content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                exportDocumentMetadata();
	                exportVirtualDocChildren();
	                System.out.println("End virtual doc export (" + getObjectId() + ")");
	                Utils.writeLog("End virtual doc export (" + getObjectId() + ")");
	            } else {
	                System.out.println("No content, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                Utils.writeLog("No content, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	                exportDocumentMetadata();
	            }
	            return rv;
	        }
	
	        // export file
	        String file = m_sObj.getFile(m_exportPath + "/" + m_sObj.getObjectId().toString() + "." + getDOSExt());
	
	        // virtual doc
	        if (isVirtualDoc()) {
	            System.out.println("Exporting virtual doc root content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            Utils.writeLog("Exporting virtual doc root content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            exportDocumentMetadata();
	            exportVirtualDocChildren();
	            System.out.println("End virtual doc export (" + getObjectId() + ")");
	            Utils.writeLog("End virtual doc export (" + getObjectId() + ")");
	        // v1.5
	        } else if (isParked()) {
	            System.out.println("Content parked, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            Utils.writeLog("Content parked, exporting metadata only: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName());
	            exportDocumentMetadata();
	        } else {
	            System.out.println("Exporting content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            Utils.writeLog("Exporting content: " + Utils.getObjectPath(m_sObj, m_session) + "/" + m_sObj.getObjectName() + " ==> " + file);
	            exportDocumentMetadata();
	        }
	        
    	} catch (Exception e) {
    		rv = false;
            System.out.println("Error exporting content: " + e.getMessage());
            Utils.writeLog("Error exporting content: " + e.getMessage());
    	}
        return rv;
    }

    private String exportDocumentMetadata() throws Exception {
        String filename = "";

        // figure out file suffix
        if (DCTMBasics.isFolder(m_sObj)) {
            filename = Utils.FOLDER_FILE_EXT;
        } else {
            filename = Utils.METADATA_FILE_EXT;
        }
        filename = m_sObj.getObjectId().toString() + filename;

        // log export
        System.out.println("\tmetadata file ==> " + filename);
        Utils.writeLog("\tmetadata file ==> " + filename);

        // open output file
        File file = new File(m_exportPath + "/" + filename);
        PrintWriter xmlFile = new PrintWriter(file);

        // output metadata XML file
        try {
            xmlFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>");
            xmlFile.print("<object r_object_id=\"" + m_sObj.getObjectId().toString() + "\" ");
            xmlFile.print("content=\"" + Boolean.toString(hasContent()) + "\" ");
            xmlFile.println("virtdoc=\"" + Boolean.toString(isVirtualDoc()) + "\">");

            // get property values
            xmlFile.println("<properties>");
            xmlFile.print(buildAttrExportString());
            xmlFile.println("</properties>");

            // save repo path
            xmlFile.println("<repo_path>" + Utils.getObjectPath(m_sObj, m_session) + "</repo_path>");

            // save content file name
            if (hasContent()) {
                xmlFile.println("<content_file>" + getObjectId() + "." + getDOSExt() + "</content_file>");
            }

            // export VD children
            if (isVirtualDoc()) {
                xmlFile.println(buildVDAttrString());
            }

            // close markup
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
        String template = "<property name=\"%s\" type=\"%s\">%s</property>\n";

        // basic attrs
        sb.append(String.format(template, Utils.ATTR_OBJ_TYPE, "string", m_sObj.getType().getName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_NAME, "string", Utils.cleanXML(m_sObj.getObjectName())));
        sb.append(String.format(template, Utils.ATTR_OBJ_TITLE, "string", Utils.cleanXML(m_sObj.getTitle())));
        sb.append(String.format(template, Utils.ATTR_OBJ_SUBJECT, "string", Utils.cleanXML(m_sObj.getSubject())));
        sb.append(String.format(template, Utils.ATTR_OBJ_ACL_DOMAIN, "string", m_sObj.getACLDomain()));
        sb.append(String.format(template, Utils.ATTR_OBJ_ACL_NAME, "string", m_sObj.getACLName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_OWNER, "string", m_sObj.getOwnerName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_VERSION, "string", m_sObj.getVersionLabels().getImplicitVersionLabel()));
        sb.append(String.format(template, Utils.ATTR_OBJ_CREATOR, "string", m_sObj.getCreatorName()));
        sb.append(String.format(template, Utils.ATTR_OBJ_CREATE_DATE, "string", m_sObj.getCreationDate().asString("yyyyMMddHHmmss")));
        sb.append(String.format(template, Utils.ATTR_OBJ_MODIFIER, "string", m_sObj.getModifier()));
        sb.append(String.format(template, Utils.ATTR_OBJ_MODIFY_DATE, "string", m_sObj.getModifyDate().asString("yyyyMMddHHmmss")));
        
        // get content specific attrs
        if (!DCTMBasics.isFolder(m_sObj)) {
            sb.append(String.format(template, Utils.ATTR_OBJ_CONTENT_TYPE, "string", m_contentType));
	        sb.append(String.format(template, Utils.ATTR_OBJ_CHRONICLE_ID, "string", m_sObj.getChronicleId().toString()));
	        sb.append(String.format(template, Utils.ATTR_OBJ_ANTECEDENT_ID, "string", m_sObj.getAntecedentId().toString()));
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
                    sb.append(String.format(template, attrObj.getName(), Utils.DATA_TYPES[attrObj.getDataType()], Utils.cleanXML(m_sObj.getString(attrObj.getName()))));
                }
            }
        }
        return sb.toString();
    }

    /*
     * Build XML to hold virtual doc children references.
     */
    private String buildVDAttrString() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<vd_children>\n");
        for (ExportObj eObj : m_VDChildren) {
            sb.append("<vd_child>" + eObj.getObjectId() + "</vd_child>\n");
        }
        sb.append("</vd_children>");
        return sb.toString();
    }

    /*
     * Export each virtual doc's children's content and metadata.  This
     * is sort of recursive if you think about it.
     */
    private void exportVirtualDocChildren() throws Exception {
        for (ExportObj eObj : m_VDChildren) {
            eObj.exportContent();
        }
    }

    /*
     * Gather the objects that are children of this virtual doc, and add them
     * to the m_VDChildren ArrayList as ExportObjs.
     */
    private void gatherVDChildren() throws Exception {
        String DQL = "select r_object_id from dm_sysobject in document id('" + getObjectId() + "') descend";
        //IDfCollection col = Utils.runQuery(DQL, getSession());
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

    // ========== Helper Methods ==========
    
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
}

/* ============================================================================
 * <SDG><
 * ============================================================================
 */