/**
 * 
 */
package com.gmail.br45entei.update;

import com.gmail.br45entei.data.DisposableByteArrayInputStream;
import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.main.PopupDialog;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;
import com.gmail.br45entei.util.FileTransfer.FileData;
import com.gmail.br45entei.util.JavaProgramArguments;

import java.io.File;
import java.io.FileOutputStream;

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
public class PerformUpdateDialog extends Dialog {
	
	protected Response				result		= Response.NO_RESPONSE;
	protected Shell					shell;
	public final Property<Double>	progress	= new Property<>("Progress");
	private ProgressBar				progressBar;
	private Label					lblUploadingFile;
	
	/** Create the dialog. */
	public PerformUpdateDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
		this.createContents();
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public final void open() {
		this.shell.open();
		this.shell.layout();
		FileData data = UpdateChecker.downloadJar(this);
		if(data.getSize() > 0) {
			File check = JavaProgramArguments.getClassPathJarFile();
			if(!check.getName().toLowerCase().endsWith(".class")) {
				final String runCmd = Main.getSysArgs().runtimeCommand;
				Main.saveSettings();
				@SuppressWarnings("resource")
				final DisposableByteArrayInputStream fis = new DisposableByteArrayInputStream(data.data);
				final Runtime runtime = Runtime.getRuntime();
				this.lblUploadingFile.setText("Overwriting " + check.getName() + ";\r\nIf the server wrapper does not re-open upon exiting, please restart it manually.");
				this.runLoop();
				try(FileOutputStream out = new FileOutputStream(check)) {
					byte[] b = new byte[4096];
					int len;
					while((len = fis.read(b)) >= 0) {
						out.write(b, 0, len);
					}
					runtime.exec(runCmd);
					out.flush();
					fis.close();
				} catch(Throwable e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		} else {
			new PopupDialog(getParent(), "Failed to download update", "Downloaded file size is 0 bytes?!").open();
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
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
				PerformUpdateDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(420, 112);
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		this.shell.setImages(Main.getShellImages());
		
		this.lblUploadingFile = new Label(this.shell, SWT.WRAP);
		this.lblUploadingFile.setBounds(10, 10, 394, 43);
		this.lblUploadingFile.setText("Downloading update, please wait...");
		
		this.progressBar = new ProgressBar(this.shell, SWT.NONE);
		this.progressBar.setBounds(10, 59, 394, 18);
	}
	
	public final void close() {
		this.result = Response.CLOSE;
	}
	
}
