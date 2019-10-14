package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Utilities Module - ExportObj
 * (c) 2013-2019 MS Roth
 * 
 * ============================================================================
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;

import com.dm_misc.dctm.DCTMBasics;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.tools.RegistryPasswordUtils;

public class Utils {

    private static Properties m_configProperties = new Properties();
    private static PrintWriter m_log = null;
	private static HashMap<String,String> m_importedObjectsMap= new HashMap<String,String>();
    
    // general constants
    public static final String PASSWORD_PREFIX = "DM_ENCR_TEXT=";
    public static final String[] DATA_TYPES = {"boolean", "integer", "string", "id", "time", "double", "undefined"};
    public static final String OP_EXPORT = "EXPORT";
    public static final String OP_IMPORT = "IMPORT";
    public static final String APP_BANNER = "QuikDIE - A Quick Documentum Import/Export Utility ";
    public static final String COPYRIGHT = "(c) 2013-2019 MS Roth";
    public static final String[] OMIT_OBJ_PREFIXES = {"dm", "d2", "c2", "c6", "x3", "o2", "dmi", "dmr", "dmc"};
    public static final String[] SKIP_IMPORT_ATTRS = {"r_object_id", "r_version_label", "r_creation_date", "r_modify_date", "i_chronicle_id", "a_antecedent_id"};
    
    // export app properties
    public static final String EXPORT_PROPERTY_FILE = "export.properties";
    public static final String EXPORT_VERSION = "1.7";
    
    // import app properties
    public static final String IMPORT_PROPERTY_FILE = "import.properties";
    public static final String IMPORT_TYPE_FOLDER = "folder";
    public static final String IMPORT_TYPE_TYPE = "type";
    public static final String IMPORT_TYPE_CONTENT = "content";
    public static final String IMPORT_VERSION = "0.1";
    
    // dctm attr constants
    public static final String OBJ_ATTR_ID = "r_object_id";
    public static final String OBJ_ATTR_NAME = "object_name";
    public static final String OBJ_ATTR_TYPE = "r_object_type";
    public static final String OBJ_ATTR_VERSION = "r_version_label";
    public static final String OBJ_ATTR_HAS_CONTENT = "content";
    public static final String OBJ_ATTR_TITLE = "title";
    public static final String OBJ_ATTR_SUBJECT = "subject";
    public static final String OBJ_ATTR_ACL_DOMAIN = "acl_domain";
    public static final String OBJ_ATTR_ACL_NAME = "acl_name";
    public static final String OBJ_ATTR_OWNER = "owner_name";
    public static final String OBJ_ATTR_CONTENT_TYPE = "a_content_type";
    public static final String OBJ_ATTR_SUPER_TYPE = "super_type";
    public static final String OBJ_ATTR_CHRONICLE_ID = "i_chronicle_id";
    public static final String OBJ_ATTR_ANTECEDENT_ID = "a_antecedent_id";
    public static final String OBJ_ATTR_CREATOR = "r_creator_name";
    public static final String OBJ_ATTR_CREATE_DATE = "r_creation_date";
    public static final String OBJ_ATTR_MODIFIER = "r_modifier";
    public static final String OBJ_ATTR_MODIFY_DATE = "r_modify_date";
    public static final String OBJ_ATTR_VIRTUAL_DOC = "virtdoc";
    
    // file extensions
    public static final String FILE_EXT_METADATA = ".metadata.xml";
    public static final String FILE_EXT_FOLDER = ".folder.xml";
    public static final String FILE_EXT_TYPEDEF = ".type.xml";
    public static final String FILE_EXT_ACLDEF = ".acl.xml";
    
    // export property config keys
    public static final String EXPORT_KEY_QUERY = "export.query";
    public static final String EXPORT_KEY_USER = "export.user";
    public static final String EXPORT_KEY_PASSWORD = "export.password";
    public static final String EXPORT_KEY_DOCBASE = "export.repo";
    public static final String EXPORT_KEY_PATH = "export.path";
    public static final String EXPORT_KEY_LOG = "export.log";
    
    // import property config keys
    public static final String IMPORT_KEY_USER = "import.user";
    public static final String IMPORT_KEY_PASSWORD = "import.password";
    public static final String IMPORT_KEY_DOCBASE = "import.repo";
    public static final String IMPORT_KEY_REPO_PATH = "import.target_path";
    public static final String IMPORT_KEY_USE_EXPORT_REPO_ATTRS = "import.use_export_repo_attrs";
    public static final String IMPORT_KEY_FILES_PATH = "import.file_source";
    public static final String IMPORT_KEY_LOG = "import.log";

    // xml constants
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>";
    public static final String XML_ATTR_NAME = "name";
    public static final String XML_ATTR_TYPE = "type";
    public static final String XML_ELEMENT_REPO_PATH = "repo_path";
    public static final String XML_ELEMENT_CONTENT_FILE = "content_file";
    public static final String XML_ELEMENT_PROPERTIES = "properties";
    public static final String XML_ELEMENT_PROPERTY = "property";
    public static final String XML_ELEMENT_PERMISSIONS = "permissions";
    public static final String XML_ELEMENT_PERMISSION = "permission";
    public static final String XML_ELEMENT_VD_CHILDREN = "vd_children";
    public static final String XML_ELEMENT_VD_CHILD = "vd_child";
    public static final String XML_ELEMENT_RENDITIONS = "renditions";
    public static final String XML_ELEMENT_RENDITION = "rendition";
    public static final String XML_ATTR_ACCESSOR = "accessor_name";
    public static final String XML_ATTR_ACCESSOR_PERMIT = "accessor_permit";
    public static final String XML_ATTR_ACCESSOR_XPERMIT = "accessor_x_permit";
    public static final String XML_ATTR_FORMAT = "format";
    public static final String XML_ATTR_CUSTOM = "custom";
    public static final String XML_TYPE_ELEMENT = "type";
    public static final String XML_ATTR_SUPER_TYPE = "super_type";
    public static final String XML_ELEMENT_ATTRIBUTES = "attributes";
    public static final String XML_ELEMENT_ATTRIBUTE = "attribute";
    public static final String XML_ATTR_SIZE = "size";
    public static final String XML_ATTR_REPEATING = "repeating";    
    public static final String XML_ATTR_DOMAIN = "domain";
    public static final String XML_ATTR_GLOBAL = "global";
    public static final String XML_ATTR_CLASS = "class";
    
    // xml templates (write strings)
    public static final String XML_TEMPLATE_PROPERTIES = "<" + XML_ELEMENT_PROPERTIES + ">\n%s</" + XML_ELEMENT_PROPERTIES + ">";
    public static final String XML_TEMPLATE_REPO_PATH = "<" + XML_ELEMENT_REPO_PATH + ">%s</" + XML_ELEMENT_REPO_PATH + ">";
    public static final String XML_TEMPLATE_CONTENT_FILE = "<" + XML_ELEMENT_CONTENT_FILE + ">%s</" + XML_ELEMENT_CONTENT_FILE + ">";
    public static final String XML_TEMPLATE_PERMISSIONS = "<" + XML_ELEMENT_PERMISSIONS + ">\n%s</" + XML_ELEMENT_PERMISSIONS + ">";
    public static final String XML_TEMPLATE_VD_CHILDREN = "<" + XML_ELEMENT_VD_CHILDREN + ">\n%s</" + XML_ELEMENT_VD_CHILDREN + ">";
    public static final String XML_TEMPLATE_RENDITIONS = "<" + XML_ELEMENT_RENDITIONS + ">\n%s</" + XML_ELEMENT_RENDITIONS + ">";
    public static final String XML_TEMPLATE_OBJECT_OPEN = "<object " + OBJ_ATTR_ID + "=\"%s\" " + OBJ_ATTR_TYPE + "=\"%s\" content=\"%s\" virtdoc=\"%s\">";

    
    public static boolean loadConfig(Class thisClass, String propFilePath) throws Exception {
        // load properties from root of classpath (i.e., in the bin folder, level with com)
        boolean result = false;
        InputStream is = null;

        try {
            // try loading from current dir
            File f = new File(propFilePath);
            is = new FileInputStream(f);

            // load properties
			m_configProperties.clear();
			m_configProperties.load(is);
			result = true;
			
        } catch (Exception e) {
            throw new Exception("Cannot find " + propFilePath + " file.");
        }

        return result;
    }

    
    // get any property
    public static String getConfigProperty(String key) {

        if (m_configProperties.containsKey(key)) {
            return m_configProperties.getProperty(key);
        } else {
            return null;
        }
    }

    
    // set any property
    public static void setConfigProperty(String key, String value) {
        m_configProperties.setProperty(key, value);
    }

    
    /*
     * Read password from properties.  If it is not encrypted, encrypt it 
     * and save back to property file.  If it is encrypted, decrypt it so 
     * it can be used for login.
     */
    public static void checkPassword(String op) {
        String propertyFile = "";
        String password = "";
        String key = "";

        try {
            if (op.equalsIgnoreCase(OP_EXPORT)) {
                password = getConfigProperty(Utils.EXPORT_KEY_PASSWORD);
                propertyFile = EXPORT_PROPERTY_FILE;
                key = EXPORT_KEY_PASSWORD;
            } else {
                password = getConfigProperty(Utils.IMPORT_KEY_PASSWORD);
                propertyFile = IMPORT_PROPERTY_FILE;
                key = IMPORT_KEY_PASSWORD;
            }

            // if password not encrypted in property file, encrypt it and save
            // it back to the property file.  Leave it un-encrypted in properties
            // hash
            if (!password.startsWith(PASSWORD_PREFIX)) {

                //write to property file
                password = writePasswordToPropertyFile(propertyFile, key);
            }

            // chop off DM_ENCR_TEXT=
            password = password.substring(PASSWORD_PREFIX.length());
            password = password.replace("\\", "");
            String newPassword = RegistryPasswordUtils.decrypt(password);
            setConfigProperty(key, newPassword);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    
    // handle password encryption and writing it back to properties file
    private static String writePasswordToPropertyFile(String propertyFile, String key) {
        String newPassword = "";
        
        try {

            // encrypt password
            newPassword = PASSWORD_PREFIX + RegistryPasswordUtils.encrypt(getConfigProperty(key));
            newPassword = newPassword.replace("\\", "");

            // this assumes the property file is in the current dir
            File file = new File(propertyFile);
            setConfigProperty(key, newPassword);
            OutputStream out = new FileOutputStream(file);
            m_configProperties.store(out, "");
            
        } catch (Exception e) {
            System.out.println("\tWARNING:  unable to write encrypted password to property file " + e.getMessage());
        }

        return newPassword;
    }


    // ensure export file system path exists or create it
    public static boolean validateFileSystemPath(String path, boolean mkdir) throws Exception {
        boolean valid = false;

        File dir = new File(path);
        if ((dir != null) && (dir.exists()) && (dir.isDirectory())) {
            valid = true;
        } else if ((dir != null) && (mkdir == true)) {
            dir.mkdirs();
            if ((dir.exists()) && (dir.isDirectory())) {
                valid = true;
            }
        }
        return valid;
    }

    
    // get the folder path of an object in the repo
    public static String getObjectPath(IDfSysObject sObj, IDfSession session) throws Exception {
        String path = "";

        IDfId folderId = sObj.getFolderId(0);
        IDfFolder folder = (IDfFolder) session.getObject(folderId);
        path = folder.getFolderPath(0);
        return path;
    }


    // ensure the folder path in the repo exists or create it
    public static boolean validateRepoPath(String repoPath, IDfSession session, boolean mkdir) throws Exception {
        boolean valid = false;

        IDfFolder folder = (IDfFolder) session.getObjectByQualification("dm_folder where any r_folder_path = '" + repoPath + "'");
        if (folder != null) {
            valid = true;
        }

        if (!valid && mkdir) {
            folder = DCTMBasics.createDocbasePath(session, repoPath);
            if (folder != null) {
                valid = true;
            }
        }
        return valid;
    }


    // TBD - move files on file system
    public static void moveFilesToDir(File[] files, String path) {
    	
        // TODO
    	
    }

    
    // create the log file
    public static boolean createLogFile(String key) {
        boolean success = true;
        try {
            //validate path to log file
            String logFile = getConfigProperty(key);
            if (logFile == null || logFile.length() == 0) {
                return false;
            }

            String logPath = logFile.substring(0, logFile.lastIndexOf("/"));
            validateFileSystemPath(logPath, true);

            // create serialized name for log file
            String filename = logFile.substring(logFile.lastIndexOf("/") + 1);
            String name = filename.substring(0, filename.lastIndexOf("."));
            String ext = filename.substring(filename.lastIndexOf(".") + 1);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String now = formatter.format(new Date());
            filename = logPath + "/" + name + "-" + now + "." + ext;

            // open file
            File log = new File(filename);
            m_log = new PrintWriter(log);
            setConfigProperty(key, filename);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            success = false;
        }
        return success;
    }

    
    // write message to log file
    public static void writeLog(String msg) {
        if (m_log != null) {
            m_log.println(msg);
            m_log.flush();
        }
    }

    
    // close log file
    public static void closeLogFile() {
        if (m_log != null) {
            m_log.close();
        }
    }


    // run a query that returns a boolean result
    // a light wrapper around DCTMBasics.runDQLQueryReturnSingleValue()
    public static boolean runDQLQueryWithBooleanResult(String dql, IDfSession session) throws Exception {
        
    	if (dql == null || dql.length() == 0) {
            return false;
        }

    	if (session == null) {
    		return false;
    	}
    	
        String rv = null;
        rv = DCTMBasics.runDQLQueryReturnSingleValue(dql, session);

        if (rv != null && !rv.isEmpty())
        	return true;
        else
        	return false;
    }
    
    
    // determine if a type exists in the repo
    public static boolean checkTypeExists(String type, IDfSession session) throws Exception {

        if (type == null || type.length() == 0) {
            return false;
        }

        String dql = "select r_object_id from dm_type where name = '" + type + "'";
        String rv = null;
        rv = DCTMBasics.runDQLQueryReturnSingleValue(dql, session);

        if (rv != null && !rv.isEmpty())
        	return true;
        else
        	return false;
    }

    
    // determine if a acl exists in the repo
    public static boolean checkACLExists(String domain, String acl, IDfSession session) throws Exception {

        if (domain == null || domain.length() == 0) {
            return false;
        }
        
        if (acl == null || acl.length() == 0) {
            return false;
        }

        String dql = "select r_object_id from dm_acl where object_name = '" + acl + "' and owner_name = '" + domain + "'";
        String rv = null;
        rv = DCTMBasics.runDQLQueryReturnSingleValue(dql, session);

        if (rv != null && !rv.isEmpty())
        	return true;
        else
        	return false;
    }
    
    
    // return the contents of the properties object formatted as key=value pairs
    public static String dumpProperties(boolean showPW) {
        StringBuilder out = new StringBuilder();

//        Set s = m_configProperties.keySet();
//        Iterator i = s.iterator();
//        while (i.hasNext()) {
        for (String k : m_configProperties.stringPropertyNames()) {
//            String k = (String) i.next();
            String v = getConfigProperty(k);
            if ((k.equalsIgnoreCase(EXPORT_KEY_PASSWORD) || (k.equalsIgnoreCase(IMPORT_KEY_PASSWORD))
                    && !showPW)) {
                v = "********";
            }
            out.append(k + "=" + v + "\n");
        }
        return out.toString();
    }

    // if a type on the OMIT list, ignore it
    public static boolean omitTypes(String obj_type) {

        for (String prefix : Utils.OMIT_OBJ_PREFIXES) {
            if (obj_type.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    // if attr on the SKIP list, ignore it
    public static boolean skipAttrs(String attr) {

        for (String skip : Utils.SKIP_IMPORT_ATTRS) {
            if (attr.equalsIgnoreCase(skip)) {
                return true;
            }
        }
        return false;
    }
    
    
    // clean the XML strings
    public static String cleanXML(String stringToclean) {
    	return StringEscapeUtils.escapeXml(stringToclean);
    }
    
    
    // look up the DOS extension in the repo for a format name
    public static String lookupDosExt(String format, IDfSession session) throws Exception {
    	String ext = DCTMBasics.runDQLQueryReturnSingleValue("select dos_extension from dm_format where name = '" + format + "'", session);
    	return ext;
    }

    
    // get list of XML files based upon file extension
	public static ArrayList<String> getXMLFilesToImport(String path, String filemask) {
		ArrayList<String> files = new ArrayList<String>();
		File dir = new File(path);
		File[] list = dir.listFiles();

		for (File f : list) {
			if (!f.isDirectory()) {
				if (f.getName().toLowerCase().contains(filemask.toLowerCase())) {
					files.add(f.getAbsolutePath());
					//System.out.println("file " + f.getName() + " added");
				}	
			}	
		}

		return files;
	}
	
	
	public static String getNewObjIdFromImportedObjMap(String oldId) {
		if (m_importedObjectsMap.containsKey(oldId)) {
			return m_importedObjectsMap.get(oldId);
		} else {
			return "";
		}
	}
	
	
	public static void addToImportedObjMap(String oldId, String newId) {
		if (!m_importedObjectsMap.containsKey(oldId)) {
			m_importedObjectsMap.put(oldId, newId);
		}
	}
	
	
	public static HashMap<String, String> getImportedObjectsMap() {
		return m_importedObjectsMap;
	}
	
	
	public static void dumpImportedObjsMap() {
		System.out.println("Imported objects r_object_id map");
		System.out.println("Old Id                New Id");
		System.out.println("----------------      ----------------");
		for (String k : m_importedObjectsMap.keySet()) {
			System.out.println(k + " ==> " + m_importedObjectsMap.get(k));
		}
	}
}

//<SDG><