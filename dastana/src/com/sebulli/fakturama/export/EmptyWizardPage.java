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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.sebulli.fakturama.logger.Logger;

/**
 * Create the first (and only) page of the sales export wizard. This page is
 * used to select the start and end date.
 * 
 * @author Gerd Bartelt
 */
public class EmptyWizardPage extends WizardPage {

	private ImageDescriptor previewImagePath = null;
	
	/**
	 * Constructor Create the page and set title and message.
	 */
	public EmptyWizardPage(String title, String message) {
		super("Wizard Page");
		//T: Title of the Sales Export Wizard Page 1
		setTitle(title);
		//T: Text of the Sales Export Wizard Page 1
		setMessage( message );
	}
	/**
	 * Constructor Create the page and set title, message and preview image
	 */
	public EmptyWizardPage(String title, String message, ImageDescriptor previewImagePath) {
		super("Wizard Page");
		//T: Title of the Sales Export Wizard Page 1
		setTitle(title);
		//T: Text of the Sales Export Wizard Page 1
		setMessage( message );
		this.previewImagePath = previewImagePath;
	}

	/**
	 * Creates the top level control for this dialog page under the given parent
	 * composite.
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		// Create the top composite
		Composite top = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(top);
		
		// Display a preview image, if it is not empty
		if (previewImagePath != null) {
			// Preview image
			Label preview = new Label(top, SWT.NONE);
			preview.setText(_("preview"));
			GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).applyTo(preview);
			try {
				preview.setImage(previewImagePath.createImage());
			}
			catch (Exception e) {
				Logger.logError(e, "Icon not found");
			}
			
		}

		setControl(top);

	}

}
