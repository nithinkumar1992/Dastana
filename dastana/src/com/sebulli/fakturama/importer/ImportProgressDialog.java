package com.sebulli.fakturama.importer;

import static com.sebulli.fakturama.Translate._;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to show the import progress
 * 
 * @author Gerd Bartelt
 *
 */
public class ImportProgressDialog extends Dialog {

	// The status text
	private Text statusText;
	private String statusTextString = "";

	/**
	 * Default constructor
	 * 
	 * @param parentShell
	 */
	public ImportProgressDialog(Shell parentShell) {
		super(parentShell);
	}
	
    @Override
    protected Control createDialogArea(Composite parent) {
		// Create the top composite
		Composite top = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(top);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(top);
		
		// Create the label with the help text
		Label labelDescription = new Label(top, SWT.NONE);
		//T: Import progress of the CSV import
		labelDescription.setText(_("Import Progress:"));
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(labelDescription);

		// Create the label with the status text
		statusText = new Text(top, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(statusText);
		statusText.setText(statusTextString);
        
        return top;
    }


    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
     */
    @Override
    protected Point getInitialSize() {
        return new Point(700, 500);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Dialog");
    }
    
	/**
	 * Displays the status of the import in the status text field
	 * 
	 * @param text
	 *            The status text to display
	 */
	public void setStatusText(String text) {
		statusTextString = text;
		if (statusText != null)
			statusText.setText(statusTextString);
	}


}
