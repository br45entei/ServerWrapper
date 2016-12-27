package com.gmail.br45entei.logging;

import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.data.RemoteClient;
import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/** @author Brian_Entei */
public class ConsoleListener extends Thread {
	private static volatile ConsoleListener	console			= null;
	private final Property<Boolean>			continueRunning	= new Property<>("ContinueRunning", Boolean.TRUE);
	private volatile boolean				isStarted		= false;
	private volatile boolean				isRunning		= false;
	private volatile boolean				hasDied			= false;
	
	private volatile PrintStream			latestLog		= null;
	
	private static final void println(String x) {
		LogUtils.info(x);//System.out.println(x);
	}
	
	private static final void warn(String x) {
		LogUtils.warn(x);//System.err.println(x);
	}
	
	/** @return The current ConsoleListener, if any */
	public static final ConsoleListener getConsole() {
		if(console != null) {
			if(console.hasDied) {
				console = null;
			}
		}
		return console;
	}
	
	/** Create the console and start it (if it was not already) */
	public static final void setupConsole() {
		if(console == null) {
			console = new ConsoleListener();
		}
		console.start();
	}
	
	/** Tell the console to shut down if it is loaded. */
	public static final void stopConsole() {
		if(console != null) {
			console.stopThread();
		}
	}
	
	/** Waits for the currently running ConsoleListener to stop running. This
	 * does not tell the console listener to stop running. */
	public static final void waitForConsoleToStop() {
		if(console != null && console.isRunning()) {
			while(console.isRunning()) {
				sleep(1.00D);
			}
		}
	}
	
	private ConsoleListener() {
		super("ConsoleThread");
		if(console != null) {
			if(console.isRunning()) {
				throw new IllegalThreadStateException("Cannot create a new ConsoleListener when there is already one running!");
			}
			console.stopThread();
			console = null;
		}
		console = this;
	}
	
	private static final String replaceEmptyLinesIn(byte[] r) {
		String t = new String(r, StandardCharsets.UTF_8);
		String[] split = t.split(Pattern.quote("\n"));
		final String logUtilsPrefix = LogUtils.carriageReturn() + LogUtils.getCarriageReturnConsolePrefix();
		t = "";
		for(String s : split) {
			if(s.startsWith(logUtilsPrefix) && s.length() > logUtilsPrefix.length()) {
				s = s.substring(logUtilsPrefix.length());
			}
			String trim = s.trim();
			if(trim.isEmpty()) {
				continue;
			}
			if(trim.contains("\r")) {
				String[] subSplit = trim.split(Pattern.quote("\r"));
				for(String s1 : subSplit) {
					t += s1 + "\r\n";
				}
			} else {
				t += trim + "\r\n";
			}
		}
		return t;
	}
	
	private static final File getLatestLogFile() {
		return new File(Main.rootDir, "serverWrapper-latest.log");
	}
	
	private static final File getLogsFolder() {
		File logs = new File(Main.rootDir, "serverWrapper-logs");
		if(!logs.exists()) {
			logs.mkdirs();
		}
		return logs;
	}
	
	/** Begins a new logging session. */
	public final void startLoggingConsole() {
		if(this.latestLog != null) {
			this.stopLoggingTheConsole(false);
		}
		try {
			this.latestLog = new PrintStream(new FileOutputStream(getLatestLogFile(), false));
		} catch(IOException e) {
			e.printStackTrace();
		}
		LogUtils.setSecondaryOutStream(this.latestLog);
		LogUtils.setSecondaryErrStream(this.latestLog);
	}
	
	/** Flushes and closes the existing serverWrapper-latest.log print stream
	 * session, then archives the current log file.
	 * 
	 * @param restartLog Whether or not {@link #startLoggingConsole()} should be
	 *            called. */
	public final void archiveLog(boolean restartLog) {
		this.stopLoggingTheConsole(false);
		File latest = getLatestLogFile();
		if(latest.isFile()) {
			try {
				byte[] r = StringUtil.readFile(latest);
				r = replaceEmptyLinesIn(r).getBytes(StandardCharsets.UTF_8);
				FileTime time = Files.readAttributes(Paths.get(latest.toURI()), BasicFileAttributes.class).lastModifiedTime();
				long modified = time.toMillis();
				String fileName = StringUtil.getTime(modified, false, true, true) + ".log.gz";
				File archived = new File(getLogsFolder(), fileName);
				GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(archived, false));
				out.write(r);
				out.flush();
				out.close();
				latest.delete();
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
		if(restartLog) {
			this.startLoggingConsole();
		}
	}
	
	/** Stops logging console output to the serverWrapper-latest.log file.
	 * 
	 * @param removeLogUtilStreams Whether or not the
	 *            {@link LogUtils#setSecondaryOutStream(PrintStream)} and
	 *            {@link LogUtils#setSecondaryErrStream(PrintStream)} methods
	 *            should be called with a {@code null} argument. */
	public final void stopLoggingTheConsole(boolean removeLogUtilStreams) {
		try {
			if(removeLogUtilStreams) {
				LogUtils.setSecondaryOutStream(null);
				LogUtils.setSecondaryErrStream(null);
			}
			if(this.latestLog != null) {
				this.latestLog.flush();
				this.latestLog.close();
				this.latestLog = null;
			}
		} catch(Throwable ignored) {
		}
	}
	
	@Override
	public final void run() {
		this.isRunning = true;
		LogUtils.replaceSystemOut();
		LogUtils.replaceSystemErr();
		this.archiveLog(true);
		LogUtils.setConsoleMode(true);
		LogUtils.setConsolePrefix(RemoteClient.consoleClient.getDisplayName(false) + ">");
		LogUtils.printConsole();
		try {
			final Property<String> data = new Property<>("Data", null);
			final Property<IOException> exception = new Property<>("IOException");
			final Runnable readCode = new Runnable() {
				@Override
				public final void run() {
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						int read;
						while((read = System.in.read()) != -1) {
							String s = new String(new byte[] {(byte) read});
							if(s.equals("\n")) {
								break;
							}
							baos.write(read);
						}
						if(baos.size() == 0 && read == -1) {
							data.setValue(null);
							return;
						}
						String rtrn = new String(baos.toByteArray(), StandardCharsets.UTF_8);
						data.setValue(rtrn.endsWith("\r") ? rtrn.substring(0, rtrn.length() - 1) : rtrn);
					} catch(IOException e) {
						exception.setValue(e);
					}
				}
			};
			while(this.shouldBeRunning()) {
				Thread readThread = new Thread(readCode);
				readThread.setDaemon(true);
				readThread.start();
				if(!readThread.isAlive()) {
					while(!readThread.isAlive()) {
						sleep(1.0D);
					}
				}
				while(readThread.isAlive()) {
					if(exception.getValue() != null) {
						break;
					}
					sleep(10.0D);
					if(!this.shouldBeRunning()) {
						break;
					}
				}
				if(exception.getValue() != null) {
					throw exception.getValue();
				}
				this.handleInput(data.getValue());
				data.setValue(null);
				if(!this.shouldBeRunning()) {
					break;
				}
			}
			System.out.println("Console thread shutting down...");
		} catch(IOException e) {
			e.printStackTrace();
		}
		this.stopLoggingTheConsole(true);
		LogUtils.setConsoleMode(false);
		this.isRunning = false;
		this.hasDied = true;
	}
	
	private static final String[][]	commandHelp		= new String[][] {								//
			{"help/?", "Used to display information about a command. You just used it!"},			//
			{"exit", "Tells the ServerWrapper that it needs to shut down."},						//
			{"start", "Attempts to start the wrapped server."},										//
			{"stop", "Attempts to stop the wrapped server."},										//
			{"restart", "Attempts to restart the wrapped server."},									//
			{"kill", "Kills the wrapped server if it is running."},									//
			{"state/query", "Print the current wrapped server state."},								//
			{"input/send [toggle]", "Send a command to the currently running wrapped server."}		//
	};
	
	private volatile boolean		inputToggled	= false;
	
	/** @param line The string to process */
	@SuppressWarnings("unused")
	public final void handleInput(String line) {
		if(line == null) {
			return;
		}
		String[] split = line.split(Pattern.quote(" "));
		final String command = (split.length == 0 ? "" : split[0]).trim();
		final String[] args = split.length < 1 ? new String[0] : Arrays.copyOfRange(split, 1, split.length);
		final String strArgs = args.length >= 1 ? StringUtil.stringArrayToString(args, ' ') : "";
		final String strArgsWithSpaces = args.length >= 1 ? line.substring(command.length() + 1) : "";
		if(this.inputToggled) {
			if(line.equalsIgnoreCase("toggle")) {
				this.inputToggled = false;
				println("Commands are no longer being forwarded to the wrapped server.");
				return;
			}
			Main.handleInput(RemoteClient.consoleClient, line);
			return;
		}
		if(command.isEmpty()) {
			println("Type 'help' or '?' to see a list of commands.");
			return;
		}
		if(command.equalsIgnoreCase("?") || command.equalsIgnoreCase("help")) {//commandHelp[0]
			if(args.length == 0) {
				for(String[] help : commandHelp) {
					println(help[0] + ": " + help[1]);
				}
			}
		} else if(command.equalsIgnoreCase("exit")) {
			if(args.length > 0) {
				if(args.length == 1 && args[0].equalsIgnoreCase("now")) {
					Main.stopServer(null, true);
					if(Main.isServerStopping()) {
						while(Main.isServerStopping()) {
							if(!this.sleepWhileWaitingOnAnotherThread()) {
								break;
							}
						}
					}
					System.exit(0);
				}
				println("Usage: " + commandHelp[1][0] + "; Desc.: " + commandHelp[1][1]);
				return;
			}
			println(Main.shutdown(true) ? "The ServerWrapper was told to shutdown successfully." : "Unable to shutdown the ServerWrapper!");
		} else if(command.equalsIgnoreCase("start")) {
			if(args.length > 0) {
				println("Usage: " + commandHelp[2][0] + "; Desc.: " + commandHelp[2][1]);
				return;
			}
			if(Main.isProcessAlive()) {
				println("The wrapped server is already running!");
				return;
			}
			if(!Main.isServerJarSelected()) {
				println("Unable to start the wrapped server!\r\nCause: No .jar selected!");
				return;
			}
			Main.launchServer(RemoteClient.consoleClient);
		} else if(command.equalsIgnoreCase("stop")) {
			if(args.length > 0) {
				println("Usage: " + commandHelp[3][0] + "; Desc.: " + commandHelp[3][1]);
				return;
			}
			if(!Main.isProcessAlive()) {
				println("The wrapped server is not currently running.");
				return;
			}
			Main.stopServer(RemoteClient.consoleClient, false);
		} else if(command.equalsIgnoreCase("restart")) {
			if(args.length > 0) {
				println("Usage: " + commandHelp[4][0] + "; Desc.: " + commandHelp[4][1]);
				return;
			}
			if(!Main.isServerJarSelected()) {
				println("Unable to start the wrapped server!\r\nCause: No .jar selected!");
				return;
			}
			Main.restartServer(RemoteClient.consoleClient);
		} else if(command.equalsIgnoreCase("kill")) {
			if(args.length > 0) {
				println("Usage: " + commandHelp[5][0] + "; Desc.: " + commandHelp[5][1]);
				return;
			}
			if(!Main.isProcessAlive()) {
				println("The wrapped server is not currently running.");
				return;
			}
			Main.stopServer(RemoteClient.consoleClient, true);
		} else if(command.equalsIgnoreCase("state") || command.equalsIgnoreCase("query")) {
			if(args.length > 0) {
				println("Usage: " + commandHelp[6][0] + "; Desc.: " + commandHelp[6][1]);
				return;
			}
			if(!Main.isProcessAlive()) {
				println("The wrapped server is not currently running.");
				return;
			}
			println("The wrapped server is currently running.\r\nMemory usage: " + Main.getProcessRamUsage() + ";\r\nCPU usage: " + Main.getProcessCpuUsage() + ";\r\nThread count: " + Main.getProcessThreadCount() + ";");
		} else if(command.equalsIgnoreCase("input") || command.equalsIgnoreCase("send") || command.equalsIgnoreCase("say")) {
			if(args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
				this.inputToggled = true;
				println("Commands will now be forwarded to the wrapped server automatically.\r\nTo disable, type \"toggle\".");
				return;
			}
			if(!Main.isProcessAlive()) {
				warn("The wrapped server is not currently running. Input will be sent anyway.");
			}
			Main.handleInput(RemoteClient.consoleClient, (command.equalsIgnoreCase("say") ? command + " " : "") + strArgsWithSpaces);
		} else {
			println("Unknown command.\r\nType 'help' or '?' to see a list of commands.");// You typed: " + command + (strArgs.isEmpty() ? "" : " " + strArgs));
		}
	}
	
	private final boolean sleepWhileWaitingOnAnotherThread() {
		sleep(5.0D);
		if(Thread.currentThread() == getConsole()) {
			return !this.shouldBeRunning();
		}
		return true;
	}
	
	/** @return Whether or not this thread is running */
	public final boolean isRunning() {
		return this.isRunning && this.isAlive();
	}
	
	/** @return Whether or not this thread <b>should</b> be running */
	public final boolean shouldBeRunning() {
		return this.isStarted && this.continueRunning.getValue() == Boolean.TRUE;
	}
	
	/** Tell this thread that it should stop running. */
	public final void stopThread() {
		this.continueRunning.setValue(Boolean.FALSE);
	}
	
	@Override
	public final synchronized void start() {
		if(this.isStarted) {
			return;
		}
		this.isStarted = true;
		super.start();
	}
	
	/** @param millis The milliseconds to sleep
	 * @return Any InterruptedException that may have been thrown while
	 *         sleeping */
	public static final InterruptedException sleep(double millis) {
		long wholeNum = (long) millis;
		int nanos = (int) Math.floor((millis - wholeNum) * 1000000);//(int) Math.floor((millis - wholeNum) / 1000000);
		try {
			Thread.sleep(wholeNum, nanos);
		} catch(InterruptedException e) {
			return e;
		}
		return null;
	}
	
}
