package com.ibm.watson.self.topics;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;
import okio.Buffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class TopicClient implements WebSocketListener {

	/*          Logging                 */

    private static Logger logger = LogManager.getLogger(TopicClient.class.getName());
    
    /*			Variables				*/
    private WebSocket socket;
    private boolean socketOpen;
    private boolean authenticated = false;
    private OkHttpClient client;
    private HashMap<String, IEvent> subscriptionMap = new HashMap<String, IEvent>();
    private static TopicClient instance = null;
    private String selfId;
    private String token;
    private WebSocketListener listener;
    private WebSocketCall call;
    
    public TopicClient() {
    	this.client = configureHttpClient();
    	this.socketOpen = false;
    	this.listener = this;
    }
    
    public static TopicClient getInstance() {
    	if(instance == null) {
    		instance = new TopicClient();
    	}    	
    	return instance;
    }
    
    private OkHttpClient configureHttpClient() {
    	logger.entry();
    	
    	OkHttpClient.Builder builder = new OkHttpClient.Builder();
    	CookieManager cookieManager = new CookieManager();
    	cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    	
 //   	builder.cookieJar(new JavaNetCookieJar(cookieManager));
    	builder.connectTimeout(60,  TimeUnit.SECONDS);
    	builder.writeTimeout(60, TimeUnit.SECONDS);
    	builder.readTimeout(90, TimeUnit.SECONDS);
    	
    	return logger.exit(builder.build());
    	
    }
    
	/**
	 * Connect to WebSocket server`
	 * @param host - ip address
	 * @param port - port number
	 * @param selfId - unique self id
	 * @param token - bearer token for authentication
	 * @return if connection has been established or not
	 * @throws IOException 
	 * @throws DeploymentException 
	 */
	public boolean connect(String host, String port)  {
		logger.entry();
		Builder builder = new Request.Builder().url(TopicConstants.WS + host 
				+ TopicConstants.COLON + port + TopicConstants.STREAM);
		builder.addHeader(TopicConstants.SELF_ID, this.selfId);
		builder.addHeader(TopicConstants.TOKEN, this.token);
		this.call = WebSocketCall.create(this.client, builder.build());
		this.call.enqueue(this.listener);
		
		return logger.exit(true);
	}
	
    /**
     * Send a message.
     * @param message
     */
    public void sendMessage(JsonObject message) {
    	logger.entry();
    	try {
    		if(this.socketOpen) {
    			message.addProperty(TopicConstants.ORIGIN, this.selfId + TopicConstants.ROOT);
    			socket.sendMessage(RequestBody.create(WebSocket.TEXT, message.toString()));
    		}
    		else
    			logger.info("Not Connected!");
    	}
    	catch (IOException e) {
    		logger.error(e.getMessage());
    	}
		logger.exit();
    }
    
    private void sendMessage(JsonObject wrapperObject, byte[] data) {
    	logger.entry();
		wrapperObject.addProperty(TopicConstants.DATA, data.length);
		wrapperObject.addProperty(TopicConstants.ORIGIN, this.selfId + TopicConstants.ROOT);
		try {
			byte[] header = wrapperObject.toString().getBytes(TopicConstants.UTF8);
			byte[] frame = new byte[header.length + data.length + 1];
			System.arraycopy(header, 0, frame, 0, header.length);
			System.arraycopy(data, 0, frame, header.length + 1, data.length);
			if(this.socketOpen) {
				socket.sendMessage(RequestBody.create(WebSocket.BINARY, frame));
			}
			else
				logger.info("Not Connected!");
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
		logger.exit();
	}
    
    public void publish(String path, String data, boolean persisted) {
    	logger.entry();
    	JsonObject wrapperObject = new JsonObject();
    	JsonArray pathArray = new JsonArray();
    	pathArray.add(new JsonPrimitive(path));
    	wrapperObject.add(TopicConstants.TARGETS, pathArray);
    	wrapperObject.addProperty(TopicConstants.MSG, TopicConstants.PUBLISH_AT);
    	wrapperObject.addProperty(TopicConstants.DATA, data);
    	wrapperObject.addProperty(TopicConstants.BINARY, false);
    	wrapperObject.addProperty(TopicConstants.PERSISTED, persisted);
    	this.sendMessage(wrapperObject);
    	logger.exit();
    }
    
    public void publish(String path, byte[] data, boolean persisted) {
    	logger.entry();
    	JsonObject wrapperObject = new JsonObject();
    	JsonArray pathArray = new JsonArray();
    	pathArray.add(new JsonPrimitive(path));
    	wrapperObject.add(TopicConstants.TARGETS, pathArray);
    	wrapperObject.addProperty(TopicConstants.MSG, TopicConstants.PUBLISH_AT);
    	wrapperObject.addProperty(TopicConstants.BINARY, true);
    	wrapperObject.addProperty(TopicConstants.PERSISTED, persisted);
    	this.sendMessage(wrapperObject, data);
    	logger.exit();
    }

	public void subscribe(String path, IEvent event) {
		logger.entry();
    	if(!subscriptionMap.containsKey(path)) {
    		subscriptionMap.put(path, event);
    	}
    	JsonObject wrapperObject = new JsonObject();
    	JsonArray wrapperArray = new JsonArray();
    	wrapperArray.add(new JsonPrimitive(path));
    	wrapperObject.add(TopicConstants.TARGETS, wrapperArray);
    	wrapperObject.addProperty(TopicConstants.MSG, TopicConstants.SUBSCRIBE);
    	this.sendMessage(wrapperObject);
    	logger.exit();
    }
    
    public boolean unsubscribe(String path, IEvent event) {
    	logger.entry();
    	if(subscriptionMap.containsKey(path)) {
    		subscriptionMap.remove(path);
    		JsonObject wrapperObject = new JsonObject();
    		JsonArray wrapperArray = new JsonArray();
    		wrapperArray.add(new JsonPrimitive(path));
    		wrapperObject.add(TopicConstants.TARGETS, wrapperArray);
    		wrapperObject.addProperty(TopicConstants.MSG, TopicConstants.UNSUBSCRIBE);
    		this.sendMessage(wrapperObject);
    		return logger.exit(true);
    	}
    	
    	return logger.exit(false);
    }
	
    /**
     * Check if socket is open.
     * @return
     */
    public boolean isConnected() {
    	return this.socketOpen && this.authenticated;
    }
    
    /**
     * Add authorization header to HTTP handshake request. Socket must not be open.
     * @param authorization
     */
    public void setHeaders(String selfId, String token) {
    	logger.entry();
		this.selfId = selfId;
		this.token = token;
		logger.exit();
    }

	public void onClose(int arg0, String arg1) {
    	logger.entry();
    	this.socketOpen = false;
    	this.authenticated = false;
        logger.info("closing websocket");
        logger.exit();	
	}

	public void onFailure(IOException arg0, Response arg1) {
		logger.entry();
		logger.info(arg1.toString());
		this.socketOpen = false;
		logger.exit();		
	}

	public void onMessage(ResponseBody message) throws IOException {
		logger.entry();
		String response = message.string();
		JsonParser parser = new JsonParser();
		JsonObject wrapperObject = parser.parse(response).getAsJsonObject();
		if(!wrapperObject.has(TopicConstants.BINARY)) {
			if(wrapperObject.has(TopicConstants.CONTROL)) {
				if (wrapperObject.get(TopicConstants.CONTROL).getAsString().equals(TopicConstants.AUTHENTICATE)) {
					this.authenticated = true;
				}
			}
		}
		else if(!wrapperObject.get(TopicConstants.BINARY).getAsBoolean()) {
			if(subscriptionMap.containsKey(wrapperObject.get(TopicConstants.TOPIC).getAsString())) {
				subscriptionMap.get(wrapperObject.get(TopicConstants.TOPIC).getAsString())
					.onEvent(wrapperObject.get(TopicConstants.DATA).getAsString());
			}
		}
		logger.exit();
		
	}

	public void onOpen(WebSocket socket, Response arg1) {
		logger.entry();
		this.socket = socket;
		this.socketOpen = true;
		logger.info("opening websocket!");	
		logger.exit();
		
	}

	public void onPong(Buffer arg0) {
		logger.info("OnPong() called"); 
	}
}
