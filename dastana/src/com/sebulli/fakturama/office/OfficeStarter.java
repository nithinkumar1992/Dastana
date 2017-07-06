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

package com.sebulli.fakturama.office;

import static com.sebulli.fakturama.Translate._;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;

import ag.ion.bion.officelayer.application.IOfficeApplication;
import ag.ion.bion.officelayer.application.OfficeApplicationException;
import ag.ion.bion.officelayer.application.OfficeApplicationRuntime;

import com.sebulli.fakturama.Activator;
import com.sebulli.fakturama.OSDependent;
import com.sebulli.fakturama.Workspace;
import com.sebulli.fakturama.logger.Logger;

/**
 * Starts the OpenOffice Application from the application's path
 * 
 * @author Gerd Bartelt
 */
public class OfficeStarter {
	
	/**
	 * Returns, if the Application exists
	 * 
	 * @param preferencePath
	 *            The path from the preference store
	 * @return TRUE, if it exists.
	 */
	static public boolean isValidPath(String preferencePath) {
		File file = new File(OSDependent.getOOBinary(preferencePath));
		return file.isFile();
	}

	/**
	 * Get the path of the OpenOffice installation 
	 * 
	 * @return
	 */
	static public String getHome () {
		
		// Return an empty string, if no OpenOffice was found
		String home = "";
		
		OfficeHomeApplication r = new OfficeHomeApplication();
		
		// Start the search in a new thread
		Thread thread = new Thread(r);
		thread.start();

		synchronized( OfficeStarter.class )
		{
			try {
				// Search maximum 10 seconds
				for (int i = 0; i < 100 && (r.getHome() == null); i++ ) {
					OfficeStarter.class.wait(100);
				}
			}
			catch (InterruptedException e) {
			}
		}
		
		// Get the home folder, and at least an empty string
		home = r.getHome();
		if (home == null)
			home ="";
		
		return home;
	}

	
	/**
	 * Opens the OpenOffice application
	 * 
	 * @return Reference to the OpenOffice application object
	 */
	static public IOfficeApplication openOfficeApplication() {

		// Get the path to the application set in the preference store
		String preferencePath = Activator.getDefault().getPreferenceStore().getString("OPENOFFICE_PATH");

		// Show a message (and exit), if there is no OpenOffice found
		if (!isValidPath(preferencePath)) {
			Workspace.showMessageBox(SWT.ICON_WARNING | SWT.OK,
					//T: Title of the Message Box that appears if the OpenOffice path is invalid.
					_("Error"), 
					//T: Text of the Message Box that appears if the OpenOffice path is invalid.
					//T: Format: OpenOffice path ... is invalid.
					_("OpenOffice-Path:") + "\n\n" + preferencePath + "\n\n"+
							//T: Text of the Message Box that appears if the OpenOffice path is invalid.
							//T: Format: OpenOffice path ... is invalid.
							_("is invalid"));
			return null;
		}

		// Activate the OpenOffice Application
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(IOfficeApplication.APPLICATION_HOME_KEY, preferencePath);
		configuration.put(IOfficeApplication.APPLICATION_TYPE_KEY, IOfficeApplication.LOCAL_APPLICATION);
		
		if (preferencePath.toLowerCase().contains("libreoffice")) {
			configuration.put("arguments",
	                   new String[] {"--nologo",
	                   "--nofirststartwizard",
	                   "--nodefault",
	                   "--norestore",
	                   "--nolockcheck"
	                   });
		}
		
		
		/*
	      configuration.put(IOfficeApplication.APPLICATION_TYPE_KEY,
	              IOfficeApplication.REMOTE_APPLICATION);
	          configuration.put(IOfficeApplication.APPLICATION_HOST_KEY, "host");
	          configuration.put(IOfficeApplication.APPLICATION_PORT_KEY, "8100");
*/
		IOfficeApplication officeApplication = null;
		try {

			// Get the application
			officeApplication = OfficeApplicationRuntime.getApplication(configuration);

			// Configure it
			try {
				officeApplication.setConfiguration(configuration);
			}
			catch (OfficeApplicationException e) {
				Logger.logError(e, "Error configuring OpenOffice");
			}

			// And activate it
			try {
				officeApplication.activate();
			}
			catch (OfficeApplicationException e) {
				Logger.logError(e, "Error activating OpenOffice");
			}
		}
		catch (OfficeApplicationException e) {
			Logger.logError(e, "Error starting OpenOffice");
		}

		//Return the Application
		return officeApplication;
	}

}
