package com.gmail.br45entei.update;

import com.gmail.br45entei.data.DisposableByteArrayInputStream;
import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.main.PopupDialog;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;
import com.gmail.br45entei.util.FileTransfer;
import com.gmail.br45entei.util.FileTransfer.FileData;
import com.gmail.br45entei.util.JavaProgramArguments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
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
	
	protected static volatile boolean	testFailedJarOverwrite	= false;
	
	protected Response					result					= Response.NO_RESPONSE;
	protected Shell						shell;
	public final Property<Double>		progress				= new Property<>("Progress");
	private ProgressBar					progressBar;
	private Label						lblUploadingFile;
	
	/** Create the dialog. */
	public PerformUpdateDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
		this.createContents();
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public final boolean open() {
		final Property<Boolean> restoredJarFile = new Property<>("RestoredJarFile", Boolean.FALSE);
		this.shell.open();
		this.shell.layout();
		FileData data = UpdateChecker.downloadJar(this);
		if(data.getSize() > 0) {
			final File check = JavaProgramArguments.getClassPathJarFile();
			if(!check.getName().toLowerCase().endsWith(".class")) {
				final String runCmd = Main.getSysArgs().runtimeCommand;
				Main.saveSettings();
				@SuppressWarnings("resource")
				final DisposableByteArrayInputStream fis = new DisposableByteArrayInputStream(data.data);
				final String jarName = check.getName();
				this.lblUploadingFile.setText("Overwriting " + jarName + ";\r\nIf the server wrapper does not re-open upon exiting, please restart it manually.");
				this.runLoop();
				FileData getBackup;
				try {
					getBackup = FileTransfer.readFile(check);
				} catch(IOException e) {
					e.printStackTrace();
					getBackup = null;
				}
				new PopupDialog(getParent(), "", "").toString();//go ahead and load these classes so we can use them when things get weird below(invalid classloader data pointers due to corrupt/changed jar file):
				new DisposableByteArrayInputStream(new byte[0]).close();
				final Runtime runtime = Runtime.getRuntime();
				final FileData backup = getBackup;
				final Property<Throwable> exception = new Property<>("Thrown Error");
				Thread updateThread = new Thread(new Runnable() {
					@Override
					public final void run() {
						try(FileOutputStream out = new FileOutputStream(check)) {
							byte[] b = new byte[4096];
							int len;
							while((len = fis.read(b)) >= 0) {
								out.write(b, 0, len);
							}
							out.flush();
							try {
								runtime.exec(testFailedJarOverwrite ? "" : runCmd);
								fis.close();
							} catch(IOException | IllegalArgumentException ignored) {
							}
							if(!testFailedJarOverwrite) {
								System.exit(0);
							} else {
								throw new Throwable("Test message!");
							}
						} catch(Throwable e) {//Let's attempt to restore the jarfile/exe so the class loader's data pointer thingys will become valid again and we can continue chugging along...
							e.printStackTrace();
							exception.setValue(e);
							if(backup != null) {
								try(FileOutputStream out = new FileOutputStream(check)) {
									out.write(backup.data);
									out.flush();
									restoredJarFile.setValue(Boolean.TRUE);
								} catch(Throwable ignored) {
								}
							}
						}
					}
				});
				updateThread.setDaemon(false);
				updateThread.start();
				if(!updateThread.isAlive()) {
					while(!updateThread.isAlive()) {
						runLoop();
						Functions.sleep(10);
					}
				}
				while(updateThread.isAlive()) {
					runLoop();
					Functions.sleep(10);
				}
				//We're still here?!
				final Throwable e = exception.getValue();
				final String warning = (restoredJarFile.getValue().booleanValue() ? "The original data was restored, but you may need to close and re-open " + jarName + "(this program should continue to run normally, but may fail due to it's file contents having been changed back and forth)." : "You may need to re-download " + jarName + ".");
				if(e != null) {
					new PopupDialog(this.getParent(), "Failed to update ServerWrapper", "An error occurred whilst overwriting " + jarName + ":\r\n" + e.getClass().getName() + ": " + e.getMessage() + "\r\n\r\n" + warning).open();
				} else {
					new PopupDialog(this.getParent(), "Failed to update ServerWrapper", "An unknown error occurred whilst overwriting " + jarName + "!\r\n" + warning).open();
				}
			}
		} else {
			new PopupDialog(this.getParent(), "Failed to download update", "Downloaded file size is 0 bytes?!").open();
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		return restoredJarFile.getValue() == null ? false : restoredJarFile.getValue().booleanValue();
	}
	
	/** @throws ClassNotFoundException not unused */
	private final void runLoop_() throws ClassNotFoundException {
		try {
			Main.mainLoop();
			this.updateUI();
		} catch(Throwable e) {
			if(e instanceof SWTException || e instanceof SWTError) {
				e.printStackTrace();
			}
		}
	}
	
	public final void runLoop() {
		try {
			this.runLoop_();
		} catch(ClassNotFoundException ignored) {
		}
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
