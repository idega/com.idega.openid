package com.idega.openid.server.bean;

import is.idega.idegaweb.egov.citizen.wsclient.islandsbanki.BirtingakerfiWSLocator;
import is.idega.idegaweb.egov.citizen.wsclient.islandsbanki.BirtingakerfiWSSoap_PortType;
import is.idega.idegaweb.egov.citizen.wsclient.landsbankinn.SendLoginDataBusiness;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.rpc.ServiceException;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.client.Stub;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.encoding.Base64;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.ibm.icu.text.DecimalFormat;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBOService;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;
import com.idega.user.data.User;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;

@Service("bankMessageBean")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BankMessageBean implements CallbackHandler {
	
	private IWApplicationContext iwac;
	

	private static final String USE_LANDSBANKINN = "USE_LANDSBANKINN";
	private static final String BANK_SENDER_PIN = "bank.sender.pin";  //usually ssn
	private static final String BANK_SENDER_USER_ID = "bank.sender.user.id";
	private static final String BANK_SENDER_USER_PASSWORD = "bank.sender.user.pw";
	private static final String BANK_LETTER_STYLESHEET = "bank.letter.stylesheet";
	private static final String BANK_LETTER_STYLESHEET_VERSION = "bank.letter.stylesheet.version";
	private static final String PROPERTY_BANK_LETTER_DEFINITION_NAME = "bank.letter.definition.name";
	private static final String PROPERTY_BANK_LETTER_FILE_NAME = "bank.letter.file.name";
	private static final String PROPERTY_BANK_ID = "bank.sending.bank.id";
	private static final String SERVICE_URL = "https://ws.isb.is/adgerdirv1/birtingakerfi.asmx";

	
	/**
	 * 
	 * @param usr The user
	 * @param customXmlPart
	 * @param args
	 * @throws IOException
	 * @throws ServiceException
	 */
	public void sendMessageToBank(User usr,	String customXmlPart, Object... args) throws IOException, ServiceException {
		String ssn = usr.getPersonalID();
		//Sender pin
		String pin = getIWApplicationContext().getApplicationSettings().getProperty(BANK_SENDER_PIN);
		String xml = getXML(pin, ssn, customXmlPart, args);
	
		if (sendToLandsbankinn()) {
	
			SendLoginDataBusiness send_data = (SendLoginDataBusiness) getServiceInstance(SendLoginDataBusiness.class);
			send_data.send(xml);
	
		} else {
//			IdGenerator uidGenerator = IdGeneratorFactory.getUUIDGenerator();		
//			//Sender type
//			String user3 = getIWApplicationContext().getApplicationSettings().getProperty(BANK_SENDER_TYPE);
//			
//			StringBuffer filename = new StringBuffer(user3.toLowerCase());
//			filename.append("sunnan3");
//			filename.append(uidGenerator.generateId());
//			filename.append(".xml");

//			encodeAndSendXML(xml, filename.toString(), ssn);
	
			String filename = getIWApplicationContext().getApplicationSettings().getProperty(PROPERTY_BANK_LETTER_FILE_NAME);


			encodeAndSendXML(xml, filename, pin);
		}
	}

	public String getXML(String pin, String userSSN, String customXmlPart, Object... args){
		//Template name
		String templateName = getIWApplicationContext().getApplicationSettings().getProperty(BANK_LETTER_STYLESHEET);
		//Template version
		String templateVersion = getIWApplicationContext().getApplicationSettings().getProperty(BANK_LETTER_STYLESHEET_VERSION, "001");
		//Bank id
		String bankID = getIWApplicationContext().getApplicationSettings().getProperty(PROPERTY_BANK_ID);
		//????
		String definitionName = getIWApplicationContext().getApplicationSettings().getProperty(PROPERTY_BANK_LETTER_DEFINITION_NAME,"idega.is");
		
		IWTimestamp date = IWTimestamp.RightNow();
		
		String acct = pin + userSSN;
		String user3 = templateName + "-" + templateVersion;
		
		String xkey;
		String user4;
		String encoding = "UTF-8";
		if(sendToLandsbankinn()){
			xkey = "1";
//			encoding = "UTF-8";
		} else {
			Calendar time = date.getCalendar();
			//The xkey cannot be larger than 9999 (for some reason)
			//This method of determining the xkey should hold as long as more 
			//than one letter is not sent to the same person within the 
			//same 10 second interval - Such scenario is highly unlikely to occur
			//with respect to the use cases this bean is intended for now.
			int xKeyInt = time.get(Calendar.SECOND);
			if(xKeyInt != 0){
				xKeyInt /= 6;
			}
			xKeyInt = 6 * time.get(Calendar.MINUTE);
			xKeyInt = 360 * time.get(Calendar.HOUR_OF_DAY);

			DecimalFormat formatter = new DecimalFormat("0000");
			// date + some number unique (per receiver) within the day (0001)
			xkey = date.getDateString("yyyyMMdd")+formatter.format(xKeyInt);
//			encoding = "iso-8859-1";
		}

		user4 = acct + xkey;
		
		StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" encoding=\"");
		xml.append(encoding);
		xml.append("\"?>\n");
		xml.append("<!DOCTYPE XML-S SYSTEM \"XML-S.dtd\"[]>\n");
		xml.append("<XML-S>\n");
		xml.append("\t<Statement Acct=\"");
		xml.append(acct);
		xml.append("\" Date=\"");
		xml.append(date.getDateString("yyyy/MM/dd"));
		xml.append("\"");
		xml.append(" XKey=\"");
		xml.append(xkey);
		xml.append("\"");
		xml.append(">\n");
		xml.append("\t\t<?bgls.BlueGill.com DefinitionName=");
		xml.append(definitionName);
		xml.append("?>\n");
		xml.append("\t\t<?bgls.BlueGill.com User1=");
		xml.append(userSSN);
		xml.append("?>\n");
		xml.append("\t\t<?bgls.BlueGill.com User3=");
		xml.append(user3);
		xml.append("?>\n");
		xml.append("\t\t<?bgls.BlueGill.com User4=");
		xml.append(user4);
		xml.append("?>\n");
		
		xml.append(MessageFormat.format(customXmlPart, args));
		
		xml.append("\t</Statement>\n");
		xml.append("</XML-S>");
		
		return xml.toString();
	}

	private void encodeAndSendXML(String xml, String filename, String pin) throws IOException, ServiceException {
		String userId = getIWApplicationContext().getApplicationSettings().getProperty(BANK_SENDER_USER_ID);

		File file  = FileUtil.getFileFromWorkspace(getResourceRealPath(getBundle(),null)+ "deploy_client.wsdd");

		EngineConfiguration config = new FileProvider(new FileInputStream(
				file));
		BirtingakerfiWSLocator locator = new BirtingakerfiWSLocator(config);
		BirtingakerfiWSSoap_PortType port = locator.getBirtingakerfiWSSoap(new URL(SERVICE_URL));

		Stub stub = (Stub) port;
		stub._setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		stub._setProperty(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
		stub._setProperty(WSHandlerConstants.USER, userId);
		stub._setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, this.getClass().getName());

//		boolean sendFileFromWorkspace = getIWApplicationContext().getApplicationSettings().getBoolean("bank.test.workspace.file", false);
//		if(!sendFileFromWorkspace){
//			Logger.getAnonymousLogger().warning("----------- Generated xml ---------");
//			Logger.getAnonymousLogger().warning(xml);
			port.sendaSkra(filename, Base64.encode(xml.getBytes()), pin);
//		} else {
//			File xmlFile  = FileUtil.getFileFromWorkspace(getResourceRealPath(getBundle(),null)+ "6001891079PW.xml");
//			String newXML = getContents(xmlFile);
//			Logger.getAnonymousLogger().warning("----------- File ---------");
//			Logger.getAnonymousLogger().warning(newXML);
//			port.sendaSkra(filename, Base64.encode(newXML.getBytes()), pin);
//		}
	}
	
//	 /**
//	  * Fetch the entire contents of a text file, and return it in a String.
//	  * This style of implementation does not throw Exceptions to the caller.
//	  *
//	  * @param aFile is a file which already exists and can be read.
//	  */
//	  private String getContents(File aFile) {
//	    //...checks on aFile are elided
//	    StringBuilder contents = new StringBuilder();
//	    
//	    try {
//	      //use buffering, reading one line at a time
//	      //FileReader always assumes default encoding is OK!
//	      BufferedReader input =  new BufferedReader(new FileReader(aFile));
//	      try {
//	        String line = null; //not declared within while loop
//	        /*
//	        * readLine is a bit quirky :
//	        * it returns the content of a line MINUS the newline.
//	        * it returns null only for the END of the stream.
//	        * it returns an empty String if two newlines appear in a row.
//	        */
//	        while (( line = input.readLine()) != null){
//	          contents.append(line);
//	          contents.append(System.getProperty("line.separator"));
//	        }
//	      }
//	      finally {
//	        input.close();
//	      }
//	    }
//	    catch (IOException ex){
//	      ex.printStackTrace();
//	    }
//	    
//	    return contents.toString();
//	  }
	
	public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
		String userId = getIWApplicationContext().getApplicationSettings().getProperty(BANK_SENDER_USER_ID);
		String passwd = getIWApplicationContext().getApplicationSettings().getProperty(BANK_SENDER_USER_PASSWORD);

		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof WSPasswordCallback) {
				WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
				if (pc.getIdentifer().equals(userId)) {
					pc.setPassword(passwd);
				}
			} else {
				throw new UnsupportedCallbackException(callbacks[i],
						"Unrecognized Callback");
			}
		}
	}


	
	
	/**
	 * Get an instance of the service bean specified by serviceClass
	 */
	@SuppressWarnings("unchecked")
	protected <T extends IBOService> T getServiceInstance(Class<? extends IBOService> serviceClass) throws IBOLookupException {
	    return (T) IBOLookup.getServiceInstance(this.getIWApplicationContext(), serviceClass);
	}

	public IWApplicationContext getIWApplicationContext() {
		if (this.iwac == null) {
			return IWMainApplication.getDefaultIWApplicationContext();
		}
		return this.iwac;
	}
	
	private IWBundle getBundle() {
		return getIWApplicationContext().getIWMainApplication().getBundle(getBundleIdentifier());
	}

	private String getBundleIdentifier() {
		return OpenIDConstants.IW_BUNDLE_IDENTIFIER;
	}

	private boolean sendToLandsbankinn() {
		return getIWApplicationContext().getApplicationSettings().getBoolean(USE_LANDSBANKINN, false);
	}
	
	protected String getResourceRealPath(IWBundle iwb, Locale locale) {
		if (locale != null) {
			return iwb.getResourcesRealPath(locale) + File.separator;
		} else {
			return iwb.getResourcesRealPath() + File.separator;
		}
	}
	
}
