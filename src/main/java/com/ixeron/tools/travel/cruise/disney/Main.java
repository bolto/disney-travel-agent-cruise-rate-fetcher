package com.ixeron.tools.travel.cruise.disney;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.HttpsURLConnection;

public class Main {

	public enum OS {WINDOWS, LINUX, MAC, SOLARIS, UNKNOWN}
	private List<String> cookies;
	private HttpsURLConnection conn;

	private final String USER_AGENT = "Mozilla/5.0";
	
	String disneyTravelAgentUsername;
	String disneyTravelAgentPassword;
	String gmailUsername;
	String gmailPassword;
	String recipientEmail;

	public String getDisneyTravelAgentUsername() {
		return disneyTravelAgentUsername;
	}

	public void setDisneyTravelAgentUsername(String disneyTravelAgentUsername) {
		this.disneyTravelAgentUsername = disneyTravelAgentUsername;
	}

	public String getDisneyTravelAgentPassword() {
		return disneyTravelAgentPassword;
	}

	public void setDisneyTravelAgentPassword(String disneyTravelAgentPassword) {
		this.disneyTravelAgentPassword = disneyTravelAgentPassword;
	}

	public String getGmailUsername() {
		return gmailUsername;
	}

	public void setGmailUsername(String gmailUsername) {
		this.gmailUsername = gmailUsername;
	}

	public String getGmailPassword() {
		return gmailPassword;
	}

	public void setGmailPassword(String gmailPassword) {
		this.gmailPassword = gmailPassword;
	}

	public String getRecipientEmail() {
		return recipientEmail;
	}

	public void setRecipientEmail(String recipientEmail) {
		this.recipientEmail = recipientEmail;
	}

	public static void main(String[] args) {
		try{
		String disneyTravelAgentUsername = args[0];
		String disneyTravelAgentPassword = args[1];
		String gmailUsername = args[2];
		String gmailPassword = args[3];
		String recipientEmail = args[4];
		
		String url = "https://www.disneytravelagents.ca/login";
		String pdfUrl = "https://media.disneywebcontent.com/StaticFiles/DTA-Domestic/pdf/DCL/DCL_SpecialTARates.pdf";

		Main http = new Main();
		http.setDisneyTravelAgentPassword(disneyTravelAgentPassword);
		http.setDisneyTravelAgentUsername(disneyTravelAgentUsername);
		http.setGmailUsername(gmailUsername);
		http.setGmailPassword(gmailPassword);
		http.setRecipientEmail(recipientEmail);

		// make sure cookies is turn on
		CookieHandler.setDefault(new CookieManager());

		// 1. Send a "GET" request, so that you can extract the form's data.
		String page = http.GetPageContent(url);
		String postParams = http.getFormParams(page, disneyTravelAgentUsername,
				disneyTravelAgentPassword);

		// 2. Construct above post's content and then send a POST request for
		// authentication
		http.sendPost(url, postParams);

		// 3. success then go to download file.
		String timestamp = getTimestampString();
		String filename = String.format("DCL_SpecialTARates_%s.pdf", timestamp);
		String userHomePath = System.getProperty("user.home");
		
		String outputFilePath = String.format("%s/%s", userHomePath,
				filename);
		http.downloadFile(pdfUrl, outputFilePath);

		// 4. success then email the file
		http.emailFile(outputFilePath, filename, timestamp);
		}catch(Exception ex){
			System.out.println("Sample usage: java disney-travel-agent-cruise-rate-fetcher agent_name@gmail.com agentpassword some_name@gmail.com some_password send_to_name@gmail.com");
		}
	}

	private static String getTimestampString() {
		java.util.Date date = new java.util.Date();
		String timestamp = "" + new Timestamp(date.getTime());
		timestamp = timestamp.replace("-", "_");
		timestamp = timestamp.replace(" ", "_");
		timestamp = timestamp.replace(":", "_");
		timestamp = timestamp.replace(".", "_");
		return timestamp;
	}

	private void emailFile(String filepath, String attachmentFilename,
			String timestamp) {
		final String username = getGmailUsername();
		final String password = getGmailPassword();

		Properties props = new Properties();
		props.put("mail.smtp.auth", true);
		props.put("mail.smtp.starttls.enable", true);
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});
		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(getGmailUsername()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress
					.parse(getRecipientEmail()));
			message.setSubject(String.format(
					"Disney Cruise Travel Agent Rate %s", timestamp));
			message.setText(String.format("Disney Cruise Travel Agent Rate %s",
					timestamp));

			MimeBodyPart messageBodyPart = new MimeBodyPart();

			Multipart multipart = new MimeMultipart();

			messageBodyPart = new MimeBodyPart();
			String file = filepath;

			DataSource source = new FileDataSource(file);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(attachmentFilename);
			multipart.addBodyPart(messageBodyPart);

			message.setContent(multipart);

			System.out.println("Sending");

			Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	private void sendPost(String url, String postParams) throws Exception {

		URL obj = new URL(url);
		conn = (HttpsURLConnection) obj.openConnection();

		// Acts like a browser
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Host", "accounts.google.com");
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		for (String cookie : this.cookies) {
			conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
		}
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("Referer",
				"https://accounts.google.com/ServiceLoginAuth");
		conn.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length",
				Integer.toString(postParams.length()));

		conn.setDoOutput(true);
		conn.setDoInput(true);

		// Send post request
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(postParams);
		wr.flush();
		wr.close();

		int responseCode = conn.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + postParams);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		// System.out.println(response.toString());

	}

	private void downloadFile(String dlUrl, String outputFilePath) {

		URL url = null;
		try {
			url = new URL(dlUrl);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			conn = (HttpsURLConnection) url.openConnection();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// default is GET
		try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		conn.setUseCaches(false);

		// act like a browser
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (cookies != null) {
			for (String cookie : this.cookies) {
				conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
			}
		}
		int responseCode = 0;
		try {
			responseCode = conn.getResponseCode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		InputStream is = null;
		try {
			is = conn.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OutputStream os = null;
		try {
			os = new FileOutputStream(outputFilePath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] b = new byte[2048];
		int length;

		try {
			while ((length = is.read(b)) != -1) {
				os.write(b, 0, length);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String GetPageContent(String url) throws Exception {

		URL obj = new URL(url);
		conn = (HttpsURLConnection) obj.openConnection();

		// default is GET
		conn.setRequestMethod("GET");

		conn.setUseCaches(false);

		// act like a browser
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (cookies != null) {
			for (String cookie : this.cookies) {
				conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
			}
		}
		int responseCode = conn.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// Get the response cookies
		setCookies(conn.getHeaderFields().get("Set-Cookie"));

		return response.toString();

	}

	public String getFormParams(String html, String username, String password)
			throws UnsupportedEncodingException {

		System.out.println("Extracting form's data...");

		Document doc = Jsoup.parse(html);

		// Google form id
		Element loginform = doc.getElementById("loginForm");
		Elements inputElements = loginform.getElementsByTag("input");
		List<String> paramList = new ArrayList<String>();
		for (Element inputElement : inputElements) {
			String key = inputElement.attr("name");
			String value = inputElement.attr("value");

			if (key.equals("login"))
				value = username;
			else if (key.equals("password"))
				value = password;
			paramList.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
		}

		// build parameters list
		StringBuilder result = new StringBuilder();
		for (String param : paramList) {
			if (result.length() == 0) {
				result.append(param);
			} else {
				result.append("&" + param);
			}
		}
		return result.toString();
	}

	public List<String> getCookies() {
		return cookies;
	}

	public void setCookies(List<String> cookies) {
		this.cookies = cookies;
	}
	private static String OPERATING_SYSTEM = System.getProperty("os.name").toLowerCase();

	public static OS getOS() {
		System.out.println(OPERATING_SYSTEM);
		if (OPERATING_SYSTEM.indexOf("win") >= 0) {
			return OS.WINDOWS;
		} else if (OPERATING_SYSTEM.indexOf("mac") >= 0) {
			return OS.MAC;
		} else if (OPERATING_SYSTEM.indexOf("nix") >= 0 || OPERATING_SYSTEM.indexOf("nux") >= 0 || OPERATING_SYSTEM.indexOf("aix") > 0 ) {
			return OS.LINUX;
		} else if (OPERATING_SYSTEM.indexOf("sunos") >= 0) {
			return OS.SOLARIS;
		} else {
			return OS.UNKNOWN;
		}
	}
}
