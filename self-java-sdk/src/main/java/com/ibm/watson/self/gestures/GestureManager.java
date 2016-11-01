package com.ibm.watson.self.gestures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.self.topics.IEvent;
import com.ibm.watson.self.topics.TopicClient;

public class GestureManager implements IEvent {

	private static GestureManager instance = null;
	private HashMap<String, IGesture> gesturesMap = new HashMap<String, IGesture>();
	private HashMap<String, Boolean> overrideMap = new HashMap<String, Boolean>();
	private List<String> inputMap = new ArrayList<String>();
	private boolean started = false;
	
	public GestureManager() {
		TopicClient.getInstance().subscribe("gesture-manager", this);
		started = true;
	}
	
	public static GestureManager getInstance() {
		if(instance == null) {
			instance = new GestureManager();
		}
		return instance;
	}
	
	/**
	 * registers a new gesture with self
	 * @param gesture - gesture class
	 * @param override - if true then any gesture with same id will be replaced 
	 * and if false then it will execute alongside existing gestures with same id
	 */
	public void addGesture(IGesture gesture, boolean override) {
		String gestureId = gesture.getGestureId();
		if(!gesturesMap.containsKey(gestureId)) {
			if(gesture.onStart()) {
				JsonObject wrapperObject = new JsonObject();
				wrapperObject.addProperty("event", "add_gesture_proxy");
				wrapperObject.addProperty("gestureId", gestureId);
				wrapperObject.addProperty("instanceId", gesture.getInstanceId());
				wrapperObject.addProperty("override", override);
				TopicClient.getInstance().publish("gesture-manager", wrapperObject.toString(), false);
				gesturesMap.put(gestureId, gesture);
				overrideMap.put(gestureId, override);
				System.out.println("Added gesture: " + gestureId);
			}
		}
	}
	
	/**
	 * Removes the provided gesture from remote self
	 * @param gesture - provided gesture
	 */
	public void removeGesture(IGesture gesture) {
		String gestureId = gesture.getGestureId();
		if(gesturesMap.containsKey(gestureId)) {
			if(gesture.onStop()) {
				gesturesMap.remove(gestureId);
				overrideMap.remove(gestureId);
				JsonObject wrapperObject = new JsonObject();
				wrapperObject.addProperty("event", "remove_gesture_proxy");
				wrapperObject.addProperty("gestureId", gestureId);
				wrapperObject.addProperty("instanceId", gesture.getInstanceId());
				TopicClient.getInstance().publish("gesture-manager", wrapperObject.toString(), false);
				System.out.println("Removed gesture: " + gestureId);
			}
		}
	}

	/**
	 * Received when message is returned from remote self
	 */
	public void onEvent(String event) {
		if(inputMap.size() == 0) {
			Thread requests = new Thread(new Worker(event));
			requests.start();
		}
		inputMap.add(event);
	}
	
	/**
	 * Check if Gesture Manager is active
	 */
	public boolean isActive() {
		return started;
	}
	
	/**
	 * Shutdown GestureManager by unsubscribing to remote self
	 */
	public void shutdown() {
		TopicClient.getInstance().unsubscribe("gesture-manager", this);	
		started = false;
	}
	
	/**
	 * Confirms gesture has finished executing and executes any other gesture in queue
	 * @param gesture - gesture 
	 * @param error - if there was any errors
	 */
	public void onGestureDone(IGesture gesture, boolean error) {
		JsonObject wrapperObject = new JsonObject();
		wrapperObject.addProperty("event", "execute_done");
		wrapperObject.addProperty("gestureId", gesture.getGestureId());
		wrapperObject.addProperty("instanceId", gesture.getInstanceId());
		wrapperObject.addProperty("error", error);
		TopicClient.getInstance().publish("gesture-manager", wrapperObject.toString(), false);
		inputMap.remove(0);
		// Perform any more tasks that have been queued up
		if(inputMap.size() > 0) {
			Thread requests = new Thread(new Worker(inputMap.get(0)));
			requests.start();
		}
	}
	
	/**
	 * Worker thread to make execution async
	 *
	 */
	class Worker implements Runnable {
		
		private String event;
		
		public Worker(String event) {
			this.event = event;
		}
		public void run() {
			JsonParser parser = new JsonParser();
			JsonObject wrapperObject = parser.parse(event).getAsJsonObject();
			String gestureId = wrapperObject.get("gestureId").getAsString();
			String instanceId = wrapperObject.get("instanceId").getAsString();
			String eventName = wrapperObject.get("event").getAsString();
			IGesture gesture = gesturesMap.get(gestureId);
			if(gesture == null) {
				System.out.println("Failed to find gesture: " + gestureId);
				return;
			}
			
			if(eventName.equals("execute_gesture")) {
				JsonObject paramsObject = wrapperObject.get("params").getAsJsonObject();
				if(!gesture.execute(paramsObject)) {
					System.out.println("Failed to execute gesture: " + gestureId);
				}
			}
			else if(eventName.equals("abort_gesture")) {
				if(!gesture.abort()) {
					System.out.println("Failed to abort gesture: " + gestureId);
					if(inputMap.size() > 0)
						inputMap.remove(0);
				}
			}
			else {
				JsonObject failedObject = new JsonObject();
				failedObject.addProperty("failed_event", eventName);
				failedObject.addProperty("event", "error");
				TopicClient.getInstance().publish("gesture-manager", failedObject.toString(), false);
				if(inputMap.size() > 0)
					inputMap.remove(0);
			}
		}
		
	}
}
