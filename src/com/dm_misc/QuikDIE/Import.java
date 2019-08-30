package com.dm_misc.QuikDIE;

import java.io.File;
import java.util.ArrayList;

import com.dm_misc.dctm.DCTMBasics;
import com.documentum.fc.client.IDfSession;

public class Import {

	
	public static void main(String[] args) {

		try {
			System.out.println("\n\nQuikDIE - Quick Documentum Import/Export");
			System.out.println("Import Content Module");
			System.out.println("========================================");
			Import imprt = new Import(args);
			imprt.run();
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private Import(String[] args) {

		//TODO: over ride properties with command line args?
	}

	public void run() throws Exception {

		//IDfClientX cx = new DfClientX();
		
        // check if DFC version is supported
        if (!DCTMBasics.checkDFCversion(6.5))
        	throw new Exception ("Only DFC v6.5 and higher is supported");
        
//		IDfClient client = cx.getLocalClient();
//		IDfSessionManager sessionMgr = client.newSessionManager();
		IDfSession session = null;
		int objCount = 0;

		// read properties file
		if (!Utils.loadConfig(this.getClass(),Utils.IMPORT_PROPERTY_FILE))
			throw new Exception("Could not load configuration");

		// validate import path
		if (!Utils.validateFileSystemPath(Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY),false))
			throw new Exception("Problem with import source path: " + Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY));
		else
			System.out.println("Import source path: " + Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY));

		// create log file
		if (!Utils.createLogFile(Utils.IMPORT_LOG_KEY))
			throw new Exception("Could not create log file: " + Utils.getProperty(Utils.IMPORT_LOG_KEY));
		else
			System.out.println("Import log: " + Utils.getProperty(Utils.IMPORT_LOG_KEY));

//		// create error path
//		if (!Utils.validateFileSystemPath(Utils.getProperty(Utils.IMPORT_ERRORS_PATH),true))
//			throw new Exception("Problem with error path: " + Utils.IMPORT_ERRORS_PATH);
//		else
//			System.out.println("Import error path: " + Utils.getProperty(Utils.IMPORT_ERRORS_PATH));

		// check encrypted password
		Utils.checkPassword(Utils.OP_IMPORT);

		// login
		//session = Utils.login(Utils.getProperty(Utils.IMPORT_DOCBASE_KEY), Utils.getProperty(Utils.IMPORT_USER_KEY), Utils.getProperty(Utils.IMPORT_PASSWORD_KEY), sessionMgr);
		session = DCTMBasics.logon(Utils.getProperty(Utils.IMPORT_DOCBASE_KEY), Utils.getProperty(Utils.IMPORT_USER_KEY), Utils.getProperty(Utils.IMPORT_PASSWORD_KEY));
		if (session == null)
			throw new Exception("Could not establish session with " + Utils.getProperty(Utils.IMPORT_DOCBASE_KEY));
		else
			System.out.println("Login successful:  " + Utils.getProperty(Utils.IMPORT_USER_KEY) + "@" + Utils.getProperty(Utils.IMPORT_DOCBASE_KEY));

		// validate repo path
		if (!Boolean.parseBoolean(Utils.getProperty(Utils.IMPORT_USE_EXPORT_REPO_ATTRS_KEY))) {
			if (Utils.getProperty(Utils.IMPORT_REPO_PATH_KEY).length() > 0) {
				if (!Utils.validateRepoPath(Utils.getProperty(Utils.IMPORT_REPO_PATH_KEY),session,true))
					throw new Exception("Problem with import repo path: " + Utils.getProperty(Utils.IMPORT_REPO_PATH_KEY));
				else
					System.out.println("Import repo path: " + Utils.getProperty(Utils.IMPORT_REPO_PATH_KEY));
			} else {
				System.out.println("No import path specified; will use export paths in metadata files.");
				Utils.setProperty(Utils.IMPORT_USE_EXPORT_REPO_ATTRS_KEY, "true");
			}
					
		}
		System.out.println();
		
		// do type defs
		ArrayList<String> typeList = getXMLFilesToImport(Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY),Utils.TYPEDEF_FILE_EXT);
		doCreateTypes(typeList,session);
		objCount += typeList.size();

		// do folders
		ArrayList<String> folderList = getXMLFilesToImport(Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY),Utils.FOLDER_FILE_EXT);
		doImport(folderList, Utils.IMPORT_TYPE_FOLDER, session);
		objCount += folderList.size();
		
		// do content objects
		ArrayList<String> docList = getXMLFilesToImport(Utils.getProperty(Utils.IMPORT_FILES_PATH_KEY),Utils.METADATA_FILE_EXT);
		doImport(docList, Utils.IMPORT_TYPE_CONTENT, session);
		objCount += docList.size();
		
		// close DCTM session
		if (session != null)
			session.getSessionManager().release(session);
		
		// close log file and end
		Utils.writeLog("Imported " + objCount + " objects.");
		System.out.println("Imported " + objCount + " objects.");
		Utils.closeLogFile();	
		System.out.println("Done.");
	}

	private ArrayList<String> getXMLFilesToImport(String path, String filemask) {
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

	private void doImport(ArrayList<String> importFiles, String objType, IDfSession session) {
		String template = "Importing %s object %s ==> ";
		
		for (String f : importFiles) {
			
			System.out.print(String.format(template,objType, f));
			
			try {
				ImportObj iObj = new ImportObj(f,session);
				if (iObj.success()) {

					// log success
					Utils.writeLog("IMPORTED: " + iObj.getImportFileName() + " ==> " + Utils.getObjectPath(iObj.getSysObject(), session) + "/" + iObj.getSysObject().getObjectName() + "(" + iObj.getObjectId() + ")");
					System.out.println(Utils.getObjectPath(iObj.getSysObject(), session) + "/" + iObj.getSysObject().getObjectName() + "(" + iObj.getObjectId() + ")");
					if (iObj.getErrorMsg().length() > 0)
						System.out.println("\t" + iObj.getErrorMsg());

				} else {

					// log failure
					Utils.writeLog("ERROR: failed to import " + iObj.getImportFileName() + " ==> " + iObj.getErrorMsg());
					System.out.println("ERROR: " + iObj.getErrorMsg());
					
					// move files
//					File[] files = {iObj.metadataFile(), iObj.contentFile()};
//					Utils.moveFilesToDir(files, Utils.IMPORT_ERRORS_PATH);

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	private void doCreateTypes(ArrayList<String> typedefs, IDfSession session) {
//		String template = "Creating type %s ==> ";
//		
//		for (String f : typedefs) {
//			
//			System.out.print(String.format(template,f));
//			
//			try {
//				TypeObj tObj = new TypeObj(f,session);
//				if (tObj.success()) {

					// log success
//					Utils.writeLog("IMPORTED: " + iObj.getImportFileName() + " ==> " + Utils.getObjectPath(iObj.getSysObject(), session) + "/" + iObj.getSysObject().getObjectName() + "(" + iObj.getObjectId() + ")");
//					System.out.println(Utils.getObjectPath(iObj.getSysObject(), session) + "/" + iObj.getSysObject().getObjectName() + "(" + iObj.getObjectId() + ")");
//					if (iObj.getErrorMsg().length() > 0)
//						System.out.println("\t" + iObj.getErrorMsg());

//				} else {

//					// log failure
//					Utils.writeLog("ERROR: failed to import " + iObj.getImportFileName() + " ==> " + iObj.getErrorMsg());
//					System.out.println("ERROR: " + iObj.getErrorMsg());
//					
//					// move files
////					File[] files = {iObj.metadataFile(), iObj.contentFile()};
////					Utils.moveFilesToDir(files, Utils.IMPORT_ERRORS_PATH);

//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		
	}

}
