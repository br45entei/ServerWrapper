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
public class FeatureNotImplementedDialog extends Dialog {
	
	protected Response	result	= Response.NO_RESPONSE;
	protected Shell		shell;
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public FeatureNotImplementedDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		setText("SWT Dialog");
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Object open(String msg) {
		createContents(msg);
		Functions.centerShell2OnShell1(getParent(), this.shell);
		if(!Main.isHeadless()) {
			this.shell.open();
			this.shell.layout();
			Display display = getParent().getDisplay();
			while(!this.shell.isDisposed()) {
				if(!display.readAndDispatch()) {
					this.shell.update();
					Functions.sleep(10L);//display.sleep();
				}
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
	private void createContents(String msg) {
		this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				FeatureNotImplementedDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.setImage(SWTResourceManager.getImage(FeatureNotImplementedDialog.class, "/assets/textures/icons/favicon.ico"));
		this.shell.setSize(450, 125);
		this.shell.setText("Feature Not Implemented");
		
		Label lblTheFeature = new Label(this.shell, SWT.WRAP);
		lblTheFeature.setBounds(64, 10, 370, 48);
		lblTheFeature.setText("The feature \"" + msg + "\" has not been implemented yet. Please check back in a newer release!");
		
		Button btnDone = new Button(this.shell, SWT.NONE);
		btnDone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FeatureNotImplementedDialog.this.result = Response.DONE;
			}
		});
		btnDone.setBounds(10, 67, 424, 23);
		btnDone.setText("Done");
		
		Label lblImage = new Label(this.shell, SWT.NONE);
		lblImage.setImage(SWTResourceManager.getImage(FeatureNotImplementedDialog.class, "/assets/textures/icons/warning.ico"));
		lblImage.setBounds(10, 10, 48, 48);
		
	}
}
