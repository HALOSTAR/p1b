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
	//public static boolean isTimeout = true;

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

		// *****************************test: svrIDPrimary & svrIDBackup ************************//
		// test svrIDPrimary: svrIDLocal = 127.0.0.1
		// 					svrIDBackupTest = 127.0.0.2
		// test svrIDBackup: svrIDLocal = 127.0.0.2
		//					svrIDBackupTest = 127.0.0.1
		svrIDLocal = "127.0.0.2";
		svrIDBackupTest = "127.0.0.1";
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Cookie[] cookies = request.getCookies();
		Cookie sessCookie = null;  //cookie needed to pass to client
		
		boolean isTimeout = true;  
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

		// whether this is a valid session in local or remote server
		if (!isTimeout) {
			if(!retrieveSession(sessCookie, sendAddrs)){  // CANNOT retrieve session
				isTimeout = true;
			}else {  // successfully retrieve session
				msg = request.getParameter("oldmsg");
				storeSession(false, sessCookie, msg, sendAddrs);  //update an old session
				response.addCookie(sessCookie);  // pass the cookie to browser
			}
		}else {  // this is NOT a valid session
			// create a new cookie
			sessCookie = SessCookieManage.createCookie(); 
			
			// update SessTbl
			msg = ORG_MSG;
			storeSession(true, sessCookie, msg, sendAddrs);  //create a new session
			isTimeout = false;
			response.addCookie(sessCookie);
		}

		// set attribute value
		if (!isTimeout) {
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
		boolean isTimeout = true;  // true if session is valid
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

		// this is a valid session in local or remote server
		if (!isTimeout) {
			if(!retrieveSession(sessCookie, sendAddrs)){  // CANNOT retrieve session
				isTimeout = true;
			}else {  // successfully retrieve session
				
				String btnParam = request.getParameter("button");
				//button: refresh
				if (btnParam.equals("buttonRefresh")) {
					msg = request.getParameter("oldmsg");
					storeSession(false, sessCookie, msg, sendAddrs);  //update an old session
					response.addCookie(sessCookie);  // pass the cookie to browser
				}
				//button: buttonReplace
				else if(btnParam.equals("buttonReplace")) {
					msg = request.getParameter("txtReplace");
					storeSession(false, sessCookie, msg, sendAddrs);  //update an old session
					response.addCookie(sessCookie);  // pass the cookie to browser
				}
				//button: buttonLogout
				else if(btnParam.equals("buttonLogout")) {
					SessTbl.remove(SessCookieManage.getSessionID(sessCookie));
					sessCookie.setMaxAge(0);
					isTimeout = true;  // session time out
					response.addCookie(sessCookie);
				}
			}
			
		}else {  // this is NOT a valid session
			// create a new cookie
			sessCookie = SessCookieManage.createCookie(); 
			
			// update SessTbl
			msg = ORG_MSG;
			storeSession(true, sessCookie, msg, sendAddrs);
			isTimeout = false;  //create a new session
			response.addCookie(sessCookie);
		}
		
		// set attribute value
		if (!isTimeout) {
			cookieInfo = sessCookie.getValue();
		}else {
			setTimeoutInfo();
		}
		//add attribute to request
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
	 * @param _cookie: cookie from browser
	 * @return: retrieve session successfully
	 */
	public boolean retrieveSession(Cookie _cookie, LinkedList<InetAddress> _sendAddrs) {
		// *****************************test: svrIDPrimary & svrIDBackup ************************//
		
//		//either SvrIDPrimary or SvrIDBackup is the receiving server's own SvrID
//		if (SessCookieManage.getServerIDPrimary(_cookie).equals(svrIDLocal) 
//				|| SessCookieManage.getServerIDBackup(_cookie).equals(svrIDLocal)) {

		//either SvrIDPrimary or SvrIDBackup is the receiving server's own SvrID
		if (SessCookieManage.getServerIDPrimary(_cookie).equals("testtest") 
				|| SessCookieManage.getServerIDBackup(_cookie).equals("testtest")) {

			try {
				System.out.println("retrieve test(key): " + SessCookieManage.getSessionID(_cookie));
				System.out.println("retrieve test(value): " + SessTbl.get(SessCookieManage.getSessionID(_cookie)));
				
				if (null != SessTbl.get(SessCookieManage.getSessionID(_cookie))) {
					return true;
				}else {
					// Retrieving Session State from another server
					if (SessCookieManage.getServerIDPrimary(_cookie).equals(svrIDLocal)) {
						_sendAddrs.clear();
						_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDBackup(_cookie)));
					}else {
						_sendAddrs.clear();
						_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDPrimary(_cookie)));
					}
					rpcClient.setReadInfo(_sendAddrs);

					// @ TODO find session on other server
					if (null != rpcClient.SessionRead(SessCookieManage.getSessionID(_cookie))) { 
						return true;
					}
				}
				
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			return false;
			
		}else {  // NEITHER SvrIDPrimary NOR SvrIDBackup is the receiving server's own SvrID
			System.out.println("RPC");

			// Retrieving Session State on other server
			try {
				_sendAddrs.clear();
				_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDPrimary(_cookie)));
				_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDBackup(_cookie)));
				rpcClient.setReadInfo(_sendAddrs);

				// find session on other server
				if (null != rpcClient.SessionRead(SessCookieManage.getSessionID(_cookie))) {
					return true;
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	
	/**
	 * store session state
	 * @param _isNew: whether this is a new client
	 * @param _cookie: cookie from browser
	 * @param _msg: message from browser
	 * @param _sendAddrs: distination address list
	 */
	public void storeSession(boolean _isNew, Cookie _cookie, String _msg, LinkedList<InetAddress> _sendAddrs) {
		
//		// Old version
//		try {
//			SessCookieManage.setVersion(_cookie, SessCookieManage.getVersion(_cookie) + 1);
//			_cookie.setMaxAge(SESSION_TIMEOUT_SECS);
//
//			//set SessTbl message and expiration-timestamp 
//			Calendar cal = Calendar.getInstance();
//			cal.add(Calendar.SECOND, _cookie.getMaxAge());
//			sessExpiration = cal.getTime().toString();
//
//			String sessData = SessCookieManage.getVersion(_cookie) + "_" + msg 
//					+ "_" + sessExpiration;
//			SessTbl.put(SessCookieManage.getSessionID(_cookie), sessData);
//			
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
		
		try {
			if (!_isNew){  // update version for valid session
				SessCookieManage.setVersion(_cookie, SessCookieManage.getVersion(_cookie) + 1);
			}
			SessCookieManage.setServerIDPrimary(_cookie, svrIDLocal);
			
			//****************** unfinished: look for svrIDBackup in view ********************//
			//here, we also need to consider no new_backup can be found
			

			//**************** test: set svrIDBackup as svrIDBackupTest ****************//
			SessCookieManage.setServerIDBackup(_cookie, svrIDBackupTest);
			_cookie.setMaxAge(SESSION_TIMEOUT_SECS);

			//set SessTbl message and expiration-timestamp 
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, _cookie.getMaxAge());
			sessExpiration = cal.getTime().toString();

			// Storing Session State on primary(local) server, put data to local SessTbl
			String sessData = SessCookieManage.getVersion(_cookie) + "_" + msg 
					+ "_" + sessExpiration;
			SessTbl.put(SessCookieManage.getSessionID(_cookie), sessData);
			
			System.out.println("store test(key): " + SessCookieManage.getSessionID(_cookie));
			System.out.println("store test(value): " + SessTbl.get(SessCookieManage.getSessionID(_cookie)));

			// Storing Session State on backup server
			_sendAddrs.clear();
			_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDPrimary(_cookie)));
			_sendAddrs.add(InetAddress.getByName(SessCookieManage.getServerIDBackup(_cookie)));
			rpcClient.setWriteInfo(_sendAddrs);
			rpcClient.SessionWrite(SessCookieManage.getSessionID(_cookie), 
					SessCookieManage.getVersion(_cookie), msg, sessExpiration);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
