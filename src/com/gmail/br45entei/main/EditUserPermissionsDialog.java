package com.gmail.br45entei.main;

import com.gmail.br45entei.main.CredentialsManager.UserPermissions;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class EditUserPermissionsDialog extends Dialog {
	private static volatile boolean	runFromMain	= true;
	
	protected Response				result		= Response.NO_RESPONSE;
	protected Shell					shell;
	protected Button				btnAllowconsoleaccess;
	protected Button				btnCanRestartServer;
	protected Button				btnCanStopServer;
	protected Button				btnCanModifyFiles;
	protected Button				btnCanDeleteFiles;
	protected Button				btnCanDownloadFiles;
	
	public final UserPermissions	permissions;
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public EditUserPermissionsDialog(Shell parent, UserPermissions permissions) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.PRIMARY_MODAL);
		this.setText("SWT Dialog");
		this.permissions = permissions;
		this.createContents();
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open() {
		if(!Main.isHeadless()) {
			this.shell.open();
			this.shell.layout();
			Display display = getParent().getDisplay();
			while(!this.shell.isDisposed()) {
				if(runFromMain) {
					Main.mainLoop();
					this.updateUI();
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
		}
		return this.result;
	}
	
	private final void updateUI() {
		Functions.setSelectionFor(this.btnAllowconsoleaccess, this.permissions.allowConsoleAccess);
		Functions.setSelectionFor(this.btnCanRestartServer, this.permissions.canRestartServer);
		Functions.setSelectionFor(this.btnCanStopServer, this.permissions.canStopServer);
		Functions.setSelectionFor(this.btnCanModifyFiles, this.permissions.canModifyFiles);
		Functions.setSelectionFor(this.btnCanDeleteFiles, this.permissions.canDeleteFiles);
		Functions.setSelectionFor(this.btnCanDownloadFiles, this.permissions.canDownloadFiles);
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(getParent(), SWT.CLOSE | SWT.TITLE | SWT.PRIMARY_MODAL);
		this.shell.setSize(283, 137);
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.character == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		this.shell.setText("User Permissions");
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				EditUserPermissionsDialog.this.result = Response.CLOSE;
			}
		});
		Functions.centerShell2OnShell1(getParent(), this.shell);
		this.shell.setImages(Main.getShellImages());
		
		this.btnAllowconsoleaccess = new Button(this.shell, SWT.CHECK);
		this.btnAllowconsoleaccess.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				e.doit = false;
				EditUserPermissionsDialog.this.permissions.allowConsoleAccess = !EditUserPermissionsDialog.this.permissions.allowConsoleAccess;
			}
		});
		this.btnAllowconsoleaccess.setBounds(10, 10, 125, 16);
		this.btnAllowconsoleaccess.setText("Allow console access");
		
		this.btnCanRestartServer = new Button(this.shell, SWT.CHECK);
		this.btnCanRestartServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				e.doit = false;
				EditUserPermissionsDialog.this.permissions.canRestartServer = !EditUserPermissionsDialog.this.permissions.canRestartServer;
			}
		});
		this.btnCanRestartServer.setBounds(10, 32, 125, 16);
		this.btnCanRestartServer.setText("Can restart server");
		
		this.btnCanModifyFiles = new Button(this.shell, SWT.CHECK);
		this.btnCanModifyFiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				e.doit = false;
				EditUserPermissionsDialog.this.permissions.canModifyFiles = !EditUserPermissionsDialog.this.permissions.canModifyFiles;
			}
		});
		this.btnCanModifyFiles.setBounds(141, 10, 125, 16);
		this.btnCanModifyFiles.setText("Can modify files");
		
		this.btnCanDeleteFiles = new Button(this.shell, SWT.CHECK);
		this.btnCanDeleteFiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				e.doit = false;
				EditUserPermissionsDialog.this.permissions.canDeleteFiles = !EditUserPermissionsDialog.this.permissions.canDeleteFiles;
			}
		});
		this.btnCanDeleteFiles.setBounds(141, 32, 125, 16);
		this.btnCanDeleteFiles.setText("Can delete files");
		
		this.btnCanDownloadFiles = new Button(this.shell, SWT.CHECK);
		this.btnCanDownloadFiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				e.doit = false;
				EditUserPermissionsDialog.this.permissions.canDownloadFiles = !EditUserPermissionsDialog.this.permissions.canDownloadFiles;
			}
		});
		this.btnCanDownloadFiles.setBounds(141, 54, 125, 16);
		this.btnCanDownloadFiles.setText("Can download files");
		
		this.btnCanStopServer = new Button(this.shell, SWT.CHECK);
		this.btnCanStopServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				e.doit = false;
				EditUserPermissionsDialog.this.permissions.canStopServer = !EditUserPermissionsDialog.this.permissions.canStopServer;
			}
		});
		this.btnCanStopServer.setBounds(10, 54, 125, 16);
		this.btnCanStopServer.setText("Can stop server");
		
		Button btnDone = new Button(this.shell, SWT.NONE);
		btnDone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EditUserPermissionsDialog.this.result = Response.DONE;
			}
		});
		btnDone.setBounds(10, 76, 256, 23);
		btnDone.setText("Done");
		
	}
	
}
