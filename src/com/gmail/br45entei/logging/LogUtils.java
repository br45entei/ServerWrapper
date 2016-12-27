package com.gmail.br45entei.logging;

import com.gmail.br45entei.logging.Condition.BooleanCondition;
import com.gmail.br45entei.logging.Condition.StringValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.UUID;

/** Utility class used to make logging detailed information to the standard
 * output stream easier
 * 
 * @author Brian_Entei */
public class LogUtils {
	/** Whether or not the debug(...) methods will print to the standard output
	 * stream */
	public static boolean			allowDebugOutput	= false;
	
	protected static String			consolePrefix		= ">";
	protected static boolean		consoleMode			= false;
	
	/** The original {@link System#in} InputStream */
	public static final InputStream	ORIGINAL_SYSTEM_IN	= System.in;
	/** The original {@link System#out} PrintStream */
	public static final PrintStream	ORIGINAL_SYSTEM_OUT	= System.out;
	/** The original {@link System#err} PrintStream */
	public static final PrintStream	ORIGINAL_SYSTEM_ERR	= System.err;
	
	private static PrintStream		out					= LogUtils.ORIGINAL_SYSTEM_OUT;
	private static PrintStream		err					= LogUtils.ORIGINAL_SYSTEM_ERR;
	protected static PrintStream	secondaryOut		= null;
	protected static PrintStream	secondaryErr		= null;
	protected static PrintStream	tertiaryOut			= null;
	protected static PrintStream	tertiaryErr			= null;
	
	private static boolean			replacedSystemOut	= false;
	private static boolean			replacedSystemErr	= false;
	
	/** @return The out stream that this class uses */
	public static final PrintStream getOut() {
		return out;
	}
	
	/** @return The error stream that this class uses */
	public static final PrintStream getErr() {
		return err;
	}
	
	/** Reassigns the "standard" output stream. */
	public static void replaceSystemOut() {
		if(!LogUtils.replacedSystemOut) {
			LogUtils.out = new PrintStreamRedirector(LogUtils.out, LogType.SYS);
			System.setOut(LogUtils.out);
			LogUtils.replacedSystemOut = true;
		}
	}
	
	/** Reassigns the "standard" error output stream. */
	public static void replaceSystemErr() {
		if(!LogUtils.replacedSystemErr) {
			LogUtils.err = new PrintStreamRedirector(LogUtils.err, LogType.SYSERR);
			System.setErr(LogUtils.err);
			LogUtils.replacedSystemErr = true;
		}
		//System.err.println("System error test");
	}
	
	/** Restores the "standard" output stream. */
	public static void restoreSystemOut() {
		if(LogUtils.replacedSystemOut) {
			LogUtils.out = LogUtils.ORIGINAL_SYSTEM_OUT;
			System.setOut(LogUtils.out);
			LogUtils.replacedSystemOut = false;
		}
	}
	
	/** Restores the "standard" error output stream. */
	public static void restoreSystemErr() {
		if(LogUtils.replacedSystemErr) {
			LogUtils.out = LogUtils.ORIGINAL_SYSTEM_ERR;
			System.setErr(LogUtils.err);
			LogUtils.replacedSystemErr = false;
		}
	}
	
	/** @param printStream The PrintStream to set. This changes where the
	 *            debug(...), info(...) and some warn(...) methods print to. */
	protected static void setOutStream(PrintStream printStream) {
		if(printStream != null) {
			LogUtils.out = printStream;
		} else {
			LogUtils.error(new IllegalArgumentException("Cannot set the output stream to a null value!"));
		}
	}
	
	/** @param printStream The PrintStream to set. This changes where the
	 *            error(...), fatal(...) and some warn(...) methods print to. */
	protected static void setErrStream(PrintStream printStream) {
		if(printStream != null) {
			LogUtils.err = printStream;
		} else {
			LogUtils.error(new IllegalArgumentException("Cannot set the error output stream to a null value!"));
		}
	}
	
	/** @param printStream The PrintStream to set. This changes where the
	 *            debug(...), info(...) and some warn(...) methods also print
	 *            to. */
	public static void setSecondaryOutStream(PrintStream printStream) {
		LogUtils.secondaryOut = printStream;
	}
	
	/** @param printStream The PrintStream to set. This changes where the
	 *            error(...), fatal(...) and some warn(...) methods also print
	 *            to. */
	public static void setSecondaryErrStream(PrintStream printStream) {
		LogUtils.secondaryErr = printStream;
	}
	
	/** @return The tertiary PrintStream. */
	public static final PrintStream getTertiaryOut() {
		return LogUtils.tertiaryOut;
	}
	
	/** @return The tertiary error PrintStream. */
	public static final PrintStream getTertiaryErr() {
		return LogUtils.tertiaryErr;
	}
	
	/** @param outStream The PrintStream to set. This changes where the
	 *            debug(...), info(...) and some warn(...) methods also print
	 *            to. */
	public static void setTertiaryOutStream(OutputStream outStream) {
		LogUtils.tertiaryOut = new PrintStream(outStream, true);
	}
	
	/** @param outStream The PrintStream to set. This changes where the
	 *            error(...), fatal(...) and some warn(...) methods also print
	 *            to. */
	public static void setTertiaryErrStream(OutputStream outStream) {
		LogUtils.tertiaryErr = new PrintStream(outStream, true);
	}
	
	/** @param str The text to check
	 * @return Whether or not the given text is prefixed with a valid
	 *         {@link LogUtils#getLoggerTimePrefix()} prefix and a valid log
	 *         type-specific prefix(
	 *         {@link LogUtils#getLoggerPrefix(com.gmail.br45entei.logging.LogUtils.LogType)}
	 *         ) */
	public static final boolean doesStrStartWithLoggerPrefix(String str) {
		if(str == null) {
			return false;
		}
		str = str.trim();//XXX Don't remove this!!!11
		final String timePrefixStr = LogUtils.getLoggerTimePrefix();
		final int timePrefixStrLength = timePrefixStr.length();
		if(str.length() > timePrefixStrLength) {
			final String timePrefix = str.substring(0, timePrefixStrLength);//14);//0, 10 without milliseconds
			if(timePrefix.startsWith("[") && timePrefix.endsWith("]")) {
				final String findThreadPrefix = str.substring(timePrefixStrLength + 1);//15);//11 without milliseconds
				final int endBracketIndex = findThreadPrefix.indexOf("]");
				if(endBracketIndex != -1) {
					final String threadPrefix = findThreadPrefix.substring(0, endBracketIndex + 1);
					if(threadPrefix.startsWith("[") && threadPrefix.endsWith("]")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private static void print(Object str) {
		LogUtils.out.print(str);
		if(LogUtils.secondaryOut != null) {
			LogUtils.secondaryOut.print(str);
		}
		if(LogUtils.tertiaryOut != null) {
			LogUtils.tertiaryOut.print(str);
		}
	}
	
	/** @param obj The object to print */
	public static void println(Object obj) {
		LogUtils.out.println(obj);
		if(LogUtils.secondaryOut != null) {
			LogUtils.secondaryOut.println(obj);
		}
		if(LogUtils.tertiaryOut != null) {
			LogUtils.tertiaryOut.println(obj);
		}
	}
	
	private static void println(String str, LogType logType) {
		if(LogUtils.out instanceof PrintStreamRedirector) {
			((PrintStreamRedirector) LogUtils.out).println(str, logType);
		} else {
			LogUtils.out.println(str);
			if(LogUtils.secondaryOut != null) {
				if(LogUtils.secondaryOut instanceof PrintStreamRedirector) {
					((PrintStreamRedirector) LogUtils.secondaryOut).println(str, logType);
				} else {
					LogUtils.secondaryOut.println(str);
				}
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.println(str);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private static void printErr(Object obj) {
		LogUtils.err.print(obj);
		if(LogUtils.secondaryErr != null) {
			LogUtils.secondaryErr.print(obj);
		}
		if(LogUtils.tertiaryErr != null) {
			LogUtils.tertiaryErr.println(obj);
		}
	}
	
	/** @param obj The object to print */
	public static void printErrln(Object obj) {
		LogUtils.err.println(obj);
		if(LogUtils.secondaryErr != null) {
			LogUtils.secondaryErr.println(obj);
		}
		if(LogUtils.tertiaryErr != null) {
			LogUtils.tertiaryErr.println(obj);
		}
	}
	
	private static void printErrln(String str, LogType logType) {
		if(LogUtils.err instanceof PrintStreamRedirector) {
			((PrintStreamRedirector) LogUtils.err).println(str, logType);
		} else {
			LogUtils.err.println(str);
			if(LogUtils.secondaryErr != null) {
				if(LogUtils.secondaryErr instanceof PrintStreamRedirector) {
					((PrintStreamRedirector) LogUtils.secondaryErr).println(str, logType);
				} else {
					LogUtils.secondaryErr.println(str);
				}
			}
			if(LogUtils.tertiaryErr != null) {
				LogUtils.tertiaryErr.println(str);
			}
		}
	}
	
	/** @return The carriage return currently being used */
	public static final String carriageReturn() {
		if(LogUtils.consoleMode) {
			return "\r";
		}
		return "";
	}
	
	private static void printConsoleIfOutNotReplaced() {
		if(!LogUtils.replacedSystemOut) {
			LogUtils.printConsole();
		}
	}
	
	private static void printConsoleIfErrNotReplaced() {
		if(!LogUtils.replacedSystemErr) {
			LogUtils.printConsole();
		}
	}
	
	/** Prints the console prefix if this class is in console mode */
	public static void printConsole() {
		if(LogUtils.consoleMode) {
			if(LogUtils.out instanceof PrintStreamRedirector) {
				((PrintStreamRedirector) LogUtils.out).printConsole();
			} else {
				LogUtils.print(LogUtils.carriageReturn() + LogUtils.consolePrefix);
			}
		}
	}
	
	/** @return The actual character used when the {@link #printConsole()}
	 *         method
	 *         is called.<br>
	 *         (Usually the ">" character) */
	public static final String getCarriageReturnConsolePrefix() {
		return consolePrefix;
	}
	
	/** @param consoleMode Whether or not this class should print in a
	 *            console-style(default: false) */
	public static void setConsoleMode(boolean consoleMode) {
		LogUtils.consoleMode = consoleMode;
	}
	
	/** @return Whether or not this class is in console mode */
	public static boolean isConsoleMode() {
		return LogUtils.consoleMode;
	}
	
	/** @param consolePrefix The console prefix to print after log messages if
	 *            console mode is enabled(default: ">") */
	public static void setConsolePrefix(String consolePrefix) {
		LogUtils.consolePrefix = consolePrefix;
	}
	
	/** @param getTimeOnly Whether or not time should be included but not date
	 *            as
	 *            well
	 * @param fileSystemSafe Whether or not the returned string will be used in
	 *            the making of a folder or file
	 * @param milliseconds Whether or not the milliseconds should be included
	 * @return The resulting string */
	public static String getSystemTime(boolean getTimeOnly, boolean fileSystemSafe, boolean milliseconds) {
		String timeAndDate = "";
		DateFormat dateFormat;
		if(getTimeOnly == false) {
			dateFormat = new SimpleDateFormat(fileSystemSafe ? "MM-dd-yyyy_HH.mm.ss" + (milliseconds ? ".SSS" : "") : "MM/dd/yyyy_HH:mm:ss" + (milliseconds ? ":SSS" : ""));
		} else {
			dateFormat = new SimpleDateFormat(fileSystemSafe ? "HH.mm.ss" + (milliseconds ? ".SSS" : "") : "HH:mm:ss" + (milliseconds ? ":SSS" : ""));
		}
		Date date = new Date();
		timeAndDate = dateFormat.format(date);
		return timeAndDate;
	}
	
	private static String getLoggerPrefixIfOutNotReplaced(LogType logType) {
		if(!LogUtils.replacedSystemOut) {
			return LogUtils.getLoggerPrefix(logType);
		}
		return "";
	}
	
	private static String getLoggerPrefixIfErrNotReplaced(LogType logType) {
		if(!LogUtils.replacedSystemErr) {
			return LogUtils.getLoggerPrefix(logType);
		}
		return "";
	}
	
	/** @param logType The LogType to get
	 * @return The log prefix */
	public static final String getLoggerPrefix(LogType logType) {
		return LogUtils.carriageReturn() + getLoggerTimePrefix() + " " + getLoggerThreadPrefix(logType) + " ";
	}
	
	/** @return The time prefix with the current time */
	public static final String getLoggerTimePrefix() {
		return "[" + LogUtils.getSystemTime(/*true*/false, false, true) + "]";
	}
	
	/** @param logType The type of log to use
	 * @return The thread prefix in the following format:<br>
	 *         <br>
	 *         <b>
	 *         {@code "[" + Thread.currentThread().getName() + "/" + logType + "]"}
	 *         </b> */
	public static final String getLoggerThreadPrefix(LogType logType) {
		return "[" + Thread.currentThread().getName() + "/" + logType + "]";
	}
	
	/** @param logType The LogType to get
	 * @return The log prefix without a carriage return in the beginning */
	public static final String getLoggerPrefixNoCarriageReturn(LogType logType) {
		return "[" + LogUtils.getSystemTime(true, false, true) + "] [" + Thread.currentThread().getName() + "/" + logType + "] ";
	}
	
	/** @param c The class whose simple name will be printed */
	public static void classInstantiatedMsg(Class<?> c) {
		LogUtils.debug("A new \"" + c.getSimpleName() + "\" class has just been instantiated.");
	}
	
	/** @param msg The message to print */
	public static void debug(String msg) {
		if(!LogUtils.allowDebugOutput) {
			return;
		}
		LogUtils.println(LogUtils.getLoggerPrefixIfOutNotReplaced(LogType.DEGUB) + msg, LogType.DEGUB);
		LogUtils.printConsoleIfOutNotReplaced();
	}
	
	/** @param msg The message to print */
	public static void info(String msg) {
		LogUtils.println(LogUtils.getLoggerPrefixIfOutNotReplaced(LogType.INFO) + msg, LogType.INFO);
		LogUtils.printConsoleIfOutNotReplaced();
	}
	
	/** @param stackTraceElements The elements to convert
	 * @return The resulting string */
	public static final String stackTraceElementsToStr(StackTraceElement[] stackTraceElements) {
		String str = "";
		for(StackTraceElement stackTrace : stackTraceElements) {
			str += (!stackTrace.toString().startsWith("Caused By") ? "     at " : "") + stackTrace.toString() + "\r\n";
		}
		return str;
	}
	
	/** @param t The throwable to convert
	 * @return The throwable, represented as a String */
	public static String throwableToStr(Throwable t) {
		if(t == null) {
			return "null";
		}
		String str = t.getClass().getName() + ": ";
		if((t.getMessage() != null) && !t.getMessage().isEmpty()) {
			str += t.getMessage() + "\r\n";
		} else {
			str += "\r\n";
		}
		str += LogUtils.stackTraceElementsToStr(t.getStackTrace());
		if(t.getCause() != null) {
			str += "Caused by:\r\n" + LogUtils.throwableToStr(t.getCause());
		}
		return str;
	}
	
	/** @param msg The message to print */
	public static void warn(String msg) {
		LogUtils.warn(msg, null);
	}
	
	private static final ArrayList<String>	warnOnceMsgs	= new ArrayList<>();
	private static final ArrayList<String>	errorOnceMsgs	= new ArrayList<>();
	
	/** @param msg The message to print */
	public static void warnOnce(String msg) {
		if(LogUtils.warnOnceMsgs.contains(msg)) {
			return;
		}
		LogUtils.warnOnceMsgs.add(msg);
		LogUtils.warn(msg);
	}
	
	/** @param msg The message to print
	 * @param t The Throwable whose stack trace will be printed */
	public static void warnOnce(String msg, Throwable t) {
		String m = msg + LogUtils.throwableToStr(t);
		if(LogUtils.warnOnceMsgs.contains(m)) {
			return;
		}
		LogUtils.warnOnceMsgs.add(m);
		LogUtils.warn(msg, t);
	}
	
	/** @param t The Throwable whose stack trace will be printed */
	public static void warn(Throwable t) {
		LogUtils.warn("", t);
	}
	
	/** @param msg The message to print
	 * @param t The Throwable whose stack trace will be printed */
	public static void warn(String msg, Throwable t) {
		LogUtils.println(LogUtils.getLoggerPrefixIfOutNotReplaced(LogType.WARN) + msg + (t != null ? (msg.isEmpty() ? "" : "\n") + LogUtils.throwableToStr(t) : ""), LogType.WARN);
		LogUtils.printConsoleIfOutNotReplaced();
	}
	
	/** @param msg The message to print */
	public static void error(String msg) {
		LogUtils.error(msg, null);
	}
	
	/** @param msg The message to print */
	public static void errorOnce(String msg) {
		if(LogUtils.errorOnceMsgs.contains(msg)) {
			return;
		}
		LogUtils.errorOnceMsgs.add(msg);
		LogUtils.error(msg);
	}
	
	/** @param t The Throwable whose stack trace will be printed */
	public static void error(Throwable t) {
		LogUtils.error("", t);
	}
	
	/** @param msg The message to print
	 * @param t The Throwable whose stack trace will be printed */
	public static void errorOnce(String msg, Throwable t) {
		String m = msg + LogUtils.throwableToStr(t);
		if(LogUtils.errorOnceMsgs.contains(m)) {
			return;
		}
		LogUtils.errorOnceMsgs.add(m);
		LogUtils.error(msg, t);
	}
	
	/** @param message The message to print
	 * @param t The Throwable whose stack trace will be printed */
	public static void error(String message, Throwable t) {
		LogUtils.printErrln(LogUtils.getLoggerPrefixIfErrNotReplaced(LogType.ERROR) + message + (t != null ? (message.isEmpty() ? "" : "\n") + LogUtils.throwableToStr(t) : ""), LogType.ERROR);
		LogUtils.printConsoleIfErrNotReplaced();
	}
	
	/** Prints the given message to the standard error console,<br>
	 * then invokes {@code System.exit(-1);} Use with caution.
	 * 
	 * @param msg The message to print */
	public static void fatal(String msg) {
		LogUtils.fatal(msg, null);
	}
	
	/** Prints the given message and throwable to the standard error
	 * console,<br>
	 * then invokes {@code System.exit(-1);} Use with caution.
	 * 
	 * @param message The message to print
	 * @param t The Throwable whose stack trace will be printed */
	public static void fatal(String message, Throwable t) {
		LogUtils.fatal(message, t, -1);
	}
	
	/** Prints the given throwable to the standard error console,<br>
	 * then invokes {@code System.exit(-1);} Use with caution.
	 * 
	 * @param t The Throwable whose stack trace will be printed */
	public static void fatal(Throwable t) {
		LogUtils.fatal(t, -1);
	}
	
	/** Prints the given throwable to the standard error console,<br>
	 * then invokes {@code System.exit(exitCode);} Use with caution.
	 * 
	 * @param t The Throwable whose stack trace will be printed
	 * @param exitCode The exit code to be used when invoking
	 *            {@code System.exit();} */
	public static void fatal(Throwable t, int exitCode) {
		LogUtils.fatal("", t, exitCode);
	}
	
	/** Prints the given message and throwable to the standard error
	 * console,<br>
	 * then invokes {@code System.exit(exitCode);} Use with caution.
	 * 
	 * @param message The message to print
	 * @param t The Throwable whose stack trace will be printed
	 * @param exitCode The exit code to be used when invoking
	 *            {@code System.exit();} */
	public static void fatal(String message, Throwable t, int exitCode) {
		String str = LogUtils.getLoggerPrefixIfErrNotReplaced(LogType.FATAL) + message + (t != null ? (message.isEmpty() ? "" : "\n") + LogUtils.throwableToStr(t) : "");
		LogUtils.printErrln(str, LogType.ERROR);
		System.exit(exitCode);
	}
	
	@SuppressWarnings("javadoc")
	public static enum LogType {
		SYS(),
		DEGUB(),
		INFO(),
		WARN(),
		ERROR(),
		SYSERR(),
		FATAL();
		
		@Override
		public final String toString() {
			return this.name();
		}
		
	}
	
	/** PrintStream class that overrides new line functions to add console
	 * support
	 * 
	 * @author Brian_Entei */
	public static final class PrintStreamRedirector extends PrintStream {
		private final LogType logType;
		
		/** @param out The output stream to redirect
		 * @param logType The log type */
		public PrintStreamRedirector(OutputStream out, LogType logType) {
			super(out, true);
			if(out instanceof PrintStreamRedirector) {
				throw new IllegalArgumentException("Cannot instantiate PrintStreamRedirector with a PrintStreamRedirector as the constructor argument!");
			}
			this.logType = logType;
		}
		
		/** Writes the specified byte to this stream. If the byte is a newline
		 * and
		 * automatic flushing is enabled then the <code>flush</code> method will
		 * be
		 * invoked.
		 * 
		 * <p>
		 * Note that the byte is written as given; to write a character that
		 * will be translated according to the platform's default character
		 * encoding, use the <code>print(char)</code> or
		 * <code>println(char)</code> methods.
		 * 
		 * @param b The byte to be written
		 * @see #print(char)
		 * @see #println(char) */
		@Override
		public void write(int b) {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.write(b);
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.write(b);
			}
			super.write(b);
		}
		
		/** Writes <code>len</code> bytes from the specified byte array starting
		 * at
		 * offset <code>off</code> to this stream. If automatic flushing is
		 * enabled then the <code>flush</code> method will be invoked.
		 * 
		 * <p>
		 * Note that the bytes will be written as given; to write characters
		 * that will be translated according to the platform's default character
		 * encoding, use the <code>print(char)</code> or
		 * <code>println(char)</code> methods.
		 * 
		 * @param buf A byte array
		 * @param off Offset from which to start taking bytes
		 * @param len Number of bytes to write */
		@Override
		public void write(byte buf[], int off, int len) {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.write(buf, off, len);
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.write(buf, off, len);
			}
			super.write(buf, off, len);
		}
		
		/* Methods that do not terminate lines */
		
		/** Prints a boolean value. The string produced by <code>{@link
		 * java.lang.String#valueOf(boolean)}</code> is translated into bytes
		 * according to the platform's default character encoding, and these
		 * bytes
		 * are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param b The <code>boolean</code> to be printed */
		@Override
		public void print(boolean b) {
			print(b + "");
		}
		
		/** Prints a character. The character is translated into one or more
		 * bytes
		 * according to the platform's default character encoding, and these
		 * bytes
		 * are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param c The <code>char</code> to be printed */
		@Override
		public void print(char c) {
			print(c + "");
		}
		
		/** Prints an integer. The string produced by <code>{@link
		 * java.lang.String#valueOf(int)}</code> is translated into bytes
		 * according to the platform's default character encoding, and these
		 * bytes
		 * are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param i The <code>int</code> to be printed
		 * @see java.lang.Integer#toString(int) */
		@Override
		public void print(int i) {
			print(i + "");
		}
		
		/** Prints a long integer. The string produced by <code>{@link
		 * java.lang.String#valueOf(long)}</code> is translated into bytes
		 * according to the platform's default character encoding, and these
		 * bytes
		 * are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param l The <code>long</code> to be printed
		 * @see java.lang.Long#toString(long) */
		@Override
		public void print(long l) {
			print(l + "");
		}
		
		/** Prints a floating-point number. The string produced by <code>{@link
		 * java.lang.String#valueOf(float)}</code> is translated into bytes
		 * according to the platform's default character encoding, and these
		 * bytes
		 * are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param f The <code>float</code> to be printed
		 * @see java.lang.Float#toString(float) */
		@Override
		public void print(float f) {
			print(f + "");
		}
		
		/** Prints a double-precision floating-point number. The string produced
		 * by <code>{@link java.lang.String#valueOf(double)}</code> is
		 * translated into
		 * bytes according to the platform's default character encoding, and
		 * these
		 * bytes are written in exactly the manner of the <code>{@link
		 * #write(int)}</code> method.
		 * 
		 * @param d The <code>double</code> to be printed
		 * @see java.lang.Double#toString(double) */
		@Override
		public void print(double d) {
			print(d + "");
		}
		
		/** Prints an array of characters. The characters are converted into
		 * bytes
		 * according to the platform's default character encoding, and these
		 * bytes
		 * are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param s The array of chars to be printed
		 * 
		 * @throws NullPointerException If <code>s</code> is
		 *             <code>null</code> */
		@Override
		public void print(char s[]) {
			print(new String(s));
		}
		
		/** Prints a string. If the argument is <code>null</code> then the
		 * string
		 * <code>"null"</code> is printed. Otherwise, the string's characters
		 * are
		 * converted into bytes according to the platform's default character
		 * encoding, and these bytes are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param s The <code>String</code> to be printed */
		@Override
		public void print(String s) {
			if(s.contains("\n")) {
				for(String str : s.split("\n")) {
					println(str);
				}
			} else {
				super.print(s);
			}
		}
		
		/** Prints the carriage return and the console prefix(if any) if console
		 * mode is set. */
		public final void printConsole() {
			if(consoleMode) {
				super.print(carriageReturn() + consolePrefix);
			}
		}
		
		/** Prints an object. The string produced by the <code>{@link
		 * java.lang.String#valueOf(Object)}</code> method is translated into
		 * bytes
		 * according to the platform's default character encoding, and these
		 * bytes
		 * are written in exactly the manner of the
		 * <code>{@link #write(int)}</code> method.
		 * 
		 * @param obj The <code>Object</code> to be printed
		 * @see java.lang.Object#toString() */
		@Override
		public void print(Object obj) {
			print(String.valueOf(obj));
		}
		
		/* Methods that do terminate lines */
		
		/** Terminates the current line by writing the line separator string.
		 * The
		 * line separator string is defined by the system property
		 * <code>line.separator</code>, and is not necessarily a single newline
		 * character (<code>'\n'</code>). */
		@Override
		public void println() {
			super.println();
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.println();
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.println();
			}
			LogUtils.printConsole();
		}
		
		/** Prints a boolean and then terminate the line. This method behaves as
		 * though it invokes <code>{@link #print(boolean)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>boolean</code> to be printed */
		@Override
		public void println(boolean x) {
			println(x + "");
		}
		
		/** Prints a character and then terminate the line. This method behaves
		 * as
		 * though it invokes <code>{@link #print(char)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>char</code> to be printed. */
		@Override
		public void println(char x) {
			println(x + "");
		}
		
		/** Prints an integer and then terminate the line. This method behaves
		 * as
		 * though it invokes <code>{@link #print(int)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>int</code> to be printed. */
		@Override
		public void println(int x) {
			println(x + "");
		}
		
		/** Prints a long and then terminate the line. This method behaves as
		 * though it invokes <code>{@link #print(long)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x a The <code>long</code> to be printed. */
		@Override
		public void println(long x) {
			println(x + "");
		}
		
		/** Prints a float and then terminate the line. This method behaves as
		 * though it invokes <code>{@link #print(float)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>float</code> to be printed. */
		@Override
		public void println(float x) {
			println(x + "");
		}
		
		/** Prints a double and then terminate the line. This method behaves as
		 * though it invokes <code>{@link #print(double)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>double</code> to be printed. */
		@Override
		public void println(double x) {
			println(x + "");
		}
		
		/** Prints an array of characters and then terminate the line. This
		 * method
		 * behaves as though it invokes <code>{@link #print(char[])}</code> and
		 * then <code>{@link #println()}</code>.
		 * 
		 * @param x an array of chars to print. */
		@Override
		public void println(char x[]) {
			println(new String(x));
		}
		
		/** Prints a String and then terminate the line. This method behaves as
		 * though it invokes <code>{@link #print(String)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>String</code> to be printed. */
		@Override
		public void println(String x) {
			x = doesStrStartWithLoggerPrefix(x) ? x : LogUtils.getLoggerPrefix(this.logType) + x;
			if(x.contains("\n")) {
				for(String str : x.split("\n")) {
					println(str);
				}
			} else {
				super.print(x);
				println();
			}
		}
		
		/** Prints a String and then terminate the line. This method behaves as
		 * though it invokes <code>{@link #print(String)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>Object</code> to be printed.
		 * @param logType The log type to use */
		public void println(String x, LogType logType) {
			println(LogUtils.getLoggerPrefix(logType) + x);
		}
		
		/** Prints an Object and then terminate the line. This method calls
		 * at first String.valueOf(x) to get the printed object's string value,
		 * then behaves as
		 * though it invokes <code>{@link #print(String)}</code> and then
		 * <code>{@link #println()}</code>.
		 * 
		 * @param x The <code>Object</code> to be printed. */
		@Override
		public void println(Object x) {
			println(String.valueOf(x));
		}
		
		/** A convenience method to write a formatted string to this output
		 * stream
		 * using the specified format string and arguments.
		 * 
		 * <p>
		 * An invocation of this method of the form <tt>out.printf(format,
		 * args)</tt> behaves in exactly the same way as the invocation
		 * 
		 * <pre>
		 * out.format(format, args)
		 * </pre>
		 * 
		 * @param format
		 *            A format string as described in <a
		 *            href="../util/Formatter.html#syntax">Format string
		 *            syntax</a>
		 * 
		 * @param args
		 *            Arguments referenced by the format specifiers in the
		 *            format
		 *            string. If there are more arguments than format
		 *            specifiers, the
		 *            extra arguments are ignored. The number of arguments is
		 *            variable and may be zero. The maximum number of arguments
		 *            is
		 *            limited by the maximum dimension of a Java array as
		 *            defined by
		 *            <cite>The Java&trade; Virtual Machine
		 *            Specification</cite>.
		 *            The behaviour on a <tt>null</tt> argument depends on the
		 *            <a
		 *            href="../util/Formatter.html#syntax">conversion</a>.
		 * 
		 * @throws IllegalFormatException
		 *             If a format string contains an illegal syntax, a format
		 *             specifier that is incompatible with the given arguments,
		 *             insufficient arguments given the format string, or other
		 *             illegal conditions. For specification of all possible
		 *             formatting errors, see the <a
		 *             href="../util/Formatter.html#detail">Details</a> section
		 *             of the
		 *             formatter class specification.
		 * 
		 * @throws NullPointerException
		 *             If the <tt>format</tt> is <tt>null</tt>
		 * 
		 * @return This output stream
		 * 
		 * @since 1.5 */
		@Override
		public PrintStream printf(String format, Object... args) {
			this.format(format, args);
			return this;
		}
		
		/** A convenience method to write a formatted string to this output
		 * stream
		 * using the specified format string and arguments.
		 * 
		 * <p>
		 * An invocation of this method of the form <tt>out.printf(l, format,
		 * args)</tt> behaves in exactly the same way as the invocation
		 * 
		 * <pre>
		 * out.format(l, format, args)
		 * </pre>
		 * 
		 * @param l
		 *            The {@linkplain java.util.Locale locale} to apply during
		 *            formatting. If <tt>l</tt> is <tt>null</tt> then no
		 *            localization
		 *            is applied.
		 * 
		 * @param format
		 *            A format string as described in <a
		 *            href="../util/Formatter.html#syntax">Format string
		 *            syntax</a>
		 * 
		 * @param args
		 *            Arguments referenced by the format specifiers in the
		 *            format
		 *            string. If there are more arguments than format
		 *            specifiers, the
		 *            extra arguments are ignored. The number of arguments is
		 *            variable and may be zero. The maximum number of arguments
		 *            is
		 *            limited by the maximum dimension of a Java array as
		 *            defined by
		 *            <cite>The Java&trade; Virtual Machine
		 *            Specification</cite>.
		 *            The behaviour on a <tt>null</tt> argument depends on the
		 *            <a
		 *            href="../util/Formatter.html#syntax">conversion</a>.
		 * 
		 * @throws IllegalFormatException
		 *             If a format string contains an illegal syntax, a format
		 *             specifier that is incompatible with the given arguments,
		 *             insufficient arguments given the format string, or other
		 *             illegal conditions. For specification of all possible
		 *             formatting errors, see the <a
		 *             href="../util/Formatter.html#detail">Details</a> section
		 *             of the
		 *             formatter class specification.
		 * 
		 * @throws NullPointerException
		 *             If the <tt>format</tt> is <tt>null</tt>
		 * 
		 * @return This output stream
		 * 
		 * @since 1.5 */
		@Override
		public PrintStream printf(Locale l, String format, Object... args) {
			this.format(l, format, args);
			return this;
		}
		
		/** Writes a formatted string to this output stream using the specified
		 * format string and arguments.
		 * 
		 * <p>
		 * The locale always used is the one returned by
		 * {@link java.util.Locale#getDefault() Locale.getDefault()}, regardless
		 * of any previous invocations of other formatting methods on this
		 * object.
		 * 
		 * @param format
		 *            A format string as described in <a
		 *            href="../util/Formatter.html#syntax">Format string
		 *            syntax</a>
		 * 
		 * @param args
		 *            Arguments referenced by the format specifiers in the
		 *            format
		 *            string. If there are more arguments than format
		 *            specifiers, the
		 *            extra arguments are ignored. The number of arguments is
		 *            variable and may be zero. The maximum number of arguments
		 *            is
		 *            limited by the maximum dimension of a Java array as
		 *            defined by
		 *            <cite>The Java&trade; Virtual Machine
		 *            Specification</cite>.
		 *            The behaviour on a <tt>null</tt> argument depends on the
		 *            <a
		 *            href="../util/Formatter.html#syntax">conversion</a>.
		 * 
		 * @throws IllegalFormatException
		 *             If a format string contains an illegal syntax, a format
		 *             specifier that is incompatible with the given arguments,
		 *             insufficient arguments given the format string, or other
		 *             illegal conditions. For specification of all possible
		 *             formatting errors, see the <a
		 *             href="../util/Formatter.html#detail">Details</a> section
		 *             of the
		 *             formatter class specification.
		 * 
		 * @throws NullPointerException
		 *             If the <tt>format</tt> is <tt>null</tt>
		 * 
		 * @return This output stream
		 * 
		 * @since 1.5 */
		@Override
		public PrintStream format(String format, Object... args) {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.format(format, args);
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.format(format, args);
			}
			super.format(format, args);
			return this;
		}
		
		/** Writes a formatted string to this output stream using the specified
		 * format string and arguments.
		 * 
		 * @param l
		 *            The {@linkplain java.util.Locale locale} to apply during
		 *            formatting. If <tt>l</tt> is <tt>null</tt> then no
		 *            localization
		 *            is applied.
		 * 
		 * @param format
		 *            A format string as described in <a
		 *            href="../util/Formatter.html#syntax">Format string
		 *            syntax</a>
		 * 
		 * @param args
		 *            Arguments referenced by the format specifiers in the
		 *            format
		 *            string. If there are more arguments than format
		 *            specifiers, the
		 *            extra arguments are ignored. The number of arguments is
		 *            variable and may be zero. The maximum number of arguments
		 *            is
		 *            limited by the maximum dimension of a Java array as
		 *            defined by
		 *            <cite>The Java&trade; Virtual Machine
		 *            Specification</cite>.
		 *            The behaviour on a <tt>null</tt> argument depends on the
		 *            <a
		 *            href="../util/Formatter.html#syntax">conversion</a>.
		 * 
		 * @throws IllegalFormatException
		 *             If a format string contains an illegal syntax, a format
		 *             specifier that is incompatible with the given arguments,
		 *             insufficient arguments given the format string, or other
		 *             illegal conditions. For specification of all possible
		 *             formatting errors, see the <a
		 *             href="../util/Formatter.html#detail">Details</a> section
		 *             of the
		 *             formatter class specification.
		 * 
		 * @throws NullPointerException
		 *             If the <tt>format</tt> is <tt>null</tt>
		 * 
		 * @return This output stream
		 * 
		 * @since 1.5 */
		@Override
		public PrintStream format(Locale l, String format, Object... args) {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.format(l, format, args);
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.format(l, format, args);
			}
			super.format(l, format, args);
			return this;
		}
		
		/** Appends the specified character sequence to this output stream.
		 * 
		 * <p>
		 * An invocation of this method of the form <tt>out.append(csq)</tt>
		 * behaves in exactly the same way as the invocation
		 * 
		 * <pre>
		 * out.print(csq.toString())
		 * </pre>
		 * 
		 * <p>
		 * Depending on the specification of <tt>toString</tt> for the character
		 * sequence <tt>csq</tt>, the entire sequence may not be appended. For
		 * instance, invoking then <tt>toString</tt> method of a character
		 * buffer will return a subsequence whose content depends upon the
		 * buffer's position and limit.
		 * 
		 * @param csq
		 *            The character sequence to append. If <tt>csq</tt> is
		 *            <tt>null</tt>, then the four characters <tt>"null"</tt>
		 *            are
		 *            appended to this output stream.
		 * 
		 * @return This output stream
		 * 
		 * @since 1.5 */
		@Override
		public PrintStream append(CharSequence csq) {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.append(csq);
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.append(csq);
			}
			super.append(csq);
			return this;
		}
		
		/** Appends a subsequence of the specified character sequence to this
		 * output
		 * stream.
		 * 
		 * <p>
		 * An invocation of this method of the form <tt>out.append(csq, start,
		 * end)</tt> when <tt>csq</tt> is not <tt>null</tt>, behaves in exactly
		 * the same way as the invocation
		 * 
		 * <pre>
		 * out.print(csq.subSequence(start, end).toString())
		 * </pre>
		 * 
		 * @param csq
		 *            The character sequence from which a subsequence will be
		 *            appended. If <tt>csq</tt> is <tt>null</tt>, then
		 *            characters
		 *            will be appended as if <tt>csq</tt> contained the four
		 *            characters <tt>"null"</tt>.
		 * 
		 * @param start
		 *            The index of the first character in the subsequence
		 * 
		 * @param end
		 *            The index of the character following the last character in
		 *            the
		 *            subsequence
		 * 
		 * @return This output stream
		 * 
		 * @throws IndexOutOfBoundsException
		 *             If <tt>start</tt> or <tt>end</tt> are negative,
		 *             <tt>start</tt> is greater than <tt>end</tt>, or
		 *             <tt>end</tt> is greater than <tt>csq.length()</tt>
		 * 
		 * @since 1.5 */
		@Override
		public PrintStream append(CharSequence csq, int start, int end) {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.append(csq, start, end);
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.append(csq, start, end);
			}
			super.append(csq, start, end);
			return this;
		}
		
		/** Appends the specified character to this output stream.
		 * 
		 * <p>
		 * An invocation of this method of the form <tt>out.append(c)</tt>
		 * behaves in exactly the same way as the invocation
		 * 
		 * <pre>
		 * out.print(c)
		 * </pre>
		 * 
		 * @param c
		 *            The 16-bit character to append
		 * 
		 * @return This output stream
		 * 
		 * @since 1.5 */
		@Override
		public PrintStream append(char c) {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.append(c);
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.append(c);
			}
			super.append(c);
			return this;
		}
		
		@Override
		public final void close() {
			if(LogUtils.secondaryOut != null) {
				LogUtils.secondaryOut.close();
			}
			if(LogUtils.tertiaryOut != null) {
				LogUtils.tertiaryOut.close();
			}
			super.close();
		}
		
	}
	
	/** BufferedReader class that overrides read functions to add timeouts
	 * 
	 * @author Brian_Entei */
	public static final class BufferedReaderRedirector extends BufferedReader {
		/** The string that is returned when this class' readLine() method times
		 * out after 10 seconds */
		public static final String READLINE_NO_RESPONSE = UUID.randomUUID().toString();
		
		/** @param in The reader to redirect */
		public BufferedReaderRedirector(Reader in) {
			super(in);
			if(in instanceof BufferedReaderRedirector) {
				throw new IllegalArgumentException("Cannot instantiate a new BufferedReaderRedirector with an instance of itself as the argument!");
			}
		}
		
		/** Reads a single character.
		 * 
		 * @return The character read, as an integer in the range
		 *         0 to 65535 (<tt>0x00-0xffff</tt>), or -1 if the
		 *         end of the stream has been reached
		 * @exception IOException If an I/O error occurs */
		@Override
		public final int read() throws IOException {
			return super.read();
		}
		
		/** Reads characters into a portion of an array.
		 * 
		 * <p>
		 * This method implements the general contract of the corresponding
		 * <code>{@link Reader#read(char[], int, int) read}</code> method of the
		 * <code>{@link Reader}</code> class. As an additional convenience, it
		 * attempts to read as many characters as possible by repeatedly
		 * invoking the <code>read</code> method of the underlying stream. This
		 * iterated <code>read</code> continues until one of the following
		 * conditions becomes true:
		 * <ul>
		 * 
		 * <li>The specified number of characters have been read,
		 * 
		 * <li>The <code>read</code> method of the underlying stream returns
		 * <code>-1</code>, indicating end-of-file, or
		 * 
		 * <li>The <code>ready</code> method of the underlying stream returns
		 * <code>false</code>, indicating that further input requests would
		 * block.
		 * 
		 * </ul>
		 * If the first <code>read</code> on the underlying stream returns
		 * <code>-1</code> to indicate end-of-file then this method returns
		 * <code>-1</code>. Otherwise this method returns the number of
		 * characters actually read.
		 * 
		 * <p>
		 * Subclasses of this class are encouraged, but not required, to attempt
		 * to read as many characters as possible in the same fashion.
		 * 
		 * <p>
		 * Ordinarily this method takes characters from this stream's character
		 * buffer, filling it from the underlying stream as necessary. If,
		 * however, the buffer is empty, the mark is not valid, and the
		 * requested length is at least as large as the buffer, then this method
		 * will read characters directly from the underlying stream into the
		 * given array. Thus redundant <code>BufferedReader</code>s will not
		 * copy data unnecessarily.
		 * 
		 * @param cbuf Destination buffer
		 * @param off Offset at which to start storing characters
		 * @param len Maximum number of characters to read
		 * 
		 * @return The number of characters read, or -1 if the end of the
		 *         stream has been reached
		 * 
		 * @exception IOException If an I/O error occurs */
		@Override
		public final int read(char cbuf[], int off, int len) throws IOException {
			return super.read(cbuf, off, len);
		}
		
		/** @param condition The condition to check
		 * @return The return value of {@link BufferedReader#readLine()} if the
		 *         condition is (and stays) true, or
		 *         {@link BufferedReaderRedirector#READLINE_NO_RESPONSE} if the
		 *         condition ever proves false */
		public final String readLineAndBreakIfConditionFalse(final BooleanCondition condition) {
			final StringValue rtrn = new StringValue(BufferedReaderRedirector.READLINE_NO_RESPONSE);
			@SuppressWarnings("resource")
			final BufferedReaderRedirector THIS = this;
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						rtrn.setValue(THIS.superReadLine());
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}, Thread.currentThread().getName() + "_Temp");
			t.setDaemon(true);
			t.start();
			while(t.isAlive() && condition.isTrue()) {
				try {
					Thread.sleep(10);
				} catch(Throwable ignored) {
				}
				if(!condition.isTrue()) {
					break;
				}
			}
			if(t.isAlive()) {
				t.interrupt();
			}
			return rtrn.getValue();
		}
		
		protected final String superReadLine() throws IOException {
			return super.readLine();
		}
		
		/** Skips characters.
		 * 
		 * @param n The number of characters to skip
		 * 
		 * @return The number of characters actually skipped
		 * 
		 * @exception IllegalArgumentException If <code>n</code> is negative.
		 * @exception IOException If an I/O error occurs */
		@Override
		public final long skip(long n) throws IOException {
			return super.skip(n);
		}
		
		/** Tells whether this stream is ready to be read. A buffered character
		 * stream is ready if the buffer is not empty, or if the underlying
		 * character stream is ready.
		 * 
		 * @exception IOException If an I/O error occurs */
		@Override
		public final boolean ready() throws IOException {
			return super.ready();
		}
		
		/** Tells whether this stream supports the mark() operation, which it
		 * does. */
		@Override
		public final boolean markSupported() {
			return super.markSupported();
		}
		
		/** Marks the present position in the stream. Subsequent calls to
		 * reset()
		 * will attempt to reposition the stream to this point.
		 * 
		 * @param readAheadLimit Limit on the number of characters that may be
		 *            read while still preserving the mark. An attempt
		 *            to reset the stream after reading characters
		 *            up to this limit or beyond may fail.
		 *            A limit value larger than the size of the input
		 *            buffer will cause a new buffer to be allocated
		 *            whose size is no smaller than limit.
		 *            Therefore large values should be used with care.
		 * 
		 * @exception IllegalArgumentException If readAheadLimit is < 0
		 * @exception IOException If an I/O error occurs */
		@Override
		public final void mark(int readAheadLimit) throws IOException {
			super.mark(readAheadLimit);
		}
		
		/** Resets the stream to the most recent mark.
		 * 
		 * @exception IOException If the stream has never been marked,
		 *                or if the mark has been invalidated */
		@Override
		public final void reset() throws IOException {
			super.reset();
		}
		
		@Override
		public final void close() throws IOException {
			super.close();
		}
		
	}
	
}
