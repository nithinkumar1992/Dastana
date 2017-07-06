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

package com.sebulli.fakturama.actions;

import static com.sebulli.fakturama.Translate._;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.sebulli.fakturama.Activator;
import com.sebulli.fakturama.ApplicationWorkbenchAdvisor;
import com.sebulli.fakturama.ApplicationWorkbenchWindowAdvisor;
import com.sebulli.fakturama.data.DataBaseConnectionState;
import com.sebulli.fakturama.data.DataSetDocument;
import com.sebulli.fakturama.logger.Logger;
import com.sebulli.fakturama.misc.DocumentType;
import com.sebulli.fakturama.views.ViewManager;
import com.sebulli.fakturama.views.datasettable.ViewContactTable;
import com.sebulli.fakturama.views.datasettable.ViewDocumentTable;
import com.sebulli.fakturama.views.datasettable.ViewPaymentTable;
import com.sebulli.fakturama.views.datasettable.ViewProductTable;
import com.sebulli.fakturama.views.datasettable.ViewShippingTable;
import com.sebulli.fakturama.views.datasettable.ViewVatTable;
import com.sebulli.fakturama.webshopimport.WebShopImportManager;

/**
 * This action opens the documents in a table view.
 * 
 * @author Gerd Bartelt
 */
public class WebShopImportAction extends Action {

	//T: Text of the action to connect to the web shop and import new data
	public final static String ACTIONTEXT = _("Web Shop"); 

	/**
	 * Constructor
	 */
	public WebShopImportAction() {

		super(ACTIONTEXT);

		//T: Tool Tip Text
		setToolTipText(_("Get new orders and products from web shop") );

		// The id is used to refer to the action in a menu or toolbar
		setId(ICommandIds.CMD_WEBSHOP_IMPORT);

		// Associate the action with a pre-defined command, to allow key
		// bindings.
		setActionDefinitionId(ICommandIds.CMD_WEBSHOP_IMPORT);

		// sets a default 16x16 pixel icon.
		setImageDescriptor(com.sebulli.fakturama.Activator.getImageDescriptor("/icons/16/shop_16.png"));
	}

	/**
	 * Run the action
	 * 
	 * Open the web shop import manager.
	 */
	@Override
	public void run() {

		// cancel, if the data base is not connected.
		if (!DataBaseConnectionState.INSTANCE.isConnected())
			return;

		// cancel, if the webshop is disabled.
		if (!Activator.getDefault().getPreferenceStore().getBoolean("WEBSHOP_ENABLED"))
			return;
		
		// Start a new web shop import manager in a
		// progress Monitor Dialog
		WebShopImportManager webShopImportManager = new WebShopImportManager();
		webShopImportManager.prepareGetProductsAndOrders();
		IWorkbenchWindow workbenchWindow = ApplicationWorkbenchWindowAdvisor.getActiveWorkbenchWindow();
		try {
			new ProgressMonitorDialog(workbenchWindow.getShell()).run(true, true, webShopImportManager);

			// If there is no error - interpret the data.
			if (!webShopImportManager.getRunResult().isEmpty()) {
				// If there is an error - display it in a message box
				MessageBox messageBox = new MessageBox(ApplicationWorkbenchWindowAdvisor.getActiveWorkbenchWindow().getShell(), SWT.ICON_ERROR);
				messageBox.setText(_("Error importing data from web shop"));
				String errorMessage = webShopImportManager.getRunResult();
				if (errorMessage.length() > 400)
					errorMessage = errorMessage.substring(0, 400) + "...";
				messageBox.setMessage(errorMessage);
				messageBox.open();
			}
		}
		catch (InvocationTargetException e) {
			Logger.logError(e, "Error running web shop import manager.");
		}
		catch (InterruptedException e) {
			Logger.logError(e, "Web shop import manager was interrupted.");
		}

		// Refresh the views
		ApplicationWorkbenchAdvisor.refreshView(ViewProductTable.ID);
		ApplicationWorkbenchAdvisor.refreshView(ViewContactTable.ID);
		ApplicationWorkbenchAdvisor.refreshView(ViewPaymentTable.ID);
		ApplicationWorkbenchAdvisor.refreshView(ViewShippingTable.ID);
		ApplicationWorkbenchAdvisor.refreshView(ViewVatTable.ID);

		// After the web shop import, open the document view
		// and set the focus to the new imported orders.
		ViewManager.showView(ViewDocumentTable.ID);
		IViewPart view = ApplicationWorkbenchWindowAdvisor.getActiveWorkbenchWindow().getActivePage().findView(ViewDocumentTable.ID);
		ViewDocumentTable viewDocumentTable = (ViewDocumentTable) view;
		viewDocumentTable.getTopicTreeViewer().selectItemByName(DocumentType.ORDER.getPluralString() + "/" + DataSetDocument.getStringNOTSHIPPED());
	}
}
