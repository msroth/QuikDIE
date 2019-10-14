package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Export Content Module
 * (c) 2013-2019 MS Roth
 * 
 * This application will export content and metadata from a Documentum
 * repository according to the query contained in the export.properties
 * file.  The content will be exported in its native format and named
 * according to its r_object_id.  A companion XML file will also be created
 * to house the content's metadata.  The XML file will be named 
 * according to the content's r_object_id also.  Finally, if any of the objects
 * exported are of custom types (i.e., do not start with 'dm_', an XML
 * file will be created containing definitions for the type's custom attributes.
 * 
 * As is implied by the name of this application, there is an envisioned import
 * application that will read the type definition, metadata, and content files
 * generated by this application and import them into Documentum.  That
 * application does not exist yet.
 * 
 * Tested on Documentum Content Server 6.5, 6.7, 7.0, 7.1.
 * 
 * --------------------------------
 * Sample export.properties file:
 *   export.query=select * from dm_sysobject where folder('/Temp',descend)
 *   export.user=dmadmin
 *   export.password=dmadmin
 *   export.repo=repo1
 *   export.path=c:/Temp/Export
 *   export.log=c:/Temp/Export/export.log
 * 
 * Notes:
 *   - the password will be encrypted and written back to the properties file 
 *     after the first run
 *   - even though the query in the sample will traverse a folder structure, all
 *     exported files will be written to a flat export destination as defined
 *     by the path property.
 * 
 * --------------------------------
 * Sample metadata XML file:
 * <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
 * <object r_object_id="0901e45380056525" content="true" virtdoc="true">
 *   <repo_path>/Temp</repo_path>
 *   <content_file>0901e45380056525.pdf</content_file>
 *   <properties>
 *     <property name="r_object_type" type="string">sr_document</property>
 *     <property name="object_name" type="string">Scott Test</property>
 *     <property name="title" type="string"></property>
 *     <property name="subject" type="string"></property>
 *     <property name="acl_domain" type="string">repo1</property>
 *     <property name="acl_name" type="string">dm_4501e45380000103</property>
 *     <property name="owner_name" type="string">dmadmin</property>
 *     <property name="r_version_label" type="string">1.1</property>
 *     <property name="r_creator_name" type="string">dmadmin</property>
 *     <property name="r_creation_date" type="string">20160407160742</property>
 *     <property name="r_modifier" type="string">dmadmin</property>
 *     <property name="r_modify_date" type="string">20160407160742</property>
 *     <property name="a_content_type" type="string">pdf</property>
 *     <property name="i_chronicle_id" type="string">0901e45380056516</property>
 *     <property name="a_antecedent_id" type="string">0901e45380056516</property>
 *     <property name="test_attr_2" type="string">test 2</property>
 *     <property name="test_attr_1" type="string">test 1</property>
 *   </properties>
 *   <permissions>
 *     <permission accessor_name="dm_world" accessor_permit="6" accessor_x_permit="0" />
 *     <permission accessor_name="dm_owner" accessor_permit="7" accessor_x_permit="0" />
 *     <permission accessor_name="docu" accessor_permit="5" accessor_x_permit="3" />
 *   </permissions>
 *   <vd_children>
 *     <vd_child>0901e4538004892d</vd_child>
 *   </vd_children>
 *   <renditions>
 *     <rendition format="msw12">0901e4538000dd17.rendition.docx</rendition>
 *     <rendition format="pdf">0901e4538000dd17.rendition.pdf</rendition>
 *   </renditions>
 * </object>
 *       
 * Notes:
 *   - the properties shown are the default properties plus two custom properties
 *     (test_attr_1 and test_attr_2)
 *   - this example is a virtual document, thus the virtdoc="true" XML attribute
 *     in the object element and the inclusion of the vd_children element.  
 *   - this example also includes a PDF and Word rendition of the root virtual 
 *     document   
 * 
 * --------------------------------         
 * Sample type definition file:
 * <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
 * <type name="sr_document" super_type="dm_document" custom_attrs="2">
 *   <attributes>
 *     <attribute name="test_attr_2" type="string" size="32" repeating="false" />
 *     <attribute name="test_attr_1" type="string" size="32" repeating="false" />
 *   </attributes>
 * </type>
 *  
 * Notes:
 *   - only custom attributes are included in type definitions.  All other
 *     attributes are assumed to be inherited from the super type.
 *             
 * --------------------------------
 * Sample ACL definition file:
 * <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
 * <acl name="d2_acl_public" domain="dmadmin" global="false" class="3" >
 *   <attributes>
 *     <attribute name="r_object_id" value="4501e45380000d03" />
 *     <attribute name="template_name" value="" />
 *     <attribute name="alias_set_name" value="" />
 *   </attributes>
 *   <permissions>
 *     <permission accessor_name="dm_world" accessor_permit="6" accessor_x_permit="458752" permit_type="0" is_group="false" />
 *     <permission accessor_name="dm_owner" accessor_permit="7" accessor_x_permit="458752" permit_type="0" is_group="false" />
 *     <permission accessor_name="admingroup" accessor_permit="7" accessor_x_permit="458752" permit_type="0" is_group="true" />
 *   </permissions>
 * </acl>
 *   
 *   
 *   
 * --------------------------------  
 * Versions:
 *   1.0 - 2013-05-09 - initial release.
 *   1.1 - 2013-05-09 - added code to omit certain Documentum objects from
 *                      being considered as custom types.  
 *   1.2 - 2013-07-17 - included i_chronicle_id so version tree can be 
 *                      reconstructed.  Changed version attr to r_version_label
 *                      and object_type to r_object_type.  Added content_file
 *                      to xml output.
 *   1.3 - 2014-04-09 - included r_creator_name, r_creation_date, r_modifier,
 *                      r_modify_date as base metadata (thanks Malcolm MacArthur)
 *                    - clean Strings before writing them to XML file (thanks 
 *                      Malcolm MacArthur)
 *                    - updated dmRecordSet to v1.2
 *   1.4 - 2015-03-25 - Recompiled using Java 7
 *                    - updated JDOM library to 2.0.6    
 *                    - checks for supported version of DFC    
 *                    - export a_antecedent_id so version tree can be reconstructed  
 *                    - updated batch file to use dctm.jar   
 *   1.5 - 2015-07-02 - added additional code to detect content "parked" on BOCS 
 *                      server
 *                    - added "dmi" extension to object types ignored  
 *                    - implemented DCTMBasics JAR        
 *   1.6 - 2019-09-03 - added "dmr" extension to object types ignored
 *                    - added renditions to exports (thanks Sindhu Pillai)
 *                    - general clean up
 *   1.7 - 2019-XX-XX - refactored main object names to be DmExport ad DmImport
 *   				  - added export of custom ACL definitions    
 *                    - added export of permissions in each object's metadata
 * 					  - added 'custom' tab to property element to help facilitate import
 * 
 * ============================================================================        
 */

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.dm_misc.collections.dmRecordSet;
import com.dm_misc.dctm.DCTMBasics;

import com.documentum.fc.client.*;
import com.documentum.fc.common.*;

import com.dm_misc.QuikDIE.Utils;

public class DmExport {

    private Set<String> m_ObjTypeSet = new HashSet<String>();
    
    // v.17
    private Set<String> m_CustomACLSet = new HashSet<String>();
    
    private static final String BANNER = "\n\n" + Utils.APP_BANNER + "\n" + Utils.COPYRIGHT + "\n\n"
            + "Export Content Module v" + Utils.EXPORT_VERSION + "\n"
            + "==================================================";

    /*
     * ignore passed arguments 
     */
    public static void main(String[] args) {

        try {
            System.out.println(BANNER);
            System.out.println(new Date().toString());
            DmExport dmexport = new DmExport(args);
            dmexport.run();
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * ignore passed arguments and do nothing on instantiation
     */
    private DmExport(String[] args) {
        // noop
    }

    /*
     * The run method contains the main process loop for the export.  After the
     * properties are read and verified, the query is executed to find the
     * objects to export.  An ExportObj is created for each r_object_id returned
     * by the query.  The ExportObj does the all the hard work of exporting.
     * Along the way, custom type definitions are collected for processing at the
     * end.
     */
    public void run() throws Exception {
  	
        System.out.println("\nInitializing...\n");
        IDfSession session = null;
        int objCount = 0;
        
        // check if DFC version is supported
        if (!DCTMBasics.checkDFCversion(6.5))
        	throw new Exception ("Only DFC v6.5 and higher is supported");

        // read properties file
        if (!Utils.loadConfig(this.getClass(), Utils.EXPORT_PROPERTY_FILE)) {
            throw new Exception("Could not load configuration");
        }

        // validate export path
        if (!Utils.validateFileSystemPath(Utils.getConfigProperty(Utils.EXPORT_KEY_PATH), true)) {
            throw new Exception("Problem with export path: " + Utils.getConfigProperty(Utils.EXPORT_KEY_PATH));
        } else {
            System.out.println("Export path: " + Utils.getConfigProperty(Utils.EXPORT_KEY_PATH));
        }

        // create log file
        if (!Utils.createLogFile(Utils.EXPORT_KEY_LOG)) {
            throw new Exception("Could not create log file: " + Utils.getConfigProperty(Utils.EXPORT_KEY_LOG));
        } else {
            System.out.println("Export log: " + Utils.getConfigProperty(Utils.EXPORT_KEY_LOG));
        }

        // check encrypted password
        Utils.checkPassword(Utils.OP_EXPORT);

        // login
        session = DCTMBasics.logon(Utils.getConfigProperty(Utils.EXPORT_KEY_DOCBASE), Utils.getConfigProperty(Utils.EXPORT_KEY_USER), Utils.getConfigProperty(Utils.EXPORT_KEY_PASSWORD));
        if (session == null) {
            throw new Exception("Could not establish session with " + Utils.getConfigProperty(Utils.EXPORT_KEY_DOCBASE));
        } else {
            System.out.println("Login successful:  " + Utils.getConfigProperty(Utils.EXPORT_KEY_USER) + "@" + Utils.getConfigProperty(Utils.EXPORT_KEY_DOCBASE));
        }

        // validate export query query
        if (!validateQuery(Utils.getConfigProperty(Utils.EXPORT_KEY_QUERY))) {
            throw new Exception("Query must start with: SELECT R_OBJECT_ID or SELECT *");
        }

        // write log file header, including contents of config file used for export
        Utils.writeLog(BANNER);
        Utils.writeLog(new Date().toString());
        Utils.writeLog("");
        Utils.writeLog(Utils.dumpProperties(false));
        Utils.writeLog("");

        // run export query and load into dmRecordSet object
        IDfCollection col = DCTMBasics.runSelectQuery(Utils.getConfigProperty(Utils.EXPORT_KEY_QUERY), session);
        dmRecordSet dmRS = new dmRecordSet(col);
        col.close();

        if (dmRS.isEmpty()) {
            throw new Exception("Query returned 0 objects to process");
        } else {
            System.out.println("Found " + dmRS.getRowCount() + " primary objects to export...\n");
            Utils.writeLog("Found " + dmRS.getRowCount() + " primary objects to export...\n");
        }

        // process result set and export found objects
        while (dmRS.hasNext()) {

            // get obj from collection
            IDfTypedObject tObj = dmRS.getNextRow();

            // create export obj
            ExportObj expObj = new ExportObj(tObj.getString("r_object_id"), Utils.getConfigProperty(Utils.EXPORT_KEY_PATH), session);

            // export the object and keep count of objects exported -- this is where the export happens
            objCount += expObj.exportContent();
            
            // check for custom types
            if (!Utils.omitTypes(expObj.getTypeName())) {
                if (!m_ObjTypeSet.contains(expObj.getTypeName())) {
                    m_ObjTypeSet.add(expObj.getTypeName());
                }
            }
            
            // v1.7
            // check for custom ACLs
            if (! expObj.getACLName().startsWith("dm_")) {
            	if (! m_CustomACLSet.contains(expObj.getACLDomain() + ":" + expObj.getACLName())) {
            		m_CustomACLSet.add(expObj.getACLDomain() + ":" + expObj.getACLName());
            	}
            }
        }

        // export custom type defs and count as exported objects
        objCount += exportTypeDefinitions(session);

        // v1.7
        // export custom ACL defs and count as exported objects
        objCount += exportACLDefinitions(session);

        // close session
        if (session != null) {
            session.getSessionManager().release(session);
        }

        // close down log files
        System.out.println("\nExported " + objCount + " objects.");
        System.out.println(new Date().toString());
        Utils.writeLog("\nExported " + objCount + " objects.");
        Utils.writeLog(new Date().toString());
        Utils.closeLogFile();
        System.out.println("Done.");
    }

    
    /*
     * Ensure the query string will return the r_object_id at a minimum.
     * We need the r_object_id to instantiate ExportObjs.
     */
    private boolean validateQuery(String query) throws Exception {
        String temp = "";

        int sIndex = query.toLowerCase().indexOf("select");
        int fIndex = query.toLowerCase().indexOf("from");

        // if not select or from found it's an invalid query
        if (sIndex > -1 && fIndex > -1) {
            temp = query.toLowerCase().substring(sIndex, fIndex);
        } else {
            return false;
        }

        // query must return r_object_id in result set
        if ((temp.indexOf("r_object_id")) < 0 && (temp.indexOf("*") < 0)) {
            return false;
        } else {
            return true;
        }
    }

    
    /*
     * Create XML files that contain the details of custom object types exported.
     * The list of custom object types found during the content export is in the
     * m_ObjTypeSet ArrayList.  XML files are created for each type that describe
     * their custom attributes.
     */
    private int exportTypeDefinitions(IDfSession session) throws Exception {
    	int count = 0;
        String path = Utils.getConfigProperty(Utils.EXPORT_KEY_PATH);
        String template = "<attribute name=\"%s\" type=\"%s\" size=\"%d\" repeating=\"%s\" />";

        for (String type : m_ObjTypeSet) {
        	count += 1; // count the number of custom type XML files created
            IDfType typeObj = session.getType(type);
            String filename = type + Utils.FILE_EXT_TYPEDEF;

            System.out.println("Type definition: " + type + " ==> " + filename);
            Utils.writeLog("Type definition: " + type + " ==> " + filename);

            // open output file
            File file = new File(path + "/" + filename);
            PrintWriter xmlFile = new PrintWriter(file);

            // get custom attrs
            int attrcnt = typeObj.getInt("attr_count") - typeObj.getInt("start_pos");

            try {
                // write xml file header
                xmlFile.println(Utils.XML_HEADER);
                xmlFile.print("<type name=\"" + type + "\" super_type=\"" + typeObj.getSuperName() + "\" custom_attrs=\"" + attrcnt + "\"");
                xmlFile.println(">");
                xmlFile.println("<attributes>");

                // loop over custom attrs
                if (attrcnt > 0) {
                    for (int i = 0; i < attrcnt; i++) {
                        int offset = typeObj.getInt("start_pos") + i;
                        IDfAttr attrObj = typeObj.getTypeAttr(offset);

                        // write custom attrs to xml file
                        xmlFile.println(String.format(template, attrObj.getName(), Utils.DATA_TYPES[attrObj.getDataType()], 
                        		attrObj.getLength(), Boolean.toString(attrObj.isRepeating()).toString()));
                    }
                }

                xmlFile.println("</attributes>");
                xmlFile.println("</type>");
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            } finally {
                xmlFile.close();
            }
        }
        return count;
    }
    
	/*
	 * v1.7
	 * Create XML files that contain the details of custom ACLs (those not 
	 * starting with "dm_". The list of custom ACLs found during the content 
	 * export is in the m_CustomACLSet ArrayList.  XML files are created for 
	 * each ACL that describe their custom permissions.
	 */
    private int exportACLDefinitions(IDfSession session) throws Exception {
    	int count = 0;
    	String path = Utils.getConfigProperty(Utils.EXPORT_KEY_PATH);
        String attr_template = "<property name=\"%s\" value=\"%s\" />";
        String perm_template = "<permission accessor_name=\"%s\" accessor_permit=\"%s\" accessor_x_permit=\"%s\" accessor_x_permit_value=\"%s\" permit_type=\"%s\" is_group=\"%s\" />";
        

        for (String acl : m_CustomACLSet) {
        	count += 1; // count the number of ACL files created
        	
        	// split ACL domain:name
        	String acl_domain = acl.split(":")[0];
        	String acl_name = acl.split(":")[1];
        	
            String filename = acl_domain + "--" + acl_name + Utils.FILE_EXT_ACLDEF;
            System.out.println("ACL definition: " + acl_domain + ":" + acl_name + " ==> " + filename);
            Utils.writeLog("ACL definition: " + acl_domain + ":" + acl_name + " ==> " + filename);

            // open output file
            File file = new File(path + "/" + filename);
            PrintWriter xmlFile = new PrintWriter(file);

            // get acl obj
        	IDfACL aclObj = session.getACL(acl_domain, acl_name);
        	
        	// get template name if id <> 0
        	String template_name = "";
        	String template_id = aclObj.getString(IDfACL.TEMPLATE_ID);
        	if (! template_id.startsWith("0000")) {
        		template_name = ((IDfSysObject) session.getObject(new DfId(template_id))).getObjectName();
        	}	
        		
        	// get alias set if id <> 0
        	String alias_set_name = "";
        	String alias_set_id = aclObj.getString(IDfACL.ALIAS_SET_ID);
        	if (! alias_set_id.startsWith("0000")) {
        		alias_set_name = ((IDfSysObject) session.getObject(new DfId(alias_set_id))).getObjectName();
        	}

            try {
                // write xml file header
                xmlFile.println(Utils.XML_HEADER);
                xmlFile.println("<acl r_object_id=\"" + aclObj.getObjectId().toString() + "\" name=\"" + aclObj.getObjectName() + "\" domain=\"" + aclObj.getDomain() + "\" >");
                
                // write general attrs
                xmlFile.println("<properties>");
                xmlFile.println(String.format(attr_template, "global", Boolean.toString(aclObj.isGloballyManaged())));
                xmlFile.println(String.format(attr_template, "class", aclObj.getACLClass()));
                xmlFile.println(String.format(attr_template, "template_name", template_name));
                xmlFile.println(String.format(attr_template, "alias_set_name", alias_set_name));
                xmlFile.println(String.format(attr_template, "description", aclObj.getDescription()));
                xmlFile.println("</properties>");
                
                // write permission sets
                xmlFile.println("<permissions>");
                for (int i=0; i < aclObj.getAccessorCount(); i++) {
                	xmlFile.println(String.format(perm_template, aclObj.getAccessorName(i), aclObj.getAccessorPermit(i), aclObj.getAccessorXPermitNames(i), 
                			aclObj.getAccessorXPermit(i), aclObj.getAccessorPermitType(i), Boolean.toString(aclObj.isGroup(i))));
                }
                xmlFile.println("</permissions>");
                
                xmlFile.println("</acl>");
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            } finally {
                xmlFile.close();
            }
        }
    	
    	return count;
    }
}

/* ============================================================================
 * <SDG><
 * ============================================================================
 */
