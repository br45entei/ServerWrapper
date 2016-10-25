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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class ConfirmHideWindowDialog extends Dialog {
	
	protected Response	result	= Response.NO_RESPONSE;
	protected Shell		shell;
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public ConfirmHideWindowDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		setText("SWT Dialog");
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
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				e.doit = e.character != SWT.ESC;
			}
		});
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				ConfirmHideWindowDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(387, 118);
		this.shell.setText("Confirm Hide Window - " + Main.getDefaultShellTitle());
		this.shell.setImages(Main.getDefaultImages());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		Label lblAreYouSure = new Label(this.shell, SWT.WRAP);
		lblAreYouSure.setBounds(10, 10, 360, 42);
		lblAreYouSure.setText("Are you sure you wish to hide the window? To restore the window, right click on the icon located in the system tray and click 'Restore Window'.");
		
		Button btnYes = new Button(this.shell, SWT.NONE);
		btnYes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConfirmHideWindowDialog.this.result = Response.YES;
			}
		});
		btnYes.setBounds(10, 58, 177, 23);
		btnYes.setText("Yes");
		
		Button btnNo = new Button(this.shell, SWT.NONE);
		btnNo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConfirmHideWindowDialog.this.result = Response.NO;
			}
		});
		btnNo.setBounds(193, 58, 177, 23);
		btnNo.setText("No");
	}
}
