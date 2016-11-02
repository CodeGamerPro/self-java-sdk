package com.ibm.watson.self.sensors;

import java.util.UUID;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class MicrophoneSensor implements ISensor {

	private boolean isStarted = false;
	private boolean isPaused = false;
	private boolean isMicrophoneOpen = false;
	private String sensorId;
	
	private TargetDataLine targetDataLine;
	
	public MicrophoneSensor() {
		UUID uuid = UUID.randomUUID();
		sensorId = uuid.toString();
	}
	
	public String getSensorId() {		
        return sensorId;
	}

	public String getSensorName() {
		return "Microphone";
	}

	public String getDataType() {
		return "AudioData";
	}

	public String getBinaryType() {
		return "audio/L16;rate=16000;channels=1";
	}

	public boolean onStart() {
		isStarted = true;
		startListening();
		if(isMicrophoneOpen) {
			Thread thread = new Thread(new CaptureAudio());
			thread.start();
			return true;
		}
		return false;
	}

	public boolean onStop() {
		isStarted = false;
		return true;
	}

	public void onPause() {
		isPaused = true;
	}

	public void onResume() {
		isPaused = false;
	}
	
	public void sendData(byte[] buffer) {
		SensorManager.getInstance().sendData(this, buffer);
	}
	
	/**
	 * Begins capturing audio from line and spawns thread
	 */
    public void startListening()
    {
    	try
    	{
	        AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, true);  
	        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat); 
	        
	        targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);  
	        targetDataLine.open(audioFormat);  
	        targetDataLine.start(); 
	        isMicrophoneOpen = true;
    	}
    	catch (LineUnavailableException e) 
    	{
    		e.printStackTrace();
    		isMicrophoneOpen = false;
    	}
    }
    
    class CaptureAudio implements Runnable {
    	
		public void run() {
			while(isStarted) {
				byte[] buffer = new byte[targetDataLine.getBufferSize() / 5];       
				int count = targetDataLine.read(buffer, 0, buffer.length);
				if(count > 0) {
					sendData(buffer);
				}
			}			
		}  	
    }
}
