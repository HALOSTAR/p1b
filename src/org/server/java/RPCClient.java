package org.server.java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
	
	public static final String SVRID_NULL= "SVRIDNULL"; 
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
	 * Clear LinkedList<InetAddress> destAddrs
	 */
	public void clearDestAddrs() {
		this.destAddrs.clear();
	}
	
	/**
	 * Add _addr to LinkedList<InetAddress> destAddrs
	 * @param _addr
	 */
	public void addDestAddr(String _addr) {
		if (! _addr.equals(SVRID_NULL)) {
			try {
				this.destAddrs.add(InetAddress.getByName(_addr));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
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
		
		// @hans
		//String curDestAddr=null;
		
		try {
			rpcSocket = new DatagramSocket();
			rpcSocket.setSoTimeout(SOCKET_TIMEOUT_MILLSEC);
			
			//byte[] outBuf = callID + "," + OPERATION_SESSIONREAD + "," + _sessID
			callID += 1;  //callID plus 1 for every call
			String outString = callID + "," + OPERATION_SESSIONREAD + "," + _sessID;
			byte[] outBuf = outString.getBytes(); 
			
			//send to destination addresses
			for(InetAddress destAddr: destAddrs) {
				
				//@hans
				//curDestAddr = destAddr.toString();
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, RPCServerThread.PORT_PROJ1_RPC);
				rpcSocket.send(sendPkt);
			}
			
			// receive DatagramPacket and fill inBuf
			// byte[] inBuf = callID + "," + foundVersion + "," + foundData
			byte[] inBuf = new byte[RPCServerThread.MAX_PACKET_SIZE];
			String inString;
			int recCallID;
			recvPkt = new DatagramPacket(inBuf, inBuf.length);
			do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				inString = new String(inBuf, "UTF-8");
				String[] inDetailsString = inString.split(",");
				recCallID = Integer.parseInt(inDetailsString[0]);  //get callID
			}while (callID != recCallID); // check if the sent callid is back  
			rpcSocket.close();
			return recvPkt;
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException stoe) {  //time out exception
			
			//@hans
			//ViewServerThread inst = new ViewServerThread();
			//inst.setServerDown(curDestAddr);
			recvPkt = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
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
			
			//byte[] outBuf =  callID + "," + OPERATION_SESSIONWRITE + "," + _sessID + "," 
			//				+ _newVersion + "," + _newData + "," + _discardTime
			String outString = callID + "," + OPERATION_SESSIONWRITE + "," + _sessID + "," 
					+ _newVersion + "," + _newData + "," + _discardTime;
			byte[] outBuf = outString.getBytes(); 
			
			//send to destination addresses
			for(InetAddress destAddr: destAddrs) {
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, RPCServerThread.PORT_PROJ1_RPC); 
				rpcSocket.send(sendPkt);
			}
			
			//receive DatagramPacket and fill inBuf
			//byte[] inBuf = callID + "," + "OK"
			byte[] inBuf = new byte[RPCServerThread.MAX_PACKET_SIZE];
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
	 * get session value from recvPkt of SessionRead 
	 * @param _recvPkt: byte[] inBuf = callID + "," + foundVersion + "," + foundData
	 * @return: session value: version + "_" + msg
	 * @throws UnsupportedEncodingException 
	 */
	public static String getSessValueFromRecvPkt(DatagramPacket _recvPkt) throws UnsupportedEncodingException {
		byte[] buf = _recvPkt.getData();
		//bufString = callID + "," + foundVersion + "," + foundData
		String bufString = new String(buf, "UTF-8");
		String[] bufDetails = bufString.split(",");
		return bufDetails[1] + "_" + bufDetails[2];
	}
}
