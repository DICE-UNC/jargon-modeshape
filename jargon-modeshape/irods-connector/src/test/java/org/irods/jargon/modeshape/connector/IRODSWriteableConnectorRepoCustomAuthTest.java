package org.irods.jargon.modeshape.connector;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.SimpleCredentials;

import junit.framework.Assert;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.testutils.AssertionHelper;
import org.irods.jargon.testutils.IRODSTestSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.irods.jargon.testutils.filemanip.ScratchFileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IRODSWriteableConnectorRepoCustomAuthTest {

	private static Properties testingProperties = new Properties();
	private static TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static ScratchFileUtils scratchFileUtils = null;
	public static final String IRODS_TEST_SUBDIR_PATH = "IRODSWriteableConnectorRepoCustomAuthTest";
	private static IRODSTestSetupUtilities irodsTestSetupUtilities = null;
	private static IRODSFileSystem irodsFileSystem;
	private static org.modeshape.jcr.ModeShapeEngine engine;

	static Logger log = LoggerFactory
			.getLogger(IRODSWriteableConnectorRepoCustomAuthTest.class);

	private static javax.jcr.Session session;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestingPropertiesHelper testingPropertiesLoader = new TestingPropertiesHelper();
		testingProperties = testingPropertiesLoader.getTestProperties();
		scratchFileUtils = new ScratchFileUtils(testingProperties);
		scratchFileUtils
				.clearAndReinitializeScratchDirectory(IRODS_TEST_SUBDIR_PATH);
		irodsTestSetupUtilities = new IRODSTestSetupUtilities();
		irodsTestSetupUtilities.initializeIrodsScratchDirectory();
		irodsTestSetupUtilities
				.initializeDirectoryForTest(IRODS_TEST_SUBDIR_PATH);
		new AssertionHelper();
		irodsFileSystem = IRODSFileSystem.instance();

		engine = new ModeShapeEngine();
		engine.start();
		RepositoryConfiguration config = RepositoryConfiguration
				.read("conf/testConfigAuth1.json");

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
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		SimpleCredentials simpleCredentials = new SimpleCredentials(
				irodsAccount.getUserName(), irodsAccount.getPassword()
						.toCharArray());

		session = repo.login(simpleCredentials, "default");

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Future<Boolean> future = engine.shutdown();
		if (future.get()) { // optional, but blocks until engine is completely
							// shutdown or interrupted
			System.out.println("Shut down ModeShape");
		}
		irodsFileSystem.closeAndEatExceptions();
	}

	@Before
	public void before() throws Exception {
		session.refresh(false);

	}

	@Test
	public void storeDocumentFile() throws Exception {
		String testFolder = "storeDocumentFile";
		String testFileName = "storeDocumentFileData.txt";
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String newFolderIrodsParentCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFolder);

		session.refresh(true);

		String parentNodeTargetPath = "/irodsGrid/"
				+ testingProperties
						.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
				+ "/" + IRODS_TEST_SUBDIR_PATH;
		Node parentNode = session.getNode(parentNodeTargetPath);

		NodeIterator nodeIter = parentNode.getNodes();

		while (nodeIter.hasNext()) {
			Node child = nodeIter.nextNode();
			System.out.println("next:" + child);
		}

		// Create a new folder node ...
		Node newParent = parentNode.addNode(testFolder, "nt:folder");

		// create file under parent node
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String fileNameOrig = FileGenerator.generateFileOfFixedLengthGivenName(
				absPath, testFileName, 200);
		File newFile = new File(fileNameOrig);

		Calendar lastModified = Calendar.getInstance();
		lastModified.setTimeInMillis(newFile.lastModified());

		// Create a buffered input stream for the file's contents ...
		InputStream stream = new BufferedInputStream(new FileInputStream(
				newFile));

		// Create an 'nt:file' node at the supplied path ...
		Node fileNode = newParent.addNode(newFile.getName(), "nt:file");

		// Upload the file to that node ...
		Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
		javax.jcr.Binary binary = session.getValueFactory()
				.createBinary(stream);
		contentNode.setProperty("jcr:data", binary);
		contentNode.setProperty("jcr:lastModified", lastModified);

		// The auto-created properties are added when the session is saved ...
		session.save();

		IRODSFile actualFile = irodsFileSystem
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
						newFolderIrodsParentCollection, testFileName);

		Assert.assertTrue("did not add file", actualFile.exists());
		Assert.assertTrue("did not add as data object", actualFile.isFile());

	}

	@Test
	public void testFolderWithAVUs() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		String testCollectionName = "testFolderWithAVUs";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";
		String expectedAttribUnit = "testunit1";

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(
				targetIrodsCollection + "/" + testCollectionName);
		targetCollectionAsFile.mkdirs();

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnit);
		collectionAO.addAVUMetadata(targetCollectionAsFile.getAbsolutePath(),
				dataToAdd);

		session.save();
		session.refresh(true);

		Node node = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH + "/"
						+ testCollectionName);

		assertThat(node.getPrimaryNodeType().getName(), is("nt:folder"));

		NodeIterator childIter = node.getNodes();
		Node childNode = null;
		while (childIter.hasNext()) {
			childNode = childIter.nextNode();
			System.out.println("childNode:" + childNode);
			if (childNode.getPrimaryNodeType().getName()
					.equals(IRODSWriteableConnector.JCR_IRODS_AVU)) {
				break;
			} else {
				childNode = null;
				continue;
			}
		}

		Assert.assertNotNull("did not find AVU child node", childNode);

		PropertyIterator propsIter = childNode.getProperties();
		boolean foundAvu = false;

		Property prop;
		while (propsIter.hasNext()) {
			prop = propsIter.nextProperty();
			System.out.println("property:" + prop);
			if (prop.getName().equals(
					"irods:" + IRODSWriteableConnector.AVU_ATTRIBUTE_PROP)) {
				foundAvu = true;
				break;
			}
		}

		Assert.assertTrue("did not find avu attribute property of avu child",
				foundAvu);

	}

}
