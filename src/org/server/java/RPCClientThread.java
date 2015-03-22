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
	
	private static final int operationSESSIONREAD = RPCServerThread.operationSESSIONREAD;
	private static final int operationSESSIONWRITE = RPCServerThread.operationSESSIONWRITE;
	//private static final int portProj1bRPC = RPCServerThread.portProj1bRPC;
	private static final int portProj1bRPC = 5300;
	private static final int maxPacketSize = RPCServerThread.maxPacketSize;
	
	/**
	 * Constructor: for test
	 * @param _sessID: session ID
	 */
	public RPCClientThread(String _sessID) {
		sessID = _sessID;
		operationCode = operationSESSIONREAD;
		destAddrs = new LinkedList<InetAddress>();
		try {
			destAddrs.add(InetAddress.getByName("127.0.0.1"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
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
		if (operationSESSIONREAD == operationCode) {
			SessionRead(sessID);
		}else if(operationSESSIONWRITE == operationCode) {
			SessionWrite(sessID, newVersion, newData, discardTime);
		}
	}
	
	public void start() {
		if (t == null) {
			t = new Thread (this, threadName);
			t.start();
		}
	}
	
	private DatagramPacket SessionRead(String _sessID) {
		DatagramSocket rpcSocket;
		DatagramPacket recvPkt;
		try {
			rpcSocket = new DatagramSocket();
			callID += 1;
			byte[] outBuf;
			
			String outString = callID + "," + operationSESSIONREAD + "," + _sessID;
			outBuf = outString.getBytes(); 
			for(InetAddress destAddr: destAddrs) {
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, portProj1bRPC);
				rpcSocket.send(sendPkt);
			}
			byte[] inBuf = new byte[maxPacketSize];
			String inString;
			int recCallID;
			recvPkt = new DatagramPacket(inBuf, inBuf.length);
			do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				inString = new String(inBuf, "UTF-8");
				String[] inDetailsString = inString.split(",");
				recCallID = Integer.parseInt(inDetailsString[0]);
			}while (callID != recCallID);

			//******************************************************************//
			System.out.println("RECEIVED: " + inString);
			return recvPkt;
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketTimeoutException stoe) {
			recvPkt = null;
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
		return null;
	}
	
	private DatagramPacket SessionWrite(String _sessID, int _newVersion, String _newData, String _discardTime) {
		DatagramSocket rpcSocket;
		DatagramPacket recvPkt;
		try {
			rpcSocket = new DatagramSocket();
			callID += 1;
			byte[] outBuf;
			
			String outString = callID + "," + operationSESSIONWRITE + "," + _sessID + "," 
					+ _newVersion + "," + _newData + "," + _discardTime;
			outBuf = outString.getBytes(); 
			for(InetAddress destAddr: destAddrs) {
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, portProj1bRPC); 
				rpcSocket.send(sendPkt);
			}
			byte[] inBuf = new byte[maxPacketSize];
			String inString;
			int recCallID;
			recvPkt = new DatagramPacket(inBuf, inBuf.length);
			do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				inString = new String(inBuf, "UTF-8");
				String[] inDetailsString = inString.split(",");
				recCallID = Integer.parseInt(inDetailsString[0]);
			}while (callID == recCallID);
			return recvPkt;
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketTimeoutException stoe) {
			recvPkt = null;
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
		return null;
	}
}
