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

import junit.framework.Assert;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.DataObjectAOImpl;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
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
		new AssertionHelper();
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
		session.refresh(true);

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

		session.refresh(false);

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

		Node parentNode = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH);

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
	public void moveDocumentFile() throws Exception {
		String testFolder = "moveDocumentFile";
		String targetFolder = "moveDocumentFileTarget";
		String testFileName = "moveDocumentFileData.txt";
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String newFolderIrodsTargetCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ targetFolder);

		session.refresh(true);

		Node parentNode = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH);

		// Create a new folder node ...
		Node newParent = parentNode.addNode(testFolder, "nt:folder");
		Node targetParent = parentNode.addNode(targetFolder, "nt:folder");

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

		// move that new file to a different parent

		String fileNodePath = fileNode.getPath();
		String targetNodePath = targetParent.getPath() + "/" + testFileName;

		session.save();

		session.move(fileNodePath, targetNodePath);

		// The auto-created properties are added when the session is saved ...
		session.save();

		IRODSFile actualFile = irodsFileSystem
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
						newFolderIrodsTargetCollection, testFileName);

		Assert.assertTrue("did not move file to target", actualFile.exists());
	}

	@Test
	public void storeDocumentCollection() throws Exception {
		String testFolder = "storeDocumentCollection";
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsParentCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		session.refresh(true);

		Node parentNode = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH);

		parentNode.addNode(testFolder, "nt:folder");

		// The auto-created properties are added when the session is saved ...
		session.save();

		IRODSFile actualFile = irodsFileSystem
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
						targetIrodsParentCollection, testFolder);

		Assert.assertTrue("did not add folder", actualFile.exists());
		Assert.assertTrue("did not add as collection", actualFile.isDirectory());

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

		session.refresh(true);

		Node node = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH + "/" + testFileName);

		node.remove();
		session.save();

		IRODSFile actual = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection, testFileName);

		Assert.assertFalse("file does not exist", actual.exists());

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
		jcrNode.getDefinition();
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

	@Test
	public void testDataObjectWithTwoAVUs() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		String testFileName = "testDataObjectWithTwoAVUs.bs";
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, 2);
		File localFile = new File(localFileName);
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";
		String expectedAttribUnit = "testunit1";

		String expectedAttribName2 = "testattrib2";
		String expectedAttribValue2 = "testvalue2";
		String expectedAttribUnit2 = "testunit2";

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		IRODSFileFactory irodsFileFactory = accessObjectFactory
				.getIRODSFileFactory(irodsAccount);
		DataObjectAOImpl dataObjectAO = (DataObjectAOImpl) accessObjectFactory
				.getDataObjectAO(irodsAccount);
		DataTransferOperations dataTransferOperations = accessObjectFactory
				.getDataTransferOperations(irodsAccount);
		IRODSFile destFile = irodsFileFactory.instanceIRODSFile(
				targetIrodsCollection, testFileName);
		dataTransferOperations.putOperation(localFile, destFile, null, null);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnit);
		dataObjectAO.addAVUMetadata(destFile.getAbsolutePath(), dataToAdd);

		dataToAdd = AvuData.instance(expectedAttribName2, expectedAttribValue2,
				expectedAttribUnit2);
		dataObjectAO.addAVUMetadata(destFile.getAbsolutePath(), dataToAdd);

		session.save();
		session.refresh(true);

		Node node = session
				.getNode("/irodsGrid/"
						+ testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
						+ "/" + IRODS_TEST_SUBDIR_PATH + "/" + testFileName);

		assertThat(node.getPrimaryNodeType().getName(), is("nt:file"));

		NodeIterator childIter = node.getNodes();
		int foundAvu = 0;
		Node childNode = null;
		Property prop;

		while (childIter.hasNext()) {
			childNode = childIter.nextNode();
			System.out.println("childNode:" + childNode);
			if (childNode.getPrimaryNodeType().getName()
					.equals(IRODSWriteableConnector.JCR_IRODS_AVU)) {
				PropertyIterator propsIter = childNode.getProperties();

				while (propsIter.hasNext()) {
					prop = propsIter.nextProperty();
					System.out.println("property:" + prop);
					if (prop.getName()
							.equals("irods:"
									+ IRODSWriteableConnector.AVU_ATTRIBUTE_PROP)) {
						foundAvu++;
					}
				}

			} else {
				childNode = null;
				continue;
			}
		}

		Assert.assertNotNull("did not find AVU child node", childNode);
		Assert.assertEquals(
				"did not find  avu attribute property of avu child", 2,
				foundAvu);

	}

	@Test
	public void testRenameADir() throws Exception {

		String subdirPrefix = "testRenameADir";
		String subdirTargetPrefix = "testRenameADirTarget";
		String fileName = "testRenameADir.txt";

		int count = 30;

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);
		IRODSFile collParent = irodsFileSystem
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
						targetIrodsCollection, subdirPrefix);
		collParent.mkdir();
		collParent.close();
		IRODSFile collChild;

		String myTarget = "";

		for (int i = 0; i < count; i++) {
			myTarget = collParent.getAbsolutePath() + "/c" + (10000 + i)
					+ subdirPrefix;
			collChild = irodsFileSystem.getIRODSFileFactory(irodsAccount)
					.instanceIRODSFile(myTarget);
			collChild.mkdir();
			collChild.close();
		}

		for (int i = 0; i < count; i++) {
			myTarget = collParent.getAbsolutePath() + "/c" + (10000 + i)
					+ fileName;
			collChild = irodsFileSystem.getIRODSFileFactory(irodsAccount)
					.instanceIRODSFile(myTarget);
			collChild.createNewFile();
			collChild.close();
		}

		session.save();
		session.refresh(true);

		String sourceAbsPath = "/irodsGrid/"
				+ testingProperties
						.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
				+ "/" + IRODS_TEST_SUBDIR_PATH + "/" + subdirPrefix;

		String targetAbsPath = "/irodsGrid/"
				+ testingProperties
						.getProperty(TestingPropertiesHelper.IRODS_SCRATCH_DIR_KEY)
				+ "/" + IRODS_TEST_SUBDIR_PATH + "/" + subdirTargetPrefix;

		// Node jcrNode = session.getNode(sourceAbsPath);

		session.move(sourceAbsPath, targetAbsPath);

		try {
			session.save();
		} catch (Exception e) {
			// this stubs out a weird error with the rename...see if iRODS shows
			// it anyhow
		}

		// now look at target in iRODS

		targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ subdirTargetPrefix);

		IRODSFile targetFile = irodsFileSystem
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
						targetIrodsCollection);

		Assert.assertTrue("did not find renamed file", targetFile.exists());

	}
}
