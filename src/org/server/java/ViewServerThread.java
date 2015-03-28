package org.server.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class ViewServerThread implements Runnable {
	private Thread t;
	private String threadName = "ViewServerThread";
	protected String svrIDLocal;
	
	//static Map<String,String> ServerView = new ConcurrentHashMap<String,String>(); 
											//EnterServlet.ServerView;
	
	//protected final static SimpleDateFormat sdf = ViewClientThread.sdf;
			//new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
	
	// Operation code
	public static final int OPERATION_VIEWREAD = ViewClientThread.OPERATION_VIEWREAD;// 3;
	public static final int OPERATION_VIEWWRITE = ViewClientThread.OPERATION_VIEWWRITE;//4;
	
	// Server property
	public static final int PORT_PROJ1_RPC_VIEWSERVER = 5301;
	public static final int MAX_PACKET_SIZE = 512;
	
	public static final String domainName = ViewClientThread.domainName;
	
	// SimpleDB connect and view keeping 
	// SimpleDB sdb = new SimpleDB("awsAccessId", "awsSecretKey", true);
	public ViewServerThread() {
		//destAddrs = new LinkedList<InetAddress>();

	}
	
	/*  	set date format and provide parsing functions 
		between strings and date values in a thread-safe manner
*/
  //ThreadLocal variable to hold the DateFormat object
  private static final ThreadLocal<DateFormat> sdf
                 = new ThreadLocal<DateFormat>(){
    @Override
    protected DateFormat initialValue() {
        return new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
    }
  };
 
  public Date stringToDate(String s)
                     throws ParseException{
    Date dt = sdf.get().parse(s);
    return dt;
  }
  public String dateToString(Date d)
          throws ParseException{
	  String s = sdf.get().format(d); 
	  return s;
  }
	
	@Override
	public void run() {
	int count=10;
	while (count >0) {
		try {
			svrIDLocal = EnterServlet.getSvrIDLocal(); //by Runtime.exec() in EC2 instances
			break;
		} catch (IOException io) {
			System.out.println("ViewServer: Error getting runtime svrIDLocal");
			count--;
		}
	}
	    try{
		//server puts in its own serverID-up-timestamp
		EnterServlet.ServerView.put(svrIDLocal, "up-"+(dateToString(new Date(System.currentTimeMillis()))));
		//server puts simpleDB domainName-up-timestamp
		EnterServlet.ServerView.put(domainName, "up-"+(dateToString(new Date(System.currentTimeMillis()))));
	    } catch (ParseException e) {
	    	System.out.println("ViewServer: simpleDateFormat parse exception.");
	    	e.printStackTrace();
	    }
		try {
			@SuppressWarnings("resource")
			DatagramSocket rpcSocket = new DatagramSocket(PORT_PROJ1_RPC_VIEWSERVER);
			
			while(true) {
				// receive DatagramPacket and fill inBuf
				//byte[] outBuf = callID + "," + OPERATION_VIEWREAD + "," + _sessID +","+ outView
				//let client server read this gossipServerID's view by VIEWWRITE everything to client
				//outView from client server to be merged with this gossipServerID' view
				byte[] inBuf = new byte[MAX_PACKET_SIZE];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				rpcSocket.receive(recvPkt);
				String inString = new String(inBuf, "UTF-8");
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				
				byte[] outBuf = null;
				switch(getOperationCode(inBuf)) {
				case OPERATION_VIEWREAD:{
					/*
					System.out.println("OPERATION_VIEWREAD");
					if (null != EnterServlet.SessTbl.get(getSessionId(inBuf))) {
						//byte[] outBuf = callID + "," + OPERATION_VIEWREAD + "," + sessID
						outBuf = Arrays.copyOf(recvPkt.getData(), recvPkt.getLength());
					}
					break;
					*/
					//ServerView Tuple= ServerID, status-timestamp: key=ServerID, value=status-timestamp
					String outView="";
					Iterator<Map.Entry<String,String>> itr = EnterServlet.ServerView.entrySet().iterator();
				    while (itr.hasNext()) {
				        Map.Entry<String,String> pair = itr.next();
				        if (outView.equals("")) outView =pair.getKey()+"-"+pair.getValue();
				        else {
				        	outView = outView+"_"+pair.getKey()+"-"+pair.getValue();
				        }
				        //outView = ServerID-Status-Timestamp_ServerID-Status-Timestamp_ ...
				        //System.out.println(pair.getKey() + " = " + pair.getValue());
				    }
				    //byte[] outBuf = callID + "," + OPERATION_VIEWWRITE + "," + _sessID +","+ outView
					//byte[] outBuf;
					String outString = getCallID(inBuf)+ "," + OPERATION_VIEWWRITE + "," 
					+ getSessionId(inBuf) +","+ outView;
					outBuf = outString.getBytes(); 
					break;
				}
				// byte[] outBuf = callID + "," + OPERATION_VIEWWRITE + "," + _sessID +","+ inView
				case OPERATION_VIEWWRITE:{
					/*
					// ViewWrite: write to the session table
					System.out.println("OPERATION_VIEWWRITE");
					//byte[] outBuf = callID + "," + OPERATION_VIEWWRITE + "," + 
					//			sessID + "," + newVersion + "," + newData + "," + discardTime
					updateSessTbl(inBuf);
					outBuf = Arrays.copyOf(recvPkt.getData(), recvPkt.getLength());
					*/
					break;
				}
				}
				
				if (null != outBuf){
					// here outBuf should contain the callID and results of the call
					DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
					rpcSocket.send(sendPkt);
				}
				String inView=  ViewClientThread.getPacketView(inString);
				//System.out.println("The received view is: "+inView);
				
				ViewClientThread vt = new ViewClientThread();
				vt.mergeServerViews(inView);
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
	
	//returns a random "up" ServerID (excluding simpleDB) from View. If none found, returns null
	public static String findRandomServer() {
	    ArrayList<String> upServerIDs = new ArrayList<String>();// = EnterServlet.ServerView.keySet();
	    Iterator<Map.Entry<String,String>> itr = EnterServlet.ServerView.entrySet().iterator();
	    while (itr.hasNext()) {
	    	Map.Entry<String, String> curr = itr.next();
	    	String curServerID = curr.getKey();
	    	//current ServerID is not the SimpleDB in ServerView
	    	if(!curServerID.equals(domainName) && viewGetStatus(curr.getValue()).equals("up")) {
	    		upServerIDs.add(curServerID);
	    	}
	    }
	    if(upServerIDs.isEmpty()) return null;
	    else {
	    	Collections.shuffle(upServerIDs);
	    	return upServerIDs.get(0);
	    }
	   
	}
	
	public String getSvrIDLocal() throws IOException{
		if (EnterServlet.isAWS) {  //it runs in AWS
			String svrIDLocal;
		    Runtime rt = Runtime.getRuntime();
		    Process proc;

		    proc= rt.exec("/opt/aws/bin/ec2-metadata --public-ipv4");
		    System.out.println("ViewServer: capture IP command error");
		    BufferedReader input = new BufferedReader(
		    		new InputStreamReader(proc.getInputStream()));

		    BufferedReader error = new BufferedReader(new 
		         InputStreamReader(proc.getErrorStream()));

		    // read the output from the command
		    String s = null;
		    StringBuilder resp = new StringBuilder();

		    while ((s = input.readLine()) != null) {
		        resp.append(s);
		    }

		    //IP addr returned as:  public-ipv4: ww.xx.yy.zz
		    String[] sArr = (resp.toString()).split(" ");
		    svrIDLocal = sArr[1].trim();

		    // read any errors from the attempted command
		    s=null;
		    while ((s = error.readLine()) != null) {
		        System.out.println(s);
		    }
		    System.out.println("svrLocalIP "+svrIDLocal);
		    
		    input.close();
		    error.close();
		    return svrIDLocal;
		}
		
		else {  //it runs locally
			String svrIDLocal=null;
			URL url;
			BufferedReader br;
			try {
				url = new URL("http://checkip.amazonaws.com/");
				br = new BufferedReader(new InputStreamReader(url.openStream()));
				svrIDLocal = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return svrIDLocal;
		}
		 
	}
	
	public static void setServerDown(String serverID) {
		if (serverID==null) return;
		try{
			ViewServerThread vs = new ViewServerThread();
			String now = vs.dateToString(new Date(System.currentTimeMillis()));
		EnterServlet.ServerView.put(serverID, "down-"+now);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	//ServerView Tuple= ServerID, status-timestamp: key=ServerID, value=status-timestamp
	//ServerView.put("IDvalue", "up-"+sdf.format(System.currentTimeMillis()));
	//return server status= up or down
	public static String viewGetStatus(String value){
		String[] s =value.split("-");
		return s[0]; 
	}
	
	//return server status timestamp
	public static String viewGetTimestamp(String value){
		String[] s =value.split("-");
		return s[1]; 
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
		return Integer.parseInt(bufDetails[1]);
	}
	
	//get session ID
	private String getSessionId(byte[] _buf) throws UnsupportedEncodingException {
		String bufString = new String(_buf, "UTF-8");
		String[] bufDetails = bufString.split(",");
		return bufDetails[2].trim();
	}
	
	
	
}
