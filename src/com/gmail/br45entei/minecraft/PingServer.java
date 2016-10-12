/**
 * 
 */
package com.gmail.br45entei.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class PingServer {
	
	/** byte value of 254 */
	public static final int pingByte = 0xFE;
	
	public static final String pingServer(String ip, int port) {
		try {
			@SuppressWarnings("resource")
			Socket sock = new Socket(ip, port);
			DataOutputStream out = new DataOutputStream(sock.getOutputStream());
			DataInputStream in = new DataInputStream(sock.getInputStream());
			
			out.write(pingByte);
			
			int b;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while((b = in.read()) != -1) {
				if(b != 0 && b > 16 && b != 255 && b != 23 && b != 24) {
					baos.write(b);
				}
			}
			
			String str = new String(baos.toByteArray(), StandardCharsets.UTF_8);
			//String[] data = str.toString().split("§");
			return str;
		} catch(UnknownHostException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static final void main(String[] args) {
		if(args.length == 2) {
			int port;
			try {
				port = Integer.valueOf(args[1]).intValue();
			} catch(NumberFormatException e) {
				System.err.print("Invalid port: ");
				e.printStackTrace();
				return;
			}
			if(port >= 0 && port <= 65535) {
				System.out.println(pingServer(args[0], port));
			} else {
				System.err.println("Invalid port given. Port values range from 0 to 65535.");
				return;
			}
		} else {
			System.out.println("Invalid arguments. Type server ip and port.");
		}
	}
	
}
