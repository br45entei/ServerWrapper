/**
 * 
 */
package com.gmail.br45entei.data;

import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.StringUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** @author Brian_Entei */
public final class RemoteClient implements Closeable {
	
	public static final ArrayList<RemoteClient>	instances	= new ArrayList<>();
	
	public static final void resetClientLogs() {
		for(RemoteClient client : new ArrayList<>(instances)) {
			if(client.socket.isConnected() && !client.socket.isClosed()) {
				client.resetLogs();
			}
		}
	}
	
	public static final void sendLogToClients(String log) {
		if(log == null) {
			return;
		}
		for(RemoteClient client : new ArrayList<>(instances)) {
			if(client.socket.isConnected() && !client.socket.isClosed()) {
				client.addLog(log);
			}
		}
	}
	
	public final Socket									socket;
	public final InputStream							in;
	public final OutputStream							outStream;
	public final PrintWriter							out;
	
	private final Thread								logSender;
	private volatile boolean							hasLogSenderBeenStarted	= false;
	
	protected final ConcurrentHashMap<Integer, String>	logsToSend				= new ConcurrentHashMap<>();
	protected volatile int								logsSent				= 0;
	
	private final ConcurrentHashMap<Integer, String>	cmdsToSend				= new ConcurrentHashMap<>();
	private volatile int								cmdsSent				= 0;
	
	public RemoteClient(Socket socket) throws IOException {
		this.socket = socket;
		this.in = socket.getInputStream();
		this.outStream = socket.getOutputStream();
		this.out = new PrintWriter(new OutputStreamWriter(this.outStream, StandardCharsets.UTF_8), true);
		this.logSender = new Thread(Thread.currentThread().getName() + "_RemoteClientLogSender") {
			@Override
			public final void run() {
				while(!socket.isClosed()) {
					sendCommands();
					Functions.sleep(10L);
					sendLogs();
				}
			}
		};
		this.logSender.setDaemon(true);
		String[] split = Main.getConsoleLogs().split(Pattern.quote("\n"));
		for(String log : split) {
			log = log.endsWith("\r") ? log.substring(0, log.length() - 1) : log;
			this.addLog(log);
		}
		instances.add(this);
	}
	
	protected final void sendCommands() {
		if(this.cmdsSent < this.cmdsToSend.size()) {
			while(this.cmdsSent <= this.cmdsToSend.size()) {
				Integer key = Integer.valueOf(this.cmdsSent);
				String cmd = this.cmdsToSend.get(key);
				if(this.cmdsSent == -1) {
					break;
				}
				if(cmd != null) {
					this.out.print(cmd + "\r\n");
					this.out.flush();
				}
				if(this.cmdsSent == -1) {
					break;
				}
				this.cmdsSent++;
				Functions.sleep(1L);
			}
			this.cmdsToSend.clear();
			this.cmdsSent = 0;
		} else {
			Functions.sleep(10L);
		}
	}
	
	protected final void sendLogs() {
		if(this.logsSent == -1) {
			this.logsSent = 0;
			this.out.print("RemAdmin/1.0 10 RESET LOGS\r\n");
			this.out.flush();
			Functions.sleep(100L);
		}
		if(this.logsSent < this.logsToSend.size()) {
			while(this.logsSent < this.logsToSend.size()) {
				Integer key = Integer.valueOf(this.logsSent);
				String log = this.logsToSend.get(key);
				if(this.logsSent == -1) {
					break;
				}
				if(log != null) {
					if(!log.trim().equals(">")) {
						this.out.print("LOG: " + log + "\r\n");
						this.out.flush();
					}
				}
				if(this.logsSent == -1) {
					break;
				}
				this.logsSent++;
				Functions.sleep(1L);
			}
			this.logsToSend.clear();
			this.logsSent = 0;
		} else {
			Functions.sleep(10L);
		}
	}
	
	public final String readLine() throws IOException {
		return StringUtil.readLine(this.in);
	}
	
	/** @param cmd The command to send */
	public final void println(String cmd) {
		if(cmd != null) {
			if(!this.hasLogSenderBeenStarted) {
				this.out.print(cmd + "\r\n");
				this.out.flush();
				return;
			}
			Integer key = new Integer(this.cmdsToSend.size());
			this.cmdsToSend.put(key, cmd);
		}
	}
	
	public final void close(String msg) throws IOException {
		this.out.print(msg + "\r\n");
		this.out.flush();
		this.outStream.flush();
		this.close();
	}
	
	@Override
	public final void close() throws IOException {
		try {
			instances.remove(this);
		} catch(Throwable ignored) {
		}
		this.out.close();
		this.in.close();
		this.outStream.close();
		this.socket.close();
	}
	
	public final void startSendingLogs() {
		if(!this.hasLogSenderBeenStarted) {
			this.logSender.start();
			this.hasLogSenderBeenStarted = true;
		}
	}
	
	public final void resetLogs() {
		this.logsToSend.clear();
		this.logsSent = -1;
	}
	
	public final void addLog(String log) {
		if(log != null) {
			Integer key = new Integer(this.logsToSend.size());
			this.logsToSend.put(key, log);
		}
	}
	
}
