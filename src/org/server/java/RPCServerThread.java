package org.server.java;

import java.net.DatagramSocket;
import java.net.SocketException;

public class RPCServerThread implements Runnable {
	private Thread t;
	private String threadName = "RPCServerThread";
	
	public static final int operationSESSIONREAD = 1;
	public static final int operationSESSIONWRITE = 2;
	public static final int portProj1bRPC = 5300;
	public static final int maxPacketSize = 512;
	
	@Override
	public void run() {
		try {
			DatagramSocket rpcSocket = new DatagramSocket(portProj1bRPC);
			while(true) {
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start() {
		if (t == null) {
			t = new Thread (this, threadName);
			t.start();
		}
	}
}
