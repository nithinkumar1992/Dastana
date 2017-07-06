/* 
 * Fakturama - Free Invoicing Software - http://fakturama.sebulli.com
 * 
 * Copyright (C) 2012 Gerd Bartelt
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gerd Bartelt - initial API and implementation
 */

package com.sebulli.fakturama.office;

import static com.sebulli.fakturama.Translate._;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Version;

import com.sebulli.fakturama.Activator;
import com.sebulli.fakturama.data.Data;
import com.sebulli.fakturama.data.DataSetDocument;
import com.sebulli.fakturama.misc.DataUtils;
import com.sebulli.fakturama.misc.DocumentType;

public class FileOrganizer {

	final public static boolean WITH_FILENAME = true;
	final public static boolean NO_FILENAME = false;
	final public static boolean WITH_EXTENSION = true;
	final public static boolean NO_EXTENSION = false;
	final public static boolean PDF = true;
	final public static boolean ODT = false;

	// Counts the documents and show the progress in the status bar
	static private int i;

	/**
	 * Returns the filename (relative to the workspace) of the OpenOffice
	 * document
	 * 
	 * @param inclFilename
	 *            True, if also the filename should be used
	 * @param inclExtension
	 *            True, if also the extension should be used
	 * @param PDF
	 *            True, if it's the PDF filename
	 * @return The filename
	 */
	public static String getOldRelativeDocumentPath(boolean inclFilename,
			boolean inclExtension, boolean isPDF, DataSetDocument document) {

		String savePath = "";

		// T: Subdirectory of the OpenOffice documents
		savePath += _("/Documents");

		if (isPDF)
			savePath += "/PDF/";
		else
			savePath += "/OpenOffice/";

		savePath += DocumentType.getPluralString(document
				.getIntValueByKey("category")) + "/";

		// Use the document name as filename
		if (inclFilename)
			savePath += document.getStringValueByKey("name");

		// Use the document name as filename
		if (inclExtension) {
			if (isPDF)
				savePath += ".pdf";
			else
				savePath += ".odt";
		}

		return savePath;

	}

	/**
	 * Replace all characters, that are not allowed in the path
	 * 
	 * @param s
	 * 	The String with special characters
	 * @return
	 *  The clean string
	 */
	public static String replaceIllegalCharacters (String s) {
		s = s.replaceAll(" ", "_");
		s = s.replaceAll("\\\\", "_");
		s = s.replaceAll("\"", "_");
		s = s.replaceAll("/", "_");
		s = s.replaceAll("\\:", "_");
		s = s.replaceAll("\\*", "_");
		s = s.replaceAll("\\?", "_");
		s = s.replaceAll("\\>", "_");
		s = s.replaceAll("\\<", "_");
		s = s.replaceAll("\\|", "_");
		s = s.replaceAll("\\&", "_");
		s = s.replaceAll("\\n", "_");
		s = s.replaceAll("\\t", "_");
		return s;
	}
	
	/**
	 * Generates the document file name from the document and the
	 * placeholder string in the preference page
	 * 
	 * @param inclFilename
	 * 	True, if the filename should also be returned
	 * @param inclExtension
	 *  True, if the extension should also be returned
	 * @param isPDF
	 *  True, if it is a PDF File
	 * @param document
	 *  The document
	 * @return
	 *  The filename
	 */
	public static String getRelativeDocumentPath(boolean inclFilename,
			boolean inclExtension, boolean isPDF, DataSetDocument document) {

		String odtpdf;
		if (isPDF)
			odtpdf = "pdf";
		else
			odtpdf = "odt";

		// T: Subdirectory of the OpenOffice documents
		String savePath = _("/Documents") + "/";
		String fileNamePlaceholder;
		fileNamePlaceholder	= Activator.getDefault().getPreferenceStore().getString("OPENOFFICE_" +odtpdf.toUpperCase() + "_PATH_FORMAT");

		// Replace all backslashes
		fileNamePlaceholder = fileNamePlaceholder.replace('\\', '/');
		
		String path = "";
		String filename = "";

		// Remove the extension
		if (fileNamePlaceholder.toLowerCase().endsWith("." + odtpdf) ||
			 fileNamePlaceholder.toLowerCase().endsWith(".pdf") )
			fileNamePlaceholder = fileNamePlaceholder.substring(0, fileNamePlaceholder.length()-4);
		
		// Replace the placeholders
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{docname\\}", document.getStringValueByKey("name"));
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{doctype\\}", DocumentType.getPluralString(document.getIntValueByKey("category")));
		
		String address = document.getStringValueByKey("addressfirstline");
		address = replaceIllegalCharacters(address);
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{address\\}", address);
		
		String name = document.getFormatedStringValueByKeyFromOtherTable("addressid.CONTACTS:name");
		name = replaceIllegalCharacters(name);
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{name\\}", name);
		
		// Find the placeholder for a decimal number with n digits
		// with the format "{Xnr}", "X" is the number of digits (which can be empty).
		Pattern p = Pattern.compile("\\{(\\d*)nr\\}");
		Matcher m = p.matcher(fileNamePlaceholder);
		if(m.find()) {  // found?
			String replacementString = "";
			String replaceNumberString = "%d";     // default
			if(m.groupCount() > 0) {               // has some digits before <nr>?
				String numberString = m.group(1);  // get the length for the resulting number
				if(numberString.matches("\\d+")) { // is this really a number?
					replaceNumberString = "%0"+numberString+"d";  // build a format replacement string 
				}
			}
			// find the current docNumber
			Pattern docNumberPattern = Pattern.compile("\\w+(\\d+)");
			Matcher docNumberMatcher = docNumberPattern.matcher(document.getStringValueByKey("name"));
			if(docNumberMatcher.find()) {
				if(docNumberMatcher.groupCount() > 0) {
					String docNumberString = docNumberMatcher.group(1);
					Integer docNumber = Integer.valueOf(docNumberString);
					replacementString = String.format(replaceNumberString, docNumber);
				}
			}
			fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{\\d*nr\\}", replacementString);
		}

		
		String dateString = document.getStringValueByKey("date");
		GregorianCalendar calendar = DataUtils.getCalendarFromDateString(dateString);
		
		int yyyy = calendar.get(Calendar.YEAR);
		int mm = calendar.get(Calendar.MONTH) + 1;
		int dd = calendar.get(Calendar.DAY_OF_MONTH);

		// Replace the date information
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{yyyy\\}", String.format("%04d", yyyy));
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{yy\\}", String.format("%04d", yyyy).substring(2, 4));
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{mm\\}", String.format("%02d", mm));
		fileNamePlaceholder = fileNamePlaceholder.replaceAll("\\{dd\\}", String.format("%02d", dd));

		// Extract path and filename
		int pos =  fileNamePlaceholder.lastIndexOf('/');
		
		if (pos < 0) {
			path = "";
			filename = fileNamePlaceholder;
		}
		else {
			path = fileNamePlaceholder.substring(0 , pos);
			filename = fileNamePlaceholder.substring(pos + 1);
		}

		savePath += path + "/";	

		// Use the document name as filename
		if (inclFilename)
			savePath += filename;

		// Use the document name as filename
		if (inclExtension) {
			savePath += "." + odtpdf;
		}

		return savePath;

	}

	
	
	/**
	 * Returns the filename (with path) of the Office document
	 * including the workspace path
	 * 
	 * @param inclFilename
	 *            True, if also the filename should be used
	 * @param inclExtension
	 *            True, if also the extension should be used
	 * @param PDF
	 *            True, if it's the PDF filename
	 * @return The filename
	 */
	public static String getDocumentPath(boolean inclFilename,
			boolean inclExtension, boolean isPDF, DataSetDocument document) {

		String workspace = Activator.getDefault().getPreferenceStore().getString("GENERAL_WORKSPACE");

		return workspace
				+ getRelativeDocumentPath(inclFilename, inclExtension, isPDF,
						document);
	}

	/**
	 * Updates the country codes list
	 * 
	 * @param version
	 *            The new Version
	 */
	public static void update(final Version newVersion, final Version oldVersion, final StatusLineManager slm) {
		if ((newVersion.compareTo(new Version (1,5,0)) >= 0 ) && 
		    (oldVersion.compareTo(new Version (1,5,0)) < 0  )){

			// Do the update in a new thread
			new Thread(new Runnable() {
				public void run() {

					// Get the path to the workspace
					ArrayList<DataSetDocument> documents = Data.INSTANCE
							.getDocuments().getActiveDatasets();
					String savePath = Activator.getDefault()
							.getPreferenceStore()
							.getString("GENERAL_WORKSPACE");

					i = 0;

					// Get all documents
					for (DataSetDocument document : documents) {
						
						boolean updated = false;

						if (document.getStringValueByKey("odtpath").isEmpty()
								&& document.getStringValueByKey("pdfpath")
										.isEmpty()) {

							
							// Update the document entry "odtpath"
							String filename = FileOrganizer
									.getOldRelativeDocumentPath(
											FileOrganizer.WITH_FILENAME,
											FileOrganizer.WITH_EXTENSION,
											FileOrganizer.ODT, document);

							if ((new File(savePath + filename)).exists()) {
								updated = true;
								document.setStringValueByKey("odtpath",
										filename);
							}

							// Update the document entry "pdfpath"
							filename = FileOrganizer.getOldRelativeDocumentPath(
									FileOrganizer.WITH_FILENAME,
									FileOrganizer.WITH_EXTENSION,
									FileOrganizer.PDF, document);

							if ((new File(savePath + filename)).exists()) {
								updated = true;
								document.setStringValueByKey("pdfpath",
										filename);
							}

							// Show a message in the status bar
							if (slm != null && updated) {
								i++;

								Display.getDefault().syncExec(new Runnable() {
									public void run() {
										// T: Message in the status bar
										slm.setMessage(_("Updating documents "
												+ i));
									}
								});
							}

						}

						// Update the document in the data base
						Data.INSTANCE.updateDataSet(document);

					}

				}
			}).start();

		}
	}
	
	/**
	 * Move a file and create the directories, if they do not exist
	 * @param source
	 * 	Source file name
	 * @param destination
	 *  Destination file name
	 */
	private static void fileMove (String source, String destination) {
		
		// Replace backslashed
		destination = destination.replace('\\', '/');

		// Extract the path
		String path;

		int pos =  destination.lastIndexOf('/');
		
		if (pos < 0) {
			path = "";
		}
		else {
			path = destination.substring(0 , pos);
		}

		// Create the directories
		File folder = new File(path);
		if (!folder.exists())
			folder.mkdirs();
		
		// Move it, if possible
		File temp = new File(destination);
		File sourceFile = new File(source);
		 
	    if(!temp.exists() && sourceFile.exists())
	        	sourceFile.renameTo(temp);
	        	
	}
	
	/**
	 * Reorganize a document's odt and pdf file
	 * 
	 * @param workspacePath
	 * 	The workspace path
	 * @param document
	 *  The document
	 * @param isPDF
	 *  PDF or ODT
	 * @return
	 *  True, if it was successful
	 */
	private static boolean reorganizeDocument(String workspacePath, DataSetDocument document, boolean isPDF) {
		String odtpdf;
		
		// ODT or PDF string
		boolean changed = false;
		if (isPDF)
			odtpdf = "pdf";
		else
			odtpdf = "odt";

		//Get the old path from the document
		String oldDocument = document.getStringValueByKey(odtpdf+ "path");
		
		if (oldDocument.isEmpty())
			return false;
		
		// Update the document entry "odtpath"
		String filename = FileOrganizer
				.getRelativeDocumentPath(
						WITH_FILENAME,
						WITH_EXTENSION,
						isPDF, document);

		// Move it, if it is existing
		if ((new File(oldDocument)).exists() &&
				!oldDocument.equals(workspacePath + filename)) {
			fileMove(oldDocument, workspacePath + filename);
			document.setStringValueByKey(odtpdf+ "path", workspacePath + filename);
			changed = true;
		}

		return changed;
		
	}
	
	/**
	 * Reorganize all documents
	 * 
	 * @param slm
	 *  Status line Manager, to display the success
	 */
	public static void reorganizeDocuments (final StatusLineManager slm) {
		
		
		// Get all documents
		ArrayList<DataSetDocument> documents = Data.INSTANCE.getDocuments().getActiveDatasets();
		// Get the workspace path
		String workspacePath = Activator.getDefault().getPreferenceStore().getString("GENERAL_WORKSPACE");
		
		i = 0;
		// Get all documents
		for (DataSetDocument document : documents) {
			
			boolean changed = false;

			// Rename and move the ODT file.
			if (reorganizeDocument(workspacePath, document, ODT)) {
				changed = true;
			}
			
			// Rename and move the PDF file
			if (reorganizeDocument(workspacePath, document, PDF)) {
				changed = true;
			}
			
			// Update the document in the database
			if (changed) {
				Data.INSTANCE.updateDataSet(document);
			}
			
			// Count the documents
			i++;
			
			// Show the progress in the status bar
			if (slm != null) {
				Display.getDefault().syncExec(new Runnable() {
				    public void run() {
						//T: Message in the status bar
				    	slm.setMessage(_("Reoganizing document " + i));
				    }
				});
			}
		}
	}

}
