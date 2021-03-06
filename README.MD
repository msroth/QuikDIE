A quick Documentum import and export tool -- emphasis on the export.  There is some functional import code included but it is a work in progress.  Use at your own risk!

If you would like to complete the import code, please do so (just let me know!).

To run export, configure the export.properties file to contain the necessary login credentials for your repository, and a query to gather the files you want to export. In addition, specify a location for the exported content on your file system. Here is my sample export.properties file.

<pre>
export.query=select * from dm_sysobject where folder('/Temp',descend)
export.user=dmadmin
export.password=dmadmin
export.repo=repo1
export.path=c:/Temp/Export
export.log=c:/Temp/Export/export.log
</pre>

<b>Notes:</b>

1. After the export tool runs once it will encrypt the password so it doesn’t sit around unprotected.
2. The query must return r_object_id.
3. Once configured, run the export.bat batch file. The batch file does its best to create the correct class path, but you might have to help it if your Documentum environment is not installed in the default manner.

The export tool will export all of the objects it gathers from running the configured query. Each content object is exported to a file name that is its r_object_id (an easy way to avoid any file name collisions), followed by the DOS extension for its content type. The accompanying metadata is similarly named, followed with .metadata.xml. All files are exported to the path you specify in the export.properties file.   Note that no folder structures are created to mirror the structure in the repository. That is, the export directory will contain a “flat” file structure (that’s why avoiding file name collisions was important). Right now, the export tool can handle dm_sysobjects, dm_folders, virtual documents, and content-less objects. Since the export tool will export whatever the query returns, you can export versions by specifying ‘(all)‘ in the query, and subfolder structures by using the FOLDER(*,'descend') function.

Here is an example *.metadata.xml file.

![metadata xml](https://github.com/msroth/QuikDIE/tree/master/images/export_xml2.jpg)

<b>Notes:</b>

1. This particular file is a virtual document, and it’s child nodes are listed in the <vd_children> element.
2. By default, the export tool exports metadata values for:  object_name, object_type, title, subject, acl_domain, acl_name, owner, version and a_content_type) (if applicable).
3. If the type is a custom type (like this example is), values for all custom metadata properties will also be exported (note sr_cat_no, sr_lot_no, and sr_expire above).
4. If the export tool encounters an object that appears to be a custom type, it will export a .typedef.xml file that contains definitions for all its custom attributes.

Here is an example of a *.typedef.xml file.

![type xml](https://github.com/msroth/QuikDIE/tree/master/images/type_xml.jpg)
