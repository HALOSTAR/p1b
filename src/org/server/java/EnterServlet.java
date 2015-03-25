package org.server.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class EnterServlet
 */
@WebServlet("/EnterServlet")
public class EnterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static String msg = "";  //message needed to show on the site
	public static final String ORG_MSG = "Hello New User!";  //original message

	public static final String COOKIE_NAME = SessCookieManage.COOKIE_NAME;  //cookie name
	public static final int SESSION_TIMEOUT_SECS = SessCookieManage.SESSION_TIMEOUT_SECS;
	public static int sess_num = 0;  //the valid session number start from 1
	public static String svrIDLocal;  //local server ID, initialized at "void init(ServletConfig config)"
	public static String svrIDBackupTest;  //mock server ID, initialized as 127.0.0.2

	//information which needed to deliver to the client
	public static String cookieInfo = "";  //all information of a cookie
	public static String sessExpiration = "";  //session expiration-timestamp
	public static boolean isTimeout = true;

	// Client 
	public static RPCClient rpcClient = new RPCClient();

	// session table
	// key: session ID
	// value: version number + "_" + msg + "_" + sessExpiration;
	public static Map<String, String> SessTbl = new ConcurrentHashMap<String, String>();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public EnterServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	static {
		// initialize SessTbl garbage thread
		SessTblGarbageThread tSess = new SessTblGarbageThread();
		tSess.start();

		// initialize RPC server thread
		RPCServerThread tRPCServer = new RPCServerThread();
		tRPCServer.start();
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		//get loacl server IDs
		URL url;
		BufferedReader br;
		try {
			url = new URL("http://checkip.amazonaws.com/");
			br = new BufferedReader(new InputStreamReader(url.openStream()));
			svrIDLocal = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// for test: make svrIDLocal = 127.0.0.1
		// 			make svrIDBackupTest = 127.0.0.2
		svrIDLocal = "127.0.0.1";
		svrIDBackupTest = "127.0.0.2";
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Cookie sessCookie = null;  //cookie needed to pass to client
		String sessData;  //session data stored in the session table
		isTimeout = false;

		//check whether this is a new client
		Cookie[] cookies = request.getCookies();
		boolean isNew = true; 
		if (cookies != null) {
			for (Cookie cookie: cookies) {
				if (COOKIE_NAME.equals(cookie.getName())){
					sessData = SessTbl.get(SessCookieManage.getSessionID(cookie));

					//check whether session ID is in sessTbl
					if (null == sessData) {
						isNew = true;  //this is a new client (without session ID)
						break;
					}
					else {
						isNew = false;  //this is NOT a new client
						sessCookie = cookie;
						SessCookieManage.setVersion(sessCookie, SessCookieManage.getVersion(sessCookie) + 1);
						sessCookie.setMaxAge(SESSION_TIMEOUT_SECS);
						response.addCookie(sessCookie);

						String sessDetails[] = sessData.split("_");

						//set SessTbl information: message
						msg = sessDetails[1];

						//set SessTbl information: expiration-timestamp
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.SECOND, sessCookie.getMaxAge());
						sessExpiration = cal.getTime().toString();

						sessData = SessCookieManage.getVersion(sessCookie) + "_" + sessDetails[1] 
								+ "_" + sessExpiration;
						SessTbl.put(SessCookieManage.getSessionID(sessCookie), sessData);
						break;
					}
				}
			}
		}

		//this is a new client
		if (isNew) {
			//cookie information
			sessCookie = SessCookieManage.createCookie(); 

			//set SessTbl information: version, message and expiration-timestamp
			msg = ORG_MSG;
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, sessCookie.getMaxAge());
			sessExpiration = cal.getTime().toString();
			sessData = SessCookieManage.getVersion(sessCookie) + "_" + msg + "_" + sessExpiration;
			SessTbl.put(SessCookieManage.getSessionID(sessCookie), sessData);
			
			System.out.println("new sessionID: " + SessCookieManage.getSessionID(sessCookie));

			response.addCookie(sessCookie);
		}

		if (sessCookie != null) {
			cookieInfo = sessCookie.getValue();
		}

		//set attribute which needed to send to the client
		request.setAttribute("msg", msg);
		request.setAttribute("cookieInfo", cookieInfo);
		request.setAttribute("sessExpiration", sessExpiration);
		RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
		dispatcher.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Cookie[] cookies = request.getCookies();
		Cookie sessCookie = null;  //cookie needed to pass to client
		String sessData;  //session data stored in the session table
		isTimeout = true;
		LinkedList<InetAddress> sendAddrs = new LinkedList<InetAddress>();

		//whether one of the cookie has the name "CS5300PROJ1SESSION"
		if (cookies != null) {
			for (Cookie cookie: cookies) {
				if (COOKIE_NAME.equals(cookie.getName())) {
					isTimeout = false;
					sessCookie = cookie;
				}
			}
		}

		if (isTimeout) {
			setTimeoutInfo();
			request.setAttribute("msg", msg);
			request.setAttribute("cookieInfo", cookieInfo);
			request.setAttribute("sessExpiration", sessExpiration);
			RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
			dispatcher.forward(request, response);
		}else {
			// *****************************test: svrIDPrimary & svrIDBackup ************************//
			
//			if (SessCookieManage.getServerIDPrimary(sessCookie).equals(svrIDLocal) 
//					|| SessCookieManage.getServerIDBackup(sessCookie).equals(svrIDLocal)) {
//				//either SvrIDPrimary or SvrIDBackup is the receiving server's own SvrID

			if (SessCookieManage.getServerIDPrimary(sessCookie).equals("testtest") 
					|| SessCookieManage.getServerIDBackup(sessCookie).equals("testtest")) {
				//either SvrIDPrimary or SvrIDBackup is the receiving server's own SvrID

				// Retrieving Session State locally
				// CANNOT find session on local server
				if (null == SessTbl.get(SessCookieManage.getSessionID(sessCookie))) {
					isTimeout = true;
					setTimeoutInfo();
				}else {  // find session on local server
					String btnParam = request.getParameter("button");

					//button: refresh
					if (btnParam.equals("buttonRefresh")) {
						msg = request.getParameter("oldmsg");
						actOnRefresh(sessCookie, msg, sendAddrs);
						response.addCookie(sessCookie);  // pass the cookie to browser
					}

					//button: buttonReplace
					else if(btnParam.equals("buttonReplace")) {
						SessCookieManage.setVersion(sessCookie, SessCookieManage.getVersion(sessCookie) + 1);
						sessCookie.setMaxAge(SESSION_TIMEOUT_SECS);

						//set SessTbl message and expiration-timestamp 
						msg = request.getParameter("txtReplace");
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.SECOND, sessCookie.getMaxAge());
						sessExpiration = cal.getTime().toString();

						// put data to local SessTbl
						sessData = SessCookieManage.getVersion(sessCookie) + "_" + msg 
								+ "_" + sessExpiration;
						SessTbl.put(SessCookieManage.getSessionID(sessCookie), sessData);
						
						
						response.addCookie(sessCookie);  // pass the cookie to browser
					}

					//button: buttonLogout
					else if(btnParam.equals("buttonLogout")) {
						SessTbl.remove(SessCookieManage.getSessionID(sessCookie));
						sessCookie.setMaxAge(0);
						response.addCookie(sessCookie);
						isTimeout = true;
						setTimeoutInfo();
					}

					if (false == isTimeout) {
						cookieInfo = sessCookie.getValue();
					}
				}
				//set attribute which needed to send to the client
				request.setAttribute("msg", msg);
				request.setAttribute("cookieInfo", cookieInfo);
				request.setAttribute("sessExpiration", sessExpiration);
				RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
				dispatcher.forward(request, response);
				
			}else {  // NEITHER SvrIDPrimary NOR SvrIDBackup is the receiving server's own SvrID
				System.out.println("RPC");

				// Retrieving Session State on other server
				sendAddrs.clear();
				sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDPrimary(sessCookie)));
				sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDBackup(sessCookie)));
				rpcClient.setReadInfo(sendAddrs);

				// CANNOT find session on other server
				if (null == rpcClient.SessionRead(SessCookieManage.getSessionID(sessCookie))) { 
					setTimeoutInfo();
				}else {  // find session on other server
					String btnParam = request.getParameter("button");

					//button: refresh
					if (btnParam.equals("buttonRefresh")) {
						msg = request.getParameter("oldmsg");
						actOnRefresh(sessCookie, msg, sendAddrs);
						response.addCookie(sessCookie);  // pass the cookie to browser
					}

					//button: buttonReplace
					else if(btnParam.equals("buttonReplace")) {
					}

					//button: buttonLogout
					else if(btnParam.equals("buttonLogout")) {

					}

					if (false == isTimeout) {
						cookieInfo = sessCookie.getValue();
					}
				}
			}
		}
		//set attribute which needed to send to the client
		request.setAttribute("msg", msg);
		request.setAttribute("cookieInfo", cookieInfo);
		request.setAttribute("sessExpiration", sessExpiration);
		RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
		dispatcher.forward(request, response);
	}

	/**
	 * fill the timeout information to the webpage
	 */
	public void setTimeoutInfo() {
		msg = "[Session Timeout]";
		cookieInfo = "[Session is expired]";
		sessExpiration = "[Session is expired]";
	}
	
	/**
	 * action for button "Refresh"
	 * @param _cookie: cookie from browser
	 * @param _msg: message from browser
	 */
	public void actOnRefresh(Cookie _cookie, String _msg, LinkedList<InetAddress> _sendAddrs) {
		/*
		// Old version
		try {
			SessCookieManage.setVersion(_cookie, SessCookieManage.getVersion(_cookie) + 1);
			_cookie.setMaxAge(SESSION_TIMEOUT_SECS);

			//set SessTbl message and expiration-timestamp 
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, _cookie.getMaxAge());
			sessExpiration = cal.getTime().toString();

			String sessData = SessCookieManage.getVersion(_cookie) + "_" + msg 
					+ "_" + sessExpiration;
			SessTbl.put(SessCookieManage.getSessionID(_cookie), sessData);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		*/
		
		try {
			SessCookieManage.setVersion(_cookie, SessCookieManage.getVersion(_cookie) + 1);
			SessCookieManage.setServerIDPrimary(_cookie, svrIDLocal);
			
			//****************** unfinished: look for svrIDBackup in view ********************//
			//here, we also need to consider no new_backup can be found
			

			//**************** test: set a fix svrIDBackup as svrIDBackupTest ****************//
			SessCookieManage.setServerIDBackup(_cookie, svrIDBackupTest);
			_cookie.setMaxAge(SESSION_TIMEOUT_SECS);

			//set SessTbl message and expiration-timestamp 
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, _cookie.getMaxAge());
			sessExpiration = cal.getTime().toString();

			// Storing Session State on backup server
			_sendAddrs.clear();
			_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDPrimary(_cookie)));
			_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDBackup(_cookie)));
			rpcClient.setWriteInfo(_sendAddrs);
			rpcClient.SessionWrite(SessCookieManage.getSessionID(_cookie), 
					SessCookieManage.getVersion(_cookie), msg, sessExpiration);

			// put data to local SessTbl
			String sessData = SessCookieManage.getVersion(_cookie) + "_" + msg 
					+ "_" + sessExpiration;
			SessTbl.put(SessCookieManage.getSessionID(_cookie), sessData);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * action for button "Replace"
	 * @param _cookie: cookie from browser
	 * @param _msg: message from browser
	 */
	public void actOnReplace(Cookie _cookie, String _msg) {
		
	}
	
	/**
	 * action for button "Logout"
	 */
	public void actOnLogout() {
		
	}
}
