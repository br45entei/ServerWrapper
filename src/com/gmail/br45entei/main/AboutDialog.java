/**
 * 
 */
package com.gmail.br45entei.main;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** @author Brian_Entei */
public class AboutDialog extends Dialog {
	
	protected Response	result	= Response.NO_RESPONSE;
	protected Shell		shell;
	
	/** Create the dialog.
	 * 
	 * @param parent The parent Shell */
	public AboutDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open() {
		this.createContents();
		if(!Main.isHeadless()) {
			this.shell.open();
			this.shell.layout();
			//Display display = getParent().getDisplay();
			while(!this.shell.isDisposed()) {
				if(this.result != Response.NO_RESPONSE) {
					break;
				}
				Main.mainLoop();
				/*if(!display.readAndDispatch()) {
					display.sleep();
				}*/
			}
			if(!this.shell.isDisposed()) {
				this.shell.close();
			}
		}
		return this.result;
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(this.getParent(), this.getStyle());
		this.shell.setSize(410, 400);
		this.shell.setText("About " + Main.getDefaultShellTitle());
		this.shell.setImages(getParent().getImages());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		Text lblMessage = new Text(this.shell, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		lblMessage.setText("  Server Wrapper is a program written in Java by Brian Entei.\r\nIt is used to 'wrap' a Minecraft server so that its input and output\r\nstreams may be read from/written to without you needing to be\r\nat(or connected to) the host machine using applications such as\r\nRemote Desktop or Teamviewer. Basically, it replaces your\r\nMinecraft server's console or terminal window with added options.\r\n\r\n  To get started, select your server by clicking on \"Choose jar...\" and\r\nthen browsing to your minecraft_server.jar(craftbukkit, spigot, etc.).\r\nNext, change the memory allocation options for your server or add\r\nyour own Java VM arguments. You can also optionally add some\r\nprogram arguments.\r\n  Now you are ready to start your server! Simply click \"Start Server\",\r\nand the server should start up, and logs should begin appearing in\r\nthe black \"Console Output\" area(Sometimes it can take a second or\r\ntwo for the Minecraft server to start).\r\n\r\n  If you haven't agreed to the eula.txt, the server will stop and you\r\nwill need to open the eula.txt and change the setting to true. You\r\ncan access your server's parent folder easily by clicking on File >\r\n\"Open server folder\". Once you've agreed to the eula.txt, you should\r\nbe able to start the server without interruption.\r\n\r\n  You're almost done! The next step is to enable or disable Remote\r\nAdministration. It is enabled by default and listens on port 17349 of\r\nyour host machine.\r\n  If you want to be able to connect to the Server Wrapper from over\r\nthe network or even the internet, then Remote Administration must\r\nbe enabled. If you do not want this feature to be active, it can be\r\nsafely turned off, even if the Minecraft server is active.\r\n\r\n  That's it! You're done. The next time you open the Server Wrapper,\r\nit will have saved all of these settings and will automatically start\r\nyour new Minecraft server for you. If you do not want your\r\nMinecraft server to start up automatically, you can toggle it off\r\nwith File > \"Start server automatically on startup\".\r\n\r\n  Additionally, if your server has a \"server-icon.png\" file, the\r\nServer Wrapper will load this file when the server is started.\r\nThe Server Wrapper will also attempt to find your server's name\r\nor motd from its server.properties file and will display these in the\r\ntitle bar. These are also sent to connecting clients so that they may\r\ndisplay them as well, but this can be toggled off with File >\"Send\r\nserver info to clients\".\r\n\r\n  Note: If you attempt to start your Minecraft server and you receive\r\nerrors such as \"Missing/invalid Yggdrasil public key!\" or similar, you\r\nmay need to change the version of Java that the Wrapper uses\r\nto launch your Minecraft server. To do this, go to File >\"Select Java\r\nexecutable...\" and browse to an up-to-date JRE(not a JDK!) version\r\nof Java. For example, on Windows, this would look something like\r\nthis:\r\nC:\\Program Files\\Java\\jre1.8u65\\bin\\java.exe\r\n  Actual path names vary depending on your machine's architecture\r\n(64 vs. 32 bit) and operating system.\r\n\r\n  Usernames and passwords are not sent to anything or anyone(not\r\neven clients!), but are saved to the local filesystem and can be\r\nchanged using the File > \"Scheduled restarting options...\" option.\r\nSource code for this application is available on GitHub at: https://github.com/br45entei/ServerWrapper\r\n\r\nQuestions/comments?\r\nLeave me a message at br45entei@gmail.com and I will be sure to\r\nget back with you! Thank you for using Server Wrapper.");
		lblMessage.setBounds(10, 10, 384, 320);
		
		Button btnDone = new Button(this.shell, SWT.NONE);
		btnDone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AboutDialog.this.result = Response.DONE;
			}
		});
		btnDone.setBounds(164, 336, 75, 25);
		btnDone.setText("Done");
		
	}
	
}
