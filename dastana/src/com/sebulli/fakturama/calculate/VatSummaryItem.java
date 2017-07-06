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

/**
 * This Class represents one entry in the VatSummarySet. It contains a net and
 * vat value and the vat name.
 * 
 * @author Gerd Bartelt
 */
public class VatSummaryItem implements Comparable<Object> {

	// Absolute Net and Vat value
	// This can be the sum of more than one item
	private PriceValue net;
	private PriceValue vat;

	// Rounding errors
	private Double netRoundingError;
	private Double vatRoundingError;

	// Vat Name and Percent Value. These values identify the VatSummaryItem
	private String vatName;
	private String description;
	private Double vatPercent;

	/**
	 * Constructor Creates a VatSummaryItem from a net and vat value and the vat
	 * name.
	 * 
	 * @param vatName
	 *            Vat name
	 * @param vatPercent
	 *            Vat value in percent
	 * @param net
	 *            Absolute Net value
	 * @param vat
	 *            Absolute Vat value
	 */
	public VatSummaryItem(String vatName, Double vatPercent, Double net, Double vat) {
		this(vatName, vatPercent, net, vat, "");
	}

	/**
	 * Constructor Creates a VatSummaryItem from a net and vat value and the vat
	 * name with an additional description
	 * 
	 * @param vatName
	 *            Vat name
	 * @param vatPercent
	 *            Vat value in percent
	 * @param net
	 *            Absolute Net value
	 * @param vat
	 *            Absolute Vat value
	 * @param description
	 *            Additional description
	 */
	public VatSummaryItem(String vatName, Double vatPercent, Double net, Double vat, String description) {
		this.vatName = vatName;
		this.vatPercent = vatPercent;
		this.net = new PriceValue(net);
		this.vat = new PriceValue(vat);
		this.netRoundingError = 0.0;
		this.vatRoundingError = 0.0;
		this.description = description;
	}

	/**
	 * Constructor Creates a VatSummaryItem from an existing VatSummaryItem.
	 * 
	 * @param vatSummaryItem
	 */
	public VatSummaryItem(VatSummaryItem vatSummaryItem) {
		this.vatName = new String(vatSummaryItem.vatName);
		this.vatPercent = new Double(vatSummaryItem.vatPercent);
		this.net = new PriceValue(vatSummaryItem.net);
		this.vat = new PriceValue(vatSummaryItem.vat);
		this.netRoundingError = 0.0;
		this.vatRoundingError = 0.0;
		this.description = new String(vatSummaryItem.description);
	}

	/**
	 * Add the net and vat value from an other VatSummaryItem.
	 * 
	 * @param other
	 *            The other VatSummaryItem
	 */
	public void add(VatSummaryItem other) {
		this.net.add(other.net.asDouble());
		this.vat.add(other.vat.asDouble());
	}

	/**
	 * Round the net and vat value and store the rounding error in the property
	 * "xxRoundingError"
	 */
	public void round() {

		// Round the net value
		netRoundingError = this.net.asDouble() - this.net.asRoundedDouble();
		this.net.set(this.net.asRoundedDouble());

		// Round the vat value
		vatRoundingError = this.vat.asDouble() - this.vat.asRoundedDouble();
		this.vat.set(this.vat.asRoundedDouble());
	}

	/**
	 * Sets the absolute net value
	 * 
	 * @param Net
	 *            value
	 */
	public void setNet(Double value) {
		net.set(value);
	}

	/**
	 * Sets the absolute vat value
	 * 
	 * @param Vat
	 *            value
	 */
	public void setVat(Double value) {
		vat.set(value);
	}

	/**
	 * Get the absolute net value
	 * 
	 * @return Net value as Double
	 */
	public Double getNet() {
		return net.asDouble();
	}

	/**
	 * Get the absolute vat value
	 * 
	 * @return Vat value as Double
	 */
	public Double getVat() {
		return vat.asDouble();
	}

	/**
	 * Get the name of the vat
	 * 
	 * @return Vat name as string
	 */
	public String getVatName() {
		return vatName;
	}

	/**
	 * Get the description
	 * 
	 * @return Vat name as string
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Percent value of this VatSummaryItem
	 * 
	 * @return Vat in percent
	 */
	public Double getVatPercent() {
		return vatPercent;
	}

	/**
	 * Get the rounding error of the net value
	 * 
	 * @return rounding error as Double
	 */
	public Double getNetRoundingError() {
		return netRoundingError;
	}

	/**
	 * Get the rounding error of the vat value
	 * 
	 * @return rounding error as Double
	 */
	public Double getVatRoundingError() {
		return vatRoundingError;
	}

	/**
	 * Sets the rounding error of the net value
	 * 
	 * @param new rounding error value
	 */
	public void setNetRoundingError(Double value) {
		netRoundingError = value;
	}

	/**
	 * Sets the rounding error of the vat value
	 * 
	 * @param new rounding error value
	 */
	public void setVatRoundingError(Double value) {
		vatRoundingError = value;
	}

	/**
	 * Compares this VatSummaryItem with an other Compares vat percent value and
	 * vat name.
	 * 
	 * @param o
	 *            The other VatSummaryItem
	 * @return result of the comparison
	 */
	@Override
	public int compareTo(Object o) {
		VatSummaryItem other = (VatSummaryItem) o;

		// First compare the vat value in percent
		if (this.vatPercent < other.vatPercent)
			return -1;
		if (this.vatPercent > other.vatPercent)
			return 1;

		// Then the vat name
		int i = this.vatName.compareToIgnoreCase(other.vatName);
		if (i != 0)
			return i;

		// Then the description
		return this.description.compareToIgnoreCase(other.description);
	}
}
