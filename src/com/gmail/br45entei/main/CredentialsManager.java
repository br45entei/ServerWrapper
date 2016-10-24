package com.gmail.br45entei.main;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;
import com.gmail.br45entei.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

import org.apache.commons.io.FileDeleteStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class CredentialsManager extends Dialog {
	
	protected Response									result		= Response.NO_RESPONSE;
	
	protected Display									display;
	protected Shell										shell;
	private Button										btnDone;
	private ScrolledComposite							scrolledComposite;
	private Composite									content;
	private CreateNewCredentialComposite				createCredential;
	protected final ConcurrentLinkedDeque<Credential>	credentials	= new ConcurrentLinkedDeque<>();
	
	public final Credential getUser(String username) {
		for(Credential user : this.credentials) {
			if(user.username.equalsIgnoreCase(username)) {
				return user;
			}
		}
		return null;
	}
	
	public static final class Credential {
		
		public static final Credential fullAccessUser = new Credential("", "", new UserPermissions(true));
		
		private static final String getFileNameFor(String username) {
			if(username != null) {
				int result = 0;
				String u = username.toLowerCase().trim();
				for(char c : u.toCharArray()) {
					int i = c * 31;
					result += i;
				}
				result += u.hashCode();
				return result + ".txt";
			}
			return null;
		}
		
		private static volatile File rootDir;
		
		public static final ArrayList<Credential> initialize(File rootFolder) {
			rootDir = new File(rootFolder, "Users");
			if(!rootDir.exists()) {
				rootDir.mkdirs();
				return null;
			}
			ArrayList<Credential> list = new ArrayList<>();
			for(String fileName : rootDir.list()) {
				if(fileName.endsWith(".txt")) {
					File file = new File(rootDir, fileName);
					Credential load = Credential.sLoadFromFile(file);
					if(load != null) {
						list.add(load);
					}
				}
			}
			return list;
		}
		
		public volatile String			username;
		public volatile String			password;
		
		public volatile UserPermissions	permissions;
		
		private Credential() {
		}
		
		public Credential(String username, String password, UserPermissions permissions) {
			this.username = username;
			this.password = password;
			this.permissions = permissions;
		}
		
		private final File getSaveFile() {
			return new File(rootDir, getFileNameFor(this.username));
		}
		
		public final boolean saveToFile() {
			File file = this.getSaveFile();
			try(PrintWriter pr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true)) {
				pr.println("username=" + this.username);
				pr.println("password=" + this.password);
				pr.println("allowConsoleAccess=" + this.permissions.allowConsoleAccess);
				pr.println("canRestartServer=" + this.permissions.canRestartServer);
				pr.println("canStopServer=" + this.permissions.canStopServer);
				pr.println("canModifyFiles=" + this.permissions.canModifyFiles);
				pr.println("canDeleteFiles=" + this.permissions.canDeleteFiles);
				pr.println("canDownloadFiles=" + this.permissions.canDownloadFiles);
				pr.println("");
				pr.flush();
			} catch(Throwable e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		public static final Credential sLoadFromFile(File file) {
			if(file == null || !file.exists()) {
				return null;
			}
			Credential user = new Credential();
			user.permissions = new UserPermissions();
			user.loadFromFile(file);
			return user;
		}
		
		public final boolean loadFromFile() {
			return this.loadFromFile(this.getSaveFile());
		}
		
		public final boolean loadFromFile(File file) {
			if(!file.exists()) {
				return false;
			}
			try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				final String regex = Pattern.quote("=");
				while(br.ready()) {
					String line = br.readLine();
					String[] split = line.split(regex);
					String param = split[0];
					String value = StringUtil.stringArrayToString(split, '=', 1);
					final boolean boolVal = value.equalsIgnoreCase("true");
					if(param.equalsIgnoreCase("username")) {
						this.username = value;
					} else if(param.equalsIgnoreCase("password")) {
						this.password = value;
					} else if(param.equalsIgnoreCase("allowConsoleAccess")) {
						this.permissions.allowConsoleAccess = boolVal;
					} else if(param.equalsIgnoreCase("canRestartServer")) {
						this.permissions.canRestartServer = boolVal;
					} else if(param.equalsIgnoreCase("canStopServer")) {
						this.permissions.canStopServer = boolVal;
					} else if(param.equalsIgnoreCase("canModifyFiles")) {
						this.permissions.canModifyFiles = boolVal;
					} else if(param.equalsIgnoreCase("canDeleteFiles")) {
						this.permissions.canDeleteFiles = boolVal;
					} else if(param.equalsIgnoreCase("canDownloadFiles")) {
						this.permissions.canDownloadFiles = boolVal;
					}
				}
			} catch(Throwable e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		public final boolean delete() {
			File file = this.getSaveFile();
			if(file.exists()) {
				FileDeleteStrategy.FORCE.deleteQuietly(file);
			}
			return !file.exists();
		}
		
	}
	
	/** Create the dialog.
	 * 
	 * @param parent The parent shell */
	public CredentialsManager(Shell parent, ArrayList<Credential> credentials) {
		super(parent, parent.getStyle());
		setText("Server Wrapper Credentials Manager");
		this.credentials.addAll(credentials);
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public ArrayList<Credential> open() {
		ArrayList<Credential> list = null;
		createContents();
		if(!Main.headless) {
			this.shell.open();
			this.shell.layout();
			this.display = getParent().getDisplay();
			while(!this.shell.isDisposed()) {
				if(this.result != Response.NO_RESPONSE) {
					break;
				}
				this.runClock();
			}
			if(this.result == Response.DONE) {
				list = new ArrayList<>(this.credentials);
			}
			if(!this.shell.isDisposed()) {
				this.shell.close();
			}
		}
		return list;
	}
	
	protected final void runClock() {
		if(this.display.isDisposed() || this.shell.isDisposed()) {
			return;
		}
		if(!this.display.readAndDispatch()) {
			this.updateUI();
			this.shell.update();
			Main.mainLoop();//Functions.sleep(10L);//display.sleep();
		}
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		this.shell.setSize(500, 380);
		this.shell.setText(this.getText());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		this.btnDone = new Button(this.shell, SWT.NONE);
		this.btnDone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CredentialsManager.this.result = Response.DONE;
			}
		});
		this.btnDone.setBounds(10, 316, 474, 25);
		this.btnDone.setText("Done");
		
		this.scrolledComposite = new ScrolledComposite(this.shell, SWT.BORDER | SWT.V_SCROLL);
		this.scrolledComposite.setExpandHorizontal(true);
		this.scrolledComposite.setBounds(10, 64, 474, 246);
		
		this.content = new Composite(this.scrolledComposite, SWT.NONE);
		this.content.setSize(0, CredentialComposite.yTotal * 2);
		
		//CredentialComposite test = new CredentialComposite(this.content);
		//test.setLocation(10, CredentialComposite.yInterval);
		
		this.createCredential = new CreateNewCredentialComposite(this.content, this);
		this.createCredential.setLocation(10, CredentialComposite.yTotal * 1);
		
		this.scrolledComposite.setContent(this.content);
		this.scrolledComposite.setMinSize(this.content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Label lblMsg = new Label(this.shell, SWT.WRAP);
		lblMsg.setBounds(10, 10, 474, 48);
		lblMsg.setText("Create or edit an existing credential to configure what username/password pairs can be used to connect to this server wrapper. If there are no credentials created, then any client may connect to this wrapper with a blank username and password. Click 'Done' to apply changes.");
		
	}
	
	private final CredentialComposite getCompositeForUser(Credential user) {
		for(Control control : this.content.getChildren()) {
			if(control instanceof CredentialComposite) {
				CredentialComposite creds = (CredentialComposite) control;
				if(creds.user == user) {//if(creds.username.getText().equals(user.username)) {// && creds.password.getText().equals(password)) {
					return creds;
				}
			}
		}
		CredentialComposite creds = new CredentialComposite(this.content, this, user);
		return creds;
	}
	
	private final void updateUI() {
		int i = 0;
		final String randomKey = StringUtil.nextSessionId();
		for(Credential user : this.credentials) {
			CredentialComposite cred = getCompositeForUser(user);
			cred.setLocation(10, Math.max(CredentialComposite.yInterval, CredentialComposite.yTotal * i));
			cred.randomKey = randomKey;
			if(!user.username.trim().equalsIgnoreCase(cred.username.getText().trim())) {
				user.delete();
			}
			String newUsername = cred.username.getText();
			String newPassword = cred.password.getText();
			if(!newUsername.trim().isEmpty()) {
				user.username = newUsername;
			} else {
				cred.username.setText(user.username);
				cred.username.selectAll();
			}
			if(!newPassword.trim().isEmpty()) {
				user.password = newPassword;
			} else {
				cred.password.setText(user.password);
				cred.password.selectAll();
			}
			/*user.permissions.allowConsoleAccess = cred.permissions.allowConsoleAccess;
			user.permissions.canRestartServer = cred.permissions.canRestartServer;
			user.permissions.canModifyFiles = cred.permissions.canModifyFiles;*/
			i++;
		}
		for(Control control : this.content.getChildren()) {
			if(control instanceof CredentialComposite) {
				CredentialComposite check = (CredentialComposite) control;
				if(!check.randomKey.equals(randomKey)) {
					check.dispose();
				}
			}
		}
		this.content.setSize(0, CredentialComposite.yTotal * this.content.getChildren().length);
		this.createCredential.setLocation(10, Math.max(CredentialComposite.yInterval, CredentialComposite.yTotal * (this.content.getChildren().length - 1)));
		final boolean createHasUsername = !this.createCredential.username.getText().trim().isEmpty();
		final boolean createHasPassword = !this.createCredential.password.getText().trim().isEmpty();
		if((createHasUsername && createHasPassword) != this.createCredential.btnCreate.isEnabled()) {
			this.createCredential.btnCreate.setEnabled(createHasUsername && createHasPassword);
		}
		boolean enableDoneButton = !createHasUsername && !createHasPassword;
		if(this.btnDone.getEnabled() != enableDoneButton) {
			this.btnDone.setEnabled(enableDoneButton);
		}
	}
	
	public static final class UserPermissions {
		public volatile boolean	allowConsoleAccess;
		public volatile boolean	canRestartServer;
		public volatile boolean	canStopServer;
		public volatile boolean	canModifyFiles;
		public volatile boolean	canDeleteFiles;
		public volatile boolean	canDownloadFiles;
		
		public UserPermissions() {
			this(false);
		}
		
		public UserPermissions(boolean allPerms) {
			this(allPerms, allPerms, allPerms, allPerms, allPerms, allPerms);
		}
		
		public UserPermissions(boolean allowConsoleAccess, boolean canRestartServer, boolean canStopServer, boolean canModifyFiles, boolean canDeleteFiles, boolean canDownloadFiles) {
			this.allowConsoleAccess = allowConsoleAccess;
			this.canRestartServer = canRestartServer;
			this.canStopServer = canStopServer;
			this.canModifyFiles = canModifyFiles;
			this.canDeleteFiles = canDeleteFiles;
			this.canDownloadFiles = canDownloadFiles;
		}
		
	}
	
	public static final class CreateNewCredentialComposite extends Composite {
		
		public final CredentialsManager	manager;
		
		public final Label				lblUsername;
		public final Label				lblPassword;
		public final Button				btnCreate;
		public final Text				username;
		public final Text				password;
		public final Button				btnEditPermissions;
		
		public volatile UserPermissions	permissions;
		
		/** @param parent
		 * @param style */
		public CreateNewCredentialComposite(Composite parent, CredentialsManager manager) {
			super(parent, SWT.NORMAL);
			this.manager = manager;
			this.permissions = new UserPermissions(false);
			this.setSize(450, CredentialComposite.ySize);
			
			this.lblUsername = new Label(this, SWT.NONE);
			this.lblUsername.setBounds(10, 10, 68, 15);
			this.lblUsername.setText("Username:");
			
			this.lblPassword = new Label(this, SWT.NONE);
			this.lblPassword.setBounds(10, 39, 68, 15);
			this.lblPassword.setText("Password:");
			
			this.btnCreate = new Button(this, SWT.NONE);
			this.btnCreate.setBounds(372, 10, 68, 44);
			this.btnCreate.setText("Create");
			this.btnCreate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String username = CreateNewCredentialComposite.this.username.getText();
					String pswd = CreateNewCredentialComposite.this.password.getText();
					if(username != null && !username.trim().isEmpty() && pswd != null && !pswd.trim().isEmpty()) {
						username = username.trim();
						if(manager.getUser(username) == null) {
							Credential user = new Credential(username, pswd, CreateNewCredentialComposite.this.permissions);
							manager.credentials.add(user);
							CreateNewCredentialComposite.this.username.setText("");
							CreateNewCredentialComposite.this.password.setText("");
							CreateNewCredentialComposite.this.permissions = new UserPermissions(false);
						}
					}
				}
			});
			
			this.username = new Text(this, SWT.BORDER);
			this.username.setBounds(84, 10, 140, 21);
			
			this.password = new Text(this, SWT.BORDER | SWT.PASSWORD);
			this.password.setBounds(84, 36, 140, 21);
			
			this.btnEditPermissions = new Button(this, SWT.NONE);
			this.btnEditPermissions.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					new EditUserPermissionsDialog(manager.shell, CreateNewCredentialComposite.this.permissions).open();
				}
			});
			this.btnEditPermissions.setBounds(241, 10, 125, 44);
			this.btnEditPermissions.setText("Edit Permissions");
			
			Label label = new Label(this, SWT.SEPARATOR | SWT.VERTICAL);
			label.setLocation(230, 10);
			label.setSize(2, 44);
		}
		
	}
	
	public static final class CredentialComposite extends Composite {
		
		public static final int			ySize		= 64;
		public static final int			yInterval	= 10;
		public static final int			yTotal		= ySize + yInterval;
		
		public final CredentialsManager	manager;
		protected volatile String		randomKey	= "";
		
		public final Credential			user;
		
		public final Label				lblUsername;
		public final Label				lblPassword;
		public final Button				btnDelete;
		public final Text				username;
		public final Text				password;
		public final Button				btnEditPermissions;
		
		public final UserPermissions	permissions;
		
		/** @param parent
		 * @param style */
		public CredentialComposite(Composite parent, CredentialsManager manager, Credential user) {
			super(parent, SWT.BORDER);
			this.manager = manager;
			this.setSize(450, ySize);
			
			this.user = user;
			this.permissions = user.permissions;
			
			this.lblUsername = new Label(this, SWT.NONE);
			this.lblUsername.setBounds(10, 10, 68, 15);
			this.lblUsername.setText("Username:");
			
			this.lblPassword = new Label(this, SWT.NONE);
			this.lblPassword.setBounds(10, 39, 68, 15);
			this.lblPassword.setText("Password:");
			
			this.btnDelete = new Button(this, SWT.NONE);
			this.btnDelete.setBounds(372, 10, 68, 44);
			this.btnDelete.setText("Delete");
			this.btnDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					manager.credentials.remove(manager.getUser(CredentialComposite.this.username.getText().trim()));
				}
			});
			
			/*this.username = new Text(this, SWT.BORDER);
			this.username.setBounds(84, 10, 282, 21);
			this.username.setEnabled(true);
			this.username.setEditable(false);
			
			this.password = new Text(this, SWT.BORDER | SWT.PASSWORD);
			this.password.setBounds(84, 36, 282, 21);*/
			this.username = new Text(this, SWT.BORDER);
			this.username.setBounds(84, 10, 140, 21);
			this.username.setText(user.username);
			
			this.password = new Text(this, SWT.BORDER | SWT.PASSWORD);
			this.password.setBounds(84, 36, 140, 21);
			this.password.setText(user.password);
			
			Label label = new Label(this, SWT.SEPARATOR | SWT.VERTICAL);
			label.setLocation(230, 10);
			label.setSize(2, 44);
			
			this.btnEditPermissions = new Button(this, SWT.NONE);
			this.btnEditPermissions.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					new EditUserPermissionsDialog(manager.shell, CredentialComposite.this.permissions).open();
				}
			});
			this.btnEditPermissions.setBounds(241, 10, 125, 44);
			this.btnEditPermissions.setText("Edit Permissions");
			
		}
		
	}
	
}
