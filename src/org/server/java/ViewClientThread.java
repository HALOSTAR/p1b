package org.server.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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
import java.lang.ThreadLocal;
import java.text.DateFormat;

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
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;

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
		private static String gossipServerID;
		private static String svrIDLocal;

		//operation code
		protected static final int OPERATION_VIEWREAD = 3;//RPCServerThread.OPERATION_VIEWREAD;  //1
		protected static final int OPERATION_VIEWWRITE = 4;//RPCServerThread.OPERATION_VIEWWRITE;  //2
		protected static final int OPERATION_VIEWWITHSIMPLEDB = 5;//

		//server property
		private static final int PORT_PROJ1_RPC_VIEWSERVER = ViewServerThread.PORT_PROJ1_RPC_VIEWSERVER;
		private static final int MAX_PACKET_SIZE = RPCServerThread.MAX_PACKET_SIZE;
		
		//client property
		private static final int SOCKET_TIMEOUT_MILLSEC = 500;
		private static final int GOSSIP_SECS = 1*1000;//5 * 1000;
		static AmazonSimpleDB sdb; //initialized in init() with credentials
		//static Map<String,String> ServerView = new ConcurrentHashMap<String,String>(); 
											//EnterServlet.ServerView;
		//ServerView Tuple= ServerID, status-timestamp: key=ServerID, value= status-timestamp
	    final static String domainName = "ServerMembership";
	    final static String attribute1 = "ServerID";
	    final static String attribute2 = "Status"; //up or down
	    final static String attribute3 = "TimeStamp";
	    //protected final static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);

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
	    
	    private void init(){
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
	        
	    	int count=10;
	    	while (count >0) {
	    		try {
	    			svrIDLocal = EnterServlet.getSvrIDLocal(); //by Runtime.exec() in EC2 instances
	    			break;
	    		} catch (IOException io) {
	    			//System.out.println("ViewClient: Error getting runtime svrIDLocal");
	    			count--;
	    		}
	    	}
	    }
	    
		public String getSvrIDLocal() throws IOException{
			
			if (EnterServlet.isAWS) {  //it runs in AWS
				String svrIDLocal;
			    Runtime rt = Runtime.getRuntime();
			    Process proc;

			    proc= rt.exec("/opt/aws/bin/ec2-metadata --public-ipv4");
			    	//System.out.println("ViewClient: capture IP command error");
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

		public ViewClientThread() {
			destAddrs = new LinkedList<InetAddress>();

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
		 * get information for VieRead
		 * @param _destAddrs
		 */
		public void setReadInfo(LinkedList<InetAddress> _destAddrs) {
			destAddrs.clear();
			destAddrs = (LinkedList<InetAddress>) _destAddrs.clone();
		}
	
		public void start() {
			if (t == null) {
				t = new Thread (this, threadName);  //instantiate a Thread object
				t.start();
			}
		
			
		}
		
		@Override
		public void run() {
			
			boolean simpleDBexists = false;
			System.out.println("===========================================");
		    System.out.println("Welcome to the AWS Java SDK!");
		    System.out.println("===========================================");
		    
		    init();
		    
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
		    while(true){
				/*
				 * String svrID = svrIDLocal;
					String sessID = EnterServlet.sess_num + "_" + svrID;
				 */
			    try{
				//server puts in its own serverID-up-timestamp
		    	EnterServlet.ServerView.put(svrIDLocal, "up-"+(dateToString(new Date(System.currentTimeMillis()))));
				//server puts simpleDB domainName-up-timestamp
		    	EnterServlet.ServerView.put(domainName, "up-"+(dateToString(new Date(System.currentTimeMillis()))));
			    } catch (ParseException e) {
			    	System.out.println("ViewClient: simpleDateFormat parse exception.");
			    	e.printStackTrace();
			    }
		    	ViewExchangeWithSimpleDB (sessID);
	
				//server chooses a random gossip partner (including simpleDB) from View, with probability near (1/View_size)
			    gossipServerID= findRandomServerOrSimpleDB();
			    //System.out.println("gossipServerID from VewThread" + gossipServerID);
	
			    //exchange view with simpleDB, done by this server and no interaction with any other server
			    if (gossipServerID.equals(domainName)) operationCode = OPERATION_VIEWWITHSIMPLEDB;
			    else {//set serverID for the server want to exchange view with
			    	operationCode = OPERATION_VIEWREAD;
			    	
				    destAddrs.clear();
				    try {
						//destAddrs.add(InetAddress.getByName("127.0.0.1"));
						destAddrs.add(InetAddress.getByName(gossipServerID));
						//System.out.println("in start(), the gossipServerID chosen: "+ gossipServerID);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
			    }
			    
				Random generator = new Random();
				
				
				if (OPERATION_VIEWWITHSIMPLEDB == operationCode)
					ViewExchangeWithSimpleDB(sessID);
				else if (OPERATION_VIEWREAD == operationCode) {
					ViewRead(sessID);
					/*
					else if(OPERATION_VIEWWRITE == operationCode) {
					}
					*/
			
				}
				try{

				    EnterServlet.putInSimpleDB();
					Thread.sleep((GOSSIP_SECS/2) + generator.nextInt( GOSSIP_SECS ));
				}
				catch (InterruptedException ie){
					System.out.println("In-between gossip sleep exception.");
				}
			} //while(true)

		}
		//ServerView Tuple= ServerID, status-timestamp: key=ServerID, value=status-timestamp
		//ServerView.put("IDvalue", "up-"+sdf.format(System.currentTimeMillis()));
		//return server status= up or down
		public static String viewGetStatus(String value){
			String[] s =value.split("-");
			return (s[0].trim()); 
		}
		
		//return server status timestamp
		public static String viewGetTimestamp(String value){
			String[] s =value.split("-");
			return (s[1].trim()); 
		}
		
		public void ViewExchangeWithSimpleDB (String _sessID){
			//simpleDB attribute list (not including item name)
			ArrayList<ReplaceableAttribute> lst = new ArrayList<ReplaceableAttribute>();
			
			Iterator<Map.Entry<String,String>> itr = EnterServlet.ServerView.entrySet().iterator();
		    while (itr.hasNext()) {
		        Map.Entry<String,String> pair = itr.next();
		        //System.out.println(pair.getKey() + " = " + pair.getValue());
				String sID = pair.getKey();
				String stat= viewGetStatus(pair.getValue());
				String time= viewGetTimestamp(pair.getValue());
				String query ="select * "+" from `"+ domainName+ "` where "+attribute1+"="+"\'"+sID+"\'";
				
				//System.out.println(sID+stat+time);
				//System.out.println(query);
				SelectResult result=null;
				String resultsID =null;	String resultStatus =null; 	String resultTimestamp=null;
				try{
					result= sdb.select(new SelectRequest(query));
					
					}catch(AmazonServiceException e){
						System.out.println("ViewClient: query failed: "+query);
					}
				//System.out.println("result "+result);
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
				//System.out.println("resultsID "+resultsID);
				//System.out.println("resultStatus "+resultStatus);
				//System.out.println("resultTimestamp "+resultTimestamp);
				if(resultsID==null || !(resultsID.equals(sID) )) { //not in table, add serverID entry
					lst.clear();
					//System.out.println("serverID NOT found");
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
						d1= stringToDate(time); //server's tuple timestamp
						d2= stringToDate(resultTimestamp); //simpleDB's tuple timestamp
						ok=true;
					} catch (ParseException e){
						e.printStackTrace();
					}
					if(ok && d1!=null && d2!=null && d1.after(d2)) {
						lst.clear();
						//System.out.println("serverID found and server's time is more recent");
						lst.add(new ReplaceableAttribute(attribute1,sID, true));
						lst.add(new ReplaceableAttribute(attribute2,stat, true));
						lst.add(new ReplaceableAttribute(attribute3,time, true));
						PutAttributesRequest put = new PutAttributesRequest(domainName,sID,lst);
						sdb.putAttributes(put);
					}
					else {//replace server's view with simpleDB's sID tuple info
						//System.out.println("serverID found and simpleDB's time is more recent");
						EnterServlet.ServerView.put(resultsID, resultStatus+"-"+resultTimestamp);
					}
				}
			} //while (itr.hasNext()) 
		    
		    //Put all simpleDB's tuples that are not in server's view into server's view
		    String query ="select * "+" from `"+ domainName+"`"; 
			//System.out.println(query);
			SelectResult result=null;
			String resultsID =null;	String resultStatus =null; 	String resultTimestamp=null;
			try{
				result= sdb.select(new SelectRequest(query));
				
				}catch(AmazonServiceException e){
					System.out.println("query failed: "+query);
				}
			//System.out.println("result "+result);
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
				if (!EnterServlet.ServerView.containsKey(resultsID)) 
					EnterServlet.ServerView.put(resultsID, resultStatus+"-"+resultTimestamp);
				}
			}
		} 

		//returns 0 if failed, return 1 if succeeded
		public static int putItemInSimpleDB (String sID, String stat){
			//sdb must have been created before putting item into simpleDB
			if (sdb==null) return 0;
			ArrayList<ReplaceableAttribute> lst = new ArrayList<ReplaceableAttribute>();
			//System.out.println("ViewClient put: svrID is " + sID);
			lst.add(new ReplaceableAttribute(attribute1,sID, true));
			lst.add(new ReplaceableAttribute(attribute2,stat, true));
			ViewClientThread v= new ViewClientThread();
			String time= null;
			try {
			time= v.dateToString(new Date(System.currentTimeMillis()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			lst.add(new ReplaceableAttribute(attribute3,time, true));
			PutAttributesRequest put = new PutAttributesRequest(domainName,sID,lst);
			sdb.putAttributes(put);
			return 1;
		}
		
		//returns 0 if failed, return 1 if succeeded
		public static int deleteItemInSimpleDB (String sID){
			//sdb must have been created before deleting item from simpleDB
			if (sdb==null) return 0;
			sdb.deleteAttributes(new DeleteAttributesRequest(domainName, sID));
			return 1;
		}
		
		/**
		 * operate ViewRead
		 * @param _sessID: session ID
		 * @return
		 */
		public DatagramPacket ViewRead(String _sessID) {
			DatagramSocket rpcSocket;
			DatagramPacket recvPkt;
			String inString="";
			try {
				rpcSocket = new DatagramSocket();
				rpcSocket.setSoTimeout(SOCKET_TIMEOUT_MILLSEC);
				
				//callID plus 1 for every call
				callID += 1;
				if (callID>=9999) callID=1;
				/*
				 * String svrID = EnterServlet.svrIDLocal;
					String sessID = EnterServlet.sess_num + "_" + svrID;
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
			    //byte[] outBuf = callID + "," + OPERATION_VIEWREAD + "," + _sessID +","+ outView
				byte[] outBuf;
				String outString = callID + "," + OPERATION_VIEWREAD + "," + _sessID +","+ outView;
				outBuf = outString.getBytes(); 
				
				//send to destination addresses
				//Only 1 gossipServerID addr: destAddrs.add(InetAddress.getByName(gossipServerID)) from start()
				for(InetAddress destAddr: destAddrs) {
					DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, PORT_PROJ1_RPC_VIEWSERVER);
					rpcSocket.send(sendPkt);
				}
				
				//gossipServerID will receive OPERATION_VIEWREAD packet and this server's view (outView).
				//gossipServerID will return its own View (inView) to this server
				// receive DatagramPacket and fill inBuf
				// byte[] inBuf = callID + "," + OPERATION_VIEWWRITE + "," + _sessID +","+ inView
				byte[] inBuf = new byte[MAX_PACKET_SIZE];
				String inView="";
				int recCallID;
				recvPkt = new DatagramPacket(inBuf, inBuf.length);
				do {
					recvPkt.setLength(inBuf.length);
					rpcSocket.receive(recvPkt);
					inString = new String(inBuf, "UTF-8");
					String[] inDetailsString = inString.split(",");
					recCallID = Integer.parseInt(inDetailsString[0]);  //get callID
				}while (callID != recCallID); // check if the sent callid is back  
				
				//received the correct callID and thus returned gossipServerID's view
				inView= getPacketView(inString);
				//System.out.println("The received view is: "+inView);
				
				mergeServerViews(inView);
				rpcSocket.close();
				return recvPkt;
				
			} catch (SocketException e) {
				e.printStackTrace();
				
			} catch (SocketTimeoutException stoe) {  //time out exception
				//set this server view's gossipServerID to status "down"
				//Only 1 gossipServerID addr: destAddrs.add(InetAddress.getByName(gossipServerID)) from start()
				//InetAddress itad = InetAddress.getByName("127.0.0.1");
				//String add = itad.getHostAddress(); //returns String 127.0.0.1
				//String name = itad.getHostName(); //returns String 127.0.0.1 (same)
				for(InetAddress destAddr: destAddrs) {
					//gossipServerID= destAddr.getHostAddress();
					//System.out.println("gossipServerID got when timeout: "+ gossipServerID);
					String now=null;
					try {
					now = dateToString(new Date(System.currentTimeMillis()));
					} catch (ParseException e) {
						e.printStackTrace();
					}
					EnterServlet.ServerView.put(gossipServerID, new String("down-")+now);
				}
				recvPkt = null;
				
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			
			return null;
		}
		
		public void mergeServerViews(String inString) { //inString= inView from gossipServerID
			
			//inString = callID + "," + OPERATION_VIEWREAD +  "," + _sessID + ","  + inView;
			//inString = inView, already parsed by ","
			//inView = ServerID-Status-Timestamp_ServerID-Status-Timestamp_ServerID-Status-Timestamp_ ...
			//String tuple ="11-up-"+(sdf.format(System.currentTimeMillis())).toString();
			if(inString==null || inString.equals("") ||inString.equals("_")||inString.equals(",")) 
				return;
			if(inString.charAt(0)=='_') inString=inString.substring(1);
			
			String[] tuples = inString.split("_");
			for (int i=0; i<tuples.length; i++){
				if(tuples[i]==null || (tuples[i]).equals("")) continue;
				String[] attrib = tuples[i].split("-");
				String sID = attrib[0];
				String stat= attrib[1];
				String time= attrib[2];
					//System.out.println(sID+stat+time);
				
			if(EnterServlet.ServerView.containsKey(sID)) {
				//serverID is in this ServerView, compare timestamps and replace with the latest info
					boolean ok=false;
					Date d1=null;
					Date d2=null; 
					try {
						//System.out.println("(test1) viewclient: " + time);
						//System.out.println("(test2) viewclient: " + viewGetTimestamp(EnterServlet.ServerView.get(sID)));
						
						ViewClientThread vc = new ViewClientThread();
						d1= vc.stringToDate(time.trim());
						d2= vc.stringToDate(viewGetTimestamp(EnterServlet.ServerView.get(sID)).trim());
						//System.out.println("d2 "+d2);
						ok=true;
					} catch (ParseException e){
						e.printStackTrace();
					}
					if(ok && d1!=null && d2!=null && d1.after(d2)) {
						//System.out.println("2serverViews: serverID found and gossipServerID's time is latest");
						EnterServlet.ServerView.put(sID, new String(stat+"-"+time));
					}
					//else this server tuple serverID is latest vs gossipServerID's, but already sent this view to gossipServerID
				}
				else {
					//System.out.println("2serverViews: gossipServerID's serverID NOT found");
					EnterServlet.ServerView.put(sID, new String(stat+"-"+time));
				}
			} //for (int i=0; i<tuples.length; i++){
		return;
			
		}
		
		//returns a random "up" ServerID (including simpleDB) from View. If none found, returns null
		public String findRandomServerOrSimpleDB() {
		    ArrayList<String> upServerIDs = new ArrayList<String>();// = EnterServlet.ServerView.keySet();
		    Iterator<Map.Entry<String,String>> itr = EnterServlet.ServerView.entrySet().iterator();
		    while (itr.hasNext()) {
		    	Map.Entry<String, String> curr = itr.next();
		    	String curServerID = curr.getKey();
		    	//current ServerID is not the SimpleDB in ServerView
		    	if(viewGetStatus(curr.getValue()).equals("up")) {
		    		upServerIDs.add(curServerID);
		    	}
		    }
		    if(upServerIDs.isEmpty()) return null;
		    else {
		    	Collections.shuffle(upServerIDs);
		    	return upServerIDs.get(0);
		    }
		   
		}
		
		//inString= callID + "," + OPERATION_VIEWREAD + "," + _sessID +","+ inView
		public static String getPacketView(String inString) {
			String[] s= inString.split(",");
			return s[3];
		}
		
		public static int getCallID() {
			return callID;
		}

		public static void setCallID(int callID) {
			ViewClientThread.callID = callID;
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
}


