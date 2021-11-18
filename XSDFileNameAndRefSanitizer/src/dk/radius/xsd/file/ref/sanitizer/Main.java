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

	
	
	/**
	 * Main process
	 * @param args
	 * @throws RenamerException
	 */
	public static void main(String[] args) throws RenamerException {
		startTime = Instant.now();
		
		processFiles(new File(SOURCE_FOLDER));
		
		endTime = Instant.now();
		
		finalizeProcessReport();
	}

	
	/**
	 * Finalize and write processing log to console.
	 */
	private static void finalizeProcessReport() {
		addSummaryToLog();
		writeProcessingLog();
	}


	/**
	 * Write processing log to console.
	 */
	private static void writeProcessingLog() {
		for (String s : processingLog) {
			System.out.println(s);
		}
	}

	
	/**
	 * Add processing summary text to processing log before writing to console.
	 */
	private static void addSummaryToLog() {
		processingLog.add("\n### Processing ended ###");
		processingLog.add("\nFiles processed: " + filesProcessed);
		processingLog.add("\nTotal processing time in seconds: " + Duration.between(startTime, endTime).getSeconds());
	}


	/**
	 * Process files found in source folder
	 * @param file		Array of files found in source folder
	 * @throws RenamerException
	 */
	private static void processFiles(File file) throws RenamerException {
		for (File f : file.listFiles()) {
			processFile(f);
		}
	}

	
	/**
	 * Process file found in source folder.
	 * @param file		File currently being processed
	 * @throws RenamerException
	 */
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

	
	/**
	 * Parse file to XML document.
	 * @param file		Current file being processed
	 * @return	document
	 * @throws RenamerException
	 */
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

	
	/**
	 * Write a file to target folder replacing "-" with "_" in filename as they are not supported in SAP PO.
	 * @param file		Current file being processed
	 * @param document	Parsed XML document with possible changes in attribute "schemaLocation"
	 * @throws RenamerException
	 */
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

	
	/**
	 * Search for "schemaLocation" attribute and rename to a valid SAP PO value.
	 * @param elementNodeList		NodeList for element "include" or "import" plus attributes
	 * @param scehamLocationAttributeName	Name of attribute to search for and rename
	 */
	private static void processNodeList(NodeList elementNodeList, String scehamLocationAttributeName) {
		for (int i = 0; i < elementNodeList.getLength(); i++) {
			Node n = elementNodeList.item(i);

			Node includeSchemaLocation = n.getAttributes().getNamedItem(scehamLocationAttributeName);
			renameAttribute(includeSchemaLocation);
		}
	}

	
	/**
	 * Change attribute value to a valid SAP PO value, replacing "-" with "_" to match the filename change.
	 * This makes sure external references are still valid.
	 * @param includeSchemaLocation
	 */
	private static void renameAttribute(Node includeSchemaLocation) {
		if (includeSchemaLocation != null) {
			// Rename existing attribute "import" or "include" with same name using "_" instead of "-" which is not supported in SAP PO
			processingLog.add("# -> schema reference found: " + includeSchemaLocation);
			includeSchemaLocation.setTextContent(includeSchemaLocation.getNodeValue().replace(RENAME_SOURCE, RENAME_TARGET));
			processingLog.add("# -> schema reference changed to: " + includeSchemaLocation);
		}
	}
}
