package com.gmail.br45entei.process;

import com.gmail.br45entei.swt.Functions;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public final class ProcessIO {
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
	}
	
	public final Thread[] startThreads(final Runnable outCode, final Runnable errCode) {
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
		if(!this.process.isAlive()) {
			while(!this.process.isAlive()) {
				Functions.sleep(10L);
			}
		}
		outThread.start();
		errThread.start();
		return new Thread[] {outThread, errThread};
	}
	
}
