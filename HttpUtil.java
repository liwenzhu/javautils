package com.veivo.utils;


import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.veivo.exception.HttpStatusException;

public class HttpUtil {
	private static final DefaultHttpClient httpClient = new DefaultHttpClient();
	private static final Client restClient;
	
	/*添加请求的基本验证*/
	static {
		String username = Config.getUsername();
		String password = Config.getPassword();
		DefaultClientConfig clientConfig = new DefaultClientConfig();
		restClient = Client.create(clientConfig);
		HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);
		restClient.addFilter(authFilter);
		restClient.setFollowRedirects(true);
		
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		Credentials credentials = new UsernamePasswordCredentials(username,password);
		httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
	}
	
	private HttpUtil() {
		
	}
	
	public static String doPost(String url,File file, String data) {
		HttpPost post = new HttpPost( url );
		MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		entity.addPart( "file", new FileBody(file, "application/zip" ));
		try {
			entity.addPart( "data", new StringBody( data , Charset.forName("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			Log.debug(HttpUtil.class, "post request form data is not suppoert utf-8!");
			e.printStackTrace();
		}
		post.setEntity(entity);
		
		String response = null;
		try {
			response = EntityUtils.toString( httpClient.execute(post).getEntity(), "UTF-8" );
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}
	
	public static String restDoGet(String url) {
		WebResource webResource = restClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		Log.debug(HttpUtil.class, "get method status:" + response.getStatus());
		int status = response.getStatus();
		if(status != 200) {
			if(status == 302) {//302 资源暂时被移动,返回新地址
				return response.getLocation() + "";
			}
			throw new HttpStatusException("Failed: HTTP error code: " 
						+ status);
		}
		return response.getEntity(String.class);
	}
	
	/* For multipart post this method is not work!, use httpclient post */
	public static String restDoPost(String url, MultivaluedMap<String, String> params) {
		WebResource webResource = restClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, params);
		if(response.getStatus() != 200) {
			throw new HttpStatusException("Failed: HTTP error code: " 
						+ response.getStatus());
		}
		return response.getEntity(String.class);
	}
	
	public static void main(String[] args) throws ParseException, ClientProtocolException, IOException {
//		String url = "https://build.phonegap.com/api/v1/apps";
//		String fileurl = "/home/vincent/www/user.zip";
//		JSONObject data = new JSONObject();
//		data.put("title", "ImgDemo");
//		data.put("package", "com.veivo");
//		data.put("version", "0.1");
//		data.put("create_method", "file");
//		data.put("private", "false");
//		File f = new File(fileurl);
//		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
////		MultivaluedMap params = new Form();
//		params.add("data", data.toString());
//		Client c = Client.create();
//		c.addFilter(new LoggingFilter());
//		HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter("157273549@qq.com", "liwenzhu1989");
//		c.addFilter(authFilter);
//		WebResource resource = c.resource(url);
//		
////		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
//		ClientResponse response = resource.queryParams(params).type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,f);
//		System.out.println(response.getEntity(String.class));
		
		/*change to apache httpclient*/
//		DefaultHttpClient client = new DefaultHttpClient();
//		client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
//		Credentials credentials = new UsernamePasswordCredentials("157273549@qq.com","liwenzhu1989");
//		client.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
//		
//		HttpPost post = new HttpPost( url );
//		MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//		entity.addPart( "file", new FileBody(f, "application/zip" ));
//		System.out.println(data.toString());
//		entity.addPart( "data", new StringBody( data.toString()));
//		
//		post.setEntity(entity);
//		
//		String response = EntityUtils.toString( client.execute(post).getEntity(), "UTF-8" );
//		System.out.println(response);
//		client.getConnectionManager().shutdown();
	}
}
