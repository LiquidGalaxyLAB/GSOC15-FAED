package is.erle.mavlink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.concurrency.ManagedCommand;
import com.MAVLink.*;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.*;
import com.MAVLink.enums.*;
import com.MAVLink.pixhawk.*;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Class;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;


/**
 * This class performs all the tasks of processing message from the drone and
 * translating it so that it is understood by other systems. It also receives
 * commands from other activities, translates them into drone understandable
 * bytes and then send it to the communication activity to be sent to the drone.
 * <p>
 * This activity subscribes to 3 channels, one from waypoint processor activity,
 * one from captain activity and one from comms activity. It will take the
 * waypoint payload data and make it Mavlink(Drone) understandable. It will then
 * publish this data on comms. It will also be subscribing comms, it will parse
 * all the data into releavant fields and then publish it on relevant topics.
 * The incoming data will have imu, compass, gps, battery, barometer etc data,
 * it will all get separated and published on individual topics. It will receive
 * commands from the captain activity and then process it and send it to the
 * drone.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 */
public class IsErleMavlinkActivity extends BaseRoutableRosActivity {

	/*
	 * Note - In communication between this activity and any other comms activity, the
	 * message type being used is byte []
	 */
	
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
	 * publishers[0] -> outputCOM_M 
	 * <p>
	 * Topic Name : comms/input
	 * <p>
	 * Usage : Send output to the comms activity
	 * <p>
	 * publishers[1] -> outputWP_M
	 * <p>
	 * Topic Name : waypoint/input
	 * <p>
	 * Usage : Send output to the waypoint generator activity
	 * <p>
	 * publishers[2] -> outputGeneral_M
	 * <p>
	 * Topic Name : mavlink/output
	 * <p>
	 * Usage : A General output topic having all the logs of mavlink activity
	 * <p>
	 * publishers[3] -> captain
	 * <p>
	 * Topic Name : captain/input
	 * <p>
	 * Usage : Send output to the captain activity
	 * <p>
	 * publishers[4] -> heartbeat
	 * <p>
	 * Topic Name : mavlink/heartbeat
	 * <p>
	 * Usage : An output topic having all the heartbeat messages
	 * <p>
	 * publishers[5] -> hud
	 * <p>
	 * Topic Name : mavlink/hud
	 * <p>
	 * Usage : An output topic having all the HUD (Head Up Display) messages
	 * <p>
	 * publishers[6] -> attitude
	 * <p>
	 * Topic Name : mavlink/attitude
	 * <p>
	 * Usage : An output topic having all the attitude messages
	 * <p>
	 * publishers[7] -> status
	 * <p>
	 * Topic Name : mavlink/system/status
	 * <p>
	 * Usage : An output topic having all the system status messages
	 * <p>
	 * publishers[8] -> time
	 * <p>
	 * Topic Name : mavlink/system/time
	 * <p>
	 * Usage : An output topic having all the system time messages
	 * <p>
	 * publishers[9] -> gps
	 * <p>
	 * Topic Name : mavlink/sensors/gps
	 * <p>
	 * Usage : An output topic having all the GPS sensor  messages
	 * <p>
	 * publishers[10] -> imu
	 * <p>
	 * Topic Name : mavlink/sensors/imu
	 * <p>
	 * Usage : An output topic having all the IMU sensor  messages
	 * <p>
	 * publishers[11] -> scaled_pressure
	 * <p>
	 * Topic Name : mavlink/sensors/pressure
	 * <p>
	 * Usage : An output topic having all the scaled Pressure sensor  messages
	 * <p>
	 * publishers[12] -> global_position
	 * <p>
	 * Topic Name : mavlink/position/global
	 * <p>
	 * Usage : An output topic having all the Global Position  messages
	 * <p>
	 * publishers[13] -> local_position
	 * <p>
	 * Topic Name : mavlink/position/local
	 * <p>
	 * Usage : An output topic having all the Local Position  messages
	 * <p>
	 * publishers[14] -> servo_output
	 * <p>
	 * Topic Name : mavlink/servoOutput
	 * <p>
	 * Usage : An output topic having all the servo/BLDC motor output  messages
	 * <p>
	 * publishers[15] -> rc_input
	 * <p>
	 * Topic Name : mavlink/rcInput
	 * <p>
	 * Usage : An output topic having all the RC transmitter input messages
	 * <p>
	 * publishers[16] -> current_mission_seq
	 * <p>
	 * Topic Name : mavlink/current_mission_seq
	 * <p>
	 * Usage : An output topic having all the Current Mission Sequence number messages
	 * <p>
	 * publishers[17] -> nav_controller_output
	 * <p>
	 * Topic Name : mavlink/controller/nav
	 * <p>
	 * Usage : An output topic having all the Current Mission Sequence number messages
	 * <p>
	 * publishers[18] -> terrain_report
	 * <p>
	 * Topic Name : mavlink/terrainReport
	 * <p>
	 * Usage : An output topic having all the Terrain Report messages
	 * <p>
	 */
	private static String publishers[];

	/**
	 * The topic names for subscribing data.
	 * <p>
	 * SUBSCRIBER MAPPING
	 * <p>
	 * subscribers[0] -> inputCOM_M 
	 * <p>
	 * Topic Name : comms/output
	 * <p>
	 * Usage : Receive data from comms activity ie from drone
	 * <p>
	 * subscribers[1] -> inputWP_M 
	 * <p>
	 * Topic Name : waypoint/output
	 * <p>
	 * Usage : Receive data from waypoint generator activity about the waypoints
	 * <p>
	 * subscribers[2] -> captain
	 * <p>
	 * Topic Name : captain/output
	 * <p>
	 * Usage : Receive command from the captain activity
	 * <p>
	 * subscribers[3] -> rc_output
	 * <p>
	 * Topic Name : captain/rc_output
	 * <p>
	 * Usage : Receive RC output from the captain activity
	 */
	private static String subscribers[];
	
    /**
    * A message to pack and unpack all messages to/from payload
    * <p>
    * Common interface for all MAVLink Messages
    * <p>
    * Packet Anatomy
    * <p>
    * This is the anatomy of one packet. It is inspired by the CAN and SAE AS-4 standards.
    * <p>
    * Byte Index  Content              Value       Explanation
    * <p>
    * 0            Packet start sign  v1.0: 0xFE   Indicates the start of a new packet.  (v0.9: 0x55)
    * <p>
    * 1            Payload length      0 - 255     Indicates length of the following payload.
    * <p>
    * 2            Packet sequence     0 - 255     Each component counts up his send sequence. Allows to detect packet loss
    * <p>
    * 3            System ID           1 - 255     ID of the SENDING system. Allows to differentiate different MAVs on the same network.
    * <p>
    * 4            Component ID        0 - 255     ID of the SENDING component. Allows to differentiate different components of the same system, e.g. the IMU and the autopilot.
    * <p>
    * 5            Message ID          0 - 255     ID of the message - the id defines what the payload means and how it should be correctly decoded.
    * <p>
    * 6 to (n+6)   Payload             0 - 255     Data of the message, depends on the message id.
    * <p>
    * (n+7)to(n+8) Checksum (low byte, high byte)  ITU X.25/SAE AS-4 hash, excluding packet start sign, so bytes 1..(n+6) Note: The checksum also includes MAVLINK_CRC_EXTRA (Number computed from message fields. Protects the packet from decoding a different version of the same packet but with different variables).
    * <p>
    * The checksum is the same as used in ITU X.25 and SAE AS-4 standards (CRC-16-CCITT), documented in SAE AS5669A. Please see the MAVLink source code for a documented C-implementation of it. LINK TO CHECKSUM
    * <p>
    * The minimum packet length is 8 bytes for acknowledgement packets without payload
    * <p>
    * The maximum packet length is 263 bytes for full payload
    */
	private MAVLinkPacket mavPacket;
	
	/**
	 * This is an instance of Parser class which has a convenience function
	 * called mavlink_parse_char which handles the complete MAVLink parsing.
	 * This function will parse one byte at a time and return the complete
	 * packet once it could be successfully decoded. Checksum and other failures
	 * will be silently ignored.
	 */
	private Parser mavParser;
	
	/**
	 * This is an instance of MAVLinkMessage which wraps up all the message
	 * classes sent by the drone.
	 */
	private MAVLinkMessage mavMessage;
	
	/**
	 * This is the default id of the target system. It essentially is the first
	 * id which the mavlink activity sees
	 */
	private static byte targetSystem ; // TO DO : Get this from the current drone
	
	/**
	 * This is the default id of the target component. It essentially is the
	 * heartbeat component id which is 0. This can be used to send data globally
	 * to a target system
	 */
	private static byte targetComponent; // TO DO : Get this from the current drone
	
	/**
	 * A flag to show if any heartbeat message has been received. This signifies
	 * that atleast one drone is connected and data can be sent to it.
	 */
	private static boolean heartbeatReceiveFlag;
	
	/**
	 * The response from comms activity stored in this global variable
	 */
	private int responseGlobal[];
	
	/**
	 * A latest heartbeat message.
	 */
	private msg_heartbeat heartbeat;
	
	/**  
	 * A List of String arrays containing waypoint data read from the drone
	 * <p>
	 * DATA MAPPING FOR STRING ARRAY
	 * <p>
	 * Index 0 -> INDEX
	 * <p>
	 * Index 1 -> CURRENT WP
	 * <p>
	 * Index 2 -> COORD FRAME
	 * <p>
	 * Index 3 -> COMMAND
	 * <p>
	 * Index 4 -> PARAM1
	 * <p>
	 * Index 5 -> PARAM2
	 * <p>
	 * Index 6 -> PARAM3
	 * <p>
	 * Index 7 -> PARAM4
	 * <p>
	 * Index 8 -> PARAM5/X/LONGITUDE
	 * <p>
	 * Index 9 -> PARAM6/Y/LATITUDE
	 * <p>
	 * Index 10 -> PARAM7/Z/ALTITUDE
	 * <p>
	 * Index 11 -> AUTOCONTINUE
	 * <p>
	 */
	private List<String []> readWaypointList;
	
	/**
	 * A waypoint count message to store a count message received after the
	 * waypoint request list. Used to indicate termination of a waypoint request
	 * list message and also receive these many mission item messages.
	 */
	private short readWaypointCount = -1;
	
	/**
	 * The current sequence number of mission item being requested/sent. Used
	 * for termination/retry of sending all the mission items.
	 */
	private short missionCurrentSeq;
	
	/**
	 * A flag to tell if the mission clear was successful.
	 */
	private boolean isMissionCleared;
	
	/**
	 * A flag to tell the successful receipt of send mission count message. Set
	 * to true after the first receipt of a mission request message
	 */
	private boolean missionRequestFlag;
	
	/**
	 * Stores the number of mission items to be sent. It is received from the
	 * waypoint generator activity.
	 */
	private short sendMissionCount=-1;
	
	/**
	 * A temporary variable to store the current target system.
	 */
	private byte tempTSystem;
	
	/**
	 * A temporary variable to store the current target component.
	 */
	private byte tempTComponent;
	
	/**
	 * Mission Acknowledgment stored here. Used for retries/terminating the
	 * sendMissionListStart message.
	 */
	private byte sendMissionAck = (byte) -1;
	
	/**
	 * Stores the command receive acknowledgment. Used for terminating/retrying do Command messages.
	 */
	private boolean isCommandSent;
	
	/**
	 * A HashMap to store the type of parameter. The parameter type is paired
	 * with a string id.
	 */
	private Map<String,Byte > paramType;
	
	/**
	 * A HashMap to store the value of the parameter with a certain string id. 
	 */
	private Map<String , Double> paramList;
	
	/**
	 * Stores the current parameter index being received.
	 */
	private short paramIndex;
	
	/**
	 * Stores the total number of parameters on the drone.
	 */
	private short paramTotal;
	
	/**
	 * A flag to check whether readParameterList completed or not.
	 */
	private boolean receiveParamList;
	
	/**
	 * A flag to check whether readParameter completed or not.
	 */
	private boolean receiveParam;
	
	/**
	 * A min max pair of Point3D type to store the safety allowed area. The
	 * minimum value denotes the bottom south west corner and the maximum value
	 * is the point diagonally opposite to it. Thus, the safety allowed area is
	 * the volume inside the box.
	 */
	private MinMaxPair<Point3D> allowedArea=null;
	
	/**
	 * Frame of reference of the safety allowed area min max pair.
	 */
	@SuppressWarnings("unused")
	private byte allowedAreaFrame;
	
	/**
	 * Stores Global GPS Origin. Useful for terminating/retrying
	 * setGlobalGpsOrigin.
	 */
	private Point3D globalGpsOrigin;
	
	/**
	 * Stores the Parameter.xml file. This file contains the allowed commands
	 * and values of all the commands.
	 */
	private File inputFile;
	
	/**
	 * Stores an object of XMLParamParser class which helps in parsing and
	 * requesting limits of certain commands/messages.
	 */
	private XMLParamParser dataXML;
	
	/**
	 * Stores the log Entry found on the remote drone.
	 */
	private List<msg_log_entry> logEntry =Collections.synchronizedList( new ArrayList<msg_log_entry>());
	
	/**
	 * Executes on activity setup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivitySetup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.mavlink setup");
        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        responseGlobal = new int[1000];
        mavParser = new Parser();
        heartbeatReceiveFlag = false;
        String directory = getActivityFilesystem().getInstallDirectory().getAbsolutePath() +"/ParameterMetaDataBackup.xml";
        inputFile = new File(directory);
		try
		{
			dataXML = new XMLParamParser(inputFile);
		}
		catch (SAXException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
    }

	/**
	 * Executes on activity startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityStartup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.mavlink startup");
    }

	/**
	 * Executes on activity post startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPostStartup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.mavlink post startup");
    }

	/**
	 * Executes on activity activate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityActivate()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.mavlink activate");
        getDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_ALL, 1);
		// getDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_RAW_SENSORS, 1);
		// getDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_RAW_CONTROLLER, 1);
		// getDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_RC_CHANNELS, 1);
		// Map<String, Object> temp = Maps.newHashMap();
		// temp.put("mission", "START");
		// sendOutputJson(getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME),
		// temp);
		// sendOutputJson("outputCOM_M", temp);
		// For waypoint list read test case
		/*
		 * try { Thread.sleep(10000); } catch (InterruptedException e) {
		 * e.printStackTrace(); }
		 */
		// readMissionListStart();
		// readParameterListStart();
		/*
		 * try { Thread.sleep(2000); } catch (InterruptedException e) {
		 * e.printStackTrace(); }
		 */
		// sendMissionListStart();
		/*
		 * for (String [] temp:readWaypointList) { for (int i=0; i<temp.length ;
		 * i++) { getLog().info(temp[i]); } }
		 */
		// setMode("Auto");
		// getLogList();
    }

	/**
	 * Executes on activity deactivate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityDeactivate()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.mavlink deactivate");
    }

	/**
	 * Executes on activity pre shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPreShutdown()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.mavlink pre shutdown");
    }

	/**
	 * Executes on activity shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityShutdown()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.mavlink shutdown");
    }

	/**
	 * Executes on activity cleanup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityCleanup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.mavlink cleanup");
    }
    
	/**
	 * Callback for new message on the subscribed topics. Processes incoming
	 * messages.
	 * 
	 * @param channelName
	 *            Channel name of incoming message
	 * @param message
	 *            Message stored in a key-value pair in a map
	 * @see interactivespaces.activity.impl.ros.BaseRoutableRosActivity
	 * @see java.util.Map
	 * @since 1.0.0
	 */
    @Override
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
		getLog().debug("Got message on input channel " + channelName);
		getLog().debug(message);
		if (channelName.equals(subscribers[0]))
		{
			// Data from drone handled here
			if (message.containsKey("comm"))
			{

				String items[] = message.get("comm").toString()
						.replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll(" ", "").split(",");
				int lenItems = items.length;
				for (int i = 0; i < lenItems; i++)
				{
					try
					{
						responseGlobal[i] = Integer.parseInt(items[i]) & 0xFF;
					}
					catch (NumberFormatException e)
					{
						getLog().error(e);
					}

				}

				for (int i = 0; i < lenItems; i++)
				{
					mavPacket = mavParser.mavlink_parse_char(responseGlobal[i]);
				}

				if (!(mavPacket == null))
				{
					mavMessage = mavPacket.unpack();
					// getLog().info(mavPacket.seq);
					// Map<String, Object> temp = Maps.newHashMap();
					// temp.put("mavMessage", mavMessage);
					// sendOutputJson(publishers[2], temp);
					getLog().info(mavMessage.toString());
					@SuppressWarnings("unused")
					ManagedCommand mavMessageHandler = getManagedCommands().submit(new Runnable()
					{
						
						public void run()
						{
							handleMavMessage(mavMessage);
							
						}
					});

					mavPacket = null;
					mavParser = new Parser();
					// getLog().info("mavPacket2 ");
				}

			}
		}
    	
    	else if (channelName.equals(subscribers[1]))
    	{
    		
    		//Waypoint generator message handling here
    		/* 
    		 * Details : http://qgroundcontrol.org/mavlink/waypoint_protocol
    		 * */
			String tempString[] = message.get("mission").toString().split("-");
			if (tempString[0].equals("START")) {
				if (heartbeatReceiveFlag) {
					sendMissionCount = Short.parseShort(tempString[1]
							.replace(" ", ""));
					
					/*msg_mission_count missionStart = new msg_mission_count();
					missionStart.count = missionCount;
					missionStart.target_system = targetSystem;
					missionStart.target_component = targetComponent;
					byte tempByte[] = missionStart.pack().encodePacket();
					Map<String, Object> tempMapMission = Maps.newHashMap();
					tempMapMission.put("comm", Arrays.toString(tempByte));
					sendOutputJson(publishers[0], tempMapMission);
					getLog().info("SENDING COUNT : "+Arrays.toString(tempByte));
					getLog().info("TARGET SYSTEM : " + targetSystem +" TARGET COMPONENT : " + targetComponent);*/
				} 
				else {
					getLog().error("Did not receive a heartbeat packet till now");
				}
			}
    		
    		else
    		{
				
    			/*
				 * Format
				 * QGC WPL <VERSION> 
				 * <INDEX> <CURRENT WP> <COORD FRAME><COMMAND> <PARAM1> <PARAM2> <PARAM3> <PARAM4><PARAM5/X/LONGITUDE> <PARAM6/Y/LATITUDE> <PARAM7/Z/ALTITUDE><AUTOCONTINUE> 
				 * 
				 * Example
				 * QGC WPL 110 
				 * 0 1 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
				 * 1 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
				 * 2 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1
				 */
    			
    			// Rest of the messages about the waypoint data
				String missionWP[] =message.get("mission").toString()
						.replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll(" ", "").split(",");
				sendMissionItem(missionWP, tempTSystem, tempTComponent);
				/*msg_mission_item missionItem = new msg_mission_item();
				missionItem.seq = Short.parseShort(missionWP[0]);
				missionItem.current = Byte.parseByte(missionWP[1]);
				missionItem.frame = Byte.parseByte(missionWP[2]);
				missionItem.command = Short.parseShort(missionWP[3]);
				missionItem.param1 = Float.parseFloat(missionWP[4]);
				missionItem.param2 = Float.parseFloat(missionWP[5]);
				missionItem.param3 = Float.parseFloat(missionWP[6]);
				missionItem.param4 = Float.parseFloat(missionWP[7]);
				missionItem.x = Float.parseFloat(missionWP[8]);
				missionItem.y = Float.parseFloat(missionWP[9]);
				missionItem.z = Float.parseFloat(missionWP[10]);
				missionItem.autocontinue =Byte.parseByte(missionWP[11]);
				missionItem.target_system = targetSystem;
				missionItem.target_component = targetComponent;
				byte tempByte[] = missionItem.pack().encodePacket();
				Map<String, Object> tempMapMission = Maps.newHashMap();
				tempMapMission.put("comm", Arrays.toString(tempByte));
				sendOutputJson(publishers[0], tempMapMission);
				getLog().info("SENDING MISSION ITEM: "+Arrays.toString(tempByte));*/
    		}
    		
    	}
    	
		else if (channelName.equals(subscribers[2]))
		{
			// Captain message handling here
			//getLog().info(message.get("command"));
			String tempString[] = message.get("command").toString().split("=");
			//getLog().info(Arrays.toString(tempString));
			handleCaptainMessage(tempString);

		}
		else if (channelName.equals(subscribers[3]))
		{
			// Data from drone handled here
			if (message.containsKey("rc"))
			{

				String items[] = message.get("rc").toString()
						.replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll(" ", "").split(",");
				sendRCPacket(items);
			}
		}
    }
    
	/**
	 * Handles all the commands from the captain activity and performs the
	 * actions. Then, it processes the results and sends it back to the captain
	 * activity.
	 * 
	 * @param message
	 *            message from captain activity split into an array using "="
	 *            separator
	 */
	private void handleCaptainMessage(String[] message)
	{
		/*
		 * enum CommandOptions HEARTBEAT, READ_MISSION,GET_MISSION,
		 * WRITE_MISSION, SET_CURRENT_ACTIVE_WP, CLEAR_MISSION, ARM,
		 * READ_PARAMETER_LIST_START, GET_PARAMETER_LIST, GET_PARAMETER,
		 * SET_PARAMETER, AUTOPILOT_REBOOT, AUTOPILOT_SHUTDOWN,
		 * BOOTLOADER_REBOOT, SYSTEM_SHUTDOWN, SYSTEM_REBOOT, SET_MODE,
		 * SET_ALLOWED_AREA, SET_GPS_ORIGIN, READ_LOG_ENTRY, GET_LOG_ENTRY,
		 * SEND_COMMAND, READ_DATASTREAM,UPDATE_TARGET
		 */
		int c = 0;
		try
		{
			c = Integer.parseInt(message[0]);
		}
		catch (NumberFormatException e1)
		{
			getLog().error("Bad Command Type from Captain Activity");
			getLog().error(e1);
		}
		switch (c)
		{
		// HEARTBEAT
		case 0:
			String heartbeatSend = heartbeat.toString();
			heartbeat = null;
			Map<String, Object> tempHeartbeat = Maps.newHashMap();
			tempHeartbeat.put("command", heartbeatSend);
			sendOutputJson(publishers[3], tempHeartbeat);
			getLog().debug("SENDING MISSION ITEM: " + heartbeatSend);
			break;

		/**
		 *  Handles READ MISSION LIST Command from the captain activity
		 */
		case 1:
			boolean result = false;
			Map<String, Object> tempMissionRead = Maps.newHashMap();
			if (message.length == 1)
			{
				result = readMissionListStart();
			}
			else if (message.length == 3)
			{
				byte system = 0;
				byte component = 0;
				try
				{
					system = Byte.parseByte(message[1]);
					component = Byte.parseByte(message[2]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in Read Mission List handler");
					getLog().error(e);
					tempMissionRead.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempMissionRead);
					return;
				}
				result = readMissionListStart(system, component);
			}
			else
			{
				tempMissionRead.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempMissionRead);
				return;
			}

			if (result)
			{
				Date start = new Date();
				while (readWaypointCount != -1
						&& (System.currentTimeMillis() - start.getTime()) < 5000)
					;
				if (!(readWaypointCount == -1))
				{
					tempMissionRead.put("command", "FAIL");
					sendOutputJson(publishers[3], tempMissionRead);
					return;
				}
				tempMissionRead.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempMissionRead);
			}
			else
			{
				tempMissionRead.put("command", "FAIL");
				sendOutputJson(publishers[3], tempMissionRead);
				return;
			}
			break;

		/**
		 * Handles GET_MISSION Command from the captain activity
		 */
		case 2:
			Map<String, Object> tempMission = Maps.newHashMap();
			if (message.length == 1)
			{
				if (readWaypointList.isEmpty())
				{
					tempMission.put("command", "NULL");
					sendOutputJson(publishers[3], tempMission);
					return;
				}
				else
				{
					tempMission.put("mission",
							Arrays.deepToString(readWaypointList.toArray()));
					tempMission.put("command","SUCCESS");
					sendOutputJson(publishers[3], tempMission);
					//getLog().info(Arrays.deepToString(readWaypointList.toArray()));
					/*
					 * Complimentary function for processing this string String
					 * [][]back= new String[2][2]; for (int i=0; i<result.length
					 * ;i++) { back[i] = result[i].replaceAll("\\[",""
					 * ).replaceAll("\\]","").replaceAll(" ","").split(","); for
					 * (String s:back[i]) { System.out.println(s); } }
					 */
				}
			}
			else
			{
				tempMission.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempMission);
				return;
			}
			break;

		/**
		 * Handles WRITE MISSION Command from the captain activity
		 */
		case 3:
			boolean resultWriteMission = false;
			Map<String, Object> tempWriteMissionStart = Maps.newHashMap();
			if (message.length == 1)
			{
				resultWriteMission = sendMissionListStart();
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultWriteMission = sendMissionListStart(system,
							component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in write Mission List Start handler");
					getLog().error(e);
					tempWriteMissionStart.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempWriteMissionStart);
					return;
				}
			}
			else
			{
				tempWriteMissionStart.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempWriteMissionStart);
				return;
			}

			if (resultWriteMission)
			{
				tempWriteMissionStart.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempWriteMissionStart);
			}
			else
			{
				tempWriteMissionStart.put("command", "FAIL");
				sendOutputJson(publishers[3], tempWriteMissionStart);
				return;
			}
			break;

		/**
		 * Handles SET CURRENT ACTIVE WP Command from the captain activity
		 */
		case 4:
			boolean resultSetWP = false;
			Map<String, Object> tempSetCurrentWP = Maps.newHashMap();
			if (message.length == 2)
			{
				try
				{
					resultSetWP = setCurrentActiveWP(Short
							.parseShort(message[1]));
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in Set Current Active Way Point handler");
					getLog().error(e);
					tempSetCurrentWP.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetCurrentWP);
					return;
				}
			}
			else if (message.length == 4)
			{
				byte system = 0;
				byte component = 0;
				try
				{
					system = Byte.parseByte(message[2]);
					component = Byte.parseByte(message[3]);
					resultSetWP = setCurrentActiveWP(
							Short.parseShort(message[1]), system, component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in Set Current Active Way Point handler");
					getLog().error(e);
					tempSetCurrentWP.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetCurrentWP);
					return;
				}
			}
			else
			{
				tempSetCurrentWP.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSetCurrentWP);
				return;
			}

			if (resultSetWP)
			{
				tempSetCurrentWP.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSetCurrentWP);
			}
			else
			{
				tempSetCurrentWP.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSetCurrentWP);
				return;
			}
			break;

		/**
		 * Handles CLEAR MISSION Command from the captain activity
		 */
		case 5:
			boolean resultClearMission = false;
			Map<String, Object> tempClearMission = Maps.newHashMap();
			if (message.length == 1)
			{
				resultClearMission = clearMissionList();
			}
			else if (message.length == 3)
			{
				byte system = 0;
				byte component = 0;
				try
				{
					system = Byte.parseByte(message[1]);
					component = Byte.parseByte(message[2]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in Clear Mission handler");
					getLog().error(e);
					tempClearMission.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempClearMission);
					return;
				}
				resultClearMission = clearMissionList(system, component);
			}
			else
			{
				tempClearMission.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempClearMission);
				return;
			}

			if (resultClearMission)
			{
				tempClearMission.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempClearMission);
			}
			else
			{
				tempClearMission.put("command", "FAIL");
				sendOutputJson(publishers[3], tempClearMission);
				return;
			}
			break;

		/**
		 * Handles ARM Command from the captain activity
		 */
		case 6:
			boolean resultARM = false;
			Map<String, Object> tempARM = Maps.newHashMap();
			if (message.length == 1)
			{
				resultARM = doARM(true);
			}
			else if (message.length == 2)
			{
				try
				{
					resultARM = doARM(Boolean.parseBoolean(message[1]));
				}
				catch (Exception e)
				{
					getLog().error("Number format exception in do ARM handler");
					getLog().error(e);
					tempARM.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempARM);
					return;
				}
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultARM = doARM(true, system, component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in do ARM handler");
					getLog().error(e);
					tempARM.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempARM);
					return;
				}
			}
			else if (message.length == 4)
			{
				try
				{
					byte system = Byte.parseByte(message[2]);
					byte component = Byte.parseByte(message[3]);
					resultARM = doARM(Boolean.parseBoolean(message[1]), system,
							component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in do ARM handler");
					getLog().error(e);
					tempARM.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempARM);
					return;
				}
			}
			else
			{
				tempARM.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempARM);
				return;
			}

			if (resultARM)
			{
				tempARM.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempARM);
			}
			else
			{
				tempARM.put("command", "FAIL");
				sendOutputJson(publishers[3], tempARM);
				return;
			}
			break;

		/**
		 * Handles READ PARAMETER LIST START Command from the captain activity
		 */
		case 7:
			boolean resultParameterList = false;
			Map<String, Object> tempReadParameterListStart = Maps.newHashMap();
			if (message.length == 1)
			{
				resultParameterList = readParameterListStart();
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultParameterList = readParameterListStart(system,
							component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in read Parameter List Start handler");
					getLog().error(e);
					tempReadParameterListStart.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempReadParameterListStart);
					return;
				}
			}
			else
			{
				tempReadParameterListStart.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempReadParameterListStart);
				return;
			}

			if (resultParameterList)
			{
				tempReadParameterListStart.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempReadParameterListStart);
			}
			else
			{
				tempReadParameterListStart.put("command", "FAIL");
				sendOutputJson(publishers[3], tempReadParameterListStart);
				return;
			}
			break;

		/**
		 * Handles GET PARAMETER LIST Command from the captain activity
		 */
		case 8:
			Map<String, Object> tempParameterList = Maps.newHashMap();
			if (message.length == 1)
			{
				if (paramList.isEmpty())
				{
					tempParameterList.put("command", "NULL");
					sendOutputJson(publishers[3], tempParameterList);
					return;
				}
				else
				{
					tempParameterList.put("param_list", paramList); // Needs to be
																	// checked
																	// thoroughly
					tempParameterList.put("command", "SUCCESS");
					sendOutputJson(publishers[3], tempParameterList);
					//getLog().info(paramList.toString());
					/*
					 * Cast it to Map<String,Double> to make it useful.
					 */
				}
			}
			else
			{
				tempParameterList.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempParameterList);
				return;
			}
			break;

		/**
		 * Handles GET PARAMETER Command from the captain activity
		 */
		case 9:
			Map<String, Object> tempParameter = Maps.newHashMap();
			if (message.length == 2)
			{
				if (paramList.isEmpty())
				{
					tempParameter.put("command", "NULL");
					sendOutputJson(publishers[3], tempParameter);
					return;
				}
				else
				{
					if (paramList.containsKey(message[1]))
					{
						tempParameter.put("param", paramList.get(message[1])
								.toString());
						tempParameter.put("command", "SUCCESS");
						sendOutputJson(publishers[3], tempParameter);
						//getLog().info(paramList.get(message[1]));
					}
					else
					{
						tempParameter.put("command", "FAIL");
						sendOutputJson(publishers[3], tempParameter);
					}
					/*
					 * Use Double.parseDouble to make it useful.
					 */
				}
			}
			else
			{
				tempParameter.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempParameter);
				return;
			}
			break;

		/**
		 * Handles SET_PARAMETER Command from the captain activity
		 */
		case 10:
			boolean resultSetParameter = false;
			Map<String, Object> tempSetParameter = Maps.newHashMap();
			if (message.length == 3)
			{
				//getLog().info(message[2]);
				float fValue;
				try
				{
					fValue = Float.parseFloat(message[2]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in set Parameter handler");
					getLog().error(e);
					tempSetParameter.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetParameter);
					return;
				}
				resultSetParameter = setParam(message[1], fValue);
			}
			else if (message.length == 5)
			{
				float fValue;
				try
				{
					fValue = Float.parseFloat(message[2]);
					byte system = Byte.parseByte(message[3]);
					byte component = Byte.parseByte(message[4]);
					resultSetParameter = setParam(message[1], fValue, system,
							component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in set Parameter handler");
					getLog().error(e);
					tempSetParameter.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetParameter);
					return;
				}
			}
			else
			{
				tempSetParameter.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSetParameter);
				return;
			}

			if (resultSetParameter)
			{
				tempSetParameter.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSetParameter);
			}
			else
			{
				tempSetParameter.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSetParameter);
				return;
			}
			break;

		/**
		 * Handles AUTOPILOT_REBOOT Command from the captain activity
		 */
		case 11:
			boolean resultAutoPilotReboot = false;
			Map<String, Object> tempAutoPilotReboot = Maps.newHashMap();
			if (message.length == 1)
			{
				resultAutoPilotReboot = doRebootAutopilot();
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultAutoPilotReboot = doRebootAutopilot(system, component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in AutoPilot reboot handler");
					getLog().error(e);
					tempAutoPilotReboot.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempAutoPilotReboot);
					return;
				}
			}
			else
			{
				tempAutoPilotReboot.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempAutoPilotReboot);
				return;
			}

			if (resultAutoPilotReboot)
			{
				tempAutoPilotReboot.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempAutoPilotReboot);
			}
			else
			{
				tempAutoPilotReboot.put("command", "FAIL");
				sendOutputJson(publishers[3], tempAutoPilotReboot);
				return;
			}
			break;

		/**
		 * Handles AUTOPILOT_SHUTDOWN Command from the captain activity
		 */
		case 12:
			boolean resultAutoPilotShutDown = false;
			Map<String, Object> tempAutoPilotShutDown = Maps.newHashMap();
			if (message.length == 1)
			{
				resultAutoPilotShutDown = doShutdownAutopilot();
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultAutoPilotShutDown = doShutdownAutopilot(system,
							component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in AutoPilot shutdown handler");
					getLog().error(e);
					tempAutoPilotShutDown.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempAutoPilotShutDown);
					return;
				}
			}
			else
			{
				tempAutoPilotShutDown.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempAutoPilotShutDown);
				return;
			}

			if (resultAutoPilotShutDown)
			{
				tempAutoPilotShutDown.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempAutoPilotShutDown);
			}
			else
			{
				tempAutoPilotShutDown.put("command", "FAIL");
				sendOutputJson(publishers[3], tempAutoPilotShutDown);
				return;
			}
			break;

		/**
		 * Handles BOOTLOADER_REBOOT Command from the captain activity
		 */
		case 13:
			boolean resultBootloaderReboot = false;
			Map<String, Object> tempBootloaderReboot = Maps.newHashMap();
			if (message.length == 1)
			{
				resultBootloaderReboot = doBootloaderReboot();
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultBootloaderReboot = doBootloaderReboot(system,
							component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in Bootloader reboot handler");
					getLog().error(e);
					tempBootloaderReboot.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempBootloaderReboot);
					return;
				}
			}
			else
			{
				tempBootloaderReboot.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempBootloaderReboot);
				return;
			}

			if (resultBootloaderReboot)
			{
				tempBootloaderReboot.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempBootloaderReboot);
			}
			else
			{
				tempBootloaderReboot.put("command", "FAIL");
				sendOutputJson(publishers[3], tempBootloaderReboot);
				return;
			}
			break;

		/**
		 * Handles SYSTEM_SHUTDOWN Command from the captain activity
		 */
		case 14:
			boolean resultSystemShutDown = false;
			Map<String, Object> tempSystemShutDown = Maps.newHashMap();
			if (message.length == 1)
			{
				resultSystemShutDown = doSystemShutdown();
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultSystemShutDown = doSystemShutdown(system, component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in System shutdown handler");
					getLog().error(e);
					tempSystemShutDown.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSystemShutDown);
					return;
				}
			}
			else
			{
				tempSystemShutDown.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSystemShutDown);
				return;
			}

			if (resultSystemShutDown)
			{
				tempSystemShutDown.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSystemShutDown);
			}
			else
			{
				tempSystemShutDown.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSystemShutDown);
				return;
			}
			break;

		/**
		 * Handles SYSTEM_REBOOT Command from the captain activity
		 */
		case 15:
			boolean resultSystemReboot = false;
			Map<String, Object> tempSystemReboot = Maps.newHashMap();
			if (message.length == 1)
			{
				resultSystemReboot = doSystemReboot();
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[1]);
					byte component = Byte.parseByte(message[2]);
					resultSystemReboot = doSystemReboot(system, component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in System reboot handler");
					getLog().error(e);
					tempSystemReboot.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSystemReboot);
					return;
				}
			}
			else
			{
				tempSystemReboot.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSystemReboot);
				return;
			}

			if (resultSystemReboot)
			{
				tempSystemReboot.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSystemReboot);
			}
			else
			{
				tempSystemReboot.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSystemReboot);
				return;
			}
			break;

		/**
		 * Handles SET_MODE Command from the captain activity
		 */
		case 16:
			boolean resultSetMode = false;
			Map<String, Object> tempSetMode = Maps.newHashMap();
			//getLog().info(message.length);
			if (message.length == 2)
			{
				resultSetMode = setMode(message[1]);
			}
			else if (message.length == 3)
			{
				try
				{
					byte system = Byte.parseByte(message[2]);
					resultSetMode = setMode(message[1], system);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in set mode handler");
					getLog().error(e);
					tempSetMode.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetMode);
					return;
				}
			}
			else
			{
				tempSetMode.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSetMode);
				return;
			}

			if (resultSetMode)
			{
				tempSetMode.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSetMode);
			}
			else
			{
				tempSetMode.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSetMode);
				return;
			}
			break;

		/**
		 * Handles SET_ALLOWED_AREA Command from the captain activity
		 */
		case 17:
			boolean resultSetAllowedArea = false;
			Map<String, Object> tempSetAllowedArea = Maps.newHashMap();
			if (message.length == 8)
			{
				Point3D min = null, max = null;
				byte frame = 0;

				try
				{
					min = new Point3D(Double.parseDouble(message[1]),
							Double.parseDouble(message[2]),
							Double.parseDouble(message[3]));
					max = new Point3D(Double.parseDouble(message[4]),
							Double.parseDouble(message[5]),
							Double.parseDouble(message[6]));
					frame = Byte.parseByte(message[7]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in set allowed area handler");
					getLog().error(e);
					tempSetAllowedArea.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetAllowedArea);
					return;
				}
				resultSetAllowedArea = setAllowedArea(min, max, frame);
			}
			else if (message.length == 10)
			{
				Point3D min = null, max = null;
				byte frame = 0, system = 0, component = 0;

				try
				{
					system = Byte.parseByte(message[8]);
					component = Byte.parseByte(message[9]);
					min = new Point3D(Double.parseDouble(message[1]),
							Double.parseDouble(message[2]),
							Double.parseDouble(message[3]));
					max = new Point3D(Double.parseDouble(message[4]),
							Double.parseDouble(message[5]),
							Double.parseDouble(message[6]));
					frame = Byte.parseByte(message[7]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in set allowed area handler");
					getLog().error(e);
					tempSetAllowedArea.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetAllowedArea);
					return;
				}
				resultSetAllowedArea = setAllowedArea(min, max, frame, system,
						component);
			}
			else
			{
				tempSetAllowedArea.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSetAllowedArea);
				return;
			}

			if (resultSetAllowedArea)
			{
				tempSetAllowedArea.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSetAllowedArea);
			}
			else
			{
				tempSetAllowedArea.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSetAllowedArea);
				return;
			}
			break;

		/**
		 * Handles SET_GPS_ORIGIN Command from the captain activity
		 */
		case 18:
			boolean resultSetGpsOrigin = false;
			Map<String, Object> tempSetGpsOrigin = Maps.newHashMap();
			if (message.length == 4)
			{
				Point3D origin = null;
				try
				{
					origin = new Point3D(Double.parseDouble(message[1]),
							Double.parseDouble(message[2]),
							Double.parseDouble(message[3]));
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in set Global GPS Origin handler");
					getLog().error(e);
					tempSetGpsOrigin.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetGpsOrigin);
					return;
				}
				resultSetGpsOrigin = setGlobalGpsOrigin(origin);
			}
			else if (message.length == 5)
			{
				Point3D origin = null;
				byte system = 0;

				try
				{
					system = Byte.parseByte(message[4]);
					origin = new Point3D(Double.parseDouble(message[1]),
							Double.parseDouble(message[2]),
							Double.parseDouble(message[3]));
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in set Global GPS Origin handler");
					getLog().error(e);
					tempSetGpsOrigin.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSetGpsOrigin);
					return;
				}
				resultSetGpsOrigin = setGlobalGpsOrigin(origin, system);
			}
			else
			{
				tempSetGpsOrigin.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSetGpsOrigin);
				return;
			}

			if (resultSetGpsOrigin)
			{
				tempSetGpsOrigin.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSetGpsOrigin);
			}
			else
			{
				tempSetGpsOrigin.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSetGpsOrigin);
				return;
			}
			break;

		/**
		 * Handles READ_LOG_ENTRY Command from the captain activity
		 */
		case 19:
			boolean resultReadLogEntry = false;
			Map<String, Object> tempReadLogEntry = Maps.newHashMap();
			if (message.length == 1)
			{
				resultReadLogEntry = getLogList();
			}
			else if (message.length == 3)
			{
				byte system = 0, component = 0;

				try
				{
					system = Byte.parseByte(message[1]);
					component = Byte.parseByte(message[2]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in read Log Entry handler");
					getLog().error(e);
					tempReadLogEntry.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempReadLogEntry);
					return;
				}
				resultReadLogEntry = getLogList(system, component);
			}
			else
			{
				tempReadLogEntry.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempReadLogEntry);
				return;
			}

			if (resultReadLogEntry)
			{
				tempReadLogEntry.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempReadLogEntry);
			}
			else
			{
				tempReadLogEntry.put("command", "FAIL");
				sendOutputJson(publishers[3], tempReadLogEntry);
				return;
			}
			break;

		/**
		 * Handles GET_LOG_ENTRY Command from the captain activity
		 */
		case 20:
			Map<String, Object> tempGetLogEntry = Maps.newHashMap();
			if (message.length == 1)
			{
				if (logEntry.isEmpty())
				{
					tempGetLogEntry.put("command", "NULL");
					sendOutputJson(publishers[3], tempGetLogEntry);
					return;
				}
				else
				{
					tempGetLogEntry.put("log_entry",
							Arrays.deepToString(logEntry.toArray()));
					tempGetLogEntry.put("command", "SUCCESS");
					sendOutputJson(publishers[3], tempGetLogEntry);
					//getLog().info(Arrays.deepToString(logEntry.toArray()));
					/*
					 * Complimentary function for processing this string String
					 * [][]back= new String[2][2]; for (int i=0; i<result.length
					 * ;i++) { back[i] = result[i].replaceAll("\\[",""
					 * ).replaceAll("\\]","").replaceAll(" ","").split(","); for
					 * (String s:back[i]) { System.out.println(s); } }
					 */
				}
			}
			else
			{
				tempGetLogEntry.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempGetLogEntry);
				return;
			}
			break;

		/**
		 * Handles SEND_COMMAND Command from the captain activity
		 */
		case 21:
			boolean resultSendCommand = false;
			Map<String, Object> tempSendCommand = Maps.newHashMap();
			if (message.length == 9)
			{
				Short actionid =null;
				Float p[] = new Float[7];
				try
				{
					actionid = Short.parseShort(message[1]);
					for (int i = 0; i < p.length; i++)
					{
						p[i] = Float.parseFloat(message[2+i]);
					} 
					resultSendCommand = doCommand(actionid, p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in send Command handler");
					getLog().error(e);
					tempSendCommand.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSendCommand);
					return;
				}
			}
			else if (message.length == 11)
			{
				byte system = 0, component = 0;
				Short actionid =null;
				Float p[] = new Float[7];
				try
				{
					system = Byte.parseByte(message[9]);
					component = Byte.parseByte(message[10]);
					actionid = Short.parseShort(message[1]);
					for (int i = 0; i < p.length; i++)
					{
						p[i] = Float.parseFloat(message[2+i]);
					} 
					resultSendCommand = doCommand(actionid,p[0], p[1], p[2], p[3], p[4], p[5], p[6],system,component);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in send Command handler");
					getLog().error(e);
					tempSendCommand.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempSendCommand);
					return;
				}
			}
			else
			{
				tempSendCommand.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempSendCommand);
				return;
			}

			if (resultSendCommand)
			{
				tempSendCommand.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempSendCommand);
			}
			else
			{
				tempSendCommand.put("command", "FAIL");
				sendOutputJson(publishers[3], tempSendCommand);
				return;
			}
			break;

		/**
		 * Handles READ_DATASTREAM Command from the captain activity
		 */
		case 22:
			Map<String, Object> tempReadDataStream = Maps.newHashMap();
			if (message.length == 3)
			{
				int id, value;
				try
				{
					id = Integer.parseInt(message[1]);
					value = Integer.parseInt(message[2]);
					getDataStream(id, value);
				}
				catch (NumberFormatException e)
				{
					getLog().error(
							"Number format exception in read DataStream handler");
					getLog().error(e);
					tempReadDataStream.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempReadDataStream);
					return;
				}
			}
			else if (message.length == 5)
			{
				int id, value;
				try
				{
					id = Integer.parseInt(message[1]);
					value = Integer.parseInt(message[2]);
					byte system = Byte.parseByte(message[3]);
					byte component = Byte.parseByte(message[4]);
					getDataStream(id, value, system, component);
				}
				catch (NumberFormatException e)
				{
					getLog().error(
							"Number format exception in read DataStream handler");
					getLog().error(e);
					tempReadDataStream.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempReadDataStream);
					return;
				}
			}
			else if (message.length == 6)
			{
				int id, value;
				boolean startStop;
				try
				{
					id = Integer.parseInt(message[1]);
					value = Integer.parseInt(message[2]);
					startStop = Boolean.parseBoolean(message[3]);
					byte system = Byte.parseByte(message[4]);
					byte component = Byte.parseByte(message[5]);
					getDataStream(id, value, startStop, system, component);
				}
				catch (NumberFormatException e)
				{
					getLog().error(
							"Number format exception in read DataStream handler");
					getLog().error(e);
					tempReadDataStream.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempReadDataStream);
					return;
				}
			}
			else
			{
				tempReadDataStream.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempReadDataStream);
				return;
			}
			tempReadDataStream.put("command", "SUCCESS");
			sendOutputJson(publishers[3], tempReadDataStream);
			break;

		/**
		 * Handles UPDATE_TARGET Command from the captain activity
		 */
		case 23:
			Map<String, Object> tempUpdateTarget = Maps.newHashMap();
			if (message.length == 2)
			{
				try
				{
					targetSystem = Byte.parseByte(message[1]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in Update Target handler");
					getLog().error(e);
					tempUpdateTarget.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempUpdateTarget);
					return;
				}
				tempUpdateTarget.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempUpdateTarget);
			}
			else if (message.length == 3)
			{
				try
				{
					targetSystem = Byte.parseByte(message[1]);
					targetComponent = Byte.parseByte(message[2]);
				}
				catch (NumberFormatException e)
				{
					getLog().error("Number format exception in Update Target handler");
					getLog().error(e);
					tempUpdateTarget.put("command", "BADCMD");
					sendOutputJson(publishers[3], tempUpdateTarget);
					return;
				}
				tempUpdateTarget.put("command", "SUCCESS");
				sendOutputJson(publishers[3], tempUpdateTarget);
			}
			else
			{
				tempUpdateTarget.put("command", "BADCMD");
				sendOutputJson(publishers[3], tempUpdateTarget);
				return;
			}
			break;
			
		default:
			break;
		}

	}

	/**
	 * Use this function to get a variable name which is equal to certain value
	 * Intended for getting variables in the enum folder of mavlink package.
	 * 
	 * @param className
	 *            Name of the class to be queried.
	 * @param matchVar
	 *            Variable to be matched
	 * @return Name of the variable in the class.
	 */
    private String getVariableName(String className , int matchVar)
    {
		String variableName = null;
    	/*Class<?> classVar = null;
		try 
		{
			classVar = Class.forName(className);
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
    	Field[] fields = classVar.getFields();
		try {
			for (int i = 0; i < fields.length; i++) 
			{
				if (matchVar == fields[i].getInt(classVar)) 
				{
					variableName =  fields[i].getName();
					break;
				}
			}
		} 
		catch (IllegalArgumentException e) 
		{
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) 
		{
				e.printStackTrace();
		}*/
		return variableName;
    }

	/**
	 * Use this function to get all the variables of a specified class Intended
	 * for getting values from mavlink package.
	 * 
	 * @param className
	 *            Name of the class to be queried.
	 * @return All the public data names.
	 */
    @SuppressWarnings("unused")
	private String [] getVariableNames(String className )
    {
    	String variableNames [];
		Class<?> classVar = null;
    	try 
    	{
    		classVar= Class.forName(className);
		} 
    	catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
    	Field [] fields = classVar.getFields();
    	variableNames = new String[fields.length];
    	for (int i = 0; i < fields.length; i++) 
    	{
			variableNames[i] = fields[i].getName();
		}
		return variableNames;
    }
    
	/**
	 * Handles all incoming messages from drone via communications activity.
	 * 
	 * @param mavMessage2
	 *            message received from communication activity
	 */
	private void handleMavMessage(MAVLinkMessage mavMessage2) 
	{
		switch (mavMessage2.msgid) 
		{
		case msg_set_cam_shutter.MAVLINK_MSG_ID_SET_CAM_SHUTTER:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_image_triggered.MAVLINK_MSG_ID_IMAGE_TRIGGERED:
			msg_image_triggered mavImageTriggered;
			if (mavMessage2 instanceof msg_image_triggered) 
			{
				mavImageTriggered = (msg_image_triggered) mavMessage2;
				Map<String, Object> tempMavImageTriggered = Maps.newHashMap();
				String tempImageTriggered = "[" + mavImageTriggered.timestamp
						+ "] , " + "SEQUENCE : " + mavImageTriggered.seq
						+ " , " + "ROLL : " + mavImageTriggered.roll + "rad , "
						+ "PITCH : " + mavImageTriggered.pitch + "rad , "
						+ "YAW : " + mavImageTriggered.yaw + "rad , "
						+ "LOCAL HEIGHT : " + mavImageTriggered.local_z
						+ "metres , " + "LATITUDE : " + mavImageTriggered.lat
						+ "degrees , " + "LONGITUDE : " + mavImageTriggered.lon
						+ "degrees , " + "GLOBAL ALTITUDE : "
						+ mavImageTriggered.alt + "metres , "
						+ "GROUND TRUTH X : " + mavImageTriggered.ground_x
						+ " , " + "GROUND TRUTH Y : "
						+ mavImageTriggered.ground_y + " , "
						+ "GROUND TRUTH Z : " + mavImageTriggered.ground_z;
				tempMavImageTriggered.put("data",
						"MAVLINK_MSG_ID_IMAGE_TRIGGERED - "
								+ tempImageTriggered);
				sendOutputJson(publishers[2], tempMavImageTriggered);
				getLog().debug(tempImageTriggered);
			}
			break;

		case msg_image_trigger_control.MAVLINK_MSG_ID_IMAGE_TRIGGER_CONTROL:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_image_available.MAVLINK_MSG_ID_IMAGE_AVAILABLE:
			msg_image_available mavImageAvailable;
			if (mavMessage2 instanceof msg_image_available) 
			{
				mavImageAvailable = (msg_image_available) mavMessage2;
				Map<String, Object> tempMavImageAvailable = Maps.newHashMap();
				String tempImageAvailable = "[" + mavImageAvailable.timestamp
						+ "] , " + "CAM ID : " + mavImageAvailable.cam_id
						+ " , " + "VALID UNTIL : "
						+ mavImageAvailable.valid_until + " ,"
						+ "IMAGE SEQUENCE : " + mavImageAvailable.img_seq
						+ " , " + "IMAGE BUFFER INDEX : "
						+ mavImageAvailable.img_buf_index + " , "
						+ "SHARED MEMORY KEY : " + mavImageAvailable.key
						+ " , " + "EXPOSURE : " + mavImageAvailable.exposure
						+ "s , " + "GAIN : " + mavImageAvailable.gain + " , "
						+ "ROLL : " + mavImageAvailable.roll + "rad , "
						+ "PITCH : " + mavImageAvailable.pitch + "rad , "
						+ "YAW : " + mavImageAvailable.yaw + "rad , "
						+ "LOCAL HEIGHT : " + mavImageAvailable.local_z
						+ "metres , " + "LATITUDE : " + mavImageAvailable.lat
						+ "degrees , " + "LONGITUDE : " + mavImageAvailable.lon
						+ "degrees , " + "GLOBAL ALTITUDE : "
						+ mavImageAvailable.alt + "metres , "
						+ "GROUND TRUTH X : " + mavImageAvailable.ground_x
						+ " , " + "GROUND TRUTH Y : "
						+ mavImageAvailable.ground_y + " , "
						+ "GROUND TRUTH Z : " + mavImageAvailable.ground_z
						+ " , " + "IMAGE WIDTH : " + mavImageAvailable.width
						+ "pixels , " + "IMAGE HEIGHT : "
						+ mavImageAvailable.height + "pixels , "
						+ "IMAGE DEPTH : " + mavImageAvailable.depth + " , "
						+ "CAMERA NUMBER : " + mavImageAvailable.cam_no + " , "
						+ "IMAGE CHANNELS : " + mavImageAvailable.channels;
				tempMavImageAvailable.put("data",
						"MAVLINK_MSG_ID_IMAGE_AVAILABLE - "
								+ tempImageAvailable);
				sendOutputJson(publishers[2], tempMavImageAvailable);
				getLog().debug(tempImageAvailable);
			}
			break;

		case msg_set_position_control_offset.MAVLINK_MSG_ID_SET_POSITION_CONTROL_OFFSET:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_position_control_setpoint.MAVLINK_MSG_ID_POSITION_CONTROL_SETPOINT:
			msg_position_control_setpoint mavPositionControlSetpoint;
			if (mavMessage2 instanceof msg_position_control_setpoint) 
			{
				mavPositionControlSetpoint = (msg_position_control_setpoint) mavMessage2;
				String tempPositionControlSetpoint = "X : "
						+ mavPositionControlSetpoint.x + "metres , " + "Y : "
						+ mavPositionControlSetpoint.y + "metres , " + "Z : "
						+ mavPositionControlSetpoint.z + "metres , " + "YAW : "
						+ mavPositionControlSetpoint.yaw + "rad , " + "ID : "
						+ mavPositionControlSetpoint.id;
				Map<String, Object> tempMavPositionControlSetpoint = Maps
						.newHashMap();
				tempMavPositionControlSetpoint.put("data",
						"MAVLINK_MSG_ID_POSITION_CONTROL_SETPOINT - "
								+ tempPositionControlSetpoint);
				sendOutputJson(publishers[2], tempMavPositionControlSetpoint);
				getLog().debug(tempPositionControlSetpoint);
			}
			break;

		case msg_marker.MAVLINK_MSG_ID_MARKER:
			msg_marker mavMarker;
			if (mavMessage2 instanceof msg_marker) 
			{
				mavMarker = (msg_marker) mavMessage2;
				String tempMarker = "X : " + mavMarker.x + "metres , " + "Y : "
						+ mavMarker.y + "metres , " + "Z : " + mavMarker.z
						+ "metres , " + "ROLL : " + mavMarker.roll + "rad , "
						+ "PITCH : " + mavMarker.pitch + "rad , " + "YAW : "
						+ mavMarker.yaw + "rad , " + "ID : " + mavMarker.id;
				Map<String, Object> tempMavMarker = Maps.newHashMap();
				tempMavMarker.put("data", "MAVLINK_MSG_ID_MARKER - "
						+ tempMarker);
				sendOutputJson(publishers[2], tempMavMarker);
				getLog().debug(tempMarker);
			}
			break;

		case msg_raw_aux.MAVLINK_MSG_ID_RAW_AUX:
			msg_raw_aux mavRawAux;
			if (mavMessage2 instanceof msg_raw_aux) 
			{
				mavRawAux = (msg_raw_aux) mavMessage2;
				String tempRawAux = "PRESSURE : " + mavRawAux.baro * 100.0
						+ "Pascal , " + "ADC1 (AD0.6) : " + mavRawAux.adc1
						+ " , " + "ADC2 (AD0.2) : " + mavRawAux.adc2 + " , "
						+ "ADC3 (AD0.1) : " + mavRawAux.adc3 + " , "
						+ "ADC4 (AD1.3) : " + mavRawAux.adc4 + " , "
						+ "BATTERY VOLTAGE : " + mavRawAux.vbat + " , "
						+ "TEMPERATURE : " + mavRawAux.temp;
				Map<String, Object> tempMavRawAux = Maps.newHashMap();
				tempMavRawAux.put("data", "MAVLINK_MSG_ID_RAW_AUX - "
						+ tempRawAux);
				sendOutputJson(publishers[2], tempMavRawAux);
				getLog().debug(tempRawAux);
			}
			break;

		case msg_watchdog_heartbeat.MAVLINK_MSG_ID_WATCHDOG_HEARTBEAT:
			msg_watchdog_heartbeat mavWatchdogHeartbeat;
			if (mavMessage2 instanceof msg_watchdog_heartbeat) 
			{
				mavWatchdogHeartbeat = (msg_watchdog_heartbeat) mavMessage2;
				String tempWatchdogHeartbeat = "WATCHDOG ID : "
						+ mavWatchdogHeartbeat.watchdog_id + " , "
						+ "PROCESS COUNT : "
						+ mavWatchdogHeartbeat.process_count;
				Map<String, Object> tempMavWatchdogHeartbeat = Maps
						.newHashMap();
				tempMavWatchdogHeartbeat.put("data",
						"MAVLINK_MSG_ID_WATCHDOG_HEARTBEAT - "
								+ tempWatchdogHeartbeat);
				sendOutputJson(publishers[2], tempMavWatchdogHeartbeat);
				getLog().debug(tempWatchdogHeartbeat);
			}
			break;

		case msg_watchdog_process_info.MAVLINK_MSG_ID_WATCHDOG_PROCESS_INFO:
			msg_watchdog_process_info mavWatchdogProcessInfo;
			if (mavMessage2 instanceof msg_watchdog_process_info) 
			{
				mavWatchdogProcessInfo = (msg_watchdog_process_info) mavMessage2;

				String processName = null;
				String processArguments = null;
				try 
				{
					processName = new String(mavWatchdogProcessInfo.name,
							"UTF-8");
					processArguments = new String(
							mavWatchdogProcessInfo.arguments, "UTF-8");
				} 
				catch (UnsupportedEncodingException e) 
				{
					getLog().error(e);
				}
				String tempWatchdogProcessInfo = "TIMEOUT : "
						+ mavWatchdogProcessInfo.timeout + " , "
						+ "WATCHDOG ID : " + mavWatchdogProcessInfo.watchdog_id
						+ " , " + "PROCESS ID : "
						+ mavWatchdogProcessInfo.process_id + " , "
						+ "PROCESS NAME : " + processName + " , "
						+ "PROCESS ARGUMENTS : " + processArguments;
				Map<String, Object> tempMavWatchdogProcessInfo = Maps
						.newHashMap();
				tempMavWatchdogProcessInfo.put("data",
						"MAVLINK_MSG_ID_WATCHDOG_PROCESS_INFO - "
								+ tempWatchdogProcessInfo);
				sendOutputJson(publishers[2], tempMavWatchdogProcessInfo);
				getLog().info(tempWatchdogProcessInfo);
			}
			break;

		case msg_watchdog_process_status.MAVLINK_MSG_ID_WATCHDOG_PROCESS_STATUS:
			msg_watchdog_process_status mavWatchdogProcessStatus;
			if (mavMessage2 instanceof msg_watchdog_process_status) 
			{
				mavWatchdogProcessStatus = (msg_watchdog_process_status) mavMessage2;
				String[] processStatus = { "RUNNING", "FINISHED", "SUSPENDED",
						"CRASHED" };
				String tempWatchdogProcessStatus = "PID : "
						+ mavWatchdogProcessStatus.pid + " , "
						+ "WATCHDOG ID : "
						+ mavWatchdogProcessStatus.watchdog_id + " , "
						+ "PROCESS ID : " + mavWatchdogProcessStatus.process_id
						+ " , " + "CRASH COUNT : "
						+ mavWatchdogProcessStatus.crashes + " , "
						+ "PRESENT STATE : " + processStatus[mavWatchdogProcessStatus.state]
						+ " , " + "IS MUTED : "
						+ mavWatchdogProcessStatus.muted;
				
				/**
				 * State : Is running / finished / suspended / crashed
				 */
				
				Map<String, Object> tempMavWatchdogProcessStatus = Maps
						.newHashMap();
				tempMavWatchdogProcessStatus.put("data",
						"MAVLINK_MSG_ID_WATCHDOG_PROCESS_STATUS - "
								+ tempWatchdogProcessStatus);
				sendOutputJson(publishers[2], tempMavWatchdogProcessStatus);
				getLog().debug(tempWatchdogProcessStatus);
			}
			break;

		case msg_watchdog_command.MAVLINK_MSG_ID_WATCHDOG_COMMAND:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_pattern_detected.MAVLINK_MSG_ID_PATTERN_DETECTED:
			msg_pattern_detected mavPatternDetected;
			if (mavMessage2 instanceof msg_pattern_detected) {
				mavPatternDetected = (msg_pattern_detected) mavMessage2;

				String fileName = null;
				try 
				{
					fileName = new String(mavPatternDetected.file, "UTF-8");
				} 
				catch (UnsupportedEncodingException e) 
				{
					getLog().error(e);
				}
				String tempPatternDetected = "CONFIDENCE : "
						+ mavPatternDetected.confidence + " , " + "TYPE : "
						+ mavPatternDetected.type + " , " + "FILE NAME : "
						+ fileName + " , " + "DETECTED : "
						+ mavPatternDetected.detected;
				Map<String, Object> tempMavPatternDetected = Maps.newHashMap();
				tempMavPatternDetected.put("data",
						"MAVLINK_MSG_ID_PATTERN_DETECTED - "
								+ tempPatternDetected);
				sendOutputJson(publishers[2], tempMavPatternDetected);
				getLog().info(tempPatternDetected);
			}
			break;

		case msg_point_of_interest.MAVLINK_MSG_ID_POINT_OF_INTEREST:
			msg_point_of_interest mavPointOfInterest;
			if (mavMessage2 instanceof msg_point_of_interest) {
				mavPointOfInterest = (msg_point_of_interest) mavMessage2;
				String[] PoiColor = { "BLUE", "YELLOW", "RED", "ORANGE",
						"GREEN", "MAGNETA" };
				String[] PoiType = { "NOTICE", "WARNING", "CRITICAL",
						"EMERGENCY", "DEBUG" };
				String[] PoiCoordinateSystem = { "GLOBAL", "LOCAL" };
				String poiName = null;
				try 
				{
					poiName = new String(mavPointOfInterest.name, "UTF-8");
				} 
				catch (UnsupportedEncodingException e)
				{
					getLog().error(e);
				}
				String tempPointOfInterest = "X : "
						+ mavPointOfInterest.x
						+ "metres , "
						+ "Y : "
						+ mavPointOfInterest.y
						+ "metres , "
						+ "Z : "
						+ mavPointOfInterest.z
						+ "metres , "
						+ "TIMEOUT : "
						+ mavPointOfInterest.timeout
						+ "s , "
						+ "TYPE : "
						+ PoiType[mavPointOfInterest.type]
						+ " , "
						+ "COLOR : "
						+ PoiColor[mavPointOfInterest.color]
						+ " , "
						+ "COORDINATE SYSTEM : "
						+ PoiCoordinateSystem[mavPointOfInterest.coordinate_system]
						+ " , " + "POI NAME : " + poiName;
				Map<String, Object> tempMavPointOfInterest = Maps.newHashMap();
				tempMavPointOfInterest.put("data",
						"MAVLINK_MSG_ID_POINT_OF_INTEREST - "
								+ tempPointOfInterest);
				sendOutputJson(publishers[2], tempMavPointOfInterest);
				getLog().info(tempPointOfInterest);
			}
			break;

		case msg_point_of_interest_connection.MAVLINK_MSG_ID_POINT_OF_INTEREST_CONNECTION:
			msg_point_of_interest_connection mavPointOfInterestConnection;
			if (mavMessage2 instanceof msg_point_of_interest_connection) {
				mavPointOfInterestConnection = (msg_point_of_interest_connection) mavMessage2;
				String[] PoiColor = { "BLUE", "YELLOW", "RED", "ORANGE",
						"GREEN", "MAGNETA" };
				String[] PoiType = { "NOTICE", "WARNING", "CRITICAL",
						"EMERGENCY", "DEBUG" };
				String[] PoiCoordinateSystem = { "GLOBAL", "LOCAL" };
				String poiName = null;
				try 
				{
					poiName = new String(mavPointOfInterestConnection.name, "UTF-8");
				} 
				catch (UnsupportedEncodingException e)
				{
					getLog().error(e);
				}
				String tempPointOfInterestConnection = "X1 : "
						+ mavPointOfInterestConnection.xp1
						+ "metres , "
						+ "Y1 : "
						+ mavPointOfInterestConnection.yp1
						+ "metres , "
						+ "Z1 : "
						+ mavPointOfInterestConnection.zp1
						+ "metres , "
						+ "X2 : "
						+ mavPointOfInterestConnection.xp2
						+ "metres , "
						+ "Y2 : "
						+ mavPointOfInterestConnection.yp2
						+ "metres , "
						+ "Z2 : "
						+ mavPointOfInterestConnection.zp2
						+ "metres , "
						+ "TIMEOUT : "
						+ mavPointOfInterestConnection.timeout
						+ "s , "
						+ "TYPE : "
						+ PoiType[mavPointOfInterestConnection.type]
						+ " , "
						+ "COLOR : "
						+ PoiColor[mavPointOfInterestConnection.color]
						+ " , "
						+ "COORDINATE SYSTEM : "
						+ PoiCoordinateSystem[mavPointOfInterestConnection.coordinate_system]
						+ " , " + "POI NAME : " + poiName;
				Map<String, Object> tempMavPointOfInterestConnection = Maps.newHashMap();
				tempMavPointOfInterestConnection.put("data",
						"MAVLINK_MSG_ID_POINT_OF_INTEREST_CONNECTION - "
								+ tempPointOfInterestConnection);
				sendOutputJson(publishers[2], tempMavPointOfInterestConnection);
				getLog().info(tempPointOfInterestConnection);
			}
			break;

		case msg_brief_feature.MAVLINK_MSG_ID_BRIEF_FEATURE:
			msg_brief_feature mavBriefFeature;
			if (mavMessage2 instanceof msg_brief_feature) 
			{
				mavBriefFeature = (msg_brief_feature) mavMessage2;
				String featureDescriptor = null;
				try 
				{
					featureDescriptor = new String(mavBriefFeature.descriptor,
							"UTF-8");
				} 
				catch (UnsupportedEncodingException e) 
				{
					getLog().error(e);
				}
				String tempBriefFeature = "X : " + mavBriefFeature.x
						+ "metres , " + "Y : " + mavBriefFeature.y
						+ "metres , " + "Z : " + mavBriefFeature.z
						+ "metres , " + "RESPONSE : "
						+ mavBriefFeature.response + " , " + "SIZE : "
						+ mavBriefFeature.size + "pixels , " + "ORIENTATION : "
						+ mavBriefFeature.orientation + " , "
						+ "ORIENTATION ASSIGNMENT : "
						+ mavBriefFeature.orientation_assignment + " , "
						+ "DESCRIPTOR : " + featureDescriptor;
				Map<String, Object> tempMavBriefFeature = Maps.newHashMap();
				tempMavBriefFeature.put("data",
						"MAVLINK_MSG_ID_BRIEF_FEATURE - " + tempBriefFeature);
				sendOutputJson(publishers[2], tempMavBriefFeature);
				getLog().info(tempBriefFeature);
			}
			break;

		case msg_attitude_control.MAVLINK_MSG_ID_ATTITUDE_CONTROL:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_detection_stats.MAVLINK_MSG_ID_DETECTION_STATS:
			msg_detection_stats mavDetectionStats;
			if (mavMessage2 instanceof msg_detection_stats) 
			{
				mavDetectionStats = (msg_detection_stats) mavMessage2;
				Map<String, Object> tempMavDetectionStats = Maps.newHashMap();
				String tempDetectionStats = "NUMBER OF DETECTIONS : "
						+ mavDetectionStats.detections + " ,"
						+ "NUMBER OF CLUSTER ITERATIONS : "
						+ mavDetectionStats.cluster_iters + " , "
						+ "BEST SCORE : " + mavDetectionStats.best_score
						+ " , " + "BEST LATITUDE : "
						+ mavDetectionStats.best_lat / 10000000.0
						+ "degrees , " + "BEST LONGITUDE : "
						+ mavDetectionStats.best_lon / 10000000.0
						+ "degrees , " + "BEST ALTITUDE : "
						+ mavDetectionStats.best_alt / 1000.0 + "metres , "
						+ "BEST DETECTION ID : "
						+ mavDetectionStats.best_detection_id + " , "
						+ "BEST CLUSTER ID : "
						+ mavDetectionStats.best_cluster_id + " , "
						+ "BEST CLUSTER ITERATION ID : "
						+ mavDetectionStats.best_cluster_iter_id + " , "
						+ "NUMBER OF IMAGE PROCESSED : "
						+ mavDetectionStats.images_done + " , "
						+ "NUMBER OF IMAGES TO PROCESS : "
						+ mavDetectionStats.images_todo + " , " + "FPS : "
						+ mavDetectionStats.fps;
				tempMavDetectionStats.put("data",
						"MAVLINK_MSG_ID_DETECTION_STATS - "
								+ tempDetectionStats);
				sendOutputJson(publishers[2], tempMavDetectionStats);
				getLog().debug(tempDetectionStats);
			}
			break;

		case msg_onboard_health.MAVLINK_MSG_ID_ONBOARD_HEALTH:
			msg_onboard_health mavOnboardHealth;
			if (mavMessage2 instanceof msg_onboard_health) 
			{
				mavOnboardHealth = (msg_onboard_health) mavMessage2;
				String[] tempDiskHealth = { "N/A", "ERROR", "READ ONLY",
						"READ WRITE" };
				Map<String, Object> tempMavOnboardHealth = Maps.newHashMap();
				String tempOnboardHealth = "UPTIME : "
						+ mavOnboardHealth.uptime + "s , " + "TOTAL RAM : "
						+ mavOnboardHealth.ram_total + "GB , "
						+ "TOTAL SWAP : " + mavOnboardHealth.swap_total
						+ "GB , " + "TOTAL DISK : "
						+ mavOnboardHealth.disk_total + "GB , "
						+ "TEMPERATURE : " + mavOnboardHealth.temp
						+ "degree Celsius , " + "SUPPLY VOLTAGE : "
						+ mavOnboardHealth.voltage + "V , "
						+ "NETWORK INBOUND : "
						+ mavOnboardHealth.network_load_in + "KB/s , "
						+ "NETWORK OUTBOUND : "
						+ mavOnboardHealth.network_load_out + "KB/s , "
						+ "CPU FREQUENCY : " + mavOnboardHealth.cpu_freq
						+ "MHz , " + "CPU LOAD : " + mavOnboardHealth.cpu_load
						+ "% , " + "RAM USED : " + mavOnboardHealth.ram_usage
						+ "% , " + "SWAP USED : " + mavOnboardHealth.swap_usage
						+ "% , " + "DISK HEALTH : "
						+ tempDiskHealth[mavOnboardHealth.disk_health] + " , "
						+ "DISK USED : " + mavOnboardHealth.disk_usage + "% ";
				tempMavOnboardHealth.put("data",
						"MAVLINK_MSG_ID_ONBOARD_HEALTH - " + tempOnboardHealth);
				sendOutputJson(publishers[2], tempMavOnboardHealth);
				getLog().debug(tempOnboardHealth);
			}
			break;

		case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
			msg_heartbeat mavHeartbeat;
			if (mavMessage2 instanceof msg_heartbeat) 
			{
				mavHeartbeat = (msg_heartbeat) mavMessage2;
				if (!heartbeatReceiveFlag) {
					targetSystem = (byte) mavHeartbeat.sysid;
					targetComponent = (byte) mavHeartbeat.compid;
					heartbeatReceiveFlag = true;
				}
				Map<String, Object> tempMavHeartbeat = Maps.newHashMap();
				String tempHeartbeat = "TYPE : "
						+ getVariableName("MAV_TYPE", mavHeartbeat.type)
						+ ","
						+ "AUTOPILOT : "
						+ getVariableName("MAV_AUTOPILOT",
								mavHeartbeat.autopilot) + ","
						+ "BASE MODE : "
						+ getVariableName("MAV_MODE_FLAG",
								mavHeartbeat.base_mode) + ","
						+ "STATUS : "
						+ getVariableName("MAV_STATE",
								mavHeartbeat.system_status)+ ","
						+ "MAVLINK VERSION : "
						+ Byte.toString(mavHeartbeat.mavlink_version);
				tempMavHeartbeat.put("data", "MAVLINK_MSG_ID_HEARTBEAT - "
						+ tempHeartbeat);
				sendOutputJson(publishers[2], tempMavHeartbeat);
				getLog().debug(tempHeartbeat);
				heartbeat = mavHeartbeat;
				
				// For heartbeat topic
				String heartbeatTopic = mavHeartbeat.sysid + ","
						+ mavHeartbeat.compid + ","
						+ mavHeartbeat.mavlink_version + ","
						+ mavHeartbeat.type + "," + mavHeartbeat.autopilot
						+ "," + mavHeartbeat.base_mode + ","
						+ mavHeartbeat.custom_mode + ","
						+ mavHeartbeat.system_status;
				tempMavHeartbeat.clear();
				tempMavHeartbeat.put("heartbeat", heartbeatTopic);
				sendOutputJson(publishers[4], tempMavHeartbeat);
			}
			break;

		case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
			Map<String,	Object> tempMavSysStatus = Maps.newHashMap();
			tempMavSysStatus.put("data", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavSysStatus);
			getLog().debug(mavMessage2.toString());
			
			tempMavSysStatus.clear();
			tempMavSysStatus.put("status", mavMessage2.toString());
			sendOutputJson(publishers[7], tempMavSysStatus);
			break;

		case msg_system_time.MAVLINK_MSG_ID_SYSTEM_TIME:
			Map<String,	Object> tempMavSysTime = Maps.newHashMap();
			tempMavSysTime.put("data", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavSysTime);
			getLog().debug(mavMessage2.toString());
			
			tempMavSysTime.clear();
			tempMavSysTime.put("time", mavMessage2.toString());
			sendOutputJson(publishers[8], tempMavSysTime);
			break;

		case msg_ping.MAVLINK_MSG_ID_PING:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_change_operator_control.MAVLINK_MSG_ID_CHANGE_OPERATOR_CONTROL:

			break;

		case msg_change_operator_control_ack.MAVLINK_MSG_ID_CHANGE_OPERATOR_CONTROL_ACK:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_auth_key.MAVLINK_MSG_ID_AUTH_KEY:
			Map<String,	Object> tempMavSAuthKey = Maps.newHashMap();
			tempMavSAuthKey.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavSAuthKey);
			getLog().debug(mavMessage2.toString());
			break;

		case msg_set_mode.MAVLINK_MSG_ID_SET_MODE:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_param_request_read.MAVLINK_MSG_ID_PARAM_REQUEST_READ:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_param_request_list.MAVLINK_MSG_ID_PARAM_REQUEST_LIST:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE:
			msg_param_value mavParamValue;
			if (mavMessage2 instanceof msg_param_value) 
			{
				mavParamValue = (msg_param_value) mavMessage2;
				Map<String, Object> tempMavParamValue = Maps.newHashMap();
				String[] paramType =
				{ "MAV_PARAM_TYPE_UINT8", "MAV_PARAM_TYPE_INT8",
						"MAV_PARAM_TYPE_UINT16", "MAV_PARAM_TYPE_INT16",
						"MAV_PARAM_TYPE_UINT32", "MAV_PARAM_TYPE_INT32",
						"MAV_PARAM_TYPE_UINT64", "MAV_PARAM_TYPE_INT64",
						"MAV_PARAM_TYPE_REAL64", "MAV_PARAM_TYPE_ENUM_END" };
				String tempParamValue = "TOTAL NUMBER OF PARAMETERS : "
						+ mavParamValue.param_count
						+ " , "
						+ "CURRENT PARAMETER INDEX : "
						+ mavParamValue.param_index
						+ " , "
						+ "PARAMETER VALUE : "
						+ mavParamValue.param_value
						+ " , "
						+ "PARAMETER ID : "
						+ new String(mavParamValue.param_id)
						+ " , "
						+ "PARAMETER TYPE : "
						+ paramType[mavParamValue.param_type-1];
				tempMavParamValue.put("data", "MAVLINK_MSG_ID_PARAM_VALUE - "
						+ tempParamValue);
				sendOutputJson(publishers[2], tempMavParamValue);
				getLog().info(tempParamValue);
				saveParam(mavParamValue);
			}
			break;

		case msg_param_set.MAVLINK_MSG_ID_PARAM_SET:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT:
			msg_gps_raw_int mavGps;
			if (mavMessage2 instanceof msg_gps_raw_int) 
			{
				mavGps = (msg_gps_raw_int) mavMessage2;
				Map<String, Object> tempMavGps = Maps.newHashMap();
				String tempGps = "[" + mavGps.time_usec + "] ," + "LATITUDE : "
						+ mavGps.lat / 10000000.0 + "degrees , "
						+ "LONGITUDE : " + mavGps.lon / 10000000.0
						+ "degrees , " + "ALTITUDE : " + mavGps.alt / 1000.0
						+ "metres , " + "HORIZONTAL DILUTION : "
						+ mavGps.eph / 100.0 + "metres , "
						+ "VERTICAL DILUTION : " + mavGps.epv / 100.0
						+ "metres , " + "VELOCITY : " + mavGps.vel / 100.0
						+ "m/s , " + "COURSE OVER GROUND : " + mavGps.cog
						/ 100.0 + "degrees , " + "FIX TYPE : "
						+ mavGps.fix_type + "D , " + "SATELLITES VISIBLE : "
						+ mavGps.satellites_visible;
				tempMavGps.put("data", "MAVLINK_MSG_ID_GPS_RAW_INT - "
						+ tempGps);
				sendOutputJson(publishers[2], tempMavGps);
				getLog().debug(tempGps);
				
				tempMavGps.clear();
				tempMavGps.put("gps",tempGps);
				sendOutputJson(publishers[9], tempMavGps);
			}
			break;

		case msg_gps_status.MAVLINK_MSG_ID_GPS_STATUS:
			Map<String,	Object> tempMavGpsStatus = Maps.newHashMap();
			tempMavGpsStatus.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavGpsStatus);
			getLog().debug(mavMessage2.toString());
			break;

		case msg_scaled_imu.MAVLINK_MSG_ID_SCALED_IMU:
			msg_scaled_imu mavScaledImu;
			if (mavMessage2 instanceof msg_scaled_imu) 
			{
				mavScaledImu = (msg_scaled_imu) mavMessage2;
				String tempScaledImu = "[" + mavScaledImu.time_boot_ms + "] , "
						+ "ACCELARATION X : " + mavScaledImu.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavScaledImu.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavScaledImu.zacc / 100000.0
						+ "metres/sec2 , " + "OMEGA X : "
						+ mavScaledImu.xgyro / 1000.0 + "rad/s , "
						+ "OMEGA Y : " + mavScaledImu.ygyro / 1000.0
						+ "rad/s , " + "OMEGA Z : "
						+ mavScaledImu.zgyro / 1000.0 + "rad/s , "
						+ "MAGNETIC FIELD X : " + mavScaledImu.xmag / 1000.0
						+ "Tesla , " + "MAGNETIC FIELD Y : "
						+ mavScaledImu.ymag / 1000.0 + "Tesla , "
						+ "MAGNETIC FIELD Z : " + mavScaledImu.zmag / 1000.0
						+ "Tesla";
				Map<String, Object> tempMavScaledImu = Maps.newHashMap();
				tempMavScaledImu.put("data", "MAVLINK_MSG_ID_SCALED_IMU - "
						+ tempScaledImu);
				sendOutputJson(publishers[2], tempMavScaledImu);
				getLog().debug(tempScaledImu);
			}
			break;

		case msg_raw_imu.MAVLINK_MSG_ID_RAW_IMU:
			msg_raw_imu mavRawImu;
			if (mavMessage2 instanceof msg_raw_imu) 
			{
				mavRawImu = (msg_raw_imu) mavMessage2;
				String tempRawImu = "[" + mavRawImu.time_usec + "] , "
						+ "ACCELARATION X : " + mavRawImu.xacc + "raw , "
						+ "ACCELARATION Y : " + mavRawImu.yacc + "raw , "
						+ "ACCELARATION Z : " + mavRawImu.zacc + "raw , "
						+ "OMEGA X : " + mavRawImu.xgyro + "raw , "
						+ "OMEGA Y : " + mavRawImu.ygyro + "raw , "
						+ "OMEGA Z : " + mavRawImu.zgyro + "raw , "
						+ "MAGNETIC FIELD X : " + mavRawImu.xmag + "raw , "
						+ "MAGNETIC FIELD Y : " + mavRawImu.ymag + "raw , "
						+ "MAGNETIC FIELD Z : " + mavRawImu.zmag + "raw";
				Map<String, Object> tempMavRawImu = Maps.newHashMap();
				tempMavRawImu.put("data", "MAVLINK_MSG_ID_RAW_IMU  - "
						+ tempRawImu);
				sendOutputJson(publishers[2], tempMavRawImu);
				getLog().debug(tempRawImu);
				
				tempMavRawImu.clear();
				tempMavRawImu.put("imu",tempRawImu);
				sendOutputJson(publishers[10], tempMavRawImu);
			}
			break;

		case msg_raw_pressure.MAVLINK_MSG_ID_RAW_PRESSURE:
			msg_raw_pressure mavRawPressure;
			if (mavMessage2 instanceof msg_raw_pressure) 
			{
				mavRawPressure = (msg_raw_pressure) mavMessage2;
				String tempRawPressure = "[" + mavRawPressure.time_usec
						+ "] , " + "ABSOLUTE PRESSURE : "
						+ mavRawPressure.press_abs + "raw , "
						+ "DIFFERENTIAL PRESSURE 1 : "
						+ mavRawPressure.press_diff1 + "raw , "
						+ "DIFFERENTIAL PRESSURE 2 : "
						+ mavRawPressure.press_diff2 + "raw , "
						+ "TEMPERATURE : " + mavRawPressure.temperature
						+ "raw ";
				Map<String, Object> tempMavRawPressure = Maps.newHashMap();
				tempMavRawPressure.put("data", "MAVLINK_MSG_ID_RAW_PRESSURE - "
						+ tempRawPressure);
				sendOutputJson(publishers[2], tempMavRawPressure);
				getLog().debug(tempRawPressure);
			}
			break;

		case msg_scaled_pressure.MAVLINK_MSG_ID_SCALED_PRESSURE:
			msg_scaled_pressure  mavScaledPressure;
			if (mavMessage2 instanceof msg_scaled_pressure) 
			{
				mavScaledPressure = (msg_scaled_pressure) mavMessage2;
				String tempScaledPressure = "["
						+ mavScaledPressure.time_boot_ms + "] , "
						+ "ABSOLUTE PRESSURE : " + mavScaledPressure.press_abs
						* 100.0 + "Pascal , " + "DIFFERENTIAL PRESSURE 1 : "
						+ mavScaledPressure.press_diff * 100 + "Pascal , "
						+ "TEMPERATURE : " + mavScaledPressure.temperature
						/ 100.0 + "degree Cesius ";
				Map<String, Object> tempMavScaledPressure = Maps.newHashMap();
				tempMavScaledPressure.put("data",
						"MAVLINK_MSG_ID_SCALED_PRESSURE - "
								+ tempScaledPressure);
				sendOutputJson(publishers[2], tempMavScaledPressure);
				getLog().debug(tempScaledPressure);
				
				tempMavScaledPressure.clear();
				tempMavScaledPressure.put("pressure", tempScaledPressure);
				sendOutputJson(publishers[11], tempMavScaledPressure);
			}
			break;

		case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
			msg_attitude  mavAttitude;
			if (mavMessage2 instanceof msg_attitude) 
			{
				mavAttitude = (msg_attitude) mavMessage2 ;
				String tempAttitude = "[" + mavAttitude.time_boot_ms + "] , "
						+ "ROLL : " + mavAttitude.roll + "rad , " + "PITCH : "
						+ mavAttitude.pitch + "rad , " + "YAW : "
						+ mavAttitude.yaw + "rad , " + "ROLL SPEED : "
						+ mavAttitude.rollspeed + "rad/s , " + "PITCH SPEED : "
						+ mavAttitude.pitchspeed + "rad/s , " + "YAW SPEED : "
						+ mavAttitude.yawspeed + "rad/s";
				Map<String, Object> tempMavAttitude = Maps.newHashMap();
				tempMavAttitude.put("data", "MAVLINK_MSG_ID_ATTITUDE - "
						+ tempAttitude);
				sendOutputJson(publishers[2], tempMavAttitude);
				getLog().debug(tempAttitude);
				
				tempMavAttitude.clear();
				tempMavAttitude.put("attitude", tempAttitude);
				sendOutputJson(publishers[6], tempMavAttitude);
			}
			break;

		case msg_attitude_quaternion.MAVLINK_MSG_ID_ATTITUDE_QUATERNION:
			msg_attitude_quaternion  mavAttitudeQuaternion;
			if (mavMessage2 instanceof msg_attitude_quaternion) 
			{
				mavAttitudeQuaternion = (msg_attitude_quaternion) mavMessage2 ;
				String tempAttitudeQuaternion = "["
						+ mavAttitudeQuaternion.time_boot_ms + "] , "
						+ "QUATERNION COMPONENT 1 : "
						+ mavAttitudeQuaternion.q1 + " , "
						+ "QUATERNION COMPONENT 2 : "
						+ mavAttitudeQuaternion.q2 + " , "
						+ "QUATERNION COMPONENT 3 : "
						+ mavAttitudeQuaternion.q3 + " , "
						+ "QUATERNION COMPONENT 4 : "
						+ mavAttitudeQuaternion.q4 + " , " + "ROLL SPEED : "
						+ mavAttitudeQuaternion.rollspeed + "rad/s , "
						+ "PITCH SPEED : " + mavAttitudeQuaternion.pitchspeed
						+ "rad/s , " + "YAW SPEED : "
						+ mavAttitudeQuaternion.yawspeed + "rad/s";
				Map<String, Object> tempMavAttitudeQuaternion = Maps
						.newHashMap();
				tempMavAttitudeQuaternion.put("data",
						"MAVLINK_MSG_ID_ATTITUDE_QUATERNION - "
								+ tempAttitudeQuaternion);
				sendOutputJson(publishers[2], tempMavAttitudeQuaternion);
				getLog().debug(tempAttitudeQuaternion);
			}
			break;

		case msg_local_position_ned.MAVLINK_MSG_ID_LOCAL_POSITION_NED:
			msg_local_position_ned mavLocalPosition;
			if (mavMessage2 instanceof msg_local_position_ned) 
			{
				mavLocalPosition = (msg_local_position_ned) mavMessage2;
				String tempLocalPosition = "[" + mavLocalPosition.time_boot_ms
						+ "]," + "X : " + mavLocalPosition.x + "metres , "
						+ "Y : " + mavLocalPosition.y + "metres , " + "Z : "
						+ mavLocalPosition.z + "metres , " + "VELOCITY X : "
						+ mavLocalPosition.vx + "m/s , " + "VELOCITY Y : "
						+ mavLocalPosition.vy + "m/s , " + "VELOCITY Z : "
						+ mavLocalPosition.vz + "m/s ";
				Map<String, Object> tempMavLocalPosition = Maps.newHashMap();
				tempMavLocalPosition.put("data",
						"MAVLINK_MSG_ID_LOCAL_POSITION_NED- "
								+ tempLocalPosition);
				sendOutputJson(publishers[2], tempMavLocalPosition);
				getLog().debug(tempLocalPosition);
				
				tempMavLocalPosition.clear();
				tempMavLocalPosition.put("local_position",tempLocalPosition);
				sendOutputJson(publishers[13], tempMavLocalPosition);
			}
			break;

		case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
			msg_global_position_int mavGlobalPosition;
			if (mavMessage2 instanceof msg_global_position_int) 
			{
				mavGlobalPosition = (msg_global_position_int) mavMessage2;
				String tempGlobalPosition = "["
						+ mavGlobalPosition.time_boot_ms + "]," + "LATITUDE : "
						+ mavGlobalPosition.lat / 10000000.0 + "degrees , "
						+ "LONGITUDE : " + mavGlobalPosition.lon / 10000000.0
						+ "degrees , " + "ALTITUDE : "
						+ mavGlobalPosition.alt / 1000.0 + "metres , "
						+ "RELATIVE ALTITUDE : "
						+ mavGlobalPosition.relative_alt / 1000.0 + "metres , "
						+ "VELOCITY X : " + mavGlobalPosition.vx / 100.0
						+ "m/s , " + "VELOCITY Y : "
						+ mavGlobalPosition.vy / 100.0 + "m/s , "
						+ "VELOCITY Z : " + mavGlobalPosition.vz / 100.0
						+ "m/s , " + "HEADING : " + mavGlobalPosition.hdg
						/ 100.0 + "degrees";
				Map<String, Object> tempMavGlobalPosition = Maps.newHashMap();
				tempMavGlobalPosition.put("data",
						"MAVLINK_MSG_ID_GLOBAL_POSITION_INT - "
								+ tempGlobalPosition);
				sendOutputJson(publishers[2], tempMavGlobalPosition);
				getLog().debug(tempGlobalPosition);
				
				tempMavGlobalPosition.clear();
				tempMavGlobalPosition.put("global_position", tempGlobalPosition);
				sendOutputJson(publishers[12], tempMavGlobalPosition);
			}
			break;

		case msg_rc_channels_scaled.MAVLINK_MSG_ID_RC_CHANNELS_SCALED:
			msg_rc_channels_scaled mavRcChannelScaled;
			if (mavMessage2 instanceof msg_rc_channels_scaled) 
			{
				mavRcChannelScaled = (msg_rc_channels_scaled) mavMessage2;
				String tempRcChannelScaled = "["
						+ mavRcChannelScaled.time_boot_ms + "],"
						+ "CHANNEL 1 : " + mavRcChannelScaled.chan1_scaled
						+ " , " + "CHANNEL 2 : "
						+ mavRcChannelScaled.chan2_scaled + " , "
						+ "CHANNEL 3 : " + mavRcChannelScaled.chan3_scaled
						+ " , " + "CHANNEL 4 : "
						+ mavRcChannelScaled.chan4_scaled + " , "
						+ "CHANNEL 5 : " + mavRcChannelScaled.chan5_scaled
						+ " , " + "CHANNEL 6 : "
						+ mavRcChannelScaled.chan6_scaled + " , "
						+ "CHANNEL 7 : " + mavRcChannelScaled.chan7_scaled
						+ " , " + "CHANNEL 8 : "
						+ mavRcChannelScaled.chan8_scaled + " , " + "PORT : "
						+ mavRcChannelScaled.port + " , "
						+ "SIGNAL STRENGTH : " + mavRcChannelScaled.rssi;

				/**
				 * RC channels value scaled, (-100%) -10000, (0%) 0, (100%)
				 * 10000, (invalid) INT16_MAX.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavRcChannelScaled = Maps.newHashMap();
				tempMavRcChannelScaled.put("data",
						"MAVLINK_MSG_ID_RC_CHANNELS_SCALED - "
								+ tempRcChannelScaled);
				sendOutputJson(publishers[2], tempMavRcChannelScaled);
				getLog().debug(tempRcChannelScaled);
			}
			break;

		case msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW:
			msg_rc_channels_raw mavRcChannelRaw;
			if (mavMessage2 instanceof msg_rc_channels_raw) 
			{
				mavRcChannelRaw = (msg_rc_channels_raw) mavMessage2;
				String tempRcChannelRaw = "[" + mavRcChannelRaw.time_boot_ms
						+ "] ," + "CHANNEL 1 : " + mavRcChannelRaw.chan1_raw
						+ " , " + "CHANNEL 2 : " + mavRcChannelRaw.chan2_raw
						+ " , " + "CHANNEL 3 : " + mavRcChannelRaw.chan3_raw
						+ " , " + "CHANNEL 4 : " + mavRcChannelRaw.chan4_raw
						+ " , " + "CHANNEL 5 : " + mavRcChannelRaw.chan5_raw
						+ " , " + "CHANNEL 6 : " + mavRcChannelRaw.chan6_raw
						+ " , " + "CHANNEL 7 : " + mavRcChannelRaw.chan7_raw
						+ " , " + "CHANNEL 8 : " + mavRcChannelRaw.chan8_raw
						+ " , " + "PORT : " + mavRcChannelRaw.port + " , "
						+ "SIGNAL STRENGTH : " + mavRcChannelRaw.rssi;

				/**
				 * RC channels value in microseconds. A value of UINT16_MAX
				 * implies the channel is unused.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavRcChannelRaw = Maps.newHashMap();
				tempMavRcChannelRaw.put("data",
						"MAVLINK_MSG_ID_RC_CHANNELS_RAW - " + tempRcChannelRaw);
				sendOutputJson(publishers[2], tempMavRcChannelRaw);
				getLog().debug(tempRcChannelRaw);
				
				tempMavRcChannelRaw.clear();
				tempMavRcChannelRaw.put("rc_raw", tempRcChannelRaw);
				sendOutputJson(publishers[15], tempMavRcChannelRaw);
			}
			break;

		case msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
			msg_servo_output_raw mavServoOutputRaw;
			if (mavMessage2 instanceof msg_servo_output_raw) 
			{
				mavServoOutputRaw = (msg_servo_output_raw) mavMessage2;
				String tempServoOutputRaw = "[" + mavServoOutputRaw.time_usec
						+ "] ," + "MOTOR 1 : " + mavServoOutputRaw.servo1_raw
						+ " , " + "MOTOR 2 : " + mavServoOutputRaw.servo2_raw
						+ " , " + "MOTOR 3 : " + mavServoOutputRaw.servo3_raw
						+ " , " + "MOTOR 4 : " + mavServoOutputRaw.servo4_raw
						+ " , " + "MOTOR 5 : " + mavServoOutputRaw.servo5_raw
						+ " , " + "MOTOR 6 : " + mavServoOutputRaw.servo6_raw
						+ " , " + "MOTOR 7 : " + mavServoOutputRaw.servo7_raw
						+ " , " + "MOTOR 8 : " + mavServoOutputRaw.servo8_raw
						+ " , " + "PORT : " + mavServoOutputRaw.port;
				Map<String, Object> tempMavServoOutputRaw = Maps.newHashMap();
				tempMavServoOutputRaw.put("data",
						"MAVLINK_MSG_ID_SERVO_OUTPUT_RAW - "
								+ tempServoOutputRaw);
				sendOutputJson(publishers[2], tempMavServoOutputRaw);
				getLog().debug(tempServoOutputRaw);
				
				tempMavServoOutputRaw.clear();
				tempMavServoOutputRaw.put("motor", tempServoOutputRaw);
				sendOutputJson(publishers[14], tempMavServoOutputRaw);
			}
			break;

		case msg_mission_request_partial_list.MAVLINK_MSG_ID_MISSION_REQUEST_PARTIAL_LIST:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_mission_write_partial_list.MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM:

			/*
			 * Format
			 * QGC WPL <VERSION> 
			 * <INDEX> <CURRENT WP> <COORD FRAME><COMMAND> <PARAM1> <PARAM2> <PARAM3> <PARAM4><PARAM5/X/LONGITUDE> <PARAM6/Y/LATITUDE> <PARAM7/Z/ALTITUDE><AUTOCONTINUE> 
			 * 
			 * Example
			 * QGC WPL 110 
			 * 0 1 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
			 * 1 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
			 * 2 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1
			 */
			msg_mission_item mavMissionItem;
			if (mavMessage2 instanceof msg_mission_item) 
			{
				mavMissionItem = (msg_mission_item) mavMessage2;
				String tempMissionItem = "INDEX :"
						+ Short.toString(mavMissionItem.seq) + " , "
						+ "CURRENT WP : " + mavMissionItem.current + " , "
						+ "COORDINATE FRAME : " + mavMissionItem.frame + " , "
						+ "COMMAND : " + mavMissionItem.command + " , "
						+ "PARAM 1 : " + mavMissionItem.param1 + " , "
						+ "PARAM 2 : " + mavMissionItem.param2 + " , "
						+ "PARAM 3 : " + mavMissionItem.param3 + " , "
						+ "PARAM 4 : " + mavMissionItem.param4 + " , " + "X : "
						+ mavMissionItem.x + " , " + "Y : " + mavMissionItem.y
						+ " , " + "Z : " + mavMissionItem.z + " , "
						+ "AUTOCONTINUE : " + mavMissionItem.autocontinue
						+ " , " + "TARGET SYSTEM : "
						+ mavMissionItem.target_system + " , "
						+ "TARGET COMPONENT : "
						+ mavMissionItem.target_component;
				Map<String, Object> tempMapMissionItem = Maps.newHashMap();
				tempMapMissionItem.put("mission",
						"MAVLINK_MSG_ID_MISSION_ITEM - " + tempMissionItem);
				sendOutputJson(publishers[2], tempMapMissionItem);
				getLog().debug(tempMissionItem);
				
				updateReadWPList(mavMissionItem);
			}
			break;

		case msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST:
			/*
			 * This mavlink message is sent on receipt of waypoint count and 
			 * when asking for the next waypoint 
			 */
			msg_mission_request mavMissionRequest;
			if (mavMessage2 instanceof msg_mission_request) 
			{
				mavMissionRequest = (msg_mission_request) mavMessage2;
				String tempMissionRequest = "MISSION_REQUEST-"
						+ Short.toString(mavMissionRequest.seq);
				
				Map<String, Object> tempMapMissionRequest = Maps.newHashMap();
				tempMapMissionRequest.put("mission",
						"MAVLINK_MSG_ID_MISSION_REQUEST - "
								+ tempMissionRequest);
				sendOutputJson(publishers[2], tempMapMissionRequest);
				getLog().debug(tempMissionRequest);
				readMissionFile(mavMissionRequest.seq);
			}
			break;

		case msg_mission_set_current.MAVLINK_MSG_ID_MISSION_SET_CURRENT:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT:
			/**
			 * Message that announces the sequence number of the current active
			 * mission item. The MAV will fly towards this mission item.
			 */
			msg_mission_current mavMissionCurrent;
			if (mavMessage2 instanceof msg_mission_current) 
			{
				mavMissionCurrent = (msg_mission_current) mavMessage2;
				String tempStringCurrent = "CURRENT SEQUENCE : "
						+ Short.toString(mavMissionCurrent.seq);
				Map<String, Object> tempMapMissionCurrent = Maps.newHashMap();
				tempMapMissionCurrent
						.put("mission", "MAVLINK_MSG_ID_MISSION_CURRENT - "
								+ tempStringCurrent);
				sendOutputJson(publishers[2], tempMapMissionCurrent);
				getLog().debug(mavMissionCurrent);
				missionCurrentSeq = mavMissionCurrent.seq;
				
				tempMapMissionCurrent.clear();
				tempMapMissionCurrent.put("mission_seq", tempStringCurrent);
				sendOutputJson(publishers[16], tempMapMissionCurrent);
			}
			break;

		case msg_mission_request_list.MAVLINK_MSG_ID_MISSION_REQUEST_LIST:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT:
			msg_mission_count mavMissionCount;
			if (mavMessage2 instanceof msg_mission_count) 
			{
				mavMissionCount = (msg_mission_count) mavMessage2;
				String tempStringCount = "WAYPOINT COUNT : "
						+ Short.toString(mavMissionCount.count) + " , "
						+ "TARGET SYSTEM : " + mavMissionCount.target_system
						+ " , " + "TARGET COMPONENT :"
						+ mavMissionCount.target_component;
				Map<String, Object> tempMapMissionCount = Maps.newHashMap();
				tempMapMissionCount.put("mission",
						"MAVLINK_MSG_ID_MISSION_COUNT - " + tempStringCount);
				sendOutputJson(publishers[2], tempMapMissionCount);
				getLog().debug(tempMapMissionCount);
				
				setMissionCount(mavMissionCount.count);
				
			}
			break;

		case msg_mission_clear_all.MAVLINK_MSG_ID_MISSION_CLEAR_ALL:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_mission_item_reached.MAVLINK_MSG_ID_MISSION_ITEM_REACHED:
			/*
			 * A certain mission item has been reached. The system will either
			 * hold this position (or circle on the orbit) or (if the
			 * autocontinue on the WP was set) continue to the next MISSION.
			 */
			msg_mission_item_reached mavMissionItemReached;
			if (mavMessage2 instanceof msg_mission_item_reached) 
			{
				mavMissionItemReached = (msg_mission_item_reached) mavMessage2;
				String tempStringItemReached = "ITEM REACHED : "
						+ Short.toString(mavMissionItemReached.seq);
				Map<String, Object> tempMapMissionItemReached = Maps
						.newHashMap();
				tempMapMissionItemReached.put("mission",
						"MAVLINK_MSG_ID_MISSION_ITEM_REACHED - "
								+ tempStringItemReached);
				sendOutputJson(publishers[2], tempMapMissionItemReached);
				getLog().debug(tempMapMissionItemReached);
			}
			break;

		case msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK:
			msg_mission_ack mavMissionAck;
			if (mavMessage2 instanceof msg_mission_ack) 
			{
				Map<String, Object> tempMapMissionAck = Maps.newHashMap();
				mavMissionAck = (msg_mission_ack) mavMessage2;
				tempMapMissionAck.put("mission",mavMissionAck.toString());
				sendOutputJson(publishers[2], tempMapMissionAck);
				getLog().debug(mavMissionAck.toString());
				
				tempMapMissionAck.clear();
				if (isMissionCleared) 
				{
					isMissionCleared = false;
					getLog().debug(
							"CLEAR MISSION RESULT : " + mavMissionAck.type);
				} 
				else 
				{
					sendMissionAck = mavMissionAck.type;
					switch (mavMissionAck.type) 
					{
					case MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED:
						/*
						 * mission accepted OK |
						 */
						tempMapMissionAck.put("mission", "MISSION_ACCEPTED");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_ERROR:
						/*
						 * generic error / not accepting mission commands at all
						 * right now |
						 */
						tempMapMissionAck.put("mission", "MISSION_ERROR");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_UNSUPPORTED_FRAME:
						/*
						 * coordinate frame is not supported |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_UNSUPPORTED_FRAME");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_UNSUPPORTED:
						/*
						 * command is not supported |
						 */
						tempMapMissionAck.put("mission", "MISSION_UNSUPPORTED");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_NO_SPACE:
						/*
						 * mission item exceeds storage space |
						 */
						tempMapMissionAck.put("mission", "MISSION_NO_SPACE");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID:
						/*
						 * one of the parameters has an invalid value |
						 */
						tempMapMissionAck.put("mission", "MISSION_INVALID");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM1:
						/*
						 * param1 has an invalid value |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_PARAM1");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM2:
						/*
						 * param2 has an invalid value |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_PARAM2");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM3:
						/*
						 * param3 has an invalid value |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_PARAM3");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM4:
						/*
						 * param4 has an invalid value |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_PARAM4");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM5_X:
						/*
						 * x/param5 has an invalid value |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_PARAM5_X");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM6_Y:
						/*
						 * y/param6 has an invalid value |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_PARAM6_Y");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM7:
						/*
						 * param7 has an invalid value |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_PARAM7");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_INVALID_SEQUENCE:
						/*
						 * received waypoint out of sequence |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_INVALID_SEQUENCE");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					case MAV_MISSION_RESULT.MAV_MISSION_RESULT_ENUM_END:
						/*
						 * not accepting any mission commands from this
						 * communication partner |
						 */
						tempMapMissionAck.put("mission",
								"MISSION_RESULT_ENUM_END");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;

					default:
						tempMapMissionAck.put("mission",
								"MISSION_RESULT_UNKNOWN");
						sendOutputJson(publishers[1], tempMapMissionAck);
						break;
					}
				}
			}
			break;

		case msg_set_gps_global_origin.MAVLINK_MSG_ID_SET_GPS_GLOBAL_ORIGIN:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_gps_global_origin.MAVLINK_MSG_ID_GPS_GLOBAL_ORIGIN:
			msg_gps_global_origin mavGpsGlobalOrigin;
			if (mavMessage2 instanceof msg_gps_global_origin) 
			{
				mavGpsGlobalOrigin = (msg_gps_global_origin) mavMessage2 ;
				Map<String, Object> tempMavGpsGlobalOrigin = Maps.newHashMap();
				String tempGpsGlobalOrigin = "LATITUDE : "
						+ mavGpsGlobalOrigin.latitude / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavGpsGlobalOrigin.longitude / 10000000.0
						+ "degrees , " + "ALTITUDE : "
						+ mavGpsGlobalOrigin.altitude / 1000.0 + "metres";
				tempMavGpsGlobalOrigin.put("data",
						"MAVLINK_MSG_ID_GPS_GLOBAL_ORIGIN - "
								+ tempGpsGlobalOrigin);
				sendOutputJson(publishers[2], tempMavGpsGlobalOrigin);
				getLog().debug(tempGpsGlobalOrigin);
				saveGlobalGpsOrigin(mavGpsGlobalOrigin);
			}
			break;

		case msg_param_map_rc.MAVLINK_MSG_ID_PARAM_MAP_RC:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_safety_set_allowed_area.MAVLINK_MSG_ID_SAFETY_SET_ALLOWED_AREA:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_safety_allowed_area.MAVLINK_MSG_ID_SAFETY_ALLOWED_AREA:
			msg_safety_allowed_area mavSafetyAllowedArea;
			if (mavMessage2 instanceof msg_safety_allowed_area) 
			{
				mavSafetyAllowedArea = (msg_safety_allowed_area) mavMessage2 ;
				String tempSafetyAllowedArea = null;
				switch (mavSafetyAllowedArea.frame) {
				case MAV_FRAME.MAV_FRAME_GLOBAL:
					tempSafetyAllowedArea = "X POSITION 1 : "
							+ mavSafetyAllowedArea.p1x + "degrees , "
							+ "Y POSITION 1 : " + mavSafetyAllowedArea.p1y
							+ "degrees , " + "ALTITUDE 1 : "
							+ mavSafetyAllowedArea.p1z + "metres , "
							+ "X POSITION 2 : " + mavSafetyAllowedArea.p2x
							+ "degrees , " + "Y POSITION 2 : "
							+ mavSafetyAllowedArea.p2y + "degrees , "
							+ "ALTITUDE 2 : " + mavSafetyAllowedArea.p2z
							+ "metres";
					break;
					
				case MAV_FRAME.MAV_FRAME_LOCAL_NED:
					tempSafetyAllowedArea = "X POSITION 1 : "
							+ mavSafetyAllowedArea.p1x + "metres , "
							+ "Y POSITION 1 : " + mavSafetyAllowedArea.p1y
							+ "metres , " + "ALTITUDE 1 : "
							+ mavSafetyAllowedArea.p1z + "metres , "
							+ "X POSITION 2 : " + mavSafetyAllowedArea.p2x
							+ "metres , " + "Y POSITION 2 : "
							+ mavSafetyAllowedArea.p2y + "metres , "
							+ "ALTITUDE 2 : " + mavSafetyAllowedArea.p2z
							+ "metres";
					break;

				case MAV_FRAME.MAV_FRAME_LOCAL_ENU:
					tempSafetyAllowedArea = "X POSITION 1 : "
							+ mavSafetyAllowedArea.p1x + "metres , "
							+ "Y POSITION 1 : " + mavSafetyAllowedArea.p1y
							+ "metres , " + "ALTITUDE 1 : "
							+ mavSafetyAllowedArea.p1z * (-1.0) + "metres , "
							+ "X POSITION 2 : " + mavSafetyAllowedArea.p2x
							+ "metres , " + "Y POSITION 2 : "
							+ mavSafetyAllowedArea.p2y + "metres , "
							+ "ALTITUDE 2 : " + mavSafetyAllowedArea.p2z
							* (-1.0) + "metres";
					break;
					
				default:
					getLog().error("Bad Coordinate frame type received");
					break;
				}
				if (tempSafetyAllowedArea != null) 
				{
					Map<String, Object> tempMavSafetyAllowedArea = Maps
							.newHashMap();
					tempMavSafetyAllowedArea.put("data",
							"MAVLINK_MSG_ID_SAFETY_ALLOWED_AREA - "
									+ tempSafetyAllowedArea);
					sendOutputJson(publishers[2], tempMavSafetyAllowedArea);
					getLog().debug(tempSafetyAllowedArea);
					saveAllowedArea(mavSafetyAllowedArea);
				}
			}
			break;

		case msg_attitude_quaternion_cov.MAVLINK_MSG_ID_ATTITUDE_QUATERNION_COV:
			msg_attitude_quaternion_cov  mavAttitudeQuaternionCov;
			if (mavMessage2 instanceof msg_attitude_quaternion_cov)
			{
				mavAttitudeQuaternionCov = (msg_attitude_quaternion_cov) mavMessage2;
				String tempAttitudeQuaternionCov = "["
						+ mavAttitudeQuaternionCov.time_boot_ms + "] , "
						+ "QUATERNION COMPONENT 1 : "
						+ mavAttitudeQuaternionCov.q[1] + " , "
						+ "QUATERNION COMPONENT 2 : "
						+ mavAttitudeQuaternionCov.q[2] + " , "
						+ "QUATERNION COMPONENT 3 : "
						+ mavAttitudeQuaternionCov.q[3] + " , "
						+ "QUATERNION COMPONENT 4 : "
						+ mavAttitudeQuaternionCov.q[4] + " , "
						+ "ROLL SPEED : " + mavAttitudeQuaternionCov.rollspeed
						+ "rad/s , " + "PITCH SPEED : "
						+ mavAttitudeQuaternionCov.pitchspeed + "rad/s , "
						+ "YAW SPEED : " + mavAttitudeQuaternionCov.yawspeed
						+ "rad/s , " + "COVARIANCE MATRIX : "
						+ Arrays.toString(mavAttitudeQuaternionCov.covariance);
				Map<String, Object> tempMavAttitudeQuaternionCov = Maps
						.newHashMap();
				tempMavAttitudeQuaternionCov.put("data",
						"MAVLINK_MSG_ID_ATTITUDE_QUATERNION_COV - "
								+ tempAttitudeQuaternionCov);
				sendOutputJson(publishers[2], tempMavAttitudeQuaternionCov);
				getLog().debug(tempAttitudeQuaternionCov);
			}
			break;

		case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:
			/*
			 * Outputs of the APM navigation controller. The primary use of this
			 * message is to check the response and signs of the controller
			 * before actual flight and to assist with tuning controller
			 * parameters.
			 */
			msg_nav_controller_output  mavNavControllerOutput;
			if (mavMessage2 instanceof msg_nav_controller_output) 
			{
				mavNavControllerOutput = (msg_nav_controller_output) mavMessage2;
				String tempNavControllerOutput = "CURRENT DESIRED ROLL : "
						+ mavNavControllerOutput.nav_roll + "degrees , "
						+ "CURRENT DESIRED PITCH : "
						+ mavNavControllerOutput.nav_pitch + "degrees , "
						+ "CURRENT DESIRED HEADING : "
						+ mavNavControllerOutput.nav_bearing + "degrees , "
						+ "CURRENT TARGET HEADING  : "
						+ mavNavControllerOutput.target_bearing + "degrees , "
						+ "ALTITUDE ERROR : "
						+ mavNavControllerOutput.alt_error + "m , "
						+ "AIR SPEED ERROR : "
						+ mavNavControllerOutput.aspd_error + "m/s , "
						+ "CROSSTRACK ERROR XY PLANE : "
						+ mavNavControllerOutput.xtrack_error + "m , "
						+ "WAYPOINT DISTANCE : "
						+ mavNavControllerOutput.wp_dist + "m";
				Map<String, Object> tempMavNavControllerOutput = Maps
						.newHashMap();
				tempMavNavControllerOutput.put("data",
						"MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT - "
								+ tempNavControllerOutput);
				sendOutputJson(publishers[2], tempMavNavControllerOutput);
				getLog().debug(tempNavControllerOutput);
				
				tempMavNavControllerOutput.clear();
				tempMavNavControllerOutput.put("nav_output", tempNavControllerOutput);
				sendOutputJson(publishers[17], tempMavNavControllerOutput);
			}
			break;

		case msg_global_position_int_cov.MAVLINK_MSG_ID_GLOBAL_POSITION_INT_COV:
			msg_global_position_int_cov mavGlobalPositionIntCov;
			if (mavMessage2 instanceof msg_global_position_int_cov) 
			{
				mavGlobalPositionIntCov = (msg_global_position_int_cov) mavMessage2;
				String tempGlobalPositionIntCov = "["
						+ mavGlobalPositionIntCov.time_boot_ms + "],"
						+ "LATITUDE : " + mavGlobalPositionIntCov.lat
						+ "degrees , " + "LONGITUDE : "
						+ mavGlobalPositionIntCov.lon + "degrees , "
						+ "ALTITUDE : " + mavGlobalPositionIntCov.alt
						+ "metres , " + "RELATIVE ALTITUDE : "
						+ mavGlobalPositionIntCov.relative_alt + "metres , "
						+ "VELOCITY X : " + mavGlobalPositionIntCov.vx
						+ "m/s , " + "VELOCITY Y : "
						+ mavGlobalPositionIntCov.vy + "m/s , "
						+ "VELOCITY Z : " + mavGlobalPositionIntCov.vz
						+ "m/s , " + "COVARIANCE : "
						+ Arrays.toString(mavGlobalPositionIntCov.covariance)
						+ " , " + "TYPE : "
						+ mavGlobalPositionIntCov.estimator_type
						+ "UTC TIME : " + mavGlobalPositionIntCov.time_utc;
				/**
				 * Covariance matrix (first six entries are the first ROW, next
				 * six entries are the second row, etc.)
				 */
				Map<String, Object> tempMavGlobalPositionIntCov = Maps
						.newHashMap();
				tempMavGlobalPositionIntCov.put("data",
						"MAVLINK_MSG_ID_GLOBAL_POSITION_INT_COV - "
								+ tempGlobalPositionIntCov);
				sendOutputJson(publishers[2], tempMavGlobalPositionIntCov);
				getLog().debug(tempGlobalPositionIntCov);
			}
			break;

		case msg_local_position_ned_cov.MAVLINK_MSG_ID_LOCAL_POSITION_NED_COV:
			msg_local_position_ned_cov mavLocalPositionCov;
			if (mavMessage2 instanceof msg_local_position_ned_cov) 
			{
				mavLocalPositionCov = (msg_local_position_ned_cov) mavMessage2;
				String tempLocalPositionCov = "["
						+ mavLocalPositionCov.time_boot_ms + "]," + "X : "
						+ mavLocalPositionCov.x + "metres , " + "Y : "
						+ mavLocalPositionCov.y + "metres , " + "Z : "
						+ mavLocalPositionCov.z + "metres , " + "VELOCITY X : "
						+ mavLocalPositionCov.vx + "m/s , " + "VELOCITY Y : "
						+ mavLocalPositionCov.vy + "m/s , " + "VELOCITY Z : "
						+ mavLocalPositionCov.vz + "m/s , "
						+ "ACCELARATION X : " + mavLocalPositionCov.ax
						+ "m/s2 , " + "ACCELARATION Y : "
						+ mavLocalPositionCov.ay + "m/s2 , "
						+ "ACCELARATION Z : " + mavLocalPositionCov.az
						+ "m/s2 , " + "COVARIANCE : "
						+ Arrays.toString(mavLocalPositionCov.covariance)
						+ " , " + "TYPE : "
						+ mavLocalPositionCov.estimator_type;
				/**
				 * Covariance matrix upper right triangular (first nine entries
				 * are the first ROW, next eight entries are the second row,
				 * etc.)
				 */
				Map<String, Object> tempMavLocalPositionCov = Maps.newHashMap();
				tempMavLocalPositionCov.put("data",
						"MAVLINK_MSG_ID_LOCAL_POSITION_NED_COV - "
								+ tempLocalPositionCov);
				sendOutputJson(publishers[2], tempMavLocalPositionCov);
				getLog().debug(tempLocalPositionCov);
			}
			break;

		case msg_rc_channels.MAVLINK_MSG_ID_RC_CHANNELS:
			msg_rc_channels mavRcChannels;
			if (mavMessage2 instanceof msg_rc_channels) 
			{
				mavRcChannels = (msg_rc_channels) mavMessage2;
				String tempRcChannels = "[" + mavRcChannels.time_boot_ms
						+ "] ," + "CHANNEL 1 : " + mavRcChannels.chan1_raw
						+ " , " + "CHANNEL 2 : " + mavRcChannels.chan2_raw
						+ " , " + "CHANNEL 3 : " + mavRcChannels.chan3_raw
						+ " , " + "CHANNEL 4 : " + mavRcChannels.chan4_raw
						+ " , " + "CHANNEL 5 : " + mavRcChannels.chan5_raw
						+ " , " + "CHANNEL 6 : " + mavRcChannels.chan6_raw
						+ " , " + "CHANNEL 7 : " + mavRcChannels.chan7_raw
						+ " , " + "CHANNEL 8 : " + mavRcChannels.chan8_raw
						+ " , " + "CHANNEL 9 : " + mavRcChannels.chan9_raw
						+ " , " + "CHANNEL 10 : " + mavRcChannels.chan10_raw
						+ " , " + "CHANNEL 12 : " + mavRcChannels.chan12_raw
						+ " , " + "CHANNEL 13 : " + mavRcChannels.chan13_raw
						+ " , " + "CHANNEL 14 : " + mavRcChannels.chan14_raw
						+ " , " + "CHANNEL 15 : " + mavRcChannels.chan15_raw
						+ " , " + "CHANNEL 16 : " + mavRcChannels.chan16_raw
						+ " , " + "CHANNEL 17 : " + mavRcChannels.chan17_raw
						+ " , " + "CHANNEL 18 : " + mavRcChannels.chan18_raw
						+ " , " + "CHANNEL COUNT : "
						+ mavRcChannels.chancount + " , "
						+ "SIGNAL STRENGTH : " + mavRcChannels.rssi;

				/**
				 * RC channels value in microseconds. A value of UINT16_MAX
				 * implies the channel is unused.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavRcChannels = Maps.newHashMap();
				tempMavRcChannels.put("data", "MAVLINK_MSG_ID_RC_CHANNELS - "
						+ tempRcChannels);
				sendOutputJson(publishers[2], tempMavRcChannels);
				getLog().debug(tempRcChannels);
			}
			break;

		case msg_request_data_stream.MAVLINK_MSG_ID_REQUEST_DATA_STREAM:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_data_stream.MAVLINK_MSG_ID_DATA_STREAM:
			msg_data_stream  mavDataStream;
			if (mavMessage2 instanceof msg_data_stream) 
			{
				mavDataStream = (msg_data_stream) mavMessage2;
				String tempDataStream = "MESSAGE RATE : "
						+ mavDataStream.message_rate + " , " + "STREAM ID : "
						+ mavDataStream.stream_id + " , " + "STREAM STATUS : "
						+ mavDataStream.on_off;
				Map<String, Object> tempMavDataStream = Maps.newHashMap();
				tempMavDataStream.put("data", "MAVLINK_MSG_ID_DATA_STREAM - "
						+ tempDataStream);
				sendOutputJson(publishers[2], tempMavDataStream);
				getLog().debug(tempDataStream);
			}
			break;

		case msg_manual_control.MAVLINK_MSG_ID_MANUAL_CONTROL:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_rc_channels_override.MAVLINK_MSG_ID_RC_CHANNELS_OVERRIDE:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_mission_item_int.MAVLINK_MSG_ID_MISSION_ITEM_INT:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
			/**
			 * Metrics typically displayed on a HUD for fixed wing aircraft
			 */
			msg_vfr_hud  mavVfrHud;
			if (mavMessage2 instanceof msg_vfr_hud) 
			{
				mavVfrHud = (msg_vfr_hud) mavMessage2;
				String tempVfrHud = "AIR SPEED : " + mavVfrHud.airspeed
						+ "m/s , " + "GROUND SPEED : " + mavVfrHud.groundspeed
						+ "m/s , " + "ALTITUDE : " + mavVfrHud.alt + "m , "
						+ "CLIMB : " + mavVfrHud.climb + "m/s , "
						+ "HEADING : " + mavVfrHud.heading + "degrees , "
						+ "THROTTLE : " + mavVfrHud.throttle + "%";
				Map<String, Object> tempMavVfrHud = Maps.newHashMap();
				tempMavVfrHud.put("data", "MAVLINK_MSG_ID_VFR_HUD - "
						+ tempVfrHud);
				sendOutputJson(publishers[2], tempMavVfrHud);
				getLog().debug(tempVfrHud);
				
				tempMavVfrHud.clear();
				tempMavVfrHud.put("hud", tempVfrHud);
				sendOutputJson(publishers[5], tempMavVfrHud);
			}
			break;

		case msg_command_int.MAVLINK_MSG_ID_COMMAND_INT:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_command_long.MAVLINK_MSG_ID_COMMAND_LONG:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK:
			/*
			 * Report status of a command. Includes feedback whether the command
			 * was executed.
			 */
			msg_command_ack  mavCommandAck;
			if (mavMessage2 instanceof msg_command_ack) 
			{
				mavCommandAck = (msg_command_ack) mavMessage2;
				isCommandSent = false;
				String tempCommandAck = "COMMAND : "
						+ getVariableName("MAV_CMD", mavCommandAck.command)
						+ " , " + "RESULT : "
						+ getVariableName("MAV_RESILT", mavCommandAck.result);
				switch (mavCommandAck.result) 
				{
				case MAV_RESULT.MAV_RESULT_ACCEPTED:
					//Send to captain activity
					break;

				case MAV_RESULT.MAV_RESULT_DENIED:
					//Send to captain activity
					break;

				case MAV_RESULT.MAV_RESULT_ENUM_END:
					//Send to captain activity
					break;

				case MAV_RESULT.MAV_RESULT_FAILED:
					//Send to captain activity and retry for limited number of times
					break;

				case MAV_RESULT.MAV_RESULT_TEMPORARILY_REJECTED:
					//Retry from captain activity until it accepts
					break;

				case MAV_RESULT.MAV_RESULT_UNSUPPORTED:
					//Send to captain activity
					break;
				default:
					break;
				}
				Map<String, Object> tempMavCommandAck = Maps.newHashMap();
				tempMavCommandAck.put("data", "MAVLINK_MSG_ID_COMMAND_ACK - "
						+ tempCommandAck);
				sendOutputJson(publishers[2], tempMavCommandAck);
				getLog().debug(tempCommandAck);
			}
			break;

		case msg_manual_setpoint.MAVLINK_MSG_ID_MANUAL_SETPOINT:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_set_attitude_target.MAVLINK_MSG_ID_SET_ATTITUDE_TARGET:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_attitude_target.MAVLINK_MSG_ID_ATTITUDE_TARGET:
			msg_attitude_target  mavAttitudeTarget;
			if (mavMessage2 instanceof msg_attitude_target) 
			{
				mavAttitudeTarget = (msg_attitude_target) mavMessage2;
				String tempAttitudeTarget = "["
						+ mavAttitudeTarget.time_boot_ms + "] , "
						+ "QUATERNION COMPONENT 1 : " + mavAttitudeTarget.q[1]
						+ " , " + "QUATERNION COMPONENT 2 : "
						+ mavAttitudeTarget.q[2] + " , "
						+ "QUATERNION COMPONENT 3 : " + mavAttitudeTarget.q[3]
						+ " , " + "QUATERNION COMPONENT 4 : "
						+ mavAttitudeTarget.q[4] + " , " + "BODY ROLL SPEED : "
						+ mavAttitudeTarget.body_roll_rate + "rad/s , "
						+ "BODY PITCH SPEED : "
						+ mavAttitudeTarget.body_pitch_rate + "rad/s , "
						+ "BODY YAW SPEED : " + mavAttitudeTarget.body_yaw_rate
						+ "rad/s , " + "THRUST : " + mavAttitudeTarget.thrust;
				/**
				 * mavAttitudeTarget.type_mask
				 * Mappings: If any of these bits are set, the corresponding
				 * input should be ignored: bit 1: body roll rate, bit 2: body
				 * pitch rate, bit 3: body yaw rate. bit 4-bit 7: reserved, bit
				 * 8: attitude
				 */
				Map<String, Object> tempMavAttitudeTarget = Maps.newHashMap();
				tempMavAttitudeTarget.put("data",
						"MAVLINK_MSG_ID_ATTITUDE_TARGET - "
								+ tempAttitudeTarget);
				sendOutputJson(publishers[2], tempMavAttitudeTarget);
				getLog().debug(tempAttitudeTarget);
			}
			break;

		case msg_set_position_target_local_ned.MAVLINK_MSG_ID_SET_POSITION_TARGET_LOCAL_NED:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_position_target_local_ned.MAVLINK_MSG_ID_POSITION_TARGET_LOCAL_NED:
			msg_position_target_local_ned mavPositionTargetLocalNed;
			if (mavMessage2 instanceof msg_position_target_local_ned) 
			{
				mavPositionTargetLocalNed = (msg_position_target_local_ned) mavMessage2;
				String tempPositionTargetLocalNed = "["
						+ mavPositionTargetLocalNed.time_boot_ms + "],"
						+ "X : " + mavPositionTargetLocalNed.x + "metres , "
						+ "Y : " + mavPositionTargetLocalNed.y + "metres , "
						+ "Z : " + mavPositionTargetLocalNed.z + "metres , "
						+ "VELOCITY X : " + mavPositionTargetLocalNed.vx
						+ "m/s , " + "VELOCITY Y : "
						+ mavPositionTargetLocalNed.vy + "m/s , "
						+ "VELOCITY Z : " + mavPositionTargetLocalNed.vz
						+ "m/s , " + "ACCELARATION X : "
						+ mavPositionTargetLocalNed.afx + "m/s2 , "
						+ "ACCELARATION Y : " + mavPositionTargetLocalNed.afy
						+ "m/s2 , " + "ACCELARATION Z : "
						+ mavPositionTargetLocalNed.afz + "m/s2 , " + "YAW : "
						+ mavPositionTargetLocalNed.yaw + "rad , "
						+ "YAW RATE : " + mavPositionTargetLocalNed.yaw_rate
						+ "rad/s , " + "TYPE MASK : " + " , "
						+ mavPositionTargetLocalNed.type_mask
						+ "COORDINATE FRAME : "
						+ mavPositionTargetLocalNed.coordinate_frame;
				/**
				 * Coordinate Frame Options Valid options are:
				 * MAV_FRAME_LOCAL_NED = 1, MAV_FRAME_LOCAL_OFFSET_NED = 7,
				 * MAV_FRAME_BODY_NED = 8, MAV_FRAME_BODY_OFFSET_NED = 9
				 */
				Map<String, Object> tempMavPositionTargetLocalNed = Maps
						.newHashMap();
				tempMavPositionTargetLocalNed.put("data",
						"MAVLINK_MSG_ID_POSITION_TARGET_LOCAL_NED - "
								+ tempPositionTargetLocalNed);
				sendOutputJson(publishers[2], tempMavPositionTargetLocalNed);
				getLog().debug(tempPositionTargetLocalNed);
			}
			break;

		case msg_set_position_target_global_int.MAVLINK_MSG_ID_SET_POSITION_TARGET_GLOBAL_INT:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_position_target_global_int.MAVLINK_MSG_ID_POSITION_TARGET_GLOBAL_INT:
			msg_position_target_global_int mavPositionTargetGlobalInt;
			if (mavMessage2 instanceof msg_position_target_global_int) 
			{
				mavPositionTargetGlobalInt = (msg_position_target_global_int) mavMessage2;
				String tempPositionTargetGlobalInt = "["
						+ mavPositionTargetGlobalInt.time_boot_ms + "],"
						+ "LATITUDE : "
						+ mavPositionTargetGlobalInt.lat_int / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavPositionTargetGlobalInt.lon_int / 10000000.0
						+ "degrees , " + "ALTITUDE : "
						+ mavPositionTargetGlobalInt.alt + "metres , "
						+ "VELOCITY X : " + mavPositionTargetGlobalInt.vx
						+ "m/s , " + "VELOCITY Y : "
						+ mavPositionTargetGlobalInt.vy + "m/s , "
						+ "VELOCITY Z : " + mavPositionTargetGlobalInt.vz
						+ "m/s , " + "ACCELARATION X : "
						+ mavPositionTargetGlobalInt.afx + "m/s2 , "
						+ "ACCELARATION Y : " + mavPositionTargetGlobalInt.afy
						+ "m/s2 , " + "ACCELARATION Z : "
						+ mavPositionTargetGlobalInt.afz + "m/s2 , " + "YAW : "
						+ mavPositionTargetGlobalInt.yaw + "rad , "
						+ "YAW RATE : " + mavPositionTargetGlobalInt.yaw_rate
						+ "rad/s , " + "TYPE MASK : " + " , "
						+ mavPositionTargetGlobalInt.type_mask
						+ "COORDINATE FRAME : "
						+ mavPositionTargetGlobalInt.coordinate_frame;
				/**
				 * Valid options are: MAV_FRAME_GLOBAL_INT = 5,
				 * MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6,
				 * MAV_FRAME_GLOBAL_TERRAIN_ALT_INT = 11
				 */
				Map<String, Object> tempMavPositionTargetGlobalInt = Maps
						.newHashMap();
				tempMavPositionTargetGlobalInt.put("data",
						"MAVLINK_MSG_ID_POSITION_TARGET_GLOBAL_INT - "
								+ tempPositionTargetGlobalInt);
				sendOutputJson(publishers[2], tempMavPositionTargetGlobalInt);
				getLog().debug(tempPositionTargetGlobalInt);
			}
			break;

		case msg_local_position_ned_system_global_offset.MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET:
			msg_local_position_ned_system_global_offset mavOffsetPositionLocalGlobal;
			if (mavMessage2 instanceof msg_local_position_ned_system_global_offset) 
			{
				mavOffsetPositionLocalGlobal = (msg_local_position_ned_system_global_offset) mavMessage2;
				String tempOffsetPositionLocalGlobal = "["
						+ mavOffsetPositionLocalGlobal.time_boot_ms + "],"
						+ "OFFSET X : " + mavOffsetPositionLocalGlobal.x
						+ "metres , " + "OFFSET Y : "
						+ mavOffsetPositionLocalGlobal.y + "metres , "
						+ "OFFSET Z : " + mavOffsetPositionLocalGlobal.z
						+ "metres , " + "OFFSET ROLL : "
						+ mavOffsetPositionLocalGlobal.roll + "rad , "
						+ "OFFSET PITCH : "
						+ mavOffsetPositionLocalGlobal.pitch + "rad , "
						+ "OFFSET YAW : " + mavOffsetPositionLocalGlobal.yaw
						+ "rad ";
				Map<String, Object> tempMavOffsetPositionLocalGlobal = Maps
						.newHashMap();
				tempMavOffsetPositionLocalGlobal.put("data",
						"MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET - "
								+ tempOffsetPositionLocalGlobal);
				sendOutputJson(publishers[2], tempMavOffsetPositionLocalGlobal);
				getLog().debug(tempOffsetPositionLocalGlobal);
			}
			break;

		case msg_hil_state.MAVLINK_MSG_ID_HIL_STATE:
			msg_hil_state  mavHilState;
			if (mavMessage2 instanceof msg_hil_state) 
			{
				mavHilState = (msg_hil_state) mavMessage2;
				String tempHilState = "[" + mavHilState.time_usec + "] , "
						+ "ROLL : " + mavHilState.roll + "rad , " + "PITCH : "
						+ mavHilState.pitch + "rad , " + "YAW : "
						+ mavHilState.yaw + "rad , " + "ROLL SPEED : "
						+ mavHilState.rollspeed + "rad/s , " + "PITCH SPEED : "
						+ mavHilState.pitchspeed + "rad/s , " + "YAW SPEED : "
						+ mavHilState.yawspeed + "LATITUDE : "
						+ mavHilState.lat / 10000000.0 + "degrees , "
						+ "LONGITUDE : " + mavHilState.lon / 10000000.0
						+ "degrees , " + "ALTITUDE : " + mavHilState.alt
						/ 1000.0 + "metres , " + "VELOCITY X : "
						+ mavHilState.vx / 100.0 + "m/s , " + "VELOCITY Y : "
						+ mavHilState.vy / 100.0 + "m/s , " + "VELOCITY Z : "
						+ mavHilState.vz / 100.0 + "m/s , "
						+ "ACCELARATION X : " + mavHilState.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavHilState.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavHilState.zacc / 100000.0
						+ "metres/sec2";
				Map<String, Object> tempMavHilState = Maps.newHashMap();
				tempMavHilState.put("data", "MAVLINK_MSG_ID_HIL_STATE - "
						+ tempHilState);
				sendOutputJson(publishers[2], tempMavHilState);
				getLog().debug(tempHilState);
			}
			break;

		case msg_hil_controls.MAVLINK_MSG_ID_HIL_CONTROLS:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_hil_rc_inputs_raw.MAVLINK_MSG_ID_HIL_RC_INPUTS_RAW:
			msg_hil_rc_inputs_raw mavHilRcInputRaw;
			if (mavMessage2 instanceof msg_hil_rc_inputs_raw) 
			{
				mavHilRcInputRaw = (msg_hil_rc_inputs_raw) mavMessage2;
				String tempHilRcInputRaw = "[" + mavHilRcInputRaw.time_usec
						+ "] ," + "CHANNEL 1 : " + mavHilRcInputRaw.chan1_raw
						+ " , " + "CHANNEL 2 : " + mavHilRcInputRaw.chan2_raw
						+ " , " + "CHANNEL 3 : " + mavHilRcInputRaw.chan3_raw
						+ " , " + "CHANNEL 4 : " + mavHilRcInputRaw.chan4_raw
						+ " , " + "CHANNEL 5 : " + mavHilRcInputRaw.chan5_raw
						+ " , " + "CHANNEL 6 : " + mavHilRcInputRaw.chan6_raw
						+ " , " + "CHANNEL 7 : " + mavHilRcInputRaw.chan7_raw
						+ " , " + "CHANNEL 8 : " + mavHilRcInputRaw.chan8_raw
						+ " , " + "CHANNEL 9 : " + mavHilRcInputRaw.chan9_raw
						+ " , " + "CHANNEL 10 : " + mavHilRcInputRaw.chan10_raw
						+ " , " + "CHANNEL 12 : " + mavHilRcInputRaw.chan12_raw
						+ " , " + "SIGNAL STRENGTH : " + mavHilRcInputRaw.rssi;

				/**
				 * RC channels value in microseconds. A value of UINT16_MAX
				 * implies the channel is unused.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavHilRcInputRaw = Maps.newHashMap();
				tempMavHilRcInputRaw.put("data",
						"MAVLINK_MSG_ID_HIL_RC_INPUTS_RAW - "
								+ tempHilRcInputRaw);
				sendOutputJson(publishers[2], tempMavHilRcInputRaw);
				getLog().debug(tempHilRcInputRaw);
			}
			break;

		case msg_optical_flow.MAVLINK_MSG_ID_OPTICAL_FLOW:
			msg_optical_flow mavOpticalFlow;
			if (mavMessage2 instanceof msg_optical_flow) 
			{
				mavOpticalFlow = (msg_optical_flow) mavMessage2;
				String tempOpticalFlow = "[" + mavOpticalFlow.time_usec
						+ "] , " + "FLOW X : " + mavOpticalFlow.flow_comp_m_x
						+ "metres , " + "FLOW Y : "
						+ mavOpticalFlow.flow_comp_m_y + "metres , "
						+ "DISTANCE : " + mavOpticalFlow.ground_distance
						+ "metres , " + "FLOW PIXELS X : "
						+ mavOpticalFlow.flow_x + "metres , "
						+ "FLOW PIXELS Y : " + mavOpticalFlow.flow_y
						+ "metres , " + "SENSOR ID : "
						+ mavOpticalFlow.sensor_id + " , " + "QUALITY : "
						+ mavOpticalFlow.quality;
				Map<String, Object> tempMavOpticalFlow = Maps.newHashMap();
				tempMavOpticalFlow.put("data", "MAVLINK_MSG_ID_OPTICAL_FLOW - "
						+ tempOpticalFlow);
				sendOutputJson(publishers[2], tempMavOpticalFlow);
				getLog().debug(tempOpticalFlow);
			}
			break;

		case msg_global_vision_position_estimate.MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE:
			msg_global_vision_position_estimate mavGlobalVisionPositionEstimate;
			if (mavMessage2 instanceof msg_global_vision_position_estimate) 
			{
				mavGlobalVisionPositionEstimate = (msg_global_vision_position_estimate) mavMessage2;
				String tempGlobalVisionPositionEstimate = "["
						+ mavGlobalVisionPositionEstimate.usec + "]," + "X : "
						+ mavGlobalVisionPositionEstimate.x + "metres , "
						+ "Y : " + mavGlobalVisionPositionEstimate.y
						+ "metres , " + "Z : "
						+ mavGlobalVisionPositionEstimate.z + "metres , "
						+ "ROLL : " + mavGlobalVisionPositionEstimate.roll
						+ "rad , " + "PITCH : "
						+ mavGlobalVisionPositionEstimate.pitch + "rad , "
						+ "YAW : " + mavGlobalVisionPositionEstimate.yaw
						+ "rad ";
				Map<String, Object> tempMavGlobalVisionPositionEstimate = Maps
						.newHashMap();
				tempMavGlobalVisionPositionEstimate.put("data",
						"MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE - "
								+ tempGlobalVisionPositionEstimate);
				sendOutputJson(publishers[2],
						tempMavGlobalVisionPositionEstimate);
				getLog().debug(tempGlobalVisionPositionEstimate);
			}
			break;

		case msg_vision_position_estimate.MAVLINK_MSG_ID_VISION_POSITION_ESTIMATE:
			msg_vision_position_estimate mavVisionPositionEstimate;
			if (mavMessage2 instanceof msg_vision_position_estimate) 
			{
				mavVisionPositionEstimate = (msg_vision_position_estimate) mavMessage2;
				String tempVisionPositionEstimate = "["
						+ mavVisionPositionEstimate.usec + "]," + "X : "
						+ mavVisionPositionEstimate.x + "metres , " + "Y : "
						+ mavVisionPositionEstimate.y + "metres , " + "Z : "
						+ mavVisionPositionEstimate.z + "metres , " + "ROLL : "
						+ mavVisionPositionEstimate.roll + "rad , "
						+ "PITCH : " + mavVisionPositionEstimate.pitch
						+ "rad , " + "YAW : " + mavVisionPositionEstimate.yaw
						+ "rad ";
				Map<String, Object> tempMavVisionPositionEstimate = Maps
						.newHashMap();
				tempMavVisionPositionEstimate.put("data",
						"MAVLINK_MSG_ID_VISION_POSITION_ESTIMATE - "
								+ tempVisionPositionEstimate);
				sendOutputJson(publishers[2], tempMavVisionPositionEstimate);
				getLog().debug(tempVisionPositionEstimate);
			}
			break;

		case msg_vision_speed_estimate.MAVLINK_MSG_ID_VISION_SPEED_ESTIMATE:
			msg_vision_speed_estimate mavVisionSpeedEstimate;
			if (mavMessage2 instanceof msg_vision_speed_estimate) 
			{
				mavVisionSpeedEstimate = (msg_vision_speed_estimate) mavMessage2;
				String tempVisionSpeedEstimate = "["
						+ mavVisionSpeedEstimate.usec + "]," + "SPEED X : "
						+ mavVisionSpeedEstimate.x + "m/s , " + "SPEED Y : "
						+ mavVisionSpeedEstimate.y + "m/s , " + "SPEED Z : "
						+ mavVisionSpeedEstimate.z + "m/s , ";
				Map<String, Object> tempMavVisionSpeedEstimate = Maps
						.newHashMap();
				tempMavVisionSpeedEstimate.put("data",
						"MAVLINK_MSG_ID_VISION_SPEED_ESTIMATE - "
								+ tempVisionSpeedEstimate);
				sendOutputJson(publishers[2], tempMavVisionSpeedEstimate);
				getLog().debug(tempVisionSpeedEstimate);
			}
			break;

		case msg_vicon_position_estimate.MAVLINK_MSG_ID_VICON_POSITION_ESTIMATE:
			msg_vicon_position_estimate mavViconPositionEstimate;
			if (mavMessage2 instanceof msg_vicon_position_estimate) 
			{
				mavViconPositionEstimate = (msg_vicon_position_estimate) mavMessage2;
				String tempViconPositionEstimate = "["
						+ mavViconPositionEstimate.usec + "]," + "X : "
						+ mavViconPositionEstimate.x + "metres , " + "Y : "
						+ mavViconPositionEstimate.y + "metres , " + "Z : "
						+ mavViconPositionEstimate.z + "metres , " + "ROLL : "
						+ mavViconPositionEstimate.roll + "rad , "
						+ "PITCH : " + mavViconPositionEstimate.pitch
						+ "rad , " + "YAW : " + mavViconPositionEstimate.yaw
						+ "rad ";
				Map<String, Object> tempMavViconPositionEstimate = Maps
						.newHashMap();
				tempMavViconPositionEstimate.put("data",
						"MAVLINK_MSG_ID_VICON_POSITION_ESTIMATE - "
								+ tempViconPositionEstimate);
				sendOutputJson(publishers[2], tempMavViconPositionEstimate);
				getLog().debug(tempViconPositionEstimate);
			}
			break;

		case msg_highres_imu.MAVLINK_MSG_ID_HIGHRES_IMU:
			msg_highres_imu mavHighresImu;
			if (mavMessage2 instanceof msg_highres_imu) 
			{
				mavHighresImu = (msg_highres_imu) mavMessage2;
				String tempHighresImu = "["
						+ mavHighresImu.time_usec
						+ "] , "
						+ "ACCELARATION X : "
						+ mavHighresImu.xacc
						+ "metres/sec2 , "
						+ "ACCELARATION Y : "
						+ mavHighresImu.yacc
						+ "metres/sec2 , "
						+ "ACCELARATION Z : "
						+ mavHighresImu.zacc
						+ "metres/sec2 , "
						+ "OMEGA X : "
						+ mavHighresImu.xgyro
						+ "rad/s , "
						+ "OMEGA Y : "
						+ mavHighresImu.ygyro
						+ "rad/s , "
						+ "OMEGA Z : "
						+ mavHighresImu.zgyro
						+ "rad/s , "
						+ "MAGNETIC FIELD X : "
						+ mavHighresImu.xmag
						+ "Gauss , "
						+ "MAGNETIC FIELD Y : "
						+ mavHighresImu.ymag
						+ "Gauss , "
						+ "MAGNETIC FIELD Z : "
						+ mavHighresImu.zmag
						+ "Gauss , "
						+ "ABSOLUTE PRESSURE : "
						+ mavHighresImu.abs_pressure
						+ "millibar , "
						+ "DIFFERENTIAL PRESSURE : "
						+ mavHighresImu.diff_pressure
						+ "millibar , "
						+ "ALTITUDE FROM PRESSURE : "
						+ mavHighresImu.pressure_alt
						+ "metres , "
						+ "TEMPERATURE : "
						+ mavHighresImu.temperature
						+ "degree Celsius , "
						+ "UPDATED FIELDS : "
						+ Integer.toBinaryString(0xFFFF & mavHighresImu.fields_updated);
				Map<String, Object> tempMavHighresImu = Maps.newHashMap();
				tempMavHighresImu.put("data", "MAVLINK_MSG_ID_HIGHRES_IMU - "
						+ tempHighresImu);
				sendOutputJson(publishers[2], tempMavHighresImu);
				getLog().debug(tempHighresImu);
			}
			break;

		case msg_optical_flow_rad.MAVLINK_MSG_ID_OPTICAL_FLOW_RAD:
			msg_optical_flow_rad mavOpticalFlowRad;
			if (mavMessage2 instanceof msg_optical_flow_rad) 
			{
				mavOpticalFlowRad = (msg_optical_flow_rad) mavMessage2;
				String tempOpticalFlowRad = "[" + mavOpticalFlowRad.time_usec
						+ "] , " + "INTEGRATION TIME : "
						+ mavOpticalFlowRad.integration_time_us
						+ "micro seconds , " + "FLOW X : "
						+ mavOpticalFlowRad.integrated_x + "rad , "
						+ "FLOW Y : " + mavOpticalFlowRad.integrated_y
						+ "rad , " + "RH ROTATION X : "
						+ mavOpticalFlowRad.integrated_xgyro + "rad , "
						+ "RH ROTATION Y : "
						+ mavOpticalFlowRad.integrated_ygyro + "rad , "
						+ "RH ROTATION Z : "
						+ mavOpticalFlowRad.integrated_zgyro + "rad , "
						+ "DELTA TIME : "
						+ mavOpticalFlowRad.time_delta_distance_us
						+ "micro seconds , " + "DISTANCE : "
						+ mavOpticalFlowRad.distance + "metres , "
						+ "TEMPERATURE : " + mavOpticalFlowRad.temperature
						/ 100.0 + "degree Celsius , " + "SENSOR ID : "
						+ mavOpticalFlowRad.sensor_id + " , " + "QUALITY : "
						+ mavOpticalFlowRad.quality;
				Map<String, Object> tempMavOpticalFlowRad = Maps.newHashMap();
				tempMavOpticalFlowRad.put("data",
						"MAVLINK_MSG_ID_OPTICAL_FLOW_RAD - "
								+ tempOpticalFlowRad);
				sendOutputJson(publishers[2], tempMavOpticalFlowRad);
				getLog().debug(tempOpticalFlowRad);
			}
			break;

		case msg_hil_sensor.MAVLINK_MSG_ID_HIL_SENSOR:
			msg_hil_sensor mavHilSensor;
			if (mavMessage2 instanceof msg_hil_sensor) 
			{
				mavHilSensor = (msg_hil_sensor) mavMessage2;
				String tempHilSensor = "["
						+ mavHilSensor.time_usec
						+ "] , "
						+ "ACCELARATION X : "
						+ mavHilSensor.xacc
						+ "metres/sec2 , "
						+ "ACCELARATION Y : "
						+ mavHilSensor.yacc
						+ "metres/sec2 , "
						+ "ACCELARATION Z : "
						+ mavHilSensor.zacc
						+ "metres/sec2 , "
						+ "OMEGA X : "
						+ mavHilSensor.xgyro
						+ "rad/s , "
						+ "OMEGA Y : "
						+ mavHilSensor.ygyro
						+ "rad/s , "
						+ "OMEGA Z : "
						+ mavHilSensor.zgyro
						+ "rad/s , "
						+ "MAGNETIC FIELD X : "
						+ mavHilSensor.xmag
						+ "Gauss , "
						+ "MAGNETIC FIELD Y : "
						+ mavHilSensor.ymag
						+ "Gauss , "
						+ "MAGNETIC FIELD Z : "
						+ mavHilSensor.zmag
						+ "Gauss , "
						+ "ABSOLUTE PRESSURE : "
						+ mavHilSensor.abs_pressure
						+ "millibar , "
						+ "DIFFERENTIAL PRESSURE : "
						+ mavHilSensor.diff_pressure
						+ "millibar , "
						+ "ALTITUDE FROM PRESSURE : "
						+ mavHilSensor.pressure_alt
						+ "metres , "
						+ "TEMPERATURE : "
						+ mavHilSensor.temperature
						+ "degree Celsius , "
						+ "UPDATED FIELDS : "
						+ Integer.toBinaryString(0xFFFF & mavHilSensor.fields_updated);
				Map<String, Object> tempMavHilSensor = Maps.newHashMap();
				tempMavHilSensor.put("data", "MAVLINK_MSG_ID_HIL_SENSOR - "
						+ tempHilSensor);
				sendOutputJson(publishers[2], tempMavHilSensor);
				getLog().debug(tempHilSensor);
			}
			break;

		case msg_sim_state.MAVLINK_MSG_ID_SIM_STATE:
			msg_sim_state  mavSimState;
			if (mavMessage2 instanceof msg_sim_state) 
			{
				mavSimState = (msg_sim_state) mavMessage2;
				String tempSimState = "ATTITUDE QUATERNION COMPONENT 1 : "
						+ mavSimState.q1 + " , "
						+ "ATTITUDE QUATERNION COMPONENT 2 : " + mavSimState.q2
						+ " , " + "ATTITUDE QUATERNION COMPONENT 3 : "
						+ mavSimState.q3 + " , "
						+ "ATTITUDE QUATERNION COMPONENT 4 : " + mavSimState.q4
						+ " , " + "ROLL : " + mavSimState.roll + "degrees , "
						+ "PITCH : " + mavSimState.pitch + "degrees , "
						+ "YAW : " + mavSimState.yaw + "degrees , "
						+ "LATITUDE : " + mavSimState.lat + "degrees , "
						+ "LONGITUDE : " + mavSimState.lon + "degrees , "
						+ "ALTITUDE : " + mavSimState.alt + "metres , "
						+ "VELOCITY NORTH : " + mavSimState.vn + "m/s , "
						+ "VELOCITY EAST : " + mavSimState.ve + "m/s , "
						+ "VELOCITY DOWN : " + mavSimState.vd + "m/s , "
						+ "STANDARD DEVIATION HORIZONTAL POSITION : "
						+ mavSimState.std_dev_horz + " , "
						+ "STANDARD DEVIATION VERTICAL POSITION : "
						+ mavSimState.std_dev_vert + " , "
						+ "ACCELARATION X : " + mavSimState.xacc
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavSimState.yacc + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavSimState.zacc
						+ "metres/sec2 , " + "OMEGA X : " + mavSimState.xgyro
						+ "rad/s , " + "OMEGA Y : " + mavSimState.ygyro
						+ "rad/s , " + "OMEGA Z : " + mavSimState.zgyro
						+ "rad/s ";
				Map<String, Object> tempMavSimState = Maps.newHashMap();
				tempMavSimState.put("data", "MAVLINK_MSG_ID_SIM_STATE - "
						+ tempSimState);
				sendOutputJson(publishers[2], tempMavSimState);
				getLog().debug(tempSimState);
			}
			break;

		case msg_radio_status.MAVLINK_MSG_ID_RADIO_STATUS:
			msg_radio_status mavRadioStatus;
			if (mavMessage2 instanceof msg_radio_status) 
			{
				mavRadioStatus = (msg_radio_status) mavMessage2;
				String tempRadioStatus = "RECEIVE ERRORS : "
						+ mavRadioStatus.rxerrors + " , "
						+ "ERROR CORRECTED PACKET COUNT : "
						+ mavRadioStatus.fixed + " , " + "SIGNAL STRENGTH : "
						+ mavRadioStatus.rssi + " , "
						+ "REMOTE SIGNAL STRENGTH :" + mavRadioStatus.remrssi
						+ " , " + "FREE BUFFERS : " + mavRadioStatus.txbuf
						+ "% , " + "NOISE : " + mavRadioStatus.noise + " , "
						+ "REMOTE NOISE : " + mavRadioStatus.remnoise;
				Map<String, Object> tempMavRadioStatus = Maps.newHashMap();
				tempMavRadioStatus.put("data", "MAVLINK_MSG_ID_RADIO_STATUS - "
						+ tempRadioStatus);
				sendOutputJson(publishers[2], tempMavRadioStatus);
				getLog().debug(tempRadioStatus);
			}
			break;

		case msg_file_transfer_protocol.MAVLINK_MSG_ID_FILE_TRANSFER_PROTOCOL:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_timesync.MAVLINK_MSG_ID_TIMESYNC:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_camera_trigger.MAVLINK_MSG_ID_CAMERA_TRIGGER:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_hil_gps.MAVLINK_MSG_ID_HIL_GPS:
			msg_hil_gps mavHilGps;
			if (mavMessage2 instanceof msg_hil_gps) 
			{
				mavHilGps = (msg_hil_gps) mavMessage2;
				Map<String, Object> tempMavHilGps = Maps.newHashMap();
				String tempHilGps = "[" + mavHilGps.time_usec + "] ,"
						+ "LATITUDE : " + mavHilGps.lat / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavHilGps.lon / 10000000.0 + "degrees , "
						+ "ALTITUDE : " + mavHilGps.alt / 1000.0 + "metres , "
						+ "HORIZONTAL DILUTION : " + mavHilGps.eph / 100.0
						+ "metres , " + "VERTICAL DILUTION : "
						+ mavHilGps.epv / 100.0 + "metres , " + "VELOCITY : "
						+ mavHilGps.vel / 100.0 + "m/s , "
						+ "COURSE OVER GROUND : " + mavHilGps.cog / 100.0
						+ "degrees , " + "FIX TYPE : " + mavHilGps.fix_type
						+ "D , " + "SATELLITES VISIBLE : "
						+ mavHilGps.satellites_visible + "VELOCITY NORTH : "
						+ mavHilGps.vn + "m/s , " + "VELOCITY EAST : "
						+ mavHilGps.ve + "m/s , " + "VELOCITY DOWN : "
						+ mavHilGps.vd + "m/s ";
				tempMavHilGps.put("data", "MAVLINK_MSG_ID_HIL_GPS - "
						+ tempHilGps);
				sendOutputJson(publishers[2], tempMavHilGps);
				getLog().debug(tempHilGps);
			}
			break;

		case msg_hil_optical_flow.MAVLINK_MSG_ID_HIL_OPTICAL_FLOW:
			msg_hil_optical_flow mavHilOpticalFlowRad;
			if (mavMessage2 instanceof msg_hil_optical_flow) 
			{
				mavHilOpticalFlowRad = (msg_hil_optical_flow) mavMessage2;
				String tempHilOpticalFlowRad = "["
						+ mavHilOpticalFlowRad.time_usec + "] , "
						+ "INTEGRATION TIME : "
						+ mavHilOpticalFlowRad.integration_time_us
						+ "micro seconds , " + "FLOW X : "
						+ mavHilOpticalFlowRad.integrated_x + "rad , "
						+ "FLOW Y : " + mavHilOpticalFlowRad.integrated_y
						+ "rad , " + "RH ROTATION X : "
						+ mavHilOpticalFlowRad.integrated_xgyro + "rad , "
						+ "RH ROTATION Y : "
						+ mavHilOpticalFlowRad.integrated_ygyro + "rad , "
						+ "RH ROTATION Z : "
						+ mavHilOpticalFlowRad.integrated_zgyro + "rad , "
						+ "DELTA TIME : "
						+ mavHilOpticalFlowRad.time_delta_distance_us
						+ "micro seconds , " + "DISTANCE : "
						+ mavHilOpticalFlowRad.distance + "metres , "
						+ "TEMPERATURE : " + mavHilOpticalFlowRad.temperature
						/ 100.0 + "degree Celsius , " + "SENSOR ID : "
						+ mavHilOpticalFlowRad.sensor_id + " , " + "QUALITY : "
						+ mavHilOpticalFlowRad.quality;
				Map<String, Object> tempMavHilOpticalFlowRad = Maps
						.newHashMap();
				tempMavHilOpticalFlowRad.put("data",
						"MAVLINK_MSG_ID_HIL_OPTICAL_FLOW - "
								+ tempHilOpticalFlowRad);
				sendOutputJson(publishers[2], tempMavHilOpticalFlowRad);
				getLog().debug(tempHilOpticalFlowRad);
			}
			break;

		case msg_hil_state_quaternion.MAVLINK_MSG_ID_HIL_STATE_QUATERNION:
			msg_hil_state_quaternion  mavHilStateQuaternion;
			if (mavMessage2 instanceof msg_hil_state_quaternion) 
			{
				mavHilStateQuaternion = (msg_hil_state_quaternion) mavMessage2;
				String tempHilStateQuaternion = "["
						+ mavHilStateQuaternion.time_usec + "] , "
						+ "QUATERNION COMPONENT 1 : "
						+ mavHilStateQuaternion.attitude_quaternion[1] + " , "
						+ "QUATERNION COMPONENT 2 : "
						+ mavHilStateQuaternion.attitude_quaternion[2] + " , "
						+ "QUATERNION COMPONENT 3 : "
						+ mavHilStateQuaternion.attitude_quaternion[3] + " , "
						+ "QUATERNION COMPONENT 4 : "
						+ mavHilStateQuaternion.attitude_quaternion[4] + " , "
						+ "ROLL SPEED : " + mavHilStateQuaternion.rollspeed
						+ "rad/s , " + "PITCH SPEED : "
						+ mavHilStateQuaternion.pitchspeed + "rad/s , "
						+ "YAW SPEED : " + mavHilStateQuaternion.yawspeed
						+ "rad/s , " + "LATITUDE : "
						+ mavHilStateQuaternion.lat / 10000000.0 + "degrees , "
						+ "LONGITUDE : " + mavHilStateQuaternion.lon
						/ 10000000.0 + "degrees , " + "ALTITUDE : "
						+ mavHilStateQuaternion.alt / 1000.0 + "metres , "
						+ "VELOCITY X : " + mavHilStateQuaternion.vx / 100.0
						+ "m/s , " + "VELOCITY Y : " + mavHilStateQuaternion.vy
						/ 100.0 + "m/s , " + "VELOCITY Z : "
						+ mavHilStateQuaternion.vz / 100.0 + "m/s , "
						+ "INDICATED AIRSPEED : "
						+ mavHilStateQuaternion.ind_airspeed / 100.0 + "m/s , "
						+ "TRUE AIRSPEED : "
						+ mavHilStateQuaternion.true_airspeed / 100.0
						+ "m/s , " + "ACCELARATION X : "
						+ mavHilStateQuaternion.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavHilStateQuaternion.yacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Z : "
						+ mavHilStateQuaternion.zacc / 100000.0 + "metres/sec2";
				Map<String, Object> tempMavHilStateQuaternion = Maps
						.newHashMap();
				tempMavHilStateQuaternion.put("data",
						"MAVLINK_MSG_ID_HIL_STATE_QUATERNION - "
								+ tempHilStateQuaternion);
				sendOutputJson(publishers[2], tempMavHilStateQuaternion);
				getLog().debug(tempHilStateQuaternion);
			}
			break;

		case msg_scaled_imu2.MAVLINK_MSG_ID_SCALED_IMU2:
			msg_scaled_imu2 mavScaledImu2;
			if (mavMessage2 instanceof msg_scaled_imu2) 
			{
				mavScaledImu2 = (msg_scaled_imu2) mavMessage2;
				String tempScaledImu2 = "[" + mavScaledImu2.time_boot_ms + "] , "
						+ "ACCELARATION X : " + mavScaledImu2.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavScaledImu2.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavScaledImu2.zacc / 100000.0
						+ "metres/sec2 , " + "OMEGA X : "
						+ mavScaledImu2.xgyro / 1000.0 + "rad/s , "
						+ "OMEGA Y : " + mavScaledImu2.ygyro / 1000.0
						+ "rad/s , " + "OMEGA Z : "
						+ mavScaledImu2.zgyro / 1000.0 + "rad/s , "
						+ "MAGNETIC FIELD X : " + mavScaledImu2.xmag / 1000.0
						+ "Tesla , " + "MAGNETIC FIELD Y : "
						+ mavScaledImu2.ymag / 1000.0 + "Tesla , "
						+ "MAGNETIC FIELD Z : " + mavScaledImu2.zmag / 1000.0
						+ "Tesla";
				Map<String, Object> tempMavScaledImu2 = Maps.newHashMap();
				tempMavScaledImu2.put("data", "MAVLINK_MSG_ID_SCALED_IMU2 - "
						+ tempScaledImu2);
				sendOutputJson(publishers[2], tempMavScaledImu2);
				getLog().debug(tempScaledImu2);
			}
			break;

		case msg_log_request_list.MAVLINK_MSG_ID_LOG_REQUEST_LIST:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_log_entry.MAVLINK_MSG_ID_LOG_ENTRY:
			msg_log_entry mavLogEntry;
			if (mavMessage2 instanceof msg_log_entry) 
			{
				mavLogEntry = (msg_log_entry) mavMessage2;
				String tempLogEntry = "[" + mavLogEntry.time_utc + "] , "
						+ "SIZE : " + mavLogEntry.size + " , " + "LOG ID : "
						+ mavLogEntry.id + " , " + "TOTAL NUMBER OF LOGS : "
						+ mavLogEntry.num_logs + " , " + "LAST LOG NUMBER : "
						+ mavLogEntry.last_log_num;
				Map<String, Object> tempMavLogEntry = Maps.newHashMap();
				tempMavLogEntry.put("data", "MAVLINK_MSG_ID_LOG_ENTRY - "
						+ tempLogEntry);
				sendOutputJson(publishers[2], tempMavLogEntry);
				getLog().debug(tempLogEntry);
				saveLogEntry(mavLogEntry);
			}
			break;

		case msg_log_request_data.MAVLINK_MSG_ID_LOG_REQUEST_DATA:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_log_data.MAVLINK_MSG_ID_LOG_DATA:
			msg_log_data mavLogData;
			if (mavMessage2 instanceof msg_log_data) 
			{
				mavLogData = (msg_log_data) mavMessage2;
				String tempLogData = "OFFSET : " + mavLogData.ofs + " , "
						+ "LOG ID : " + mavLogData.id + " , "
						+ "TOTAL NUMBER OF BYTES : " + mavLogData.count + " , "
						+ "DATA : " + mavLogData.data;
				Map<String, Object> tempMavLogData = Maps.newHashMap();
				tempMavLogData.put("data", "MAVLINK_MSG_ID_LOG_DATA - "
						+ tempLogData);
				sendOutputJson(publishers[2], tempMavLogData);
				getLog().debug(tempLogData);
			}
			break;

		case msg_log_erase.MAVLINK_MSG_ID_LOG_ERASE:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_log_request_end.MAVLINK_MSG_ID_LOG_REQUEST_END:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_gps_inject_data.MAVLINK_MSG_ID_GPS_INJECT_DATA:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_gps2_raw.MAVLINK_MSG_ID_GPS2_RAW:
			msg_gps2_raw mavGps2;
			if (mavMessage2 instanceof msg_gps2_raw) 
			{
				mavGps2 = (msg_gps2_raw) mavMessage2;
				Map<String, Object> tempMavGps2 = Maps.newHashMap();
				String tempGps2 = "[" + mavGps2.time_usec + "] ,"
						+ "LATITUDE : " + mavGps2.lat / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavGps2.lon / 10000000.0 + "degrees , "
						+ "ALTITUDE : " + mavGps2.alt / 1000.0 + "metres , "
						+ "HORIZONTAL DILUTION : " + mavGps2.eph / 100.0
						+ "metres , " + "VERTICAL DILUTION : "
						+ mavGps2.epv / 100.0 + "metres , " + "VELOCITY : "
						+ mavGps2.vel / 100.0 + "m/s , "
						+ "COURSE OVER GROUND : " + mavGps2.cog / 100.0
						+ "degrees , " + "FIX TYPE : " + mavGps2.fix_type
						+ "D , " + "SATELLITES VISIBLE : "
						+ mavGps2.satellites_visible + "DGPS INFO AGE : "
						+ mavGps2.dgps_age + "DGPS SATELLITE NUMBER : "
						+ mavGps2.dgps_numch;
				tempMavGps2
						.put("data", "MAVLINK_MSG_ID_GPS2_RAW - " + tempGps2);
				sendOutputJson(publishers[2], tempMavGps2);
				getLog().debug(tempGps2);
			}
			break;

		case msg_power_status.MAVLINK_MSG_ID_POWER_STATUS:
			Map<String,	Object> tempMavPowerStatus= Maps.newHashMap();
			tempMavPowerStatus.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavPowerStatus);
			getLog().debug(mavMessage2.toString());
			break;

		case msg_serial_control.MAVLINK_MSG_ID_SERIAL_CONTROL:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_gps_rtk.MAVLINK_MSG_ID_GPS_RTK:
			msg_gps_rtk mavGpsRtk;
			if (mavMessage2 instanceof msg_gps_rtk) 
			{
				mavGpsRtk = (msg_gps_rtk) mavMessage2;
				String tempGpsRtk =null;
				switch (mavGpsRtk.baseline_coords_type) 
				{
				case 0:
					tempGpsRtk = "[" + mavGpsRtk.time_last_baseline_ms + "] ,"
							+ "GPS Time of Week : " + mavGpsRtk.tow + " , "
							+ "CURRENT BASELINE IN ECEF X : "
							+ mavGpsRtk.baseline_a_mm + "mm , "
							+ "CURRENT BASELINE IN ECEF Y : "
							+ mavGpsRtk.baseline_b_mm + "mm , "
							+ "CURRENT BASELINE IN ECEF Z : "
							+ mavGpsRtk.baseline_c_mm + "mm , " + "ACCURACY : "
							+ mavGpsRtk.accuracy + " , "
							+ "NUMBER OF INTEGER AMBIGUITY HYPOTHESIS : "
							+ mavGpsRtk.iar_num_hypotheses + " , "
							+ "GPS WEEK NUMBER OF LAST BASELINE : "
							+ mavGpsRtk.wn + " , " + "RTK RECEIVER ID : "
							+ mavGpsRtk.rtk_receiver_id + " , "
							+ "RTK RECEIVER RATE : " + mavGpsRtk.rtk_rate
							+ "Hz , " + "RTK RECEIVER HEALTH : "
							+ mavGpsRtk.rtk_health + " , "
							+ "NUMBER OF SATELLITES USED : " + mavGpsRtk.nsats;
					break;

				case 1:
					tempGpsRtk = "[" + mavGpsRtk.time_last_baseline_ms + "] ,"
							+ "GPS Time of Week : " + mavGpsRtk.tow + " , "
							+ "NED NORTH COMPONENT : "
							+ mavGpsRtk.baseline_a_mm + "mm , "
							+ "NED EAST COMPONENT : " + mavGpsRtk.baseline_b_mm
							+ "mm , " + "NED DOWN COMPONENT : "
							+ mavGpsRtk.baseline_c_mm + "mm , " + "ACCURACY : "
							+ mavGpsRtk.accuracy + " , "
							+ "NUMBER OF INTEGER AMBIGUITY HYPOTHESIS : "
							+ mavGpsRtk.iar_num_hypotheses + " , "
							+ "GPS WEEK NUMBER OF LAST BASELINE : "
							+ mavGpsRtk.wn + " , " + "RTK RECEIVER ID : "
							+ mavGpsRtk.rtk_receiver_id + " , "
							+ "RTK RECEIVER RATE : " + mavGpsRtk.rtk_rate
							+ "Hz , " + "RTK RECEIVER HEALTH : "
							+ mavGpsRtk.rtk_health + " , "
							+ "NUMBER OF SATELLITES USED : " + mavGpsRtk.nsats;
					break;
					
				default:
					getLog().error("Bad Baseline Coordinate System Type");
					break;
				}
				if (tempGpsRtk != null) 
				{
					Map<String, Object> tempMavGpsRtk = Maps.newHashMap();
					tempMavGpsRtk.put("data", "MAVLINK_MSG_ID_GPS_RTK - "
							+ tempGpsRtk);
					sendOutputJson(publishers[2], tempMavGpsRtk);
					getLog().debug(tempGpsRtk);	
				}
			}
			break;

		case msg_gps2_rtk.MAVLINK_MSG_ID_GPS2_RTK:
			msg_gps2_rtk mavGps2Rtk;
			if (mavMessage2 instanceof msg_gps2_rtk) 
			{
				mavGps2Rtk = (msg_gps2_rtk) mavMessage2;
				String tempGps2Rtk =null;
				switch (mavGps2Rtk.baseline_coords_type) 
				{
				case 0:
					tempGps2Rtk = "[" + mavGps2Rtk.time_last_baseline_ms + "] ,"
							+ "GPS Time of Week : " + mavGps2Rtk.tow + " , "
							+ "CURRENT BASELINE IN ECEF X : "
							+ mavGps2Rtk.baseline_a_mm + "mm , "
							+ "CURRENT BASELINE IN ECEF Y : "
							+ mavGps2Rtk.baseline_b_mm + "mm , "
							+ "CURRENT BASELINE IN ECEF Z : "
							+ mavGps2Rtk.baseline_c_mm + "mm , " + "ACCURACY : "
							+ mavGps2Rtk.accuracy + " , "
							+ "NUMBER OF INTEGER AMBIGUITY HYPOTHESIS : "
							+ mavGps2Rtk.iar_num_hypotheses + " , "
							+ "GPS WEEK NUMBER OF LAST BASELINE : "
							+ mavGps2Rtk.wn + " , " + "RTK RECEIVER ID : "
							+ mavGps2Rtk.rtk_receiver_id + " , "
							+ "RTK RECEIVER RATE : " + mavGps2Rtk.rtk_rate
							+ "Hz , " + "RTK RECEIVER HEALTH : "
							+ mavGps2Rtk.rtk_health + " , "
							+ "NUMBER OF SATELLITES USED : " + mavGps2Rtk.nsats;
					break;

				case 1:
					tempGps2Rtk = "[" + mavGps2Rtk.time_last_baseline_ms + "] ,"
							+ "GPS Time of Week : " + mavGps2Rtk.tow + " , "
							+ "NED NORTH COMPONENT : "
							+ mavGps2Rtk.baseline_a_mm + "mm , "
							+ "NED EAST COMPONENT : " + mavGps2Rtk.baseline_b_mm
							+ "mm , " + "NED DOWN COMPONENT : "
							+ mavGps2Rtk.baseline_c_mm + "mm , " + "ACCURACY : "
							+ mavGps2Rtk.accuracy + " , "
							+ "NUMBER OF INTEGER AMBIGUITY HYPOTHESIS : "
							+ mavGps2Rtk.iar_num_hypotheses + " , "
							+ "GPS WEEK NUMBER OF LAST BASELINE : "
							+ mavGps2Rtk.wn + " , " + "RTK RECEIVER ID : "
							+ mavGps2Rtk.rtk_receiver_id + " , "
							+ "RTK RECEIVER RATE : " + mavGps2Rtk.rtk_rate
							+ "Hz , " + "RTK RECEIVER HEALTH : "
							+ mavGps2Rtk.rtk_health + " , "
							+ "NUMBER OF SATELLITES USED : " + mavGps2Rtk.nsats;
					break;
					
				default:
					getLog().error("Bad Baseline Coordinate System Type");
					break;
				}
				if (tempGps2Rtk != null) 
				{
					Map<String, Object> tempMavGps2Rtk = Maps.newHashMap();
					tempMavGps2Rtk.put("data", "MAVLINK_MSG_ID_GPS2_RTK - "
							+ tempGps2Rtk);
					sendOutputJson(publishers[2], tempMavGps2Rtk);
					getLog().debug(tempGps2Rtk);	
				}
			}
			break;

		case msg_scaled_imu3.MAVLINK_MSG_ID_SCALED_IMU3:
			msg_scaled_imu3 mavScaledImu3;
			if (mavMessage2 instanceof msg_scaled_imu3) 
			{
				mavScaledImu3 = (msg_scaled_imu3) mavMessage2;
				String tempScaledImu3 = "[" + mavScaledImu3.time_boot_ms + "] , "
						+ "ACCELARATION X : " + mavScaledImu3.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavScaledImu3.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavScaledImu3.zacc / 100000.0
						+ "metres/sec2 , " + "OMEGA X : "
						+ mavScaledImu3.xgyro / 1000.0 + "rad/s , "
						+ "OMEGA Y : " + mavScaledImu3.ygyro / 1000.0
						+ "rad/s , " + "OMEGA Z : "
						+ mavScaledImu3.zgyro / 1000.0 + "rad/s , "
						+ "MAGNETIC FIELD X : " + mavScaledImu3.xmag / 1000.0
						+ "Tesla , " + "MAGNETIC FIELD Y : "
						+ mavScaledImu3.ymag / 1000.0 + "Tesla , "
						+ "MAGNETIC FIELD Z : " + mavScaledImu3.zmag / 1000.0
						+ "Tesla";
				Map<String, Object> tempMavScaledImu3 = Maps.newHashMap();
				tempMavScaledImu3.put("data", "MAVLINK_MSG_ID_SCALED_IMU3 - "
						+ tempScaledImu3);
				sendOutputJson(publishers[2], tempMavScaledImu3);
				getLog().debug(tempScaledImu3);
			}
			break;

		case msg_data_transmission_handshake.MAVLINK_MSG_ID_DATA_TRANSMISSION_HANDSHAKE:
			msg_data_transmission_handshake mavDataTransmissionHandshake;
			if (mavMessage2 instanceof msg_data_transmission_handshake) 
			{
				mavDataTransmissionHandshake = (msg_data_transmission_handshake) mavMessage2;
				String tempDataTransmissionHandshake = "TOTAL SIZE : "
						+ mavDataTransmissionHandshake.size
						+ " , "
						+ "WIDTH : "
						+ mavDataTransmissionHandshake.width
						+ " , "
						+ "HEIGHT : "
						+ mavDataTransmissionHandshake.height
						+ " , "
						+ "PACKETS SENT : "
						+ mavDataTransmissionHandshake.packets
						+ " , "
						+ "DATA TYPE : "
						+ getVariableName("DATA_TYPES",
								mavDataTransmissionHandshake.type) + " , "
						+ "PAYLOAD SIZE : "
						+ mavDataTransmissionHandshake.payload + " , "
						+ "JPG QUALITY : "
						+ mavDataTransmissionHandshake.jpg_quality;
				Map<String, Object> tempMavDataTransmissionHandshake = Maps
						.newHashMap();
				tempMavDataTransmissionHandshake.put("data",
						"MAVLINK_MSG_ID_DATA_TRANSMISSION_HANDSHAKE - "
								+ tempDataTransmissionHandshake);
				sendOutputJson(publishers[2], tempMavDataTransmissionHandshake);
				getLog().debug(tempDataTransmissionHandshake);
			}
			break;

		case msg_encapsulated_data.MAVLINK_MSG_ID_ENCAPSULATED_DATA:
			msg_encapsulated_data mavEncapsulatedData;
			if (mavMessage2 instanceof msg_encapsulated_data) 
			{
				mavEncapsulatedData = (msg_encapsulated_data) mavMessage2;
				String tempEncapsulatedData = "SEQUENCE NUMBER : "
						+ mavEncapsulatedData.seqnr + " , " + "Data : "
						+ mavEncapsulatedData.data;
				Map<String, Object> tempMavEncapsulatedData = Maps.newHashMap();
				tempMavEncapsulatedData.put("data",
						"MAVLINK_MSG_ID_ENCAPSULATED_DATA - "
								+ tempEncapsulatedData);
				sendOutputJson(publishers[2], tempMavEncapsulatedData);
				getLog().debug(tempEncapsulatedData);
			}
			break;

		case msg_distance_sensor.MAVLINK_MSG_ID_DISTANCE_SENSOR:
			msg_distance_sensor mavDistanceSensor;
			if (mavMessage2 instanceof msg_distance_sensor) 
			{
				mavDistanceSensor = (msg_distance_sensor) mavMessage2;
				String tempDistanceSensor = "["
						+ mavDistanceSensor.time_boot_ms
						+ "] , "
						+ "MINIMUM DISTANCE : "
						+ mavDistanceSensor.min_distance
						+ "cm , "
						+ "MAXIMUM DISTANCE : "
						+ mavDistanceSensor.max_distance
						+ "cm , "
						+ "CURRENT DISTANCE : "
						+ mavDistanceSensor.current_distance
						+ "cm , "
						+ "SENSOR TYPE : "
						+ getVariableName("MAV_DISTANCE_SENSOR",
								mavDistanceSensor.type)
						+ "SENSOR ID : "
						+ mavDistanceSensor.id
						+ "SENSOR ORIENTATION : "
						+ getVariableName("MAV_SENSOR_ORIENTATION",
								mavDistanceSensor.orientation)
						+ "COVARIANCE : " + mavDistanceSensor.covariance + "cm";
				Map<String, Object> tempMavDistanceSensor = Maps.newHashMap();
				tempMavDistanceSensor.put("data",
						"MAVLINK_MSG_ID_DISTANCE_SENSOR - "
								+ tempDistanceSensor);
				sendOutputJson(publishers[2], tempMavDistanceSensor);
				getLog().debug(tempDistanceSensor);
			}
			break;

		case msg_terrain_request.MAVLINK_MSG_ID_TERRAIN_REQUEST:
			/**
			 * Request for terrain data and terrain status
			 */
			msg_terrain_request  mavTerrainRequest;
			if (mavMessage2 instanceof msg_terrain_request) 
			{
				mavTerrainRequest = (msg_terrain_request) mavMessage2;
				String tempTerrainRequest = "BITMASK : "
						+ mavTerrainRequest.mask + " , "
						+ "SW CORNER LATITUDE : " + mavTerrainRequest.lat
						/ 10000000.0 + "degrees , " + "SW CORNER LONGITUDE : "
						+ mavTerrainRequest.lon / 10000000.0 + "degrees , "
						+ "GRID SPACING : " + mavTerrainRequest.grid_spacing
						+ "m ";
				Map<String, Object> tempMavTerrainRequest = Maps.newHashMap();
				tempMavTerrainRequest.put("data",
						"MAVLINK_MSG_ID_TERRAIN_REQUEST - "
								+ tempTerrainRequest);
				sendOutputJson(publishers[2], tempMavTerrainRequest);
				getLog().debug(tempTerrainRequest);
			}
			break;

		case msg_terrain_data.MAVLINK_MSG_ID_TERRAIN_DATA:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_terrain_check.MAVLINK_MSG_ID_TERRAIN_CHECK:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_terrain_report.MAVLINK_MSG_ID_TERRAIN_REPORT:
			/**
			 * Response from a TERRAIN_CHECK request
			 */
			msg_terrain_report  mavTerrainReport;
			if (mavMessage2 instanceof msg_terrain_report) 
			{
				mavTerrainReport = (msg_terrain_report) mavMessage2;
				String tempTerrainReport = "LATITUDE : " + mavTerrainReport.lat
						/ 10000000.0 + "degrees , " + "LONGITUDE : "
						+ mavTerrainReport.lon / 10000000.0 + "degrees , "
						+ "TERRAIN HEIGHT : " + mavTerrainReport.terrain_height
						+ "m , " + "CURRENT HEIGHT : "
						+ mavTerrainReport.current_height + "m , "
						+ "GRID SPACING : " + mavTerrainReport.spacing + "m , "
						+ "NUMBER OF BLOCKS PENDING : "
						+ mavTerrainReport.pending + " , "
						+ "NUMBER OF BLOCKS LOADED : "
						+ mavTerrainReport.loaded;
				Map<String, Object> tempMavTerrainReport = Maps.newHashMap();
				tempMavTerrainReport.put("data",
						"MAVLINK_MSG_ID_TERRAIN_REPORT - " + tempTerrainReport);
				sendOutputJson(publishers[2], tempMavTerrainReport);
				getLog().debug(tempTerrainReport);
				
				tempMavTerrainReport.clear();
				tempMavTerrainReport.put("terrain_report", tempTerrainReport);
				sendOutputJson(publishers[18], tempMavTerrainReport);
			}
			break;

		case msg_scaled_pressure2.MAVLINK_MSG_ID_SCALED_PRESSURE2:
			msg_scaled_pressure2  mavScaledPressure2;
			if (mavMessage2 instanceof msg_scaled_pressure2) 
			{
				mavScaledPressure2 = (msg_scaled_pressure2) mavMessage2;
				String tempScaledPressure2 = "["
						+ mavScaledPressure2.time_boot_ms + "] , "
						+ "ABSOLUTE PRESSURE : " + mavScaledPressure2.press_abs
						* 100.0 + "Pascal , " + "DIFFERENTIAL PRESSURE 1 : "
						+ mavScaledPressure2.press_diff * 100 + "Pascal , "
						+ "TEMPERATURE : " + mavScaledPressure2.temperature
						/ 100.0 + "degree Cesius ";
				Map<String, Object> tempMavScaledPressure2 = Maps.newHashMap();
				tempMavScaledPressure2.put("data",
						"MAVLINK_MSG_ID_SCALED_PRESSURE2 - "
								+ tempScaledPressure2);
				sendOutputJson(publishers[2], tempMavScaledPressure2);
				getLog().debug(tempScaledPressure2);
				
				tempMavScaledPressure2.clear();
				tempMavScaledPressure2.put("pressure2", tempScaledPressure2);
				sendOutputJson(publishers[11], tempMavScaledPressure2);
			}
			break;

		case msg_att_pos_mocap.MAVLINK_MSG_ID_ATT_POS_MOCAP:
			msg_att_pos_mocap  mavAttPosMocap;
			if (mavMessage2 instanceof msg_att_pos_mocap) 
			{
				mavAttPosMocap = (msg_att_pos_mocap) mavMessage2;
				String tempAttPosMocap = "[" + mavAttPosMocap.time_usec
						+ "] , " + "QUATERNION COMPONENT 1 : "
						+ mavAttPosMocap.q[1] + " , "
						+ "QUATERNION COMPONENT 2 : " + mavAttPosMocap.q[2]
						+ " , " + "QUATERNION COMPONENT 3 : "
						+ mavAttPosMocap.q[3] + " , "
						+ "QUATERNION COMPONENT 4 : " + mavAttPosMocap.q[4]
						+ " , " + "X : " + mavAttPosMocap.x + "metres , "
						+ "Y : " + mavAttPosMocap.y + "metres , " + "Z : "
						+ mavAttPosMocap.z + "metres";
				Map<String, Object> tempMavAttPosMocap = Maps.newHashMap();
				tempMavAttPosMocap.put("data",
						"MAVLINK_MSG_ID_ATT_POS_MOCAP - " + tempAttPosMocap);
				sendOutputJson(publishers[2], tempMavAttPosMocap);
				getLog().debug(tempAttPosMocap);
			}
			break;

		case msg_set_actuator_control_target.MAVLINK_MSG_ID_SET_ACTUATOR_CONTROL_TARGET:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_actuator_control_target.MAVLINK_MSG_ID_ACTUATOR_CONTROL_TARGET:
			/**
			 * Actuator controls. Normed to -1..+1 where 0 is neutral position.
			 * Throttle for single rotation direction motors is 0..1, negative
			 * range for reverse direction. Standard mapping for attitude
			 * controls (group 0): (index 0-7): roll, pitch, yaw, throttle,
			 * flaps, spoilers, airbrakes, landing gear. Load a pass-through
			 * mixer to repurpose them as generic outputs.
			 */
			msg_actuator_control_target mavActuatorControlTarget;
			if (mavMessage2 instanceof msg_actuator_control_target) 
			{
				mavActuatorControlTarget = (msg_actuator_control_target) mavMessage2;
				String[] actuatorMapping = { "ROLL", "PITCH", "YAW",
						"THROTTLE", "FLAPS", "SPOILERS", "AIRBRAKES",
						"LANDING GEAR" };
				String tempActuatorControlTarget = "["
						+ mavActuatorControlTarget.time_usec + " ] , "
						+ actuatorMapping[0] + "CONTROL : "
						+ mavActuatorControlTarget.controls[0] + " , "
						+ actuatorMapping[1] + "CONTROL : "
						+ mavActuatorControlTarget.controls[1] + " , "
						+ actuatorMapping[2] + "CONTROL : "
						+ mavActuatorControlTarget.controls[2] + " , "
						+ actuatorMapping[3] + "CONTROL : "
						+ mavActuatorControlTarget.controls[3] + " , "
						+ actuatorMapping[4] + "CONTROL : "
						+ mavActuatorControlTarget.controls[4] + " , "
						+ actuatorMapping[5] + "CONTROL : "
						+ mavActuatorControlTarget.controls[5] + " , "
						+ actuatorMapping[6] + "CONTROL : "
						+ mavActuatorControlTarget.controls[6] + " , "
						+ actuatorMapping[7] + "CONTROL : "
						+ mavActuatorControlTarget.controls[7] + " , "
						+ "GROUP MIX : " + mavActuatorControlTarget.group_mlx;
				Map<String, Object> tempMavActuatorControlTarget = Maps
						.newHashMap();
				tempMavActuatorControlTarget.put("data",
						"MAVLINK_MSG_ID_ACTUATOR_CONTROL_TARGET - "
								+ tempActuatorControlTarget);
				sendOutputJson(publishers[2], tempMavActuatorControlTarget);
				getLog().debug(tempActuatorControlTarget);
			}
			break;

		case msg_battery_status.MAVLINK_MSG_ID_BATTERY_STATUS:
			msg_battery_status mavBatteryStatus;
			if (mavMessage2 instanceof msg_battery_status) 
			{
				mavBatteryStatus = (msg_battery_status) mavMessage2;
				String tempBatteryStatus = "CHARGE CONSUMED : "
						+ mavBatteryStatus.current_consumed + " mAh , "
						+ "ENERGY CONSUMED : "
						+ mavBatteryStatus.energy_consumed / 100.0
						+ "Joules , " + "TEMPERATURE : "
						+ mavBatteryStatus.temperature + "degree Celsius , "
						+ "VOLTAGES : "
						+ Arrays.toString(mavBatteryStatus.voltages) + "mV , "
						+ "BATTERY CURRENT : "
						+ mavBatteryStatus.current_battery / 10.0 + "mA , "
						+ "BATTERY ID : " + mavBatteryStatus.id
						+ "BATTERY FUNCTION : "
						+ mavBatteryStatus.battery_function + "BATTERY TYPE : "
						+ mavBatteryStatus.type + "REMAINING BATTERY : "
						+ mavBatteryStatus.battery_remaining + "%";
				Map<String, Object> tempMavBatteryStatus = Maps.newHashMap();
				tempMavBatteryStatus.put("data",
						"MAVLINK_MSG_ID_BATTERY_STATUS - " + tempBatteryStatus);
				sendOutputJson(publishers[2], tempMavBatteryStatus);
				getLog().debug(tempBatteryStatus);
			}
			break;

		case msg_autopilot_version.MAVLINK_MSG_ID_AUTOPILOT_VERSION:
			Map<String,	Object> tempMavAutopilotVersion= Maps.newHashMap();
			tempMavAutopilotVersion.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavAutopilotVersion);
			getLog().debug(mavMessage2.toString());
			break;

		case msg_landing_target.MAVLINK_MSG_ID_LANDING_TARGET:
			msg_landing_target mavLandingTarget;
			if (mavMessage2 instanceof msg_landing_target) 
			{
				mavLandingTarget = (msg_landing_target) mavMessage2;
				String tempLandingTarget = "ANGLE X : "
						+ mavLandingTarget.angle_x + "rad , " + "ANGLE Y : "
						+ mavLandingTarget.angle_y + "rad , " + "DISTANCE : "
						+ mavLandingTarget.distance + "metres , "
						+ "TARGET ID : " + mavLandingTarget.target_num + " , "
						+ "FRAME : "
						+ getVariableName("MAV_FRAME", mavLandingTarget.frame);
				Map<String, Object> tempMavLandingTarget = Maps.newHashMap();
				tempMavLandingTarget.put("data",
						"MAVLINK_MSG_ID_LANDING_TARGET - " + tempLandingTarget);
				sendOutputJson(publishers[2], tempMavLandingTarget);
				getLog().debug(tempLandingTarget);
			}
			break;

		case msg_v2_extension.MAVLINK_MSG_ID_V2_EXTENSION:
			/*
			 * Not a message receive case
			 */
			break;

		case msg_memory_vect.MAVLINK_MSG_ID_MEMORY_VECT:
			/**
			 * Send raw controller memory. The use of this message is
			 * discouraged for normal packets, but a quite efficient way for
			 * testing new messages and getting experimental debug output.
			 */
			msg_memory_vect mavMemoryVect;
			if (mavMessage2 instanceof msg_memory_vect) 
			{
				mavMemoryVect = (msg_memory_vect) mavMessage2;
				String[] memoryVectType = { "16 x int16_t", "16 x uint16_t",
						"16 x Q15", "16 x 1Q14" };
				String tempMemoryVect = "STARTING ADDRESS : "
						+ mavMemoryVect.address + " , " + "VERSION : "
						+ mavMemoryVect.ver + " , " + "TYPE : "
						+ memoryVectType[mavMemoryVect.type] + " , "
						+ "VALUE : " + mavMemoryVect.value;
				/**
				 * Type code of the memory variables. for ver = 1: 0=16 x
				 * int16_t, 1=16 x uint16_t, 2=16 x Q15, 3=16 x 1Q14
				 */
				Map<String, Object> tempMavMemoryVect = Maps.newHashMap();
				tempMavMemoryVect.put("data", "MAVLINK_MSG_ID_MEMORY_VECT - "
						+ tempMemoryVect);
				sendOutputJson(publishers[2], tempMavMemoryVect);
				getLog().debug(tempMemoryVect);
			}
			break;

		case msg_debug_vect.MAVLINK_MSG_ID_DEBUG_VECT:
			msg_debug_vect  mavDebugVect;
			if (mavMessage2 instanceof msg_debug_vect) 
			{
				mavDebugVect = (msg_debug_vect) mavMessage2;
				String tempDebugVect = "[" + mavDebugVect.time_usec + "] , "
						+ "X : " + mavDebugVect.x + "metres , " + "Y : "
						+ mavDebugVect.y + "metres , " + "Z : "
						+ mavDebugVect.z + "metres , " + "NAME : "
						+ Arrays.toString(mavDebugVect.name);
				Map<String, Object> tempMavDebugVect = Maps.newHashMap();
				tempMavDebugVect.put("data", "MAVLINK_MSG_ID_DEBUG_VECT - "
						+ tempDebugVect);
				sendOutputJson(publishers[2], tempMavDebugVect);
				getLog().debug(tempDebugVect);
			}
			break;

		case msg_named_value_float.MAVLINK_MSG_ID_NAMED_VALUE_FLOAT:
			Map<String,	Object> tempMavNamedValueFloat= Maps.newHashMap();
			tempMavNamedValueFloat.put("data", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavNamedValueFloat);
			getLog().debug(mavMessage2.toString());
			break;

		case msg_named_value_int.MAVLINK_MSG_ID_NAMED_VALUE_INT:
			Map<String,	Object> tempMavNamedValueInt= Maps.newHashMap();
			tempMavNamedValueInt.put("data", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavNamedValueInt);
			getLog().debug(mavMessage2.toString());
			break;

		case msg_statustext.MAVLINK_MSG_ID_STATUSTEXT:
			/*
			 * Status text message. These messages are printed in yellow in the
			 * COMM console of QGroundControl. WARNING: They consume quite some
			 * bandwidth, so use only for important status and error messages.
			 * If implemented wisely, these messages are buffered on the MCU and
			 * sent only at a limited rate (e.g. 10 Hz).
			 */
			msg_statustext  mavStatusText;
			if (mavMessage2 instanceof msg_statustext) 
			{
				mavStatusText = (msg_statustext) mavMessage2;
				String tempStatusText = "SEVERITY : "
						+ getVariableName("MAV_SEVERITY",
								mavStatusText.severity) + " , " + "TEXT : "
						+ new String (mavStatusText.text);
				Map<String, Object> tempMavStatusText = Maps.newHashMap();
				tempMavStatusText.put("data", "MAVLINK_MSG_ID_STATUSTEXT - "
						+ tempStatusText);
				sendOutputJson(publishers[2], tempMavStatusText);
				getLog().info(tempStatusText);
			}
			break;

		case msg_debug.MAVLINK_MSG_ID_DEBUG:
			msg_debug  mavDebug;
			if (mavMessage2 instanceof msg_debug) 
			{
				mavDebug = (msg_debug) mavMessage2;
				String tempDebug = "[" + mavDebug.time_boot_ms + "] , "
						+ "VALUE : " + mavDebug.value + " , " + "INDEX : "
						+ mavDebug.ind;
				Map<String, Object> tempMavDebug = Maps.newHashMap();
				tempMavDebug.put("data", "MAVLINK_MSG_ID_DEBUG - " + tempDebug);
				sendOutputJson(publishers[2], tempMavDebug);
				getLog().debug(tempDebug);
			}
			break;

		default:
			break;
		}
		
	}

	/**
	 * It sends a START message to the waypoint activity, denoting the start of
	 * a send mission sequence. Function overload for
	 * {@link #sendMissionListStart(byte, byte)}. Calls
	 * {@link #sendMissionListStart(byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @return <code>true</code> if sending of the mission send list command
	 *         is successful; <code>false</code> otherwise.
	 */
	private boolean sendMissionListStart()
	{
		return sendMissionListStart(targetSystem, targetComponent);
	}
	
	/**
	 * It sends a START message to the waypoint activity, denoting the start of
	 * a send mission sequence.it also sets the global {@link #tempTSystem} and
	 * {@link #tempTComponent} with the values asked by the user. It waits for
	 * the mission acknowledgement message from the drone, it it returns success
	 * then true is returned.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return <code>true</code> if sending of the mission send list command
	 *         is successful; <code>false</code> otherwise.
	 */
	private boolean sendMissionListStart(byte tSystem, byte tComponent)
	{
		String tempMissionRequest = "START";
		Map<String, Object> tempMapMissionRequest = Maps.newHashMap();
		tempMapMissionRequest.put("mission", tempMissionRequest);
		sendOutputJson(publishers[1], tempMapMissionRequest);
		getLog().debug(tempMissionRequest);

		tempTSystem = tSystem;
		tempTComponent = tComponent;

		Date start = new Date();
		int retry = 3;
		boolean result = false;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis())
					&& sendMissionCount != -1)
			{
				if (retry > 0)
				{
					result = sendMissionListCount(sendMissionCount, tSystem,
							tComponent);
					getLog().info(
							"SENDING MISSION COUNT, ATTEMPTS LEFT : " + retry);
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on send mission list");
					return false;
				}
			}
			if (result)
			{
				// getLog().info("Successfully get a mission request message");
				break;
			}
		}

		start = new Date();
		while ((System.currentTimeMillis() - start.getTime()) < 1000
				&& sendMissionAck == -1)
			;
		tempTSystem = 0;
		tempTComponent = 0;
		if (sendMissionAck == 0)
		{
			sendMissionAck = -1;
			getLog().info("Sent Mission File successfully");
			return true;
		}
		else if (sendMissionAck == -1)
		{
			getLog().warn("Timeout on Mission Acknowledgement read");
			return false;
		}
		else
		{
			getLog().error("Error : Could not write mission file");
			sendMissionAck = -1;
			return false;
		}
	}
	
	/**
	 * Function overload for {@link #sendMissionListCount(short, byte, byte)}.
	 * Calls {@link #sendMissionListCount(short, byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}
	 * 
	 * @param count
	 *            Number of mission items in the mission file.
	 * @return <code>true</code> if the sending of the mission count command is
	 *         successful and the activity receives a waypoint request;
	 *         <code>false</code> otherwise.
	 */
	@SuppressWarnings("unused")
	private boolean sendMissionListCount(short count)
	{
		return sendMissionListCount(count,targetSystem,targetComponent);
	}
	
	/**
	 * It sends a mission count message to the communications activity to be
	 * sent to th drone, denoting the start of a send mission sequence.it also
	 * sets the global {@link #missionRequestFlag} to false initially and on
	 * successful receive of waypoint request message it sets it to false.
	 * 
	 * 
	 * @param count
	 *            Number of mission items in the mission file.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return <code>true</code> if the sending of the mission count command is
	 *         successful and the activity receives a waypoint request;
	 *         <code>false</code> otherwise.
	 */
	private boolean sendMissionListCount(short count, byte tSystem, byte tComponent)
	{
		missionRequestFlag = false;
		sendMissionCount = count;
		msg_mission_count missionStart = new msg_mission_count();
		missionStart.count = count;
		missionStart.target_system = tSystem;
		missionStart.target_component = tComponent;
		byte tempByte[] = missionStart.pack().encodePacket();
		Map<String, Object> tempMapMission = Maps.newHashMap();
		tempMapMission.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempMapMission);
		getLog().info("SENDING COUNT : "+ missionStart.count);
		getLog().info("TARGET SYSTEM : " + targetSystem +" TARGET COMPONENT : " + targetComponent);
		
		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempMapMission);
					getLog().info("SENDING MISSION LIST AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on send mission list");
					return false;
				}
			}
			if (missionRequestFlag)
			{
				getLog().info("Successfully get a mission request message");
				missionRequestFlag = false;
				return true;
			}
		}
		/*
		 * It will receive a mission request message after this
		 */
	}
	
	/**
	 * Read the waypoint data from the mission file locally with this sequence number.
	 * 
	 * @param seq
	 *            Seq number of the waypoint to read in mission file.
	 */
	private void readMissionFile(short seq)
	{
		String request = "MISSION_REQUEST-"
				+ Short.toString(seq);
		
		Map<String, Object> tempRequest = Maps.newHashMap();
		tempRequest.put("mission", request);
		sendOutputJson(publishers[1], tempRequest);
		if (seq == 0)
		{
			missionRequestFlag = true;
		}
	}
	
	/**
	 * It sends a mission item message to the communications activity to be sent
	 * to th drone, after getting a waypoint request.
	 * 
	 * 
	 * @param missionWP
	 *            Waypoint data from the mission file.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 */
	private void sendMissionItem(String [] missionWP, byte tSystem, byte tComponent)
	{

		/*
		 * Format
		 * QGC WPL <VERSION> 
		 * <INDEX> <CURRENT WP> <COORD FRAME><COMMAND> <PARAM1> <PARAM2> <PARAM3> <PARAM4><PARAM5/X/LONGITUDE> <PARAM6/Y/LATITUDE> <PARAM7/Z/ALTITUDE><AUTOCONTINUE> 
		 * 
		 * Example
		 * QGC WPL 110 
		 * 0 1 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
		 * 1 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
		 * 2 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1
		 */
		
		msg_mission_item missionItem = new msg_mission_item();
		missionItem.seq = Short.parseShort(missionWP[0]);
		missionItem.current = Byte.parseByte(missionWP[1]);
		missionItem.frame = Byte.parseByte(missionWP[2]);
		missionItem.command = Short.parseShort(missionWP[3]);
		missionItem.param1 = Float.parseFloat(missionWP[4]);
		missionItem.param2 = Float.parseFloat(missionWP[5]);
		missionItem.param3 = Float.parseFloat(missionWP[6]);
		missionItem.param4 = Float.parseFloat(missionWP[7]);
		missionItem.x = Float.parseFloat(missionWP[8]);
		missionItem.y = Float.parseFloat(missionWP[9]);
		missionItem.z = Float.parseFloat(missionWP[10]);
		missionItem.autocontinue =Byte.parseByte(missionWP[11]);
		missionItem.target_system = tSystem;
		missionItem.target_component = tComponent;
		byte tempByte[] = missionItem.pack().encodePacket();
		Map<String, Object> tempMapMission = Maps.newHashMap();
		tempMapMission.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempMapMission);
		getLog().info("SENDING MISSION ITEM: " + missionItem.seq);
		getLog().info(missionItem.toString()); 
	}

	/**
	 * Function overload for {@link #readMissionListStart(byte, byte)}. Calls
	 * {@link #readMissionListStart(byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @return <code>true</code> if the sending of the mission request list
	 *         command is successful; <code>false</code> otherwise and even
	 *         timeout.
	 */
	private boolean readMissionListStart()
	{
		return readMissionListStart(targetSystem, targetComponent);
	}

	/**
	 * It sends a request list command to the drone to read all the mission file
	 * on the drone. It then waits for the drone to send a waypoint count
	 * message by checking for {@link #readWaypointCount} value. If it is not -1
	 * then some waypoint count message has been received. If it doesn't
	 * receieve the message it will retry for three times after which it will
	 * timeout and return a false.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return <code>true</code> if the sending of the mission request list command
	 *         is successful; <code>false</code> otherwise and even timeout.
	 */
	private boolean readMissionListStart(byte tSystem, byte tComponent)
	{
		msg_mission_request_list reqMissionList = new msg_mission_request_list();
		reqMissionList.target_component = tComponent;
		reqMissionList.target_system = tSystem;
		byte tempByte[] = reqMissionList.pack().encodePacket();
		Map<String, Object> tempReadMission = Maps.newHashMap();
		tempReadMission.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempReadMission);
		getLog().debug(
				"SENDING READ START SEQUENCE : " + Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempReadMission);
					getLog().info("REQUESTING GET MISSION LIST AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on request list");
					return false;
				}
			}
			if (!(readWaypointCount == -1))
			{
				getLog().info("Successfully get a waypoint count message");
				return true;
			}
		}
		/*
		 * It will receive a mission count message after this
		 */
	}

	/**
	 * Function overload for {@link #setMissionCount(short, byte, byte)}. Calls
	 * {@link #setMissionCount(short, byte, byte)} with {@link #targetSystem}
	 * and {@link #targetComponent}
	 * 
	 * @param count
	 *            Number of waypoint data stored on the drone.
	 * @return <code>true</code> if the sending of the waypoint request command
	 *         is successful; <code>false</code> otherwise and even timeout.
	 */
	private boolean setMissionCount(short count)
	{
		return setMissionCount(count, targetSystem, targetComponent);
	}

	/**
	 * It sets the value of {@link #readWaypointCount} with that received from
	 * the drone. It sends a mission request command to the drone to read the
	 * zeroth waypoint data on the drone. It then waits for the drone to send a
	 * mission item message after which it return true, otherwise false.
	 * 
	 * @param count
	 *            Number of waypoint data stored on the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return <code>true</code> if the sending of the waypoint request command
	 *         is successful; <code>false</code> otherwise and even timeout.
	 */
	private boolean setMissionCount(short count, byte tSystem, byte tComponent)
	{
		/*
		 * Called by mission count message receive case
		 */
		readWaypointCount = count;
		readWaypointList = Collections.synchronizedList(new ArrayList<String []>(count));
		return sendWPRequest((short) 0, tSystem, tComponent);
	}

	/**
	 * Function overload for {@link #sendWPRequest(short, byte, byte)}. Calls
	 * {@link #sendWPRequest(short, byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @param i
	 *            Request this waypoint data from the drone.
	 * @return <code>true</code> if the sending of the waypoint request command
	 *         is successful and the activity receives a valid waypoint message;
	 *         <code>false</code> otherwise and even timeout.
	 */
	@SuppressWarnings("unused")
	private boolean sendWPRequest(short i)
	{
		return sendWPRequest(i, targetSystem, targetComponent);
	}

	/**
	 * It sends a waypoint request to the drone for ith waypoint data. It then
	 * checks whether a waypoint dat ahas been received or not by checking the
	 * size of {@link #readWaypointList}. If it increased by one, that means a
	 * waypoint data has been received and then it returns a true value,
	 * otherwise it return false.
	 * 
	 * @param i
	 *            Request this waypoint data from the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return <code>true</code> if the sending of the waypoint request command
	 *         is successful and the activity receives a valid waypoint message;
	 *         <code>false</code> otherwise and even timeout.
	 */
	private boolean sendWPRequest(short i, byte tSystem, byte tComponent)
	{
		/*
		 * Called by setMissionCount and updateReadWaypointList
		 */
		msg_mission_request reqWaypoint = new msg_mission_request();
		reqWaypoint.seq = i;
		reqWaypoint.target_component = tComponent;
		reqWaypoint.target_system = tSystem;
		byte tempByte[] = reqWaypoint.pack().encodePacket();
		Map<String, Object> tempReadMission = Maps.newHashMap();
		tempReadMission.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempReadMission);
		getLog().debug(
				"SENDING WAYPOINT REQUEST : " + "[" + i + "]"
						+ Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempReadMission);
					getLog().info("SENDING WAYPOINT REQUEST AGAIN ");
					start = new Date();
					retry--;
					//getLog().info(readWaypointList.size() + "  " + i);
					continue;
				}
				else
				{
					getLog().error("Timeout on waypoint read");
					return false;
				}
			}
			if (readWaypointList.size()>= (i + 1))
			{
				getLog().info("Successfully get waypoint data");
				return true;
			}
		}
		/*
		 * It will receive a mission item message after this
		 */
	}

	/**
	 * Updates the {@link #readWaypointList} with the new mission item received.
	 * Function overload for
	 * {@link #updateReadWPList(msg_mission_item, byte, byte)}. Calls
	 * {@link #updateReadWPList(msg_mission_item, byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}
	 * 
	 * @param mavMissionItem
	 *            Mission data received from the drone.
	 */
	private void updateReadWPList(msg_mission_item mavMissionItem)
	{
		updateReadWPList(mavMissionItem, targetSystem, targetComponent);
	}

	/**
	 * Updates the {@link #readWaypointList} with the new mission item received.
	 * It is called after receiving a mission item from the drone. If the
	 * expected nuber of the mission items have been received then, it sends a
	 * mission acknowledgement message otherwise it sends a waypoint request
	 * message to get the next mission item.
	 * 
	 * @param mavMissionItem
	 *            Mission data received from the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 */
	private void updateReadWPList(msg_mission_item mavMissionItem,
			byte tSystem, byte tComponent)
	{
		/*
		 * Called after a mission_item message case
		 */
		String[] tempWP = new String[12];
		tempWP[0] = Short.toString(mavMissionItem.seq);
		tempWP[1] = Byte.toString(mavMissionItem.current);
		tempWP[2] = Byte.toString(mavMissionItem.frame);
		tempWP[3] = Short.toString(mavMissionItem.command);
		tempWP[4] = Float.toString(mavMissionItem.param1);
		tempWP[5] = Float.toString(mavMissionItem.param2);
		tempWP[6] = Float.toString(mavMissionItem.param3);
		tempWP[7] = Float.toString(mavMissionItem.param4);
		tempWP[8] = Float.toString(mavMissionItem.x);
		tempWP[9] = Float.toString(mavMissionItem.y);
		tempWP[10] = Float.toString(mavMissionItem.z);
		tempWP[11] = Byte.toString(mavMissionItem.autocontinue);
		if (!readWaypointList.isEmpty())
		{
			if (!readWaypointList.get(readWaypointList.size()-1)[0].equals(tempWP[0]))
			{
				readWaypointList.add(tempWP);
			}
		}
		else
		{
			readWaypointList.add(tempWP);
		}
		if (readWaypointCount == (mavMissionItem.seq + 1))
		{
			sendMissionAck((byte) MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED);
			readWaypointCount = -1;
			//getLog().info(Arrays.deepToString(readWaypointList.toArray()));
			/*
			 * If it is the last waypoint, send an acknowledgement message
			 */
		}
		else
		{
			sendWPRequest((short) (mavMissionItem.seq + 1), tSystem, tComponent);
			/*
			 * Other wise request the next waypoint
			 */
		}
	}

	/**
	 * Sends an acknowledgement data to the drone saying the mission receive was
	 * successful or failure due to some reasons.
	 * Function overload for {@link #sendMissionAck(byte, byte, byte)}. Calls
	 * {@link #sendMissionAck(byte, byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @param ackType
	 *            Acknowledgement type to be sent.
	 * @see MAV_MISSION_RESULT
	 */
	private void sendMissionAck(byte ackType)
	{
		sendMissionAck(ackType, targetSystem, targetComponent);
	}

	/**
	 * Send an acknowledgement data to the drone saying the mission receive was
	 * successful or failure due to some reasons.
	 * 
	 * @param ackType
	 *            Acknowledgement type to be sent.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @see MAV_MISSION_RESULT
	 */
	private void sendMissionAck(byte ackType, byte tSystem, byte tComponent)
	{
		msg_mission_ack missionAck = new msg_mission_ack();
		missionAck.target_component = tComponent;
		missionAck.target_system = tSystem;
		missionAck.type = ackType;
		byte tempByte[] = missionAck.pack().encodePacket();
		Map<String, Object> tempMissionAck = Maps.newHashMap();
		tempMissionAck.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempMissionAck);
		getLog().debug(
				"SENDING MISSION ACKNOWLEDGEMENT : "
						+ Arrays.toString(tempByte));
		/*
		 * No Response from drone in response to this
		 */
	}

	/**
	 * Sets the current mission sequence on the drone to the specified sequence.
	 * Function overload for {@link #setCurrentActiveWP(short, byte, byte)}.
	 * Calls {@link #setCurrentActiveWP(short, byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}
	 * 
	 * @param currentSequence
	 *            Current mission sequence to be set(made active) on the drone.
	 * @return It checks for the {@link #missionCurrentSeq} and if it is equal
	 *         to the currentSequence, it returns a <code>true</code>, otherwise
	 *         it times out after 3 retries and returns <code>false</code>.
	 */
	private boolean setCurrentActiveWP(short currentSequence)
	{
		return setCurrentActiveWP(currentSequence, targetSystem, targetComponent);
	}

	/**
	 * Sets the current mission sequence on the drone to the specified sequence.
	 * It means that, the drone will start executing that particular mission
	 * sequence stored on the mission file.
	 * 
	 * @param currentSequence
	 *            Current mission sequence to be set(made active) on the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #missionCurrentSeq} and if it is equal
	 *         to the currentSequence, it returns a <code>true</code>, otherwise
	 *         it times out after 3 retries and returns <code>false</code>.
	 */
	private boolean setCurrentActiveWP(short currentSequence, byte tSystem,
			byte tComponent)
	{
		msg_mission_set_current missionCurrent = new msg_mission_set_current();
		missionCurrent.seq = currentSequence;
		missionCurrent.target_system = tSystem;
		missionCurrent.target_component = tComponent;
		byte tempByte[] = missionCurrent.pack().encodePacket();
		Map<String, Object> tempMissionWPCurrent = Maps.newHashMap();
		tempMissionWPCurrent.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempMissionWPCurrent);
		getLog().debug(
				"SENDING MISSION CURRENT WAYPOINT SET : "
						+ Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 5;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempMissionWPCurrent);
					getLog().info("SENDING MISSION CURRENT WAYPOINT SET AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on set current active Waypoint");
					return false;
				}
			}
			if (currentSequence == missionCurrentSeq)
			{
				getLog().info("Successfully set current active Waypoint");
				return true;
			}
		}
		/*
		 * It will receive a Mission Current message after it
		 */
	}

	/**
	 * Clears all the mission data on the drone.
	 * Function overload for {@link #clearMissionList(byte, byte)}.
	 * Calls {@link #clearMissionList(byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}
	 * 
	 * @return It checks for the {@link #isMissionCleared} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise
	 *         it times out after 3 retries and returns <code>false</code>.
	 */
	private boolean clearMissionList()
	{
		return clearMissionList(targetSystem, targetComponent);
	}

	/**
	 * Clears all the mission data on the drone.
	 *
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #isMissionCleared} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise
	 *         it times out after 3 retries and returns <code>false</code>.
	 */
	private boolean clearMissionList(byte tSystem, byte tComponent)
	{
		msg_mission_clear_all missionClear = new msg_mission_clear_all();
		missionClear.target_component = tComponent;
		missionClear.target_system = tSystem;
		isMissionCleared = true;
		byte tempByte[] = missionClear.pack().encodePacket();
		Map<String, Object> tempMissionClear = Maps.newHashMap();
		tempMissionClear.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempMissionClear);
		getLog().debug(
				"SENDING MISSION CURRENT WAYPOINT SET : "
						+ Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempMissionClear);
					getLog().info("SENDING MISSION CURRENT WAYPOINT SET AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on clear Mission list");
					return false;
				}
			}
			if (!isMissionCleared)
			{
				getLog().info("Successfully clear Mission list");
				return true;
			}
		}
	}

	/**
	 * It arms/disarms the drone.
	 * Function overload for {@link #doARM(boolean, byte, byte)}. Calls
	 * {@link #doARM(boolean, byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @param armit
	 *            True for arming the drone, false for disarming the drone.
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doARM(boolean armit)
	{
		return doARM(armit, targetSystem, targetComponent);
	}

	/**
	 * It arms/disarms the drone.
	 * It calls a doCommand function with
	 * {@link MAV_CMD#MAV_CMD_COMPONENT_ARM_DISARM}. Be careful with this
	 * function, it can arm/disarm without any safety checks meaning the drone
	 * can get disarmed in air too!
	 * 
	 * @param armit
	 *            True for arming the drone, false for disarming the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doARM(boolean armit, byte tSystem, byte tComponent)
	{
		return doCommand((short) MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM, armit ? 1 : 0,
				21196, 0, 0, 0, 0, 0, tSystem, tComponent);
	}

	/**
	 * Function overload for
	 * {@link #doCommand(short, float, float, float, float, float, float, float, byte, byte)}
	 * . Calls
	 * {@link #doCommand(short, float, float, float, float, float, float, float, byte, byte)}
	 * with {@link #targetSystem} and {@link #targetComponent}
	 * 
	 * @param actionid
	 *            {@link MAV_CMD} class has all the commands to send.
	 * @param p1
	 *            Parameter 1
	 * @param p2
	 *            Parameter 2
	 * @param p3
	 *            Parameter 3
	 * @param p4
	 *            Parameter 4
	 * @param p5
	 *            Parameter 5
	 * @param p6
	 *            Parameter 6
	 * @param p7
	 *            Parameter 7
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doCommand(short actionid, float p1, float p2, float p3,
			float p4, float p5, float p6, float p7)
	{
		return doCommand(actionid, p1, p2, p3, p4, p5, p6, p7, targetSystem, targetComponent);
	}

	/**
	 * Sends a command to the drone from a list of commands in the
	 * {@link MAV_CMD} class.
	 * 
	 * @param actionid
	 *            {@link MAV_CMD} class has all the commands to send.
	 * @param p1
	 *            Parameter 1
	 * @param p2
	 *            Parameter 2
	 * @param p3
	 *            Parameter 3
	 * @param p4
	 *            Parameter 4
	 * @param p5
	 *            Parameter 5
	 * @param p6
	 *            Parameter 6
	 * @param p7
	 *            Parameter 7
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doCommand(short actionid, float p1, float p2, float p3,
			float p4, float p5, float p6, float p7, byte tSystem,
			byte tComponent)
	{
		msg_command_long req = new msg_command_long();

		req.target_system = tSystem;
		req.target_component = tComponent;

		req.command = (short) actionid;

		req.param1 = p1;
		req.param2 = p2;
		req.param3 = p3;
		req.param4 = p4;
		req.param5 = p5;
		req.param6 = p6;
		req.param7 = p7;
		byte tempByte[] = req.pack().encodePacket();
		Map<String, Object> tempCommand = Maps.newHashMap();
		tempCommand.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempCommand);
		isCommandSent = true;
		getLog().debug("SENDING COMMAND : " + Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempCommand);
					getLog().info("SENDING COMMAND AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on send command");
					return false;
				}
			}
			if (!isCommandSent)
			{
				getLog().info("Successfully send command");
				return true;
			}
		}
		/*
		 * It will Receive command acknowledgment message case after this
		 */
	}

	/**
	 * This function reads all the parameters stored on the drone. WARNING -
	 * Never call this function when the drone is in air. Function overload for
	 * {@link #readParameterListStart(byte, byte)} . Calls
	 * {@link #readParameterListStart(byte, byte)} with {@link #targetSystem}
	 * and {@link #targetComponent}
	 * 
	 * @return It checks for {@link #receiveParamList} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean readParameterListStart()
	{
		return readParameterListStart(targetSystem, targetComponent);
	}

	/**
	 * This function reads all the parameters stored on the drone. WARNING -
	 * Never call this function when the drone is in air. Function overload for
	 * {@link #readParamList(byte, byte)} . Calls
	 * {@link #readParamList(byte, byte)} with {@link #targetSystem}
	 * and {@link #targetComponent}
	 * 
	 * @return It checks for {@link #receiveParamList} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	@SuppressWarnings("unused")
	private boolean readParamList()
	{
		return readParamList(targetSystem, targetComponent);
	}

	/**
	 * Gets a single Parameter from the drone with the given index. WARNING -
	 * Never call this function when the drone is in air. Function overload for
	 * {@link #readParam(short, byte, byte)} . Calls
	 * {@link #readParam(short, byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @param index
	 *            The index of parameter to be requested.
	 * @return It checks for {@link #receiveParam} value and if it is equal to
	 *         false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	@SuppressWarnings("unused")
	private boolean readParam(short index)
	{
		return readParam(index, targetSystem, targetComponent);
	}

	/**
	 * Gets a single Parameter from the drone with a given string id. WARNING -
	 * Never call this function when the drone is in air. Function overload for
	 * {@link #readParam(String, byte, byte)} . Calls
	 * {@link #readParam(String, byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @param id
	 *            The string id of parameter to be requested.
	 * @return It checks for {@link #receiveParam} value and if it is equal to
	 *         false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	@SuppressWarnings("unused")
	private boolean readParam(String id)
	{
		return readParam(id, targetSystem, targetComponent);
	}

	/**
	 * This function starts reading all the parameters stored on the drone. WARNING -
	 * Never call this function when the drone is in air. It initialises all the
	 * maps used for storing the parameters with a Concurrent HashMap.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for {@link #receiveParamList} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean readParameterListStart(byte tSystem, byte tComponent)
	{
		paramList = new ConcurrentHashMap<String, Double>(600);
		paramType = new ConcurrentHashMap<String, Byte>(600);
		paramIndex = 0;
		paramTotal = 1;
		receiveParamList = true;
		return readParamList(tSystem, tComponent);
	}

	/**
	 * This function reads all the parameters stored on the drone. WARNING -
	 * Never call this function when the drone is in air. This function should
	 * not be called before the readParameterListStart function as it
	 * initializes the critical components to be used.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for {@link #receiveParamList} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean readParamList(byte tSystem, byte tComponent)
	{
		if (paramList == null || paramType == null)
		{
			getLog().error(
					"Use readParameterListStart function instead of this");
			return false;
		}
		msg_param_request_list req = new msg_param_request_list();
		req.target_component = tComponent;
		req.target_system = tSystem;
		byte tempByte[] = req.pack().encodePacket();
		Map<String, Object> tempParameterList = Maps.newHashMap();
		tempParameterList.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempParameterList);
		getLog().debug(
				"REQUESTING PARAMETER LIST : " + Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (paramList.isEmpty())
			{
				if (!((start.getTime() + 17000) > System.currentTimeMillis()))
				{
					if (retry > 0)
					{
						sendOutputJson(publishers[0], tempParameterList);
						getLog().info("REQUESTING GET PARAMETER LIST AGAIN ");
						start = new Date();
						retry--;
						continue;
					}
					else
					{
						getLog().error("Timeout on get Parameter List");
						return false;
					}
				}
			}
			if (!receiveParamList)
			{
				getLog().info("Successfully get parameter list");
				return true;
			}
		}
		/*
		 * A sequence of Parameter value messages will be received after it
		 */
	}

	/**
	 * Gets a single Parameter from the drone with a given index. WARNING -
	 * Never call this function when the drone is in air. Gets the parameter
	 * with this index from the drone.
	 * 
	 * @param index
	 *            The index of parameter to be requested.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for {@link #receiveParam} value and if it is equal to
	 *         false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	private boolean readParam(short index, byte tSystem, byte tComponent)
	{
		msg_param_request_read req = new msg_param_request_read();
		req.target_system = tSystem;
		req.target_component = tComponent;
		req.param_index = index;
		req.param_id = new byte[16];
		byte tempByte[] = req.pack().encodePacket();
		Map<String, Object> tempParameter = Maps.newHashMap();
		tempParameter.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempParameter);
		getLog().debug("REQUESTING PARAMETER : " + Arrays.toString(tempByte));
		receiveParam = true;

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempParameter);
					getLog().info("REQUESTING GET PARAMETER AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on get Parameter");
					return false;
				}
			}
			if (!receiveParam)
			{
				getLog().info("Successfully get parameter");
				return true;
			}
		}
		/*
		 * A Parameter value message will be received after it
		 */
	}

	/**
	 * Gets a single Parameter from the drone with a given string id. WARNING -
	 * Never call this function when the drone is in air. Gets the parameter
	 * with this String from the drone.
	 * 
	 * @param id
	 *            The string id of parameter to be requested.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for {@link #receiveParam} value and if it is equal to
	 *         false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	private boolean readParam(String id, byte tSystem, byte tComponent)
	{
		msg_param_request_read req = new msg_param_request_read();
		req.target_system = tSystem;
		req.target_component = tComponent;
		req.param_index = -1;
		req.param_id = Arrays
				.copyOf(id.getBytes(StandardCharsets.US_ASCII), 16);
		byte tempByte[] = req.pack().encodePacket();
		Map<String, Object> tempParameter = Maps.newHashMap();
		tempParameter.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempParameter);
		getLog().debug("REQUESTING PARAMETER : " + Arrays.toString(tempByte));
		receiveParam = true;

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempParameter);
					getLog().info("REQUESTING GET PARAMETER AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on get Parameter");
					return false;
				}
			}
			if (!receiveParam)
			{
				getLog().info("Successfully get parameter");
				return true;
			}
		}
		/*
		 * A Parameter value message will be received after it
		 */
	}

	/**
	 * Saves the parameter into the map after checking whether 
	 * {@link #readParamList(byte, byte)} has been called or just
	 * {@link #readParam(String, byte, byte)} has been called. Based on it
	 * calculates whether all the parameter data has been successfully received
	 * or not.
	 * 
	 * @param paramValue Parameter value message from the drone.
	 */
	private void saveParam(msg_param_value paramValue)
	{
		if (receiveParamList)
		{
			paramIndex++;
			String paramID = (new String(paramValue.param_id)).split("\0", 2)[0];
			paramList.put(paramID, (double) paramValue.param_value);
			paramType.put(paramID, paramValue.param_type);
			paramTotal = paramValue.param_count;
			if ((paramTotal - 1) == (paramIndex))
			{
				getLog().info("Received all the parameters successfully");
				receiveParamList = false;
			}
			else
			{
				// readParam(paramIndex);
			}
		}
		else
		{
			String paramID = (new String(paramValue.param_id)).split("\0", 2)[0];
			paramList.put(paramID, (double) paramValue.param_value);
			paramType.put(paramID, paramValue.param_type);
			receiveParam = false;
		}

	}
	
	/**
	 * Returns the parameter value from the {@link #paramList} map associated
	 * with the String Id.
	 * 
	 * @param ID
	 *            Id of parameter requested.
	 * @return Value of the parameter Id in the {@link #paramList} map.
	 */
	public double getParam(String ID)
	{
		return paramList.get(ID);
	}
	
	/**
	 * Returns the {@link #paramList} map
	 * 
	 * @return {@link #paramList}
	 */
	public Map<String , Double> getParamList()
	{
		return paramList;
	}

	/**
	 * Sets the parameter with the given string id on the default drone. WARNING
	 * - Never call this function when the drone is in air. Function overload
	 * for {@link #setParam(String, float, byte, byte)} . Calls
	 * {@link #setParam(String, float, byte, byte)} with {@link #targetSystem}
	 * and {@link #targetComponent}
	 * 
	 * @param pID
	 *            String Id of the parameter to be set.
	 * @param pValue
	 *            Value of the Parameter ID to be set on the drone.
	 * @return It checks for the parameter's value in {@link #paramList} and
	 *         if it is equal to the input, it return a <code>true</code>,
	 *         otherwise it times out after 3 retries and returns
	 *         <code>false</code>.
	 */
	private boolean setParam(String pID, float pValue)
	{
		return setParam(pID, pValue, targetSystem, targetComponent);
	}

	/**
	 * Sets the parameter with the given string id on the drone. It checks for
	 * the updated values from the {@link #paramList} map and when it gets a
	 * match with the input paramter values. Otherwise, it times out retrying
	 * and returns false.
	 * 
	 * @param pID
	 *            String Id of the parameter to be set.
	 * @param pValue
	 *            Value of the Parameter ID to be set on the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the parameter's value in {@link #paramList} and
	 *         if it is equal to the input, it returns a <code>true</code>,
	 *         otherwise it times out after 3 retries and returns
	 *         <code>false</code>.
	 */
	private boolean setParam(String pID, float pValue, byte tSystem,
			byte tComponent)
	{
		if (paramList.containsKey(pID))
		{
			Map<String, Object> tempParameterSet;
			msg_param_set req = new msg_param_set();
			req.target_component = tComponent;
			req.target_system = tSystem;
			req.param_value = pValue;
			req.param_id = Arrays.copyOf(
					pID.getBytes(StandardCharsets.US_ASCII), 16);
			req.param_type = paramType.get(pID);
			byte tempByte[] = req.pack().encodePacket();
			tempParameterSet = Maps.newHashMap();
			tempParameterSet.put("comm", Arrays.toString(tempByte));
			sendOutputJson(publishers[0], tempParameterSet);
			getLog().debug(
					"REQUESTING SET PARAMETER : " + Arrays.toString(tempByte));

			Date start = new Date();
			int retry = 3;
			while (true)
			{
				if (!((start.getTime() + 700) > System.currentTimeMillis()))
				{
					if (retry > 0)
					{
						sendOutputJson(publishers[0], tempParameterSet);
						getLog().debug("REQUESTING SET PARAMETER AGAIN ");
						start = new Date();
						retry--;
						continue;
					}
					else
					{
						getLog().error("Timeout on set Parameter");
						return false;
					}
				}
				if (paramList.get(pID) == pValue)
				{
					getLog().info("Successfully set parameter");
					return true;
				}
			}
		}
		else
		{
			getLog().warn("No such parameter on the drone");
			return false;
		}
		/**
		 * Set a parameter value TEMPORARILY to RAM. It will be reset to default
		 * on system reboot. Send the ACTION MAV_ACTION_STORAGE_WRITE to
		 * PERMANENTLY write the RAM contents to EEPROM. IMPORTANT: The
		 * receiving component should acknowledge the new parameter value by
		 * sending a param_value message to all communication partners. This
		 * will also ensure that multiple GCS all have an up-to-date list of all
		 * parameters. If the sending GCS did not receive a PARAM_VALUE message
		 * within its timeout time, it should re-send the PARAM_SET message.
		 */

	}
	
	/**
	 * It reboots the autopilot. WARNING - Never call this function when the
	 * drone is in air. Function overload for
	 * {@link #doRebootAutopilot(byte, byte)} . Calls
	 * {@link #doRebootAutopilot(byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doRebootAutopilot()
    {
		return doRebootAutopilot(targetSystem, targetComponent);
    }

	/**
	 * It reboots the autopilot. WARNING - Never call this function when the
	 * drone is in air. It calls the
	 * {@link #doCommand(short, float, float, float, float, float, float, float, byte, byte)}
	 * function with a {@link MAV_CMD#MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN} option
	 * and other parameters as specified in its comments.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	private boolean doRebootAutopilot(byte tSystem, byte tComponent)
    {
        int param1 = 1;
        boolean result=true;
        if (tSystem != 0 && tComponent != 0)
        {
            result = doCommand((short) MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, param1, 0, 0, 0, 0, 0, 0,tSystem,tComponent);
        }
        else
        {

            for (short a = 0; a < 255; a++)
            {
               result &= doCommand((short)MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, param1, 0, 0, 0, 0, 0, 0,(byte)a,(byte) 0);
            }
        }
        return result;
    }
	
	/**
	 * It shuts down the autopilot. WARNING - Never call this function when the
	 * drone is in air. Function overload for
	 * {@link #doRebootAutopilot(byte, byte)} . Calls
	 * {@link #doRebootAutopilot(byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doShutdownAutopilot()
    {
		return doShutdownAutopilot(targetSystem, targetComponent);
    }
	
	/**
	 * It shuts down the autopilot. WARNING - Never call this function when the
	 * drone is in air. It calls the
	 * {@link #doCommand(short, float, float, float, float, float, float, float, byte, byte)}
	 * function with a {@link MAV_CMD#MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN} option
	 * and other parameters as specified in its comments.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	private boolean doShutdownAutopilot(byte tSystem, byte tComponent)
    {
        int param1 = 2;
        boolean result = true;
        if (tSystem != 0 && tComponent != 0)
        {
            result = doCommand((short) MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, param1, 0, 0, 0, 0, 0, 0,tSystem,tComponent);
        }
        else
        {

            for (short a = 0; a < 255; a++)
            {
                result &= doCommand((short)MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, param1, 0, 0, 0, 0, 0, 0,(byte)a,(byte) 0);
            }
        }
        return result;
    }

	/**
	 * It reboots the system in bootloader mode so that new image can be
	 * uploaded. WARNING - Never call this function when the drone is in air.
	 * Function overload for {@link #doBootloaderReboot(byte, byte)} . Calls
	 * {@link #doBootloaderReboot(byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doBootloaderReboot()
    {
		return doBootloaderReboot(targetSystem, targetComponent);
    }

	/**
	 * It reboots the system in bootloader mode so that new image can be
	 * uploaded. WARNING - Never call this function when the drone is in air. It
	 * calls the
	 * {@link #doCommand(short, float, float, float, float, float, float, float, byte, byte)}
	 * function with a {@link MAV_CMD#MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN} option
	 * and other parameters as specified in its comments.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	private boolean doBootloaderReboot(byte tSystem, byte tComponent)
    {
        int param1 = 3;
        boolean result = true;
        if (tSystem != 0 && tComponent != 0)
        {
            result = doCommand((short) MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, param1, 0, 0, 0, 0, 0, 0,tSystem,tComponent);
        }
        else
        {

            for (short a = 0; a < 255; a++)
            {
                result &= doCommand((short)MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, param1, 0, 0, 0, 0, 0, 0,(byte)a,(byte) 0);
            }
        }
        return result;
    }

	/**
	 * It reboots the entire system. WARNING - Never call this function when the
	 * drone is in air. Function overload for
	 * {@link #doSystemReboot(byte, byte)} . Calls
	 * {@link #doSystemReboot(byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it return a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doSystemReboot()
    {
		return doSystemReboot(targetSystem, targetComponent);
    }

	/**
	 * It reboots the entire system. WARNING - Never call this function when the
	 * drone is in air. It calls the
	 * {@link #doCommand(short, float, float, float, float, float, float, float, byte, byte)}
	 * function with a {@link MAV_CMD#MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN} option
	 * and other parameters as specified in its comments.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	private boolean doSystemReboot(byte tSystem, byte tComponent)
    {
        int param2 = 1;
        boolean result = true;
        if (tSystem != 0 && tComponent != 0)
        {
            result =  doCommand((short) MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, 0, param2, 0, 0, 0, 0, 0,tSystem,tComponent);
        }
        else
        {

            for (short a = 0; a < 255; a++)
            {
                result &= doCommand((short)MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, 0, param2, 0, 0, 0, 0, 0,(byte)a,(byte) 0);
            }
        }
        return result;
    }
	
	/**
	 * It shuts down the entire system. WARNING - Never call this function when
	 * the drone is in air. Function overload for
	 * {@link #doSystemShutdown(byte, byte)} . Calls
	 * {@link #doSystemShutdown(byte, byte)} with {@link #targetSystem} and
	 * {@link #targetComponent}
	 * 
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean doSystemShutdown()
    {
		return doSystemShutdown(targetSystem, targetComponent);
    }

	/**
	 * It shuts down the entire system. WARNING - Never call this function when
	 * the drone is in air. It calls the
	 * {@link #doCommand(short, float, float, float, float, float, float, float, byte, byte)}
	 * function with a {@link MAV_CMD#MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN} option
	 * and other parameters as specified in its comments.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times out
	 *         after 3 retries and returns <code>false</code>.
	 */
	private boolean doSystemShutdown(byte tSystem, byte tComponent)
    {
        int param2 = 2;
        boolean result = true;
        if (tSystem != 0 && tComponent != 0)
        {
            result = doCommand((short) MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, 0, param2, 0, 0, 0, 0, 0,tSystem,tComponent);
        }
        else
        {

            for (short a = 0; a < 255; a++)
            {
                result &= doCommand((short)MAV_CMD.MAV_CMD_PREFLIGHT_REBOOT_SHUTDOWN, 0, param2, 0, 0, 0, 0, 0,(byte)a,(byte) 0);
            }
        }
        return result;
    }
	
	/**
	 * It sets the flight mode of the default drone. Function overload for
	 * {@link #setMode(String, byte)} . Calls {@link #setMode(String, byte)}
	 * with {@link #targetSystem}.
	 * 
	 * @param mode
	 *            Flight mode to be set on the drone.
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean setMode(String mode)
	{
		return setMode(mode, targetSystem);
	}

	/**
	 * It sets the flight mode of the target drone. It first looks for the
	 * numerical mapping value of the flight mode specified in the Parameter.xml
	 * file. After that it sends a set mode command to the target system for
	 * which it receives an acknowledgement.
	 * 
	 * @param mode
	 *            Flight mode to be set on the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @return It checks for the {@link #isCommandSent} value and if it is equal
	 *         to false, it returns a <code>true</code>, otherwise it times
	 *         out after 3 retries and returns <code>false</code>.
	 */
	private boolean setMode(String mode, byte tSystem)
	{
		Map<String, Short> modeMap = dataXML.getParamOptions("FLTMODE1",
				"ArduCopter2");
		//getLog().info(modeMap);
		if (modeMap.containsKey(mode))
		{
			msg_set_mode req = new msg_set_mode();
			req.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED;
			req.target_system = tSystem;
			req.custom_mode = modeMap.get(mode);
			byte tempByte[] = req.pack().encodePacket();
			Map<String, Object> tempModeSet;
			tempModeSet = Maps.newHashMap();
			tempModeSet.put("comm", Arrays.toString(tempByte));
			sendOutputJson(publishers[0], tempModeSet);
			getLog().debug("REQUESTING SET MODE : " + Arrays.toString(tempByte));
			//sendOutputJson(publishers[0], tempModeSet);
			
			isCommandSent = true;

			Date start = new Date();
			int retry = 3;
			while (true)
			{
				if (!((start.getTime() + 700) > System.currentTimeMillis()))
				{
					if (retry > 0)
					{
						sendOutputJson(publishers[0], tempModeSet);
						getLog().info("REQUESTING SET MODE AGAIN ");
						start = new Date();
						retry--;
						continue;
					}
					else
					{
						getLog().error("Timeout on set mode command");
						return false;
					}
				}
				if (!isCommandSent)
				{
					getLog().info("Successfully send set mode command");
					return true;
				}
			}
			/*
			 * It will Receive command acknowledgment message case after this
			 */
		}
		else
		{
			getLog().warn("Mode type not found in the Parameter file");
		}
		return false;
	}
	
	/**
	 * Saves the allowed area message in {@link #allowedArea}.
	 * 
	 * @param allowed
	 *            Allowed area message from the drone.
	 */
	private void saveAllowedArea(msg_safety_allowed_area allowed)
	{
		Point3D tempMin= new Point3D(allowed.p1x, allowed.p1y, allowed.p1z);
		Point3D tempMax = new Point3D(allowed.p2x, allowed.p2y, allowed.p2z);
		allowedArea = new MinMaxPair<Point3D>(tempMin,tempMax);
		allowedAreaFrame = allowed.frame;
	}
	
	/**
	 * Sets the allowed area of the drone. Function overload for
	 * {@link #setAllowedArea(Point3D, Point3D, byte, byte, byte)} . Calls
	 * {@link #setAllowedArea(Point3D, Point3D, byte, byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}
	 * 
	 * @param minLatLongAlt
	 *            South West Front Corner of the box you want to limit your
	 *            drone to.
	 * @param maxLatLongAlt
	 *            North East Back Corner of the box you want to limit your drone
	 *            to.
	 * @param frame
	 *            Frame of reference of the above coordinates.
	 * @return It compares the {@link #allowedArea} value with the input values
	 *         and if they are equal, it returns a <code>true</code>, otherwise it
	 *         times out after 3 retries and returns <code>false</code>.
	 */
	private boolean setAllowedArea(Point3D minLatLongAlt,
			Point3D maxLatLongAlt, byte frame)
	{
		return setAllowedArea(minLatLongAlt, maxLatLongAlt, frame, targetSystem, targetComponent);
	}

	/**
	 * Sets the allowed area of the drone. It sends a safety set allowed area
	 * message to the drone with two diagonally opposite corners of a box the
	 * drone is supposed to fly in.
	 * 
	 * @param minLatLongAlt
	 *            South West Front Corner of the box you want to limit your
	 *            drone to.
	 * @param maxLatLongAlt
	 *            North East Back Corner of the box you want to limit your drone
	 *            to.
	 * @param frame
	 *            Frame of reference of the above coordinates.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return It compares the {@link #allowedArea} value with the input values
	 *         and if they are equal, it returns a <code>true</code>, otherwise
	 *         it times out after 3 retries and returns <code>false</code>.
	 */
	private boolean setAllowedArea(Point3D minLatLongAlt,
			Point3D maxLatLongAlt, byte frame, byte tSystem, byte tComponent)
	{
		MinMaxPair<Point3D> prevAllowedArea = allowedArea;
		allowedArea = null;
		msg_safety_set_allowed_area req = new msg_safety_set_allowed_area();
		req.target_system = tSystem;
		req.target_component = tComponent;
		req.p1x = (float) minLatLongAlt.getX();
		req.p1y = (float) minLatLongAlt.getY();
		req.p1z = (float) minLatLongAlt.getZ();
		req.p2x = (float) maxLatLongAlt.getX();
		req.p2y = (float) maxLatLongAlt.getY();
		req.p2z = (float) maxLatLongAlt.getZ();
		req.frame = frame;
		Map<String, Object> tempAllowedAreaSet;
		byte tempByte[] = req.pack().encodePacket();
		tempAllowedAreaSet = Maps.newHashMap();
		tempAllowedAreaSet.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempAllowedAreaSet);
		getLog().debug(
				"REQUESTING SET SAFETY AREA : " + Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempAllowedAreaSet);
					getLog().debug("REQUESTING SET SAFETY AREA AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on set safety area");
					allowedArea = prevAllowedArea;
					return false;
				}
			}
			if (allowedArea != null)
			{
				if (allowedArea.getMin().equals(minLatLongAlt)
						&& allowedArea.getMax().equals(maxLatLongAlt))
				{
					getLog().info("Successfully set safety area");
					return true;
				}
			}
		}
	}
	
	/**
	 * It injects GPS data into the drone. Function overload for
	 * {@link #injectGpsData(byte[], int, byte, byte)} . Calls
	 * {@link #injectGpsData(byte[], int, byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}
	 * 
	 * @param data
	 *            Data array to be inserted into the drone as GPS data.
	 * @param length
	 *            Length of this data array.
	 * 
	 */
	@SuppressWarnings("unused")
	private void injectGpsData(byte[] data, int length)
	{
		injectGpsData(data, length, targetSystem, targetComponent);
	}
	
	/**
	 * It injects GPS data into the drone from an external source like a rtk
	 * gps.
	 * 
	 * @param data
	 *            Data array to be inserted into the drone as GPS data.
	 * @param length
	 *            Length of this data array.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 */
	private void injectGpsData(byte[] data, int length, byte tSystem,
			byte tComponent)
	{
		msg_gps_inject_data req = new msg_gps_inject_data();
		req.data = new byte[110];
		req.len = (byte) length;
		req.data = Arrays.copyOf(data, length);
		req.target_system = tSystem;
		req.target_component = tComponent;
		Map<String, Object> tempInjectGpsData;
		byte tempByte[] = req.pack().encodePacket();
		tempInjectGpsData = Maps.newHashMap();
		tempInjectGpsData.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempInjectGpsData);
		getLog().debug("INJECTING GPS DATA : " + Arrays.toString(tempByte));
	}
	
	/**
	 * Saves Global GPS Origin received from the drone
	 * {@link #globalGpsOrigin}.
	 * 
	 * @param msg
	 *            Global GPS Origin message from the drone.
	 */
	private void saveGlobalGpsOrigin(msg_gps_global_origin msg)
	{
		float tempLat = (float) (msg.latitude / 10000000.0);
		float tempLon = (float) (msg.longitude / 10000000.0);
		float tempAlt = (float) (msg.altitude / 10000000.0);
		globalGpsOrigin = new Point3D(tempLon, tempLat, tempAlt);
	}

	/**
	 * It sets Global GPS Origin of the drone. Function overload for
	 * {@link #setGlobalGpsOrigin(Point3D, byte)} . Calls
	 * {@link #setGlobalGpsOrigin(Point3D, byte)} with {@link #targetSystem}.
	 * 
	 * @param latLonAlt
	 *            GPS Origin to be set.
	 * @return It compares the {@link #globalGpsOrigin} value with the input
	 *         value and if they are equal , it returns a <code>true</code>,
	 *         otherwise it times out after 3 retries and returns
	 *         <code>false</code>.
	 * 
	 */
	private boolean setGlobalGpsOrigin(Point3D latLonAlt)
	{
		return setGlobalGpsOrigin(latLonAlt, targetSystem);
	}

	/**
	 * It sets Global GPS Origin of the drone. It sends a
	 * {@link msg_set_gps_global_origin} message to the drone with the given
	 * coordinates and then waits for the {@link #globalGpsOrigin} values to be
	 * set.
	 * 
	 * @param latLonAlt
	 *            GPS Origin to be set.
	 * @param tSystem
	 *            Target system of the drone.
	 * @return It compares the {@link #globalGpsOrigin} value with the input
	 *         value and if they are equal , it returns a <code>true</code>,
	 *         otherwise it times out after 3 retries and returns
	 *         <code>false</code>.
	 * 
	 */
	private boolean setGlobalGpsOrigin(Point3D latLonAlt, byte tSystem)
	{
		Point3D prevGlobalGpsOrigin = globalGpsOrigin;
		globalGpsOrigin = null;
		msg_set_gps_global_origin req = new msg_set_gps_global_origin();
		req.longitude = (int) (latLonAlt.getX() * 10000000);
		req.latitude = (int) (latLonAlt.getY() * 10000000);
		req.altitude = (int) (latLonAlt.getZ() * 1000);
		req.target_system = tSystem;
		Map<String, Object> tempGlobalGpsOrigin;
		byte tempByte[] = req.pack().encodePacket();
		tempGlobalGpsOrigin = Maps.newHashMap();
		tempGlobalGpsOrigin.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempGlobalGpsOrigin);
		getLog().debug(
				"SETTING GLOBAL GPS ORIGIN : " + Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempGlobalGpsOrigin);
					getLog().debug("REQUESTING SET GLOBAL GPS ORIGIN AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on set global gps origin");
					globalGpsOrigin = prevGlobalGpsOrigin;
					return false;
				}
			}
			if (globalGpsOrigin != null)
			{
				if (globalGpsOrigin.equals(latLonAlt))
				{
					getLog().info("Successfully set global gps origin");
					return true;
				}
			}
		}
	}
	
	/**
	 * It gets Log entries stored on the default drone. Function overload for
	 * {@link #getLogEntry(short, short, byte, byte)} . Calls
	 * {@link #getLogEntry(short, short, byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}.
	 * 
	 * @param startno
	 *            Starting address of the log entries.
	 * @param end
	 *            Ending address of the Log entries.
	 * @return It compares the {@link #logEntry} size with the value received
	 *         from the drone and if they are equal , it returns a
	 *         <code>true</code>, otherwise it times out after 3 retries and
	 *         returns <code>false</code>.
	 * 
	 */
	private boolean getLogEntry(short startno, short end)
	{
		return getLogEntry(startno, end, targetSystem, targetComponent);
	}
	
	/**
	 * It gets all the Log entries stored on the drone. Function overload for
	 * {@link #getLogEntry(short, short, byte, byte)} . Calls
	 * {@link #getLogEntry(short, short, byte, byte)} with
	 * 0 and 0xFFFF.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return It compares the {@link #logEntry} size with the value received
	 *         from the drone and if they are equal , it returns a
	 *         <code>true</code>, otherwise it times out after 3 retries and
	 *         returns <code>false</code>.
	 * 
	 */
	@SuppressWarnings("unused")
	private boolean getLogEntry(byte tSystem, byte tComponent)
	{
		return getLogEntry((short)0, (short)0xFFFF, tSystem, tComponent);
	}
	
	/**
	 * Saves the log entries got from the drone.
	 * 
	 * @param entry
	 *            Log entry from the drone.
	 */
	private void saveLogEntry(msg_log_entry entry)
	{
		logEntry.add(entry);
	}

	/**
	 * It gets Log entries stored on the drone from the starting address to the
	 * ending address given.
	 * 
	 * @param startno
	 *            Starting address of the log entries.
	 * @param end
	 *            Ending address of the Log entries.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return It compares the {@link #logEntry} size with the value received
	 *         from the drone and if they are equal , it returns a
	 *         <code>true</code>, otherwise it times out after 3 retries and
	 *         returns <code>false</code>.
	 * 
	 */
	private boolean getLogEntry(short startno, short end, byte tSystem,
			byte tComponent)
	{
		int size = logEntry.size();
		msg_log_request_list req = new msg_log_request_list();
		req.start = startno;
		req.end = end;
		req.target_system = tSystem;
		req.target_component = tComponent;
		Map<String, Object> tempGetLogEntry;
		byte tempByte[] = req.pack().encodePacket();
		tempGetLogEntry = Maps.newHashMap();
		tempGetLogEntry.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempGetLogEntry);
		getLog().debug("GETTING LOG ENTRY : " + Arrays.toString(tempByte));

		Date start = new Date();
		int retry = 3;
		while (true)
		{
			if (!((start.getTime() + 700) > System.currentTimeMillis()))
			{
				if (retry > 0)
				{
					sendOutputJson(publishers[0], tempGetLogEntry);
					getLog().debug("REQUESTING GET LOG ENTRY AGAIN ");
					start = new Date();
					retry--;
					continue;
				}
				else
				{
					getLog().error("Timeout on get log entry");
					return false;
				}
			}
			if (logEntry.size() == (size + 1))
			{
				//getLog().info((int)(logEntry.get(0).id&0xFFFF) +"  " +  (int)(startno&0xFFFF)+"  "+(int)(end&0xFFFF));
				if ( ((int)(logEntry.get(size).id&0xFFFF) >= (int)(startno&0xFFFF))
						&& ((int)(logEntry.get(size).id&0xFFFF) <= (int)(end&0xFFFF)))
				{
					getLog().info("Successfully get log entry");
					return true;
				}
				else
				{
					getLog().warn("Did not get valid log entry");
					logEntry.remove(size);
					//return true;
				}
			}
		}
	}

	/**
	 * It gets all the Log entries stored on the default drone. Function overload for
	 * {@link #getLogEntry(short, short)} . Calls
	 * {@link #getLogEntry(short, short)} with 0 and 0xFFFF.
	 * 
	 * @return It compares the {@link #logEntry} size with the value received
	 *         from the drone and if they are equal , it returns a
	 *         <code>true</code>, otherwise it times out after 3 retries and
	 *         returns <code>false</code>.
	 * 
	 */
	@SuppressWarnings("unused")
	private boolean getLogEntry()
	{
		return getLogEntry((short)0, (short)0xFFFF);
	}
	
	/**
	 * Erases all the log entries on the default drone.Function overload for
	 * {@link #eraseLog(byte, byte)} . Calls {@link #eraseLog(byte, byte)} with
	 * {@link #targetSystem} and {@link #targetComponent}.
	 */
	@SuppressWarnings("unused")
	private void eraseLog()
	{
		eraseLog(targetSystem, targetComponent);
	}
	
	/**
	 * Erases all the Log entries on the drone.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 */
	private void eraseLog(byte tSystem, byte tComponent)
	{
		msg_log_erase req = new msg_log_erase();
		req.target_system = tSystem;
		req.target_component = tComponent;
		Map<String, Object> tempEraseLog;
		byte tempByte[] = req.pack().encodePacket();
		tempEraseLog = Maps.newHashMap();
		tempEraseLog.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempEraseLog);
		getLog().debug("ERASING LOG : " + Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempEraseLog);
	}
	
	/**
	 * It gets all the Log entries stored on the drone. Function overload for
	 * {@link #getLogList(byte, byte)} . Calls {@link #getLogList(byte, byte)}
	 * with {@link #targetSystem} and {@link #targetComponent}.
	 * 
	 * @return It compares the {@link #logEntry} size with the value received
	 *         from the drone and if they are equal , it returns a
	 *         <code>true</code>, otherwise it times out after 3 retries and
	 *         returns <code>false</code>.
	 * 
	 */
	private boolean getLogList()
	{
		return getLogList(targetSystem, targetComponent);
	}
	
	/**
	 * It gets all the Log entries stored on the drone.
	 * 
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 * @return It compares the {@link #logEntry} size with the value received
	 *         from the drone and if they are equal , it returns a
	 *         <code>true</code>, otherwise it times out after 3 retries and
	 *         returns <code>false</code>.
	 * 
	 */
	private boolean getLogList(byte tSystem, byte tComponent)
	{
		logEntry = new ArrayList<msg_log_entry>();
		if (getLogEntry((short) 0, (short) 0xffff, tSystem, tComponent))
		{
			//int lastLogNumber = logEntry.get(0).last_log_num;
			int logCount = logEntry.get(0).num_logs;
			/*
			 * logEntry.remove(0); for (int i = (lastLogNumber - logCount +1); i
			 * <= lastLogNumber; i++) { if (!getLogEntry((short) i,
			 * (short)i,tSystem , tComponent)) {
			 * getLog().warn("getLogEntry returned false"); return false; } }
			 */
			Date start = new Date();
			while (true)
			{
				if (logEntry.size() == logCount)
				{

					getLog().info("Successfully get log entry");
					//getLog().info(Arrays.toString(logEntry.toArray()));
					return true;
				}
				if ((System.currentTimeMillis() - start.getTime()) > 3000)
				{
					getLog().warn("Timeout on getting all log entry");
					return false;
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks whether the requested rate of data stream is achievable or not.
	 * 
	 * @param pps
	 *            packets per second of that stram.
	 * @param rate
	 *            requeste rate of the stream.
	 * @return <code>true</code> if the rate is achievable, otherwise <code>false</code>.
	 */
	@SuppressWarnings("unused")
	private boolean rateCheck(double pps, int rate)
	{
		if (pps == Double.POSITIVE_INFINITY || pps == Double.NEGATIVE_INFINITY)
		{
			return false;
		}
		else if (rate == 0 && pps == 0)
		{
			return true;
		}
		else if (rate == 1 && pps >= 0.5 && pps < 2)
		{
			return true;
		}
		else if (rate == 3 && pps >= 2 && pps < 5)
		{
			return true;
		}
		else if (rate == 10 && pps >= 5 && pps < 15)
		{
			return true;
		}
		else if (rate > 15 && pps >= 15)
		{
			return true;
		}
		return false;
	}

	/**
	 * It starts/stops a data stream from the drone like a gps data stream, imu data
	 * stream at a given rate.
	 * 
	 * @param id
	 *            Stream ID from {@link MAV_DATA_STREAM}
	 * @param rate
	 *            Requested rate of stream
	 * @param startStop
	 *            Start or stop the stream.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 */
	private void getDataStream(int id, int rate, boolean startStop,
			byte tSystem, byte tComponent)
	{
		msg_request_data_stream req = new msg_request_data_stream();
		req.target_system = tSystem;
		req.target_component = tComponent;
		req.req_stream_id = (byte) id;
		req.req_message_rate = (short) rate;
		req.start_stop = (byte) (startStop ? 1 : 0);
		Map<String, Object> tempRequestDataStream;
		byte tempByte[] = req.pack().encodePacket();
		tempRequestDataStream = Maps.newHashMap();
		tempRequestDataStream.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempRequestDataStream);
		getLog().debug("REQUESTING DATA STREAM : " + Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempRequestDataStream);
	}
	
	/**
	 * It gets a data stream from the drone like a gps data stream, imu data
	 * stream. Function overload for
	 * {@link #getDataStream(int, int, boolean, byte, byte)} . Calls
	 * {@link #getDataStream(int, int, boolean, byte, byte)} with true.
	 * 
	 * @param id
	 *            Stream ID from {@link MAV_DATA_STREAM}
	 * @param rate
	 *            Requested rate of stream
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 */
	private void getDataStream(int id, int rate, byte tSystem, byte tComponent)
	{
		getDataStream(id, rate, true, tSystem, tComponent);
	}

	/**
	 * It gets a data stream from the drone like a gps data stream, imu data
	 * stream. Function overload for
	 * {@link #getDataStream(int, int, byte, byte)} . Calls
	 * {@link #getDataStream(int, int, byte, byte)} with {@link #targetSystem}
	 * and {@link #targetComponent}.
	 * 
	 * @param id
	 *            Stream ID from {@link MAV_DATA_STREAM}
	 * @param rate
	 *            Requested rate of stream
	 */
	private void getDataStream(int id, int rate)
	{
		getDataStream(id, rate, targetSystem, targetComponent);
	}
	
	/**
	 * Sends a RC packet to the drone thus emulating a rc transmitter. Function overload for
	 * {@link #sendRCPacket(String[], byte, byte)} . Calls
	 * {@link #sendRCPacket(String[], byte, byte)} with {@link #targetSystem}
	 * and {@link #targetComponent}.
	 * 
	 * @param mesg
	 *            RC Message to be sent of length 8.
	 */
	private void sendRCPacket(String [] mesg)
	{
		sendRCPacket(mesg, targetSystem	, targetComponent);
	}
	
	/**
	 * Sends a RC packet to the drone thus emulating a rc transmitter.
	 * 
	 * @param mesg
	 *            RC Message to be sent of length 8.
	 * @param tSystem
	 *            Target system of the drone.
	 * @param tComponent
	 *            Target Component on the drone.
	 */
	private void sendRCPacket(String [] mesg, byte tSystem, byte tComponent)
	{
		if (mesg.length!=8)
		{
			getLog().warn("Input string array does not contain 8 values, aborting send");
			return;
		}
		msg_rc_channels_override req = new msg_rc_channels_override();
		req.target_system = tSystem;
		req.target_component = tComponent ;
		req.chan1_raw = Short.parseShort(mesg[0]);
		req.chan2_raw = Short.parseShort(mesg[1]);
		req.chan3_raw = Short.parseShort(mesg[2]);
		req.chan4_raw = Short.parseShort(mesg[3]);
		req.chan5_raw = Short.parseShort(mesg[4]);
		req.chan6_raw = Short.parseShort(mesg[5]);
		req.chan7_raw = Short.parseShort(mesg[6]);
		req.chan8_raw = Short.parseShort(mesg[7]);
		
		Map<String, Object> tempRCPacketSend;
		byte tempByte[] = req.pack().encodePacket();
		tempRCPacketSend = Maps.newHashMap();
		tempRCPacketSend.put("comm", Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempRCPacketSend);
		getLog().debug("SENDING RC PACKET TO THE DRONE : " + Arrays.toString(tempByte));
		sendOutputJson(publishers[0], tempRCPacketSend);
	}
}



