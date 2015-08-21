package is.erle.comms;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpoint;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointListener;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointService;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpoint;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpointListener;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpointService;
import interactivespaces.service.comm.network.server.UdpServerRequest;

/**
 * This is the communication layer of the project. It will receive data to send
 * from mavlink activity and send it to the drone. Also it will receive data
 * from the drone and it will publish it. Mavlink will process this data and put
 * it on several relevant topics to be used by other activities.
 * <p>
 * This activity takes care of the communication with the drone via a UDP
 * port. This receives/transmits messages from/to mavlink activity and then is
 * sent to the drone in the form of bytes.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 */
public class IsErleCommsActivity extends BaseRoutableRosActivity {

	/**
	 * The name of the config property for obtaining the UDP server host.
	 */
	private static final String CONFIGURATION_SERVER_PORT = "space.comm.udp.server.port";
	
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
	@SuppressWarnings("unused")
	private static long jsonOutputCounter = 0;
	
	/**
	 * A counter to count the the number of calls onNewInputJson calls.
	 */
	@SuppressWarnings("unused")
	private static long jsonInputCounter = 0 ;
	
	/**
	 * Stores the address of the drone to send data to.
	 */
	@SuppressWarnings("unused")
	private InetSocketAddress udpDroneAddress;
	
	/**
	 * A flag to check whether any drone has sent data or not.
	 */
	private static boolean droneAddressFlag;
	
	/**
	 * A flag to check when to send data to the drone.
	 */
	private static boolean sendFlag;
	
	/**
	 * An instance of UdpServer to receive and send messages to the drone.
	 */
	private UdpServerNetworkCommunicationEndpoint udpDroneServer ;
	
	/**
	 * Stores the data received from the mavlink activity as a FIFO queue and
	 * sent to the drone as response after the udp request.
	 */
	private Queue<byte []> responseGlobal;
	
	/**
	 * An Udp Clinet instance.
	 */
	private UdpClientNetworkCommunicationEndpoint udpClient;
	
	/**
	 * A date instance to check and implement timeout for server response and switch to udpClient.
	 */
	@SuppressWarnings("unused")
	private Date start;
	 
	/**
	 * Executes on activity setup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivitySetup()
	 * @since 1.0.0
	 */
    @Override
	public void onActivitySetup()
	{
		getLog().info("Activity is.erle.comms setup");
        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
		droneAddressFlag = false;
		sendFlag = false;
		responseGlobal = new ArrayBlockingQueue<byte[]>(20);
		start = new Date();
		UdpServerNetworkCommunicationEndpointService udpServerService = getSpaceEnvironment()
				.getServiceRegistry()
				.getRequiredService(
						UdpServerNetworkCommunicationEndpointService.SERVICE_NAME);
		final int port = getConfiguration().getRequiredPropertyInteger(
				CONFIGURATION_SERVER_PORT);
		udpDroneServer = udpServerService.newServer(port, getLog());
		udpDroneServer
				.addListener(new UdpServerNetworkCommunicationEndpointListener()
				{

					public void onUdpRequest(
							UdpServerNetworkCommunicationEndpoint server,
							UdpServerRequest req)
					{
						handleUdpDroneServerResponse(req.getRequest(), server);
						// getLog().info(req.getRemoteAddress()
						// +Arrays.toString(req.getRequest()));
						// req.writeResponse("Server recieved your message and is replying".getBytes());
						// getLog().info(udpDroneAddress+"  " +
						// droneAddressFlag);
						if (!droneAddressFlag)
						{
							// udpDroneAddress = req.getRemoteAddress();
							udpDroneAddress = new InetSocketAddress(req
									.getRemoteAddress().getHostString(), port);
							//req.writeResponse("sh /etc/init.d/rc.usb"
							//		.getBytes());
							droneAddressFlag = true;
						}
						if (sendFlag)
						{
							//req.writeResponse(responseGlobal);
							synchronized (this)
							{
								byte [] temp = responseGlobal.poll();
								while (temp!=null)
								{
									req.writeResponse(temp);
									temp=responseGlobal.poll();
								}
								sendFlag = false;
							}
						}
					}
				});
		addManagedResource(udpDroneServer);
		
		UdpClientNetworkCommunicationEndpointService udpClientService = getSpaceEnvironment().getServiceRegistry().getRequiredService(UdpClientNetworkCommunicationEndpointService.SERVICE_NAME);
        udpClient = udpClientService.newClient(getLog());
        udpClient.addListener(new UdpClientNetworkCommunicationEndpointListener() {
			
			public void onUdpResponse(UdpClientNetworkCommunicationEndpoint client,
					byte[] data, InetSocketAddress address) {
				handleUdpDroneClientResponse(data, address);
				getLog().info("In client");
			}
		});
        addManagedResource(udpClient);
	}

	/**
	 * Executes on activity startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityStartup()
	 * @since 1.0.0
	 */
	@Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.comms startup");
    }

	/**
	 * Executes on activity post startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPostStartup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.comms post startup");
    }

	/**
	 * Executes on activity activate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityActivate()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.comms activate");
        jsonOutputCounter = 0;
        udpDroneAddress = new InetSocketAddress("192.168.7.2", 6000);
//        Map<String,Object> temp=Maps.newHashMap();
//        temp.put(Long.toString(jsonOutputCounter++), "ACTIVATE");
//        sendOutputJson("output", temp);
        
    }

	/**
	 * Executes on activity deactivate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityDeactivate()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.comms deactivate");
//        Map<String,Object> temp=Maps.newHashMap();
//        temp.put(Long.toString(jsonOutputCounter), "DEACTIVATE");
//        sendOutputJson("output", temp);
    }

	/**
	 * Executes on activity pre shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPreShutdown()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.comms pre shutdown");
    }

	/**
	 * Executes on activity shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityShutdown()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.comms shutdown");
    }

	/**
	 * Executes on activity cleanup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityCleanup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.comms cleanup");
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
		if (channelName.equals(subscribers[0]))
		{
			getLog().debug("Sending to drone");
			getLog().debug(message.get("comm").toString());
			byte[] response;
			String items[] = message.get("comm").toString()
					.replaceAll("\\[", "").replaceAll("\\]", "")
					.replaceAll(" ", "").split(",");
			int lenItems = items.length;
			response = new byte[lenItems];
			for (int i = 0; i < lenItems; i++)
			{
				try
				{
					response[i] = Byte.parseByte(items[i]);
				}
				catch (NumberFormatException e)
				{
					getLog().error(e);
				}

			}
			if (droneAddressFlag)
			{
				/*
				 * if ((System.currentTimeMillis()-start.getTime()) <1000) {
				 * udpClient.write(udpDroneAddress, response); } else {
				 */
				synchronized (this)
				{
					responseGlobal.add(response);
					sendFlag = true;

				}
				// }

			}
			else
			{
				getLog().info("No Drones connected now");
			}
			jsonInputCounter++; // Take care of this variable
		}
	}

	/**
	 * Handle the UDP response that has come in.
	 * 
	 * @param response	Response from the drone.
	 * @param address	Remote address of the drone.
	 * @see				java.net.InetSocketAddress
	 * @since			1.0.0
	 */
    protected void handleUdpDroneClientResponse(byte[] response,
			InetSocketAddress address) {
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", Arrays.toString(response));
        sendOutputJson(publishers[0], temp);
		
	}
    
    /**
     * Handle the UDP request that has come in.
     * 
     * @param response 	Response from the drone.
     * @param address	Address of the server.
     * @see				interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpoint
	 * @since			1.0.0
     */
    protected void handleUdpDroneServerResponse(byte[] response,
    		UdpServerNetworkCommunicationEndpoint address) {
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", Arrays.toString(response));
        sendOutputJson(publishers[0], temp);
		
	}
}
