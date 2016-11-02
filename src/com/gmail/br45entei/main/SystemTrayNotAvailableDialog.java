package com.gmail.br45entei.main;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class SystemTrayNotAvailableDialog extends Dialog {
	
	protected Response	result	= Response.NO_RESPONSE;
	protected Shell		shell;
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public SystemTrayNotAvailableDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		setText("Unable to hide window - " + Main.getDefaultShellTitle());
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open() {
		createContents();
		if(!Main.isHeadless()) {
			this.shell.open();
			this.shell.layout();
			while(!this.shell.isDisposed()) {
				Main.mainLoop();
				if(this.result != Response.NO_RESPONSE) {
					break;
				}
			}
			if(!this.shell.isDisposed()) {
				this.shell.dispose();
			}
		}
		return this.result;
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		this.shell.setSize(386, 116);
		this.shell.setText("Unable to hide window - " + Main.getDefaultShellTitle());
		this.shell.setImages(Main.getDefaultImages());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		Label lblTheSystemTray = new Label(this.shell, SWT.WRAP);
		lblTheSystemTray.setBounds(10, 10, 360, 42);
		lblTheSystemTray.setText("The system tray is unavailable or not present, therefore the main window cannot be hidden.");
		
		Button btnOk = new Button(this.shell, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SystemTrayNotAvailableDialog.this.result = Response.OK;
			}
		});
		btnOk.setBounds(10, 58, 360, 23);
		btnOk.setText("Ok");
	}
	
}
