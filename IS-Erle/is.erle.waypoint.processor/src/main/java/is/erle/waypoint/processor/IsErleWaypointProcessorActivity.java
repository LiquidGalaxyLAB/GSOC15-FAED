package is.erle.waypoint.processor;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.concurrency.ManagedCommand;

/**
 * 
 * This activity will take a text file from FAED Mesh data. This activity will
 * act as a bridge between waypoint reader and FAED Mesh. The text file will
 * keep checking the temporary data directory of the controller for a mission
 * file. Once the file is found/updated, it sends start signal to the captain
 * activity. The file changed check is done at an interval of 1 second.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 */
public class IsErleWaypointProcessorActivity extends BaseRoutableRosActivity {

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
	 */
	private static String subscribers[];
	
	/**
	 * File name constant.
	 */
	private static final String FILE_NAME = "mission.txt";
	
	/**
	 * Contains the directory of the file associated with FILE_NAME.
	 */
	private static String fileWithDirectory;
	
	/**
	 * A thread to see the file changed status. It keeps seeing the file for any
	 * change in it.
	 */
	@SuppressWarnings("unused")
	private ManagedCommand fileThread;
	
	/**
	 * Date when the file was last modified.
	 */
	private Date lastModified;
	
	/**
	 * Executes on activity setup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivitySetup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.waypoint.processor setup");
        
        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        fileWithDirectory = getSpaceEnvironment().getFilesystem().getTempDirectory().getAbsolutePath()+"/"+FILE_NAME;
        
        getLog().info("Mission File Source : " + fileWithDirectory);
        
        fileThread= getManagedCommands().scheduleWithFixedDelay(new Runnable()
		{
			
			public void run()
			{
				File mission = new File(fileWithDirectory);
				if (mission.exists())
				{
					if(lastModified==null)
					{
						lastModified = new Date(mission.lastModified());
						sendFly();
					}
					else
					{
						Date current = new Date (mission.lastModified());
						if(!current.equals(lastModified))
						{
							sendFly();
						}
					}
				}
			}
		}, 20, 1, TimeUnit.SECONDS);
    }

	/**
	 * Executes on activity startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityStartup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.waypoint.processor startup");
    }

	/**
	 * Executes on activity post startup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPostStartup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.waypoint.processor post startup");
    }

	/**
	 * Executes on activity activate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityActivate()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.waypoint.processor activate");
    }

	/**
	 * Executes on activity deactivate.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityDeactivate()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.waypoint.processor deactivate");
    }

	/**
	 * Executes on activity pre shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityPreShutdown()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.waypoint.processor pre shutdown");
    }

	/**
	 * Executes on activity shutdown.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityShutdown()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.waypoint.processor shutdown");
    }

	/**
	 * Executes on activity cleanup.
	 * 
	 * @see interactivespaces.activity.impl.BaseActivity#onActivityCleanup()
	 * @since 1.0.0
	 */
    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.waypoint.processor cleanup");
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
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
    	if(subscribers[0].equals(channelName))
    	{
    		if (message.get("response").toString().equals("SENT"))
    		{
        		getLog().info("Sent to the drone");
    		}
    	}
    }
    
	/**
	 * Send FLY to captain activity. This make the captain activity send the
	 * mission file on the drone and then start flying.
	 * @since	1.0.0
	 */
    private void sendFly()
    {
    	String command = "FLY";
		Map<String, Object> fly = Maps.newHashMap();
		fly.put("fly", command);
		sendOutputJson(publishers[0], fly);
    }
}
