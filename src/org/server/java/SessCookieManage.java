package org.server.java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.Cookie;

public class SessCookieManage {
	
	public static final String COOKIE_NAME = "CS5300PROJ1SESSION";
	public static final int SESSION_TIMEOUT_SECS = 60 * 2;
	
	/** creat a new cookie
	 * sessionID = <session number, server ID>;
	 * version number = 0;
	 * location metadata = "NewMetadata"
	 * MaxAge = 60 * 5;  //5 minutes
	 * we use "_" to connect the tuples here. 
	 * we can also use  URLDecoder.decode / URLEncoder.encode
	 * @throws IOException 
	 */
	public static Cookie createCookie() throws IOException {
		EnterServlet.sess_num += 1;
		String svrID = EnterServlet.svrIDLocal;
		String sessID = EnterServlet.sess_num + "_" + svrID;
		String version = "" + 0;
		String svrIDPrimary = svrID;
		String svrIDBackup = "svrIDBackup";
		String metadata = svrIDPrimary + "_" + svrIDBackup;
		String cookieValue = sessID + "_" + version + "_" + metadata;
		
		Cookie cookie = new Cookie(COOKIE_NAME, cookieValue);
		cookie.setMaxAge(SESSION_TIMEOUT_SECS);
		return cookie;
	}
	
	/** Get session ID from Cookie _cookie */
	public static String getSessionID(Cookie _cookie) throws UnsupportedEncodingException {
		String valueDetails[] = _cookie.getValue().split("_");
		return valueDetails[0] + "_" + valueDetails[1];
	}

	/** Get version number from Cookie _cookie */
	public static int getVersion(Cookie _cookie) throws UnsupportedEncodingException {
		String valueDetails[] = _cookie.getValue().split("_");
		int versionNum = Integer.parseInt(valueDetails[2]);
		return versionNum;
	}

	/** Set version number to Cookie _cookie */
	public static void setVersion(Cookie _cookie, int _version) throws UnsupportedEncodingException {
		String valueDetails[] = _cookie.getValue().split("_");
		String newCookieValue = valueDetails[0] + "_" + valueDetails[1] + "_" 
				+ _version + "_" + valueDetails[3] + "_" + valueDetails[4];
		_cookie.setValue(newCookieValue);
	}
	
	/** Get metadata from Cookie _cookie */
	public static String getMetadata(Cookie _cookie) {
		String valueDetails[] = _cookie.getValue().split("_");
		return valueDetails[3] + "_" + valueDetails[4];
	}
}
