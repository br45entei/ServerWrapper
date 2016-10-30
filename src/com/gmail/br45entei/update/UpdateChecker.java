package com.gmail.br45entei.update;

import com.gmail.br45entei.data.DisposableByteArrayInputStream;
import com.gmail.br45entei.data.DisposableByteArrayOutputStream;
import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.main.RemoteAdmin;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.FileTransfer.FileData;
import com.gmail.br45entei.util.JavaProgramArguments;
import com.gmail.br45entei.util.StringUtil;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class UpdateChecker {
	
	public static final String getJarFileName() {
		return "ServerWrapper-x" + (StringUtil.isJvm64bit() ? "64" : "86") + (JavaProgramArguments.getClassPathJarFile().getName().toLowerCase().endsWith(".exe") ? ".exe" : ".jar");
	}
	
	public static final String getDownloadIP() {
		return "redsandbox.ddns.net";
	}
	
	public static final String getDownloadPath() {
		return "/Files/ServerWrapper/" + getJarFileName();
	}
	
	public static final void main(String[] args) {
		JavaProgramArguments.initializeFromMainClass(UpdateChecker.class, args);
		System.out.println("Download url: http://" + getDownloadIP() + getDownloadPath());//I'd love to use https:// , but I don't have a valid trusted signed cert. to use on my webserver, soo...
		testDownloadJar();
		//System.out.println("Update available: " + updateAvailable());
	}
	
	public static final void testDownloadJar() {
		String dir = System.getProperty("user.dir");
		System.out.println(dir);
		FileData data = downloadJar();
		System.out.println("data name: " + data.name);
		System.out.println("data length received: " + Functions.humanReadableByteCount(data.getSize(), false, 2));
		if(data.getSize() > 0) {
			File file = new File(dir + File.separator + data.name);
			try(FileOutputStream out = new FileOutputStream(file)) {
				DisposableByteArrayInputStream fis = new DisposableByteArrayInputStream(data.data);
				byte[] b = new byte[4096];
				int len;
				while((len = fis.read(b)) >= 0) {
					out.write(b, 0, len);
				}
				out.flush();
				fis.close();
				Desktop.getDesktop().browse(file.getParentFile().toURI());
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final class UpdateResult {
		
		public final UpdateType	type;
		public final int		fileSize;
		
		protected UpdateResult(UpdateType type, int fileSize) {
			this.type = type;
			this.fileSize = fileSize;
		}
		
		@Override
		public final String toString() {
			return this.type + ": " + this.fileSize;
		}
		
	}
	
	public static enum UpdateType {
		AVAILABLE,
		UP_TO_DATE,
		UNKNOWN,
		NO_CONNECTION;
	}
	
	public static final UpdateResult updateAvailable() {
		return updateAvailable(null);
	}
	
	public static final UpdateResult updateAvailable(final UpdateCheckerDialog dialog) {
		if(dialog != null) {
			dialog.progress.setValue(Double.valueOf(10));
			dialog.runLoop();
		}
		final Property<UpdateResult> result = new Property<>("Result");
		Thread updateThread = new Thread(new Runnable() {
			@Override
			public final void run() {
				try {
					final File original = JavaProgramArguments.getClassPathJarFile();
					BasicFileAttributes attributes = Files.readAttributes(Paths.get(original.toURI()), BasicFileAttributes.class);
					FileTime time = attributes.lastModifiedTime();
					SocketAddress address = new InetSocketAddress(getDownloadIP(), 80);
					Socket server = new Socket();
					server.connect(address, 2000);
					if(dialog != null) {
						dialog.isConnected.setValue(Boolean.TRUE);
					}
					server.setSoTimeout(2000);
					server.setKeepAlive(true);
					PrintWriter pr = new PrintWriter(new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8), true);
					pr.println("HEAD " + getDownloadPath() + " HTTP/1.1\nhost: " + getDownloadIP() + "\nUser-Agent: " + getJarFileName() + " (" + RemoteAdmin.PROTOCOL + ") Java/" + Runtime.class.getPackage().getImplementationVersion() + "\nIf-Modified-Since: " + StringUtil.getCacheTime(time.toMillis()));
					pr.flush();
					if(dialog != null) {
						dialog.progress.setValue(Double.valueOf(10));
					}
					InputStream in = server.getInputStream();
					String responseCode = null;
					String contentLength = null;
					boolean firstLine = true;
					String line;
					while((line = StringUtil.readLine(in)) != null) {
						if(line.isEmpty()) {
							break;
						}
						if(firstLine) {
							firstLine = false;
							responseCode = line;
							continue;
						}
						if(line.toLowerCase().startsWith("content-length: ")) {
							contentLength = line.substring("content-length: ".length());
						}
						if(dialog != null) {
							dialog.progress.setValue(Double.valueOf(dialog.progress.getValue().doubleValue() + 10.0D));
						}
					}
					try {
						in.close();
					} catch(Throwable ignored) {
					}
					try {
						server.close();
					} catch(Throwable ignored) {
					}
					if(dialog != null) {
						dialog.progress.setValue(Double.valueOf(100.0D));
					}
					if(responseCode != null && responseCode.contains("200") && StringUtil.isStrInt(contentLength)) {
						result.setValue(new UpdateResult(UpdateType.AVAILABLE, Integer.parseInt(contentLength)));
						return;
					} else if(responseCode != null && responseCode.contains("304")) {
						result.setValue(new UpdateResult(UpdateType.UP_TO_DATE, -1));
						return;
					}
					try {
						in.close();
						server.close();
					} catch(Throwable ignored) {
					}
				} catch(IOException e) {
					e.printStackTrace();
					result.setValue(new UpdateResult(UpdateType.NO_CONNECTION, -1));
					return;
				}
			}
		});
		updateThread.setDaemon(true);
		updateThread.start();
		while(updateThread.isAlive()) {
			if(dialog != null) {
				dialog.runLoop();
			}
			Functions.sleep(10L);
		}
		return result.getValue() == null ? new UpdateResult(UpdateType.UNKNOWN, -1) : result.getValue();
	}
	
	public static final FileData downloadJar() {
		return downloadJar(null);
	}
	
	@SuppressWarnings("resource")
	public static final FileData downloadJar(final PerformUpdateDialog dialog) {
		if(dialog != null) {
			dialog.progress.setValue(Double.valueOf(10));
			dialog.runLoop();
		}
		final FileData data = new FileData();
		final File original = JavaProgramArguments.getClassPathJarFile();
		Thread updateThread = new Thread(new Runnable() {
			@Override
			public final void run() {
				try {
					BasicFileAttributes attributes = Files.readAttributes(Paths.get(original.toURI()), BasicFileAttributes.class);
					FileTime time = attributes.lastModifiedTime();
					SocketAddress address = new InetSocketAddress(getDownloadIP(), 80);
					Socket server = new Socket();
					server.connect(address, 2000);
					server.setSoTimeout(2000);
					server.setKeepAlive(true);
					PrintWriter pr = new PrintWriter(new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8), true);
					pr.println("GET " + getDownloadPath() + " HTTP/1.1\nhost: " + getDownloadIP() + "\nUser-Agent: " + getJarFileName() + " (" + RemoteAdmin.PROTOCOL + ") Java/" + Runtime.class.getPackage().getImplementationVersion() + "\nIf-Modified-Since: " + StringUtil.getCacheTime(time.toMillis()));
					pr.flush();
					InputStream in = server.getInputStream();
					String responseCode = null;
					String contentLength = null;
					boolean firstLine = true;
					String line;
					while((line = StringUtil.readLine(in)) != null) {
						if(line.isEmpty()) {
							break;
						}
						if(firstLine) {
							firstLine = false;
							responseCode = line;
							continue;
						}
						if(line.toLowerCase().startsWith("content-length: ")) {
							contentLength = line.substring("content-length: ".length());
						}
						if(dialog != null) {
							dialog.progress.setValue(Double.valueOf(dialog.progress.getValue().doubleValue() + 12.0D));
						}
					}
					if(responseCode != null && responseCode.contains("200") && StringUtil.isStrInt(contentLength)) {
						final int size = Integer.parseInt(contentLength);
						DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
						int count = 0;
						byte[] buf = new byte[4096];
						int remaining = size - count;
						int read = in.read(buf, 0, Math.min(buf.length, remaining));
						count += read;
						baos.write(buf, 0, read);
						remaining = size - count;
						while(remaining > 0) {
							remaining = size - count;
							read = in.read(buf, 0, Math.min(buf.length, remaining));
							if(read == -1) {
								break;
							}
							count += read;
							baos.write(buf, 0, read);
							remaining = size - count;
							if(dialog != null) {
								dialog.progress.setValue(Double.valueOf(((count + 0.0D) / (size + 0.0D)) * 100.00D));
							}
						}
						data.data = baos.getBytesAndDispose();
						data.name = getJarFileName();
					} else {
						try {
							in.close();
							server.close();
						} catch(Throwable ignored) {
						}
						data.name = responseCode;
					}
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		});
		updateThread.setDaemon(true);
		updateThread.start();
		while(updateThread.isAlive()) {
			if(dialog != null) {
				dialog.runLoop();
			}
			Functions.sleep(10L);
		}
		if(dialog != null) {
			dialog.progress.setValue(Double.valueOf(100.00D));
			dialog.runLoop();
		}
		return data;
	}
	
}
