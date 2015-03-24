package org.server.java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

/**
 * RPC server thread, receive call message and reply confirm to client
 * Operate for operation code
 * One RPCServerThread per server, initialized when the server runs. 
 * 
 * A call message consist of: 
 * 		a unique callID for the call
 * 		an operation code
 * 		zero or more arguments, whose format is determined by the operation code
 * A reply message could consist of:
 * 		the callID of the call to which this is a reply
 * 		zero or more results, whose format is determined by the operation code
 * @author jingyi
 *
 */
public class RPCServerThread implements Runnable {
	private Thread t;
	private String threadName = "RPCServerThread";
	
	// Operation code
	public static final int OPERATION_SESSIONREAD = 1;
	public static final int OPERATION_SESSIONWRITE = 2;
	
	// Server property
	public static final int PORT_PROJ1_RPC = 5300;
	public static final int MAX_PACKET_SIZE = 512;
	
	// SimpleDB connect and view keeping 
	// SimpleDB sdb = new SimpleDB("awsAccessId", "awsSecretKey", true);
	
	
	@Override
	public void run() {
		try {
			@SuppressWarnings("resource")
			DatagramSocket rpcSocket = new DatagramSocket(PORT_PROJ1_RPC);
			RPCClient client = new RPCClient();
			
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
				case OPERATION_SESSIONREAD:{
					//SessionRead accepts all args and returns call results
					client.setOperationCode(OPERATION_SESSIONREAD);
					
					client.SessionRead(getSessionId(inBuf));
					
					
					outBuf = Arrays.copyOf(recvPkt.getData(), recvPkt.getLength());
					break;
					
				}
				
				case OPERATION_SESSIONWRITE:{
					//SessionWrite starts writing to the session table
						
					}
				
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
	
	//get operation code
	private String getSessionId(byte[] _inBuf) throws UnsupportedEncodingException {
		System.out.println("decode");
		String inString = new String(_inBuf, "UTF-8");
		String[] inDetailsString = inString.split(",");
		return inDetailsString[2];
	}
}
