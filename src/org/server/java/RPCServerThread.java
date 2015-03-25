package org.server.java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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
			
			while(true) {
				// receive DatagramPacket and fill inBuf
				// byte[] inBuf = callID + "," + OPERATION_SESSIONREAD + "," + sessID
				byte[] inBuf = new byte[MAX_PACKET_SIZE];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				rpcSocket.receive(recvPkt);
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				
				byte[] outBuf = null;
				switch(getOperationCode(inBuf)) {
				case OPERATION_SESSIONREAD:{
					// SessionRead: look up whether session ID is in sessTbl
					System.out.println("OPERATION_SESSIONREAD");
					if (null != EnterServlet.SessTbl.get(getSessionId(inBuf))) {
						//byte[] outBuf = callID + "," + OPERATION_SESSIONREAD + "," + sessID
						outBuf = Arrays.copyOf(recvPkt.getData(), recvPkt.getLength());
					}
					break;
				}
				case OPERATION_SESSIONWRITE:{
					// SessionWrite: write to the session table
					System.out.println("OPERATION_SESSIONWRITE");
					//byte[] outBuf = callID + "," + OPERATION_SESSIONWRITE + "," + 
					//			sessID + "," + newVersion + "," + newData + "," + discardTime
					updateSessTbl(inBuf);
					outBuf = Arrays.copyOf(recvPkt.getData(), recvPkt.getLength());
					break;
				}
				}
				
				if (null != outBuf){
					// here outBuf should contain the callID and results of the call
					System.out.println("server outBuf: " + outBuf.toString());
					DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
					rpcSocket.send(sendPkt);
				}
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
	
	//get callID
	private int getCallID(byte[] _buf) throws UnsupportedEncodingException {
		String bufString = new String(_buf, "UTF-8");
		String[] inDetailsString = bufString.split(",");
		return Integer.parseInt(inDetailsString[0]);
	}
	
	//get operation code
	private int getOperationCode(byte[] _buf) throws UnsupportedEncodingException {
		String bufString = new String(_buf, "UTF-8");
		String[] bufDetails = bufString.split(",");
		//test
		System.out.println("operation code: " + bufDetails[1]);
		return Integer.parseInt(bufDetails[1]);
	}
	
	//get session ID
	private String getSessionId(byte[] _buf) throws UnsupportedEncodingException {
		String bufString = new String(_buf, "UTF-8");
		String[] bufDetails = bufString.split(",");
		return bufDetails[2].trim();
	}
	
	/**
	 * update the SessTbl with data in the _buf
	 * @param _buf
	 * @throws UnsupportedEncodingException
	 */
	public void updateSessTbl(byte[] _buf) throws UnsupportedEncodingException {
		String bufString = new String(_buf, "UTF-8");
		String[] bufDetails = bufString.split(",");
		String sessID = bufDetails[2].trim();
		String sessData = bufDetails[3].trim() + "_" + bufDetails[4].trim() + "_" + bufDetails[5].trim();
		EnterServlet.SessTbl.put(sessID, sessData);
		System.out.println(EnterServlet.SessTbl.get(sessID));
	}
}
