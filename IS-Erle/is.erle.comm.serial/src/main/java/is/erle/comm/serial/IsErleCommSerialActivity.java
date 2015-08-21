package is.erle.comm.serial;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.service.comm.serial.SerialCommunicationEndpoint;
import interactivespaces.service.comm.serial.SerialCommunicationEndpointService;
import interactivespaces.util.concurrency.CancellableLoop;
import interactivespaces.util.concurrency.ManagedCommand;
import interactivespaces.util.resource.ManagedResourceWithTask;

/**
 * This activity takes care of the communication with the drone via a serial
 * port. This receives/transmits messages from/to mavlink activity and then is
 * sent to the drone in the form of bytes.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class IsErleCommSerialActivity extends BaseRoutableRosActivity {

	/**
	 * The name of the config property for obtaining the publisher List.
	 */
	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	
	/**
	 * The name of the config property for obtaining the subscriber List.
	 */
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	
	/**
	 * The topic names for publishing data.
	 * <p>
	 * PUBLISHER MAPPING
	 * <p>
	 * publishers[0] -> output 
	 * <p>
	 * Topic Name : comms/output
	 * <p>
	 * Usage : Send output to the mavlink activity after receiving it from the drone.
	 * <p>
	 */
	private static String publishers[];

	/**
	 * The topic names for subscribing data.
	 * <p>
	 * SUBSCRIBER MAPPING
	 * <p>
	 * subscribers[0] -> input 
	 * <p>
	 * Topic Name : comms/input
	 * <p>
	 * Usage : Receive data from mavlink activity and send it to the drone.
	 * <p>
	 */
	private static String subscribers[];
	
	/**
	 * A counter to count the the number of calls sendOutputJson calls.
	 */
	private static long jsonOutputCounter = 0;
	
	/**
	 * A counter to count the the number of calls onNewInputJson calls.
	 */
	private static long jsonInputCounter = 0 ;

	/**
	 * A serial object to handle all the serial communication with the drone.
	 */
	private static SerialCommunicationEndpoint serial;
	
	/**
	 * A global object to store the received serial data
	 */
	private static byte[] serialData;

	/**
	 * Executes on activity setup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivitySetup()
	 * @since 1.0.0
	 */
	@Override
	public void onActivitySetup() {
		getLog().info("Activity is.erle.comm.serial setup");

        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        //getLog().info(publishers[0]+ "  " + subscribers[0]);
		SerialCommunicationEndpointService serialService = getSpaceEnvironment()
				.getServiceRegistry().getRequiredService(
						SerialCommunicationEndpointService.SERVICE_NAME);
		String portName = getConfiguration().getRequiredPropertyString(
				"space.hardware.serial.port");
		serial = serialService.newSerialEndpoint(portName);
		serial.setBaud(115200);
		serial.setInputBufferSize(10000);
		serial.setOutputBufferSize(1000);
		serialData = new byte[600];
		//serial.startup();

		/*ManagedCommand threadSender = getManagedCommands().submit(new Runnable() {
						public void run() {
							while (!Thread.interrupted())
						{
								while (serial.available() >0) 
								{
									int tempInt = serial.read(serialData);
									serialData = ArrayUtils.subarray(serialData, 0, tempInt);
									Map<String, Object> temp = Maps.newHashMap();
									temp.put("comm", Arrays.toString(serialData));
									sendOutputJson(publishers[0], temp);
								}
								
							}
						}
					});*/
		
		ManagedResourceWithTask serialTask = new ManagedResourceWithTask(serial, new CancellableLoop()
		{
			
			@Override
			protected void loop() throws InterruptedException
			{
				handleSerialInput();
				
			}
			
			protected void handleException(Exception e)
			{
				getLog().error(e);
			}
		}, getSpaceEnvironment());
		addManagedResource(serialTask);
	}

	/**
	 * Executes on activity startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityStartup()
	 * @since 1.0.0
	 */
	@Override
	public void onActivityStartup() {
		getLog().info("Activity is.erle.comm.serial startup");
	}

	/**
	 * Executes on activity post startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPostStartup()
	 * @since 1.0.0
	 */
	@Override
	public void onActivityPostStartup() {
		getLog().info("Activity is.erle.comm.serial post startup");
	}

	/**
	 * Executes on activity activate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityActivate()
	 * @since 1.0.0
	 */
	@Override
	public void onActivityActivate() {
		getLog().info("Activity is.erle.comm.serial activate");
		jsonOutputCounter = 0;
		/*
		 * Map<String,Object> temp=Maps.newHashMap();
		 * temp.put(Long.toString(jsonOutputCounter++), "ACTIVATE");
		 * sendOutputJson("output", temp);
		 */
	}

	/**
	 * Executes on activity deactivate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityDeactivate()
	 * @since 1.0.0
	 */
	@Override
	public void onActivityDeactivate() {
		getLog().info("Activity is.erle.comm.serial deactivate");
		
//		 Map<String, Object> temp = Maps.newHashMap();
//		 temp.put(Long.toString(jsonOutputCounter), "DEACTIVATE");
//		 sendOutputJson("output", temp);
		 
	}

	/**
	 * Executes on activity pre shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPreShutdown()
	 * @since 1.0.0
	 */
	@Override
	public void onActivityPreShutdown() {
		getLog().info("Activity is.erle.comm.serial pre shutdown");
		//serial.shutdown();
	}

	/**
	 * Executes on activity shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityShutdown()
	 * @since 1.0.0
	 */
	@Override
	public void onActivityShutdown() {
		getLog().info("Activity is.erle.comm.serial shutdown");
	}

	/**
	 * Executes on activity cleanup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityCleanup()
	 * @since 1.0.0
	 */
	@Override
	public void onActivityCleanup() {
		getLog().info("Activity is.erle.comm.serial cleanup");
	}

	/**
	 * Callback for new message on the subscribed topics.
	 * Processes incoming messages.
	 * 
	 * @param channelName 	Channel name of incoming message
	 * @param message 		Message stored in a key-value pair in a map
	 * @see 				interactivespaces.activity.impl.ros.BaseRoutableRosActivity
	 * @see					java.util.Map
	 * @since				1.0.0
	 */
	@Override
	public void onNewInputJson(String channelName, Map<String, Object> message)
	{
		if (subscribers[0].equals(channelName))
		{
			byte[] responseGlobal;
			String items[] = message.get("comm").toString()
					.replaceAll("\\[", "").replaceAll("\\]", "")
					.replaceAll(" ", "").split(",");
			int lenItems = items.length;
			responseGlobal = new byte[lenItems];
			for (int i = 0; i < lenItems; i++)
			{
				try
				{
					responseGlobal[i] = Byte.parseByte(items[i]);
				}
				catch (NumberFormatException e)
				{
					getLog().error(e);
				}

			}
			getLog().debug(Arrays.toString(responseGlobal));
			serial.write(responseGlobal);
			jsonInputCounter++; // Take care of this variable
		}
	}
	
	/**
	 * Callback for serial listener.
	 */
	private void handleSerialInput()
	{
		int tempInt = serial.read(serialData);
		serialData = ArrayUtils.subarray(serialData, 0, tempInt);
		Map<String, Object> temp = Maps.newHashMap();
		temp.put("comm", Arrays.toString(serialData));
		sendOutputJson(publishers[0], temp);
	}
	
}
