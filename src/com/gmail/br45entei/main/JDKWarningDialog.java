/**
 * 
 */
package com.gmail.br45entei.main;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class JDKWarningDialog extends Dialog {
	
	protected Response	response	= Response.NO_RESPONSE;
	protected Shell		shell;
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public JDKWarningDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Object open() {
		createContents();
		this.shell.open();
		this.shell.layout();
		Display display = getParent().getDisplay();
		while(this.response == Response.NO_RESPONSE) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		return this.response;
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(this.getParent(), this.getStyle());
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				JDKWarningDialog.this.response = Response.CLOSE;
			}
		});
		this.shell.setSize(345, 165);
		this.shell.setText("Java JDK Warning");
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		Label lblImage = new Label(this.shell, SWT.NONE);
		lblImage.setImage(SWTResourceManager.getImage(JDKWarningDialog.class, "/assets/textures/icons/warning.ico"));
		lblImage.setBounds(10, 10, 48, 85);
		
		Label lblMsg = new Label(this.shell, SWT.WRAP);
		lblMsg.setBounds(64, 10, 265, 85);
		lblMsg.setText("You appear to be using a Java Development Kit(JDK) instead of a Java Runtime Environment(JRE) to start the server. Since the server may not start correctly, it is recommended that you switch to a JRE, such as jre1.8.0_25.");
		
		Button btnOk = new Button(this.shell, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				JDKWarningDialog.this.response = Response.OK;
			}
		});
		btnOk.setBounds(10, 101, 319, 25);
		btnOk.setText("Ok");
		
	}
}
