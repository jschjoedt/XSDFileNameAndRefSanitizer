package dk.radius.xsd.file.ref.sanitizer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {

	private static final String SOURCE_FOLDER = "C:\\Users\\jsch\\Desktop\\XSD 20210410\\";
	private static final String TARGET_FOLDER = "C:\\Users\\jsch\\Desktop\\XSD 20210410\\Renamed\\";
	private static final String RENAME_SOURCE = "-";
	private static final String RENAME_TARGET = "_";
	private static final String FILE_SUFFIX = ".xsd";
	private static final String ATTR_NAME_SCHEMA_LOCATION = "schemaLocation";
	private static int filesProcessed = 0;
	private static Instant startTime;
	private static Instant endTime;
	private static ArrayList<String> processingLog = new ArrayList<String>();

	
	
	public static void main(String[] args) throws RenamerException {
		startTime = Instant.now();
		
		processFiles(new File(SOURCE_FOLDER));
		
		endTime = Instant.now();
		
		generateOutput();
	}

	
	private static void generateOutput() {
		generateSummary();
		writeProcessingLog();
	}


	private static void writeProcessingLog() {
		for (String s : processingLog) {
			System.out.println(s);
		}
	}


	private static void generateSummary() {
		processingLog.add("\n### Processing ended ###");
		processingLog.add("\nFiles processed: " + filesProcessed);
		processingLog.add("\nTotal processing time in seconds: " + Duration.between(startTime, endTime).getSeconds());
	}


	private static void processFiles(File f) throws RenamerException {
		for (File file : f.listFiles()) {
			processFile(file);
		}
	}

	
	private static void processFile(File file) throws RenamerException {
		if (file.isFile() && file.getName().endsWith(FILE_SUFFIX)) {
			filesProcessed++;
			processingLog.add("\n# (" + filesProcessed + ") File found:" + file.getName());
			
			Document document = parseInputFile(file);

			processNodeList(document.getElementsByTagName("xsd:include"), ATTR_NAME_SCHEMA_LOCATION);	

			processNodeList(document.getElementsByTagName("xs:import"), ATTR_NAME_SCHEMA_LOCATION);	

			writeDocumentToFile(file, document);
		}
	}

	
	private static Document parseInputFile(File file) throws RenamerException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			Document document = builder.parse(file);
			return document;
			
		} catch (ParserConfigurationException pce) {
			throw new RenamerException("Could not create new builder: " + pce.getMessage());
		} catch (SAXException se) {
			throw new RenamerException("Error parsing file: " + se.getMessage());
		} catch (IOException ioe) {
			throw new RenamerException("Error reading file to be parsed: " + ioe.getMessage());
		}
	}

	
	private static void writeDocumentToFile(File file, Document document) throws RenamerException {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			DOMSource src = new DOMSource(document);
			StreamResult res = new StreamResult(new File(TARGET_FOLDER + file.getName().replace(RENAME_SOURCE, RENAME_TARGET)));
			transformer.transform(src, res);
			
		} catch (TransformerConfigurationException tce) {
			throw new RenamerException("Error creating transformer: " + tce.getMessage());
		} catch (TransformerException te) {
			throw new RenamerException("Could not transform: " + te.getMessage());			
		}
	}

	
	private static void processNodeList(NodeList includeNL, String scehamLocationAttributeName) {
		for (int i = 0; i < includeNL.getLength(); i++) {
			Node n = includeNL.item(i);

			Node includeSchemaLocation = n.getAttributes().getNamedItem(scehamLocationAttributeName);
			renameAttribute(includeSchemaLocation);
		}
	}

	
	private static void renameAttribute(Node includeSchemaLocation) {
		if (includeSchemaLocation != null) {
			// Rename existing attribute "import" or "include" with same name using "_" instead of "-" which is not supported in SAP PO
			processingLog.add("# -> schema reference found: " + includeSchemaLocation);
			includeSchemaLocation.setTextContent(includeSchemaLocation.getNodeValue().replace(RENAME_SOURCE, RENAME_TARGET));
			processingLog.add("# -> schema reference changed to: " + includeSchemaLocation);
		}
	}
}
