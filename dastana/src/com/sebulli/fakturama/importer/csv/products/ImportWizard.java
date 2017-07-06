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

package com.sebulli.fakturama.importer.csv.products;

import static com.sebulli.fakturama.Translate._;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.sebulli.fakturama.importer.ImportOptionPage;
import com.sebulli.fakturama.importer.ImportProgressDialog;
import com.sebulli.fakturama.views.datasettable.ViewDataSetTable;
import com.sebulli.fakturama.views.datasettable.ViewProductTable;
import com.sebulli.fakturama.views.datasettable.ViewVatTable;

/**
 * A wizard to import tables in CSV file format
 * 
 * @author Gerd Bartelt
 */
public class ImportWizard extends Wizard implements IImportWizard {

	// The wizard pages
	ImportOptionPage optionPage;

	// The selected file to import
	String selectedFile = "";

	/**
	 * Constructor
	 * 
	 * Creates a new wizard with one page
	 */
	public ImportWizard() {
		//T: Title of the CSV Import wizard
		setWindowTitle(_("Import CSV"));
		//T: Import product wizard
		optionPage = new ImportOptionPage(_("Import Products"),
				//T: Import product wizard
				_("Set some import options."),
				"/icons/preview/products2.png");

		optionPage.setPageComplete(true);
		addPage(optionPage);
		setNeedsProgressMonitor(true);
	}

	/**
	 * Performs any actions appropriate in response to the user having pressed
	 * the Finish button
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {

		FileDialog fileDialog = new FileDialog(this.getShell());
		//fileDialog.setFilterPath("/");
		fileDialog.setFilterExtensions(new String[] { "*.csv" });

		// Start at the user's home
		String userLocation;
		userLocation = Platform.getUserLocation().getURL().getPath();
		
		fileDialog.setFilterPath(userLocation);
		
		//T: CSV Import File Dialog Title
		fileDialog.setText(_("Select file to import"));

		//T: CSV Import File Filter
		fileDialog.setFilterNames(new String[] { _("Table as CSV")+ " (*.csv)" });
		selectedFile = fileDialog.open();
		if (selectedFile != null) {

			// Import the selected file
			if (!selectedFile.isEmpty()) {

				Importer csvImporter = new Importer();
				csvImporter.importCSV(selectedFile, false,optionPage.getUpdateExisting(), optionPage.getUpdateWithEmptyValues());

				ImportProgressDialog dialog= new ImportProgressDialog(this.getShell());
				dialog.setStatusText(csvImporter.getResult());

				// Find the voucher table view
				ViewDataSetTable view = (ViewDataSetTable) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.findView(ViewProductTable.ID);

				// Refresh it
				if (view != null)
					view.refresh();

				// Find the VAT table view
				view = (ViewDataSetTable) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ViewVatTable.ID);

				// Refresh it
				if (view != null)
					view.refresh();
				
				return (dialog.open() == ImportProgressDialog.OK);

			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}


}
