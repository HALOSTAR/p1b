package org.server.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class RPCClientThread implements Runnable {
	private Thread t;
	private String threadName = "RPCClientThread";
	private static int callID = 0;  //increase 1 by every call
	private String sessID;
	private int newVersion;
	private String newData;
	private String discardTime;
	
	private LinkedList<InetAddress> destAddrs;
	private int operationCode;

	//operation code
	private static final int OPERATION_SESSIONREAD = RPCServerThread.OPERATION_SESSIONREAD;  //1
	private static final int OPERATION_SESSIONWRITE = RPCServerThread.OPERATION_SESSIONWRITE;  //2

	//server property
	private static final int PORT_PROJ1_RPC = RPCServerThread.PORT_PROJ1_RPC;
	private static final int MAX_PACKET_SIZE = RPCServerThread.MAX_PACKET_SIZE;
	
	/**
	 * Constructor: for test
	 * @param _sessID: session ID
	 */
	public RPCClientThread(String _sessID) {
		sessID = _sessID;
		operationCode = OPERATION_SESSIONREAD;
		destAddrs = new LinkedList<InetAddress>();
		try {
			destAddrs.add(InetAddress.getByName("127.0.0.1"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * constructor: for operationSESSIONREAD
	 * @param _sessID: session ID
	 * @param _operationCode: operationSESSIONREAD
	 * @param _destAddrs: destination address list
	 */
	public RPCClientThread(String _sessID, int _operationCode, LinkedList<InetAddress> _destAddrs) {
		sessID = _sessID;
		operationCode = _operationCode;
		destAddrs = (LinkedList<InetAddress>) _destAddrs.clone();
	}
	
	/**
	 * constructor: for operationSESSIONREAD
	 * @param _sessID: session ID
	 * @param _newVersion: new version number
	 * @param _newData: new data
	 * @param _discardTime: discard time
	 * @param _operationCode: operationSESSIONWRITE
	 * @param _destAddrs: destination address list
	 */
	public RPCClientThread(String _sessID, int _newVersion, String _newData, String _discardTime,
			int _operationCode, LinkedList<InetAddress> _destAddrs) {
		sessID = _sessID;
		newVersion = _newVersion;
		newData = _newData;
		discardTime = _discardTime;
		operationCode = _operationCode;
		destAddrs = (LinkedList<InetAddress>) _destAddrs.clone();
	}
	
	@Override
	public void run() {
		if (OPERATION_SESSIONREAD == operationCode) {
			SessionRead(sessID);
		}else if(OPERATION_SESSIONWRITE == operationCode) {
			SessionWrite(sessID, newVersion, newData, discardTime);
		}
	}
	
	public void start() {
		if (t == null) {
			t = new Thread (this, threadName);  //instantiate a Thread object
			t.start();
		}
	}
	
	/**
	 * operate SessionRead
	 * @param _sessID: session ID
	 * @return
	 */
	public DatagramPacket SessionRead(String _sessID) {
		DatagramSocket rpcSocket;
		DatagramPacket recvPkt;
		try {
			rpcSocket = new DatagramSocket();
			
			//callID plus 1 for every call
			callID += 1;
			
			//fill outBuf with callID, operation code and session ID
			byte[] outBuf;
			String outString = callID + "," + OPERATION_SESSIONREAD + "," + _sessID;
			outBuf = outString.getBytes(); 
			
			//send to destination addresses
			for(InetAddress destAddr: destAddrs) {
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, PORT_PROJ1_RPC);
				rpcSocket.send(sendPkt);
			}
			
			//receive DatagramPacket and fill inBuf
			byte[] inBuf = new byte[MAX_PACKET_SIZE];
			String inString;
			int recCallID;
			recvPkt = new DatagramPacket(inBuf, inBuf.length);
			do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				inString = new String(inBuf, "UTF-8");
				String[] inDetailsString = inString.split(",");
				recCallID = Integer.parseInt(inDetailsString[0]);  //get callID
			}while (callID != recCallID);
			rpcSocket.close();
			return recvPkt;
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException stoe) {
			recvPkt = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
	
	/**
	 * operate session write
	 * @param _sessID: session ID
	 * @param _newVersion: new version
	 * @param _newData: new data
	 * @param _discardTime: discard time
	 * @return
	 */
	public DatagramPacket SessionWrite(String _sessID, int _newVersion, String _newData, String _discardTime) {
		DatagramSocket rpcSocket;
		DatagramPacket recvPkt;
		try {
			rpcSocket = new DatagramSocket();
			
			//callID plus 1 for every call
			callID += 1;
			
			//fill outBuf with callID, operation code, session ID, version number, data and discard time
			byte[] outBuf;
			String outString = callID + "," + OPERATION_SESSIONWRITE + "," + _sessID + "," 
					+ _newVersion + "," + _newData + "," + _discardTime;
			outBuf = outString.getBytes(); 
			
			//send to destination addresses
			for(InetAddress destAddr: destAddrs) {
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, PORT_PROJ1_RPC); 
				rpcSocket.send(sendPkt);
			}
			
			//receive DatagramPacket and fill inBuf
			byte[] inBuf = new byte[MAX_PACKET_SIZE];
			String inString;
			int recCallID;
			recvPkt = new DatagramPacket(inBuf, inBuf.length);
			do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				inString = new String(inBuf, "UTF-8");
				String[] inDetailsString = inString.split(",");
				recCallID = Integer.parseInt(inDetailsString[0]);  //get callID
			}while (callID != recCallID);
			rpcSocket.close();
			return recvPkt;
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException stoe) {
			recvPkt = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
}
