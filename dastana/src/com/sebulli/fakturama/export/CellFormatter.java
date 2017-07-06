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

import com.sebulli.fakturama.logger.Logger;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.Locale;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.BorderLine;
import com.sun.star.table.TableBorder;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;

/**
 * Formats an OpenOffice Calc cell. Sets the border, the color or the font
 * style.
 * 
 * @author Gerd Bartelt
 */
public class CellFormatter {

	/**
	 * Set the property of a Calc cell.
	 * 
	 * @param cell
	 *            The cell to format
	 * @param property
	 *            The property
	 * @param value
	 *            The value of the property
	 */
	private static void setCellProperty(XCell cell, String property, Object value) {

		// Get the property set of a cell
		XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, cell);

		try {

			// Set the cell's property to a new value
			xPropertySet.setPropertyValue(property, value);

		}
		catch (UnknownPropertyException e) {
			Logger.logError(e, "Error 'UnknownProperty' setting cell property " + property + " to " + value.toString());
		}
		catch (PropertyVetoException e) {
			Logger.logError(e, "Error 'PropertyVeto' setting cell property " + property + " to " + value.toString());
		}
		catch (IllegalArgumentException e) {
			Logger.logError(e, "Error 'IllegalArgument' setting cell property " + property + " to " + value.toString());
		}
		catch (WrappedTargetException e) {
			Logger.logError(e, "Error 'WrappedTarget' setting cell property " + property + " to " + value.toString());
		}

	}

	/**
	 * Set the property of a Calc cells range.
	 * 
	 * @param cell
	 *            The cells to format
	 * @param property
	 *            The property
	 * @param value
	 *            The value of the property
	 */
	private static void setCellsProperty(XCellRange cells, String property, Object value) {

		// Get the property set of a cell
		XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, cells);

		try {

			// Set the cell's property to a new value
			xPropertySet.setPropertyValue(property, value);

		}
		catch (UnknownPropertyException e) {
			Logger.logError(e, "Error 'UnknownProperty' setting cell property " + property + " to " + value.toString());
		}
		catch (PropertyVetoException e) {
			Logger.logError(e, "Error 'PropertyVeto' setting cell property " + property + " to " + value.toString());
		}
		catch (IllegalArgumentException e) {
			Logger.logError(e, "Error 'IllegalArgument' setting cell property " + property + " to " + value.toString());
		}
		catch (WrappedTargetException e) {
			Logger.logError(e, "Error 'WrappedTarget' setting cell property " + property + " to " + value.toString());
		}

	}

	/**
	 * Set the border of a cell to a specified color
	 * 
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param color
	 *            The new color of the border
	 * @param top
	 *            Select, if the top border should be modified
	 * @param right
	 *            Select, if the right border should be modified
	 * @param bottom
	 *            Select, if the bottom border should be modified
	 * @param left
	 *            Select, if the left border should be modified
	 */
	public static void setBorder(XSpreadsheet spreadsheet, int row, int column, int color, boolean top, boolean right, boolean bottom, boolean left) {

		// Get the cell by the row and the column
		XCell cell = getCell(spreadsheet, row, column);

		// Create an invisible border
		BorderLine noBorderLine = new BorderLine();
		noBorderLine.Color = 0;
		noBorderLine.InnerLineWidth = 0;
		noBorderLine.OuterLineWidth = 0;
		noBorderLine.LineDistance = 0;

		// Create a single line boarder
		BorderLine singleBorderLine = new BorderLine();
		singleBorderLine.Color = color;
		singleBorderLine.InnerLineWidth = 30;
		singleBorderLine.OuterLineWidth = 0;
		singleBorderLine.LineDistance = 0;

		// Create a border object to format a table
		TableBorder tableBorder = new TableBorder();

		// Set the top border
		if (top)
			tableBorder.TopLine = singleBorderLine;
		else
			tableBorder.TopLine = noBorderLine;
		tableBorder.IsTopLineValid = true;

		// Set the bottom border
		if (bottom)
			tableBorder.BottomLine = singleBorderLine;
		else
			tableBorder.BottomLine = noBorderLine;
		tableBorder.IsBottomLineValid = true;

		// Set the left border
		if (left)
			tableBorder.LeftLine = singleBorderLine;
		else
			tableBorder.LeftLine = noBorderLine;
		tableBorder.IsLeftLineValid = true;

		// Set the right border
		if (right)
			tableBorder.RightLine = singleBorderLine;
		else
			tableBorder.RightLine = noBorderLine;
		tableBorder.IsRightLineValid = true;

		// other settings
		tableBorder.HorizontalLine = noBorderLine;
		tableBorder.IsHorizontalLineValid = true;
		tableBorder.VerticalLine = noBorderLine;
		tableBorder.IsVerticalLineValid = true;

		// Set the cell property
		setCellProperty(cell, "TableBorder", tableBorder);

	}

	/**
	 * Set the text color of a cell
	 * 
	 * @param spreadsheet
	 *            The Spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param color
	 *            The new color of the text
	 */
	public static void setColor(XSpreadsheet spreadsheet, int row, int column, int color) {

		// Get the cell by the row and the column
		XCell cell = getCell(spreadsheet, row, column);

		// Set the new color
		setCellProperty(cell, "CharColor", new Integer(color));
	}

	/**
	 * Set the background color of a cell
	 * 
	 * @param spreadsheet
	 *            The Spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param color
	 *            The new color of the background
	 */
	public static void setBackgroundColor(XSpreadsheet spreadsheet, int row, int column, int color) {

		// Get the cell by the row and the column
		XCell cell = getCell(spreadsheet, row, column);

		// Set the new background color
		setCellProperty(cell, "CellBackColor", new Integer(color));
	}

	/**
	 * Set the background color of a cell
	 * 
	 * @param spreadsheet
	 *            The Spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @param color
	 *            The new color of the background
	 */
	public static void setBackgroundColor(XSpreadsheet spreadsheet, int left, int top, int right, int bottom, int color) {

		// Get the cell by the row and the column
		XCellRange cells = getCells(spreadsheet, left, top, right, bottom);

		// Set the new background color
		setCellsProperty(cells, "CellBackColor", new Integer(color));
	}

	/**
	 * Set the font weight of a cell to bold
	 * 
	 * @param spreadsheet
	 *            The Spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 */
	public static void setBold(XSpreadsheet spreadsheet, int row, int column) {

		// Get the cell by the row and the column
		XCell cell = getCell(spreadsheet, row, column);

		// Set the new font weight
		setCellProperty(cell, "CharWeight", new Float(com.sun.star.awt.FontWeight.BOLD));
	}

	/**
	 * Set the font style of a cell to italic
	 * 
	 * @param spreadsheet
	 *            The Spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 */
	public static void setItalic(XSpreadsheet spreadsheet, int row, int column) {

		// Get the cell by the row and the column
		XCell cell = getCell(spreadsheet, row, column);

		// Set the new font style
		setCellProperty(cell, "CharPosture", com.sun.star.awt.FontSlant.ITALIC);
	}

	/**
	 * Set the cell format to the local currency
	 * 
	 * @param xSpreadsheetDocument
	 *            The spreadsheet document
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 */
	public static void setLocalCurrency(XSpreadsheetDocument xSpreadsheetDocument, XSpreadsheet spreadsheet, int row, int column) {

		// Get the cell by the row and the column
		XCell cell = getCell(spreadsheet, row, column);

		// Query the number formats supplier of the spreadsheet document
		com.sun.star.util.XNumberFormatsSupplier xNumberFormatsSupplier = (com.sun.star.util.XNumberFormatsSupplier) UnoRuntime.queryInterface(
				com.sun.star.util.XNumberFormatsSupplier.class, xSpreadsheetDocument);

		// Get the number formats from the supplier
		com.sun.star.util.XNumberFormats xNumberFormats = xNumberFormatsSupplier.getNumberFormats();

		// Query the XNumberFormatTypes interface
		com.sun.star.util.XNumberFormatTypes xNumberFormatTypes = (com.sun.star.util.XNumberFormatTypes) UnoRuntime.queryInterface(
				com.sun.star.util.XNumberFormatTypes.class, xNumberFormats);

		// Get the standard currency of the system
		int nCurrKey = xNumberFormatTypes.getStandardFormat(com.sun.star.util.NumberFormat.CURRENCY, new Locale());

		// Set the new cell format
		setCellProperty(cell, "NumberFormat", new Integer(nCurrKey));
	}

	/**
	 * Get a cell by spreadsheet, row and column
	 * 
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param row
	 *            The cell row
	 * @param column
	 *            The cell column
	 * @return The cell
	 */
	public static XCell getCell(XSpreadsheet spreadsheet, int row, int column) {

		// Try to get the cell
		try {
			return spreadsheet.createCursor().getCellByPosition(column, row);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Get a cell range by spreadsheet, left, top, right, bottom
	 * 
	 * @param spreadsheet
	 *            The spreadsheet that contains the cell
	 * @param left
	 *            The left side of the range
	 * @param top
	 *            The top side of the range
	 * @param right
	 *            The right side of the range
	 * @param bottom
	 *            The bottom side of the range
	 * @return The cell range
	 */
	public static XCellRange getCells(XSpreadsheet spreadsheet, int left, int top, int right, int bottom) {

		// Try to get the cell
		try {
			return spreadsheet.createCursor().getCellRangeByPosition(left, top, right, bottom);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Get the name of a Calc cell by row and column
	 * 
	 * @param row
	 * @param column
	 * @return Cell name (like "A2")
	 */
	public static String getCellName(int row, int column) {

		// Most significant character
		char cM = 0;
		// Least significant character
		char cL;

		// Convert the column into a decimal format with
		// a base of 26 ( Number of letters in the alphabet) 
		// Use 2 characters. So A will be 1, Z will be 26 and
		// AA will be 27
		int columnM = column / 26;
		int columnL = column % 26;

		// Maximum 25x26 columns
		if (column > (25 * 26)) {
			Logger.logError("Columns out of range");
			return "ZZ1";
		}

		// Only if the column is > 26 (columnM >0), use
		// a second letter
		if (columnM > 0)
			cM = (char) ('A' + columnM - 1);

		// Convert the number to a letter
		cL = (char) ('A' + columnL);

		// Add Column letter and row number
		String s = "";
		s = cL + Integer.toString(row + 1);

		// Use a second letter
		if (columnM > 0)
			s = cM + s;

		// The complete cell name
		return s;
	}

}
