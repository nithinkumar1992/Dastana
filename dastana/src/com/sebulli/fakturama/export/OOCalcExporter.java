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

package com.sebulli.fakturama.export;

import static com.sebulli.fakturama.Translate._;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import ag.ion.bion.officelayer.application.IOfficeApplication;
import ag.ion.bion.officelayer.application.OfficeApplicationException;
import ag.ion.bion.officelayer.document.DocumentDescriptor;
import ag.ion.bion.officelayer.document.IDocument;
import ag.ion.bion.officelayer.spreadsheet.ISpreadsheetDocument;
import ag.ion.noa.NOAException;

import com.sebulli.fakturama.Activator;
import com.sebulli.fakturama.data.DataSetDocument;
import com.sebulli.fakturama.data.UniDataSet;
import com.sebulli.fakturama.logger.Logger;
import com.sebulli.fakturama.misc.DataUtils;
import com.sebulli.fakturama.misc.DocumentType;
import com.sebulli.fakturama.office.OfficeStarter;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

/**
 * The sales exporter. This class collects all the sales and fills a Calc table
 * with the data
 * 
 * @author Gerd Bartelt
 */
public class OOCalcExporter {

	public final static boolean PAID = true;
	public final static boolean UNPAID = false;
	
	// The begin and end date to specify the export periode
	protected GregorianCalendar startDate;
	protected GregorianCalendar endDate;
	
	// Use start and end date or export all
	protected boolean doNotUseTimePeriod;

	// the date key to sort the documents
	protected String documentDateKey;
	// Settings from the preference page
	protected boolean usePaidDate ;

	// The "Export" spreadsheet
	protected XSpreadsheet spreadsheet = null;
	protected XSpreadsheetDocument xSpreadsheetDocument = null;

	// export paid or unpaid invoices
	protected boolean exportPaid = true;

	
	/**
	 * Default constructor
	 */
	public OOCalcExporter() {
		this.startDate = null;
		this.endDate = null;
		this.doNotUseTimePeriod = true;
	}

	/**
	 * Constructor Sets the begin and end date
	 * 
	 * @param startDate
	 *            Begin date
	 * @param endDate
	 *            Begin date
	 */
	public OOCalcExporter(GregorianCalendar startDate, GregorianCalendar endDate, boolean doNotUseTimePeriod) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.doNotUseTimePeriod = doNotUseTimePeriod;
	}

	protected void fillCompanyInformation(int row) {
		
		// Fill the first cells with company data
		setCellTextInItalic(row++, 0, Activator.getDefault().getPreferenceStore().getString("YOURCOMPANY_COMPANY_NAME"));
		setCellTextInItalic(row++, 0, Activator.getDefault().getPreferenceStore().getString("YOURCOMPANY_COMPANY_OWNER"));
		setCellTextInItalic(row++, 0, Activator.getDefault().getPreferenceStore().getString("YOURCOMPANY_COMPANY_STREET"));
		setCellTextInItalic(row++, 0, Activator.getDefault().getPreferenceStore().getString("YOURCOMPANY_COMPANY_ZIP") + " "
				+ Activator.getDefault().getPreferenceStore().getString("YOURCOMPANY_COMPANY_CITY"));

	}
	
	protected void fillTimeIntervall(int row) {

		// Do not display a time period
		if (doNotUseTimePeriod) {
			return;
		}
		
		// Display the time interval
		//T: Sales Exporter - Text in the Calc document for the period
		setCellTextInBold(row++, 0, _("Period"));
		//T: Sales Exporter - Text in the Calc document for the period
		setCellText(row, 0, _("from:"));
		setCellText(row++, 1, DataUtils.getDateTimeAsLocalString(startDate));
		//T: Sales Exporter - Text in the Calc document for the period
		setCellText(row, 0, _("till:"));
		setCellText(row++, 1, DataUtils.getDateTimeAsLocalString(endDate));
	}
	
	
	/**
	 * Returns, if a given document should be used to export. Only invoice and
	 * credit documents that are paid in the specified time interval are
	 * exported.
	 * 
	 * @param document
	 *            The document that is tested
	 * @return True, if the document should be exported
	 */
	protected boolean documentShouldBeExported(DataSetDocument document) {

		// By default, the document will be exported.
		boolean isInIntervall = true;

		// Use the time period
		if (!doNotUseTimePeriod) {
			// Get the date of the document and convert it to a
			// GregorianCalendar object.
			GregorianCalendar documentDate = new GregorianCalendar();
			try {
				DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

				String documentDateString = document.getStringValueByKey(documentDateKey);

				documentDate.setTime(formatter.parse(documentDateString));
			}
			catch (ParseException e) {
				Logger.logError(e, "Error parsing Date");
			}

			// Test, if the document's date is in the interval
			if ((startDate != null) && (endDate != null)) {
				if (startDate.after(documentDate))
					isInIntervall = false;
				if (endDate.before(documentDate))
					isInIntervall = false;
			}
		}

		// Only invoiced and credits in the interval
		// will be exported.
		boolean isInvoiceOrCreditInIntervall = ((document.getIntValueByKey("category") == DocumentType.INVOICE.getInt()) || (document.getIntValueByKey("category") == DocumentType.CREDIT
				.getInt())) && isInIntervall;
		
		
		// Export paid or unpaid documents
		if (exportPaid)
			// export paid
			return isInvoiceOrCreditInIntervall && document.getBooleanValueByKey("paid");
		else
			// export unpaid
			return isInvoiceOrCreditInIntervall && !document.getBooleanValueByKey("paid");
	}

	/**
	 * Returns, if a given data set should be used to export. Only
	 * entries in the specified time interval are exported.
	 * 
	 * @param uds
	 *            The uni data set that is tested
	 * @return True, if the uni data set should be exported
	 */
	protected boolean isInTimeIntervall(UniDataSet uds) {

		// By default, the document will be exported.
		boolean isInIntervall = true;

		// Use the time period
		if (doNotUseTimePeriod) {
			return true;
		}
		
		// Get the date of the voucher and convert it to a
		// GregorianCalendar object.
		GregorianCalendar documentDate = new GregorianCalendar();
		try {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

			String dateString = "";

			// Use date  
			dateString = uds.getStringValueByKey("date");

			documentDate.setTime(formatter.parse(dateString));
		}
		catch (ParseException e) {
			Logger.logError(e, "Error parsing Date");
		}

		// Test, if the voucher's date is in the interval
		if ((startDate != null) && (endDate != null)) {
			if (startDate.after(documentDate))
				isInIntervall = false;
			if (endDate.before(documentDate))
				isInIntervall = false;
		}

		// Return, if voucher is in the interval
		return isInIntervall;
	}
	
	protected boolean createSpreadSheet() {
		// Get the OpenOffice application
		final IOfficeApplication officeAplication = OfficeStarter.openOfficeApplication();
		if (officeAplication == null)
			return false;

		// Create a new OpenOffice Calc document
		IDocument oOdocument = null;
		try {
			oOdocument = officeAplication.getDocumentService().constructNewDocument(IDocument.CALC, DocumentDescriptor.DEFAULT);
		}
		catch (NOAException e) {
			Logger.logError(e, "NOA Error opening CALC");
			return false;
		}
		catch (OfficeApplicationException e) {
			Logger.logError(e, "OO Error opening CALC");
			return false;
		}

		// Get the spreadsheets
		ISpreadsheetDocument spreadDocument = (ISpreadsheetDocument) oOdocument;
		xSpreadsheetDocument = spreadDocument.getSpreadsheetDocument();
		XSpreadsheets spreadsheets = xSpreadsheetDocument.getSheets();

		try {
			//T: Name of the Table
			String tableName = _("Export");
			spreadsheets.insertNewByName(tableName, (short) 0);

			// Remove all other spreadsheets
			String names[] = spreadsheets.getElementNames();
			for (String name : names) {
				if (!name.equals(tableName))
					spreadsheets.removeByName(name);
			}

			// Get a reference to the Export sheet
			spreadsheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, spreadsheets.getByName(tableName));

		}
		catch (NoSuchElementException e) {
			Logger.logError(e, "Error getting spreadsheet");
			return false;
		}
		catch (WrappedTargetException e) {
			Logger.logError(e, "Error getting spreadsheet");
			return false;
		}
		return true;

	}
	

	/**
	 * Fill a cell with a text
	 * 
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param text
	 *            The text that will be insert
	 */
	protected void setCellText(int row, int column, String text) {
		XText cellText = (XText) UnoRuntime.queryInterface(XText.class, CellFormatter.getCell(spreadsheet, row, column));
		cellText.setString(text);
	}

	/**
	 * Fill a cell with a text. Use a bold font.
	 * 
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param text
	 *            The text that will be insert
	 */
	protected void setCellTextInBold(int row, int column, String text) {
		setCellText(row, column, text);
		CellFormatter.setBold(spreadsheet, row, column);
	}

	/**
	 * Fill a cell with a text. Use an italic font style.
	 * 
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param text
	 *            The text that will be insert
	 */
	protected void setCellTextInItalic(int row, int column, String text) {
		setCellText(row, column, text);
		CellFormatter.setItalic(spreadsheet, row, column);
	}

	/**
	 * Fill a cell with a text. Use a red and bold font
	 * 
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param text
	 *            The text that will be insert
	 */
	protected void setCellTextInRedBold(int row, int column, String text) {
		setCellText(row, column, text);
		CellFormatter.setBold(spreadsheet, row, column);
		CellFormatter.setColor(spreadsheet, row, column, 0x00FF0000);
	}

	/**
	 * Set a cell to a double value and format it with the local currency.
	 * 
	 * @param xSpreadsheetDocument
	 *            The spreadsheet document
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param d
	 *            The value that will be inserted.
	 */
	protected void setCellValueAsLocalCurrency( int row, int column, Double d) {
		CellFormatter.getCell(spreadsheet, row, column).setValue(d);
		CellFormatter.setLocalCurrency(xSpreadsheetDocument, spreadsheet, row, column);
	}
	
	protected void setBackgroundColor(int row, int column, int color) {
		CellFormatter.setBackgroundColor(spreadsheet, row, column, color);
	}

	protected void setBackgroundColor(int left, int top, int right, int bottom, int color) {
		CellFormatter.setBackgroundColor(spreadsheet, left, top, right, bottom , color);
	}

	protected void setBold(int row, int column) {
		CellFormatter.setBold(spreadsheet, row, column);
	}
	protected void setBorder(int row, int column, int color, boolean top, boolean right, boolean bottom, boolean left) {
		CellFormatter.setBorder(spreadsheet, row, column, color, top, right, bottom, left); 
	}

	protected void setFormula(int column, int row, String formula) {
		try {
			spreadsheet.getCellByPosition(column, row).setFormula(formula);
		}
		catch (IndexOutOfBoundsException e) {
			Logger.logError(e, "No access to cell: " + column + ":" +row);
		}
	}

}
