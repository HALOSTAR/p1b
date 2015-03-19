package org.server.java;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class SessTblManageThread implements Runnable {
	private Thread t;
	private String threadName = "CookieMapThread";
	
	@Override
	public void run() {
		while(true) {
			Iterator iter = EnterServlet.SessTbl.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry entry = (Map.Entry)iter.next();
				
				Calendar calNow = Calendar.getInstance();
				Calendar calSession = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
				
				//get expiration-timestamp from SessTbl
				String sessionID = (String)entry.getKey();
				String sessionData = (String)entry.getValue();
				String sessionDetails[] = sessionData.split("_");
				try {
					calSession.setTime(sdf.parse(sessionDetails[2]));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//calNow is later than or equals to calSession
				if (calNow.compareTo(calSession) >= 0) { 
					EnterServlet.SessTbl.remove(sessionID);
				}
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void start() {
		if (t == null) {
			t = new Thread (this, threadName);
			t.start();
		}
	}
}
