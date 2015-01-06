/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.Assert;

import org.irods.jargon.core.pub.Stream2StreamAO;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.modeshape.connector.unittest.ConnectorIrodsSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.irods.jargon.testutils.filemanip.ScratchFileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE ref
 *         https://github.com/ModeShape/modeshape/blob/master
 *         /connectors/modeshape
 *         -connector-git/src/test/java/org/modeshape/connector
 *         /git/GitConnectorTest.java
 * 
 * 
 *         parent class source at
 *         https://github.com/ModeShape/modeshape/blob/master
 *         /modeshape-jcr/src/test
 *         /java/org/modeshape/jcr/MultiUseAbstractTest.java
 */
public class IrodsConnectorTest {

	private static org.modeshape.jcr.ModeShapeEngine engine;
	private static ConnectorIrodsSetupUtilities connectorIrodsSetupUtilities;
	public static final Logger log = LoggerFactory
			.getLogger(IrodsConnectorTest.class);
	private static Properties testingProperties = new Properties();
	@SuppressWarnings("unused")
	private static TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static Session session;
	private static ScratchFileUtils scratchFileUtils = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestingPropertiesHelper testingPropertiesLoader = new TestingPropertiesHelper();
		testingProperties = testingPropertiesLoader.getTestProperties();
		connectorIrodsSetupUtilities = new ConnectorIrodsSetupUtilities();
		connectorIrodsSetupUtilities.init();
		scratchFileUtils = new ScratchFileUtils(testingProperties);
		scratchFileUtils
				.clearAndReinitializeScratchDirectory(connectorIrodsSetupUtilities
						.absolutePathForProjectionRoot());

		engine = new ModeShapeEngine();
		engine.start();
		RepositoryConfiguration config = RepositoryConfiguration
				.read("conf/testConfig1.json");

		// Verify the configuration for the repository ...
		org.modeshape.common.collection.Problems problems = config.validate();
		if (problems.hasErrors()) {
			System.err.println("Problems starting the engine.");
			System.err.println(problems);
			System.exit(-1);
		}

		javax.jcr.Repository repo = engine.deploy(config);

		String repositoryName = config.getName();
		log.info("repo name:{}", repositoryName);

		session = repo.login("default");

		// Get the root node ...
		Node root = session.getRootNode();
		dumpNodes(root, 0);

		assert root != null;

	}

	private static void dumpNodes(Node parent, int depth)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append("--");
		}
		sb.append("name:");
		sb.append(parent.getName());
		sb.append("  path:");
		sb.append(parent.getPath());
		sb.append(" id:");
		sb.append(parent.getIdentifier());
		sb.append(" nodeType:");
		sb.append(parent.getPrimaryNodeType());
		log.info(sb.toString());

		NodeIterator iter = parent.getNodes();
		int currentLevel = depth;
		while (iter.hasNext()) {
			Node next = iter.nextNode();
			dumpNodes(next, currentLevel + 1);
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Future<Boolean> future = engine.shutdown();
		if (future.get()) { // optional, but blocks until engine is completely
							// shutdown or interrupted
			System.out.println("Shut down ModeShape");
		}
	}

	@Test
	public void testGetTopOfIrodsProjection() throws Exception {
		File rootFile = (File) connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSFileFactory(
						connectorIrodsSetupUtilities.getIrodsAccount())
				.instanceIRODSFile(
						connectorIrodsSetupUtilities
								.absolutePathForProjectionRoot() + "/col1");
		Node actual = session.getNode("/irodsGrid/col1");

		assertFolder(actual, rootFile);

	}

	@Test
	public void testFileInProjection() throws Exception {
		File rootFile = (File) connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSFileFactory(
						connectorIrodsSetupUtilities.getIrodsAccount())
				.instanceIRODSFile(
						connectorIrodsSetupUtilities
								.absolutePathForProjectionRoot()
								+ "/col1/file1.txt");
		Node actual = session.getNode("/irodsGrid/col1/file1.txt");

		assertFile(actual, rootFile);

	}

	@Test
	public void testFileInProjectionBinaryContent() throws Exception {
		File rootFile = (File) connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSFileFactory(
						connectorIrodsSetupUtilities.getIrodsAccount())
				.instanceIRODSFile(
						connectorIrodsSetupUtilities
								.absolutePathForProjectionRoot()
								+ "/col1/subcol1/file0.txt");
		Node actual = session.getNode("/irodsGrid/col1/subcol1/file0.txt");

		assertFile(actual, rootFile);

		dumpNodes(actual, 0);

		Node node1Content = actual.getNode("jcr:content");

		assertThat(node1Content.getName(), is("jcr:content"));
		assertThat(node1Content.getPrimaryNodeType().getName(),
				is("nt:resource"));

		dumpProperties(node1Content);

		javax.jcr.Binary binary = node1Content.getProperty("jcr:data")
				.getBinary();

		Stream2StreamAO stream2stream = connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSAccessObjectFactory()
				.getStream2StreamAO(
						connectorIrodsSetupUtilities.getIrodsAccount());

		IRODSFile testFile = connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSFileFactory(
						connectorIrodsSetupUtilities.getIrodsAccount())
				.instanceIRODSFile(
						connectorIrodsSetupUtilities
								.absolutePathForProjectionRoot()
								+ "/col1/subcol1/file0.txt");

		byte[] expected = stream2stream.streamFileToByte(testFile);
		assertBinaryContains(binary, expected);
	}

	@Test
	public void testStoreDocumentCollection() throws Exception {
		String testFileName = "storeDocumentCollection";

		Node parentNode = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);

		// Create an 'nt:file' node at the supplied path ...
		parentNode.addNode(testFileName, "nt:folder");
		session.save();
		// Get the just added node

		Node actual = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testFileName);
		Assert.assertNotNull("did not find new node", actual);

	}

	@Test
	public void testStoreDocumentFile() throws Exception {
		String testFileName = "storeDocumentFileData.txt";
		Node parentNode = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);

		// create file under parent node
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);
		String fileNameOrig = FileGenerator.generateFileOfFixedLengthGivenName(
				absPath, testFileName, 200);
		File newFile = new File(fileNameOrig);

		Calendar lastModified = Calendar.getInstance();
		lastModified.setTimeInMillis(newFile.lastModified());

		// Create a buffered input stream for the file's contents ...
		InputStream stream = new BufferedInputStream(new FileInputStream(
				newFile));

		// Create an 'nt:file' node at the supplied path ...
		Node fileNode = parentNode.addNode(testFileName, "nt:file");

		// Upload the file to that node ...
		Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
		javax.jcr.Binary binary = session.getValueFactory()
				.createBinary(stream);
		contentNode.setProperty("jcr:data", binary);
		contentNode.setProperty("jcr:lastModified", lastModified);

		// The auto-created properties are added when the session is saved ...
		session.save();

		// Get the just added node

		Node actual = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testFileName);
		Assert.assertNotNull("did not find new node", actual);

	}

	@Test
	public void removeDocumentFile() throws Exception {
		String testFileName = "removeDocumentFile.txt";
		Node parentNode = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);

		// create file under parent node
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);
		String fileNameOrig = FileGenerator.generateFileOfFixedLengthGivenName(
				absPath, testFileName, 200);
		File newFile = new File(fileNameOrig);

		Calendar lastModified = Calendar.getInstance();
		lastModified.setTimeInMillis(newFile.lastModified());

		// Create a buffered input stream for the file's contents ...
		InputStream stream = new BufferedInputStream(new FileInputStream(
				newFile));

		// Create an 'nt:file' node at the supplied path ...
		Node fileNode = parentNode.addNode(testFileName, "nt:file");

		// Upload the file to that node ...
		Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
		javax.jcr.Binary binary = session.getValueFactory()
				.createBinary(stream);
		contentNode.setProperty("jcr:data", binary);
		contentNode.setProperty("jcr:lastModified", lastModified);

		// The auto-created properties are added when the session is saved ...
		session.save();

		// Get the just added node

		Node actual = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testFileName);
		Assert.assertNotNull("did not find new node", actual);

		// remove the node

		session.removeItem(actual.getPath());

		// The auto-created properties are added when the session is saved ...
		session.save();

		boolean foundException = false;

		try {

			actual = session.getNode("/irodsGrid/"
					+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
					+ "/" + testFileName);
		} catch (PathNotFoundException pnf) {
			foundException = true;
		}

		Assert.assertTrue("should not have found removed node", foundException);

		IRODSFile actualFile = connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSFileFactory(
						connectorIrodsSetupUtilities.getIrodsAccount())
				.instanceIRODSFile(
						connectorIrodsSetupUtilities
								.absolutePathForProjectionRoot()
								+ "/"
								+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
								+ "/" + testFileName);
		Assert.assertFalse("did not remove file in iRODS", actualFile.exists());

	}

	@Test
	public void moveDocumentFile() throws Exception {
		String testFileName = "moveDocumentFile.txt";
		String testFileTargetName = "moveDocumentFileTarget.txt";
		Node parentNode = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);

		// create file under parent node
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);
		String fileNameOrig = FileGenerator.generateFileOfFixedLengthGivenName(
				absPath, testFileName, 20000);
		File newFile = new File(fileNameOrig);

		Calendar lastModified = Calendar.getInstance();
		lastModified.setTimeInMillis(newFile.lastModified());

		// Create a buffered input stream for the file's contents ...
		InputStream stream = new BufferedInputStream(new FileInputStream(
				newFile));

		// Create an 'nt:file' node at the supplied path ...
		Node fileNode = parentNode.addNode(testFileName, "nt:file");

		// Upload the file to that node ...
		Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
		javax.jcr.Binary binary = session.getValueFactory()
				.createBinary(stream);
		contentNode.setProperty("jcr:data", binary);
		contentNode.setProperty("jcr:lastModified", lastModified);

		// The auto-created properties are added when the session is saved ...
		session.save();

		// Get the just added node

		Node actual = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testFileName);
		Assert.assertNotNull("did not find new node", actual);

		// move that new file to a different parent

		String fileNodePath = fileNode.getPath();
		String targetNodePath = "/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testFileTargetName;

		session.save();

		session.move(fileNodePath, targetNodePath);

		// The auto-created properties are added when the session is saved ...
		session.save();

		actual = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testFileTargetName);
		Assert.assertNotNull("did not find new node", actual);

		IRODSFile actualFile = connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSFileFactory(
						connectorIrodsSetupUtilities.getIrodsAccount())
				.instanceIRODSFile(
						connectorIrodsSetupUtilities
								.absolutePathForProjectionRoot()
								+ "/"
								+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
								+ "/" + testFileTargetName);
		Assert.assertTrue("did not move file to target", actualFile.exists());

		this.assertFile(actual, (File) actualFile);

	}

	@Test
	public void moveDocumentCollection() throws Exception {
		String testCollectionName = "moveDocumentCollection";
		String testCollectionTargetName = "moveDocumentCollectionTarget";
		Node parentNode = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH);

		Node folderNode = parentNode.addNode(testCollectionName, "nt:folder");

		String testFileName1 = "testFileName1.txt";
		String testFileName2 = "testFileName2.txt";

		// create file under parent node
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
						+ "/" + testCollectionName);
		String fileNameOrig1 = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath,
						testCollectionName, 20000);
		File newFile1 = new File(fileNameOrig1);

		Calendar lastModified = Calendar.getInstance();
		lastModified.setTimeInMillis(newFile1.lastModified());

		// Create a buffered input stream for the file's contents ...
		InputStream stream = new BufferedInputStream(new FileInputStream(
				newFile1));

		// Create an 'nt:file' node at the supplied path ...
		Node fileNode1 = folderNode.addNode(testFileName1, "nt:file");

		// Upload the file to that node ...
		Node contentNode = fileNode1.addNode("jcr:content", "nt:resource");
		javax.jcr.Binary binary = session.getValueFactory()
				.createBinary(stream);
		contentNode.setProperty("jcr:data", binary);
		contentNode.setProperty("jcr:lastModified", lastModified);

		String fileNameOrig2 = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath,
						testCollectionName, 20000);
		File newFile2 = new File(fileNameOrig2);

		lastModified.setTimeInMillis(newFile2.lastModified());

		// Create a buffered input stream for the file's contents ...
		stream = new BufferedInputStream(new FileInputStream(newFile2));

		// Create an 'nt:file' node at the supplied path ...
		Node fileNode2 = folderNode.addNode(testFileName2, "nt:file");

		// Upload the file to that node ...
		contentNode = fileNode2.addNode("jcr:content", "nt:resource");
		javax.jcr.Binary binary2 = session.getValueFactory().createBinary(
				stream);
		contentNode.setProperty("jcr:data", binary2);
		contentNode.setProperty("jcr:lastModified", lastModified);

		// The auto-created properties are added when the session is saved ...
		session.save();

		// Get the just added node

		Node actual = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testCollectionName);
		Assert.assertNotNull("did not find new node", actual);

		// move that new file to a different parent

		String collectionNodePath = folderNode.getPath();
		String targetNodePath = "/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testCollectionTargetName;

		session.save();

		session.move(collectionNodePath, targetNodePath);

		// The auto-created properties are added when the session is saved ...
		session.save();

		actual = session.getNode("/irodsGrid/"
				+ ConnectorIrodsSetupUtilities.FILES_CREATED_IN_TESTS_PATH
				+ "/" + testCollectionTargetName);
		Assert.assertNotNull("did not find new node", actual);
	}

	protected void dumpProperties(final Node node) throws Exception {
		PropertyIterator iter = node.getProperties();
		Property prop;
		while (iter.hasNext()) {
			prop = iter.nextProperty();
			log.info("property");
			log.info("name:{}", prop.getName());
			log.info("path:{}", prop.getPath());
			log.info("type:{}", prop.getType());
			if (prop.getType() == 2) {
				log.info("is binary!....");
				Binary val = prop.getBinary();
				log.info("class:{}", prop.getClass());
				log.info("size:{}", val.getSize());
				log.info("stream:{}", val.getStream());
			}
		}
	}

	protected void assertBinaryContains(final javax.jcr.Binary binary,
			final byte[] expectedContent) throws IOException,
			RepositoryException {
		byte[] actual = IoUtil.readBytes(binary.getStream());
		assertThat(actual, is(expectedContent));
	}

	protected void assertFolder(final Node node, final File dir)
			throws RepositoryException {

		assertThat(dir.exists(), is(true));
		assertThat(dir.canRead(), is(true));
		assertThat(dir.isDirectory(), is(true));

		log.info("node name:{}", node.getName());
		log.info("node.identifier:{}", node.getIdentifier());

		assertThat(node.getIndex(), is(1));
		assertThat(node.getPrimaryNodeType().getName(), is("nt:folder"));
		assertThat(node.getProperty("jcr:created").getLong(),
				is(dir.lastModified()));
	}

	protected void assertFile(final Node node, final File file)
			throws RepositoryException {
		long lastModified = file.lastModified();
		assertThat(node.getName(), is(file.getName()));
		assertThat(node.getIndex(), is(1));
		assertThat(node.getPrimaryNodeType().getName(), is("nt:file"));
		assertThat(node.getProperty("jcr:created").getLong(), is(lastModified));
		Node content = node.getNode("jcr:content");
		assertThat(content.getName(), is("jcr:content"));
		assertThat(content.getIndex(), is(1));
		assertThat(content.getPrimaryNodeType().getName(), is("nt:resource"));
		assertThat(content.getProperty("jcr:lastModified").getLong(),
				is(lastModified));
	}

	@SuppressWarnings("unused")
	private void assertPathNotFound(final String path) throws Exception {
		try {
			session.getNode(path);
			fail(path + " was found, even though it shouldn't have been");
		} catch (PathNotFoundException e) {
			// expected
		}

	}

}
