/**
* Copyright 2016 IBM Corp. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/

package com.ibm.watson.self.agents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.watson.self.constants.SelfConfigurationConstants;
import com.ibm.watson.self.topics.TopicClient;

public class TestAgent {

	private static String host = null;
	private static String port = null;
	
	private static Logger logger = LogManager.getLogger(TestAgent.class.getName());
	
	public static void main(String[] args) {
		
		if(args.length == 2) {
			host = args[0];
			port = args[1];
		}
		new TestAgent();
			
	}
	
	private boolean connectToIntu() {
		TopicClient client = TopicClient.getInstance();
		client.setHeaders(SelfConfigurationConstants.SELF_ID, 
				SelfConfigurationConstants.TOKEN);
		if(host != null) {
			client.connect(host, port);
		}
		else {
			client.connect(SelfConfigurationConstants.HOST, 
					SelfConfigurationConstants.PORT);
		}
		return true;
	}
	
	public TestAgent() {
		TopicClient client = TopicClient.getInstance();
		client.setHeaders(SelfConfigurationConstants.SELF_ID, 
				SelfConfigurationConstants.TOKEN);
		client.connect(SelfConfigurationConstants.HOST, 
				SelfConfigurationConstants.PORT);
		while(!client.isConnected()) {
			try {
				System.out.println("Client not connected yet");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		GameAgent agent = new GameAgent();
		AgentSociety.getInstance().addAgent(agent, false);
		
		int i = 0;
		while(i < 60) {
			try {
				Thread.sleep(1000);
				i++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		AgentSociety.getInstance().removeAgent(agent);
	}
}
