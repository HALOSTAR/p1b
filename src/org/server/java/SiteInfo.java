package org.server.java;

public class SiteInfo {

	//Information pass to the webpage
	private String siteMsg;
	private String siteSvrIDRequest;
	private String siteSvrIDFound;
	private String siteSvrIDFoundPB;
	private String siteSvrIDPrimary;
	private String siteSvrIDBackup;
	private String siteExpirationTime;
	private String siteDiscardTime;
	private String siteView;
	
	/**
	 * constructor
	 */
	public SiteInfo() {
		siteMsg = "";
		siteSvrIDRequest = "";
		siteSvrIDFound = "";
		siteSvrIDFoundPB = "";
		siteSvrIDPrimary = "";
		siteSvrIDBackup = "";
		siteExpirationTime = "";
		siteDiscardTime = "";
	}
	
	/**
	 * set the site information 
	 * when session CANNOT be retrieved
	 */
	public void setSiteNotRetrieve(String _msg) {
		siteMsg = _msg;
		siteSvrIDFound = "[CANNOT RETRIEVE]";
		siteSvrIDFoundPB = "[CANNOT RETRIEVE]";
		siteSvrIDPrimary = "[CANNOT RETRIEVE]";
		siteSvrIDBackup = "[CANNOT RETRIEVE]";
		siteExpirationTime = "[CANNOT RETRIEVE]";
		siteDiscardTime = "[CANNOT RETRIEVE]";
	}
	
	public void setSiteTimeout() {
		siteMsg = "[SESSION TIMEOUT]";
		siteSvrIDPrimary = "[SESSION TIMEOUT]";
		siteSvrIDBackup = "[SESSION TIMEOUT]";
		siteExpirationTime = "[SESSION TIMEOUT]";
		siteDiscardTime = "[SESSION TIMEOUT]";
	}

	public String getSiteMsg() {
		return siteMsg;
	}

	public void setSiteMsg(String siteMsg) {
		this.siteMsg = siteMsg;
	}

	public String getSiteSvrIDRequest() {
		return siteSvrIDRequest;
	}

	public void setSiteSvrIDRequest(String siteSvrIDRequest) {
		this.siteSvrIDRequest = siteSvrIDRequest;
	}

	public String getSiteSvrIDFound() {
		return siteSvrIDFound;
	}

	public void setSiteSvrIDFound(String siteSvrIDFound) {
		this.siteSvrIDFound = siteSvrIDFound;
	}

	public String getSiteSvrIDFoundPB() {
		return siteSvrIDFoundPB;
	}

	public void setSiteSvrIDFoundPB(String siteSvrIDFoundPB) {
		this.siteSvrIDFoundPB = siteSvrIDFoundPB;
	}

	public String getSiteSvrIDPrimary() {
		return siteSvrIDPrimary;
	}

	public void setSiteSvrIDPrimary(String siteSvrIDPrimary) {
		this.siteSvrIDPrimary = siteSvrIDPrimary;
	}

	public String getSiteSvrIDBackup() {
		return siteSvrIDBackup;
	}

	public void setSiteSvrIDBackup(String siteSvrIDBackup) {
		this.siteSvrIDBackup = siteSvrIDBackup;
	}

	public String getSiteExpirationTime() {
		return siteExpirationTime;
	}

	public void setSiteExpirationTime(String siteExpirationTime) {
		this.siteExpirationTime = siteExpirationTime;
	}

	public String getSiteDiscardTime() {
		return siteDiscardTime;
	}

	public void setSiteDiscardTime(String siteDiscardTime) {
		this.siteDiscardTime = siteDiscardTime;
	}

	public String getSiteView() {
		return siteView;
	}

	public void setSiteView(String siteView) {
		this.siteView = siteView;
	}
}
