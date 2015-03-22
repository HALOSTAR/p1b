package org.server.java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

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
				byte[] inBuf = new byte[maxPacketSize];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				rpcSocket.receive(recvPkt);
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				// here inBuf contains the callID and operationCode
				byte[] outBuf = null;
				
				switch(getOperationCode(inBuf)) {
				
				case operationSESSIONREAD:
					//SessionRead accepts all args and returns call results
					outBuf = Arrays.copyOf(recvPkt.getData(), recvPkt.getLength());
					break;
				}
				// here outBuf should contain the callID and results of the call
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
				rpcSocket.send(sendPkt);
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
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
	
	private int getOperationCode(byte[] _inBuf) throws UnsupportedEncodingException {
		String inString = new String(_inBuf, "UTF-8");
		String[] inDetailsString = inString.split(",");
		return Integer.parseInt(inDetailsString[0]);
	}
}
