/**
 * 
 */
package com.gmail.br45entei.minecraft;

import com.gmail.br45entei.data.DisposableByteArrayOutputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class PingServer {
	
	/** byte value of 254 */
	public static final int pingByte = 0xFE;
	
	public static final String[] pingServer(String ip, int port) {
		byte[] ping = pingServerBytes(ip, port);
		ping = ping == null ? new byte[0] : ping;
		String str = new String(ping, StandardCharsets.UTF_8);
		char s = (char) 65533;
		return str.split(Pattern.quote(s + ""));//"§"));
	}
	
	public static final byte[] pingServerBytes(String ip, int port) {
		try(Socket sock = new Socket(ip, port)) {
			DataOutputStream out = new DataOutputStream(sock.getOutputStream());
			DataInputStream in = new DataInputStream(sock.getInputStream());
			
			out.write(pingByte);
			
			int b;
			DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
			while((b = in.read()) != -1) {
				if(b != 0 && b > 16 && b != 255 && b != 23 && b != 24) {
					baos.write(b);
				}
			}
			byte[] data = baos.toByteArray();
			baos.close();
			return data;
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
				for(String data : pingServer(args[0], port)) {
					System.out.println(data);
				}
			} else {
				System.err.println("Invalid port given. Port values range from 0 to 65535.");
				return;
			}
		} else {
			System.out.println("Invalid arguments. Type server ip and port.");
		}
	}
	
}
