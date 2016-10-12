package com.gmail.br45entei.process;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.net.ServerSocketFactory;

/** @author <a href="http://vafer.org/blog/20061010091658/">Binding JMX on a
 *         dedicated address | Torsten Curdt's weblog</a> */
@SuppressWarnings("javadoc")
public class RMIServerSocketFactoryImpl implements RMIServerSocketFactory {
	
	private final InetAddress localAddress;
	
	public RMIServerSocketFactoryImpl(final InetAddress pAddress) {
		this.localAddress = pAddress;
	}
	
	@Override
	public ServerSocket createServerSocket(final int pPort) throws IOException {
		return ServerSocketFactory.getDefault().createServerSocket(pPort, 0, this.localAddress);
	}
	
	protected static final RMIConnectorServer startRMIServer(String address, int protocolPort, int namingPort) throws Throwable {
		RMIServerSocketFactory serverFactory = new RMIServerSocketFactoryImpl(InetAddress.getByName(address));
		LocateRegistry.createRegistry(namingPort, null, serverFactory);
		String url = "service:jmx:rmi://" + address + ":" + protocolPort + "/jndi/rmi://" + address + ":" + namingPort + "/connector";
		Map<String, RMIServerSocketFactory> env = new HashMap<>();
		env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverFactory);
		RMIConnectorServer rmiServer = new RMIConnectorServer(new JMXServiceURL(url.toString()), env, ManagementFactory.getPlatformMBeanServer());
		rmiServer.start();
		return rmiServer;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		if(obj == this) {
			return true;
		}
		
		return obj.getClass().equals(getClass());
	}
	
	@Override
	public int hashCode() {
		return RMIServerSocketFactoryImpl.class.hashCode();
	}
}
