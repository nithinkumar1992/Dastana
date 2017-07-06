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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import org.eclipse.swt.widgets.Display;

import ag.ion.bion.officelayer.application.IOfficeApplication;
import ag.ion.bion.officelayer.application.OfficeApplicationException;
import ag.ion.bion.officelayer.desktop.GlobalCommands;
import ag.ion.bion.officelayer.desktop.IFrame;
import ag.ion.bion.officelayer.document.IDocument;
import ag.ion.bion.officelayer.filter.PDFFilter;
import ag.ion.bion.officelayer.text.IText;
import ag.ion.bion.officelayer.text.ITextContentService;
import ag.ion.bion.officelayer.text.ITextDocument;
import ag.ion.bion.officelayer.text.ITextDocumentImage;
import ag.ion.bion.officelayer.text.ITextField;
import ag.ion.bion.officelayer.text.ITextFieldService;
import ag.ion.bion.officelayer.text.ITextTable;
import ag.ion.bion.officelayer.text.ITextTableCell;
import ag.ion.bion.officelayer.text.TextException;
import ag.ion.noa.NOAException;
import ag.ion.noa.document.URLAdapter;
import ag.ion.noa.frame.IDispatchDelegate;
import ag.ion.noa.graphic.GraphicInfo;

import com.sebulli.fakturama.Activator;
import com.sebulli.fakturama.OSDependent;
import com.sebulli.fakturama.Workspace;
import com.sebulli.fakturama.calculate.Price;
import com.sebulli.fakturama.calculate.VatSummaryItem;
import com.sebulli.fakturama.calculate.VatSummarySet;
import com.sebulli.fakturama.calculate.VatSummarySetManager;
import com.sebulli.fakturama.data.Data;
import com.sebulli.fakturama.data.DataSetDocument;
import com.sebulli.fakturama.data.DataSetItem;
import com.sebulli.fakturama.logger.Logger;
import com.sebulli.fakturama.misc.DataUtils;
import com.sebulli.fakturama.misc.Placeholders;
import com.sebulli.fakturama.views.datasettable.ViewDataSetTable;
import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.text.HoriOrientation;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.VertOrientation;
import com.sun.star.uno.UnoRuntime;

/**
 * This class opens an OpenOffice Writer template  and replaces all the
 * placeholders with the document data.
 * 
 * @author Gerd Bartelt
 */
public class OfficeDocument {

	// The UniDataSet document, that is used to fill the OpenOffice document 
	private DataSetDocument document;

	// The UniDataSet contact of the document
	//	private DataSetContact contact;

	// A list of properties that represents the placeholders of the
	// OpenOffice Writer template
	private Properties properties;

	// OpenOffice objects
	private IOfficeApplication officeApplication;
	private IDocument oOdocument;
	private ITextDocument textDocument;
	private IFrame officeFrame;

	private ITextFieldService textFieldService;

	// Template name
	private String template;	
	
	private ArrayList<String> allPlaceholders;
	
	// View with all documents
	private ViewDataSetTable documentView;
	
	/**
	 * Constructor Create a new OpenOffice document. Open it by using a template
	 * and replace the placeholders with the UniDataSet document
	 * 
	 * @param document
	 *            The UniDataSet document that will be converted to an
	 *            OpenOffice Writer document
	 * @param template
	 *            OpenOffice template file name
	 */
	public OfficeDocument(DataSetDocument document, String template, boolean forceRecreation, ViewDataSetTable documentView) {

		// Set a reference to the documents view
		this.documentView = documentView;
		
		// URL of the template file
		String url = null;
		this.template = template;
		//Open an existing document instead of creating a new one
		boolean openExisting = false;

		// Set a reference to the UniDatSet document
		this.document = document;

		// Try to generate the OpenOffice document
		try {

			// Get the OpenOffice application
			officeApplication = OfficeStarter.openOfficeApplication();
			if (officeApplication == null)
				return;
			
			// Check, whether there is already a document then do not 
			// generate one by the data, but open the existing one.
			if (testOpenAsExisting(document, template) && !forceRecreation) {
				openExisting = true;
				template = FileOrganizer.getDocumentPath(
						FileOrganizer.WITH_FILENAME,
						FileOrganizer.WITH_EXTENSION,
						FileOrganizer.ODT,
						document);
			}
			
			// Get the template file (*ott)
			try {
				url = URLAdapter.adaptURL(template);
			}
			catch (Exception e) {
				Logger.logError(e, "Error in template filename:" + template);
			}

			//Workaround for a NOA problem
			Thread.sleep(200);

			// Load the template
			oOdocument = officeApplication.getDocumentService().loadDocument(url);
			textDocument = (ITextDocument) oOdocument;

			// Bring the open office window on top.
			officeFrame = textDocument.getFrame();
			XFrame xFrame = officeFrame.getXFrame();
			XTopWindow topWindow = (XTopWindow) UnoRuntime.queryInterface(XTopWindow.class, xFrame.getContainerWindow());
			topWindow.toFront();
			xFrame.activate();

			// Override the "SAVE" command of the OpenOffice application
			officeFrame.addDispatchDelegate(GlobalCommands.SAVE, new IDispatchDelegate() {

				@Override
				public void dispatch(Object[] objects) {

					// Save the document as *.odt and *.pdf
					saveOODocument(textDocument);

				}

			});
			officeFrame.updateDispatches();

			// Stop here and do not fill the document's placeholders, if it's an existing document
			if (openExisting)
				return;

			// Recalculate the sum of the document before exporting
			this.document.calculate();

			
			// Get the placeholders of the OpenOffice template
			textFieldService = textDocument.getTextFieldService();
			ITextField[] placeholders = textFieldService.getPlaceholderFields();

			// Create a new ArrayList with all placeholders
			allPlaceholders = new ArrayList<String>();

			// This is a workaroud for a NOA problem.
			// Scan max. 10 times, until all placeholders are with a name
			boolean emptyPlaceholders;
			int i_emptyPHsearch = 0;
			do{
				textFieldService = textDocument.getTextFieldService();
				placeholders = textFieldService.getPlaceholderFields();

				emptyPlaceholders = false;
				
				// Scan all placeholders to find the item and the vat table
				for (ITextField placeholder : placeholders) {
					
					// Is there an empty placeholder ?
					if (placeholder.getDisplayText().isEmpty())
						emptyPlaceholders = true;
				}

				//If there was an empty placeholder, wait and repeat
				if (emptyPlaceholders)
					Thread.sleep(500);
				
				i_emptyPHsearch++;
			} while (emptyPlaceholders && (i_emptyPHsearch<10));

			//System.out.println("Search for empty Placeholders:" + i_emptyPHsearch);
			// JOptionPane.showMessageDialog(null,"Infozeichen","Titel", JOptionPane.INFORMATION_MESSAGE);
			
			for (ITextField placeholder : placeholders) {
				// Collect all placeholders
				allPlaceholders.add(placeholder.getDisplayText());
			}
			
			// Fill the property list with the placeholder values
			properties = new Properties();
			setCommonProperties();

			// A reference to the item and vat table
			ITextTable itemsTable = null;
			ITextTable vatListTable = null;
			ITextTableCell itemCell = null;
			ITextTableCell vatListCell = null;
			ArrayList<ITextTableCell> discountCellList = new ArrayList<ITextTableCell>();
			ArrayList<ITextTableCell> depositCellList = new ArrayList<ITextTableCell>();
			
			// Scan all placeholders to find the item and the vat table
			for (int i = 0; i < placeholders.length; i++) {

				// Get the placeholder's text
				ITextField placeholder = placeholders[i];
				String placeholderDisplayText = placeholder.getDisplayText().toUpperCase();
				
				// Find the item table
				if (placeholderDisplayText.startsWith("<ITEM.")) {
					itemCell = placeholder.getTextRange().getCell();
					itemsTable = itemCell.getTextTable();
				}

				// Find the vat table
				if (placeholderDisplayText.startsWith("<VATLIST.")) {
					vatListCell = placeholder.getTextRange().getCell();
					vatListTable = vatListCell.getTextTable();
				}

				// Find the discount placeholders
				if (placeholderDisplayText.startsWith("<ITEMS.DISCOUNT.")) {
					discountCellList.add(placeholder.getTextRange().getCell());
				}
				// Find the deposit placeholders
				if (placeholderDisplayText.startsWith("<DOCUMENT.DEPOSIT.")) {
					depositCellList.add(placeholder.getTextRange().getCell());
				}
			}

			// Get the items of the UniDataSet document
			ArrayList<DataSetItem> itemDataSets = document.getItems().getActiveDatasets();
			int lastItemTemplateRow = 0;
			int lastVatTemplateRow = 0;

			// Fill the item table with the items
			if (itemsTable != null) {

				// Add the necessary rows for the items
				int itemCellRow = itemCell.getName().getRowIndex();
				lastItemTemplateRow = itemCellRow + itemDataSets.size();
				itemsTable.addRow(itemCellRow, itemDataSets.size());

				for (int i = 0; i < placeholders.length; i++) {

					// Get each placeholder
					ITextField placeholder = placeholders[i];
					String placeholderDisplayText = placeholder.getDisplayText();

					if (placeholder.getTextRange().getCell() != null) {

						// Do it only, if the placeholder is in the items table
						ITextTable textTable = placeholder.getTextRange().getCell().getTextTable();
						if (textTable.getName().equals(itemsTable.getName())) {

							// Fill the corresponding table column with the
							// item's data.
							int column = placeholder.getTextRange().getCell().getName().getColumnIndex();
							ITextTableCell itemC = placeholder.getTextRange().getCell();
							String itemCellText = itemC.getTextService().getText().getText();
							fillItemTableWithData(placeholderDisplayText, column, itemDataSets, itemsTable, itemCellRow, itemCellText);
						}
					}
				}
			}

			// Get the VAT summary of the UniDataSet document
			VatSummarySetManager vatSummarySetManager = new VatSummarySetManager();
			vatSummarySetManager.add(this.document, 1.0);

			int vatListTemplateRow = 0;
			if (vatListTable != null) {

				// Add the necessary rows for the VAT entries
				vatListTemplateRow = vatListCell.getName().getRowIndex();
				lastVatTemplateRow = vatListTemplateRow + vatSummarySetManager.size();
				vatListTable.addRow(vatListTemplateRow, vatSummarySetManager.size());

				// Scan all placeholders for the VAT placeholders
				for (int i = 0; i < placeholders.length; i++) {

					// Get the placeholder text
					ITextField placeholder = placeholders[i];
					String placeholderDisplayText = placeholder.getDisplayText().toUpperCase();

					if (placeholder.getTextRange().getCell() != null) {

						// Test, if the placeholder is in the VAT table
						ITextTable textTable = placeholder.getTextRange().getCell().getTextTable();
						if (textTable.getName().equals(vatListTable.getName())) {

							// Fill the corresponding table column with the
							// VAT data.
							int column = placeholder.getTextRange().getCell().getName().getColumnIndex();
							ITextTableCell c = placeholder.getTextRange().getCell();
							String cellText = c.getTextService().getText().getText();
							replaceVatListPlaceholder(placeholderDisplayText, column, vatSummarySetManager.getVatSummaryItems(), vatListTable,
									vatListTemplateRow, cellText);
						}
					}
				}
			}

			// Replace all other placeholders
			for (int i = 0; i < placeholders.length; i++) {
				replaceText(placeholders[i]);
			}

			// remove the temporary row of the item table
			if (itemsTable != null) {
				itemsTable.removeRow(lastItemTemplateRow);
			}

			// remove the temporary row of the VAT table
			if (vatListTable != null) {
				vatListTable.removeRow(lastVatTemplateRow);
			}

			// Remove the discount cells, if there is no discount set
			if (DataUtils.DoublesAreEqual(document.getSummary().getDiscountNet().asDouble(), 0.0)) {
				for (int i = 0; i < discountCellList.size(); i++) {
					ITextTableCell cell = discountCellList.get(i);
					try {
						if (cell != null) {
							ITextTable table = cell.getTextTable();
							if (table != null)
								table.removeRow(cell.getName().getRowIndex());
						}
					}
					catch (TextException te) {
					}
				}
			}

			// Remove the Deposit & the Finalpayment Row if there is no Deposit
			if (DataUtils.DoublesAreEqual(document.getSummary().getDeposit().asDouble(), 0.0)) {
				for (int i = 0; i < depositCellList.size(); i++) {
					ITextTableCell cell = depositCellList.get(i);
					try {
						if (cell != null) {
							ITextTable table = cell.getTextTable();
							if (table != null)
								table.removeRow(cell.getName().getRowIndex()-1);
								table.removeRow(cell.getName().getRowIndex());
						}
					}
					catch (TextException te) {
					}
				}
			}
			
			// Save the document
			saveOODocument(textDocument);

			// Print and close the OpenOffice document
			/*
			textDocument.getFrame().getDispatch(GlobalCommands.PRINT_DOCUMENT_DIRECT).dispatch();
			try {
			    Thread.sleep(2000);
			}
			catch (Exception e1) {
			    e1.printStackTrace();
			}
			textDocument.close();
			*/

			//officeAplication.deactivate();

		}
		catch (Exception e) {
			Logger.logError(e, "Error starting OpenOffice from " + url);
		}
	}

	/**
	 * Close the connection to the OpenOffice Document
	 */
	public void close() {

		// Remove the SAVE dispatcher
		if (officeFrame != null)
			officeFrame.removeDispatchDelegate(GlobalCommands.SAVE);

		// Close only open document
		if (oOdocument != null && oOdocument.isOpen())
			oOdocument.close();
		
		// Get the remaining documents
		int remainingDocuments = 0;
		
		try {
			remainingDocuments = officeApplication.getDocumentService().getCurrentDocuments().length;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		// Close the OpenOffice document
		try {
			if (officeApplication != null)
				if (remainingDocuments == 0 ) {
					officeApplication.deactivate();
				}
		}
		catch (OfficeApplicationException e) {
			Logger.logError(e, "Error closing OpenOffice");
		}
	}


	/**
	 * Save an OpenOffice document as *.odt and as *.pdf
	 * 
	 * @param textDocument
	 *            The document
	 */
	public void saveOODocument(ITextDocument textDocument) {

		boolean wasSaved = false;

		if (Activator.getDefault().getPreferenceStore().getString("OPENOFFICE_ODT_PDF").contains("PDF")) {

			// Create the directories, if they don't exist.
			File directory = new File(FileOrganizer.getDocumentPath(
					FileOrganizer.NO_FILENAME,
					FileOrganizer.NO_EXTENSION,
					FileOrganizer.PDF,
					document));
			
			if (!directory.exists())
				directory.mkdirs();

			// Add the time String, if this file is still existing
			/*
			File file = new File(savePath + ".odt");
			if (file.exists()) {
				DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
				savePath += "_" + dfmt.format(new Date());
			}
			*/

			// Save the document
			try {
				FileOutputStream fs = new FileOutputStream(new File(FileOrganizer.getDocumentPath(
						FileOrganizer.WITH_FILENAME,
						FileOrganizer.WITH_EXTENSION,
						FileOrganizer.PDF,
						document)));
				
				PDFFilter pdfFilter = new PDFFilter();
				pdfFilter.getPDFFilterProperties().setPdfVersion(1);
				textDocument.getPersistenceService().export(fs, pdfFilter);

				wasSaved = true;

			}
			catch (FileNotFoundException e) {
				Logger.logError(e, "Error saving the OpenOffice Document");
			}
			catch (NOAException e) {
				Logger.logError(e, "Error saving the OpenOffice Document");
			}

		}

		if (Activator.getDefault().getPreferenceStore().getString("OPENOFFICE_ODT_PDF").contains("ODT")) {

			// Create the directories, if they don't exist.
			File directory = new File(FileOrganizer.getDocumentPath(
					FileOrganizer.NO_FILENAME,
					FileOrganizer.NO_EXTENSION,
					FileOrganizer.ODT,
					document));
			
			if (!directory.exists())
				directory.mkdirs();

			// Add the time String, if this file is still existing
			/*
			File file = new File(savePath + ".odt");
			if (file.exists()) {
				DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
				savePath += "_" + dfmt.format(new Date());
			}
			*/

			// Save the document
			try {
				FileOutputStream fs = new FileOutputStream(new File(FileOrganizer.getDocumentPath(
						FileOrganizer.WITH_FILENAME,
						FileOrganizer.WITH_EXTENSION,
						FileOrganizer.ODT,
						document)));
				textDocument.getPersistenceService().storeAs(fs);
				

				wasSaved = true;

			}
			catch (FileNotFoundException e) {
				Logger.logError(e, "Error saving the OpenOffice Document");
			}
			catch (NOAException e) {
				Logger.logError(e, "Error saving the OpenOffice Document");
			}

		}


		// Mark the document as printed, if it was saved as ODT or PDF
		if (wasSaved) {
			// Mark the document as "printed"
			document.setBooleanValueByKey("printed", true);
			document.setStringValueByKey("printedtemplate", template);
			
			String filename = FileOrganizer.getDocumentPath(
					FileOrganizer.WITH_FILENAME,
					FileOrganizer.WITH_EXTENSION,
					FileOrganizer.ODT,
					document);
			if ((new File(filename)).exists()) {
				document.setStringValueByKey("odtpath",
						filename);
			}

			// Update the document entry "pdfpath"
			filename = FileOrganizer.getDocumentPath(FileOrganizer.WITH_FILENAME,
					FileOrganizer.WITH_EXTENSION,
					FileOrganizer.PDF, document);
			if ((new File(filename)).exists()) {
				document.setStringValueByKey("pdfpath",
						filename);
			}

			Data.INSTANCE.getDocuments().updateDataSet(document);
			
			// Refresh the document view
			if (documentView != null) {
				Display.getDefault().syncExec(new Runnable() {
				    public void run() {
				    	documentView.refresh();
				    }
				});
			}

		}

	}

	/**
	 * Replace one column of the VAT table with the VAT entries
	 * 
	 * @param placeholderDisplayText
	 *            Name of the column, and of the VAT property
	 * @param column
	 *            Number of the column in the table
	 * @param vatSummarySet
	 *            VAT data
	 * @param vatListTable
	 *            The VAT table to fill
	 * @param templateRow
	 *            The first row of the table
	 * @param cellText
	 *            The cell's text.
	 */
	private void replaceVatListPlaceholder(String placeholderDisplayText, int column, VatSummarySet vatSummarySet, ITextTable vatListTable, int templateRow,
			String cellText) {
		int i = 0;

		// Get all VATs
		for (Iterator<VatSummaryItem> iterator = vatSummarySet.iterator(); iterator.hasNext(); i++) {
			VatSummaryItem vatSummaryItem = iterator.next();
			try {

				// Get the cell and fill the cell content
				IText iText = vatListTable.getCell(column, templateRow + i).getTextService().getText();
				fillVatTableWithData(placeholderDisplayText, vatSummaryItem, iText, i, cellText);

			}
			catch (TextException e) {
				Logger.logError(e, "Error replacing Vat List Placeholders");
			}
		}
	}

	/**
	 * Add a user text field to the OpenOffice document
	 * 
	 * @param key
	 *            The key of the user text field
	 * @param value
	 *            The value of the user text field
	 */
	private void addUserTextField(String key, String value) {

		if (value == null)
			return;
		
		try {
			textFieldService.addUserTextField(key, value);
		}
		catch (TextException e) {
		}
	}

	/**
	 * Add a user text field to the OpenOffice document The key contains an
	 * additional index.
	 * 
	 * @param key
	 *            The key of the user text field
	 * @param value
	 *            The value of the user text field
	 * @param i
	 *            Additional index, added to the key
	 */
	private void addUserTextField(String key, String value, int i) {
		key = key + "." + Integer.toString(i);
		addUserTextField(key, value);
	}

	/**
	 * Fill the cell of the VAT table with the VAT data
	 * 
	 * @param placeholderDisplayText
	 *            Column header
	 * @param key
	 *            VAT key (VAT description)
	 * @param value
	 *            VAT value
	 * @param iText
	 *            The Text that is set
	 * @param index
	 *            Index of the VAT entry
	 * @param cellText
	 *            The cell's text.
	 */
	private void fillVatTableWithData(String placeholderDisplayText, VatSummaryItem vatSummaryItem, IText iText, int index, String cellText) {
		String key = vatSummaryItem.getVatName();
		String value = Double.toString(vatSummaryItem.getVat());
		// Get the text of the column. This is to determine, if it is the column
		// with the VAT description or with the VAT value
		String textValue;
		String textKey = placeholderDisplayText.substring(1, placeholderDisplayText.length() - 1);

		// It's the VAT description
		if (placeholderDisplayText.equals("<VATLIST.DESCRIPTIONS>")) {
			textValue = key;
		}
		// It's the VAT value
		else if (placeholderDisplayText.equals("<VATLIST.VALUES>")) {
			textValue = DataUtils.DoubleToFormatedPriceRound(Double.parseDouble(value));
		}
		else if (placeholderDisplayText.equals("<VATLIST.PERCENT>")) {
			textValue = DataUtils.DoubleToFormatedPercent(vatSummaryItem.getVatPercent());
		}
		else if (placeholderDisplayText.equals("<VATLIST.VATSUBTOTAL>")) {
			textValue = DataUtils.DoubleToFormatedPrice(vatSummaryItem.getNet());
		}
		else
			return;

		// Set the text
		iText.setText(cellText.replaceAll(placeholderDisplayText, Matcher.quoteReplacement(textValue)));

		// And also add it to the user defined text fields in the OpenOffice
		// Writer document.
		addUserTextField(textKey, textValue, index);

	}

	/**
	 * Fill all cells of the item table with the item data
	 * 
	 * @param placeholderDisplayText
	 *            Column header
	 * @param column
	 *            The index of the column
	 * @param itemDataSets
	 *            Item data
	 * @param itemsTable
	 *            The item table
	 * @param lastTemplateRow
	 *            Counts the last row of the table
	 * @param cellText
	 *            The cell's text.
	 */
	private void fillItemTableWithData(String placeholderDisplayText, int column, ArrayList<DataSetItem> itemDataSets, ITextTable itemsTable,
			int lastTemplateRow, String cellText) {

		// Get all items
		for (int row = 0; row < itemDataSets.size(); row++) {
			try {

				// Get a reference to the cell content
				IText iText = itemsTable.getCell(column, lastTemplateRow + row).getTextService().getText();

				// Get the item
				DataSetItem item = itemDataSets.get(row);

				// Set the cell content
				fillItemTableWithData(row, placeholderDisplayText, item, iText, row, cellText);

			}
			catch (TextException e) {
				Logger.logError(e, "Error replacing Placeholders");
			}
		}

	}

	/**
	 * Converts all \r\n to \n
	 * \r\n are Generated by SWT text controls on a windows system.
	 * 
	 * @param s
	 * 		The string to convert
	 * @return
	 * 		The converted string
	 */
	private String convertCRLF2LF(String s){
		s = s.replaceAll("\\r\\n", "\n");
		return s;
	}
	
	/**
	 * Fill the cell of the item table with the item data
	 * 
	 * @param placeholderDisplayText
	 *            Column header
	 * @param item
	 * @param iText
	 *            The Text that is set
	 * @param index
	 *            Index of the VAT entry
	 * @param cellText
	 *            The cell's text.
	 */
	private void fillItemTableWithData(int row, String placeholderDisplayText, DataSetItem item, IText iText, int index, String cellText) {

		String value = "";
		
		// Get the column's header
		String placeholder = placeholderDisplayText.substring(1, placeholderDisplayText.length() - 1);
		String key = placeholder.split("\\$")[0];

		Price price = new Price(item);

		// Get the item quantity
		if (key.equals("ITEM.QUANTITY")) {
			value = DataUtils.DoubleToFormatedQuantity(item.getDoubleValueByKey("quantity"));
		}


		// The position
		else if (key.equals("ITEM.POS")) {
			value = Integer.toString(row + 1);
		}

		// The text for optional items
		else if (key.equals("ITEM.OPTIONAL.TEXT")) {
			if (item.getBooleanValueByKey("optional")) {
				value = Activator.getDefault().getPreferenceStore().getString("OPTIONALITEMS_OPTIONALITEM_TEXT");
				value = value.replaceAll("<br>", "\n");
			}
		}
		
		// Get the item name
		else if (key.equals("ITEM.NAME")) {
			value = item.getStringValueByKey("name");
		}

		// Get the item number
		else if (key.equals("ITEM.NR")) {
			value = item.getStringValueByKey("itemnr");
		}

		// Get the quanity unit
		else if (key.equals("ITEM.QUANTITYUNIT")) {
			value = item.getStringValueByKey("qunit");
		}

		// Get the item description
		else if (key.equals("ITEM.DESCRIPTION")) {
			value = item.getStringValueByKey("description");
			// Remove pre linebreak if description is empty to avoid empty lines
			if( value.isEmpty() ) {
				placeholderDisplayText = placeholderDisplayText.replaceFirst("\n<ITEM.DESCRIPTION>", "<ITEM.DESCRIPTION>");
			}
		}

		// Get the item discount
		else if (key.equals("ITEM.DISCOUNT.PERCENT")) {
			value = DataUtils.DoubleToFormatedPercent(item.getDoubleValueByKey("discount"));
		}

		// Get the item's VAT
		else if (key.equals("ITEM.VAT.PERCENT")) {
			value = DataUtils.DoubleToFormatedPercent(item.getDoubleValueByKey("vatvalue"));
		}

		// Get the item's VAT name
		else if (key.equals("ITEM.VAT.NAME")) {
			value = item.getStringValueByKey("vatname");
		}

		// Get the item's VAT description
		else if (key.equals("ITEM.VAT.DESCRIPTION")) {
			value = item.getStringValueByKey("vatdescription");
		}

		// Get the item net value
		else if (key.equals("ITEM.UNIT.NET")) {
			value = price.getUnitNetRounded().asFormatedString();
		}

		// Get the item VAT
		else if (key.equals("ITEM.UNIT.VAT")) {
			value = price.getUnitVatRounded().asFormatedString();
		}

		// Get the item gross value
		else if (key.equals("ITEM.UNIT.GROSS")) {
			value = price.getUnitGrossRounded().asFormatedString();
		}

		// Get the discounted item net value
		else if (key.equals("ITEM.UNIT.NET.DISCOUNTED")) {
			value = price.getUnitNetDiscountedRounded().asFormatedString();
		}

		// Get the discounted item VAT
		else if (key.equals("ITEM.UNIT.VAT.DISCOUNTED")) {
			value = price.getUnitVatDiscountedRounded().asFormatedString();
		}

		// Get the discounted item gross value
		else if (key.equals("ITEM.UNIT.GROSS.DISCOUNTED")) {
			value = price.getUnitGrossDiscountedRounded().asFormatedString();
		}

		// Get the total net value
		else if (key.equals("ITEM.TOTAL.NET")) {
			value = price.getTotalNetRounded().asFormatedString();
			if (item.getBooleanValueByKey("optional")) {
				if (Activator.getDefault().getPreferenceStore().getBoolean("OPTIONALITEMS_REPLACE_PRICE"))
					value = Activator.getDefault().getPreferenceStore().getString("OPTIONALITEMS_PRICE_REPLACEMENT");
			}

		}

		// Get the total VAT
		else if (key.equals("ITEM.TOTAL.VAT")) {
			value = price.getTotalVatRounded().asFormatedString();
			if (item.getBooleanValueByKey("optional")) {
				if (Activator.getDefault().getPreferenceStore().getBoolean("OPTIONALITEMS_REPLACE_PRICE"))
					value = Activator.getDefault().getPreferenceStore().getString("OPTIONALITEMS_PRICE_REPLACEMENT");
			}
		}

		// Get the total gross value
		else if (key.equals("ITEM.TOTAL.GROSS")) {
			value = price.getTotalGrossRounded().asFormatedString();
			if (item.getBooleanValueByKey("optional")) {
				if (Activator.getDefault().getPreferenceStore().getBoolean("OPTIONALITEMS_REPLACE_PRICE"))
					value = Activator.getDefault().getPreferenceStore().getString("OPTIONALITEMS_PRICE_REPLACEMENT");
			}
		}
		
		// Get product picture
		else if (key.startsWith("ITEM.PICTURE")){
			
			String width_s = Placeholders.extractParam(placeholder,"WIDTH");
			String height_s = Placeholders.extractParam(placeholder,"HEIGHT");

			if (!item.getStringValueByKey("picturename").isEmpty()) {
				// Default height and with
				int pixelWidth = 0;
				int pixelHeight = 0;

				// Use the parameter values
				try {
					pixelWidth = Integer.parseInt(width_s);
					pixelHeight = Integer.parseInt(height_s);
				}
				catch (NumberFormatException e) {
				}
				
				// Use default values
				if (pixelWidth < 1)
					pixelWidth = 150;
				if (pixelHeight < 1)
					pixelHeight = 100;


				String imagePath = Workspace.INSTANCE.getWorkspace() + 
				 					Workspace.productPictureFolderName + 
				 					item.getStringValueByKey("picturename");
				int pictureHeight = 100;
				int pictureWidth = 100;
				double pictureRatio = 1.0;
				double pixelRatio = 1.0;
			      
				// Read the image a first time to get width and height
				try {
					File f = new File(imagePath);
					BufferedImage image = ImageIO.read(f);
					pictureHeight = image.getHeight();
					pictureWidth = image.getWidth();

					// Calculate the ratio of the original image
					if (pictureHeight > 0) {
						pictureRatio = (double)pictureWidth/(double)pictureHeight;
					}
					
					// Calculate the ratio of the placeholder
					if (pixelHeight > 0) {
						pixelRatio = (double)pixelWidth/(double)pixelHeight;
					}
					
					// Correct the height and width of the placeholder 
					// to match the original image
					if ((pictureRatio > pixelRatio) &&  (pictureRatio != 0.0)) {
						pixelHeight = (int) Math.round(((double)pixelWidth / pictureRatio));
					}
					if ((pictureRatio < pixelRatio) &&  (pictureRatio != 0.0)) {
						pixelWidth = (int) Math.round(((double)pixelHeight * pictureRatio));
					}
					
					// Generate the image
					GraphicInfo graphicInfo = null;
					graphicInfo = new GraphicInfo(new FileInputStream(imagePath),
						    pixelWidth,
						    true,
						    pixelHeight,
						    true,
						    VertOrientation.TOP,
						    HoriOrientation.LEFT,
						    TextContentAnchorType.AT_PARAGRAPH);

					ITextContentService textContentService = textDocument.getTextService().getTextContentService();
					ITextDocumentImage textDocumentImage = textContentService.constructNewImage(graphicInfo);
					textContentService.insertTextContent(iText.getTextCursorService().getTextCursor().getEnd(), textDocumentImage);

					// replace the placeholder
					iText.setText(cellText.replaceAll(Matcher.quoteReplacement(placeholderDisplayText), Matcher.quoteReplacement(value)));
					return;
				}
				catch (IOException e) {
				}
				catch (NOAException e) {
				}
				catch (TextException e) {
					e.printStackTrace();
				}
			}
			
			value = "";
		}
		
		else
			return;

		// Interpret all parameters
		value = Placeholders.interpretParameters(placeholder,value);
		
		// Convert CRLF to LF 
		value = convertCRLF2LF(value);

		// If iText's string is not empty, use that string instead of the template
		String iTextString = iText.getText();
		if (!iTextString.isEmpty()) {
			cellText = iTextString;
		}
		
		// Set the text of the cell
		iText.setText(cellText.replaceAll(Matcher.quoteReplacement(placeholderDisplayText), Matcher.quoteReplacement(value)));

		// And also add it to the user defined text fields in the OpenOffice
		// Writer document.
		addUserTextField(key, value, index);
	}

	/**
	 * Set a property and add it to the user defined text fields in the
	 * OpenOffice Writer document.
	 * 
	 * @param key
	 *            The property key
	 * @param value
	 *            The property value
	 */
	private void setProperty(String key, String value) {

		if (key == null)
			return;

		if (value == null)
			return;
		
		// Convert CRLF to LF 
		value = convertCRLF2LF(value);
		
		// Set the user defined text field
		addUserTextField(key, value);
		
		// Extract parameters
		for (String placeholder : allPlaceholders) {
			if ( (placeholder.equals("<" + key+">")) || 
					( (placeholder.startsWith("<" + key+"$")) && (placeholder.endsWith(">")) ) ) {

				// Set the placeholder
				properties.setProperty(placeholder.toUpperCase(), Placeholders.interpretParameters(placeholder, value));
			}
		}
		
	}
	
	/**
	 * Set a common property
	 * 
	 * @param key
	 * 	Name of the placeholder
	 */
	private void setCommonProperty(String key) {
		setProperty(key,Placeholders.getDocumentInfo( document, key) );
		
	}
	
	
	/**
	 * Fill the property list with the placeholder values
	 */
	private void setCommonProperties() {

		if (document == null)
			return;
		
		document.calculate();

		// Get all placeholders and set them
		for (String placeholder: Placeholders.getPlaceholders()) {
			setCommonProperty(placeholder);
		}

	}

	/**
	 * Replace a placeholder with the content of the property in the property
	 * list.
	 * 
	 * @param placeholder
	 *            The placeholder and the name of the key in the property list
	 */
	private void replaceText(ITextField placeholder) {
		// Get the placeholder's text
		String placeholderDisplayText = placeholder.getDisplayText().toUpperCase();
		
		// Get the value of the Property list.
		String text = properties.getProperty(placeholderDisplayText);
		
		// If the String is non empty, replace the OS new line with the OpenOffice new line
		if(text != null){
			text = text.replaceAll(OSDependent.getNewLine(), "\r");
		}
		// Replace the placeholder with the value of the property list.
		placeholder.getTextRange().setText(text);
	}
	
	static public boolean testOpenAsExisting(DataSetDocument document, String template) {
		// Check, whether there is already a document then do not 
		// generate one by the data, but open the existing one.
		File oODocumentFile = new File(FileOrganizer.getDocumentPath(
				FileOrganizer.WITH_FILENAME,
				FileOrganizer.WITH_EXTENSION, 
				FileOrganizer.ODT, document));
		
		if (oODocumentFile.exists() && document.getBooleanValueByKey("printed") &&
				DocumentFilename.filesAreEqual(document.getStringValueByKey("printedtemplate"),template)) {
			return true;
		}
		
		return false;
	
	}

}
