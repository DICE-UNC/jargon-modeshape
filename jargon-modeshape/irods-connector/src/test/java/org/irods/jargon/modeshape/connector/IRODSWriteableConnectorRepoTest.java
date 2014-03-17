package org.irods.jargon.modeshape.connector;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Properties;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NodeDefinition;

import junit.framework.Assert;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
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
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IRODSWriteableConnectorRepoTest {

	private static Properties testingProperties = new Properties();
	private static TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static ScratchFileUtils scratchFileUtils = null;
	public static final String IRODS_TEST_SUBDIR_PATH = "IRODSWriteableConnectorRepoTest";
	private static IRODSTestSetupUtilities irodsTestSetupUtilities = null;
	private static AssertionHelper assertionHelper = null;
	private static IRODSFileSystem irodsFileSystem;
	private static org.modeshape.jcr.ModeShapeEngine engine;

	static Logger log = LoggerFactory
			.getLogger(IRODSWriteableConnectorRepoTest.class);

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
		assertionHelper = new AssertionHelper();
		irodsFileSystem = IRODSFileSystem.instance();

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
		// Projection projection = new Projection("readonly-files",
		// projectionDir);

		// projection.initialize();
		session = repo.login("default");

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

	}

	@Test
	public void testFileURIBased() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		String testFileName = "testFileURIBased.txt";
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String fileNameOrig = FileGenerator.generateFileOfFixedLengthGivenName(
				absPath, testFileName, 2);

		DataTransferOperations dto = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);
		dto.putOperation(fileNameOrig, targetIrodsCollection, testingProperties
				.getProperty(TestingPropertiesHelper.IRODS_RESOURCE_KEY), null,
				null);

		Node rootNode = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH);

		Node node = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH + "/" + testFileName);

		assertThat(rootNode.getName(), is(IRODS_TEST_SUBDIR_PATH));
		assertThat(rootNode.getPrimaryNodeType().getName(), is("nt:folder"));

		assertThat(node.getName(), is(testFileName));
		assertThat(node.getPrimaryNodeType().getName(), is("nt:file"));

		Node node1Content = node.getNode("jcr:content");

		assertThat(node1Content.getName(), is("jcr:content"));
		assertThat(node1Content.getPrimaryNodeType().getName(),
				is("nt:resource"));

		Binary binary = (Binary) node1Content.getProperty("jcr:data")
				.getBinary();
		String dsChecksum = binary.getHexHash();

		DataObjectAO dataObjectAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataObjectAO(irodsAccount);
		IRODSFile testFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection + '/' + testFileName);
		byte[] computedChecksum = dataObjectAO
				.computeSHA1ChecksumOfIrodsFileByReadingDataFromStream(testFile
						.getAbsolutePath());
		session.save();
		Assert.assertEquals("checksum mismatch",
				StringUtil.getHexString(computedChecksum), dsChecksum);
	}

	@Test
	public void testRemoveDocument() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		String testFileName = "testRemoveDocument.txt";
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String fileNameOrig = FileGenerator.generateFileOfFixedLengthGivenName(
				absPath, testFileName, 2);

		DataTransferOperations dto = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);
		dto.putOperation(fileNameOrig, targetIrodsCollection, testingProperties
				.getProperty(TestingPropertiesHelper.IRODS_RESOURCE_KEY), null,
				null);

		Node node = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH + "/" + testFileName);

		node.remove();

		IRODSFile actual = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection, testFileName);

		Assert.assertFalse("file does not exist", actual.exists());

	}

	@Test
	public void assertFolder() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		Node node = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH);
		IRODSFile dir = irodsFileSystem.getIRODSAccessObjectFactory()
				.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);

		assertThat(dir.exists(), is(true));
		assertThat(dir.canRead(), is(true));
		assertThat(dir.isDirectory(), is(true));
		assertThat(node.getName(), is(dir.getName()));
		assertThat(node.getIndex(), is(1));
		assertThat(node.getPrimaryNodeType().getName(), is("nt:folder"));
		assertThat(node.getProperty("jcr:created").getLong(),
				is(dir.lastModified()));
		session.save();
	}

	@Test
	public void testCreateAndListADir() throws Exception {

		String subdirPrefix = "testCreateAndListADir";
		String fileName = "testCreateAndListADir.txt";

		int count = 30;

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ subdirPrefix);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdir();
		irodsFile.close();

		String myTarget = "";

		for (int i = 0; i < count; i++) {
			myTarget = targetIrodsCollection + "/c" + (10000 + i)
					+ subdirPrefix;
			irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
					.instanceIRODSFile(myTarget);
			irodsFile.mkdir();
			irodsFile.close();
		}

		for (int i = 0; i < count; i++) {
			myTarget = targetIrodsCollection + "/c" + (10000 + i) + fileName;
			irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
					.instanceIRODSFile(myTarget);
			irodsFile.createNewFile();
			irodsFile.close();
		}

		Node jcrNode = session.getNode("/irodsGrid");
		NodeDefinition def = jcrNode.getDefinition();
		Assert.assertNotNull("null node definition");
		log.info("jcr node irodsGrid:{}", jcrNode);

		NodeIterator iter = jcrNode.getNodes();

		while (iter.hasNext()) {
			Node next = iter.nextNode();
			log.info("next node under irodsGrid:{}", next);

			Node parent = next.getParent();
			Assert.assertEquals("cannot get parent back for node",
					jcrNode.getIdentifier(), parent.getIdentifier());

		}

		session.save();

		// projection.create(testRoot, "readonly");

	}
}
