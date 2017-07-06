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

/**
 * This class contains the total sum of volume and quantity
 * 
 * @author Gerd Bartelt
 */
public class TotalSoldAndQuantity {
	
	private Double totalSold = 0.0;
	private int totalQuantity = 0;
	
	/**
	 * Returns the total sum
	 * 
	 * @return
	 * 			The total sum
	 */
	public Double getTotalSold() {
		return totalSold;
	}

	/**
	 * Sets the total sum
	 * 
	 * @param totalSold
	 * 			The new value of the total sum
	 */
	public void setTotalSold(Double totalSold) {
		this.totalSold = totalSold;
	}
	
	/**
	 * Add a new value to the total sum
	 * 
	 * @param totalSold
	 * 			The new value to add
	 */
	public void addTotalSold(Double totalSold) {
		this.totalSold += totalSold;
	}
	
	/**
	 * Returns the total quantity
	 * 
	 * @return
	 * 			The total quantity
	 */
	public int getTotalQuantity() {
		return totalQuantity;
	}
	
	/**
	 * Sets the total quantity
	 * 
	 * @param totalQuantity
	 * 			The new value of the total quantity
	 */
	public void setTotalQuantity(int totalQuantity) {
		this.totalQuantity = totalQuantity;
	}
	
	/**
	 * Add a new value to the total sum
	 * 
	 * @param totalQuantity
	 * 			The new value to add
	 */
	public void addTotalQuantity(int totalQuantity) {
		this.totalQuantity += totalQuantity;
	}

	/**
	 * Add a new object to this one
	 * 
	 * @param totalSoldAndQuantity
	 * 			The new object to add
	 */
	public void add (TotalSoldAndQuantity totalSoldAndQuantity) {
		this.totalSold += totalSoldAndQuantity.getTotalSold();
		this.totalQuantity += totalSoldAndQuantity.getTotalQuantity();
	}
		
	
}
