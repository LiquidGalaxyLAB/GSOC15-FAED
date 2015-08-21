package is.erle.mavlink;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class processes the ParameterData.xml file for the required information.
 * It has utility functions to read the xml file and then parse it into a map, a
 * string or a range of values.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 */
public class XMLParamParser
{
	/**
	 * The ParameterData.xml file.
	 */
	private File xmlFile;

	/**
	 * The ParameterData.xml document.
	 */
	private Document xmlDocument;
	
    public final String PARAMDELIMETER = "@";
    public final String PATHDELIMETER = ",";
    public final String PARAM = "Param";
    public final String GROUP = "Group";
    public final String PATH = "Path";
    
    public final String DISPLAYNAME = "DisplayName";
    public final String DESCRIPTION = "Description";
    public final String UNITS = "Units";
    public final String RANGE = "Range";
    public final String VALUES = "Values";
    public final String INCREMENT = "Increment";
    public final String USER = "User";
    public final String REBOOTREQUIRED = "RebootRequired";
    public final String BITMASK = "Bitmask";
    
    public final String ADVANCED = "Advanced";
    public final String STANDARD = "Standard";
    
	/**
	 * Get {@link #xmlFile}
	 * 
	 * @return {@link #xmlFile}
	 */
	public File getFile()
	{
		return xmlFile;
	}
	
	/**
	 * Get {@link #xmlDocument}
	 * 
	 * @return {@link #xmlDocument}
	 */
	public Document getDocument()
	{
		return xmlDocument;
	}
	
	/**
	 * Set {@link #xmlFile}
	 * 
	 * @param xmlFile
	 *            Value to set to {@link #xmlFile}
	 */
	public void setFile(File xmlFile)
	{
		this.xmlFile = xmlFile;
	}
	
	/**
	 * Set {@link #xmlDocument}
	 * 
	 * @param xmlDocument
	 *            Value to set to {@link #xmlDocument}
	 */
	public void setFile(Document xmlDocument)
	{
		this.xmlDocument = xmlDocument;
	}
	
	/**
	 * Constructor with xmlFile as input.
	 * 
	 * @param xmlFile
	 *            Value to set to {@link #xmlFile}
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public XMLParamParser(File xmlFile) throws SAXException, IOException, ParserConfigurationException
	{
		this.xmlFile = xmlFile ;
		xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this.xmlFile);
	}
	
	/**
	 * Constructor with xmlDocument as input.
	 * 
	 * @param xmlDocument
	 *            Value to set to {@link #xmlDocument}
	 */
	public XMLParamParser(Document xmlDocument)
	{
		this.xmlDocument = xmlDocument ;
	}
	
	/**
	 * It processes the {@link #xmlDocument} to return the string contained for
	 * a given nodeKey,metaKey and vehicleType
	 * 
	 * @param nodeKey
	 *            Name of the node queried like "FLTMODE1"
	 * @param metaKey
	 *            Name of the subnode or metakey inside this node whose value is
	 *            being queried.
	 * @param vehicleType
	 *            Type of the vehicle to look for this node.
	 * @return String contained within the metakey of the {@link #xmlDocument}
	 */
	@SuppressWarnings("unused")
	public String getParamDataXml(String nodeKey, String metaKey,
			String vehicleType)
	{
		if (xmlDocument.getDocumentElement().getNodeName().equals("Params"))
		{
			String value = null;
			Node vehicleNode = null;
			try
			{
				vehicleNode = xmlDocument.getElementsByTagName(vehicleType)
						.item(0);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			Element vehicle = null;
			if (vehicleNode.getNodeType() == Node.ELEMENT_NODE)
			{
				vehicle = (Element) vehicleNode;
			}
			NodeList nodeKeyList = null;
			try
			{
				nodeKeyList = vehicle.getElementsByTagName(nodeKey);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			for (int i = 0; i < nodeKeyList.getLength(); i++)
			{
				Node nodeMetaKey = nodeKeyList.item(i);
				Element elementMetaKey = null;
				if (nodeMetaKey.getNodeType() == Node.ELEMENT_NODE)
				{
					elementMetaKey = (Element) nodeMetaKey;
				}
				try
				{
					value = elementMetaKey.getElementsByTagName(metaKey)
							.item(0).getTextContent();
				}
				catch (DOMException e2)
				{
					e2.printStackTrace();
				}
				return value;
			}
		}
		return null;
	}
	
	/**
	 * It processes the {@link #xmlDocument} to return a HashMap containing the
	 * parameter options for a given nodeKey and vehicleType. The
	 * metakey used here is {@link #VALUES}
	 * 
	 * @param nodeKey
	 *            Name of the node queried like "FLTMODE1"
	 * @param vehicleType
	 *            Type of the vehicle to look for this node.
	 * @return Parameter options for the given node and vehicle type
	 *         {@link #xmlDocument}
	 */
	public HashMap<String, Short> getParamOptions(String nodeKey,
			String vehicleType)
	{
		HashMap<String, Short> valueMap = new HashMap<String, Short>();
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, VALUES, vehicleType);
			if (!(rawData.isEmpty()))
			{
				String[] values = rawData.split(",");
				for (int i = 0; i < values.length; i++)
				{
					try
					{
						String[] valuePart = values[i].split(":");
						valueMap.put(valuePart[1],
								Short.parseShort(valuePart[0].trim()));
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return valueMap;
	}
	
	/**
	 * It processes the {@link #xmlDocument} to return a HashMap containing the
	 * Bitmask parameter options for a given nodeKey and vehicleType. The
	 * metakey used here is {@link #BITMASK}
	 * 
	 * @param nodeKey
	 *            Name of the node queried
	 * @param vehicleType
	 *            Type of the vehicle to look for this node.
	 * @return Bitmask Parameter options for the given node and vehicle type
	 *         {@link #xmlDocument}
	 */
	public HashMap<String, Short> getParamBitMask(String nodeKey,
			String vehicleType)
	{
		HashMap<String, Short> valueMap = new HashMap<String, Short>();
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, BITMASK, vehicleType);
			if (!(rawData.isEmpty()))
			{
				String[] values = rawData.split(",");
				for (int i = 0; i < values.length; i++)
				{
					try
					{
						String[] valuePart = values[i].split(":");
						valueMap.put(valuePart[1],
								Short.parseShort(valuePart[0].trim()));
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return valueMap;
	}
	
	/**
	 * It processes the {@link #xmlDocument} to return whether a reboot is
	 * required or not. The metakey used here is {@link #REBOOTREQUIRED}
	 * 
	 * @param nodeKey
	 *            Name of the node queried
	 * @param vehicleType
	 *            Type of the vehicle to look for this node.
	 * @return <code>true</code> if reboot is required for the given node key;
	 *         otherwise <code>false</code>
	 */
	public boolean getParamRebootRequired(String nodeKey, String vehicleType)
	{
		boolean answer = false;
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, REBOOTREQUIRED, vehicleType);
			if (!(rawData.isEmpty()))
			{
				try
				{
					answer = Boolean.parseBoolean(rawData);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return answer;
	}

	/**
	 * It processes the {@link #xmlDocument} to return a pair containing the
	 * parameter range for a given nodeKey and vehicleType. The metakey used
	 * here is {@link #RANGE}
	 * 
	 * @param nodeKey
	 *            Name of the node queried
	 * @param vehicleType
	 *            Type of the vehicle to look for this node.
	 * @return Minimum and Maximum value of a parameter.
	 */
	public MinMaxPair<Float> getParamRange(String nodeKey, String vehicleType)
	{
		MinMaxPair<Float> pair = null;
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, RANGE, vehicleType);
			if (!(rawData.isEmpty()))
			{
				String[] values = rawData.split(" ");
				if (values.length == 2)
				{
					try
					{
						float min = Float.parseFloat(values[0].trim());
						float max = Float.parseFloat(values[1].trim());
						pair = new MinMaxPair<Float>(min, max);
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return pair;
	}
	
	
}