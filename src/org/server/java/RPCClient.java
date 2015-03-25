package org.server.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

/**
 * RPC client thread, send call message and receive confirm from server
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
public class RPCClient{
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
	
	//client property
	private static final int SOCKET_TIMEOUT_MILLSEC = 500;
	
	/**
	 * Constructor: for test
	 * @param _sessID: session ID
	 */
	public RPCClient() {
		destAddrs = new LinkedList<InetAddress>();

	}
	
	public static int getCallID() {
		return callID;
	}

	public static void setCallID(int callID) {
		RPCClient.callID = callID;
	}

	public String getSessID() {
		return sessID;
	}

	public void setSessID(String sessID) {
		this.sessID = sessID;
	}

	public int getNewVersion() {
		return newVersion;
	}

	public void setNewVersion(int newVersion) {
		this.newVersion = newVersion;
	}

	public String getNewData() {
		return newData;
	}

	public void setNewData(String newData) {
		this.newData = newData;
	}

	public String getDiscardTime() {
		return discardTime;
	}

	public void setDiscardTime(String discardTime) {
		this.discardTime = discardTime;
	}

	public LinkedList<InetAddress> getDestAddrs() {
		return destAddrs;
	}

	public void setDestAddrs(LinkedList<InetAddress> destAddrs) {
		this.destAddrs = destAddrs;
	}

	public int getOperationCode() {
		return operationCode;
	}

	public void setOperationCode(int operationCode) {
		this.operationCode = operationCode;
	}

	/**
	 * Constructor: for test
	 * @param _sessID: session ID
	 */
	public RPCClient(String _sessID) {
		sessID = _sessID;
		operationCode = OPERATION_SESSIONREAD;
		destAddrs = new LinkedList<InetAddress>();
		
		try {
			// 127.0.0.1 is just for test purpose
			destAddrs.add(InetAddress.getByName("127.0.0.1"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
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
	@SuppressWarnings("unchecked")
	public RPCClient(String _sessID, int _newVersion, String _newData, String _discardTime,
			int _operationCode, LinkedList<InetAddress> _destAddrs) {
		sessID = _sessID;
		newVersion = _newVersion;
		newData = _newData;
		discardTime = _discardTime;
		operationCode = _operationCode;
		destAddrs = (LinkedList<InetAddress>) _destAddrs.clone();
	}
	
	/**
	 * get information for SessionRead
	 * @param _destAddrs
	 */
	public void setReadInfo(LinkedList<InetAddress> _destAddrs) {
		destAddrs.clear();
		destAddrs = (LinkedList<InetAddress>) _destAddrs.clone();
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
			rpcSocket.setSoTimeout(SOCKET_TIMEOUT_MILLSEC);
			
			//callID plus 1 for every call
			callID += 1;
			
			//byte[] outBuf = callID + "," + OPERATION_SESSIONREAD + "," + _sessID
			byte[] outBuf;
			String outString = callID + "," + OPERATION_SESSIONREAD + "," + _sessID;
			outBuf = outString.getBytes(); 
			
			//send to destination addresses
			for(InetAddress destAddr: destAddrs) {
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, PORT_PROJ1_RPC);
				rpcSocket.send(sendPkt);
			}
			
			// receive DatagramPacket and fill inBuf
			// byte[] inBuf = callID + "," + OPERATION_SESSIONREAD + "," + _sessID
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
		} catch (SocketTimeoutException stoe) {  //time out exception
			recvPkt = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
	
	/**
	 * get information for SessionWrite
	 * @param _destAddrs
	 */
	public void setWriteInfo(LinkedList<InetAddress> _destAddrs) {
		destAddrs.clear();
		destAddrs = (LinkedList<InetAddress>) _destAddrs.clone();
	}
	
	/**
	 * operate session write, hail other 
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
			rpcSocket.setSoTimeout(SOCKET_TIMEOUT_MILLSEC);
			
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
		} catch (SocketTimeoutException stoe) {  //time out exception
			recvPkt = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
}
