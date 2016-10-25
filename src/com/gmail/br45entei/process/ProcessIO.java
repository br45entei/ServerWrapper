package com.gmail.br45entei.process;

import com.gmail.br45entei.data.RemoteClient;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.IOUtils;
import com.gmail.br45entei.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

@SuppressWarnings("javadoc")
public final class ProcessIO {
	public final ProcessBuilder		builder;
	public volatile Process			process;
	
	public volatile InputStream		out;
	public volatile InputStream		err;
	public volatile OutputStream	in;
	public volatile PrintWriter		input;
	
	public final String				cmdLine;
	
	public volatile RemoteClient	stopServerClient		= null;
	
	//=====
	
	private volatile Thread			resourceUpdateThread	= null;
	public volatile double			cpuUsage				= -1L;
	public volatile long			usedRam					= -1L;
	public volatile long			committedRam			= -1L;
	public volatile int				threadCount				= -1;
	
	private volatile int			jmxRemotePort			= -1;
	private JMXConnector			jmxConnector;
	protected volatile boolean		isJMXConnected			= false;
	
	private static final String		jmxVMArgs				= " -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=127.0.0.2 -Dcom.sun.management.jmxremote.port=PORT -Dcom.sun.management.jmxremote.rmi.port=PORT ";
	
	//=====
	
	public ProcessIO(ProcessBuilder builder) {
		this.builder = builder;
		this.cmdLine = StringUtil.stringArrayToString(' ', builder.command());
	}
	
	public ProcessIO(String process, String vmArgs, String programArgs, File builderDirectory) {
		this.jmxRemotePort = IOUtils.getNextAvailablePort();
		this.cmdLine = process + jmxVMArgs.replace("PORT", Integer.toString(this.jmxRemotePort)) + vmArgs + programArgs;
		this.builder = new ProcessBuilder(this.cmdLine.split(Pattern.quote(" ")));
		this.builder.redirectOutput(Redirect.PIPE);
		this.builder.redirectError(Redirect.PIPE);
		this.builder.redirectInput(Redirect.PIPE);
		this.builder.directory(builderDirectory);
		this.resourceUpdateThread = new Thread() {
			@Override
			public final void run() {
				while(!ProcessIO.this.isJMXConnected) {
					Functions.sleep(10L);
				}
				while(ProcessIO.this.isJMXConnected) {
					Double cpuUsagePercent = ProcessIO.this.getCPULoad();
					ProcessIO.this.cpuUsage = cpuUsagePercent == null ? -1L : (cpuUsagePercent.doubleValue() * 100.00D);
					Long[] memUsage = ProcessIO.this.getMemoryUsage();
					if(memUsage.length == 0) {
						ProcessIO.this.usedRam = -1L;
						ProcessIO.this.committedRam = -1L;
					} else {
						ProcessIO.this.usedRam = memUsage[0].longValue();
						ProcessIO.this.committedRam = memUsage[1].longValue();
					}
					if(!ProcessIO.this.isJMXConnected) {
						break;
					}
					Integer threadCount = ProcessIO.this.getThreadCount();
					if(threadCount == null) {
						ProcessIO.this.threadCount = -1;
					} else {
						ProcessIO.this.threadCount = threadCount.intValue();
					}
					//System.out.println("Used: " + (memUsage.length == 2 ? memUsage[0] : null) + "; Committed: " + (memUsage.length == 2 ? memUsage[1] : null) + "; ThreadCount: " + threadCount);
					Functions.sleep(250L);//Don't want to bog the server down with constant queries!
				}
			}
		};
		this.resourceUpdateThread.setDaemon(true);
		this.resourceUpdateThread.start();
	}
	
	public final Integer getThreadCount() {
		if(this.jmxConnector == null) {
			return null;
		}
		try {
			Object o = this.jmxConnector.getMBeanServerConnection().getAttribute(new ObjectName("java.lang:type=Threading"), "ThreadCount");
			//CompositeData cd = (CompositeData) o;
			//Object obj = cd.get("ThreadCount");
			return o instanceof Integer ? (Integer) o : null;//obj == null ? null : Integer.valueOf(obj.toString());
		} catch(Throwable ignored) {
			ignored.printStackTrace();
			this.isJMXConnected = false;
			return null;
		}
	}
	
	public final Long[] getMemoryUsage() {
		if(this.jmxConnector == null) {
			return new Long[0];
		}
		try {
			Object o = this.jmxConnector.getMBeanServerConnection().getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
			CompositeData cd = (CompositeData) o;
			//System.out.println("CPU: " + getCPULoad());
			return new Long[] {Long.valueOf(cd.get("used").toString()), Long.valueOf(cd.get("committed").toString())};
		} catch(Throwable ignored) {
			this.isJMXConnected = false;
			return new Long[0];
		}
	}
	
	public final Double getCPULoad() {//FUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU y dis so hard to look up??!!!1
		if(this.jmxConnector == null) {
			return null;
		}
		try {
			final Object o = this.jmxConnector.getMBeanServerConnection().getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "ProcessCpuLoad");
			return o == null ? null : Double.valueOf(o.toString());
		} catch(Throwable ignored) {
			this.isJMXConnected = false;
			return null;
		}
	}
	
	public final void startProcess() throws Throwable {
		this.process = this.builder.start();
		this.out = this.process.getInputStream();
		this.err = this.process.getErrorStream();
		this.in = this.process.getOutputStream();
		this.input = new PrintWriter(new OutputStreamWriter(this.in, StandardCharsets.UTF_8), true);
		final String host = "127.0.0.2";
		final int port = this.jmxRemotePort;
		if(port > 0) {
			this.jmxConnector = JMXConnectorFactory.newJMXConnector(createConnectionURL(host, port), null);
			this.jmxConnector.connect();
			this.isJMXConnected = true;
		}
	}
	
	private static final JMXServiceURL createConnectionURL(String host, int port) throws MalformedURLException {
		return new JMXServiceURL("rmi", "", 0, "/jndi/rmi://" + host + ":" + port + "/jmxrmi");
	}
	
	public final InterruptedException waitForProcess() {
		InterruptedException e = null;
		if(!this.process.isAlive()) {
			while(!this.process.isAlive()) {
				e = Functions.sleep(10L);
				if(e != null) {
					break;
				}
			}
		}
		return e;
	}
	
	public final Thread[] startThreads(final Runnable outCode, final Runnable errCode) throws Throwable {
		Thread outThread = new Thread() {
			@Override
			public final void run() {
				outCode.run();
			}
		};
		Thread errThread = new Thread() {
			@Override
			public final void run() {
				errCode.run();
			}
		};
		outThread.setDaemon(true);
		errThread.setDaemon(true);
		if(this.process == null) {
			this.startProcess();
		}
		if(this.waitForProcess() == null) {
			outThread.start();
			errThread.start();
			return new Thread[] {outThread, errThread};
		}
		return new Thread[0];
	}
	
}
