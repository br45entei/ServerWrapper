package com.gmail.br45entei.data;

import com.gmail.br45entei.logging.LogUtils;
import com.gmail.br45entei.main.CredentialsManager.UserPermissions;
import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.main.RemoteAdmin;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.AddressUtil;
import com.gmail.br45entei.util.FileTransfer;
import com.gmail.br45entei.util.StringUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public final class RemoteClient implements Closeable {
	
	public static final ConcurrentLinkedQueue<RemoteClient> instances = new ConcurrentLinkedQueue<>();
	
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
	
	public static final void sendCommandToClients(String cmd) {
		if(cmd == null || cmd.trim().isEmpty()) {
			return;
		}
		for(RemoteClient client : new ArrayList<>(instances)) {
			if(client.socket.isConnected() && !client.socket.isClosed()) {
				client.println(cmd);
			}
		}
	}
	
	public final Socket									socket;
	public final InputStream							in;
	public final OutputStream							outStream;
	public final PrintWriter							out;
	private volatile String								type					= "";
	public volatile String								username				= "";
	private volatile UserPermissions					user					= null;
	
	private final Thread								logSender;
	private volatile boolean							hasLogSenderBeenStarted	= false;
	
	protected final ConcurrentLinkedQueue<String>		logsToSend				= new ConcurrentLinkedQueue<>();
	protected volatile int								logsSent				= 0;
	
	private final ConcurrentHashMap<Integer, String>	cmdsToSend				= new ConcurrentHashMap<>();
	private volatile int								cmdsSent				= 0;
	
	private static volatile File						serverIconToSend		= null;
	private static volatile byte[]						serverIconData			= null;
	private static volatile String						serverTitle				= null;
	private volatile boolean							sentServerIcon			= false;
	private volatile boolean							sentServerTitle			= false;
	
	//==========================================
	
	public volatile File								rootFTDir				= Main.getServerFolder();
	public volatile RemoteClient						ftConnection			= null;
	public volatile File								currentFTDir			= this.rootFTDir;
	
	public volatile boolean								showPopupDialogs		= true;
	
	public static final RemoteClient					consoleClient			= new RemoteClient();
	
	static {
		consoleClient.username = "CONSOLE";
	}
	
	//==========================================
	
	public final RemoteClient setUserPermissions(UserPermissions user) {
		this.user = user;
		String rootFTDirPath = user.rootFTDir;
		if("FILETRANSFER".equals(this.type)) {
			if(!this.rootFTDir.getAbsolutePath().equalsIgnoreCase(rootFTDirPath)) {
				File rootFTDir = new File(rootFTDirPath);
				if(rootFTDir.isDirectory()) {
					try {
						this.changeRootDir(rootFTDir);
					} catch(IOException e1) {
						Main.appendLog("==Failed to set root FT directory for client " + this.getDisplayName(true) + "!");
					}
				}
			}
			if(!this.currentFTDir.getAbsolutePath().toLowerCase().startsWith(this.rootFTDir.getAbsolutePath().toLowerCase())) {
				this.currentFTDir = this.rootFTDir;
			}
		} else if("NORMAL".equals(this.type)) {
			if(this.ftConnection != null) {
				this.ftConnection.setUserPermissions(user);
			}
		}
		return this;
	}
	
	public final UserPermissions getUserPermissions() {
		return this.user;
	}
	
	public static final ArrayList<RemoteClient> getClientsWithUsername(String username) {
		if(username == null) {
			return null;
		}
		ArrayList<RemoteClient> list = new ArrayList<>();
		for(RemoteClient client : instances) {
			if(client.getUsername().equalsIgnoreCase(username)) {
				list.add(client);
			}
		}
		return list;
	}
	
	public final void changeRootDir(File rootDir) throws IOException {
		if(!"FILETRANSFER".equals(this.type)) {
			return;
		}
		this.rootFTDir = rootDir;
		this.currentFTDir = rootDir;
		this.listFiles();
	}
	
	public static final RemoteClient getFromFTConnection(RemoteClient ftConnection) {
		if(ftConnection != null && ftConnection.type.equals("FILETRANSFER")) {
			final String ipAddress = AddressUtil.getClientAddressNoPort(ftConnection.getIpAddress());
			for(RemoteClient client : instances) {
				if(client == ftConnection) {
					continue;
				}
				String clientIp = AddressUtil.getClientAddressNoPort(client.getIpAddress());
				if(client.type.equals("NORMAL") && client.username.equalsIgnoreCase(ftConnection.username) && ipAddress.equals(clientIp)) {
					if(client.ftConnection != null) {
						if(!client.ftConnection.isAlive()) {
							try {
								client.ftConnection.disconnect();
							} catch(IOException ignored) {
							}
							client.ftConnection = null;
						} else {
							continue;
						}
					}
					return client;
				}
			}
		}
		return null;
	}
	
	private volatile long lastPingCheck = System.currentTimeMillis();
	
	public final boolean isAlive() {
		long now = System.currentTimeMillis();
		if(now - this.lastPingCheck > 500L) {
			this.out.println("");
			this.lastPingCheck = now;
		}
		return !this.out.checkError();
	}
	
	public static final boolean isConnected(String username, String ipAddress) {
		ipAddress = AddressUtil.getClientAddressNoPort(ipAddress);
		for(RemoteClient client : instances) {
			String clientIp = AddressUtil.getClientAddressNoPort(client.getIpAddress());
			if(client.type.equals("NORMAL") && client.username.equalsIgnoreCase(username) && ipAddress.equals(clientIp)) {
				return true;
			}
		}
		return false;
	}
	
	public final void listFiles() throws IOException {
		if(!this.type.equals("FILETRANSFER")) {
			return;
		}
		if(!this.currentFTDir.getAbsolutePath().toLowerCase().startsWith(this.rootFTDir.getAbsolutePath().toLowerCase())) {
			this.currentFTDir = this.rootFTDir;
		}
		File[] list = this.currentFTDir.listFiles();
		ArrayList<File> folders = new ArrayList<>();
		ArrayList<File> files = new ArrayList<>();
		if(list == null) {
			list = new File[0];
		}
		for(File file : list) {
			if(Files.isReadable(Paths.get(file.toURI()))) {
				if(file.isDirectory()) {
					folders.add(file);
				}
				if(file.isFile()) {
					files.add(file);
				}
			}
		}
		final boolean isHomeDir = this.currentFTDir.getAbsolutePath().equalsIgnoreCase(this.rootFTDir.getAbsolutePath());
		ArrayList<String> folderPaths = new ArrayList<>();
		ArrayList<String> filePaths = new ArrayList<>();
		for(File file : folders) {
			if(!file.getName().equalsIgnoreCase("Users") || !isHomeDir) {
				folderPaths.add(RemoteAdmin.getPathRelativeTo(this.rootFTDir, file) + "?" + Functions.humanReadableByteCount(0, true, 2) + "?" + StringUtil.getCacheTime(file.lastModified()));
			}
		}
		for(File file : files) {
			if(!file.equals(Main.getClassPathJarFile()) && (!file.getName().equalsIgnoreCase("settings.txt") || !isHomeDir)) {
				URLConnection url = file.toURI().toURL().openConnection();
				String hash = FileTransfer.hashFile(file);
				filePaths.add(RemoteAdmin.getPathRelativeTo(this.rootFTDir, file) + "?" + Functions.humanReadableByteCount(url.getContentLengthLong(), true, 6) + "?" + StringUtil.getCacheTime(url.getLastModified()) + "?" + hash);
				try {
					url.getInputStream().close();
					url.getOutputStream().close();
				} catch(Throwable ignored) {
				}
			}
		}
		folderPaths.sort(String.CASE_INSENSITIVE_ORDER);
		//filePaths.sort(String.CASE_INSENSITIVE_ORDER);
		final File parentDir = isHomeDir ? this.currentFTDir : this.currentFTDir.getParentFile();
		String rootPath = this.rootFTDir.getAbsolutePath();
		rootPath = rootPath.endsWith(File.separator) ? rootPath.substring(0, rootPath.length() - File.separator.length()) : rootPath;
		rootPath = rootPath.replace(":", ";");
		this.println("ROOTNAME: " + FilenameUtils.getName(rootPath));
		this.println("DIR: " + RemoteAdmin.getPathRelativeTo(this.rootFTDir, this.currentFTDir));
		this.println("LIST: " + (folderPaths.size() + filePaths.size() + (!isHomeDir ? 1 : 0)));
		if(!isHomeDir) {
			URLConnection url = parentDir.toURI().toURL().openConnection();
			String parentDirPath = "../?" + Functions.humanReadableByteCount(0, true, 2) + "?" + StringUtil.getCacheTime(url.getLastModified());
			try {
				url.getInputStream().close();
				url.getOutputStream().close();
			} catch(Throwable ignored) {
			}
			this.println(parentDirPath);//filePath
			this.println("0");//fileHash(directories don't have bytes to read, so no hashing!)
		}
		for(String folderPath : folderPaths) {
			this.println(folderPath);
			this.println("0");//fileHash(directories don't have bytes to read, so no hashing!)
		}
		for(String filePath : filePaths) {
			String[] split = filePath.split(Pattern.quote("?"));
			String hash = split[split.length - 1];//last arg
			filePath = filePath.substring(0, filePath.length() - (hash.length() + 1));
			System.out.println("filePath: " + filePath);
			System.out.println("hash: " + hash);
			this.println(filePath);
			this.println(hash);
		}
	}
	
	public static final void setServerTitle(String title) {
		if(!Main.sendServerInfoToClients || title == null) {
			if(serverTitle != null) {
				serverTitle = null;
				for(RemoteClient client : new ArrayList<>(instances)) {
					client.sentServerTitle = true;
				}
			}
		} else if(!title.isEmpty()) {
			if(!title.equals(serverTitle)) {
				serverTitle = title;
				for(RemoteClient client : new ArrayList<>(instances)) {
					client.sentServerTitle = false;
				}
			}
		}
	}
	
	public static final void setServerIconFile(File serverIcon) {
		if(!Main.sendServerInfoToClients) {
			serverIconToSend = serverIcon;
			serverIconData = null;
			for(RemoteClient client : new ArrayList<>(instances)) {
				client.sentServerIcon = false;
			}
			return;
		}
		serverIconToSend = serverIcon;
		serverIconData = null;//Let one of the client processes read the file
		if(serverIconToSend != null) {
			for(RemoteClient client : new ArrayList<>(instances)) {
				client.sentServerIcon = false;
			}
		}
	}
	
	private RemoteClient() {
		this.socket = null;
		this.in = null;
		this.outStream = null;
		this.out = null;
		this.logSender = null;
	}
	
	public RemoteClient(Socket socket) throws IOException {
		this.socket = socket;
		this.in = socket.getInputStream();
		this.outStream = socket.getOutputStream();
		this.out = new PrintWriter(new OutputStreamWriter(this.outStream, Main.stringCharset), true);
		this.logSender = new Thread(Thread.currentThread().getName() + "_RemoteClientLogSender") {
			@Override
			public final void run() {
				while(!socket.isClosed()) {
					sendCommands();
					Functions.sleep(1L);
					sendLogs();
					Functions.sleep(10L);
				}
			}
		};
		this.logSender.setDaemon(true);
		String[] split = Main.getConsoleLogs().split(Pattern.quote("\n"));
		for(String log : split) {
			log = log.endsWith("\r") ? log.substring(0, log.length() - 1) : log;
			this.addLog(log);
		}
		split = Main.getConsoleErrors().split(Pattern.quote("\n"));
		for(String log : split) {
			log = log.endsWith("\r") ? log.substring(0, log.length() - 1) : log;
			this.addLog("WARN: " + log);
		}
		instances.add(this);
	}
	
	public final String getConnectionType() {
		return this.type;
	}
	
	public final RemoteClient setConnectionType(String type) {
		this.type = type;
		return this;
	}
	
	public final String getUsername() {
		return this.username;
	}
	
	public final RemoteClient setUsername(String username) {
		if(username != null) {
			this.username = username;
		}
		return this;
	}
	
	public final String getIpAddress() {
		return this == consoleClient ? "127.0.0.1" : StringUtils.replaceOnce(this.socket.getRemoteSocketAddress().toString(), "/", "");
	}
	
	public final String getNickName() {
		return(this.username.isEmpty() ? "<null>" : this.getUsername());
	}
	
	public final String getDisplayName(boolean showPort) {
		return (this.username.isEmpty() ? "" : this.getUsername() + "@") + (showPort ? this.getIpAddress() : AddressUtil.getClientAddressNoPort(this.getIpAddress()));
	}
	
	protected final void sendCommands() {
		if(serverTitle == null) {
			if(this.sentServerTitle) {
				this.sentServerTitle = false;
				this.out.print("TITLE: null\n");
				this.out.flush();
			}
		} else {
			if(!this.sentServerTitle && serverTitle != null) {
				this.out.print("TITLE: " + serverTitle + "\n");
				this.out.flush();
			}
		}
		if(serverIconToSend == null) {
			if(this.sentServerIcon) {
				this.sentServerIcon = false;
				this.out.print("FAVICON: length=0\n");
				this.out.flush();
			}
			serverIconData = null;
		} else if(!this.sentServerIcon && serverIconToSend.isFile()) {
			this.sentServerIcon = true;
			if(serverIconData == null) {
				serverIconData = StringUtil.readFile(serverIconToSend);//Reads the file if the data is null
			}
			if(serverIconData != null) {
				try {
					this.outStream.write(("FAVICON: length=" + serverIconData.length + "\r\n").getBytes(Main.stringCharset));
					this.outStream.flush();
					this.outStream.write(serverIconData, 0, serverIconData.length);
					this.outStream.flush();
				} catch(IOException ignored) {
				}
				Functions.sleep(10L);
			}
		}
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
			this.out.print(RemoteAdmin.PROTOCOL + " 10 RESET LOGS\r\n");
			this.out.flush();
			Functions.sleep(100L);
		}
		if(this.logsSent < this.logsToSend.size()) {
			if(this.logsToSend.size() > 500) {
				while(this.logsToSend.size() > 500) {
					this.logsToSend.poll();
				}
			}
			Iterator<String> iterator = this.logsToSend.iterator();
			while(iterator.hasNext()) {
				String log = iterator.next();
				if(this.logsSent == -1) {
					break;
				}
				if(log != null) {
					//if(!log.trim().equals(">")) {
					this.out.print((log.startsWith("WARN: ") ? "" : "LOG: ") + log + "\r\n");
					this.out.flush();
					//}
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
		if(this == consoleClient) {
			if(cmd != null) {
				if(cmd.startsWith("WARN: ")) {
					LogUtils.warn(cmd.substring("WARN: ".length()));
					return;
				}
				LogUtils.info(cmd.startsWith("LOG: ") ? cmd.substring("LOG: ".length()) : cmd);
				return;
			}
		}
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
	
	public final boolean sendPopupMessage(String msg) {
		if(this.showPopupDialogs) {
			try {
				byte[] data = msg.getBytes(StandardCharsets.UTF_8);
				this.out.println("MESSAGE: " + data.length);
				this.out.flush();
				this.outStream.write(data);
				this.outStream.flush();
				return true;
			} catch(IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	public final void disconnect() throws IOException {
		this.close(RemoteAdmin.PROTOCOL + " -1 CLOSE");
	}
	
	public final void close(String msg) throws IOException {
		this.out.print(msg + "\r\n");
		this.out.flush();
		this.outStream.flush();
		this.close();
	}
	
	@Override
	public final void close() throws IOException {
		if(this.type.equals("NORMAL")) {
			try {
				for(RemoteClient client : instances) {
					if(client == this) {
						continue;
					}
					if(client.type.equals("FILETRANSFER")) {
						if(client.username.equalsIgnoreCase(this.username)) {
							client.close();
						}
					}
				}
			} catch(Throwable ignored) {
			}
		}
		instances.remove(this);
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
		if(log != null && !log.isEmpty()) {
			this.logsToSend.add(log);
		}
	}
	
}
