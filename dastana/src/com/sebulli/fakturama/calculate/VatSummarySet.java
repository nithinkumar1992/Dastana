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

package com.sebulli.fakturama.calculate;

import java.util.Iterator;
import java.util.TreeSet;

import com.sebulli.fakturama.misc.DataUtils;

/**
 * This Class can contain multiple VatSummaryItems.
 * 
 * If an item is added, and an other VatSummaryItem with the same name and the
 * same vat value in percent is existing, the absolute net and vat values of
 * this item are added to the existing.
 * 
 * If there is no entry with the same name and vat percent value, a new one is
 * created.
 * 
 * @author Gerd Bartelt
 */
public class VatSummarySet extends TreeSet<VatSummaryItem> {

	private static final long serialVersionUID = 1L;

	/**
	 * Add a new VatSummaryItem to this tree
	 * 
	 * @param vatSummaryItem
	 *            The new Item
	 * @return True, if it was added as new item
	 */
	@Override
	public boolean add(VatSummaryItem vatSummaryItemTemplate) {

		VatSummaryItem vatSummaryItem = new VatSummaryItem(vatSummaryItemTemplate);

		// try to add it
		boolean added = super.add(vatSummaryItem);

		// If there was already an item with the same value and name ..
		if (!added) {

			// add the net and vat to the existing one
			VatSummaryItem existing = super.ceiling(vatSummaryItem);
			existing.add(vatSummaryItem);
		}

		return added;
	}

	/**
	 * Returns the index of a VatSummaryItem
	 * 
	 * @param vatSummaryItem
	 *            to Search for
	 * @return index or -1, if it was not found.
	 */
	public int getIndex(VatSummaryItem vatSummaryItem) {
		int i = -1;

		// Search all items
		for (Iterator<VatSummaryItem> iterator = this.iterator(); iterator.hasNext();) {
			i++;
			VatSummaryItem item = iterator.next();

			// Returns the item, if it is the same
			if (item.compareTo(vatSummaryItem) == 0)
				break;
		}
		return i;
	}

	/**
	 * Round all items of the VatSummarySet
	 */
	public void roundAllEntries() {

		Double netSum = 0.0;
		Double vatSum = 0.0;
		Double netSumOfRounded = 0.0;
		Double vatSumOfRounded = 0.0;
		Double netRoundedSum = 0.0;
		Double vatRoundedSum = 0.0;
		int missingCents = 0;
		Double oneCent;
		Double roundingError;
		boolean searchForMaximum;

		// First, add all values to get the sum of net and vat
		for (Iterator<VatSummaryItem> iterator = this.iterator(); iterator.hasNext();) {
			VatSummaryItem item = iterator.next();

			//Add all values
			netSum += item.getNet();
			vatSum += item.getVat();
		}

		// Round the sum
		netRoundedSum = DataUtils.round(netSum);
		vatRoundedSum = DataUtils.round(vatSum);

		// round all items
		for (Iterator<VatSummaryItem> iterator = this.iterator(); iterator.hasNext();) {
			VatSummaryItem item = iterator.next();

			item.round();

			// calculate the sum of rounded values
			netSumOfRounded += item.getNet();
			vatSumOfRounded += item.getVat();

		}

		// Calculate the rounding error in cent
		roundingError = (netRoundedSum - netSumOfRounded) * 100.000001;
		missingCents = roundingError.intValue();

		// Decrease or increase the entries
		if (missingCents >= 0) {
			searchForMaximum = true;
			oneCent = 0.01;
		}
		else {
			searchForMaximum = false;
			missingCents = -missingCents;
			oneCent = -0.01;
		}

		// Dispense the missing cents to those values with the maximum
		// rounding error.
		for (int i = 0; i < missingCents; i++) {

			Double maxRoundingError = -oneCent;
			VatSummaryItem maxItem = null;

			// Search for the item with the maximum error
			for (Iterator<VatSummaryItem> iterator = this.iterator(); iterator.hasNext();) {
				VatSummaryItem item = iterator.next();

				// Search for maximum or minimum
				if (searchForMaximum) {
					if (item.getNetRoundingError() > maxRoundingError) {
						maxRoundingError = item.getNetRoundingError();
						maxItem = item;
					}
				}
				else {
					// If found, mark it
					if (item.getNetRoundingError() < maxRoundingError) {
						maxRoundingError = item.getNetRoundingError();
						maxItem = item;
					}
				}

			}

			// Correct the item be one cent
			if (maxItem != null) {
				maxItem.setNet(maxItem.getNet() + oneCent);
				maxItem.setNetRoundingError(maxItem.getNetRoundingError() - oneCent);
			}
		}

		// Do the same with the vat entry

		// Calculate the rounding error in cent
		roundingError = (vatRoundedSum - vatSumOfRounded) * 100.000001;
		missingCents = roundingError.intValue();

		// Decrease or increase the entries
		if (missingCents >= 0) {
			searchForMaximum = true;
			oneCent = 0.01;
		}
		else {
			searchForMaximum = false;
			missingCents = -missingCents;
			oneCent = -0.01;
		}

		// dispense the missing cents to those values with the maximum
		// rounding error.
		for (int i = 0; i < missingCents; i++) {

			Double maxRoundingError = -oneCent;
			VatSummaryItem maxItem = null;

			// Search for the item with the maximum error
			for (Iterator<VatSummaryItem> iterator = this.iterator(); iterator.hasNext();) {
				VatSummaryItem item = iterator.next();

				// Search for maximum or minimum
				if (searchForMaximum) {
					// If found, mark it
					if (item.getVatRoundingError() > maxRoundingError) {
						maxRoundingError = item.getVatRoundingError();
						maxItem = item;
					}
				}
				else {
					// If found, mark it
					if (item.getVatRoundingError() < maxRoundingError) {
						maxRoundingError = item.getVatRoundingError();
						maxItem = item;
					}
				}
			}

			// Correct the item by one cent
			if (maxItem != null) {
				maxItem.setVat(maxItem.getVat() + oneCent);
				maxItem.setVatRoundingError(maxItem.getVatRoundingError() - oneCent);
			}

		}

	}

	/**
	 * Add all items of an other VatSummarySet
	 * 
	 * @param otherVatSummarySet
	 *            The other VatSummarySet
	 */
	public void addVatSummarySet(VatSummarySet otherVatSummarySet) {

		// Add all items of the other VatSummarySet
		for (Iterator<VatSummaryItem> iterator = otherVatSummarySet.iterator(); iterator.hasNext();) {
			VatSummaryItem item = iterator.next();
			this.add(item);
		}

	}

}
