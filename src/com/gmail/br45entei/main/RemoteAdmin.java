package com.gmail.br45entei.main;

import com.gmail.br45entei.data.RemoteClient;
import com.gmail.br45entei.main.CredentialsManager.Credential;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.FileTransfer;
import com.gmail.br45entei.util.FileTransfer.FileData;
import com.gmail.br45entei.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class RemoteAdmin {
	
	public static final int					fNumberOfThreads			= 20;
	private static final ThreadPoolExecutor	fThreadPool					= ((ThreadPoolExecutor) Executors.newFixedThreadPool(fNumberOfThreads/*, namedThreadFactory*/));
	
	public static volatile boolean			enableRemoteAdministration	= true;
	
	public static final String				PROTOCOL_NAME				= "RemAdmin";
	public static final String				PROTOCOL_DELIMITER			= "/";
	public static final String				PROTOCOL_VERSION			= "1.02";
	
	/** This application's networking protocol */
	public static final String				PROTOCOL					= PROTOCOL_NAME + PROTOCOL_DELIMITER + PROTOCOL_VERSION;
	
	public static final int					defaultListenPort			= 17349;
	public static volatile int				listenPort					= defaultListenPort;
	protected static volatile ServerSocket	listenSocket				= null;
	protected static volatile Thread		listenThread				= null;
	protected static volatile boolean		hasListenThreadStarted		= false;
	
	private static volatile int				lastListenPort				= listenPort;
	
	public static final void changeClientPorts() {
		if(lastListenPort != listenPort) {
			try {
				for(RemoteClient client : new ArrayList<>(RemoteClient.instances)) {
					client.out.println("PORT-CHANGE: " + listenPort);
					client.out.flush();
					try {
						client.close(PROTOCOL + " -1 CLOSE");
					} catch(Throwable ignored) {
					}
				}
			} catch(ConcurrentModificationException e) {
				System.err.print("I thought I fixed this: ");
				e.printStackTrace();
			} catch(Throwable e) {
				System.err.print("An error occurred while telling clients to change ports: ");
				e.printStackTrace();
			}
		}
	}
	
	private static final void closeAllClients() {
		try {
			for(RemoteClient client : new ArrayList<>(RemoteClient.instances)) {
				if(!enableRemoteAdministration) {
					client.out.print("WARN: Remote Administration was just disabled.\r\n");
					client.out.flush();
				}
				try {
					client.close(PROTOCOL + " -1 CLOSE");
				} catch(Throwable ignored) {
				}
			}
		} catch(ConcurrentModificationException e) {
			System.err.print("I thought I fixed this: ");
			e.printStackTrace();
		} catch(Throwable e) {
			System.err.print("An error occurred while closing connected clients: ");
			e.printStackTrace();
		}
	}
	
	public static final void closeListenSocket() {
		if(listenSocket != null && !listenSocket.isClosed()) {
			closeAllClients();
			try {
				listenSocket.close();
			} catch(IOException ignored) {
			}
			listenThread.interrupt();
			try {
				listenThread.join();
			} catch(InterruptedException ignored) {
			}
			listenThread = null;
			hasListenThreadStarted = false;
			listenSocket = null;
		}
	}
	
	public static final void setupListenSocket() {
		if(!enableRemoteAdministration) {
			closeListenSocket();
			return;
		}
		if(listenSocket != null && !listenSocket.isClosed()) {
			return;
		}
		try {
			listenSocket = new ServerSocket(listenPort);
			lastListenPort = listenPort;
			listenThread = new Thread(Thread.currentThread().getName() + "_SocketListenThread") {
				@Override
				public final void run() {
					try {
						while(listenSocket != null && !listenSocket.isClosed() && Main.isRunning()) {
							try {
								handleRemoteAdminClient(listenSocket.accept());
							} catch(SocketException ignored) {
								return;
							} catch(IOException e) {
								e.printStackTrace();
								Main.appendLog("[ServerWrapper] Unable to handle incoming client for remote administration: " + Functions.throwableToStr(e));
							}
						}
					} catch(Throwable e) {
						e.printStackTrace();
						return;
					}
				}
			};
			listenThread.setDaemon(true);
			hasListenThreadStarted = false;
		} catch(BindException ignored) {//This will be handled automatically
		} catch(IOException e) {
			System.err.print("Unable to set up listen socket for remote administration: ");
			e.printStackTrace();
		}
	}
	
	public static final boolean startListenThread() {
		if(!enableRemoteAdministration) {
			closeListenSocket();
			return false;
		}
		if(listenThread != null && (!listenThread.isAlive() && !hasListenThreadStarted)) {
			listenThread.start();
			hasListenThreadStarted = true;
			return true;
		}
		return false;
	}
	
	public static final boolean restartListenSocket() {
		RemoteAdmin.closeListenSocket();
		if(enableRemoteAdministration) {
			RemoteAdmin.setupListenSocket();
			return RemoteAdmin.startListenThread();
		}
		return false;
	}
	
	protected static final void handleRemoteAdminClient(final Socket socket) throws RejectedExecutionException {
		Runnable command = new Runnable() {
			@Override
			public final void run() {
				String clientUsername = null;
				String clientIp = null;
				try {
					RemoteClient client = new RemoteClient(socket);
					clientIp = client.getIpAddress();
					String authenticationLine = client.readLine();
					if(authenticationLine != null && !authenticationLine.isEmpty()) {
						String[] args = authenticationLine.split(Pattern.quote(" "));
						if(args.length == 3 || args.length == 4) {
							String method = args[0];
							String authentication = args[1].trim();
							String protocolVersion = args[2];
							if(method.equals("HelloIAm")) {
								boolean correctVersion = false;
								if(protocolVersion.startsWith(PROTOCOL_NAME + PROTOCOL_DELIMITER)) {
									String[] versionSplit = protocolVersion.split(Pattern.quote("/"));
									String clientVersion = "";
									if(versionSplit.length == 2) {
										clientVersion = versionSplit[1];
										correctVersion = PROTOCOL_VERSION.equals(clientVersion);
									}
									if(correctVersion) {
										String clientResponse;
										try {
											clientResponse = new String(DatatypeConverter.parseBase64Binary(authentication)).trim();//new String(Base64.getDecoder().decode(authentication));
										} catch(IllegalArgumentException ignored) {
											clientResponse = "";
										}
										String[] creds = clientResponse.split(":");
										String username = creds.length == 2 ? creds[0] : "";
										String password = creds.length == 2 ? creds[1] : "";
										clientUsername = username;
										client.setUsername(username);
										final Credential user = Main.getCredentialsFor(username, password);
										if(user != null) {
											if(args.length == 3) {
												client.println(PROTOCOL + " 42 PASS");
												client.setConnectionType("NORMAL");
												client.startSendingLogs();
												Main.appendLog("User \"" + client.getDisplayName() + "\" logged in.");
												while(!client.socket.isClosed()) {
													String data = "";
													String line;
													try {
														while((line = client.readLine()) != null) {
															if(line.trim().isEmpty()) {
																break;
															}
															data += line + "\r\n";
														}
														if(!handleRemoteAdminClientData(data, client, user)) {
															break;
														}
													} catch(IOException e) {
														client.println(PROTOCOL + " 25 ERROR PARSING DATA: " + Functions.throwableToStr(e).replace("\r\n", " ").replace("\n", " "));
														if(client.out.checkError()) {
															break;
														}
													}
													Functions.sleep(10L);
												}
												Main.appendLog("User \"" + client.getDisplayName() + "\" disconnected.");
											} else if(args.length == 4) {
												String connectionType = args[3];
												if(connectionType.equals("FILETRANSFER")) {
													client.setConnectionType("FILETRANSFER");
													if(!user.permissions.canDeleteFiles && !user.permissions.canDownloadFiles && !user.permissions.canModifyFiles) {
														if(RemoteClient.isConnected(username, client.getIpAddress())) {
															@SuppressWarnings("resource")
															RemoteClient clientUser = RemoteClient.getFromFTConnection(client);
															clientUser.sendPopupMessage("You do not have permission to use the file transfer dialog on this server.");
														}
													} else {
														if(RemoteClient.isConnected(username, client.getIpAddress())) {
															client.println(PROTOCOL + " 43 FILETRANSFER CONNECTION ESTABLISHED");
															@SuppressWarnings("resource")
															RemoteClient clientUser = RemoteClient.getFromFTConnection(client);
															if(clientUser != null) {
																clientUser.ftConnection = client;
																RemoteClient.instances.remove(client);
																Main.appendLog("User \"" + client.getDisplayName() + "\" file transfer connection established.");
																while(!client.socket.isClosed()) {
																	try {
																		if(!handleRemoteAdminFileTransferData(clientUser, user)) {
																			break;
																		}
																	} catch(IOException e) {
																		client.println(PROTOCOL + " 25 ERROR PARSING DATA: " + Functions.throwableToStr(e).replace("\r\n", " ").replace("\n", " "));
																		if(client.out.checkError()) {
																			break;
																		}
																	}
																	Functions.sleep(10L);
																}
																clientUser.ftConnection = null;
																Main.appendLog("User \"" + client.getDisplayName() + "\" file transfer disconnected.");
															} else {
																client.println(PROTOCOL + " 28 USER ALREADY CONNECTED: " + username);
															}
														} else {
															client.println(PROTOCOL + " 27 USER NOT CONNECTED: " + username);
														}
													}
												} else {
													client.println(PROTOCOL + " 26 UNKNOWN CONNECTION TYPE NEGOTIATED: " + args[3]);
												}
											}
										} else {
											Main.appendLog("User \"" + client.getUsername() + "\"(" + client.getIpAddress() + ") failed to log in: Unknown username or bad password. ");
											client.println(PROTOCOL + " 1 Authentication Failure");
										}
									} else {
										double serverVersion = Double.valueOf(PROTOCOL_VERSION).doubleValue();
										double clientVrsn = -1;
										if(StringUtil.isStrDouble(clientVersion)) {
											clientVrsn = Double.valueOf(clientVersion).doubleValue();
										}
										final boolean clientOutdated = serverVersion > clientVrsn;
										client.out.println(PROTOCOL + " 2 Version Mismatch: " + (clientOutdated ? "Client out of date!" : "Server out of date!"));
										client.out.flush();
										client.out.println("This server is running: " + PROTOCOL_VERSION);
										client.out.flush();
										client.out.println("You(the client) are running: " + (clientVrsn == -1 ? "-1 (Invalid version: \"" + clientVersion + "\"!)" : clientVrsn + ""));
										client.out.flush();
										if(clientOutdated) {
											client.out.println("Download an updated client at: http://redsandbox.ddns.net/Files/ServerWrapper/ServerWrapperClient.jar");
										} else {
											Main.appendLog("==An updated client(version: " + clientVrsn + ") just attempted to connect to this server!");
											Main.appendLog("==Download an updated server wrapper at: http://redsandbox.ddns.net/Files/ServerWrapper/ServerWrapper.jar");
										}
										client.out.flush();
									}
								} else {
									Main.appendLog("Failed login from: \"" + client.getIpAddress() + "\"; Cause: Unknown protocol \"" + protocolVersion + "\"!");
									client.println(PROTOCOL + " 24 UNKNOWN PROTOCOL");
								}
							} else {
								Main.appendLog("Failed login from: \"" + client.getIpAddress() + "\"; Cause: Unknown method \"" + method + "\"!");
								client.println(PROTOCOL + " 23 UNKNOWN METHOD");
							}
						} else {
							Main.appendLog("Failed login from: \"" + client.getIpAddress() + "\"; Cause: Bad/malformed authentication line!");
							client.println(PROTOCOL + " 22 BAD REQUEST - LENGTH: " + args.length + "; REQUEST RECEIVED: \"" + authenticationLine + "\"");
						}
					}
					client.println(PROTOCOL + " -1 CLOSE");
					try {
						Thread.sleep(1000L);
					} catch(InterruptedException ignored) {
					}
					client.close();
				} catch(IOException e) {
					Main.appendLog("==Failed to handle remote admin client data: " + Functions.throwableToStr(e));
					if(clientIp != null) {
						Main.appendLog("User \"" + clientUsername + " @ " + clientIp + "\" disconnected.");
					}
				}
			}
		};
		fThreadPool.execute(command);
	}
	
	/*public static final void main(String[] args) {//XXX Relative path testing:
		Main.serverJar = new File("E:\\Java\\cube.ico");//Makes the server folder E:\Java\
		String path = getPathRelativeToServerFolder(new File("E:\\Java\\Minecraft\\Servers\\1.10.2\\ServerWrapper.jar"));
		System.out.println("path: " + path);
		System.out.println("path exists: " + getFileFromRelativePath(path).exists());
	}*/
	
	public static final File getFileFromRelativePath(String path) {
		File serverFolder = Main.getServerFolder();
		if(serverFolder == null) {
			return null;
		}
		if(path.equals("/")) {
			return serverFolder;
		}
		return new File(serverFolder, path.replace("/", File.separator));
	}
	
	public static final String getPathRelativeToServerFolder(File file) {
		if(file == null || !file.exists()) {
			return file == null ? "null" : "/";
		}
		File serverFolder = Main.getServerFolder();
		if(serverFolder == null) {
			return "NO JAR SELECTED";
		}
		if(serverFolder.getAbsolutePath().equalsIgnoreCase(file.getAbsolutePath())) {
			return "/";
		}
		String dir = file.getAbsolutePath().replace(serverFolder.getAbsolutePath(), "").replace("\\", "/");
		if(!dir.startsWith("/")) {
			dir = "/" + dir;
		}
		if(!dir.endsWith("/") && file.isDirectory()) {
			dir += "/";
		}
		return dir;
	}
	
	protected static final boolean handleRemoteAdminFileTransferData(RemoteClient client, Credential user) throws IOException {
		if(client == null || client.ftConnection == null) {
			return false;
		}
		user = user == null ? Credential.fullAccessUser : user;
		boolean continueData = true;
		if(!client.ftConnection.currentFTDir.getAbsolutePath().toLowerCase().startsWith(Main.getServerFolder().getAbsolutePath().toLowerCase())) {
			client.ftConnection.currentFTDir = Main.getServerFolder();
			client.ftConnection.listFiles();
		}
		final boolean isDirHomeDir = getPathRelativeToServerFolder(client.ftConnection.currentFTDir).equals("/");
		String line = StringUtil.readLine(client.ftConnection.in);
		if(line == null || line.equals(PROTOCOL + " -1 CLOSE")) {
			return false;
		}
		if(line.equals("FILE")) {
			FileData data = FileTransfer.readFile(client.ftConnection.in);//Go ahead and parse the incoming file data to prevent garbled data on the next run, even if the user doesn't have permission to upload files
			if(data == null) {
				continueData = false;
				throw new IOException("Invalid file data sent!");
			}
			if(user.permissions.canModifyFiles && user.permissions.canDeleteFiles) {//Included delete perms because if you can't delete, why be able to create a bazillion files and then have no way of cleaning it up?
				String fileName = data.name;
				if(fileName.equalsIgnoreCase("settings.txt") && isDirHomeDir) {
					client.sendPopupMessage("You cannot modify that file!");
					return continueData;
				}
				File file = new File(client.ftConnection.currentFTDir, fileName);
				boolean success = true;
				try(FileOutputStream out = new FileOutputStream(file)) {
					out.write(data.data, 0, data.data.length);
					out.flush();
				} catch(IOException e) {
					success = false;
					client.sendPopupMessage("Unable to write to file \"" + file.getName() + "\";\r\nIs it in use?\r\nStackTrace: " + Functions.throwableToStr(e));
				}
				if(success) {
					client.sendPopupMessage("File \"" + file.getName() + "\"(size: " + Functions.humanReadableByteCount(data.data.length, true, 2) + ") uploaded successfully.");
				}
				data.data = null;
				data = null;
				System.gc();
				client.ftConnection.listFiles();
				Main.appendLog("User FT: \"" + client.getDisplayName() + "\" uploaded file \"" + getPathRelativeToServerFolder(file) + "\";");
			} else {
				client.sendPopupMessage("You do not have permission to modify server files." + (!user.permissions.canDeleteFiles ? "\r\n\r\n('Delete' is also a required permission when creating files and folders.)" : ""));
			}
		} else if(line.startsWith("GETFILE: ")) {
			if(!user.permissions.canDownloadFiles) {
				client.sendPopupMessage("You do not have permission to download server files.");
				return continueData;
			}
			String path = line.substring("GETFILE: ".length());
			File file = getFileFromRelativePath(path);
			if(!file.isFile()) {
				client.ftConnection.println(PROTOCOL + " 45 FILE NOT EXIST OR IS DIRECTORY");
				return continueData;
			}
			if(isDirHomeDir && file.getName().equalsIgnoreCase("settings.txt")) {
				client.sendPopupMessage("You cannot download that file!");
				return continueData;
			}
			client.ftConnection.println("FILE");
			FileTransfer.sendFile(file, client.ftConnection.outStream, null);
			Main.appendLog("User FT: \"" + client.getDisplayName() + "\" downloaded file \"" + getPathRelativeToServerFolder(file) + "\";");
		} else if(line.equals("DIR")) {
			client.ftConnection.println("DIR: " + getPathRelativeToServerFolder(client.ftConnection.currentFTDir));
		} else if(line.startsWith("DIR: ")) {
			String path = line.substring("DIR: ".length());
			if(path.equals("../")) {
				path = getPathRelativeToServerFolder(client.ftConnection.currentFTDir).equals("/") ? "/" : getPathRelativeToServerFolder(client.ftConnection.currentFTDir.getParentFile());
			}
			if(path.endsWith("./")) {
				path = FilenameUtils.normalize(path);
			}
			File check = getFileFromRelativePath(path);
			if(check.isDirectory()) {
				client.ftConnection.currentFTDir = check;
				client.ftConnection.println("DIR: " + getPathRelativeToServerFolder(client.ftConnection.currentFTDir));
				client.ftConnection.listFiles();
			} else {
				client.ftConnection.println("DIR: INVALID_PATH '" + path + "'");
			}
		} else if(line.equals("LIST")) {
			client.ftConnection.listFiles();
		} else if(line.startsWith("DELETE: ")) {
			if(!user.permissions.canDeleteFiles) {
				client.sendPopupMessage("You do not have permission to delete server files.");
				return continueData;
			}
			String deletePath = line.substring("DELETE: ".length());
			File file = getFileFromRelativePath(deletePath);
			if(!file.exists()) {
				client.ftConnection.println(PROTOCOL + " 46 FILESYSTEM OBJECT NOT EXIST");
				client.ftConnection.listFiles();
				return continueData;
			}
			if(isDirHomeDir && (file.getName().equalsIgnoreCase("settings.txt") || file.getName().equalsIgnoreCase("Users"))) {
				client.sendPopupMessage("You cannot delete that " + (file.isFile() ? "file" : "folder") + "!");
				return continueData;
			}
			final boolean deleted = FileDeleteStrategy.FORCE.deleteQuietly(file);
			client.sendPopupMessage(deleted ? "File \"" + deletePath + "\" was deleted successfully." : "Unable to delete file \"" + deletePath + "\";\r\nis it in use?");
			if(deleted) {
				Main.appendLog("User FT: \"" + client.getDisplayName() + "\" DELETED file \"" + deletePath + "\";");
				client.ftConnection.listFiles();
			} else {
				Main.appendLog("User FT: \"" + client.getDisplayName() + "\" attepted to DELETE file \"" + deletePath + "\", but file was inaccessible;");
			}
		} else if(line.startsWith("RENAME: ")) {
			if(!user.permissions.canModifyFiles) {
				client.sendPopupMessage("You do not have permission to modify server files.");
				return continueData;
			}
			String renamePath = line.substring("RENAME: ".length());
			String renameTo = StringUtil.readLine(client.ftConnection.in);
			if(renameTo.isEmpty() || renameTo.equals("null") || renameTo.equals(FilenameUtils.getName(renamePath))) {
				return continueData;
			}
			if(StringUtil.isFileSystemSafe(renameTo)) {
				File file = getFileFromRelativePath(renamePath);
				if(!file.exists()) {
					client.ftConnection.println(PROTOCOL + " 46 FILESYSTEM OBJECT NOT EXIST");
					client.ftConnection.listFiles();
					return continueData;
				}
				if(isDirHomeDir && (file.getName().equalsIgnoreCase("settings.txt") || file.getName().equalsIgnoreCase("Users"))) {
					client.sendPopupMessage("You cannot rename that " + (file.isFile() ? "file" : "folder") + "!");
					return continueData;
				}
				Path source = Paths.get(file.toURI());
				try {
					Files.move(source, source.resolveSibling(renameTo));
					Main.appendLog("User FT: \"" + client.getDisplayName() + "\" RENAMED file \"" + renamePath + "\" to: \"" + renameTo + "\";");
					client.ftConnection.listFiles();
				} catch(IOException e) {
					client.sendPopupMessage("An error occurred when renaming the file \"" + renamePath + "\".\r\nIs the file in use?\r\nError message: " + e.getMessage());
				}
			} else {
				client.sendPopupMessage("The file name \"" + renameTo + "\" is not filesystem safe.\r\nPlease remove any invalid characters and try again.");
			}
		} else if(line.startsWith("MKDIR: ")) {
			if(!(user.permissions.canModifyFiles && user.permissions.canDeleteFiles)) {
				client.sendPopupMessage("You do not have permission to modify server files." + (!user.permissions.canDeleteFiles ? "\r\n\r\n('Delete' is also a required permission when creating files and folders.)" : ""));
				return continueData;
			}
			String newFolderName = line.substring("MKDIR: ".length());
			if(newFolderName.isEmpty() || newFolderName.equals("null")) {
				return continueData;
			}
			if(StringUtil.isFileSystemSafe(newFolderName)) {
				File parent = client.currentFTDir;
				File check = new File(parent, newFolderName);
				if(check.exists()) {
					client.ftConnection.println(PROTOCOL + " 47 FILESYSTEM OBJECT ALREADY EXISTS");
					client.ftConnection.listFiles();
					return continueData;
				}
				if(!check.mkdirs()) {
					client.sendPopupMessage("An unknown error occurred when creating the new folder \"" + check.getName() + "\".\r\nPlease try again.");
				} else {
					Main.appendLog("User FT: \"" + client.getDisplayName() + "\" CREATED folder \"" + newFolderName + "\";");
					client.sendPopupMessage("The folder \"" + check.getName() + "\" was created in the current directory successfully.");
					client.ftConnection.listFiles();
				}
			} else {
				client.sendPopupMessage("The file name \"" + newFolderName + "\" is not filesystem safe.\r\nPlease remove any invalid characters and try again.");
			}
		} else if(line.startsWith("MKDIRGODIR: ")) {
			if(!(user.permissions.canModifyFiles && user.permissions.canDeleteFiles)) {
				client.sendPopupMessage("You do not have permission to modify server files." + (!user.permissions.canDeleteFiles ? "\r\n\r\n('Delete' is also a required permission when creating files and folders.)" : ""));
				return continueData;
			}
			String newFolderName = line.substring("MKDIRGODIR: ".length());
			if(newFolderName.isEmpty() || newFolderName.equals("null")) {
				return continueData;
			}
			if(StringUtil.isFileSystemSafe(newFolderName)) {
				File parent = client.currentFTDir;
				File check = new File(parent, newFolderName);
				if(check.exists()) {
					client.ftConnection.currentFTDir = check;
					client.ftConnection.println("DIR: " + getPathRelativeToServerFolder(check));
					return continueData;
				}
				if(!check.mkdirs()) {
					client.sendPopupMessage("An unknown error occurred when creating the new folder \"" + check.getName() + "\".\r\nPlease try again.");
				} else {
					Main.appendLog("User FT: \"" + client.getDisplayName() + "\" CREATED folder \"" + newFolderName + "\";");
					client.ftConnection.currentFTDir = check;
					client.ftConnection.println("DIR: " + getPathRelativeToServerFolder(check));
				}
			} else {
				client.sendPopupMessage("The file name \"" + newFolderName + "\" is not filesystem safe.\r\nPlease remove any invalid characters and try again.");
			}
		} else if(line.startsWith("GETDIR: ")) {
			if(!user.permissions.canDownloadFiles) {
				client.sendPopupMessage("You do not have permission to download server files.");
				return continueData;
			}
			String path = line.substring("GETDIR: ".length());
			File file = getFileFromRelativePath(path);
			if(!file.isDirectory()) {
				client.ftConnection.println(PROTOCOL + " 48 FILE NOT A DIRECTORY");
				return continueData;
			}
			if(isDirHomeDir && file.getName().equalsIgnoreCase("Users")) {
				client.sendPopupMessage("You cannot download that folder!");
				return continueData;
			}
			for(File send : file.listFiles()) {
				if(!Files.isReadable(Paths.get(send.toURI()))) {
					continue;
				}
				client.ftConnection.println("FILE");
				FileTransfer.sendFile(file, client.ftConnection.outStream, null);
			}
			Main.appendLog("User FT: \"" + client.getDisplayName() + "\" downloaded contents of folder \"" + getPathRelativeToServerFolder(file) + "\";");
		} else if(line.startsWith("NOPOPUPDIALOGS: ")) {
			client.showPopupDialogs = !line.substring("NOPOPUPDIALOGS: ".length()).equalsIgnoreCase("true");
		} else {
			client.ftConnection.println(PROTOCOL + " 44 UNKNOWN FILETRANSFER OPERATION: " + line);
			continueData = false;
		}
		return continueData;
	}
	
	protected static final boolean handleRemoteAdminClientData(String data, RemoteClient client, Credential user) {
		if(data.trim().isEmpty()) {
			return true;
		}
		user = user == null ? Credential.fullAccessUser : user;
		boolean continueData = true;
		String[] split = data.split(Pattern.quote("\r\n"));
		for(String line : split) {
			if(!line.trim().isEmpty()) {
				if(line.equalsIgnoreCase(PROTOCOL + " -1 CLOSE")) {
					continueData = false;
					continue;
				}
				if(line.toUpperCase().startsWith("COMMAND: ")) {
					if(user.permissions.allowConsoleAccess || !Main.isProcessAlive()) {
						String command = line.substring("COMMAND:".length());
						command = command.startsWith(" ") ? command.substring(1) : command;
						if(command.toLowerCase().split(Pattern.quote(" "))[0].startsWith("stop") || command.toLowerCase().split(Pattern.quote(" "))[0].startsWith("restart")) {
							if(user.permissions.canRestartServer) {
								try {
									Main.handleInput(client, command);
								} catch(Throwable e) {
									client.println("COMMAND_ERROR: " + Functions.throwableToStr(e));
									client.println("COMMAND_ERROR_END");
								}
							}
						}
					} else {
						client.sendPopupMessage("You do not have permission to use the console!");
					}
				} else if(line.toUpperCase().startsWith("PING: ")) {
					String pingStr = line.substring("PING:".length());
					pingStr = pingStr.startsWith(" ") ? pingStr.substring(1) : pingStr;
					client.println("PONG: " + pingStr);
				} else if(line.toUpperCase().startsWith("GET: ")) {
					String pname = line.substring("GET:".length());
					pname = pname.startsWith(" ") ? pname.substring(1) : pname;
					
					if(pname.equalsIgnoreCase("SERVER-STATE")) {
						client.println("SERVER-STATE: " + Main.getServerState());
						//Main.appendLog("TEST 1");
					} else if(pname.equalsIgnoreCase("AUTOMATIC-RESTART")) {
						client.println("AUTOMATIC-RESTART: " + Main.automaticServerRestart);
					} else if(pname.equalsIgnoreCase("CPU-USAGE")) {
						client.println("CPU-USAGE: " + Main.getProcessCpuUsage());
					} else if(pname.equalsIgnoreCase("RAM-USAGE")) {
						client.println("RAM-USAGE: " + Main.getProcessRamUsage());
					} else if(pname.equalsIgnoreCase("THREAD-COUNT")) {
						client.println("THREAD-COUNT: " + Main.getProcessThreadCount());
					}
				} else if(line.toUpperCase().startsWith("SET: ")) {
					String getEntry = line.substring("SET:".length());
					getEntry = getEntry.startsWith(" ") ? getEntry.substring(1) : getEntry;
					if(getEntry.contains("=")) {
						String[] entry = getEntry.split(Pattern.quote("="));
						String pname = entry[0];
						String pvalue = entry.length > 1 ? StringUtil.stringArrayToString(entry, '=', 1) : "";
						if(pname.equalsIgnoreCase("AUTOMATIC-RESTART")) {
							if(user.permissions.canRestartServer) {
								Main.automaticServerRestart = Boolean.valueOf(pvalue.trim()).booleanValue();
							} else {
								client.sendPopupMessage("You do not have permission to change the automatic restart settings.");
							}
						}
					}
				} else if(line.equals("START-SERVER")) {
					if(user.permissions.canRestartServer) {
						if(!Main.isProcessAlive()) {
							Main.launchServer(client);
						}
						client.println("SERVER-STATE: " + Main.getServerState());
						//Main.appendLog("TEST 2");
					} else {
						client.sendPopupMessage("You do not have permission to start the server.");
					}
				} else if(line.equals("STOP-SERVER")) {
					if(user.permissions.canStopServer) {
						if(Main.isProcessAlive()) {
							Main.stopServer(client, false);
						}
						client.println("SERVER-STATE: " + Main.getServerState());
						//Main.appendLog("TEST 3");
					} else {
						client.sendPopupMessage("You do not have permission to stop the server.");
					}
				} else if(line.equals("KILL-SERVER")) {
					if(user.permissions.canStopServer) {
						if(Main.isProcessAlive()) {
							Main.stopServer(client, true);
						}
						client.println("SERVER-STATE: " + Main.getServerState());
						//Main.appendLog("TEST 3");
					} else {
						client.sendPopupMessage("You do not have permission to forcibly kill the server.");
					}
				} else if(line.equals("RESTART-SERVER")) {
					if(user.permissions.canRestartServer) {
						Main.restartServer(client);
						client.println("SERVER-STATE: " + Main.getServerState());
					} else {
						client.sendPopupMessage("You do not have permission to restart the server.");
					}
				}
			}
		}
		return continueData;
	}
	
}
