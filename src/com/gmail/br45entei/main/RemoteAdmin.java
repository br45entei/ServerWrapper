package com.gmail.br45entei.main;

import com.gmail.br45entei.data.Credentials;
import com.gmail.br45entei.data.RemoteClient;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.StringUtil;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

/** @author Brian_Entei */
public class RemoteAdmin {
	
	public static final int					fNumberOfThreads			= 20;
	private static final ThreadPoolExecutor	fThreadPool					= ((ThreadPoolExecutor) Executors.newFixedThreadPool(fNumberOfThreads/*, namedThreadFactory*/));
	
	public static volatile boolean			enableRemoteAdministration	= true;
	
	public static final String				PROTOCOL_NAME				= "RemAdmin";
	public static final String				PROTOCOL_DELIMITER			= "/";
	public static final String				PROTOCOL_VERSION			= "1.01";
	
	/** This application's networking protocol */
	public static final String				PROTOCOL					= PROTOCOL_NAME + PROTOCOL_DELIMITER + PROTOCOL_VERSION;
	
	public static final int					defaultListenPort			= 17349;
	public static volatile int				listenPort					= defaultListenPort;
	protected static volatile ServerSocket	listenSocket				= null;
	protected static volatile Thread		listenThread				= null;
	protected static volatile boolean		hasListenThreadStarted		= false;
	
	private static volatile int				lastListenPort				= listenPort;
	
	protected static final Credentials getRemoteAdminCreds() {
		return Credentials.getOrCreateCredentials("RemoteAdmin");
	}
	
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
						Credentials remoteAdmin = getRemoteAdminCreds();
						while(listenSocket != null && !listenSocket.isClosed() && Main.isRunning()) {
							try {
								handleRemoteAdminClient(listenSocket.accept(), remoteAdmin);
							} catch(SocketException ignored) {
								return;
							} catch(IOException e) {
								e.printStackTrace();
								Main.appendLog("Unable to handle incoming client for remote administration: " + Functions.throwableToStr(e));
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
	
	protected static final void handleRemoteAdminClient(final Socket socket, final Credentials remoteAdmin) throws RejectedExecutionException {
		Runnable command = new Runnable() {
			@Override
			public final void run() {
				try {
					RemoteClient client = new RemoteClient(socket);
					String authenticationLine = client.readLine();
					if(authenticationLine != null && !authenticationLine.isEmpty()) {
						String[] args = authenticationLine.split(Pattern.quote(" "));
						if(args.length == 3) {
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
										if(remoteAdmin.doCredentialsMatch(username, password)) {
											client.println(PROTOCOL + " 42 PASS");
											client.startSendingLogs();
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
													if(!handleRemoteAdminClientData(data, client)) {
														break;
													}
												} catch(IOException e) {
													client.println(PROTOCOL + " 25 ERROR PARSING DATA: " + Functions.throwableToStr(e));
													if(client.out.checkError()) {
														break;
													}
												}
												Functions.sleep(10L);
											}
										} else {
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
										client.out.println("Server is running: " + PROTOCOL_VERSION);
										client.out.flush();
										client.out.println("You(the client) are running: " + (clientVrsn == -1 ? "-1 (Invalid version: \"" + clientVersion + "\"!)" : clientVrsn + ""));
										client.out.flush();
										if(clientOutdated) {
											client.out.println("Download an updated client at: http://redsandbox.no-ip.org/Files/ServerWrapper/ServerWrapperClient.jar");
										} else {
											Main.appendLog("An updated client(version: " + clientVrsn + ") just attempted to connect to this server!");
											Main.appendLog("Download an updated server wrapper at: http://redsandbox.no-ip.org/Files/ServerWrapper/ServerWrapper.jar");
										}
										client.out.flush();
									}
								} else {
									client.println(PROTOCOL + " 24 UNKNOWN PROTOCOL");
								}
							} else {
								client.println(PROTOCOL + " 23 UNKNOWN METHOD");
							}
						} else {
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
					Main.appendLog("Failed to handle remote admin client data: " + Functions.throwableToStr(e));
				}
			}
		};
		fThreadPool.execute(command);
	}
	
	protected static final boolean handleRemoteAdminClientData(String data, RemoteClient client) {
		if(data.trim().isEmpty()) {
			return true;
		}
		boolean continueData = true;
		String[] split = data.split(Pattern.quote("\r\n"));
		for(String line : split) {
			if(!line.trim().isEmpty()) {
				if(line.equalsIgnoreCase(PROTOCOL + " -1 CLOSE")) {
					continueData = false;
					continue;
				}
				if(line.toUpperCase().startsWith("COMMAND: ")) {
					String command = line.substring("COMMAND:".length());
					command = command.startsWith(" ") ? command.substring(1) : command;
					try {
						Main.handleInput(command);
					} catch(Throwable e) {
						client.println("COMMAND_ERROR: " + Functions.throwableToStr(e));
						client.println("COMMAND_ERROR_END");
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
					}
				} else if(line.equals("START-SERVER")) {
					if(!Main.isProcessAlive()) {
						Main.launchServer();
					}
					client.println("SERVER-STATE: " + Main.getServerState());
					//Main.appendLog("TEST 2");
				} else if(line.equals("STOP-SERVER")) {
					if(Main.isProcessAlive()) {
						Main.stopServer();
					}
					client.println("SERVER-STATE: " + Main.getServerState());
					//Main.appendLog("TEST 3");
				}
			}
		}
		return continueData;
	}
	
}
