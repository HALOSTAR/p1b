package org.server.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
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
	
	//information which needed to deliver to the client
	public static String cookieInfo = "";  //all information of a cookie
	public static String sessExpiration = "";  //session expiration-timestamp
	public static boolean isLogout = true;
	
	//session table
	public static Map<String, String> SessTbl = new ConcurrentHashMap<String, String>();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EnterServlet() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    static {
    	SessTblGarbageThread tSess = new SessTblGarbageThread();
    	tSess.start();
    	
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
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Cookie sessCookie = null;  //cookie needed to pass to client
		String sessData;  //session data stored in the session table
		isLogout = false;
		
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
			
			response.addCookie(sessCookie);
		}
		
		if (sessCookie != null) {
			cookieInfo = sessCookie.getValue();
		}

		//set attribute which needed to send to the client
		request.setAttribute("msg", msg);
		request.setAttribute("curCookieInfo", cookieInfo);
		request.setAttribute("curSessionExpiration", sessExpiration);
		request.setAttribute("isLogout", isLogout);
        RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
        dispatcher.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String hdnParam = request.getParameter("pagename");

		//******************************************************************//
		RPCClient tRPCClient = new RPCClient("abc");
		

		if (hdnParam.equals("response")) {
			Cookie[] cookies = request.getCookies();
			Cookie sessCookie = null;  //cookie needed to pass to client
			String sessData;  //session data stored in the session table
			isLogout = true;
			
			if (cookies != null) {
				for (Cookie cookie: cookies) {
					if (EnterServlet.COOKIE_NAME.equals(cookie.getName())) {
						isLogout = false;
						sessCookie = cookie;
					}
				}
			}
			
			if (isLogout) {
				msg = "You've already logged out, please enter the page again.";
			}else {
				String btnParam = request.getParameter("button");
				
				//button: refresh
				if (btnParam.equals("buttonRefresh")) {
					SessCookieManage.setVersion(sessCookie, SessCookieManage.getVersion(sessCookie) + 1);
					sessCookie.setMaxAge(SESSION_TIMEOUT_SECS);
					response.addCookie(sessCookie);
					
					//get SessTbl information
					//lastSessionData
					String lastSessionData; 
					lastSessionData = SessTbl.get(SessCookieManage.getSessionID(sessCookie)); 
					String sessionDetails[] = lastSessionData.split("_");
					
					//set SessTbl information: message
					msg = sessionDetails[1];

					//set SessTbl information: expiration-timestamp
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.SECOND, sessCookie.getMaxAge());
					sessExpiration = cal.getTime().toString();
					
					sessData = SessCookieManage.getVersion(sessCookie) + "_" + msg 
							+ "_" + sessExpiration;
					SessTbl.put(SessCookieManage.getSessionID(sessCookie), sessData);
				}
				
				//button: buttonReplace
				else if(btnParam.equals("buttonReplace")) {
					SessCookieManage.setVersion(sessCookie, SessCookieManage.getVersion(sessCookie) + 1);
					sessCookie.setMaxAge(SESSION_TIMEOUT_SECS);
					response.addCookie(sessCookie);
					
					//set SessTbl information: message
					msg = request.getParameter("txtReplace");

					//set SessTbl information: expiration-timestamp
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.SECOND, sessCookie.getMaxAge());
					sessExpiration = cal.getTime().toString();
					
					sessData = SessCookieManage.getVersion(sessCookie) + "_" + msg 
							+ "_" + sessExpiration;
					SessTbl.put(SessCookieManage.getSessionID(sessCookie), sessData);
				}
				
				//button: buttonLogout
				else if(btnParam.equals("buttonLogout")) {
					SessTbl.remove(SessCookieManage.getSessionID(sessCookie));
					sessCookie.setMaxAge(0);
					response.addCookie(sessCookie);
					isLogout = true;
					msg = "You've already logged out, please enter the page again.";
				}

				cookieInfo = sessCookie.getValue();
			}

			//set attribute which needed to send to the client
			request.setAttribute("msg", msg);
			request.setAttribute("curCookieInfo", cookieInfo);
			request.setAttribute("curSessionExpiration", sessExpiration);
			request.setAttribute("isLogout", isLogout);
	        RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
	        dispatcher.forward(request, response);
		}
	}

}
