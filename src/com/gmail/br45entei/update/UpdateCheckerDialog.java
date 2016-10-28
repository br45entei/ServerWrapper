package com.gmail.br45entei.update;

import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;
import com.gmail.br45entei.update.UpdateChecker.UpdateResult;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class UpdateCheckerDialog extends Dialog {
	
	protected Response				result		= Response.NO_RESPONSE;
	protected Shell					shell;
	public final Property<Double>	progress	= new Property<>("Progress");
	private ProgressBar				progressBar;
	private Label					lblUploadingFile;
	public final Property<Boolean>	isConnected	= new Property<>("Connected", Boolean.FALSE);
	
	/** Create the dialog. */
	public UpdateCheckerDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
		this.createContents();
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public UpdateResult open() {
		this.shell.open();
		this.shell.layout();
		UpdateResult result = UpdateChecker.updateAvailable(this);
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		return result;
	}
	
	public final void runLoop() {
		Main.mainLoop();
		this.updateUI();
	}
	
	private final void updateUI() {
		int selection = (int) Math.round(this.progress.getValue().doubleValue() * 100.00D);
		if(this.progressBar.getSelection() != selection) {
			this.progressBar.setSelection(selection);
		}
		Functions.setShellImages(this.shell, Main.getShellImages());
		Functions.setTextFor(this.shell, "Update check progress - " + this.getParent().getText());
		if(this.isConnected.getValue().booleanValue() && this.lblUploadingFile.getText().equals("Connecting to update server...")) {
			Functions.setTextFor(this.lblUploadingFile, "Checking latest version progress:");
		}
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(this.getParent(), SWT.BORDER | SWT.TITLE);
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.character == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		this.shell.setText("Update check progress - " + this.getParent().getText());
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				UpdateCheckerDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(420, 89);
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		this.shell.setImages(Main.getShellImages());
		
		this.lblUploadingFile = new Label(this.shell, SWT.WRAP);
		this.lblUploadingFile.setBounds(10, 10, 394, 20);
		this.lblUploadingFile.setText("Connecting to update server...");
		
		this.progressBar = new ProgressBar(this.shell, SWT.NONE);
		this.progressBar.setBounds(10, 36, 394, 18);
	}
	
	public final void close() {
		this.result = Response.CLOSE;
	}
	
}
