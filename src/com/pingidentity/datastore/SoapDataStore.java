
package com.pingidentity.datastore;

import java.io.*;
import java.util.*;

import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.conf.SimpleFieldList;
import org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;

import com.pingidentity.sources.CustomDataSourceDriver;
import com.pingidentity.sources.CustomDataSourceDriverDescriptor;
import com.pingidentity.sources.SourceDescriptor;
import com.pingidentity.sources.gui.FilterFieldsGuiDescriptor;

import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;


/**
 * Custom Data Store to make a SOAP call to return data.
 *
 * Example uses weather service web service to convert zipcode into lat + long
 */
 
public class SoapDataStore implements CustomDataSourceDriver
{
    private static final String CONFIG_PROPS_PATH = "Path to properties directory";
    private static final String FILTER_ZIPCODE = "ZipCode Filter";

    // A reference to our CustomDataSourceDriverDescriptor
    private final CustomDataSourceDriverDescriptor descriptor;

    // A list of fields that will be returned to the user, which can be selected and mapped
    // to an adapter contract.
    private static final List<String> listOfFields = new ArrayList<String>();

    static
    {
        listOfFields.add("latitude");
        listOfFields.add("longitude");
    }

    // Path to the directory containing all the properties files
    private String propertiesDirectory;

    public SoapDataStore()
    {

        FilterFieldsGuiDescriptor filterFieldsDescriptor = new FilterFieldsGuiDescriptor();
        filterFieldsDescriptor.addField(new TextFieldDescriptor(
                FILTER_ZIPCODE,
                "The zipcode filter"));

        // create the configuration descriptor for our custom data store
        AdapterConfigurationGuiDescriptor dataStoreConfigGuiDesc = new AdapterConfigurationGuiDescriptor(
                "Configuration settings for the custom data store.");

        descriptor = new CustomDataSourceDriverDescriptor(this, "SOAP Data Store",
                dataStoreConfigGuiDesc, filterFieldsDescriptor);
    }

    /**
     */
    @Override
    public SourceDescriptor getSourceDescriptor()
    {
        return descriptor;
    }

    /**
     */
    @Override
    public void configure(Configuration configuration)
    {
        // load the data store configuration settings from the Configuration object
        // nothing to load...
    }

    /**
     */
    @Override
    public boolean testConnection()
    {
        return true;
    }

    /**
     */
    @Override
    public Map<String, Object> retrieveValues(Collection<String> attributeNamesToFill,
            SimpleFieldList filterConfiguration)
    {
        String zipCode = filterConfiguration.getFieldValue(FILTER_ZIPCODE);
        Map<String, Object> results = new HashMap<String, Object>();

        try {
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Send SOAP Message to SOAP Server
            String url = "http://graphical.weather.gov/xml/SOAP_server/ndfdXMLserver.php";
            SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(zipCode), url);

            // Process the SOAP Response
    		String result = parseSOAPResponse(soapResponse);
    		String[] latlon = result.split(",");
    		results.put("latitude", latlon[0]);
    		results.put("longitude", latlon[1]);

            soapConnection.close();
        } catch (Exception e) {
            System.err.println("Error occurred while sending SOAP Request to Server");
            e.printStackTrace();
        }

        return results;
    }

    /**
     */
    @Override
    public List<String> getAvailableFields()
    {
        return listOfFields;
    }



// 	<soapenv:Header> 
// 		<platformMsgs:passport>  
// 			<platformCore:email>jdoe@netsuite.com</platformCore:email>  
// 			<platformCore:password>mypassword</platformCore:password>  
// 			<platformCore:account>000034</platformCore:account>  
// 			<platformCore:role internalId="3"/> 
// 		</platformMsgs:passport> 
// 	</soapenv:Header>
// 	<soap:Body>
// 	<platformMsgs:search>
// 	<searchRecord xsi:type="ContactSearch">
// 		<customerJoin xsi:type="CustomerSearchBasic">
// 			<email operator=”contains” xsi:type="platformCore:SearchStringField">
// 			<platformCore:searchValue>shutterfly.com</platformCore:searchValue>
// 			<email>
// 		<customerJoin>
// 	</searchRecord>
// 	</search>
// 	</soap:Body>


    private static SOAPMessage createSOAPRequest(String ZipCode) throws Exception {
    
        MessageFactory messageFactory = MessageFactory.newInstance();
        
        SOAPHeader
        
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        String serverURI = "http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl";

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("ndf", serverURI);

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapBodyElem = soapBody.addChildElement("LatLonListZipCode", "ndf");
        SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("zipCodeList", "ndf");
        soapBodyElem1.addTextNode(ZipCode);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", serverURI  + "LatLonListZipCode");

        soapMessage.saveChanges();

        return soapMessage;
    }

    /**
     * Method used to print the SOAP Response
     */
    private static String parseSOAPResponse(SOAPMessage soapResponse) throws Exception {
    
      	SOAPPart soapPart = soapResponse.getSOAPPart();
       	SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
       	SOAPBody soapBody = soapEnvelope.getBody();
        	
       	String innerXml = soapBody.getElementsByTagName("listLatLonOut").item(0).getTextContent();

       	DocumentBuilderFactory innerXmlDocBuilder = DocumentBuilderFactory.newInstance();
       	innerXmlDocBuilder.setNamespaceAware(true);
       	Document innerXmlDoc = innerXmlDocBuilder.newDocumentBuilder().parse(new ByteArrayInputStream(innerXml.getBytes()));

       	String LatLon = innerXmlDoc.getElementsByTagName("latLonList").item(0).getTextContent();
        	
       	return LatLon;
        	
	}

}
