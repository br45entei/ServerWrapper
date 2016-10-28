/**
 * 
 */
package com.gmail.br45entei.update;

import com.gmail.br45entei.main.Main;
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

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class PromptDownloadUpdateDialog extends Dialog {
	private static volatile boolean	runFromMain	= true;
	
	protected Response				result		= Response.NO_RESPONSE;
	protected Shell					shell;
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public PromptDownloadUpdateDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		setText("SWT Dialog");
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open(String updateDetails) {
		createContents(updateDetails);
		this.shell.open();
		this.shell.layout();
		Display display = getParent().getDisplay();
		while(!this.shell.isDisposed()) {
			if(runFromMain) {
				Main.mainLoop();
			} else {
				if(!display.readAndDispatch()) {
					this.shell.update();
					Functions.sleep(10);//display.sleep();
				}
			}
			if(this.result != Response.NO_RESPONSE) {
				break;
			}
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		return this.result;
	}
	
	/** Create contents of the dialog. */
	private void createContents(String updateDetails) {
		this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		/*this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				e.doit = e.character != SWT.ESC;
			}
		});*/
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				PromptDownloadUpdateDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(387, 172);
		this.shell.setText("Confirm delete local files - " + Main.getShellTitle());
		this.shell.setImages(Main.getShellImages());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		Label lblAreYouSure = new Label(this.shell, SWT.WRAP);
		lblAreYouSure.setBounds(10, 10, 360, 96);
		lblAreYouSure.setText(updateDetails + "\r\nWould you like to download this update now?\r\n\r\nYes: Download the new '" + UpdateChecker.getJarFileName() + "' file and replace this one, then restart.\r\nNo / Cancel(escape/close): Do nothing.");
		
		Button btnYes = new Button(this.shell, SWT.NONE);
		btnYes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PromptDownloadUpdateDialog.this.result = Response.YES;
			}
		});
		btnYes.setBounds(10, 112, 177, 23);
		btnYes.setText("Yes");
		
		Button btnNo = new Button(this.shell, SWT.NONE);
		btnNo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PromptDownloadUpdateDialog.this.result = Response.NO;
			}
		});
		btnNo.setBounds(193, 112, 177, 23);
		btnNo.setText("No");
	}
	
}
