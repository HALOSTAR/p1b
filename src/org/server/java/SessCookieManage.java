package org.server.java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.Cookie;

public class SessCookieManage {
	
	public static final String COOKIE_NAME = "CS5300PROJ1SESSION";
	public static final int SESSION_TIMEOUT_SECS = 60 * 1;
	public static final int SESSION_TIMEOUT_DELTA_SECS = 5;
	
	/** creat a new cookie
	 * sessionID = <session number, server ID>;
	 * version number = 0;  //version number will plus 1 for every request
	 * sessionID_version_svrIDPrimary_svrIDBackup, sessionID = session number_sererID
	 * MaxAge = 60 * 5;  //5 minutes
	 * we use "_" to connect the tuples here. 
	 * @throws IOException 
	 */
	public static Cookie createCookie() throws IOException {
		EnterServlet.sess_num += 1;
		String svrID = EnterServlet.svrIDLocal;
		String sessID = EnterServlet.sess_num + "_" + svrID;
		String version = "" + 0;
		String svrIDPrimary = EnterServlet.svrIDLocal;
		String svrIDBackup = RPCClient.SVRID_NULL;
		String cookieValue = sessID + "_" + version + "_" + svrIDPrimary + "_" + svrIDBackup;
		
		Cookie cookie = new Cookie(COOKIE_NAME, cookieValue);
		cookie.setMaxAge(SESSION_TIMEOUT_SECS);
		return cookie;
	}
	
	/** Get session ID from Cookie _cookie */
	public static String getSessionID(Cookie _cookie) throws UnsupportedEncodingException {
		String valueDetails[] = _cookie.getValue().split("_");
		String sessionID = valueDetails[0] + "_" + valueDetails[1];
		return sessionID;
	}

	/** Get version number from Cookie _cookie */
	public static int getVersion(Cookie _cookie) throws UnsupportedEncodingException {
		String valueDetails[] = _cookie.getValue().split("_");
		return Integer.parseInt(valueDetails[2]);
	}

	/** Set version number to Cookie _cookie */
	public static void setVersion(Cookie _cookie, int _version) throws UnsupportedEncodingException {
		String valueDetails[] = _cookie.getValue().split("_");
		String newCookieValue = valueDetails[0] + "_" + valueDetails[1] + "_" 
				+ _version + "_" + valueDetails[3] + "_" + valueDetails[4];
		_cookie.setValue(newCookieValue);
	}
	
	/** Get primary server ID from Cookie _cookie */
	public static String getServerIDPrimary(Cookie _cookie) {
		String valueDetails[] = _cookie.getValue().split("_");
		return valueDetails[3];
	}
	
	/** Set primary server ID to Cookie _cookie */
	public static void setServerIDPrimary(Cookie _cookie, String _svrIDPrimary) {
		String valueDetails[] = _cookie.getValue().split("_");
		String newCookieValue = valueDetails[0] + "_" + valueDetails[1] + "_" 
				+ valueDetails[2] + "_" + _svrIDPrimary + "_" + valueDetails[4];
		_cookie.setValue(newCookieValue);
	}
	
	/** Get backup server ID from Cookie _cookie */
	public static String getServerIDBackup(Cookie _cookie) {
		String valueDetails[] = _cookie.getValue().split("_");
		return valueDetails[4];
	}
	
	/** Set backup server ID to Cookie _cookie */
	public static void setServerIDBackup(Cookie _cookie, String _svrIDBackup) {
		//@hans
		//if (_cookie==null || _svrIDBackup==null) return;
		String valueDetails[] = _cookie.getValue().split("_");
		String newCookieValue = valueDetails[0] + "_" + valueDetails[1] + "_" 
				+ valueDetails[2] + "_" + valueDetails[3] + "_" + _svrIDBackup;
		_cookie.setValue(newCookieValue);
	}
}
