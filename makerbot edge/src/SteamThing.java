import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;

//import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinitions;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.metadata.collections.FieldDefinitionCollection;
import com.thingworx.relationships.RelationshipTypes.ThingworxEntityTypes;
import com.thingworx.types.BaseTypes;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.constants.CommonPropertyNames;
import com.thingworx.types.primitives.StringPrimitive;

//Refer to the "Steam Sensor Example" section of the documentation
//for a detailed explanation of this example's operation 

// Property Definitions
@SuppressWarnings("serial")
@ThingworxPropertyDefinitions(properties = {
		@ThingworxPropertyDefinition(name="Temperature", description="Current Temperature", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="Status", description="Current operation", baseType="STRING", category="Status", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="FaultStatus", description="Fault status", baseType="BOOLEAN", category="Faults", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="InletValve", description="Inlet valve state", baseType="BOOLEAN", category="Status", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="TemperatureLimit", description="Temperature fault limit", baseType="NUMBER", category="Faults", aspects={"isReadOnly:false"}),
		@ThingworxPropertyDefinition(name="TotalFlow", description="Total flow", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
	})

// Event Definitions
@ThingworxEventDefinitions(events = {
	@ThingworxEventDefinition(name="SteamSensorFault", description="Steam sensor fault", dataShape="SteamSensor.Fault", category="Faults", isInvocable=true, isPropertyEvent=false)
})

// Steam Thing virtual thing class that simulates a Steam Sensor
public class SteamThing extends VirtualThing implements Runnable {
	
	private double _totalFlow = 0.0;
	private Thread _shutdownThread = null;
	ValueCollection parameters;
	public SteamThing(String name, String description, String identifier, ConnectedThingClient client) {
		super(name,description,identifier,client);
		
		// Data Shape definition that is used by the steam sensor fault event
		// The event only has one field, the message
		FieldDefinitionCollection faultFields = new FieldDefinitionCollection();
		faultFields.addFieldDefinition(new FieldDefinition(CommonPropertyNames.PROP_MESSAGE,BaseTypes.STRING));
		defineDataShapeDefinition("SteamSensor.Fault", faultFields);

		// Populate the thing shape with the properties, services, and events that are annotated in this code
		super.initializeFromAnnotations();
		
		parameters = new ValueCollection(); 
		parameters.put("sourceFile", new StringPrimitive("pic1.png"));
		parameters.put("sourcePath", new StringPrimitive("C:\\junk\\makerbot\\edge server\\"));
		parameters.put("sourceRepo", new StringPrimitive(client.getGatewayName()));
		parameters.put("targeFile", new StringPrimitive("pic1.png"));
		parameters.put("targetPath", new StringPrimitive("\\Thingworx\\MediaEntities"));
		parameters.put("targetRepo", new StringPrimitive("SystemRepository"));
	}

	// From the VirtualThing class
	// This method will get called when a connect or reconnect happens
	// Need to send the values when this happens
	// This is more important for a solution that does not send its properties on a regular basis
	public void synchronizeState() {
		// Be sure to call the base class
		super.synchronizeState();
		// Send the property values to Thingworx when a synchronization is required
		super.syncProperties();
	}
	
	// The processScanRequest is called by the SteamSensorClient every scan cycle
	@Override
	public void processScanRequest() throws Exception {
		// Be sure to call the base classes scan request
		super.processScanRequest();
		// Execute the code for this simulation every scan
		this.scanDevice();
	}
	
	// Performs the logic for the steam sensor, occurs every scan cycle
	public void scanDevice() throws Exception {
		//Path path = Paths.get("pic1.png");
	    //byte[] pic = Files.readAllBytes(path);
	    
		//getClient().invokeService(ThingworxEntityTypes.Subsystems, "FileTransferSubsystem", "Copy", parameters, 60000);
		
		
		Path path = Paths.get("temp.txt");
		List<String> s = Files.readAllLines(path, StandardCharsets.UTF_8);
	    
		// Set the Temperature property value in the range of 400-440
		double temperature = Double.valueOf(s.get(0));
		String status = s.get(1);
		//System.out.println("status "+status);
		// Set the Pressure property value in the range of 18-23 
		double pressure = 18 + 5 * Math.random();
		// Add a random double value from 0.0-1.0 to the total flow
		this._totalFlow += Math.random();
				
		// Set the InletValve property value to true by default
		boolean inletValveStatus = true;
		
		// If the current second value is divisible by 15, set the InletValve property value to false
		int seconds = DateTime.now().getSecondOfMinute();
		if((seconds % 15) == 0)
			inletValveStatus = false;
		
		// Set the property values
		super.setProperty("Temperature", temperature);
		super.setProperty("Status", new StringPrimitive(status));
		super.setProperty("TotalFlow", _totalFlow);
		super.setProperty("InletValve", inletValveStatus);
		
		// Get the TemperatureLimmit property value from memory
		double temperatureLimit = (Double)getProperty("TemperatureLimit").getValue().getValue();
				
		// Set the FaultStatus property value if the TemperatureLimit value is exceeded
		// and it is greater than zero
		boolean faultStatus = false;
		if(temperatureLimit > 0 && temperature > temperatureLimit)
			faultStatus = true;
		
		// If the sensor has a fault...
		if(faultStatus) {
			// Get the previous value of the fault from the property
			// This is the current value because it hasn't been set yet
			// This is done because we don't want to send the event every time it enters the fault state, 
			// only send the fault on the transition from non-faulted to faulted
			boolean previousFaultStatus = (Boolean)getProperty("FaultStatus").getValue().getValue();
			
			// If the current value is not faulted, then create and queue the event
			if(!previousFaultStatus) {
				// Set the event information of the defined data shape for the event
				ValueCollection eventInfo = new ValueCollection();
				eventInfo.put(CommonPropertyNames.PROP_MESSAGE, new StringPrimitive("Temperature at " + temperature + " was above limit of " + temperatureLimit));
				// Queue the event
				super.queueEvent("SteamSensorFault", DateTime.now(), eventInfo);
			}
		}
		
		// Set the fault status property value
		super.setProperty("FaultStatus", faultStatus);

		// Update the subscribed properties and events to send any updates to Thingworx
		// Without calling these methods, the property and event updates will not be sent
		// The numbers are timeouts in milliseconds.
		super.updateSubscribedProperties(15000);
		super.updateSubscribedEvents(60000);
	}
	
	@ThingworxServiceDefinition( name="AddNumbers", description="Add Two Numbers")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="NUMBER" )
	public Double AddNumbers( 
			@ThingworxServiceParameter( name="a", description="Value 1", baseType="NUMBER" ) Double a,
			@ThingworxServiceParameter( name="b", description="Value 2", baseType="NUMBER" ) Double b) throws Exception {
		
		return a + b;
	}

	@ThingworxServiceDefinition( name="GetBigString", description="Get big string")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="STRING" )
	public String GetBigString() {
		StringBuilder sbValue = new StringBuilder();
		
		for(int i=0;i<24000;i++) {
			sbValue.append('0');
		}
		
		return sbValue.toString();
	}

	@ThingworxServiceDefinition( name="Shutdown", description="Shutdown the client")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="", baseType="NOTHING")
	public synchronized void Shutdown() throws Exception {
		// Should not have to do this, but guard against this method being called more than once.
		if(this._shutdownThread == null) {
			// Create a thread for shutting down and start the thread
			this._shutdownThread = new Thread(this);
			this._shutdownThread.start();
		}
	}

	@Override
	public void run() {
		try {
			// Delay for a period to verify that the Shutdown service will return
			Thread.sleep(1000);
			// Shutdown the client
			this.getClient().shutdown();
		} catch (Exception x) {
			// Not much can be done if there is an exception here
			// In the case of production code should at least log the error
		}
	}
}
