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

import java.util.HashMap;
import java.util.Map;

import com.sebulli.fakturama.calculate.Price;
import com.sebulli.fakturama.data.DataSetItem;

/**
 * This class contains the total sum and a list with all the customer,
 * who bought this item.
 * 
 * @author Gerd Bartelt
 */
public class BuyersAndTotal extends Object{

	// Sum of the item
	private TotalSoldAndQuantity totalSoldAndQuantity= new TotalSoldAndQuantity();
	
	// List with all buyers
	private Map<String, TotalSoldAndQuantity> buyers = new HashMap<String, TotalSoldAndQuantity>();
	
	/** 
	 * Constructor
	 * 
	 * Create a new instance 
	 */
	public BuyersAndTotal () {
	}
	/** 
	 * Constructor
	 * 
	 * Create a new instance and add the item and the buyer

	 * @param buyer 
	 * 			The buyer who bought this item
	 * @param item 
	 * 			The item
	 */
	public BuyersAndTotal (String buyer, DataSetItem item) {
		add(buyer, item);
	}


	/**
	 *  Add a new item with it's buyer
	 *  
	 * @param buyer
	 * 			The buyer who bought this item
	 * @param item 
	 * 			The item to add
	 */
	public void add(String buyer, DataSetItem item) {
		
		// Get the item's price
		Price price = new Price(item);
		Double total = price.getTotalGrossRounded().asDouble();

		// Create a new TotalSoldAndQuantity object with the items data
		TotalSoldAndQuantity buyerTotalSoldAndQuantity = new TotalSoldAndQuantity();
		buyerTotalSoldAndQuantity.setTotalSold(total);
		buyerTotalSoldAndQuantity.setTotalQuantity(item.getIntValueByKey("quantity"));

		// Add it to the total sum of this item
		totalSoldAndQuantity.add(buyerTotalSoldAndQuantity);
		
		// And to the List of all buyers
		if (buyers.containsKey(buyer)) {
			buyers.get(buyer).add(buyerTotalSoldAndQuantity);
		}
		else {
			buyers.put(buyer, buyerTotalSoldAndQuantity);
		}
	}
	
	/**
	 * Returns the sum of all sold
	 * 
	 * @return 
	 * 		The sum 
	 */
	public Double getTotalSold () {
		return totalSoldAndQuantity.getTotalSold();
	}
	
	/**
	 * Returns the quantity of all items
	 * 
	 * @return 
	 * 		The quantity
	 */
	public int getTotalQuantity () {
		return totalSoldAndQuantity.getTotalQuantity();
	}
	
	/**
	 * Returns a list with all buyers
	 * 
	 * @return 
	 * 		A List (Map) with all buyers
	 */
	public Map<String, TotalSoldAndQuantity> getBuyers() {
		return buyers;
	}

	
}
