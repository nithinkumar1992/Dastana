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

package com.sebulli.fakturama.views.datasettable;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.sebulli.fakturama.data.UniDataSet;
import com.sebulli.fakturama.misc.DataUtils;

/**
 * Filters the contents of the table view
 * 
 * @author Gerd Bartelt
 */
public class TableFilter extends ViewerFilter {

	private String searchColumns[];
	private String searchString;

	/**
	 * Constructor Set the search columns. Only these columns are compared with
	 * the search filter.
	 * 
	 * @param searchColumns
	 */
	public TableFilter(String searchColumns[]) {
		this.searchColumns = searchColumns;
	}

	/**
	 * Set the search string and add a wildcard character to the beginning and
	 * the end
	 * 
	 * @param s
	 *            The search string
	 */
	public void setSearchText(String s) {
		this.searchString = ".*(?i:" + s + ".*)";
	}

	/**
	 * Returns whether the given element makes it through this filter.
	 * 
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {

		// If the filter is empty, show all elements
		if (searchString == null || searchString.length() == 0) { return true; }

		// Get the element
		UniDataSet uds = (UniDataSet) element;

		// Search all the columns
		for (int i = 0; i < searchColumns.length; i++) {
			if (DataUtils.getSingleLine(uds.getStringValueByKey(searchColumns[i])).matches(searchString)) { return true; }
		}

		return false;
	}
}
