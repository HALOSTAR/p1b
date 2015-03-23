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
	
	//operation code
	public static final int OPERATION_SESSIONREAD = 1;
	public static final int OPERATION_SESSIONWRITE = 2;
	
	//server property
	public static final int PORT_PROJ1_RPC = 5300;
	public static final int MAX_PACKET_SIZE = 512;
	
	@Override
	public void run() {
		try {
			DatagramSocket rpcSocket = new DatagramSocket(PORT_PROJ1_RPC);
			while(true) {
				// receive DatagramPacket and fill inBuf
				// inBuf will contains callID and operationCode
				byte[] inBuf = new byte[MAX_PACKET_SIZE];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				rpcSocket.receive(recvPkt);
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				
				//fill outBuf
				byte[] outBuf = null;
				System.out.println("fill outBuf");
				switch(getOperationCode(inBuf)) {
				case OPERATION_SESSIONREAD:
					//SessionRead accepts all args and returns call results
					outBuf = Arrays.copyOf(recvPkt.getData(), recvPkt.getLength());
					break;
				}
				// here outBuf should contain the callID and results of the call
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
				rpcSocket.send(sendPkt);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		if (t == null) {
			t = new Thread (this, threadName);  //instantiate a Thread object
			t.start();
		}
	}
	
	//get operation code
	private int getOperationCode(byte[] _inBuf) throws UnsupportedEncodingException {
		System.out.println("decode");
		String inString = new String(_inBuf, "UTF-8");
		String[] inDetailsString = inString.split(",");
		return Integer.parseInt(inDetailsString[1]);
	}
}
