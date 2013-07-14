package com.veivo.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import com.veivo.core.domain.MonitorURLBean;
import com.veivo.util.VeivoHelper;

public class HttpClientUtil {
	
	public static final String MOZILLA_USER_AGENT= "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; Q312461)";
	
	static HttpClient httpClient = new HttpClient (new MultiThreadedHttpConnectionManager());
	
	/**
     * Log4j logger
     */
    static Logger logger = Logger.getLogger(HttpClientUtil.class);
    
    static Timer tmr;
    
    private static class InstanceHolder {
		private static HttpClientUtil instance = new HttpClientUtil();
	}
    
    /**
     * Constructor
     *
     */
    private HttpClientUtil () {
    	if (tmr == null) tmr = new Timer("HttpConnTimer");
    }
    
    /**
     * Get instance of HttpClientUtil
     * @return
     */
    public static HttpClientUtil getInstance() {
    	return InstanceHolder.instance;
    }
    
    /**
     * Cancel timer 
     *
     */
    public static void cancelTimer () {
    	if (tmr != null) tmr.cancel();
    }
    
    /**
     * Recommend method to get http url response by Apache http client util
     * @param url
     * @param validate
     * @return
     * @throws NetworkException
     */
    public MonitorURLBean getHttpResponseBean (String url, boolean validate) 
    throws NetworkException 
    {
    	return getHttpResponseBean (url, validate, true,"en");
    }
    public MonitorURLBean getHttpResponseBean (String url, boolean validate,String referer) 
    throws NetworkException 
    {
    	return getHttpResponseBean (url, validate, true,"en",referer);
    }
    public MonitorURLBean getHttpResponseBeanByLang (String url, boolean validate,String lang) 
    throws NetworkException 
    {
    	return getHttpResponseBean (url, validate, true,lang);
    }

    /**
     * Recommend method to get http url response by Apache http client util
     * @param url
     * @param validate
     * @return
     * @throws NetworkException
     */
    public MonitorURLBean getHttpResponseBean (String url, boolean validate, boolean reportError,String lang) 
    throws NetworkException 
    {
    	if(logger.isDebugEnabled()){
    		logger.debug("getHttpResponseBean from url = " + url);
    	}
    	//test if it is a wellform url
    	try {
			new URL (url);
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
    	MonitorURLBean urlBean = new MonitorURLBean();
    	urlBean.setLink(url);
    	GetMethod getMethod = new GetMethod (url);
    	getMethod.getParams().setSoTimeout(60000);
    	//getMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);  
    	getMethod.addRequestHeader("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 6.0; zh-CN; rv:1.8.1.3) Gecko/20070309 Firefox/2.0.0.3");
    	getMethod.addRequestHeader("Accept","text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
    	//getMethod.addRequestHeader("Accept-Language","zh-cn,zh;q=0.5");
    	//getMethod.addRequestHeader("Accept-Encoding","gzip,deflate");
    	getMethod.addRequestHeader("Accept-Charset","utf-8;q=0.7,*;q=0.7");
    	getMethod.addRequestHeader("Keep-Alive","300");
    	getMethod.addRequestHeader("Connection","keep-alive");
    	getMethod.addRequestHeader("If-Modified-Since","Thu, 26 Aug 2002 08:24:16 GMT"); 
    	
    	//if(lang!=null&&lang.equals("zh")){
    	if(true){
    		getMethod.addRequestHeader("Accept-Language","en-us,en;q=0.5");
    	}
    	getMethod.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    	getMethod.setFollowRedirects(true);
    	TimerTask task = new ConnectionTimer(getMethod);
    	try {
    		tmr.schedule(task, 120000);
    		int respCode;
    		respCode = httpClient.executeMethod(getMethod);
    		urlBean.setExpectedLength(getMethod.getResponseContentLength());
    		String urlAfter = getMethod.getURI().toString();
    		urlAfter = CommonUtil.cleanURL(urlAfter);
    		//URL might changed due to redirect
    		if (!url.equals(urlAfter)) {
    			urlBean.setLink(urlAfter);
    		}
    		if (respCode != 200) {
    			logger.warn ("Get http response code is not 200, but " + respCode + " (url = " + url + ")");
    			return urlBean;
    		}
    		//console ("response code is : " + respCode);
    		byte [] content = CommonUtil.getContentFromInputStream(getMethod.getResponseBodyAsStream());
    		String contentType = getMethod.getResponseHeader("Content-Type").toExternalForm();
    		//logger.debug("contentType = " + contentType);
    		if (contentType != null && contentType.toLowerCase().indexOf("xml") != -1) {
				urlBean.setContentType(MonitorURLBean.XML_CONTENT_TYPE);
			} 
//    		else if (content != null && CommonUtil.isXMLType (content)) {
//				urlBean.setContentType(MonitorURLBean.XML_CONTENT_TYPE);
//			} 
    		else if (contentType != null && contentType.toLowerCase().indexOf("text/html") != -1) 
    		{
				urlBean.setContentType(MonitorURLBean.HTML_CONTENT_TYPE);
    		} else {
				logger.warn("Can NOT recognized content type: " + contentType + " from url: " + url);
				throw new NetworkException ("Can NOT processed content type: " + contentType);
			}
    		urlBean.setContent(content);
		} catch (IOException e) {
			logger.warn("getHttpResponseBean error: Network I/O error when visit -  " + url  + " -: " + e.getMessage(), e);
			urlBean.setContent(null);
		} finally {
			task.cancel();
			getMethod.releaseConnection();
		}
		return urlBean;
    }
    public MonitorURLBean getHttpResponseBean (String url, boolean validate, boolean reportError,String lang,String referer) 
    throws NetworkException 
    {
    	if(logger.isDebugEnabled()){
    		logger.debug("getHttpResponseBean from url = " + url);
    	}
    	//test if it is a wellform url
    	try {
			new URL (url);
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
    	MonitorURLBean urlBean = new MonitorURLBean();
    	urlBean.setLink(url);
    	GetMethod getMethod = new GetMethod (url);
    	getMethod.getParams().setSoTimeout(60000);
    	//getMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);  
    	getMethod.addRequestHeader("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 6.0; zh-CN; rv:1.8.1.3) Gecko/20070309 Firefox/2.0.0.3");
    	getMethod.addRequestHeader("Accept","text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
    	//getMethod.addRequestHeader("Accept-Language","zh-cn,zh;q=0.5");
    	//getMethod.addRequestHeader("Accept-Encoding","gzip,deflate");
    	getMethod.addRequestHeader("Accept-Charset","utf-8;q=0.7,*;q=0.7");
    	getMethod.addRequestHeader("Keep-Alive","300");
    	getMethod.addRequestHeader("Connection","keep-alive");
    	getMethod.addRequestHeader("If-Modified-Since","Thu, 26 Aug 2002 08:24:16 GMT"); 
    	getMethod.addRequestHeader("Referer",referer);
    	
    	//if(lang!=null&&lang.equals("zh")){
    	if(true){
    		getMethod.addRequestHeader("Accept-Language","en-us,en;q=0.5");
    	}
    	getMethod.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    	getMethod.setFollowRedirects(true);
    	TimerTask task = new ConnectionTimer(getMethod);
    	try {
    		tmr.schedule(task, 120000);
    		int respCode;
    		respCode = httpClient.executeMethod(getMethod);
    		urlBean.setExpectedLength(getMethod.getResponseContentLength());
    		String urlAfter = getMethod.getURI().toString();
    		urlAfter = CommonUtil.cleanURL(urlAfter);
    		//URL might changed due to redirect
    		if (!url.equals(urlAfter)) {
    			urlBean.setLink(urlAfter);
    		}
    		if (respCode != 200) {
    			logger.warn ("Get http response code is not 200, but " + respCode + " (url = " + url + ")");
    			return urlBean;
    		}
    		//console ("response code is : " + respCode);
    		byte [] content = CommonUtil.getContentFromInputStream(getMethod.getResponseBodyAsStream());
    		String contentType = getMethod.getResponseHeader("Content-Type").toExternalForm();
    		//logger.debug("contentType = " + contentType);
    		if (contentType != null && contentType.toLowerCase().indexOf("xml") != -1) {
				urlBean.setContentType(MonitorURLBean.XML_CONTENT_TYPE);
			} 
//    		else if (content != null && CommonUtil.isXMLType (content)) {
//				urlBean.setContentType(MonitorURLBean.XML_CONTENT_TYPE);
//			} 
    		else if (contentType != null && contentType.toLowerCase().indexOf("text/html") != -1) 
    		{
				urlBean.setContentType(MonitorURLBean.HTML_CONTENT_TYPE);
    		} else {
				logger.warn("Can NOT recognized content type: " + contentType + " from url: " + url);
				throw new NetworkException ("Can NOT processed content type: " + contentType);
			}
    		urlBean.setContent(content);
		} catch (IOException e) {
			logger.warn("getHttpResponseBean error: Network I/O error when visit -  " + url  + " -: " + e.getMessage(), e);
			urlBean.setContent(null);
		} finally {
			task.cancel();
			getMethod.releaseConnection();
		}
		return urlBean;
    }

    /**
     * use for localization
     * @param url
     * @param validate
     * @param reportError
     * @param req
     * @return
     * @throws NetworkException
     */
    public MonitorURLBean getHttpResponseBean (String url, boolean validate, boolean reportError,HttpServletRequest req) 
    throws NetworkException 
    {
    	if(logger.isDebugEnabled()){
    		logger.debug("getHttpResponseBean from url = " + url);
    	}
    	//test if it is a wellform url
    	try {
			new URL (url);
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
    	MonitorURLBean urlBean = new MonitorURLBean();
    	urlBean.setLink(url);
    	GetMethod getMethod = new GetMethod (url);
    	getMethod.getParams().setSoTimeout(60000);
    	getMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);
    	//for localization
    	getMethod.addRequestHeader("Accept-Language",req.getHeader("Accept-Language"));
    	getMethod.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    	getMethod.setFollowRedirects(true);
    	TimerTask task = new ConnectionTimer(getMethod);
    	try {
    		tmr.schedule(task, 120000);
    		int respCode;
    		respCode = httpClient.executeMethod(getMethod);
    		urlBean.setExpectedLength(getMethod.getResponseContentLength());
    		String urlAfter = getMethod.getURI().toString();
    		urlAfter = CommonUtil.cleanURL(urlAfter);
    		//URL might changed due to redirect
    		if (!url.equals(urlAfter)) {
    			urlBean.setLink(urlAfter);
    		}
    		if (respCode != 200) {
    			logger.warn ("Get http response code is not 200, but " + respCode + " (url = " + url + ")");
    			return urlBean;
    		}
    		//console ("response code is : " + respCode);
    		byte [] content = CommonUtil.getContentFromInputStream(getMethod.getResponseBodyAsStream());
    		String contentType = getMethod.getResponseHeader("Content-Type").toExternalForm();
    		//logger.debug("contentType = " + contentType);
    		if (contentType != null && contentType.toLowerCase().indexOf("xml") != -1) {
				urlBean.setContentType(MonitorURLBean.XML_CONTENT_TYPE);
			}
//    		else if (content != null && CommonUtil.isXMLType (content)) {
//				urlBean.setContentType(MonitorURLBean.XML_CONTENT_TYPE);
//			}
			else if (contentType != null && contentType.toLowerCase().indexOf("text/html") != -1) 
    		{
				urlBean.setContentType(MonitorURLBean.HTML_CONTENT_TYPE);
    		} 
			else {
				throw new NetworkException ("Can NOT processed content type: " + contentType);
			}
    		urlBean.setContent(content);
		} catch (IOException e) {
			logger.warn("getHttpResponseBean error: Network I/O error when visit -  " + url  + " -: " + e.getMessage(), e);
			urlBean.setContent(null);
		} finally {
			task.cancel();
			getMethod.releaseConnection();
		}
		return urlBean;
    }    
    /**
     * Validate url bean by HtmlValidator
     * @param urlBean
     * @throws NetworkException 
     */
//    private MonitorURLBean validateURLBean(MonitorURLBean urlBean, boolean reportError) 
//    throws NetworkException 
//    {
//		logger.debug("Validate url bean for url = " + urlBean.getLink());
//    	if (urlBean.getContent() == null) return null;
//		MonitorURLBean newUrlBean = null;
//		try {
//			HtmlValidator validator = new HtmlValidator (urlBean.getLink(), urlBean.getContent(), urlBean.getExpectedLength());
//			int validateCode = validator.getValidationCode();
//			switch (validateCode) {
//				case HtmlValidator.AUTO_REDIRECT_HTML:
//					logger.info("Auto refresh and redirect to url = " + validator.getRedirectURL());
//					newUrlBean = getHttpResponseBean(validator.getRedirectURL(), true);
//					//if (reportError) theReport.removeErrorObject(urlBean.getLink());
//					break;
//				case HtmlValidator.FRAMESET_HTML:
//					logger.info("Found Frame page and main frame url = " + validator.getFrameURL());
//					newUrlBean = getHttpResponseBean(validator.getFrameURL(), true);
//					//if (reportError) theReport.removeErrorObject(urlBean.getLink());
//					break;
//				case HtmlValidator.NO_CLOSE_TAG_HTML:
//					/*if (reportError) {
//						theReport.error(
//								urlBean.getLink(), "Can not find </html> close tag in html page. (url = " 
//								+ urlBean.getLink() + "). Please verify it by click on " + Constant.VEIVO_HOST 
//								+ "/internal/pagemanager?atx=validatepage&pagelink=" 
//								+ urlBean.getLink(), null);
//					} else {*/
//						theReport.warn(
//								urlBean.getLink(), "Can not find </html> close tag in html page. (url = " 
//								+ urlBean.getLink() + "). Please verify it by click on " + Constant.VEIVO_HOST 
//								+ "/internal/pagemanager?atx=validatepage&pagelink=" 
//								+ urlBean.getLink(), null);
//					//}
//					urlBean.setContent(null);
//					break;
//				case HtmlValidator.NO_OPEN_TAG_HTML:
//					theReport.warn(
//							urlBean.getLink(), "Can not find <html> open tag in html page or read timeout. (url = " 
//							+ urlBean.getLink() + "). Please verify it by click on " + Constant.VEIVO_HOST 
//							+ "/internal/pagemanager?atx=validatepage&pagelink=" 
//							+ urlBean.getLink(), null);
//					urlBean.setContent(null);
//					break;
//				case HtmlValidator.REDUNDANT_CONTENT_HTML:
//					logger.info("Redundant html page. url = " + urlBean.getLink());
//					urlBean.setContent(validator.getValidatedContent());
//					//if (reportError) theReport.removeErrorObject(urlBean.getLink());
//					break;
//				case HtmlValidator.WELLFORMED_HTML:
//					logger.info("Welformed html. url = " + urlBean.getLink());
//					//if (reportError) theReport.removeErrorObject(urlBean.getLink());
//					break;
//			}
//		} catch (UnsupportedEncodingException usee) {
//			logger.warn("Encoding exception when validate html page, " + usee, usee);
//		}
//		return newUrlBean;
//	}

	/**
     * Recommend method to get http url response by Apache http client util
     * @param url
     * @return
     * @throws NetworkException
     */
    public byte [] getHttpResponse (String url, String username, String password, int port) 
    throws NetworkException 
    {
    	//test if it is a wellform url
    	String host = "";
    	HttpClient myHttpClient = new HttpClient();
    	try {
			host = new URL (url).getHost();
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
    	byte [] response = new byte [0];
    	GetMethod getMethod = new GetMethod (url);
    	getMethod.getParams().setSoTimeout(60000);
    	getMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);
    	getMethod.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    	getMethod.setFollowRedirects(true);
    	if (username != null) {
    		Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
    		myHttpClient.getState().setCredentials(new AuthScope(host, port, AuthScope.ANY_REALM), defaultcreds);
    	}
    	TimerTask task = new ConnectionTimer(getMethod);
    	long bodyLength = -1;
    	try {
    		tmr.schedule(task, 120000);
    		int respCode = myHttpClient.executeMethod(getMethod);
    		bodyLength = getMethod.getResponseContentLength();
    		if (respCode != 200) {
    			logger.warn ("Get http response code is not 200, but " + respCode + " (url = " + url + ")");
    			throw new NetworkException ("Get http response code is not 200, but " + respCode + " from url - " + url);
    		}
    		//console ("response code is : " + respCode);
    		response =CommonUtil.getContentFromInputStream(getMethod.getResponseBodyAsStream());
    		//console ("content is : " + new String(urlBean.getContent()));
		} catch (IOException e) {
			logger.warn("getHttpResponse error: ", e);
			throw new NetworkException ("Network I/O error: " + e.getMessage() , e);
		} finally {
			task.cancel();
			getMethod.releaseConnection();
		}
		if (bodyLength != -1 && bodyLength > response.length) {
			logger.warn("!!!Found response body length is " + response.length + " which is less than expected length " + bodyLength);
			response = new byte [0]; 
		} 
		return response;
    }
    
    /**
     * Recommend method to get http url response by Apache http client util
     * @param url
     * @return
     * @throws NetworkException
     */
    public byte [] getHttpResponse (String url, HttpClient client) 
    throws NetworkException 
    {
    	return getHttpResponse (url, CookiePolicy.BROWSER_COMPATIBILITY, client);
    }
    
    /**
     * Recommend method to get http url response by Apache http client util
     * @param url
     * @param cookiePolicy 
     * @return
     * @see org.apache.commons.httpclient.cookie.CookiePolicy
     * @throws NetworkException
     */
    public byte [] getHttpResponse (String url, String cookiePolicy, HttpClient client) 
    throws NetworkException 
    {
    	//test if it is a wellform url
    	try {
			new URL (url);
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
    	byte [] response = new byte [0];
    	GetMethod getMethod = new GetMethod (url);
    	getMethod.getParams().setSoTimeout(60000);
    	getMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);
    	getMethod.addRequestHeader("Accept-Encoding", "gzip,deflate,sdch");
    	getMethod.getParams().setCookiePolicy(cookiePolicy);
    	getMethod.setFollowRedirects(true);
    	
    	TimerTask task = new ConnectionTimer(getMethod);
    	long bodyLength = -1;
    	try {
    		tmr.schedule(task, 120000);
    		int respCode;
    		boolean enableProxy = VeivoHelper.isEnableHttpClientProxy();
    		
    		int port = VeivoHelper.getIntHttpClientProxyPort();
    		String ip = VeivoHelper.getHttpClientProxyIp();
    		if(client==null)
    			client = httpClient;
    		
    			if(enableProxy){
    				HostConfiguration hostConfiguration = new HostConfiguration();  
    		    	URI uri = new URI(url, true);  
    		    	hostConfiguration.setHost(uri);  
    		    	client.setHostConfiguration(hostConfiguration);
    				client.getHostConfiguration().setProxy(ip,port);
    			}
    			long start = System.currentTimeMillis();
    			respCode = client.executeMethod(getMethod);
    			
    			
    	
    			
    		
    		bodyLength = getMethod.getResponseContentLength();
    		if (respCode != 200) {
    			logger.warn ("Get http response code is not 200, but " + respCode + " (url = " + url + ")");
    			throw new NetworkException ("Get http response code is not 200, but " + respCode + " from url - " + url);
    		}
    		
    		byte[] responseBody = getMethod.getResponseBody();
    		long end = System.currentTimeMillis();
    		boolean isGzipped = isGzipped(responseBody);
    		InputStream in = null;
    		if(isGzipped)
    			in = new GZIPInputStream(getMethod.getResponseBodyAsStream());
    		else
    			in = getMethod.getResponseBodyAsStream();
    		//console ("response code is : " + respCode);
    		response =CommonUtil.getContentFromInputStream(in);
    		//console ("content is : " + new String(urlBean.getContent()));
		} catch (IOException e) {
			logger.warn("getHttpResponse error: ", e);
			e.printStackTrace();
			throw new NetworkException ("Visit url (" + url + ") network I/O error: " + e.getMessage() , e);
		} finally {
			task.cancel();
			getMethod.releaseConnection();
		}
		if (bodyLength != -1 && bodyLength > response.length) {
			logger.warn("!!!Found response body length is " + response.length + " which is less than expected length " + bodyLength);
			response = new byte [0]; 
		} 
		return response;
    }
    /*
     * Determines if a byte array is compressed. The java.util.zip GZip
     * implementaiton does not expose the GZip header so it is difficult to determine
     * if a string is compressed.
     * 
     * @param bytes an array of bytes
     * @return true if the array is compressed or false otherwise
     * @throws java.io.IOException if the byte array couldn't be read
     */
     public boolean isGzipped(byte[] bytes) throws IOException
     {
          if ((bytes == null) || (bytes.length < 2))
          {
               return false;
          }
          else
          {
                return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
          }
     }    
    
    public  VeivoHttpResponse getHttpResponse (String url, String cookiePolicy, HttpClient client,boolean flag) 
    throws NetworkException 
    {
    	//test if it is a wellform url
    	try {
			new URL (url);
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
		VeivoHttpResponse response = new VeivoHttpResponse();
    	GetMethod getMethod = new GetMethod (url);
    	getMethod.getParams().setSoTimeout(60000);
    	getMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);
    	getMethod.getParams().setCookiePolicy(cookiePolicy);
    	getMethod.setFollowRedirects(flag);
    	
    	TimerTask task = new ConnectionTimer(getMethod);
    	long bodyLength = -1;
    	try {
    		tmr.schedule(task, 120000);
    		int respCode;
    		if (client != null) {
    			respCode = client.executeMethod(getMethod);
    		} else {
    			respCode = httpClient.executeMethod(getMethod);
    		}
    		
    		//console ("response code is : " + respCode);
    		response.setRespHeaders(getMethod.getResponseHeaders());
    		response.setStatuscode(getMethod.getStatusCode());
    		response.setResponse(CommonUtil.getContentFromInputStream(getMethod.getResponseBodyAsStream()));
    		//console ("content is : " + new String(urlBean.getContent()));
		} catch (IOException e) {
			logger.warn("getHttpResponse error: ", e);
			e.printStackTrace();
			throw new NetworkException ("Visit url (" + url + ") network I/O error: " + e.getMessage() , e);
		} finally {
			task.cancel();
			getMethod.releaseConnection();
		}
		/*if (bodyLength != -1 && bodyLength > response.length) {
			logger.warn("!!!Found response body length is " + response.length + " which is less than expected length " + bodyLength);
			response = new byte [0]; 
		} */
		return response;
    }
    
    
    public  VeivoHttpResponse getHttpResponseGzip (String url, String cookiePolicy, HttpClient client) 
    throws NetworkException 
    {
    	//test if it is a wellform url
    	try {
			new URL (url);
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
		VeivoHttpResponse response = new VeivoHttpResponse();
    	GetMethod getMethod = new GetMethod (url);
    	getMethod.getParams().setSoTimeout(60000);
    	getMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);
    	//getMethod.addRequestHeader("Accept-Encoding", "gzip,deflate");
    	getMethod.getParams().setCookiePolicy(cookiePolicy);
    	getMethod.setFollowRedirects(false);
    	
    	TimerTask task = new ConnectionTimer(getMethod);
    	long bodyLength = -1;
    	try {
    		tmr.schedule(task, 120000);
    		int respCode;
    		if (client != null) {
    			respCode = client.executeMethod(getMethod);
    		} else {
    			respCode = httpClient.executeMethod(getMethod);
    		}
    		
    		//console ("response code is : " + respCode);
    		response.setRespHeaders(getMethod.getResponseHeaders());
    		response.setStatuscode(getMethod.getStatusCode());
    		response.setResponse(CommonUtil.getContentFromInputStream(getMethod.getResponseBodyAsStream()));
    		//console ("content is : " + new String(urlBean.getContent()));
		} catch (IOException e) {
			logger.warn("getHttpResponse error: ", e);
			e.printStackTrace();
			throw new NetworkException ("Visit url (" + url + ") network I/O error: " + e.getMessage() , e);
		} finally {
			task.cancel();
			getMethod.releaseConnection();
		}
		/*if (bodyLength != -1 && bodyLength > response.length) {
			logger.warn("!!!Found response body length is " + response.length + " which is less than expected length " + bodyLength);
			response = new byte [0]; 
		} */
		return response;
    }
    /**
     * Recommend method to post to http url and get the response by Apache http client util
     * @param url
     * @param nameValues
     * @return
     * @throws NetworkException
     */
    public VeivoHttpResponse postHttpResponse (String url, NameValuePair [] nameValues, HttpClient client) 
    throws NetworkException 
    {
    	return postHttpResponse (url, nameValues, CookiePolicy.BROWSER_COMPATIBILITY, client);
    }
    /**
     * Recommend method to post to http url and get the response by Apache http client util
     * @param url
     * @param nameValues
     * @return
     * @throws NetworkException
     */
    public VeivoHttpResponse postHttpResponse (String url, NameValuePair [] nameValues, String cookiePolicy, HttpClient client) 
    throws NetworkException 
    {
    	//test if it is a wellform url
    	try {
			new URL (url);
		} catch (MalformedURLException e1) {
			throw new NetworkException (e1.getMessage(), e1);
		}
    	VeivoHttpResponse response = new VeivoHttpResponse();
    	
    	PostMethod postMethod = new PostMethod (url);
    	postMethod.getParams().setSoTimeout(60000);
    	postMethod.addRequestHeader("User-Agent", MOZILLA_USER_AGENT);
    	postMethod.getParams().setCookiePolicy(cookiePolicy);
    	postMethod.addParameters(nameValues);
    	TimerTask task = new ConnectionTimer(postMethod);
    	try {
    		tmr.schedule(task, 120000);
    		if (client != null) {
    			client.executeMethod(postMethod);
    		} else {
    			httpClient.executeMethod(postMethod);
    		}
    		response.setRespHeaders(postMethod.getRequestHeaders());
    		response.setStatuscode(postMethod.getStatusCode());
    		//console ("response code is : " + respCode);
    		response.setResponse(CommonUtil.getContentFromInputStream(postMethod.getResponseBodyAsStream()));
    		//console ("content is : " + new String(urlBean.getContent()));
		} catch (IOException e) {
			logger.warn("postHttpResponse error: ", e);
			throw new NetworkException ("Visit url (" + url + ") network I/O error when visit - " + url + " -: " + e.getMessage() , e);
		} finally {
			task.cancel();
    		postMethod.releaseConnection();
		}
    	return response;
    }
    
    
    
    //debug
    public static void console (Object object) {
    	if(logger.isDebugEnabled()){
    		logger.debug(object);
    	}
    }
    
    /**
     * Inner class to terminate http connection
     * 
     * @author Administrator
     *
     */
    private class ConnectionTimer extends TimerTask {
    	
    	HttpMethod httpMethod;
    	
    	public ConnectionTimer (HttpMethod method) {
    		httpMethod = method;
    	}
    	
    	public void run() {
    		httpMethod.abort();
    	}
    }
    
    public byte[] deleteHttpRequest(String url, HttpClient client) throws HttpException, IOException, NetworkException {
    	DeleteMethod delete = new DeleteMethod(url);
    	boolean enableProxy = VeivoHelper.isEnableHttpClientProxy();
    	int respCode = 0;
    	byte [] response = new byte [0];
    	int port = VeivoHelper.getIntHttpClientProxyPort();
		String ip = VeivoHelper.getHttpClientProxyIp();
    	if(client==null) {
			client = httpClient;
			if(enableProxy){
				HostConfiguration hostConfiguration = new HostConfiguration();  
		    	URI uri = new URI(url, true);  
		    	hostConfiguration.setHost(uri);  
		    	client.setHostConfiguration(hostConfiguration);
				client.getHostConfiguration().setProxy(ip,port);
			}
    	}
		respCode = client.executeMethod(delete);
		if (respCode != 200) {
			logger.warn ("Get http response code is not 200, but " + respCode + " (url = " + url + ")");
			throw new NetworkException ("Get http response code is not 200, but " + respCode + " from url - " + url);
		}	
		byte[] responseBody = delete.getResponseBody();
		boolean isGzipped = isGzipped(responseBody);
		InputStream in = null;
		if(isGzipped)
			in = new GZIPInputStream(delete.getResponseBodyAsStream());
		else
			in = delete.getResponseBodyAsStream();
		
		response =CommonUtil.getContentFromInputStream(in);
		return response;
		
    }
    
    /**
     * Test purpose
     * Cookie: B=84cit611sbmlu&b=3&s=2i
     * Cookie: F=a=_hV35hcsvdbPuWLzYzpkxhiAHvlkJAe6CGEtBfekxbgdA59103dcRsbInzSx&b=fUQO
     * Cookie: Y=v=1&n=94dhg1s6btjce&l=9m0d6rqqr/o&p=m2dvvcn012080000&iz=100080&r=fh&lg=gb&intl=cn
     * Cookie: T=z=FrdxDBFxyxDBhcwGlSvm7AENjI3BjQ1MDBOTjI3TzQ-&a=QAE&sk=DAAh6N4sTtrUiB&d=c2wBTVRVd0FUTXlOemM1T1RVd09ETS0BYQFRQUUBdGlwAWI3STk0RAF6egFGcmR4REJnV0E-&af=QUFBQkFBJnRzPTExMzcwNDAwNjkmcHM9RGhhZFZ5M2M2WnJ0RXlCOEtzYk5vQS0t
     * 
     * @param args
     * @throws Exception
     */
    public static void main (String [] args) throws Exception {
    	//HttpClient client = new HttpClient();
    	HttpClientUtil util = HttpClientUtil.getInstance();
    	/**
    	HttpState initialState = new HttpState();
        // Initial set of cookies can be retrieved from persistent storage and 
        // re-created, using a persistence mechanism of choice,
    	Cookie bCookie = new Cookie(".yahoo.com", "B", "84cit611sbmlu&b=3&s=2i", "/", 3600 * 24 * 100, false);
    	Cookie fCookie = new Cookie(".yahoo.com", "F", "a=_hV35hcsvdbPuWLzYzpkxhiAHvlkJAe6CGEtBfekxbgdA59103dcRsbInzSx&b=fUQO", "/", 3600 * 24 * 100, false);
    	Cookie yCookie = new Cookie(".yahoo.com", "Y", "v=1&n=94dhg1s6btjce&l=9m0d6rqqr/o&p=m2dvvcn012080000&iz=100080&r=fh&lg=gb&intl=cn", "/", 3600 * 24 * 100, false);
    	Cookie tCookie = new Cookie(".yahoo.com", "T", "z=FrdxDBFxyxDBhcwGlSvm7AENjI3BjQ1MDBOTjI3TzQ-&a=QAE&sk=DAAh6N4sTtrUiB&d=c2wBTVRVd0FUTXlOemM1T1RVd09ETS0BYQFRQUUBdGlwAWI3STk0RAF6egFGcmR4REJnV0E-&af=QUFBQkFBJnRzPTExMzcwNDAwNjkmcHM9RGhhZFZ5M2M2WnJ0RXlCOEtzYk5vQS0t", "/", 3600 * 24 * 100, false);
        // and then added to your HTTP state instance
        initialState.addCookie(bCookie);
        initialState.addCookie(fCookie);
        initialState.addCookie(yCookie);
        initialState.addCookie(tCookie);
        client.setState(initialState);
        byte [] response = util.getHttpResponse("http://cn.f150.mail.yahoo.com/ym/ShowFolder?rb=Inbox", CookiePolicy.DEFAULT, client);
        logger.debug("Response from yahoo: \n" + new String(response));
        **/
    	if(logger.isDebugEnabled()){
//    		logger.debug(new String(util.getHttpResponse("http://news.baidu.com/n?cmd=1&class=civilnews&tn=rss", null)));
    		/*
    		 * POST test
    		 */
//    		NameValuePair[] nameValuePairs = new NameValuePair[1];
//    		NameValuePair nvp = new NameValuePair();
//    		nvp.setName("allurl");
//    		nvp.setValue("http%3A%2F%2Fwww.iapps.im%2Ffeed");
//    		nameValuePairs[0] = nvp;
//    		logger.debug(new String(util.postHttpResponse("http://localhost:8080/rest/rss", nameValuePairs, null).getResponse()));
    		/*
    		 * DELETE test
    		 */
    		System.out.println(new String(util.deleteHttpRequest("http://localhost:8080/rest/status?url=http%3A%2F%2Frss.it.sohu.com%2Frss%2Fityejie.xml", null)));
    	}
    }
}