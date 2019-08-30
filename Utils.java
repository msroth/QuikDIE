package com.dm_misc.QuikDIE;
/* ============================================================================
 * QuikDIE - Quik Documentum Import/Export
 * Export Content Module - ExportObj
 * (c) 2013-2015 MS Roth
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

import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import com.dm_misc.dctm.DCTMBasics;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.tools.RegistryPasswordUtils;

public class Utils {

    private static Properties m_properties = new Properties();
    private static PrintWriter m_log = null;
    // general constants
    public static final String PASSWORD_PREFIX = "DM_ENCR_TEXT=";
    public static final String[] DATA_TYPES = {"boolean", "integer", "string", "id", "time", "double", "undefined"};
    public static final String OP_EXPORT = "EXPORT";
    public static final String OP_IMPORT = "IMPORT";
    public static final String APP_BANNER = "QuikDIE - A Quick Documentum Import/Export Utility ";
    public static final String COPYRIGHT = "(c) 2013-2015 MS Roth";
    public static final String[] OMIT_OBJ_PREFIXES = {"dm", "d2", "c2", "c6", "x3", "o2", "dmi"};
    // export app properties
    public static final String EXPORT_PROPERTY_FILE = "export.properties";
    public static final String EXPORT_VERSION = "1.5";
    // import app properties
    public static final String IMPORT_PROPERTY_FILE = "import.properties";
    public static final String IMPORT_TYPE_FOLDER = "folder";
    public static final String IMPORT_TYPE_TYPE = "type";
    public static final String IMPORT_TYPE_CONTENT = "content";
    public static final String IMPORT_VERSION = "1.0";
    // attr constants
    public static final String ATTR_OBJ_ID = "r_object_id";
    public static final String ATTR_OBJ_NAME = "object_name";
    public static final String ATTR_OBJ_TYPE = "r_object_type";
    public static final String ATTR_OBJ_VERSION = "r_version_label";
    public static final String ATTR_OBJ_HAS_CONTENT = "content";
    public static final String ATTR_OBJ_TITLE = "title";
    public static final String ATTR_OBJ_SUBJECT = "subject";
    public static final String ATTR_OBJ_ACL_DOMAIN = "acl_domain";
    public static final String ATTR_OBJ_ACL_NAME = "acl_name";
    public static final String ATTR_OBJ_OWNER = "owner_name";
    public static final String ATTR_OBJ_CONTENT_TYPE = "a_content_type";
    public static final String ATTR_OBJ_SUPER_TYPE = "super_type";
    public static final String ATTR_OBJ_CHRONICLE_ID = "i_chronicle_id";
    public static final String ATTR_OBJ_ANTECEDENT_ID = "a_antecedent_id";
    public static final String ATTR_OBJ_CREATOR = "r_creator_name";
    public static final String ATTR_OBJ_CREATE_DATE = "r_creation_date";
    public static final String ATTR_OBJ_MODIFIER = "r_modifier";
    public static final String ATTR_OBJ_MODIFY_DATE = "r_modify_date";
    // file extensions
    public static final String METADATA_FILE_EXT = ".metadata.xml";
    public static final String FOLDER_FILE_EXT = ".folder.xml";
    public static final String TYPEDEF_FILE_EXT = ".type.xml";
    // export property keys
    public static final String EXPORT_QUERY_KEY = "export.query";
    public static final String EXPORT_USER_KEY = "export.user";
    public static final String EXPORT_PASSWORD_KEY = "export.password";
    public static final String EXPORT_DOCBASE_KEY = "export.repo";
    public static final String EXPORT_PATH_KEY = "export.path";
    public static final String EXPORT_LOG_KEY = "export.log";
    // import property keys
    public static final String IMPORT_USER_KEY = "import.user";
    public static final String IMPORT_PASSWORD_KEY = "import.password";
    public static final String IMPORT_DOCBASE_KEY = "import.repo";
    public static final String IMPORT_REPO_PATH_KEY = "import.target_path";
    public static final String IMPORT_USE_EXPORT_REPO_ATTRS_KEY = "import.use_export_repo_attrs";
    public static final String IMPORT_FILES_PATH_KEY = "import.file_source";
    public static final String IMPORT_LOG_KEY = "import.log";

    public static boolean loadConfig(Class thisClass, String propFilePath) throws Exception {
        // load properties from root of classpath (i.e., in the bin folder, level with com)
        boolean result = false;
        InputStream is = null;

        try {
            // try loading from current dir
            File f = new File(propFilePath);
            //System.out.println("load config path=" + f.getAbsolutePath());
            is = new FileInputStream(f);

            if (is != null) {
                // load properties
                m_properties.clear();
                m_properties.load(is);
                result = true;
            } else {
                throw new Exception("Cannot read " + propFilePath + " file; checked here: " + f.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new Exception("Cannot find " + propFilePath + " file.");
        }

        return result;
    }

    public static String getProperty(String key) {

        if (m_properties.containsKey(key)) {
            return m_properties.getProperty(key);
        } else {
            return null;
        }
    }

    public static void setProperty(String key, String value) {
        m_properties.setProperty(key, value);
    }

//    public static IDfSession login(String docbase, String user, String password, IDfSessionManager sessionMgr) throws Exception {
//        IDfSession session = null;
//        IDfLoginInfo li = new DfLoginInfo();
//
//        li.setUser(user);
//        li.setPassword(password);
//        sessionMgr.setIdentity(docbase, li);
//        session = sessionMgr.newSession(docbase);
//        return session;
//    }

    public static void checkPassword(String op) {
        // if password not encrypted, encrypt it and save back to property file
        String propertyFile = "";
        String password = "";
        String key = "";

        try {
            if (op.equalsIgnoreCase(OP_EXPORT)) {
                password = getProperty(Utils.EXPORT_PASSWORD_KEY);
                propertyFile = EXPORT_PROPERTY_FILE;
                key = EXPORT_PASSWORD_KEY;
            } else {
                password = getProperty(Utils.IMPORT_PASSWORD_KEY);
                propertyFile = IMPORT_PROPERTY_FILE;
                key = IMPORT_PASSWORD_KEY;
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
            setProperty(key, newPassword);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String writePasswordToPropertyFile(String propertyFile, String key) {
        String newPassword = "";
        
        try {

            // encrypt password
            newPassword = PASSWORD_PREFIX + RegistryPasswordUtils.encrypt(getProperty(key));
            newPassword = newPassword.replace("\\", "");

            // this assumes the property file is in the current dir
            File file = new File(propertyFile);
            setProperty(key, newPassword);
            OutputStream out = new FileOutputStream(file);
            m_properties.store(out, "");
            
        } catch (Exception e) {
            System.out.println("\tWARNING:  unable to write encrypted password to property file " + e.getMessage());
        }

        return newPassword;
    }

//    public static IDfCollection runQuery(String dql, IDfSession session) throws Exception {
//        IDfCollection col = null;
//        IDfQuery query = new DfQuery();
//
//        query.setDQL(dql);
//        col = query.execute(session, DfQuery.DF_READ_QUERY);
//        return col;
//    }

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

    public static String getObjectPath(IDfSysObject sObj, IDfSession session) throws Exception {
        String path = "";

        IDfId folderId = sObj.getFolderId(0);
        IDfFolder folder = (IDfFolder) session.getObject(folderId);
        path = folder.getFolderPath(0);
        return path;
    }

//    public static boolean isFolder(IDfSysObject sObj) throws Exception {
//        if (sObj.getObjectId().toString().startsWith("0b")) {
//            return true;
//        } else {
//            return false;
//        }
//    }

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

//    private static IDfFolder dmCreateStoragePath(IDfSession session, String path) throws Exception {
//        IDfFolder folder = null;
//
//        // first see if the folder already exists
//        folder = (IDfFolder) session.getObjectByQualification("dm_folder where any r_folder_path='" + path + "'");
//
//        // if not build it
//        if (null == folder) {
//            // split path into separate folders
//            String[] dirs = path.split("/");
//
//            // loop through path folders and build
//            String dm_path = "";
//            for (int i = 0; i < dirs.length; i++) {
//
//                if (dirs[i].length() > 0) {
//
//                    // build up path
//                    dm_path = dm_path + "/" + dirs[i];
//
//                    // see if this path exists
//                    IDfFolder testFolder = (IDfFolder) session.getObjectByQualification("dm_folder where any r_folder_path='" + dm_path + "'");
//                    if (null == testFolder) {
//
//                        // check if a cabinet need to be made
//                        if (dm_path.equalsIgnoreCase("/" + dirs[i])) {
//                            IDfFolder cab = (IDfFolder) session.newObject("dm_cabinet");
//                            cab.setObjectName(dirs[i]);
//                            cab.save();
//                            // else make a folder 
//                        } else {
//                            folder = (IDfFolder) session.newObject("dm_folder");
//                            folder.setObjectName(dirs[i]);
//
//                            // link it to parent
//                            String parent_path = "";
//                            for (int j = 0; j < i; j++) {
//                                if (dirs[j].length() > 0) {
//                                    parent_path = parent_path + "/" + dirs[j];
//                                }
//                            }
//                            folder.link(parent_path);
//                            folder.save();
//                        }
//                    }
//                }
//            }
//        }
//        return folder;
//    }

    public static void moveFilesToDir(File[] files, String path) {
        // TODO
//		// see if folder for filename exists.
//        int last = destfile.lastIndexOf('/');
//
//        if (last < 0) {
//            DrxWriteError("CopyFile", "Destination filepath " + destfile + " doesn't contain /");
//            throw new java.io.FileNotFoundException(destfile);
//        }
//        String parent = destfile.substring(0, last);
//        if (parent.length() > 0) {
//            File f = new File(parent);
//
//            if (!f.isDirectory()) {
//                if (!f.mkdirs()) {
//                    DrxWriteError("CopyFile", "Folder " + parent + " doesn't exist, cannot create");
//                    // let FileOutputStream throw the exception
//                }
//            }
//        }
//
//        // Create channel on the source
//        FileChannel srcChannel = new FileInputStream(sourcefile).getChannel();
//
//        // Create channel on the destination
//        FileChannel dstChannel = new FileOutputStream(destfile).getChannel();
//
//        // Copy file contents from source to destination
//        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
//
//        // Close the channels
//        srcChannel.close();
//        dstChannel.close();
//
//        return destfile;
    }

    public static boolean createLogFile(String key) {
        boolean success = true;
        try {
            //validate path to log file
            String logFile = getProperty(key);
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
            setProperty(key, filename);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            success = false;
        }
        return success;
    }

    public static void writeLog(String msg) {
        if (m_log != null) {
            m_log.println(msg);
            m_log.flush();
        }
    }

    public static void closeLogFile() {
        if (m_log != null) {
            m_log.close();
        }
    }

    public static boolean checkTypeExists(String type, IDfSession session) throws Exception {
        boolean result = false;

        if (type == null || type.length() == 0) {
            return false;
        }

        String dql = "select r_object_id from dm_type where name = '" + type + "'";
        String rv = null;
        rv = DCTMBasics.runDQLQueryReturnSingleValue(dql, session);
//        IDfCollection col = runQuery(dql, session);
//        while (col.next()) {
//            result = true;
//        }
//        col.close();
        if (rv != null && !rv.isEmpty())
        	return result;
        else
        	return false;
    }

    public static String dumpProperties(boolean showPW) {
        StringBuilder out = new StringBuilder();

        Set s = m_properties.keySet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            String k = (String) i.next();
            String v = getProperty(k);
            if ((k.equalsIgnoreCase(EXPORT_PASSWORD_KEY) || (k.equalsIgnoreCase(IMPORT_PASSWORD_KEY))
                    && !showPW)) {
                v = "********";
            }
            out.append(k + "=" + v + "\n");
        }
        return out.toString();

    }

    public static boolean omitTypes(String obj_type) {

        for (String prefix : Utils.OMIT_OBJ_PREFIXES) {
            if (obj_type.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
    
    public static String cleanXML(String stringToclean) {
    	
    	return StringEscapeUtils.escapeXml(stringToclean);
    }
    
//    public static boolean checkDFCversion(String DFCversion) {
//        DFCversion = DFCversion.substring(0,3);
//        float fDFC = Float.parseFloat(DFCversion);
//        if (fDFC < 6.5)
//        	return false;
//        else 
//        	return true;
//    }
}

//<SDG><