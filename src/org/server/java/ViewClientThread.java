package org.server.java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Date;
import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Iterator;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataResult;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.ListDomainsRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.Attribute;

public class ViewClientThread implements Runnable {
		private Thread t;
		private String threadName = "ViewClientThread";
		private static int callID = 0;  //increase 1 by every call
		private String sessID;
		private int newVersion;
		private String newData;
		private String discardTime;
		
		private LinkedList<InetAddress> destAddrs;
		private int operationCode;
		

		//operation code
		private static final int OPERATION_VIEWREAD = 3;//RPCServerThread.OPERATION_VIEWREAD;  //1
		private static final int OPERATION_VIEWWRITE = 4;//RPCServerThread.OPERATION_VIEWWRITE;  //2
		private static final int OPERATION_VIEWWITHSIMPLEDB = 5;//RPCServerThread.OPERATION_VIEWWRITE;  //2

		//server property
		private static final int PORT_PROJ1_RPC = RPCServerThread.PORT_PROJ1_RPC;
		private static final int MAX_PACKET_SIZE = RPCServerThread.MAX_PACKET_SIZE;
		
		static AmazonSimpleDB sdb; //initialized in init() with credentials
		static Map<String,String> ServerView = new ConcurrentHashMap<String,String>(); 
	    final static String domainName = "ServerMembership";
	    final static String attribute1 = "ServerID";
	    final static String attribute2 = "Status"; //up or down
	    final static String attribute3 = "TimeStamp";
	    final static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);

	    /**
	     * The only information needed to create a client are security credentials
	     * consisting of the AWS Access Key ID and Secret Access Key. All other
	     * configuration, such as the service endpoints, are performed
	     * automatically. Client parameters, such as proxies, can be specified in an
	     * optional ClientConfiguration object when constructing a client.
	     *
	     * @see com.amazonaws.auth.BasicAWSCredentials
	     * @see com.amazonaws.auth.PropertiesCredentials
	     * @see com.amazonaws.ClientConfiguration
	     */
	    private static void init() throws Exception {

	        /*
	         * The ProfileCredentialsProvider will return your [default]
	         * credential profile by reading from the credentials file located at
	         * (/home/cs4752/.aws/credentials).
	         */
	        AWSCredentials credentials = null;
	        try {
	            credentials = new ProfileCredentialsProvider("default").getCredentials();
	        } catch (Exception e) {
	            throw new AmazonClientException(
	                    "Cannot load the credentials from the credential profiles file. " +
	                    "Please make sure that your credentials file is at the correct " +
	                    "location (/home/cs4752/.aws/credentials), and is in valid format.",
	                    e);
	        }

	        sdb = new AmazonSimpleDBClient(credentials);
	    }
		
		/**
		 * Constructor: for test
		 * @param _sessID: session ID
		 */
		public ViewClientThread(String _sessID) {
			sessID = _sessID;
			operationCode = OPERATION_VIEWREAD;
			destAddrs = new LinkedList<InetAddress>();
			try {
				destAddrs.add(InetAddress.getByName("127.0.0.1"));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * constructor: for operationVIEWREAD
		 * @param _sessID: session ID
		 * @param _operationCode: operationVIEWREAD
		 * @param _destAddrs: destination address list
		 */
		public ViewClientThread(String _sessID, int _operationCode, LinkedList<InetAddress> _destAddrs) {
			sessID = _sessID;
			operationCode = _operationCode;
			destAddrs = (LinkedList<InetAddress>) _destAddrs.clone();
		}
		
		/**
		 * constructor: for operationVIEWREAD
		 * @param _sessID: session ID
		 * @param _newVersion: new version number
		 * @param _newData: new data
		 * @param _discardTime: discard time
		 * @param _operationCode: operationVIEWWRITE
		 * @param _destAddrs: destination address list
		 */
		public ViewClientThread(String _sessID, int _newVersion, String _newData, String _discardTime,
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
			
			if (OPERATION_VIEWWITHSIMPLEDB == operationCode)
				ViewExchangeWithSimpleDB(sessID);
			else if (OPERATION_VIEWREAD == operationCode) {
				SessionRead(sessID);
			}else if(OPERATION_VIEWWRITE == operationCode) {
				SessionWrite(sessID, newVersion, newData, discardTime);
			}
		}
		
		public void start() {
			if (t == null) {
				t = new Thread (this, threadName);  //instantiate a Thread object
				t.start();
			}
			
			boolean simpleDBexists = false;
			System.out.println("===========================================");
		    System.out.println("Welcome to the AWS Java SDK!");
		    System.out.println("===========================================");

		    try{
		    	init();
		    } catch (Exception e){
		    	System.out.println("Initialize view thread exception!");
		    }
		    //find if simpleDB with domainName exists, if not create a simpleDB
	        try {
	            ListDomainsRequest sdbRequest = new ListDomainsRequest().withMaxNumberOfDomains(100);
	            ListDomainsResult sdbResult = sdb.listDomains(sdbRequest);
	            int totalItems = 0;
	            for (String domainN : sdbResult.getDomainNames()) {
	            	if(domainN.equals(domainName)) simpleDBexists=true;
	                	DomainMetadataRequest metadataRequest = new DomainMetadataRequest().withDomainName(domainN);
	                	DomainMetadataResult domainMetadata = sdb.domainMetadata(metadataRequest);
	                	totalItems += domainMetadata.getItemCount();
	            }

	            		System.out.println("You have " + sdbResult.getDomainNames().size() + " Amazon SimpleDB domain(s)" +
	                    "containing a total of " + totalItems + " items.");
	            
	            
	        } catch (AmazonServiceException ase) {
	                System.out.println("Caught Exception: " + ase.getMessage());
	                System.out.println("Reponse Status Code: " + ase.getStatusCode());
	                System.out.println("Error Code: " + ase.getErrorCode());
	                System.out.println("Request ID: " + ase.getRequestId());
	        }
	        
		    if (simpleDBexists==false) {
		    	sdb.createDomain(new CreateDomainRequest(domainName));		    
		    	System.out.println("SimpleDB domain created "+sdb.listDomains());
		    }
		    //Thread.sleep(10000);
			
			/*
			 * String svrID = EnterServlet.svrIDLocal;
				String sessID = EnterServlet.sess_num + "_" + svrID;
			 */
			//server puts in its own serverID-up-timestamp
			ServerView.put(EnterServlet.svrIDLocal, "up-"+(sdf.format(System.currentTimeMillis())).toString());
			//server puts simpleDB domainName-up-timestamp
			ServerView.put(domainName, "up-"+(sdf.format(System.currentTimeMillis())).toString());
			
			//server chooses a random gossip partner from View, with probability near (1/View_size)
			//Returns a pseudo-random number between min and max, inclusive
			// NOTE: Usually this should be a field rather than a method
		    // variable so that it is not re-seeded every call.
		    Random rand = new Random();
		    // nextInt is normally exclusive of the top value, so add 1 to make it inclusive
		    int min = 0;
		    int max = ServerView.size()-1;
		    int randomNum = 0;
		    String gossipServerID= EnterServlet.svrIDLocal;
		    while(gossipServerID.equals(EnterServlet.svrIDLocal)) {
			    randomNum = rand.nextInt((max - min) + 1) + min; //randomNum>=0
			    int count = 0;
			    Iterator<Map.Entry<String,String>> it = ServerView.entrySet().iterator();
			    while (count<=randomNum && it.hasNext()) {
			        Map.Entry<String,String> pair = it.next();
			        //System.out.println(pair.getKey() + " = " + pair.getValue());
			        if (count==randomNum 
			        		&& viewGetStatus(pair.getValue()).equals("up"))gossipServerID = pair.getKey();
			        //do not choose server that is down to gossip
			        count++;    
			    }
		    }
		    //exchange view with simpleDB, done by this server and no interaction with any other server
		    if (gossipServerID.equals(domainName)) operationCode = OPERATION_VIEWWITHSIMPLEDB;
		    else {//set serverID for the server want to exchange view with
		    	operationCode = OPERATION_VIEWREAD;
		    	
			    destAddrs.clear();
			    try {
					//destAddrs.add(InetAddress.getByName("127.0.0.1"));
					destAddrs.add(InetAddress.getByName(gossipServerID));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
		    }
		}
		
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
		
		public void ViewExchangeWithSimpleDB (String _sessID){
			//simpleDB attribute list (not including item name)
			ArrayList<ReplaceableAttribute> lst = new ArrayList<ReplaceableAttribute>();
			
			Iterator<Map.Entry<String,String>> itr = ServerView.entrySet().iterator();
		    while (itr.hasNext()) {
		        Map.Entry<String,String> pair = itr.next();
		        //System.out.println(pair.getKey() + " = " + pair.getValue());
				String sID = pair.getKey();
				String stat= viewGetStatus(pair.getValue());
				String time= viewGetTimestamp(pair.getValue());
					System.out.println(sID+stat+time);
				String query ="select * "+" from `"+ domainName+ 
						"` where "+attribute1+"="+"\'"+sID+"\'";
				System.out.println(query);
				SelectResult result=null;
				String resultsID =null;	String resultStatus =null; 	String resultTimestamp=null;
				try{
					result= sdb.select(new SelectRequest(query));
					
					}catch(AmazonServiceException e){
						System.out.println("query failed: "+query);
					}
				System.out.println("result "+result);
				//serverID is in table, get values of each attribute
				if(result!=null) {
					//ArrayList<Item> itlst= new ArrayList<Item>(result.getItems());
					//resultsID= itlst.get(0).getName(); //Item name = ServerID
					for (Item it: result.getItems()) {
					//ArrayList<Attribute> itAttributes = new ArrayList<Attribute>(it.getAttributes());
						for (Attribute at: it.getAttributes()) {
							if((at.getName()).equals(attribute1)) { //ServerID attribute
								resultsID= at.getValue();
							}
							if((at.getName()).equals(attribute2)) { //status attribute
								resultStatus= at.getValue();
							}
							if((at.getName()).equals(attribute3)) { //timestamp attribute
								resultTimestamp= at.getValue();
							}
						}
					}
				}
				System.out.println("resultsID "+resultsID);
				System.out.println("resultStatus "+resultStatus);
				System.out.println("resultTimestamp "+resultTimestamp);
				if(resultsID==null || !(resultsID.equals(sID) )) { //not in table, add serverID entry
					lst.clear();
					System.out.println("serverID NOT found");
					lst.add(new ReplaceableAttribute(attribute1,sID, true));
					lst.add(new ReplaceableAttribute(attribute2,stat, true));
					lst.add(new ReplaceableAttribute(attribute3,time, true));
					PutAttributesRequest put = new PutAttributesRequest(domainName,sID,lst);
					sdb.putAttributes(put);
				}
				else { //serverID is in table, compare timestamps and replace with the latest info
					boolean ok=false;
					Date d1=null;
					Date d2=null; 
					try {
						d1= sdf.parse(time); //server's tuple timestamp
						d2= sdf.parse(resultTimestamp); //simpleDB's tuple timestamp
						ok=true;
					} catch (ParseException e){
						e.printStackTrace();
					}
					if(ok && d1!=null && d2!=null && d1.after(d2)) {
						lst.clear();
						System.out.println("serverID found and server's time is more recent");
						lst.add(new ReplaceableAttribute(attribute1,sID, true));
						lst.add(new ReplaceableAttribute(attribute2,stat, true));
						lst.add(new ReplaceableAttribute(attribute3,time, true));
						PutAttributesRequest put = new PutAttributesRequest(domainName,sID,lst);
						sdb.putAttributes(put);
					}
					else {//replace server's view with simpleDB's sID tuple info
						System.out.println("serverID found and simpleDB's time is more recent");
						ServerView.put(resultsID, resultStatus+"-"+resultTimestamp);
					}
				}
			} //while (itr.hasNext()) 
		    
		    //Put all simpleDB's tuples that are not in server's view into server's view
		    String query ="select * "+" from `"+ domainName+"`"; 
			System.out.println(query);
			SelectResult result=null;
			String resultsID =null;	String resultStatus =null; 	String resultTimestamp=null;
			try{
				result= sdb.select(new SelectRequest(query));
				
				}catch(AmazonServiceException e){
					System.out.println("query failed: "+query);
				}
			System.out.println("result "+result);
			//serverID from table, get values of each attribute
			if(result!=null) {
				//ArrayList<Item> itlst= new ArrayList<Item>(result.getItems());
				//resultsID= itlst.get(0).getName(); //Item name = ServerID
				for (Item it: result.getItems()) {//Item labels a tuple in simpleDB
				//ArrayList<Attribute> itAttributes = new ArrayList<Attribute>(it.getAttributes());
					for (Attribute at: it.getAttributes()) {
						if((at.getName()).equals(attribute1)) { //ServerID attribute
							resultsID= at.getValue();
						}
						if((at.getName()).equals(attribute2)) { //status attribute
							resultStatus= at.getValue();
						}
						if((at.getName()).equals(attribute3)) { //timestamp attribute
							resultTimestamp= at.getValue();
						}
					}
				//Put all simpleDB's tuples that are not in server's view into server's view	
				if (!ServerView.containsKey(resultsID)) 
					ServerView.put(resultsID, resultStatus+"-"+resultTimestamp);
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
			String inString="blank_blank_blank";
			try {
				rpcSocket = new DatagramSocket();
				
				//callID plus 1 for every call
			/*	callID += 1;
			
				//fill outBuf with callID, operation code and session ID
				byte[] outBuf;
				String outString = callID + "," + OPERATION_VIEWREAD + "," + _sessID;
				outBuf = outString.getBytes(); 
				
				//send to destination addresses
				for(InetAddress destAddr: destAddrs) {
					DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, PORT_PROJ1_RPC);
					rpcSocket.send(sendPkt);
				}
				*/
				//receive DatagramPacket and fill inBuf
				byte[] inBuf = new byte[MAX_PACKET_SIZE];
				
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
				//return recvPkt;
				
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (SocketTimeoutException stoe) {
				recvPkt = null;
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			
			//simpleDB attribute list (not including item name)
			ArrayList<ReplaceableAttribute> lst = new ArrayList<ReplaceableAttribute>();

			//receive String inString, look up in table: String is: SvrID_status_time
			//String tuple ="11-up-"+(sdf.format(System.currentTimeMillis())).toString();
			if(inString==null || inString.equals("") ||inString.equals("_")||inString.equals(",")) 
				return null;
			String[] inSt = inString.split(",");
			inString = inSt[3]; //outString = callID + "," + OPERATION_VIEWREAD +  "," + _sessID + ","  +(ViewString);
			String outString= "";
			if(inString.charAt(0)=='_') inString=inString.substring(1);
			
			String[] tuples = inString.split("_");
			for (int i=0; i<tuples.length; i++){
				if(tuples[i]==null || (tuples[i]).equals("")) continue;
				String[] attrib = tuples[i].split("-");
				String sID = attrib[0];
				String stat= attrib[1];
				String time= attrib[2];
					System.out.println(sID+stat+time);
				String query ="select * "+" from `"+ domainName+ 
						"` where "+attribute1+"="+"\'"+sID+"\'";
				System.out.println(query);
				SelectResult result=null;
				String resultsID =null;	String resultStatus =null; 	String resultTimestamp=null;
				try{
					result= sdb.select(new SelectRequest(query));
					
					}catch(AmazonServiceException e){
						System.out.println("query failed: "+query);
					}
				System.out.println("result "+result);
				//serverID is in table, get values of each attribute
				if(result!=null) {
					//ArrayList<Item> itlst= new ArrayList<Item>(result.getItems());
					//resultsID= itlst.get(0).getName(); //Item name = ServerID
					for (Item it: result.getItems()) {
					//ArrayList<Attribute> itAttributes = new ArrayList<Attribute>(it.getAttributes());
						for (Attribute at: it.getAttributes()) {
							if((at.getName()).equals(attribute1)) { //ServerID attribute
								resultsID= at.getValue();
							}
							if((at.getName()).equals(attribute2)) { //status attribute
								resultStatus= at.getValue();
							}
							if((at.getName()).equals(attribute3)) { //timestamp attribute
								resultTimestamp= at.getValue();
							}
						}
					}
				}
				System.out.println("resultsID "+resultsID);
				System.out.println("resultStatus "+resultStatus);
				System.out.println("resultTimestamp "+resultTimestamp);
				if(resultsID==null || !(resultsID.equals(sID) )) { //not in table, add serverID entry
					lst.clear();
					System.out.println("serverID NOT found");
					lst.add(new ReplaceableAttribute(attribute1,sID, true));
					lst.add(new ReplaceableAttribute(attribute2,stat, true));
					lst.add(new ReplaceableAttribute(attribute3,time, true));
					PutAttributesRequest put = new PutAttributesRequest(domainName,sID,lst);
					sdb.putAttributes(put);
				}
				else { //serverID is in table, compare timestamps and replace with the latest info
					boolean ok=false;
					Date d1=null;
					Date d2=null; 
					try {
						d1= sdf.parse(time);
						d2= sdf.parse(resultTimestamp);
						ok=true;
					} catch (ParseException e){
						e.printStackTrace();
					}
					if(ok && d1!=null && d2!=null && d1.after(d2)) {
						lst.clear();
						System.out.println("serverID found and sender's time is more recent");
						lst.add(new ReplaceableAttribute(attribute1,sID, true));
						lst.add(new ReplaceableAttribute(attribute2,stat, true));
						lst.add(new ReplaceableAttribute(attribute3,time, true));
						PutAttributesRequest put = new PutAttributesRequest(domainName,sID,lst);
						sdb.putAttributes(put);
					}
					else {//send to sender to replace sender's view with simpleDB's sID tuple info
						System.out.println("serverID found and simpleDB's time is more recent");
						if(outString.equals("")) outString= sID+"-"+stat+"-"+time;
						else outString= outString+"_"+sID+"-"+stat+"-"+time;
					}
				}
			} //for (int i=0; i<tuples.length; i++){
			
			//send to sender only the View tuples that are different (between sender's and SimpleDB's
			//fill outBuf with callID, operation code and session ID
			try {
				rpcSocket = new DatagramSocket();
				
				//callID plus 1 for every call
				callID += 1;
				
				//fill outBuf with callID, operation code, session ID, version number, data and discard time
				byte[] outBuf;
				//outString = callID + "," + OPERATION_VIEWREAD +  "," + _sessID + ","  +(ViewString);
				outString = callID + "," + OPERATION_VIEWWRITE + _sessID + "," +outString;
				outBuf = outString.getBytes(); 
				/*
				 * String svrID = EnterServlet.svrIDLocal;
					String sessID = EnterServlet.sess_num + "_" + svrID;
				 */
				//send to destination addresses
				for(InetAddress destAddr: destAddrs) {
					DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, PORT_PROJ1_RPC); 
					rpcSocket.send(sendPkt);
				}
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
				String outString = callID + "," + OPERATION_VIEWWRITE + "," + _sessID + "," 
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
