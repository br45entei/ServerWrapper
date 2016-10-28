package com.gmail.br45entei.main;

import com.gmail.br45entei.data.DisposableByteArrayOutputStream;
import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.data.RemoteClient;
import com.gmail.br45entei.main.CredentialsManager.Credential;
import com.gmail.br45entei.main.ScheduledRestartingOptionsDialog.TimeResult;
import com.gmail.br45entei.process.ProcessIO;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;
import com.gmail.br45entei.update.PerformUpdateDialog;
import com.gmail.br45entei.update.PromptDownloadUpdateDialog;
import com.gmail.br45entei.update.UpdateChecker.UpdateResult;
import com.gmail.br45entei.update.UpdateChecker.UpdateType;
import com.gmail.br45entei.update.UpdateCheckerDialog;
import com.gmail.br45entei.util.IOUtils;
import com.gmail.br45entei.util.JavaProgramArguments;
import com.gmail.br45entei.util.StringUtil;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public final class Main {
	
	private static final boolean					headless	= GraphicsEnvironment.isHeadless();
	// Would use https://www.x.org/archive/X11R6.8.2/doc/Xvfb.1.html , but not opening shell works too... XD
	private static volatile boolean					actHeadless	= false;
	private static volatile JavaProgramArguments	sysArgs;
	
	public static final JavaProgramArguments getSysArgs() {
		return sysArgs;
	}
	
	public static final File getClassPathJarFile() {
		return JavaProgramArguments.getClassPathJarFile();
	}
	
	public static final boolean isHeadless() {
		return headless || actHeadless;
	}
	
	public static final Charset								stringCharset			= StandardCharsets.UTF_8;
	
	protected static Thread									swtThread;
	
	public static volatile String							javaHome;
	public static volatile String							javaExecutable;
	public static volatile File								serverJar				= null;
	
	protected static volatile boolean						isRunning				= false;
	
	/** The working directory of this application as determined by<br>
	 * <code>System.getProperty("user.dir")</code> */
	public static final File								rootDir					= new File(System.getProperty("user.dir"));
	protected static final File								settingsFile			= new File(rootDir, "settings.txt");
	
	//Console font settings ================
	
	protected static volatile String						consoleFontName			= "Consolas";
	protected static volatile int							consoleFontSize			= 8;
	protected static volatile boolean						consoleFontBold			= false;
	protected static volatile boolean						consoleFontStrikeout	= false;
	protected static volatile boolean						consoleFontUnderLined	= false;
	protected static volatile boolean						consoleFontItalicized	= true;
	
	//General wrapper settings =============
	
	protected static volatile boolean						automaticServerStartup	= true;
	protected static volatile boolean						automaticServerRestart	= false;
	
	protected static volatile boolean						alwaysShowTrayIcon		= false;
	
	/** Whether or not server information such as icons and server names/motds
	 * are sent to clients */
	public static volatile boolean							sendServerInfoToClients	= true;
	
	//=============
	
	protected static final ArrayList<String>				logsToAppend			= new ArrayList<>();
	
	protected static final HashMap<Integer, String>			inputtedCommands		= new HashMap<>();
	protected static final Property<Integer>				selectedCommand			= new Property<>("Selected Command", Integer.valueOf(0));
	protected static final DisposableByteArrayOutputStream	out						= new DisposableByteArrayOutputStream();
	protected static final Property<String>					outTxt					= new Property<>("Console Text", "");
	protected static final Thread							outTxtUpdateThread		= new Thread(new Runnable() {
																						@Override
																						public final void run() {
																							int lastBufferSize = 0;
																							while(true) {
																								if(out.size() != lastBufferSize) {
																									lastBufferSize = out.size();
																									try {
																										final String text;
																										String t = out.toString();
																										//Functions.sleep(15L);
																										//String random = ((Object) "".toCharArray()).toString();
																										//t = t.replace("\r>", "").replace("\r\n", random).replace("\r", "\n").replace("\n\n", "\n").replace(random, "\r\n").replace("\n\n", "\n");
																										
																										Functions.sleep(10L);
																										final int maxLines = 10000;
																										final int numOfLines = StringUtil.getNumOfLinesInStr(t);
																										Functions.sleep(10L);
																										if(numOfLines > maxLines) {
																											final int numOfLinesToSkip = numOfLines - maxLines;
																											int i = 0;
																											String[] split = t.split(Pattern.quote("\n"));
																											t = "";
																											for(String s : split) {
																												i++;
																												if(i >= numOfLinesToSkip) {
																													t += s + "\n";
																												}
																												Functions.sleep(10L);
																											}
																										}
																										text = t;																																	// + ">";
																										outTxt.setValue(text);
																									} catch(Throwable e) {
																										e.printStackTrace();
																									}
																								}
																								Functions.sleep(20L);
																							}
																						}
																					}, "ConsoleWindow_UpdateThread");
	protected static final DisposableByteArrayOutputStream	err						= new DisposableByteArrayOutputStream();
	protected static final Property<String>					errTxt					= new Property<>("Console Text", "");
	protected static final Thread							errTxtUpdateThread		= new Thread(new Runnable() {
																						@Override
																						public final void run() {
																							int lastBufferSize = 0;
																							while(true) {
																								if(err.size() != lastBufferSize) {
																									lastBufferSize = err.size();
																									try {
																										final String text;
																										String t = err.toString();
																										//Functions.sleep(15L);
																										//String random = ((Object) "".toCharArray()).toString();
																										//t = t.replace("\r>", "").replace("\r\n", random).replace("\r", "\n").replace("\n\n", "\n").replace(random, "\r\n").replace("\n\n", "\n");
																										
																										Functions.sleep(10L);
																										final int maxLines = 10000;
																										final int numOfLines = StringUtil.getNumOfLinesInStr(t);
																										Functions.sleep(10L);
																										if(numOfLines > maxLines) {
																											final int numOfLinesToSkip = numOfLines - maxLines;
																											int i = 0;
																											String[] split = t.split(Pattern.quote("\n"));
																											t = "";
																											for(String s : split) {
																												i++;
																												if(i >= numOfLinesToSkip) {
																													t += s + "\n";
																												}
																												Functions.sleep(10L);
																											}
																										}
																										text = t;																																	// + ">";
																										errTxt.setValue(text);
																									} catch(Throwable e) {
																										e.printStackTrace();
																									}
																								}
																								Functions.sleep(20L);
																							}
																						}
																					}, "ConsoleWindow_Err_UpdateThread");
	
	public static final String getConsoleLogs() {
		return outTxt.getValue();
	}
	
	public static final String getConsoleErrors() {
		return errTxt.getValue();
	}
	
	static {
		if(!rootDir.exists()) {
			rootDir.mkdirs();
		}
		outTxtUpdateThread.setDaemon(true);
		errTxtUpdateThread.setDaemon(true);
	}
	
	protected static volatile ProcessIO							process;
	protected static volatile int								processExitCode				= 0;
	protected static volatile boolean							hasProcessDied				= false;
	protected static volatile boolean							startServer					= false;
	private static volatile RemoteClient						startServerClient			= null;
	protected static volatile boolean							updateShellAppearance		= false;
	
	protected static volatile boolean							isProcessBeingStarted		= false;
	
	protected static volatile boolean							scheduledRestartInEffect	= false;
	
	protected static volatile boolean							serverStoppedByUser			= false;
	
	private static volatile String								serverState;
	protected static volatile boolean							aboutDialogIsOpen			= false;
	
	protected static Display									display;
	protected static Shell										shell;
	protected static StyledText									output;
	protected static StyledText									error;
	protected static Text										inputField;
	
	protected static MenuItem									mntmExittaltf;
	protected static MenuItem									mntmSelectJavaExecutable;
	protected static MenuItem									mntmStartServerAutomatically;
	protected static MenuItem									mntmTrayIconAlways;
	protected static MenuItem									mntmRestartServerAutomatically;
	protected static MenuItem									mntmSendServerInfo;
	protected static MenuItem									mntmCheckForUpdates;
	protected static MenuItem									mntmOpenServerFolder;
	protected static MenuItem									mntmEnableScheduledRestarts;
	protected static Button										btnSendInput;
	protected static Text										txtServerjar;
	protected static Text										txtVmArgs;
	protected static Button										btnUseG1GC;
	protected static Text										txtProgramArgs;
	protected static StyledText									wrapperLog;
	protected static Button										btnStartServer;
	protected static Button										btnStopServer;
	protected static Button										btnRestartServer;
	protected static Button										btnKillProcess;
	protected static Button										btnChooseJar;
	protected static Label										verticalSeparator;
	protected static Label										horizontalSeparator;
	protected static Label										horizontalSeparator2;
	protected static Spinner									serverListenPort;
	protected static volatile boolean							serverListenPortAvailable;
	protected static Button										btnChangePort;
	protected static Label										lblPortWas;
	protected static Button										btnDisableRemoteAdmin;
	protected static Button										btnEnableRemoteAdmin;
	private static MenuItem										mntmabout;
	protected static MenuItem									mntmHideWindow;
	
	public static final String									G1GC_VM_ARGS				= "-XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=50 -XX:TargetSurvivorRatio=90 -XX:G1NewSizePercent=50 -XX:G1MaxNewSizePercent=80 -XX:InitiatingHeapOccupancyPercent=10 -XX:G1MixedGCLiveThresholdPercent=50 -XX:+AggressiveOpts";
	protected static volatile boolean							useG1GC						= false;
	
	//Temporary Variables =======
	
	protected static volatile boolean							startServerImmediately		= false;
	protected static volatile boolean							startHiddenInTray			= false;
	
	protected static final ConcurrentLinkedDeque<Credential>	savedCredentials			= new ConcurrentLinkedDeque<>();
	
	//===========================
	
	public static final boolean doUseG1GC() {
		return Main.useG1GC;
	}
	
	public static final Credential getCredentialsFor(String username, String password) {
		for(Credential user : savedCredentials) {
			if(user.username.equalsIgnoreCase(username) && user.password.equals(password)) {
				return user;
			}
		}
		return null;
	}
	
	public static final boolean isRunning() {
		return Main.isRunning;
	}
	
	public static final String getDefaultShellTitle() {
		return "Server Wrapper - Version " + RemoteAdmin.PROTOCOL_VERSION;
	}
	
	public static final Image[] getDefaultImages() {
		return new Image[] {SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-16x16.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-32x32.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-64x64.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-128x128.png")};
	}
	
	/** Launch the application.
	 * 
	 * @param args System command arguments */
	public static final void main(String[] args) {
		JavaProgramArguments.initializeFromMainClass(Main.class, args);
		sysArgs = JavaProgramArguments.getArguments();
		Main.javaHome = sysArgs.javaHome;
		Main.javaExecutable = sysArgs.javaExecutable;
		Main.startServerImmediately = StringUtil.containsIgnoreCase(sysArgs.arguments, "startServer") || StringUtil.containsIgnoreCase(sysArgs.arguments, "runServer");
		Main.startHiddenInTray = StringUtil.containsIgnoreCase(sysArgs.arguments, "trayOnly") || StringUtil.containsIgnoreCase(sysArgs.arguments, "silent") || StringUtil.containsIgnoreCase(sysArgs.arguments, "hidden");
		Main.actHeadless = StringUtil.containsIgnoreCase(sysArgs.arguments, "-headless") || StringUtil.containsIgnoreCase(sysArgs.arguments, "headless");
		Main.swtThread = Thread.currentThread();
		
		Main.display = Display.getDefault();
		shell = new Shell(Main.display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				Main.shutdown();
			}
		});
		shell.setSize(904, 592);
		shell.setMinimumSize(904, 592);
		shell.setText(getDefaultShellTitle());
		shell.setImages(getDefaultImages());
		Functions.centerShellOnPrimaryMonitor(shell);
		
		Main.createContents();
		
		Main.loadSettings();
		int index = -1;
		int i = 0;
		for(String arg : args) {
			if(arg != null) {
				if(arg.equalsIgnoreCase("-start")) {
					index = i + 1;
					break;
				}
			}
			i++;
		}
		if(index != -1) {
			String jarPath = StringUtil.stringArrayToString(args, ' ', index);
			if(!jarPath.isEmpty()) {
				File check = new File(jarPath);
				if(check.isFile()) {
					Main.serverJar = check;
					Main.saveSettings();
				} else {
					System.err.println("Unable to resolve requested file path: " + jarPath);
				}
			}
		}
		Main.serverListenPort.setSelection(RemoteAdmin.listenPort);
		
		ArrayList<Credential> credentials = Credential.initialize(rootDir);
		if(credentials != null) {
			savedCredentials.addAll(credentials);
		}
		RemoteAdmin.setupListenSocket();
		outTxtUpdateThread.start();
		errTxtUpdateThread.start();
		
		Main.isRunning = true;
		Main.startRemoteAdmin();
		
		if(Main.trayIcon != null && Main.startHiddenInTray) {
			Main.hideShell(true);
		} else {
			Main.openShell();
		}
		if((Main.startServerImmediately || Main.automaticServerStartup) && Main.isServerJarSelected()) {//Automatic startup if jar file is loaded from config
			Main.automaticServerStartup = true;
			Main.launchServer(null);
			Main.saveSettings();
		}
		while(Main.isRunning && !Main.shell.isDisposed()) {
			Main.mainLoop();
		}
		Main.appendLog("ServerWraper shutting down; please wait...");
		if(process != null && process.process.isAlive()) {
			Main.serverStoppedByUser = true;
			Main.handleInput(null, "save-all\nstop");//process.process.destroyForcibly();
			Main.isRunning = true;//kek
			Main.openShell();
			final long startTime = System.currentTimeMillis();
			final long waitTime = 15000L;
			long lastSecond = startTime;
			int secondsWaited = 15;
			Main.appendLog("Waiting " + secondsWaited + " seconds for server to shutdown:");
			while(process != null && process.process.isAlive()) {
				Main.mainLoop();
				final long now = System.currentTimeMillis();
				long elapsedTime = now - startTime;
				long elapsedSecondTime = now - lastSecond;
				if(elapsedSecondTime >= 1000L) {
					lastSecond = now;
					Main.appendLog("\t" + --secondsWaited + " seconds remaining");
				}
				if(elapsedTime > waitTime) {
					if(process != null) {
						process.process.destroyForcibly();
					}
					break;
				}
			}
			Main.isRunning = false;//un-kek
		}
		Main.saveSettings();
		Main.display.dispose();
		for(Credential user : savedCredentials) {
			user.saveToFile();
		}
		System.exit(0);
	}
	
	private static final String getXmsFromXmxForG1GC() {
		String rtrn = "";
		if(Main.btnUseG1GC.getSelection()) {
			if(Main.txtVmArgs.getText().contains("-Xmx")) {
				String[] split = Main.txtVmArgs.getText().split(Pattern.quote(" "));
				for(String arg : split) {
					if(arg.startsWith("-Xmx")) {
						rtrn = arg.replace("-Xmx", "-Xms");
						break;
					}
				}
			}
		}
		return rtrn;
	}
	
	private static volatile boolean checkVMArgsForXms = false;
	
	protected static final void checkVMArgsForXms() {
		if(!Main.checkThreadAccess()) {
			Main.checkVMArgsForXms = true;
			return;
		}
		Main.checkVMArgsForXms = false;
		if(Main.btnUseG1GC.getSelection()) {
			if(Main.txtVmArgs.getText().contains("-Xms")) {
				String newArgs = "";
				String[] split = Main.txtVmArgs.getText().split(Pattern.quote(" "));
				int index = 0;
				for(String arg : split) {
					if(!arg.startsWith("-Xms")) {
						newArgs += arg + (index + 1 != split.length ? " " : "");
					}
					index++;
				}
				if(!Main.txtVmArgs.getText().equals(newArgs)) {
					Main.txtVmArgs.setText(newArgs);
					Main.appendLog("==Java's -Xms vm option should be the same as the -Xmx option when using the G1 Garbage Collector.\nTherefore, it will be handled automatically.");
				}
			}
		}
	}
	
	private static final void setShellTitle() {
		final String defaultTitle = Main.getDefaultShellTitle();
		String text = defaultTitle;
		if(Main.isServerJarSelected() && Main.isProcessAlive()) {
			System.out.println("Wrapper root dir: " + rootDir.getAbsolutePath());
			System.out.println("Selected server jar: " + Main.serverJar.getAbsolutePath());
			File serverFolder = Main.serverJar.getParentFile();
			if(serverFolder.isDirectory()) {
				System.out.println("Resulting server folder: " + serverFolder.getAbsolutePath());
				File serverProperties = new File(serverFolder, "server.properties");
				if(serverProperties.isFile()) {
					System.out.println("Server properties file: " + serverProperties.getAbsolutePath());
					byte[] bytes = StringUtil.readFile(serverProperties);
					String data = bytes == null ? null : new String(bytes);
					bytes = null;
					if(data != null) {
						String[] split = data.split(Pattern.quote("\n"));
						final HashMap<String, String> properties = new HashMap<>();
						for(String line : split) {
							if(line.endsWith("\r")) {
								line = line.substring(0, line.length() - 1);
							}
							String[] entry = line.split(Pattern.quote("="));
							if(entry.length > 1) {
								String pname = entry[0];
								String value = StringUtil.stringArrayToString(entry, '=', 1).trim();
								properties.put(pname, value);
							}
						}
						final String serverName = properties.get("server-name");
						final String motd = properties.get("motd");
						if(serverName != null && !serverName.isEmpty()) {
							text = serverName;
						} else if(motd != null && !motd.isEmpty()) {
							text = motd;
						}
					}
				}
			}
		}
		if(!defaultTitle.equals(text)) {
			text = text.trim().replace("&", "&&").replaceAll(Pattern.quote("\r"), "").replaceAll(Pattern.quote("\n"), " ").trim();
			RemoteClient.setServerTitle(text);
			text += " - " + defaultTitle;
		} else {
			RemoteClient.setServerTitle(null);
		}
		Functions.setTextFor(Main.shell, text);
	}
	
	private static final void setShellIcon() {
		Image[] images = Main.getDefaultImages();
		if(Main.isServerJarSelected() && Main.isProcessAlive()) {
			File serverFolder = Main.serverJar.getParentFile();
			if(serverFolder.isDirectory()) {//i.e. if it exists and is a directory
				File serverIcon = new File(serverFolder, "server-icon.png");
				if(!serverIcon.exists()) {
					serverIcon = new File(serverFolder, "server-icon.ico");
				}
				if(serverIcon.isFile()) {//i.e. if it exists and is a file
					Image image = SWTResourceManager.getImage(serverIcon.getAbsolutePath());
					if(image != null) {
						RemoteClient.setServerIconFile(serverIcon);
						images = new Image[] {image};
					} else {
						RemoteClient.setServerIconFile(null);
					}
				}
			}
		} else {
			RemoteClient.setServerIconFile(null);
		}
		Functions.setShellImages(shell, images);
		if(Main.trayIcon != null) {
			Main.trayIcon.setImage(images[0]);
		}
	}
	
	public static final String getShellTitle() {
		return Main.shell.getText();
	}
	
	public static final Image[] getShellImages() {
		return Main.shell.getImages();
	}
	
	private static final void updateShellAppearance() {
		if(!Main.checkThreadAccess()) {
			Main.updateShellAppearance = true;
			return;
		}
		Main.updateShellAppearance = false;
		Main.setShellTitle();
		Main.setShellIcon();
		if(!Main.isProcessAlive()) {
			RemoteClient.setServerIconFile(null);
		}
	}
	
	public static final void launchServer(RemoteClient from) {
		if(Main.isProcessAlive() || !Main.isServerJarSelected()) {
			Main.startServer = false;//Prevents potential loops/stackoverflows
			return;
		}
		if(!Main.checkThreadAccess()) {
			Main.startServer = true;
			Main.startServerClient = from;
			return;
		}
		Main.stoppingServer = false;//Fix for 'stoppingServer' variable becoming stuck to true, causing the stop and restart buttons to stop working indefinitely...
		if(Main.startServer && from == null) {
			from = Main.startServerClient;
		}
		Main.startServerClient = null;
		Main.isProcessBeingStarted = true;
		try {
			Main.serverStoppedByUser = false;
			Main.hasProcessDied = false;
			Main.processExitCode = 0;
			Main.out.dispose();
			Main.err.dispose();
			//RemoteClient.resetClientLogs();
			//appendLog("==\r\n");//wrapperLog.setText("");
			final String jarPath = Main.serverJar.getAbsolutePath();
			final String jarCmdLine = " -jar " + (jarPath.contains(" ") ? "\"" + jarPath + "\"" : jarPath).trim();
			final String vmArgsCmdLine = (Main.txtVmArgs.getText().trim().isEmpty() ? "" : " " + Main.txtVmArgs.getText().trim()) + (Main.useG1GC ? " " + Main.getXmsFromXmxForG1GC() + " " + G1GC_VM_ARGS : "");
			final String progArgsCmdLine = Main.txtProgramArgs.getText().trim().isEmpty() ? "" : " " + Main.txtProgramArgs.getText().trim();
			final String command = "\"" + Main.javaExecutable + "\"" + vmArgsCmdLine + jarCmdLine + progArgsCmdLine + (progArgsCmdLine.isEmpty() ? " nogui" : (!progArgsCmdLine.contains("nogui") ? " nogui" : ""));
			final Runnable[] runnables = new Runnable[] {new Runnable() {
				@Override
				public final void run() {
					final ProcessIO THIS = process;
					//byte[] buf = new byte[4096];
					//int read;
					while(THIS.process != null && THIS.process.isAlive()) {
						try {
							String line = StringUtil.readLine(THIS.out);
							if(line != null) {
								Main.addLogToConsole(line);
							}
							/*read = THIS.out.read(buf);
							if(read != -1) {
								Main.out.write(buf, 0, read);
							} else {
								break;
							}*/
						} catch(IOException e) {
							e.printStackTrace();
							break;
						}
					}
					//System.out.println("Out stream ended.");
				}
			}, new Runnable() {
				@Override
				public final void run() {
					final ProcessIO THIS = process;
					//byte[] buf = new byte[4096];
					//int read;
					while(THIS.process != null && THIS.process.isAlive()) {
						try {
							String line = StringUtil.readLine(THIS.err);
							if(line != null) {
								addErrorToConsole(line);
							}
							/*read = THIS.err.read(buf);
							if(read != -1) {
								Main.err.write(buf, 0, read);
							} else {
								break;
							}*/
						} catch(IOException e) {
							e.printStackTrace();
							break;
						}
					}
					//System.err.println("Error stream ended.");
				}
			}};
			Thread[] threads = new Thread[0];
			try {
				process = new ProcessIO("\"" + Main.javaExecutable + "\"", vmArgsCmdLine + jarCmdLine, progArgsCmdLine + (progArgsCmdLine.isEmpty() ? " nogui" : (!progArgsCmdLine.contains("nogui") ? " nogui" : "")), Main.serverJar.getParentFile());
				Main.appendLog("==Launch command:\r\n\t" + process.cmdLine);
				threads = process.startThreads(runnables[0], runnables[1]);
			} catch(Throwable e) {
				if(process != null && process.process != null) {
					process.process.destroyForcibly();
					process = null;
					threads = new Thread[0];
				}
				if(e.getMessage() != null && e.getMessage().startsWith("Failed to retrieve RMIServer stub: ")) {//Failed to retrieve RMIServer stub: javax.naming.ServiceUnavailableException [Root exception is java.rmi.ConnectException: Connection refused to host: 127.0.0.1; nested exception is:
					if(Main.javaExecutable.replace("\\", "/").contains("/jdk")) {
						new JDKWarningDialog(Main.shell).open();
					} else if(Main.btnUseG1GC.getSelection()) {
						Main.appendLog("==Launch failed; re-attempting launch without G1 garbage collector enabled...\r\n");
						Main.btnUseG1GC.setSelection(false);
						Main.useG1GC = false;
						Main.startServer = false;
						Main.launchServer(from);//Try again without G1GC
						Main.isProcessBeingStarted = false;
						return;
					} else {
						Main.appendLog("==" + Functions.throwableToStr(e));
						Main.appendLog("==Launch failed; attempting launch without memory monitoring features enabled...\r\n");
						Main.appendLog("==Launch command:\r\n\t" + command);
						ProcessBuilder builder = new ProcessBuilder(command.split(Pattern.quote(" ")));
						builder.redirectOutput(Redirect.PIPE);
						builder.redirectError(Redirect.PIPE);
						builder.redirectInput(Redirect.PIPE);
						builder.directory(Main.serverJar.getParentFile());
						Main.appendLog("Starting server; please wait...");
						process = new ProcessIO(builder);
						threads = process.startThreads(runnables[0], runnables[1]);
					}
				} else {
					throw e;
				}
			}
			Main.updateShellAppearance();
			if(threads.length == 2) {
				Main.appendLog("Server told to start successfully.");
			} else {
				Main.appendLog("Failed to start server: Unable to verify server process started successfully!");
				Main.startServer = false;
				Main.stopServer(null, false);
			}
		} catch(Throwable e) {
			Main.appendLog("Failed to launch server: " + Functions.throwableToStr(e));
			if(e.getMessage() != null && e.getMessage().startsWith("Failed to retrieve RMIServer stub: ")) {
				e.printStackTrace();
				if(Main.javaExecutable.replace("\\", "/").contains("/jdk")) {
					new JDKWarningDialog(Main.shell).open();
				}
			}
		}
		Main.startServer = false;
		Main.isProcessBeingStarted = false;
	}
	
	private static volatile boolean			stoppingServer			= false;
	private static Composite				consoleOutputTabComposite;
	private static TabFolder				tabFolder;
	
	private static Composite				cpuUsageComposite;
	private static ProgressBar				cpuUsageBar;
	private static Label					cpuUsageTxt;
	
	private static Composite				ramUsageComposite;
	private static Label					lblRamUsage;
	private static Label					lblThreadCount;
	private static Label					threadCountTxt;
	private static ProgressBar				ramUsageBar;
	private static Label					ramUsageTxt;
	private static Composite				resourceTabComposite;
	private static Button					btnTab;
	private static TabItem					tbtmConnectedClients;
	private static ScrolledComposite		connectedClientsContent;
	private static Label					noConnectedClientsMsg;
	protected static Composite				contentArea;
	
	protected static volatile TrayIcon		trayIcon;
	
	protected static volatile boolean		enableScheduledRestarts	= false;
	protected static volatile TimeResult	scheduledRestartData	= new TimeResult();
	protected static volatile String		restartCommands			= "";
	
	protected static final String			noConnText				= "There are no connected clients at this time.";
	protected static final String			remAdminDisabled		= "Remote Administration is disabled. To allow clients to connect, please click the \"Enable\" button above.";
	
	public static final void stopServer(RemoteClient from, boolean rightNow) {
		if(Main.stoppingServer && !rightNow) {
			final String msg = "The server has already been told to shut down.\r\nGive it a second!";
			if(from != null) {
				from.println("WARN: " + msg);
			} else {
				Main.appendLog("==" + msg);
			}
			return;
		}
		Main.stoppingServer = true;
		if(process != null && process.process.isAlive()) {
			if(from != null) {
				process.stopServerClient = from;
			} else {
				from = process.stopServerClient;
			}
			if(rightNow) {
				Main.appendLog("Forcibly closing server process; please wait...");
				process.process.destroyForcibly();
				Main.serverStoppedByUser = true;
			} else {
				Main.handleInput(from, "save-all\nstop");//process.process.destroyForcibly();
				final long startTime = System.currentTimeMillis();
				final long waitTime = 15000L;
				long lastSecond = startTime;
				int secondsWaited = 15;
				Main.serverStoppedByUser = true;
				Main.appendLog("Stopping server; waiting up to 15 seconds for server to shutdown:");
				while(process != null && process.process.isAlive()) {
					Main.mainLoop();
					final long now = System.currentTimeMillis();
					long elapsedTime = now - startTime;
					long elapsedSecondTime = now - lastSecond;
					if(elapsedSecondTime >= 1000L) {
						lastSecond = now;
						Main.appendLog("\t" + --secondsWaited + " seconds remaining");
					}
					if(elapsedTime > waitTime) {
						if(process != null) {
							process.process.destroyForcibly();
						}
						break;
					}
				}
			}
			if(from != null) {
				Main.appendLog("Server " + (rightNow ? "forcibly closed" : "shut") + " down by \"" + from.getDisplayName(true) + "\".");
			}
			Main.updateShellAppearance();
		} else if(from != null) {
			from.println("WARN: The server is not active.");
			Main.appendLog("User \"" + from.getDisplayName(true) + "\" attempted to stop the server when it is already stopped!");
		}
		Main.stoppingServer = false;
	}
	
	public static final void restartServer(RemoteClient from) {
		if(Main.isProcessAlive()) {
			if(from != null) {
				from.println("SERVER-STATE: " + Main.getServerState());
				Main.appendLog("Restarting server; please wait...");
			}
			Main.stopServer(from, false);
			Main.waitUntilProcessEnded();
			Main.stoppingServer = false;
			Main.delayedServerStartup(from);
			if(from != null) {
				from.println("SERVER-STATE: " + Main.getServerState());
			}
		} else if(from != null) {
			from.println("WARN: The server is not active.");
			Main.appendLog("User \"" + from.getDisplayName(true) + "\" attempted to restart the server when it is not already started!");
		}
	}
	
	protected static final void waitUntilProcessEnded() {
		while(Main.isProcessAlive()) {
			Main.mainLoop();
		}
	}
	
	public static final String getProcessCpuUsage() {
		if(!Main.isProcessAlive()) {
			return null;
		}
		return process.cpuUsage != -1L ? process.cpuUsage + "" : null;
	}
	
	public static final String getProcessRamUsage() {
		if(!Main.isProcessAlive()) {
			return null;
		}
		return process.usedRam != -1L && process.committedRam != -1L ? process.usedRam + "-" + process.committedRam : null;
	}
	
	public static final String getProcessThreadCount() {
		if(!Main.isProcessAlive()) {
			return null;
		}
		return process.threadCount != -1 ? process.threadCount + "" : null;
	}
	
	protected static final void delayedServerStartup(RemoteClient from) {
		final long startTime = System.currentTimeMillis();
		final long waitTime = 2500L;//2.5 seconds
		while(System.currentTimeMillis() - startTime <= waitTime) {
			Main.mainLoop();
		}
		Main.launchServer(from);
	}
	
	private static final void onServerShutdown(RemoteClient from) {
		if(!Main.hasProcessDied || !Main.automaticServerRestart) {
			return;
		}
		if(!Main.serverStoppedByUser) {
			Main.appendLog("Restarting server; please wait...");
			Main.delayedServerStartup(null);
		} else {
			Main.appendLog("Server stopped by user" + (from != null ? " \"" + from.getDisplayName(true) + "\"" : "") + "; aborting automatic restart.");
		}
	}
	
	public static final void addLogToConsole(String log) {
		RemoteClient.sendLogToClients(log);
		byte[] data = (log + "\r\n").getBytes(stringCharset);
		Main.out.write(data, 0, data.length);
	}
	
	public static final void addErrorToConsole(String err) {
		RemoteClient.sendLogToClients("WARN: " + err);
		byte[] data = (err + "\r\n").getBytes(stringCharset);
		Main.err.write(data, 0, data.length);
	}
	
	public static final void handleInput(final RemoteClient from, final String input) {
		if(process != null) {
			if(process.process.isAlive()) {
				process.input.print(input + "\n");
				process.input.flush();
			}
		}
		Main.addLogToConsole((from != null ? from.getDisplayName(false) : "[Console]@127.0.0.1") + ">" + input);
	}
	
	protected static final void setTextFor(StyledText styledText) {
		String text = "";
		if(styledText == Main.output) {//if(errorStr == null) {
			text = outTxt.getValue() + ">";//XXX Console caret addition
		} else if(styledText == Main.error) {
			text = errTxt.getValue();
		}
		if(text == null) {
			text = "";
		}
		if(!styledText.getText().equals(text)) {
			final int numOfVisibleLines = Math.floorDiv(styledText.getSize().y, styledText.getLineHeight());
			final int originalIndex = styledText.getTopIndex();
			int index = originalIndex;
			final int lineCount = styledText.getLineCount();
			/*if(HTTPClientRequest.debug) {
				this.inputField.setText("index: \"" + index + "\"; line count: \"" + lineCount + "\"; visible lines: \"" + numOfVisibleLines + "\";");
			}*/
			Main.runClock();
			if(lineCount - index == numOfVisibleLines) {
				index = -1;
			}
			final Point selection = styledText.getSelection();
			final int caretOffset = styledText.getCaretOffset();
			//==
			//styledText.setText(text);
			styledText.getContent().setText(text);
			//==
			try {
				if(caretOffset == selection.x) {//Right to left text selection
					styledText.setCaretOffset(caretOffset);
					styledText.setSelection(selection.y, selection.x);
				} else {//Left to right text selection
					styledText.setSelection(selection);
					styledText.setCaretOffset(caretOffset);
				}
			} catch(IllegalArgumentException ignored) {
			}
			final int newLineCount = styledText.getLineCount();
			if(index == -1) {
				index = newLineCount - 1;
			} else {
				if(newLineCount >= lineCount) {
					index = newLineCount - (lineCount - index);
				} else {
					index = newLineCount - (newLineCount - index);
				}
			}
			styledText.setTopIndex(index);//originalIndex);//this.isScrollLocked ? originalIndex : index);
			Main.runClock();
		}
	}
	
	public static final boolean loadSettings() {
		if(!settingsFile.exists()) {
			return Main.saveSettings();
		}
		try(BufferedReader br = new BufferedReader(new FileReader(settingsFile))) {
			while(br.ready()) {
				String line = br.readLine();
				if(line.startsWith("#")) {
					continue;
				}
				String[] split = line.trim().split(Pattern.quote("="));
				if(split.length == 2) {
					String pname = split[0].trim();
					String value = split[1].trim();
					if(pname.equalsIgnoreCase("fontName")) {
						Main.consoleFontName = value;
					} else if(pname.equalsIgnoreCase("fontSize")) {
						if(StringUtil.isStrLong(value)) {
							Main.consoleFontSize = Long.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("fontBold")) {
						Main.consoleFontBold = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("fontItalicized")) {
						Main.consoleFontItalicized = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("fontStrikeout")) {
						Main.consoleFontStrikeout = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("fontUnderlined")) {
						Main.consoleFontUnderLined = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("automaticServerStartup")) {
						Main.automaticServerStartup = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("automaticServerRestart")) {
						Main.automaticServerRestart = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("alwaysShowTrayIcon")) {
						Main.alwaysShowTrayIcon = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("serverJar")) {
						File oldServerJar = serverJar;
						Main.serverJar = new File(value);
						if(!Main.serverJar.exists()) {
							Main.serverJar = oldServerJar;
						}
					} else if(pname.equalsIgnoreCase("vmArgs")) {
						Main.txtVmArgs.setText(value);
					} else if(pname.equalsIgnoreCase("useG1GC")) {
						Main.btnUseG1GC.setSelection(Boolean.valueOf(value).booleanValue());
						Main.useG1GC = Main.btnUseG1GC.getSelection();
					} else if(pname.equalsIgnoreCase("progArgs")) {
						Main.txtProgramArgs.setText(value);
					} else if(pname.equalsIgnoreCase("javaHome")) {
						File home = new File(value);
						if(home.isDirectory()) {
							Main.javaHome = value;
						}
					} else if(pname.equalsIgnoreCase("javaExec")) {
						File java = new File(value);
						if(java.isFile()) {
							Main.javaExecutable = value;
						}
					} else if(pname.equalsIgnoreCase("remAdminListenPort")) {
						if(StringUtil.isStrLong(value)) {
							int port = Long.valueOf(value).intValue();
							if(port >= 0 && port < 65535) {
								RemoteAdmin.listenPort = port;
							}
						}
					} else if(pname.equalsIgnoreCase("enableScheduledRestarts")) {
						Main.enableScheduledRestarts = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("restartCommands")) {
						Main.restartCommands = value.replace("<newline>", "\r\n");
					} else if(pname.equalsIgnoreCase("enableRemoteAdministration")) {
						RemoteAdmin.enableRemoteAdministration = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("monday")) {
						Main.scheduledRestartData.monday = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("tuesday")) {
						Main.scheduledRestartData.tuesday = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("wednesday")) {
						Main.scheduledRestartData.wednesday = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("thursday")) {
						Main.scheduledRestartData.thursday = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("friday")) {
						Main.scheduledRestartData.friday = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("saturday")) {
						Main.scheduledRestartData.saturday = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("sunday")) {
						Main.scheduledRestartData.sunday = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("enable1")) {
						Main.scheduledRestartData.enable1 = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("enable2")) {
						Main.scheduledRestartData.enable2 = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("enable3")) {
						Main.scheduledRestartData.enable3 = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("enable4")) {
						Main.scheduledRestartData.enable4 = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("enable5")) {
						Main.scheduledRestartData.enable5 = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("enable6")) {
						Main.scheduledRestartData.enable6 = Boolean.valueOf(value).booleanValue();
					} else if(pname.equalsIgnoreCase("hour1")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.hour1 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("hour2")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.hour2 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("hour3")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.hour3 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("hour4")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.hour4 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("hour5")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.hour5 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("hour6")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.hour6 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("minute1")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.minute1 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("minute2")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.minute2 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("minute3")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.minute3 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("minute4")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.minute4 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("minute5")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.minute5 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("minute6")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.minute6 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("second1")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.second1 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("second2")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.second2 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("second3")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.second3 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("second4")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.second4 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("second5")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.second5 = Integer.valueOf(value).intValue();
						}
					} else if(pname.equalsIgnoreCase("second6")) {
						if(StringUtil.isStrInt(value)) {
							Main.scheduledRestartData.second6 = Integer.valueOf(value).intValue();
						}
					}
				}
			}
			Main.updateConsoleFont();
			return true;
		} catch(Throwable e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static final boolean saveSettings() {
		try(PrintWriter pr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(settingsFile), StandardCharsets.UTF_8), true)) {
			pr.println("# Console font settings:");
			pr.println("fontName=" + Main.consoleFontName);
			pr.println("fontSize=" + Main.consoleFontSize);
			pr.println("fontBold=" + Main.consoleFontBold);
			pr.println("fontItalicized=" + Main.consoleFontItalicized);
			pr.println("fontStrikeout=" + Main.consoleFontStrikeout);
			pr.println("fontUnderLined=" + Main.consoleFontUnderLined);
			pr.println("");
			pr.println("# Minecraft server configuration:");
			pr.println("automaticServerStartup=" + Main.automaticServerStartup);
			pr.println("automaticServerRestart=" + Main.automaticServerRestart);
			pr.println("alwaysShowTrayIcon=" + Main.alwaysShowTrayIcon);
			final String sJarPath = Main.serverJar.getAbsolutePath();//The following extra code allows for saving a relative file path if the user moves their server files around a lot(e.g. removable drive, rename a parent folder, etc.)
			int indexOfDot = sJarPath.indexOf(".\\") == -1 ? sJarPath.indexOf("./") : sJarPath.indexOf(".\\");
			pr.println("serverJar=" + (indexOfDot != -1 ? sJarPath.substring(indexOfDot) : sJarPath));
			pr.println("vmArgs=" + Main.txtVmArgs.getText());
			pr.println("useG1GC=" + Main.btnUseG1GC.getSelection());
			pr.println("progArgs=" + Main.txtProgramArgs.getText());
			pr.println("javaHome=" + JavaProgramArguments.getArguments().javaHome);
			pr.println("javaExec=" + Main.javaExecutable);
			pr.println("");
			pr.println("# Remote administration settings:");
			pr.println("remAdminListenPort=" + RemoteAdmin.listenPort);
			pr.println("enableRemoteAdministration=" + RemoteAdmin.enableRemoteAdministration);
			pr.println("");
			pr.println("# Scheduled restart settings:");
			pr.println("enableScheduledRestarts=" + Main.enableScheduledRestarts);
			pr.println("restartCommands=" + Main.restartCommands.replace("\r\n", "<newline>"));
			pr.println(Main.scheduledRestartData.toString());
			pr.println();
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	protected static final void appendLog(String str) {
		boolean sendToClients = !str.startsWith("==");
		if(!sendToClients) {
			str = str.substring(2);
		}
		try {
			if(!Main.wrapperLog.isDisposed()) {
				if(Main.wrapperLog.getText().isEmpty()) {
					Main.wrapperLog.setText(str + "\r\n");
				} else {
					Main.wrapperLog.setText(Main.wrapperLog.getText() + "\r\n" + str);
				}
				Main.wrapperLog.setTopIndex(Main.wrapperLog.getLineCount() - 1);
			}
			if(sendToClients) {
				for(String s : str.split(Pattern.quote("\n"))) {
					s = s.endsWith("\r") ? s.substring(0, s.length() - 1) : s;
					if(!s.trim().isEmpty()) {
						if(!s.startsWith("==")) {
							RemoteClient.sendLogToClients("WARN: " + s);
						}
					}
				}
			}
		} catch(Throwable ignored) {
			logsToAppend.add(str);
		}
	}
	
	protected static final void updateConsoleFont() {
		if(Main.shell == null || Main.shell.isDisposed() || Main.output == null) {
			return;
		}
		final Font font = SWTResourceManager.getFont(Main.consoleFontName, Main.consoleFontSize, (Main.consoleFontItalicized ? SWT.ITALIC : SWT.NORMAL), Main.consoleFontStrikeout, Main.consoleFontUnderLined);
		if(font != null) {
			if(Main.consoleFontBold) {
				Main.output.setFont(SWTResourceManager.getBoldFont(font));
			} else {
				Main.output.setFont(font);
			}
		} else {
			Main.appendLog("==The font \"" + Main.consoleFontName + "\" was not found or did not load.");
		}
	}
	
	@SuppressWarnings("unused")
	private static final void createContents() {
		final Point shellSize = shell.getSize();
		Main.updateConsoleFont();
		
		tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		tabFolder.setBounds(384, 56, 500, 472);
		
		TabItem consoleTab = new TabItem(tabFolder, SWT.NONE);
		consoleTab.setToolTipText("View the server console");
		consoleTab.setText("Console Output");
		
		consoleOutputTabComposite = new Composite(tabFolder, SWT.NONE);
		consoleOutputTabComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		consoleTab.setControl(consoleOutputTabComposite);
		output = new StyledText(consoleOutputTabComposite, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		output.setBounds(0, 0, 492, 413);
		output.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		output.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		
		error = new StyledText(shell, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		error.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		error.setBounds(10, 345, 360, 183);
		
		btnSendInput = new Button(consoleOutputTabComposite, SWT.NONE);
		btnSendInput.setBounds(340, 417, 75, 25);
		btnSendInput.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					Main.handleInput(null, inputField.getText());
				} catch(Throwable e) {
					appendLog("==Failed to send input to server process's input stream: " + Functions.throwableToStr(e));
				}
				inputField.setText("");
			}
		});
		btnSendInput.setText("Send input");
		
		inputField = new Text(consoleOutputTabComposite, SWT.BORDER);
		inputField.setBounds(0, 417, 334, 25);
		
		btnTab = new Button(consoleOutputTabComposite, SWT.NONE);
		btnTab.setToolTipText("This feature is not implemented yet, as I can't figure out how to make the server recognize the tab.");
		btnTab.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(isProcessAlive()) {
					process.input.print(inputField.getText() + "\t");//Haha, nope. Not how it works :/
					process.input.flush();
				}
			}
		});
		btnTab.setBounds(421, 417, 71, 25);
		btnTab.setText("TAB");
		btnTab.setEnabled(false);//Sad face.
		
		TabItem tbtmResourceUsage = new TabItem(tabFolder, SWT.NONE);
		tbtmResourceUsage.setToolTipText("Monitor system resources used by the server");
		tbtmResourceUsage.setText("Resource Usage");
		
		resourceTabComposite = new Composite(tabFolder, SWT.NONE);
		resourceTabComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		tbtmResourceUsage.setControl(resourceTabComposite);
		
		ramUsageComposite = new Composite(resourceTabComposite, SWT.NONE);
		ramUsageComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		ramUsageComposite.setBounds(10, 84, 472, 68);
		
		lblRamUsage = new Label(ramUsageComposite, SWT.NONE);
		lblRamUsage.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		lblRamUsage.setBounds(10, 10, 108, 15);
		lblRamUsage.setText("RAM Usage:");
		
		ramUsageBar = new ProgressBar(ramUsageComposite, SWT.NONE);
		ramUsageBar.setForeground(SWTResourceManager.getColor(51, 153, 255));
		ramUsageBar.setBounds(124, 31, 338, 27);
		
		ramUsageTxt = new Label(ramUsageComposite, SWT.WRAP);
		ramUsageTxt.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		ramUsageTxt.setBounds(10, 31, 108, 27);
		
		lblThreadCount = new Label(ramUsageComposite, SWT.NONE);
		lblThreadCount.setBounds(124, 10, 85, 15);
		lblThreadCount.setText("Thread Count:");
		lblThreadCount.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		
		threadCountTxt = new Label(ramUsageComposite, SWT.NONE);
		threadCountTxt.setBounds(215, 10, 114, 15);
		threadCountTxt.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		
		cpuUsageComposite = new Composite(resourceTabComposite, SWT.NONE);
		cpuUsageComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		cpuUsageComposite.setBounds(10, 10, 472, 68);
		
		cpuUsageBar = new ProgressBar(cpuUsageComposite, SWT.NONE);
		cpuUsageBar.setForeground(SWTResourceManager.getColor(51, 153, 255));
		cpuUsageBar.setBounds(124, 10, 338, 48);
		
		Label lblCpuUsage = new Label(cpuUsageComposite, SWT.NONE);
		lblCpuUsage.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		lblCpuUsage.setBounds(10, 16, 75, 15);
		lblCpuUsage.setText("CPU Usage:");
		
		cpuUsageTxt = new Label(cpuUsageComposite, SWT.NONE);
		cpuUsageTxt.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		cpuUsageTxt.setBounds(10, 43, 108, 15);
		
		tbtmConnectedClients = new TabItem(tabFolder, SWT.NONE);
		tbtmConnectedClients.setText("Connected Clients");
		
		connectedClientsContent = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		connectedClientsContent.setExpandHorizontal(true);
		tbtmConnectedClients.setControl(connectedClientsContent);
		contentArea = new Composite(connectedClientsContent, SWT.NONE);
		contentArea.setSize(contentArea.getSize().x, (RemoteClient.instances.size() * ConnectedClientsComposite.yTotal));
		connectedClientsContent.setContent(contentArea);
		connectedClientsContent.setMinHeight(85);
		noConnectedClientsMsg = new Label(contentArea, SWT.BORDER | SWT.WRAP);
		noConnectedClientsMsg.setBounds(10, 10, contentArea.getSize().x - 35, 30);//455, 30);
		noConnectedClientsMsg.setText(noConnText);
		
		//ConnectedClientsComposite test = new ConnectedClientsComposite(contentArea, null, "ipAddress", "Nickname");
		
		final KeyAdapter inputKeyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				try {
					if(event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {
						event.doit = false;
						int size = inputtedCommands.size();
						int pos = selectedCommand.getValue().intValue();
						if(event.keyCode == SWT.ARROW_UP) {
							if(pos - 1 >= 0) {
								selectedCommand.setValue(Integer.valueOf(pos - 1));
							}
						} else if(event.keyCode == SWT.ARROW_DOWN) {
							if(pos + 1 < size) {
								selectedCommand.setValue(Integer.valueOf(pos + 1));
							}
						}
						pos = selectedCommand.getValue().intValue();
						if(pos < 0 || pos >= size) {
							if(pos < 0) {
								pos = 0;
							} else if(pos >= size) {
								pos = size - 1;
							}
							selectedCommand.setValue(Integer.valueOf(pos));
						}
						String text = inputtedCommands.get(selectedCommand.getValue());
						if(text == null) {
							text = "";
						}
						inputField.setText(text);
						inputField.setFocus();
						inputField.setSelection(text.length());
					} else if(event.character == SWT.CR) {
						event.doit = false;
						final String input = inputField.getText();
						final Integer key = Integer.valueOf(inputtedCommands.size());
						String lastInput = inputtedCommands.get(selectedCommand.getValue());
						if(!input.equals(lastInput)) {
							selectedCommand.setValue(key);
							inputtedCommands.put(key, input);
						}
						
						String command = "";
						final String[] args;
						String mkArgs = "";
						for(String arg : input.split(" ")) {
							if(command.isEmpty()) {
								command = arg;
							} else {
								mkArgs += arg + " ";
							}
						}
						mkArgs = mkArgs.trim();
						if(mkArgs.isEmpty()) {
							args = new String[0];
						} else {
							args = mkArgs.split(" ");
						}
						if(command.equalsIgnoreCase("cls") && args.length == 0) {
							Main.out.dispose();
							inputField.setText("");
							return;
						}
						if(command.toLowerCase().startsWith("setfont")) {
							if(command.equalsIgnoreCase("setfont")) {
								if(args.length >= 1) {
									if(args.length == 1 && args[0].equals("?") || args[0].equalsIgnoreCase("help")) {
										appendLog("==Font commands:\r\nsetFont {font name]\r\nsetFontSize {size}\r\nsetFontBold {true|false}\r\nsetFontItalic {true|false}\r\nsetFontUnderline {true|false}\r\nsetFontStrikeout {true|false}\r\nsaveFontSettings");
									} else {
										consoleFontName = StringUtil.stringArrayToString(args, ' ');
									}
								}
							} else if(command.equalsIgnoreCase("setfontsize")) {
								if(args.length == 1) {
									if(StringUtil.isStrLong(args[0])) {
										int size = Long.valueOf(args[0]).intValue();
										if(size >= 0) {
											consoleFontSize = size;
										} else {
											appendLog("==Font sizes must be positive.");
										}
									} else {
										appendLog("==\"" + args[0] + "\" is not a valid integer.");
									}
								} else {
									appendLog("==Usage: \"setFontSize {size}\"");
								}
							} else if(command.equalsIgnoreCase("setfontbold")) {
								if(args.length == 1) {
									consoleFontBold = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("==Usage: \"setFontbold {true|false}\"");
								}
							} else if(command.equalsIgnoreCase("setfontitalic")) {
								if(args.length == 1) {
									consoleFontItalicized = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("==Usage: \"setFontItalic {true|false}\"");
								}
							} else if(command.equalsIgnoreCase("setfontunderline")) {
								if(args.length == 1) {
									consoleFontUnderLined = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("==Usage: \"setFontUnderline {true|false}\"");
								}
							} else if(command.equalsIgnoreCase("setfontstrikeout")) {
								if(args.length == 1) {
									consoleFontStrikeout = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("==Usage: \"setFontStrikeout {true|false}\"");
								}
							} else {
								appendLog("==Unknown font command. Type \"setfont ?\" for help.");
							}
							updateConsoleFont();
						} else if(command.equalsIgnoreCase("savesettings")) {
							if(saveSettings()) {
								appendLog("==Font settings saved.");
							} else {
								appendLog("==Something went wrong when saving the font settings.");
							}
						} else if(command.equalsIgnoreCase("loadsettings")) {
							if(loadSettings()) {
								appendLog("==Font settings loaded.");
							} else {
								appendLog("==Something went wrong when loading the font settings.");
							}
						} else {
							try {
								Main.handleInput(null, input);
							} catch(Throwable e) {
								e.printStackTrace();
							}
						}
						inputField.setText("");
					}
				} catch(Throwable e) {
					e.printStackTrace();
				}
			}
		};
		
		inputField.addKeyListener(inputKeyAdapter);
		output.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.character == SWT.CR) {// || e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
					inputKeyAdapter.keyPressed(e);
					return;
				}
				if(e.character < 32 && e.character != SWT.BS) {
					return;
				}
				e.doit = false;
				String text = inputField.getText();
				if(e.character == SWT.BS) {
					if(!text.isEmpty()) {
						text = text.substring(0, text.length() - 1);
					}
				} else {
					text += e.character;
				}
				inputField.setText(text);
				//ConsoleWindow.this.txtInputfield.setFocus();
				inputField.setSelection(inputField.getText().length());
			}
		});
		
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		
		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");
		
		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);
		
		mntmOpenServerFolder = new MenuItem(menu_1, SWT.NONE);
		mntmOpenServerFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if(isServerJarSelected()) {
					try {
						Desktop.getDesktop().open(serverJar.getParentFile());
					} catch(Throwable e) {
						System.err.print("Unable to browse to folder: ");
						e.printStackTrace();
					}
				}
			}
		});
		mntmOpenServerFolder.setText("&Open server folder");
		mntmOpenServerFolder.setAccelerator(SWT.CTRL | 'O');
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		mntmExittaltf = new MenuItem(menu_1, SWT.NONE);
		mntmExittaltf.setAccelerator(SWT.ALT | SWT.F4);
		mntmExittaltf.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shutdown();
			}
		});
		mntmExittaltf.setText("E&xit\t(Alt+F4)");
		
		MenuItem mntmOptions = new MenuItem(menu, SWT.CASCADE);
		mntmOptions.setText("Options");
		
		Menu menu_2 = new Menu(mntmOptions);
		mntmOptions.setMenu(menu_2);
		
		mntmSelectJavaExecutable = new MenuItem(menu_2, SWT.NONE);
		mntmSelectJavaExecutable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(shell, SWT.NONE);
				dialog.setText("Select Main Java Executable");
				dialog.setFileName("java");
				dialog.setFilterPath(Main.javaHome);
				String javaPath = dialog.open();
				if(javaPath != null) {
					File java = new File(javaPath);
					if(java.exists()) {
						Main.javaExecutable = java.getAbsolutePath();
					}
				}
			}
		});
		mntmSelectJavaExecutable.setText("&Select Java executable...");
		
		MenuItem menuItem = new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem mntmManageCredentials = new MenuItem(menu_2, SWT.NONE);
		mntmManageCredentials.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArrayList<Credential> list = new CredentialsManager(shell, new ArrayList<>(savedCredentials)).open();
				if(list != null) {
					for(Credential oldUser : savedCredentials) {
						oldUser.delete();
					}
					savedCredentials.clear();
					savedCredentials.addAll(list);
					for(Credential newUser : savedCredentials) {
						newUser.saveToFile();
					}
					System.gc();
				}
			}
		});
		mntmManageCredentials.setText("Manage Credentials...");
		
		MenuItem menuItem_1 = new MenuItem(menu_2, SWT.SEPARATOR);
		
		mntmHideWindow = new MenuItem(menu_2, SWT.NONE);
		mntmHideWindow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				hideShell(false);
			}
		});
		mntmHideWindow.setText("Hide Window");
		
		mntmTrayIconAlways = new MenuItem(menu_2, SWT.CHECK);
		mntmTrayIconAlways.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				alwaysShowTrayIcon = mntmTrayIconAlways.getSelection();
			}
		});
		mntmTrayIconAlways.setText("Tray Icon Always Active");
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		mntmRestartServerAutomatically = new MenuItem(menu_2, SWT.CHECK);
		mntmRestartServerAutomatically.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Main.automaticServerRestart = mntmRestartServerAutomatically.getSelection();
			}
		});
		mntmRestartServerAutomatically.setSelection(Main.automaticServerRestart);
		mntmRestartServerAutomatically.setText("Restart server automatically upon server exit");
		
		mntmStartServerAutomatically = new MenuItem(menu_2, SWT.CHECK);
		mntmStartServerAutomatically.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Main.automaticServerStartup = mntmStartServerAutomatically.getSelection();
			}
		});
		mntmStartServerAutomatically.setSelection(Main.automaticServerStartup);
		mntmStartServerAutomatically.setText("Start server automatically on startup");
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		mntmEnableScheduledRestarts = new MenuItem(menu_2, SWT.CHECK);
		mntmEnableScheduledRestarts.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableScheduledRestarts = mntmEnableScheduledRestarts.getSelection();
			}
		});
		mntmEnableScheduledRestarts.setText("Enable Scheduled Restarts");
		
		MenuItem mntmScheduledRestartingOptions = new MenuItem(menu_2, SWT.NONE);
		mntmScheduledRestartingOptions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TimeResult newData = new TimeResult(scheduledRestartData);//new FeatureNotImplementedDialog(shell).open("Scheduled Restarting Options");
				ScheduledRestartingOptionsDialog dialog = new ScheduledRestartingOptionsDialog(shell, newData, Main.restartCommands);
				Response result = dialog.open();
				if(result == Response.DONE) {
					scheduledRestartData = newData;
					restartCommands = dialog.commandsToRun;
					saveSettings();
				}
			}
		});
		mntmScheduledRestartingOptions.setText("Scheduled restarting options...");
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		mntmSendServerInfo = new MenuItem(menu_2, SWT.CHECK);
		mntmSendServerInfo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Main.sendServerInfoToClients = mntmSendServerInfo.getSelection();
			}
		});
		mntmSendServerInfo.setSelection(true);
		mntmSendServerInfo.setText("Send server info to clients");
		
		mntmCheckForUpdates = new MenuItem(menu, SWT.NONE);
		mntmCheckForUpdates.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				UpdateResult result = new UpdateCheckerDialog(shell).open();
				if(result.type == UpdateType.AVAILABLE) {
					File check = Main.getClassPathJarFile();
					if(check.getName().endsWith(".class")) {
						new PopupDialog(shell, "Update Results", "Heya! It looks like you're running this in a development environment.\r\nI can't overwrite a .jar if there isn't one!\r\nDisplaying normal output:\r\n\r\nThere is an update available!\r\nSize to download: " + Functions.humanReadableByteCount(result.fileSize, true, 2)).open();
					} else {
						PromptDownloadUpdateDialog dialog = new PromptDownloadUpdateDialog(shell);
						Response response = dialog.open("There is an update available!\r\nSize to download: " + Functions.humanReadableByteCount(result.fileSize, true, 2));
						if(response == Response.YES) {
							new PerformUpdateDialog(shell).open();
							//we're still here?!
							new PopupDialog(shell, "Update Warning", "Automatic update seems to have failed.\r\nYou may need to close and re-open this client.").open();
						}
					}
				} else if(result.type == UpdateType.UP_TO_DATE) {
					new PopupDialog(shell, "Update Results", "You are running the latest version of ServerWrapper.").open();
				} else if(result.type == UpdateType.NO_CONNECTION) {
					new PopupDialog(shell, "Update Results", "Unable to contact the update server; is the server down?\r\n\r\nPlease try again later.").open();
				} else {
					new PopupDialog(shell, "Update Results", "Unable to check for updates: An unknown error occurred.").open();
				}
			}
		});
		mntmCheckForUpdates.setText("Check for Updates...");
		
		mntmabout = new MenuItem(menu, SWT.NONE);
		mntmabout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!aboutDialogIsOpen) {
					aboutDialogIsOpen = true;
					new AboutDialog(shell).open();
					aboutDialogIsOpen = false;
				}
			}
		});
		mntmabout.setAccelerator(SWT.ALT | 'A');
		mntmabout.setText("&About...");
		
		verticalSeparator = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
		verticalSeparator.setBounds(376, 11, 2, shellSize.y - 75);
		
		horizontalSeparator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		horizontalSeparator.setBounds(10, 10, shellSize.x - 30, 2);
		
		Label lblStderr = new Label(shell, SWT.NONE);
		lblStderr.setBounds(10, 324, 95, 15);
		lblStderr.setText("Errors(Stderr):");
		
		Label lblServerJar = new Label(shell, SWT.NONE);
		lblServerJar.setBounds(10, 14, 55, 25);
		lblServerJar.setText("Server jar:");
		
		txtServerjar = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
		txtServerjar.setEditable(false);
		txtServerjar.setBounds(71, 14, 214, 25);
		
		btnChooseJar = new Button(shell, SWT.NONE);
		btnChooseJar.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(shell, SWT.NONE);
				dialog.setText("Choose server .jar");
				dialog.setFileName("minecraft_server.jar");
				dialog.setFilterExtensions(new String[] {"*.jar;*.exe", "*.jar", "*.exe"});
				final File jarParent = serverJar == null ? null : serverJar.getParentFile();
				dialog.setFilterPath(jarParent == null ? rootDir.getAbsolutePath() : jarParent.getAbsolutePath());
				String serverJarPath = dialog.open();
				if(serverJarPath != null) {
					File serverJar = new File(serverJarPath);
					if(serverJar.exists()) {
						Main.serverJar = serverJar;
					}
				}
			}
		});
		btnChooseJar.setBounds(291, 14, 79, 25);
		btnChooseJar.setText("Choose jar...");
		
		Label lblVmargs = new Label(shell, SWT.NONE);
		lblVmargs.setBounds(10, 45, 55, 25);
		lblVmargs.setText("VmArgs:");
		
		txtVmArgs = new Text(shell, SWT.BORDER);
		txtVmArgs.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if(btnUseG1GC != null) {//Fix NPE during initialization
					checkVMArgsForXms();
				}
			}
		});
		txtVmArgs.setText("-Xms512m -Xmx" + (Functions.isJvm64bit() ? "2048m" : "1024m"));
		txtVmArgs.setBounds(71, 45, 190, 25);
		
		btnUseG1GC = new Button(shell, SWT.CHECK);
		btnUseG1GC.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useG1GC = btnUseG1GC.getSelection();
				checkVMArgsForXms();
			}
		});
		btnUseG1GC.setBounds(267, 45, 103, 25);
		btnUseG1GC.setText("Use Java G1GC");
		
		Label lblProgramArgs = new Label(shell, SWT.NONE);
		lblProgramArgs.setBounds(10, 76, 79, 25);
		lblProgramArgs.setText("Program Args:");
		
		txtProgramArgs = new Text(shell, SWT.BORDER);
		txtProgramArgs.setBounds(95, 76, 275, 25);
		
		Label label_2 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_2.setBounds(10, 107, 368, 2);
		
		btnStartServer = new Button(shell, SWT.NONE);
		btnStartServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				launchServer(null);
			}
		});
		btnStartServer.setBounds(10, 115, 177, 25);
		btnStartServer.setText("Start Server");
		
		Label lblWrapperLog = new Label(shell, SWT.NONE);
		lblWrapperLog.setBounds(10, 177, 95, 15);
		lblWrapperLog.setText("Wrapper Log:");
		
		wrapperLog = new StyledText(shell, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		wrapperLog.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		wrapperLog.setEditable(false);
		wrapperLog.setBounds(10, 198, 360, 120);
		
		btnStopServer = new Button(shell, SWT.NONE);
		btnStopServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopServer(null, false);
			}
		});
		btnStopServer.setBounds(193, 115, 177, 25);
		btnStopServer.setText("Stop Server");
		
		btnRestartServer = new Button(shell, SWT.NONE);
		btnRestartServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				restartServer(null);
			}
		});
		btnRestartServer.setBounds(10, 146, 177, 25);
		btnRestartServer.setText("Restart Server");
		
		btnKillProcess = new Button(shell, SWT.NONE);
		btnKillProcess.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopServer(null, true);
			}
		});
		btnKillProcess.setBounds(193, 146, 177, 25);
		btnKillProcess.setText("Kill Process");
		
		horizontalSeparator2 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		horizontalSeparator2.setBounds(384, 48, 500, 2);
		
		Label lblRemoteAdministrationListen = new Label(shell, SWT.NONE);
		lblRemoteAdministrationListen.setBounds(384, 20, 189, 15);
		lblRemoteAdministrationListen.setText("Remote administration listen port:");
		
		serverListenPort = new Spinner(shell, SWT.BORDER);
		serverListenPort.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				serverListenPortAvailable = IOUtils.isPortAvailable(serverListenPort.getSelection());
			}
		});
		serverListenPort.setEnabled(false);
		serverListenPort.setMaximum(65535);
		serverListenPort.setBounds(579, 17, 75, 22);
		serverListenPort.setSelection(RemoteAdmin.listenPort);
		
		btnChangePort = new Button(shell, SWT.NONE);
		btnChangePort.setBounds(660, 17, 87, 22);
		btnChangePort.setText("Change port");
		
		lblPortWas = new Label(shell, SWT.NONE);
		lblPortWas.setBounds(753, 20, 125, 15);
		lblPortWas.setText("Was: " + RemoteAdmin.listenPort);
		lblPortWas.setVisible(false);
		
		btnEnableRemoteAdmin = new Button(shell, SWT.NONE);
		btnEnableRemoteAdmin.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RemoteAdmin.enableRemoteAdministration = true;
				startRemoteAdmin();
			}
		});
		btnEnableRemoteAdmin.setEnabled(false);
		btnEnableRemoteAdmin.setBounds(753, 17, 55, 22);
		btnEnableRemoteAdmin.setText("Enable");
		
		btnDisableRemoteAdmin = new Button(shell, SWT.NONE);
		btnDisableRemoteAdmin.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopRemoteAdmin();
			}
		});
		btnDisableRemoteAdmin.setText("Disable");
		btnDisableRemoteAdmin.setBounds(814, 17, 55, 22);
		
		btnChangePort.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(btnChangePort.getText().equals("Change port")) {
					serverListenPort.setEnabled(true);
					lblPortWas.setText("Was: " + RemoteAdmin.listenPort);
					lblPortWas.setVisible(true);
					btnEnableRemoteAdmin.setVisible(false);
					btnDisableRemoteAdmin.setVisible(false);
				} else {
					btnChangePort.setText("Change port");
					serverListenPort.setEnabled(false);
					lblPortWas.setVisible(false);
					btnEnableRemoteAdmin.setVisible(true);
					btnDisableRemoteAdmin.setVisible(true);
					mainLoop();
					if(!btnChangePort.getText().equals("Cancel") && serverListenPortAvailable) {
						changePort();
					} else {
						serverListenPort.setSelection(RemoteAdmin.listenPort);
					}
				}
			}
		});
		
		if(!isHeadless()) {
			final Tray tray = display.getSystemTray();
			if(tray != null) {
				trayIcon = new TrayIcon(shell, tray);
			}
		}
		appendLog("==Type 'cls' and press enter to clear the screen,\nor type 'setfont ?' and press enter to view font commands.");
	}
	
	protected static final void startRemoteAdmin() {
		if(RemoteAdmin.enableRemoteAdministration) {
			if(!RemoteAdmin.restartListenSocket()) {
				RemoteAdmin.listenPort++;
				if(RemoteAdmin.listenPort > 65535) {
					RemoteAdmin.listenPort = RemoteAdmin.defaultListenPort;
				}
				while(!RemoteAdmin.restartListenSocket()) {
					RemoteAdmin.listenPort++;
					if(RemoteAdmin.listenPort > 65535) {
						RemoteAdmin.listenPort = RemoteAdmin.defaultListenPort;
					}
					mainLoop();
					if(!RemoteAdmin.enableRemoteAdministration) {
						stopRemoteAdmin();//Just in case.
						break;
					}
				}
				serverListenPort.setSelection(RemoteAdmin.listenPort);
				serverListenPort.setEnabled(false);
				btnChangePort.setText("Change port");
				lblPortWas.setVisible(false);
				btnDisableRemoteAdmin.setVisible(true);
				btnEnableRemoteAdmin.setVisible(true);
				lblPortWas.setText("Was: " + RemoteAdmin.listenPort);
			}
		} else {
			stopRemoteAdmin();
		}
	}
	
	protected static final void stopRemoteAdmin() {
		RemoteAdmin.enableRemoteAdministration = false;
		RemoteAdmin.closeListenSocket();
	}
	
	protected static final void changePort() {
		int oldPort = RemoteAdmin.listenPort;
		RemoteAdmin.listenPort = serverListenPort.getSelection();
		if(oldPort != RemoteAdmin.listenPort) {
			RemoteAdmin.changeClientPorts();
			startRemoteAdmin();
		}
	}
	
	public static final void shutdown() {
		if(aboutDialogIsOpen) {
			return;
		}
		isRunning = false;
	}
	
	/** @return True if the current thread is the SWT main thread */
	public static final boolean checkThreadAccess() {
		return Thread.currentThread() == swtThread;
	}
	
	public static final void mainLoop() {
		if(!checkThreadAccess()) {
			Functions.sleep(10L);
			return;
		}
		if(!isRunning || display.isDisposed() || shell.isDisposed()) {
			return;
		}
		runClock();
		updateUI();
		runClock();
	}
	
	private static final void runClock() {
		if(!isRunning || display.isDisposed() || shell.isDisposed()) {
			return;
		}
		if(!display.readAndDispatch()) {
			shell.update();
			Functions.sleep(10L);//display.sleep();
		}
	}
	
	public static final boolean isServerJarSelected() {
		return serverJar != null && serverJar.exists();
	}
	
	public static final File getServerFolder() {
		return isServerJarSelected() ? Main.serverJar.getParentFile() : null;
	}
	
	public static final boolean isProcessAlive() {
		return process != null && process.process != null && process.process.isAlive();
	}
	
	public static final boolean hasProcessDied() {
		return hasProcessDied;
	}
	
	public static final String getServerState() {
		return serverState;
	}
	
	private static final void resizeToShellSize() {
		final Point shellSize = shell.getSize();//904 592
		Functions.setSizeFor(tabFolder, new Point(shellSize.x - 404, shellSize.y - 120));//new Point(shellSize.x - 404, shellSize.y - 172); <-- old console size settings
		final Point conOutComTabSize = consoleOutputTabComposite.getSize();
		final Point resourceComTabSize = resourceTabComposite.getSize();
		
		Point outputSize = new Point(conOutComTabSize.x, conOutComTabSize.y - 30);//new Point(shellSize.x - 412, shellSize.y - 179);//492 413
		Point errorSize = new Point(360, shellSize.y - 409);//378);
		Rectangle inputFieldBounds = new Rectangle(0, conOutComTabSize.y - 25, conOutComTabSize.x - 158, 25);//new Rectangle(0, shellSize.y - 173, shellSize.x - 493, 25);//419 411 25
		Point tabLocation = new Point(conOutComTabSize.x - 71, conOutComTabSize.y - 25);
		Point sendInputLocation = new Point(conOutComTabSize.x - 152, conOutComTabSize.y - 25);//75 25//new Point(shellSize.x - 487, shellSize.y - 89);//417 419
		Point vertSepSize = new Point(2, shellSize.y - 75);
		Point horiSepSize = new Point(shellSize.x - 30, 2);
		Point horiSepSize2 = new Point(shellSize.x - 404, 2);
		
		Functions.setSizeFor(cpuUsageComposite, new Point(resourceComTabSize.x - 20, cpuUsageComposite.getSize().y));
		Point cpuUsageBarSize = new Point(cpuUsageComposite.getSize().x - (cpuUsageBar.getLocation().x + 10), cpuUsageBar.getSize().y);
		Functions.setSizeFor(ramUsageComposite, new Point(resourceComTabSize.x - 20, ramUsageComposite.getSize().y));
		Point ramUsageBarSize = new Point(ramUsageComposite.getSize().x - (ramUsageBar.getLocation().x + 10), ramUsageBar.getSize().y);
		
		Functions.setSizeFor(output, outputSize);
		Functions.setSizeFor(error, errorSize);
		Functions.setBoundsFor(inputField, inputFieldBounds);
		Functions.setLocationFor(btnTab, tabLocation);
		Functions.setLocationFor(btnSendInput, sendInputLocation);
		Functions.setSizeFor(verticalSeparator, vertSepSize);
		Functions.setSizeFor(horizontalSeparator, horiSepSize);
		Functions.setSizeFor(horizontalSeparator2, horiSepSize2);
		
		Functions.setSizeFor(cpuUsageBar, cpuUsageBarSize);
		Functions.setSizeFor(ramUsageBar, ramUsageBarSize);
	}
	
	private static final boolean doesCalendarDayMatchSchedule() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(System.currentTimeMillis()));
		final int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		return (dayOfWeek == 1 && scheduledRestartData.sunday) || //
				(dayOfWeek == 2 && scheduledRestartData.monday) || //
				(dayOfWeek == 3 && scheduledRestartData.tuesday) || //
				(dayOfWeek == 4 && scheduledRestartData.wednesday) || //
				(dayOfWeek == 5 && scheduledRestartData.thursday) || //
				(dayOfWeek == 6 && scheduledRestartData.friday) || //
				(dayOfWeek == 7 && scheduledRestartData.saturday);
	}
	
	private static final boolean doesTimeMatchSchedule() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(System.currentTimeMillis()));
		final int hour = c.get(Calendar.HOUR_OF_DAY);
		final int minute = c.get(Calendar.MINUTE);
		final int second = c.get(Calendar.SECOND);
		return (scheduledRestartData.enable1 && scheduledRestartData.hour1 == hour && scheduledRestartData.minute1 == minute && scheduledRestartData.second1 == second) || //
				(scheduledRestartData.enable2 && scheduledRestartData.hour2 == hour && scheduledRestartData.minute2 == minute && scheduledRestartData.second2 == second) || //
				(scheduledRestartData.enable3 && scheduledRestartData.hour3 == hour && scheduledRestartData.minute3 == minute && scheduledRestartData.second3 == second) || //
				(scheduledRestartData.enable4 && scheduledRestartData.hour4 == hour && scheduledRestartData.minute4 == minute && scheduledRestartData.second4 == second) || //
				(scheduledRestartData.enable5 && scheduledRestartData.hour5 == hour && scheduledRestartData.minute5 == minute && scheduledRestartData.second5 == second) || //
				(scheduledRestartData.enable6 && scheduledRestartData.hour6 == hour && scheduledRestartData.minute6 == minute && scheduledRestartData.second6 == second);
	}
	
	private static final boolean needToRestartProcess() {
		return enableScheduledRestarts && (doesCalendarDayMatchSchedule() && doesTimeMatchSchedule() && !isProcessBeingStarted);
	}
	
	private static final void updateUI() {
		if(updateShellAppearance) {
			updateShellAppearance();
		}
		if(checkVMArgsForXms) {
			checkVMArgsForXms();
		}
		resizeToShellSize();
		serverState = (Main.isProcessAlive() ? "ACTIVE" : (Main.hasProcessDied() ? "DEAD" : "NOT_STARTED")) + "," + (Main.isServerJarSelected() ? "SELECTED" : "NOT-SELECTED");
		final boolean serverJarExists = isServerJarSelected();
		final boolean processIsAlive = isProcessAlive();
		if(!processIsAlive && startServer) {
			launchServer(null);
		} else {
			startServer = false;
		}
		if(processIsAlive) {
			if(process.cpuUsage != -1L) {
				int progress = Double.valueOf(Math.round(process.cpuUsage)).intValue();
				if(cpuUsageBar.getSelection() != progress) {
					cpuUsageBar.setSelection(progress);
				}
				final String cpuStr = Functions.roundToStr(process.cpuUsage) + "%";
				if(!cpuUsageTxt.getText().equals(cpuStr)) {
					cpuUsageTxt.setText(cpuStr);
				}
			} else {
				if(!cpuUsageTxt.getText().equals("[No data available.]")) {
					cpuUsageTxt.setText("[No data available.]");
				}
				if(cpuUsageBar.getSelection() != 0) {
					cpuUsageBar.setSelection(0);
				}
			}
			if(process.usedRam != -1L && process.committedRam != -1L) {
				int progress = Math.round(((process.usedRam + 0.00F) / (process.committedRam + 0.00F)) * 100.00F);
				if(ramUsageBar.getSelection() != progress) {
					ramUsageBar.setSelection(progress);
				}
				final String ramStr = Functions.humanReadableByteCount(process.usedRam, true, 2) + " / " + Functions.humanReadableByteCount(process.committedRam, true, 2);
				if(!ramUsageTxt.getText().equals(ramStr)) {
					ramUsageTxt.setText(ramStr);
				}
			} else {
				if(ramUsageBar.getSelection() != 0) {
					ramUsageBar.setSelection(0);
				}
				if(!ramUsageTxt.getText().equals("[No data available.]")) {
					ramUsageTxt.setText("[No data available.]");
				}
			}
			if(process.threadCount != -1) {
				if(!threadCountTxt.getText().equals("" + process.threadCount)) {
					threadCountTxt.setText("" + process.threadCount);
				}
			} else {
				if(!threadCountTxt.getText().equals("[No data available.]")) {
					threadCountTxt.setText("[No data available.]");
				}
			}
		} else {
			if(!cpuUsageTxt.getText().equals("[No data available.]")) {
				cpuUsageTxt.setText("[No data available.]");
			}
			if(cpuUsageBar.getSelection() != 0) {
				cpuUsageBar.setSelection(0);
			}
			if(!ramUsageTxt.getText().equals("[No data available.]")) {
				ramUsageTxt.setText("[No data available.]");
			}
			if(ramUsageBar.getSelection() != 0) {
				ramUsageBar.setSelection(0);
			}
			if(!threadCountTxt.getText().equals("[No data available.]")) {
				threadCountTxt.setText("[No data available.]");
			}
		}
		
		String serverJarText = serverJarExists ? serverJar.getAbsolutePath() : "";
		if(!txtServerjar.getText().equals(serverJarText)) {
			txtServerjar.setText(serverJarText);
		}
		btnStartServer.setEnabled(serverJarExists ? process == null : false);
		btnStopServer.setEnabled(processIsAlive);
		btnRestartServer.setEnabled(processIsAlive);
		btnKillProcess.setEnabled(processIsAlive);
		//inputField.setEnabled(processIsAlive);
		if(!inputField.getEnabled() && !inputField.getText().isEmpty()) {
			inputField.setText("");
		}
		//btnSendInput.setEnabled(processIsAlive);
		txtVmArgs.setEnabled(!processIsAlive);
		btnUseG1GC.setEnabled(!processIsAlive);
		txtProgramArgs.setEnabled(!processIsAlive);
		btnChooseJar.setEnabled(!processIsAlive);
		if(mntmExittaltf.getEnabled() == aboutDialogIsOpen) {
			mntmExittaltf.setEnabled(!aboutDialogIsOpen);
		}
		mntmSelectJavaExecutable.setEnabled(!processIsAlive);
		mntmStartServerAutomatically.setSelection(Main.automaticServerStartup);
		mntmRestartServerAutomatically.setSelection(Main.automaticServerRestart);
		mntmEnableScheduledRestarts.setSelection(Main.enableScheduledRestarts);
		mntmSendServerInfo.setSelection(Main.sendServerInfoToClients);
		mntmOpenServerFolder.setEnabled(serverJarExists);
		setTextFor(output);
		setTextFor(error);
		
		if(alwaysShowTrayIcon != mntmTrayIconAlways.getSelection()) {
			mntmTrayIconAlways.setSelection(alwaysShowTrayIcon);
		}
		
		if(trayIcon != null) {
			if(!trayIcon.isDisposed()) {
				trayIcon.updateUI();
				if(!mntmHideWindow.isEnabled()) {
					mntmHideWindow.setEnabled(true);
				}
			} else {
				trayIcon = null;
			}
		} else {
			openShell();
			if(mntmHideWindow.isEnabled()) {
				mntmHideWindow.setEnabled(false);
			}
		}
		
		Functions.setSizeFor(noConnectedClientsMsg, new Point(contentArea.getSize().x - 35, noConnectedClientsMsg.getSize().y));
		if(RemoteAdmin.enableRemoteAdministration) {
			if(serverListenPort.isEnabled()) {
				String text = (serverListenPort.getSelection() == RemoteAdmin.listenPort || !serverListenPortAvailable) ? "Cancel" : "Done";
				if(!text.equals(btnChangePort.getText())) {
					btnChangePort.setText(text);
				}
			}
			if(!btnChangePort.isEnabled()) {
				btnChangePort.setEnabled(true);
				btnChangePort.setText("Change port");
				lblPortWas.setVisible(false);
				serverListenPort.setEnabled(false);
				serverListenPort.setSelection(RemoteAdmin.listenPort);
				btnDisableRemoteAdmin.setEnabled(true);
				btnDisableRemoteAdmin.setVisible(true);
				btnEnableRemoteAdmin.setEnabled(false);
				btnEnableRemoteAdmin.setVisible(true);
			}
			updateConnectedClientComposites();
			noConnectedClientsMsg.setVisible(RemoteClient.instances.size() == 0);
			if(!noConnectedClientsMsg.getText().equals(noConnText)) {
				noConnectedClientsMsg.setText(noConnText);
			}
			Functions.setSizeFor(contentArea, new Point(contentArea.getSize().x, Math.max((RemoteClient.instances.size() * ConnectedClientsComposite.yTotal), noConnectedClientsMsg.getSize().y + ConnectedClientsComposite.yInterval)));
		} else {
			if(!noConnectedClientsMsg.getText().equals(remAdminDisabled)) {
				noConnectedClientsMsg.setText(remAdminDisabled);
			}
			noConnectedClientsMsg.setVisible(true);
			btnDisableRemoteAdmin.setEnabled(false);
			btnDisableRemoteAdmin.setVisible(true);
			btnEnableRemoteAdmin.setEnabled(true);
			btnEnableRemoteAdmin.setVisible(true);
			lblPortWas.setVisible(false);
			serverListenPort.setEnabled(false);
			btnChangePort.setEnabled(false);
			if(!btnChangePort.getText().equals("Change port")) {
				btnChangePort.setText("Change port");
			}
			Functions.setSizeFor(contentArea, new Point(contentArea.getSize().x, noConnectedClientsMsg.getSize().y + ConnectedClientsComposite.yInterval));
		}
		if(process != null && process.process != null) {
			if(!process.process.isAlive()) {
				processExitCode = process.process.exitValue();
				hasProcessDied = true;
				appendLog("Process terminated with error code: " + process.process.exitValue());
				RemoteClient from = process.stopServerClient;
				process.stopServerClient = null;
				process = null;
				onServerShutdown(from);
				from = null;
			}
		}
		if(logsToAppend.size() > 0) {
			ArrayList<String> copy = new ArrayList<>(logsToAppend);
			logsToAppend.clear();
			for(String str : copy) {
				appendLog(str);
			}
		}
		if(needToRestartProcess()) {
			if(!scheduledRestartInEffect) {
				appendLog("Performing scheduled restart...");
				scheduledRestartInEffect = true;
				if(!isProcessAlive()) {
					startServer = true;
				} else {
					String[] split = restartCommands.split(Pattern.quote("\n"));
					for(String cmd : split) {
						cmd = cmd.trim();
						if(cmd.isEmpty()) {
							continue;
						}
						Main.handleInput(null, cmd);
					}
					final long startTime = System.currentTimeMillis();
					final long endTime = startTime + 10000L;
					while(System.currentTimeMillis() <= endTime) {
						Main.mainLoop();
					}
					stopServer(null, false);
					startServer = true;
					appendLog("Restart complete.");
				}
			}
		} else {
			scheduledRestartInEffect = false;
		}
	}
	
	protected static final void openShell() {
		if(isHeadless()) {
			return;
		}
		if(!shell.getVisible()) {
			shell.setVisible(true);
			shell.open();
			shell.layout();
		}
	}
	
	protected static final void hideShell(boolean skipCheck) {
		if(isHeadless()) {
			return;
		}
		Tray tray = display.getSystemTray();
		if(tray != null) {
			boolean check = skipCheck;
			if(!check) {
				ConfirmHideWindowDialog dialog = new ConfirmHideWindowDialog(shell);
				check = dialog.open() == Response.YES;
			}
			if(check) {
				if(shell.isVisible()) {
					shell.setVisible(false);
				}
				trayIcon.setVisible(true);
			}
		} else {
			SystemTrayNotAvailableDialog notify = new SystemTrayNotAvailableDialog(shell);
			notify.open();
		}
	}
	
	protected static final void updateConnectedClientComposites() {
		final String keyCheck = StringUtil.nextSessionId();
		int i = 0;
		for(RemoteClient client : RemoteClient.instances) {
			ConnectedClientsComposite comp = getCCCompositeForClient(client);
			comp.keyCheck = keyCheck;
			comp.updateUI(i);
			i++;
		}
		for(Control control : contentArea.getChildren()) {
			if(control instanceof ConnectedClientsComposite) {
				ConnectedClientsComposite comp = (ConnectedClientsComposite) control;
				if(!comp.keyCheck.equals(keyCheck)) {
					comp.dispose();
				}
			}
		}
		shell.update();//Fix for strange textbox and button behavior
	}
	
	private static final ConnectedClientsComposite getCCCompositeForClient(RemoteClient client) {
		for(Control control : contentArea.getChildren()) {
			if(control instanceof ConnectedClientsComposite) {
				ConnectedClientsComposite comp = (ConnectedClientsComposite) control;
				if(comp.ipAddress.equals(client.getIpAddress())) {
					return comp;
				}
			}
		}
		return new ConnectedClientsComposite(contentArea, client, client.getIpAddress(), client.getNickName());
	}
	
	protected static final class ConnectedClientsComposite extends Composite {
		
		public static final int		yInterval	= 10;
		public static final int		ySize		= 55;
		public static final int		yTotal		= ySize + yInterval;
		
		public final RemoteClient	client;
		
		public volatile String		keyCheck	= "";
		
		public final String			ipAddress;
		public volatile String		nickName;
		protected final Button		btnDisconnect;
		protected final Button		btnSendMessage;
		public final Text			messageText;
		public final Label			lblAddressText;
		public final Label			lblNameText;
		
		/** @param parent
		 * @param style */
		public ConnectedClientsComposite(Composite parent, RemoteClient client, String ipAddress, String nickName) {
			super(parent, SWT.BORDER);
			this.client = client;
			this.ipAddress = ipAddress;
			this.nickName = nickName;
			this.setBounds(10, 10, contentArea.getSize().x - 35, ySize);
			
			Label lblIpAddress = new Label(this, SWT.NONE);
			lblIpAddress.setBounds(10, 10, 66, 13);
			lblIpAddress.setText("Ip Address:");
			
			this.lblAddressText = new Label(this, SWT.NONE);
			this.lblAddressText.setBounds(82, 10, 90, 13);
			this.lblAddressText.setText(ipAddress);
			
			Label lblName = new Label(this, SWT.NONE);
			lblName.setBounds(10, 29, 49, 13);
			lblName.setText("Name:");
			
			this.lblNameText = new Label(this, SWT.NONE);
			this.lblNameText.setBounds(82, 29, 90, 13);
			this.lblNameText.setText(nickName);
			
			this.btnDisconnect = new Button(this, SWT.NONE);
			this.btnDisconnect.setBounds(this.getSize().x - 82, 10, 68, 32);
			this.btnDisconnect.setText("Disconnect");
			if(client != null) {
				this.btnDisconnect.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						try {
							client.sendPopupMessage("The server administrator has disconnected you.");
							ConnectedClientsComposite.this.btnSendMessage.setEnabled(false);
							ConnectedClientsComposite.this.messageText.setEnabled(false);
							ConnectedClientsComposite.this.btnDisconnect.setEnabled(false);
							long startTime = System.currentTimeMillis();
							long endTime = startTime + 500L;
							while(System.currentTimeMillis() <= endTime) {
								Main.mainLoop();
							}
							client.disconnect();
						} catch(IOException ignored) {
						}
					}
				});
			}
			this.btnSendMessage = new Button(this, SWT.NONE);
			if(client != null) {
				this.btnSendMessage.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						final String msg = ConnectedClientsComposite.this.messageText.getText();
						ConnectedClientsComposite.this.messageText.setText("");
						if(msg.trim().isEmpty()) {
							return;
						}
						try {
							client.sendPopupMessage(msg);
							appendLog("==Message sent to client \"" + client.getDisplayName(true) + "\".");
						} catch(Throwable e) {
							e.printStackTrace();
						}
					}
				});
			}
			
			this.messageText = new Text(this, SWT.BORDER | SWT.WRAP | SWT.MULTI);
			this.btnSendMessage.setBounds(178, 10, 88, 32);
			this.btnSendMessage.setText("Send Message");
			this.messageText.setBounds(272, 10, this.getSize().x - 360, 32);
		}
		
		public final void updateUI(int index) {
			if(this.messageText.getText().isEmpty()) {
				if(this.btnSendMessage.isEnabled()) {
					this.btnSendMessage.setEnabled(false);
				}
			} else {
				if(!this.btnSendMessage.isEnabled()) {
					this.btnSendMessage.setEnabled(true);
				}
			}
			this.nickName = this.client.getNickName();
			this.setBounds(this.getLocation().x, yInterval + (index * yTotal), contentArea.getSize().x - 35, ySize);
			this.btnDisconnect.setBounds(this.getSize().x - 82, 10, 68, 32);
			this.messageText.setBounds(272, 10, this.getSize().x - 360, 32);
			if(!this.lblNameText.getText().equals(this.nickName)) {
				this.lblNameText.setText(this.nickName);
			}
			shell.update();//Fix for strange textbox and button behavior
		}
		
	}
	
	protected static final class TrayIcon {
		private final Shell			shell;
		
		protected final TrayItem	icon;
		protected final Menu		menu;
		protected final MenuItem	mi;
		public final MenuItem		reStartServer;
		public final MenuItem		stopServer;
		private final MenuItem		mi3;
		
		/** Constructs a new instance of this class given its parent
		 * (which must be a <code>Shell</code>) and the system tray.
		 * 
		 * @param parent a shell which will be the parent of the new instance
		 * @param tray a composite control which will be the parent of the new
		 *            instance (cannot be null)
		 *
		 * @exception IllegalArgumentException
		 *                <ul>
		 *                <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
		 *                </ul>
		 * @exception SWTException
		 *                <ul>
		 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from
		 *                the thread that created the parent</li>
		 *                <li>ERROR_INVALID_SUBCLASS - if this class is not an
		 *                allowed subclass</li>
		 *                </ul>
		 *
		 * @see SWT
		 * @see Widget#checkSubclass
		 * @see Widget#getStyle */
		@SuppressWarnings("unused")
		public TrayIcon(final Shell parent, final Tray tray) {
			this.shell = parent;
			this.icon = new TrayItem(tray, SWT.NONE);
			
			this.menu = new Menu(this.shell, SWT.POP_UP);
			this.mi = new MenuItem(this.menu, SWT.PUSH);
			this.mi.setText("Restore Window");
			this.mi.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if(TrayIcon.this.mi.getText().equals("Restore Window")) {
						openShell();
					} else {
						hideShell(true);
					}
				}
			});
			new MenuItem(this.menu, SWT.SEPARATOR);
			this.reStartServer = new MenuItem(this.menu, SWT.PUSH);
			this.reStartServer.setText("(Re)Start Server");
			this.reStartServer.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if(isProcessAlive()) {
						restartServer(null);
					} else {
						launchServer(null);
					}
				}
			});
			this.stopServer = new MenuItem(this.menu, SWT.PUSH);
			this.stopServer.setText("Stop Server");
			this.stopServer.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if(isProcessAlive()) {
						stopServer(null, false);
					}
				}
			});
			new MenuItem(this.menu, SWT.SEPARATOR);
			this.mi3 = new MenuItem(this.menu, SWT.PUSH);
			this.mi3.setText("Close ServerWrapper");
			this.mi3.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					openShell();
					TrayIcon.this.dispose();
					shutdown();
				}
			});
			this.icon.addListener(SWT.MenuDetect, new Listener() {
				@Override
				public void handleEvent(Event event) {
					TrayIcon.this.menu.setVisible(true);
				}
			});
			this.icon.setImage(getDefaultImages()[0]);
			this.icon.setToolTipText("Window hidden - " + getDefaultShellTitle());
			this.icon.addSelectionListener(new SelectionAdapter() {
				@Override
				public final void widgetDefaultSelected(SelectionEvent event) {
					if(TrayIcon.this.mi.getText().equals("Restore Window")) {
						openShell();
					} else {
						hideShell(true);
					}
				}
			});
			this.menu.setDefaultItem(this.mi);
		}
		
		public final void updateUI() {
			final boolean isAlive = isProcessAlive();
			if(this.stopServer.isEnabled() != isAlive) {
				this.stopServer.setEnabled(isAlive);
			}
			String text = isAlive ? "Restart Server" : "Start Server";
			if(!this.reStartServer.getText().equals(text)) {
				this.reStartServer.setText(text);
			}
			String iconToolTipText = (this.shell.getVisible() ? "" : "Window hidden - ") + this.shell.getText();
			if(!this.icon.getToolTipText().equals(iconToolTipText)) {
				this.icon.setToolTipText(iconToolTipText);
			}
			String restoreMiText = alwaysShowTrayIcon ? (this.shell.getVisible() ? "Hide Window" : "Restore Window") : "Restore Window";
			if(!this.mi.getText().equals(restoreMiText)) {
				this.mi.setText(restoreMiText);
			}
			if(alwaysShowTrayIcon) {
				if(!this.isVisible()) {
					this.setVisible(true);
				}
			} else if(this.shell.getVisible() == this.isVisible()) {
				this.setVisible(!this.shell.getVisible());
			}
		}
		
		/** Returns the receiver's image if it has one, or null
		 * if it does not.
		 *
		 * @return the receiver's image
		 *
		 * @exception SWTException
		 *                <ul>
		 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
		 *                disposed</li>
		 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from
		 *                the thread that created the receiver</li>
		 *                </ul>
		*/
		public final Image getImage() {
			return this.icon.getImage();
		}
		
		/** Sets the receiver's image.
		 *
		 * @param image the new image
		 *
		 * @exception IllegalArgumentException
		 *                <ul>
		 *                <li>ERROR_INVALID_ARGUMENT - if the image has been
		 *                disposed</li>
		 *                </ul>
		 * @exception SWTException
		 *                <ul>
		 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
		 *                disposed</li>
		 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from
		 *                the thread that created the receiver</li>
		 *                </ul>
		*/
		public final void setImage(Image image) {
			this.icon.setImage(image);
		}
		
		/** Returns <code>true</code> if the receiver is visible and
		 * <code>false</code> otherwise.
		 *
		 * @return the receiver's visibility
		 *
		 * @exception SWTException
		 *                <ul>
		 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
		 *                disposed</li>
		 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from
		 *                the thread that created the receiver</li>
		 *                </ul>
		*/
		public final boolean isVisible() {
			return this.icon.getVisible();
		}
		
		/** Makes the receiver visible if the argument is <code>true</code>,
		 * and makes it invisible otherwise.
		 *
		 * @param visible the new visibility state
		 *
		 * @exception SWTException
		 *                <ul>
		 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
		 *                disposed</li>
		 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from
		 *                the thread that created the receiver</li>
		 *                </ul>
		*/
		public final void setVisible(boolean visible) {
			this.icon.setVisible(visible);
		}
		
		/** Returns <code>true</code> if the widget has been disposed,
		 * and <code>false</code> otherwise.
		 * <p>
		 * This method gets the dispose state for the widget. When a widget has
		 * been disposed, it is an error to invoke any other method (except
		 * {@link #dispose()}) using the widget.
		 * </p>
		 *
		 * @return <code>true</code> when the widget is disposed and
		 *         <code>false</code> otherwise */
		public final boolean isDisposed() {
			return this.icon.isDisposed();
		}
		
		/** Disposes of the operating system resources associated with
		 * the receiver and all its descendants. After this method has
		 * been invoked, the receiver and all descendants will answer
		 * <code>true</code> when sent the message <code>isDisposed()</code>.
		 * Any internal connections between the widgets in the tree will
		 * have been removed to facilitate garbage collection.
		 * This method does nothing if the widget is already disposed.
		 * <p>
		 * NOTE: This method is not called recursively on the descendants of the
		 * receiver. This means that, widget implementers can not detect when a
		 * widget is being disposed of by re-implementing this method, but
		 * should instead listen for the <code>Dispose</code> event.
		 * </p>
		 *
		 * @exception SWTException
		 *                <ul>
		 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from
		 *                the thread that created the receiver</li>
		 *                </ul>
		 *
		 * @see Widget#addDisposeListener
		 * @see Widget#removeDisposeListener
		 * @see Widget#checkWidget */
		public final void dispose() {
			this.icon.dispose();
			this.menu.dispose();
		}
		
	}
}
