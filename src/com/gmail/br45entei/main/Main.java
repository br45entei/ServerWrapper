package com.gmail.br45entei.main;

import com.gmail.br45entei.data.Credentials;
import com.gmail.br45entei.data.DisposableByteArrayOutputStream;
import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.data.RemoteClient;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.IOUtils;
import com.gmail.br45entei.util.JavaProgramArguments;
import com.gmail.br45entei.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
public class Main {
	
	protected static volatile Thread						swtThread;
	public static final File								rootDir					= new File(System.getProperty("user.dir"));
	
	public static volatile String							javaHome;
	public static volatile String							javaExecutable;
	public static volatile File								serverJar				= null;													//new File(rootDir, "minecraft_server.jar");
																																				
	protected static volatile boolean						isRunning				= false;
	
	protected static Display								display;
	protected static Shell									shell;
	protected static StyledText								output;
	protected static StyledText								error;
	protected static Text									inputField;
	
	protected static MenuItem								mntmSelectJavaExecutable;
	
	//Console font settings ================
	
	protected static volatile String						consoleFontName			= "Consolas";
	protected static volatile int							consoleFontSize			= 8;
	protected static volatile boolean						consoleFontBold			= false;
	protected static volatile boolean						consoleFontStrikeout	= false;
	protected static volatile boolean						consoleFontUnderLined	= false;
	protected static volatile boolean						consoleFontItalicized	= true;
	
	protected static final File								settingsFile			= new File(rootDir, "settings.txt");
	
	//=============
	
	protected static final ArrayList<String>				logsToAppend			= new ArrayList<>();
	
	protected static final HashMap<Integer, String>			inputtedCommands		= new HashMap<>();
	protected static final Property<Integer>				selectedCommand			= new Property<>("Selected Command", Integer.valueOf(0));
	protected static final DisposableByteArrayOutputStream	out						= new DisposableByteArrayOutputStream();
	protected static final Property<String>					outTxt					= new Property<>("Console Text", "");
	protected static volatile int							numOfLinesInOutTxt		= 0;
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
																										numOfLinesInOutTxt = numOfLines;
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
																										text = t + ">";
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
																										Functions.sleep(15L);
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
																										text = t;// + ">";
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
	
	public static final int getNumberOfConsoleLogs() {
		return numOfLinesInOutTxt;//StringUtil.getNumOfLinesInStr(outTxt.getValue());
	}
	
	static {
		outTxtUpdateThread.setDaemon(true);
		errTxtUpdateThread.setDaemon(true);
	}
	
	protected static volatile ProcessIO	process;
	protected static volatile int		processExitCode	= 0;
	protected static volatile boolean	hasProcessDied	= false;
	protected static volatile boolean	startServer		= false;
	private static volatile String		serverState;
	
	protected static Button				btnSendInput;
	protected static Text				txtServerjar;
	protected static Text				txtVmArgs;
	protected static Text				txtProgramArgs;
	protected static StyledText			wrapperLog;
	protected static Button				btnStartServer;
	protected static Button				btnStopServer;
	protected static Button				btnChooseJar;
	protected static Label				verticalSeparator;
	protected static Label				horizontalSeparator;
	protected static Label				horizontalSeparator2;
	protected static Spinner			serverListenPort;
	protected static volatile boolean	serverListenPortAvailable;
	protected static Button				btnChangePort;
	protected static Label				lblPortWas;
	protected static Button				btnDisableRemoteAdmin;
	protected static Button				btnEnableRemoteAdmin;
	
	public static final boolean isRunning() {
		return isRunning;
	}
	
	/** Launch the application.
	 * 
	 * @param args System command arguments */
	public static void main(String[] args) {
		JavaProgramArguments.initializeFromMainClass(Main.class, args);
		javaHome = JavaProgramArguments.getArguments().javaHome;
		javaExecutable = JavaProgramArguments.getArguments().javaExecutable;
		swtThread = Thread.currentThread();
		
		display = Display.getDefault();
		shell = new Shell(display, SWT.SHELL_TRIM);
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				shutdown();
			}
		});
		shell.setSize(904, 592);
		shell.setMinimumSize(904, 592);
		shell.setText("Minecraft Server Wrapper - Made by Brian_Entei");
		shell.setImages(new Image[] {SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-16x16.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-32x32.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-64x64.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-128x128.png")});
		Functions.centerShellOnPrimaryMonitor(shell);
		
		createContents();
		
		loadSettings();
		RemoteAdmin.setupListenSocket();
		Credentials.initialize(rootDir);
		Credentials.loadInstancesFromFile();
		outTxtUpdateThread.start();
		errTxtUpdateThread.start();
		
		isRunning = true;
		startRemoteAdmin();
		openShell();
		if(serverJar != null && serverJar.isFile()) {//Automatic startup if jar file is loaded from config
			launchServer();
		}
		while(isRunning && !shell.isDisposed()) {
			mainLoop();
		}
		if(process != null && process.process.isAlive()) {
			handleInput("save-all\nstop");//process.process.destroyForcibly();
			isRunning = true;//kek
			openShell();
			final long startTime = System.currentTimeMillis();
			final long waitTime = 10000L;
			long lastSecond = startTime;
			int secondsWaited = 10;
			appendLog("Waiting 10 seconds for server to shutdown:");
			while(process != null && process.process.isAlive()) {
				mainLoop();
				final long now = System.currentTimeMillis();
				long elapsedTime = now - startTime;
				long elapsedSecondTime = now - lastSecond;
				if(elapsedSecondTime >= 1000L) {
					lastSecond = now;
					appendLog("\t" + --secondsWaited + " seconds remaining");
				}
				if(elapsedTime > waitTime) {
					if(process != null) {
						process.process.destroyForcibly();
					}
					break;
				}
			}
			isRunning = false;//un-kek
		}
		saveSettings();
		display.dispose();
		Credentials.saveInstancesToFile();
		System.exit(0);
	}
	
	public static final void launchServer() {
		if(isProcessAlive() || !isServerJarSelected()) {
			startServer = false;//Prevents potential loops
			return;
		}
		if(!checkThreadAccess()) {
			startServer = true;
			return;
		}
		try {
			hasProcessDied = false;
			processExitCode = 0;
			Main.out.dispose();
			Main.err.dispose();
			RemoteClient.resetClientLogs();
			wrapperLog.setText("");
			final String jarPath = serverJar.getAbsolutePath();
			final String jarCmdLine = " -jar " + (jarPath.contains(" ") ? "\"" + jarPath + "\"" : jarPath).trim();
			final String vmArgsCmdLine = txtVmArgs.getText().trim().isEmpty() ? "" : " " + txtVmArgs.getText().trim();
			final String progArgsCmdLine = txtProgramArgs.getText().trim().isEmpty() ? "" : " " + txtProgramArgs.getText().trim();
			String command = "\"" + javaExecutable + "\"" + vmArgsCmdLine + jarCmdLine + progArgsCmdLine + (progArgsCmdLine.isEmpty() ? " nogui" : (!progArgsCmdLine.contains("nogui") ? " nogui" : ""));
			ProcessBuilder builder = new ProcessBuilder(command.split(Pattern.quote(" ")));
			appendLog("Launch command:\r\n\t" + command);
			builder.redirectOutput(Redirect.PIPE);
			builder.redirectError(Redirect.PIPE);
			builder.redirectInput(Redirect.PIPE);
			builder.directory(serverJar.getParentFile());
			process = new ProcessIO(builder.start());
			startServer = false;
		} catch(IOException e) {
			appendLog("Failed to launch server: " + Functions.throwableToStr(e));
		}
	}
	
	public static final void stopServer() {
		if(process != null && process.process.isAlive()) {
			handleInput("save-all\nstop");//process.process.destroyForcibly();
			final long startTime = System.currentTimeMillis();
			final long waitTime = 10000L;
			long lastSecond = startTime;
			int secondsWaited = 10;
			appendLog("Waiting 10 seconds for server to shutdown:");
			while(process != null && process.process.isAlive()) {
				mainLoop();
				final long now = System.currentTimeMillis();
				long elapsedTime = now - startTime;
				long elapsedSecondTime = now - lastSecond;
				if(elapsedSecondTime >= 1000L) {
					lastSecond = now;
					appendLog("\t" + --secondsWaited + " seconds remaining");
				}
				if(elapsedTime > waitTime) {
					if(process != null) {
						process.process.destroyForcibly();
					}
					break;
				}
			}
		}
	}
	
	public static final void addLogToConsole(String log) {
		RemoteClient.sendLogToClients(log);
		byte[] data = (log + "\r\n").getBytes(StandardCharsets.UTF_8);
		Main.out.write(data, 0, data.length);
	}
	
	public static final class ProcessIO {
		public final Process		process;
		
		public final InputStream	out;
		public final InputStream	err;
		public final OutputStream	in;
		public final PrintWriter	input;
		
		public ProcessIO(Process process) {
			this.process = process;
			this.out = process.getInputStream();
			this.err = process.getErrorStream();
			this.in = process.getOutputStream();
			this.input = new PrintWriter(new OutputStreamWriter(this.in, StandardCharsets.UTF_8), true);
			Thread outReader = new Thread() {
				@Override
				public final void run() {
					final ProcessIO THIS = ProcessIO.this;
					byte[] buf = new byte[4096];
					int read;
					while(THIS.process.isAlive()) {
						try {
							String line = StringUtil.readLine(THIS.out);
							if(line != null) {
								addLogToConsole(line);
								/*read = THIS.out.read(buf);
								if(read != -1) {
									Main.out.write(buf, 0, read);
								} else {
									break;
								}*/
							}
						} catch(IOException e) {
							e.printStackTrace();
							break;
						}
					}
					//System.out.println("Out stream ended.");
				}
			};
			Thread errReader = new Thread() {
				@Override
				public final void run() {
					final ProcessIO THIS = ProcessIO.this;
					byte[] buf = new byte[4096];
					int read;
					while(THIS.process.isAlive()) {
						try {
							read = THIS.err.read(buf);
							if(read != -1) {
								Main.err.write(buf, 0, read);
							} else {
								break;
							}
						} catch(IOException e) {
							e.printStackTrace();
							break;
						}
					}
					//System.err.println("Error stream ended.");
				}
			};
			outReader.setDaemon(true);
			errReader.setDaemon(true);
			if(!this.process.isAlive()) {
				while(!this.process.isAlive()) {
					Functions.sleep(10L);
				}
			}
			outReader.start();
			errReader.start();
		}
		
	}
	
	public static final void handleInput(final String input) {
		if(process != null) {
			if(process.process.isAlive()) {
				process.input.print(input + "\n");
				process.input.flush();
			}
		}
		addLogToConsole(">" + input);
	}
	
	protected static final void setTextFor(StyledText styledText) {
		String text = "";
		if(styledText == output) {//if(errorStr == null) {
			text = outTxt.getValue();
		} else if(styledText == error) {
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
			runClock();
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
			runClock();
		}
	}
	
	protected static final boolean loadSettings() {
		if(!settingsFile.exists()) {
			return saveSettings();
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
					} else if(pname.equalsIgnoreCase("serverJar")) {
						File oldServerJar = serverJar;
						serverJar = new File(value);
						if(!serverJar.exists()) {
							serverJar = oldServerJar;
						}
					} else if(pname.equalsIgnoreCase("vmArgs")) {
						txtVmArgs.setText(value);
					} else if(pname.equalsIgnoreCase("progArgs")) {
						txtProgramArgs.setText(value);
					} else if(pname.equalsIgnoreCase("javaHome")) {
						File home = new File(value);
						if(home.isDirectory()) {
							javaHome = value;
						}
					} else if(pname.equalsIgnoreCase("javaExec")) {
						File java = new File(value);
						if(java.isFile()) {
							javaExecutable = value;
						}
					} else if(pname.equalsIgnoreCase("remAdminListenPort")) {
						if(StringUtil.isStrLong(value)) {
							int port = Long.valueOf(value).intValue();
							if(port >= 0 && port < 65535) {
								RemoteAdmin.listenPort = port;
							}
						}
					} else if(pname.equalsIgnoreCase("enableRemoteAdministration")) {
						RemoteAdmin.enableRemoteAdministration = Boolean.valueOf(value).booleanValue();
						System.out.println("Set enableRemoteAdministration to " + RemoteAdmin.enableRemoteAdministration + " because saved value was: " + value);
					}
				}
			}
			updateConsoleFont();
			return true;
		} catch(Throwable e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected static final boolean saveSettings() {
		try(PrintWriter pr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(settingsFile), StandardCharsets.UTF_8), true)) {
			pr.println("# Console font settings:");
			pr.println("fontName=" + consoleFontName);
			pr.println("fontSize=" + consoleFontSize);
			pr.println("fontBold=" + consoleFontBold);
			pr.println("fontItalicized=" + consoleFontItalicized);
			pr.println("fontStrikeout=" + consoleFontStrikeout);
			pr.println("fontUnderLined=" + consoleFontUnderLined);
			pr.println("");
			pr.println("# Minecraft server configuration:");
			pr.println("serverJar=" + serverJar.getAbsolutePath());
			pr.println("vmArgs=" + txtVmArgs.getText());
			pr.println("progArgs=" + txtProgramArgs.getText());
			pr.println("javaHome=" + JavaProgramArguments.getArguments().javaHome);
			pr.println("javaExec=" + javaExecutable);
			pr.println("");
			pr.println("# Remote administration settings:");
			pr.println("remAdminListenPort=" + RemoteAdmin.listenPort);
			pr.println("enableRemoteAdministration=" + RemoteAdmin.enableRemoteAdministration);
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	protected static final void appendLog(String str) {
		try {
			if(!wrapperLog.isDisposed()) {
				if(wrapperLog.getText().isEmpty()) {
					wrapperLog.setText(str + "\r\n");
				} else {
					wrapperLog.setText(wrapperLog.getText() + "\r\n" + str);
				}
				wrapperLog.setTopIndex(wrapperLog.getLineCount() - 1);
			}
		} catch(Throwable ignored) {
			logsToAppend.add(str);
		}
	}
	
	protected static final void updateConsoleFont() {
		if(shell == null || shell.isDisposed() || output == null) {
			return;
		}
		final Font font = SWTResourceManager.getFont(consoleFontName, consoleFontSize, (consoleFontItalicized ? SWT.ITALIC : SWT.NORMAL), consoleFontStrikeout, consoleFontUnderLined);
		if(font != null) {
			if(consoleFontBold) {
				output.setFont(SWTResourceManager.getBoldFont(font));
			} else {
				output.setFont(font);
			}
		} else {
			appendLog("The font \"" + Main.consoleFontName + "\" was not found or did not load.");
		}
	}
	
	private static final void createContents() {
		final Point shellSize = shell.getSize();
		output = new StyledText(shell, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		output.setBounds(384, 77, 500, 420);
		updateConsoleFont();
		output.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		output.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		
		error = new StyledText(shell, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		error.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		error.setBounds(10, 314, 360, 214);
		
		inputField = new Text(shell, SWT.BORDER);
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
										appendLog("Font commands:\r\nsetFont {font name]\r\nsetFontSize {size}\r\nsetFontBold {true|false}\r\nsetFontItalic {true|false}\r\nsetFontUnderline {true|false}\r\nsetFontStrikeout {true|false}\r\nsaveFontSettings");
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
											appendLog("Font sizes must be positive.");
										}
									} else {
										appendLog("\"" + args[0] + "\" is not a valid integer.");
									}
								} else {
									appendLog("Usage: \"setFontSize {size}\"");
								}
							} else if(command.equalsIgnoreCase("setfontbold")) {
								if(args.length == 1) {
									consoleFontBold = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("Usage: \"setFontbold {true|false}\"");
								}
							} else if(command.equalsIgnoreCase("setfontitalic")) {
								if(args.length == 1) {
									consoleFontItalicized = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("Usage: \"setFontItalic {true|false}\"");
								}
							} else if(command.equalsIgnoreCase("setfontunderline")) {
								if(args.length == 1) {
									consoleFontUnderLined = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("Usage: \"setFontUnderline {true|false}\"");
								}
							} else if(command.equalsIgnoreCase("setfontstrikeout")) {
								if(args.length == 1) {
									consoleFontStrikeout = Boolean.valueOf(args[0]).booleanValue();
								} else {
									appendLog("Usage: \"setFontStrikeout {true|false}\"");
								}
							} else {
								appendLog("Unknown font command. Type \"setfont ?\" for help.");
							}
							updateConsoleFont();
						} else if(command.equalsIgnoreCase("savesettings")) {
							if(saveSettings()) {
								appendLog("Font settings saved.");
							} else {
								appendLog("Something went wrong when saving the font settings.");
							}
						} else if(command.equalsIgnoreCase("loadsettings")) {
							if(loadSettings()) {
								appendLog("Font settings loaded.");
							} else {
								appendLog("Something went wrong when loading the font settings.");
							}
						} else {
							try {
								Main.handleInput(input);
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
		
		inputField.setBounds(384, shellSize.y - 89, 419, 25);
		
		btnSendInput = new Button(shell, SWT.NONE);
		btnSendInput.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					Main.handleInput(inputField.getText());
				} catch(Throwable e) {
					appendLog("Failed to send input to server process: " + Functions.throwableToStr(e));
				}
				inputField.setText("");
			}
		});
		btnSendInput.setBounds(shellSize.x - 95, shellSize.y - 89, 75, 25);
		btnSendInput.setText("Send input");
		
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		
		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");
		
		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);
		
		mntmSelectJavaExecutable = new MenuItem(menu_1, SWT.NONE);
		mntmSelectJavaExecutable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(shell, SWT.NONE);
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
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		MenuItem mntmExittaltf = new MenuItem(menu_1, SWT.NONE);
		mntmExittaltf.setAccelerator(SWT.ALT | SWT.F4);
		mntmExittaltf.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shutdown();
			}
		});
		mntmExittaltf.setText("E&xit\t(Alt+F4)");
		
		Label lblConsoleOutput = new Label(shell, SWT.NONE);
		lblConsoleOutput.setBounds(384, 56, 95, 15);
		lblConsoleOutput.setText("Console Output:");
		
		verticalSeparator = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
		verticalSeparator.setBounds(376, 11, 2, shellSize.y - 75);
		
		horizontalSeparator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		horizontalSeparator.setBounds(10, 10, shellSize.x - 30, 2);
		
		Label lblStderr = new Label(shell, SWT.NONE);
		lblStderr.setBounds(10, 293, 95, 15);
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
				dialog.setFileName("minecraft_server.jar");
				dialog.setFilterExtensions(new String[] {"*.jar", "*.exe"});
				dialog.setFilterPath(rootDir.getAbsolutePath());
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
		txtVmArgs.setText("-Xms512m -Xmx2048m");
		txtVmArgs.setBounds(71, 45, 299, 25);
		
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
				launchServer();
			}
		});
		btnStartServer.setBounds(10, 115, 177, 25);
		btnStartServer.setText("Start Server");
		
		Label lblWrapperLog = new Label(shell, SWT.NONE);
		lblWrapperLog.setBounds(10, 146, 95, 15);
		lblWrapperLog.setText("Wrapper Log:");
		
		wrapperLog = new StyledText(shell, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		wrapperLog.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		wrapperLog.setEditable(false);
		wrapperLog.setBounds(10, 167, 360, 120);
		
		btnStopServer = new Button(shell, SWT.NONE);
		btnStopServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopServer();
			}
		});
		btnStopServer.setBounds(193, 115, 177, 25);
		btnStopServer.setText("Stop Server");
		
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
		RemoteAdmin.closeListenSocket();
		RemoteAdmin.enableRemoteAdministration = false;
	}
	
	protected static final void changePort() {
		int oldPort = RemoteAdmin.listenPort;
		RemoteAdmin.listenPort = serverListenPort.getSelection();
		if(oldPort != RemoteAdmin.listenPort) {
			startRemoteAdmin();
		}
	}
	
	public static final void shutdown() {
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
	
	public static final boolean isProcessAlive() {
		return process != null && process.process.isAlive();
	}
	
	public static final boolean hasProcessDied() {
		return hasProcessDied;
	}
	
	public static final String getServerState() {
		return serverState;
	}
	
	private static final void resizeToShellSize() {
		final Point shellSize = shell.getSize();
		Point outputSize = new Point(shellSize.x - 404, shellSize.y - 172);
		Point errorSize = new Point(360, shellSize.y - 378);
		Rectangle inputFieldBounds = new Rectangle(384, shellSize.y - 89, shellSize.x - 485, 25);
		Point sendInputLocation = new Point(shellSize.x - 95, shellSize.y - 89);
		Point vertSepSize = new Point(2, shellSize.y - 75);
		Point horiSepSize = new Point(shellSize.x - 30, 2);
		Point horiSepSize2 = new Point(shellSize.x - 404, 2);
		Functions.setSizeFor(output, outputSize);
		Functions.setSizeFor(error, errorSize);
		Functions.setBoundsFor(inputField, inputFieldBounds);
		Functions.setLocationFor(btnSendInput, sendInputLocation);
		Functions.setSizeFor(verticalSeparator, vertSepSize);
		Functions.setSizeFor(horizontalSeparator, horiSepSize);
		Functions.setSizeFor(horizontalSeparator2, horiSepSize2);
	}
	
	private static final void updateUI() {
		resizeToShellSize();
		serverState = (Main.isProcessAlive() ? "ACTIVE" : (Main.hasProcessDied() ? "DEAD" : "NOT_STARTED")) + "," + (Main.isServerJarSelected() ? "SELECTED" : "NOT-SELECTED");
		final boolean serverJarExists = isServerJarSelected();
		final boolean processIsAlive = isProcessAlive();
		if(!processIsAlive && startServer) {
			launchServer();
		} else {
			startServer = false;
		}
		
		String serverJarText = serverJarExists ? serverJar.getAbsolutePath() : "";
		if(!txtServerjar.getText().equals(serverJarText)) {
			txtServerjar.setText(serverJarText);
		}
		btnStartServer.setEnabled(serverJarExists ? process == null : false);
		btnStopServer.setEnabled(processIsAlive);
		//inputField.setEnabled(processIsAlive);
		if(!inputField.getEnabled() && !inputField.getText().isEmpty()) {
			inputField.setText("");
		}
		//btnSendInput.setEnabled(processIsAlive);
		txtVmArgs.setEnabled(!processIsAlive);
		txtProgramArgs.setEnabled(!processIsAlive);
		btnChooseJar.setEnabled(!processIsAlive);
		mntmSelectJavaExecutable.setEnabled(!processIsAlive);
		setTextFor(output);
		setTextFor(error);
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
		} else {
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
		}
		if(process != null) {
			if(!process.process.isAlive()) {
				processExitCode = process.process.exitValue();
				hasProcessDied = true;
				appendLog("Process terminated with error code: " + process.process.exitValue());
				process = null;
			}
		}
		if(logsToAppend.size() > 0) {
			ArrayList<String> copy = new ArrayList<>(logsToAppend);
			logsToAppend.clear();
			for(String str : copy) {
				appendLog(str);
			}
		}
	}
	
	protected static final void openShell() {
		if(!shell.isVisible()) {
			shell.setVisible(true);
			shell.open();
			shell.layout();
		}
	}
	
	protected static final void hideShell() {
		if(shell.isVisible()) {
			shell.setVisible(false);
		}
	}
}
