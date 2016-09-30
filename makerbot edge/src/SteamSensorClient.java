import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.communications.common.SecurityClaims;
import com.thingworx.relationships.RelationshipTypes.ThingworxEntityTypes;
import com.thingworx.types.collections.ValueCollection;

import java.net.Socket;
import java.util.HashMap;

import org.python.core.PyCode;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

//Refer to the "Steam Sensor Example" section of the documentation
//for a detailed explanation of this example's operation 
public class SteamSensorClient extends ConnectedThingClient {
	public SteamSensorClient(ClientConfigurator config) throws Exception {
		super(config);
	}
	
	// Test example
	public static void main(String[] args) throws Exception {
		if(args.length < 3) {
			System.out.println("Required arguments not found!");
			System.out.println("URI AppKey ScanRate <StartSensor> <Number Of Sensors>");
			System.out.println("Example:");
			System.out.println("ws://localhost:80/Thingworx/WS xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx 1000 1 10");
			return;
		}
		 
		// Set the required configuration information
		ClientConfigurator config = new ClientConfigurator();
		// The uri for connecting to Thingworx
		config.setUri(args[0]);
		// Reconnect every 15 seconds if a disconnect occurs or if initial connection cannot be made
		config.setReconnectInterval(15);
		
		// Set the security using an Application Key
		String appKey = args[1];
		SecurityClaims claims = SecurityClaims.fromAppKey(appKey);
		config.setSecurityClaims(claims);
		
		// Set the name of the client
		config.setName("SteamSensorGateway");
		// This client is a SDK
		config.setAsSDKType();

		// Get the scan rate (milliseconds) that is specific to this example
		// The example will execute the processScanRequest of the VirtualThing
		// based on this scan rate
		int scanRate = Integer.parseInt(args[2]);

		int startSensor = 0;
		int nSensors = 2;
		
		if(args.length == 5) {
			startSensor = Integer.parseInt(args[3]);
			nSensors = Integer.parseInt(args[4]);
		}
		
		// Create the client passing in the configuration from above
		SteamSensorClient client = new SteamSensorClient(config);

		for(int sensor=1;sensor <= nSensors; sensor++) {
			int sensorID = startSensor + sensor;
			SteamThing steamSensorThing = new SteamThing("SteamSensor" + sensorID,"Steam Sensor #" + sensorID,"MSN000" + sensorID,client);
			client.bindThing(steamSensorThing);
		}
		
	    HashMap<String, String> virtualDirs = new HashMap<String, String>();
		String userDir = "c:\\junk\\marius\\makerbot\\edge server";//System.getProperty("user.dir");
		               
		virtualDirs.put("logs", userDir );
		virtualDirs.put("incoming", userDir);
		 
		FileTransfer fthing = new FileTransfer("MyFileTransferThing", "Makerbot File Transfer", client, virtualDirs);

		client.bindThing(fthing);
		
		try {
			// Start the client
			client.start();
		}
		catch(Exception eStart) {
			System.out.println("Initial Start Failed : " + eStart.getMessage());
		}
		
		// As long as the client has not been shutdown, continue
		while(!client.isShutdown()) {
			// Only process the Virtual Things if the client is connected
			if(client.isConnected()) {
				// Loop over all the Virtual Things and process them
				for(VirtualThing thing : client.getThings().values()) {
					try {
						thing.processScanRequest();
					}
					catch(Exception eProcessing) {
						System.out.println("Error Processing Scan Request for [" + thing.getName() + "] : " + eProcessing.getMessage());
					}
				}
			}
			// Suspend processing at the scan rate interval
			Thread.sleep(scanRate);
		}
	}
}
