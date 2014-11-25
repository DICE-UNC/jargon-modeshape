/**
 * 
 */
package org.irods.jargon.modeshape.connector.unittest;

import java.io.File;
import java.util.Properties;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.testutils.IRODSTestSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.TestingUtilsException;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.irods.jargon.testutils.filemanip.ScratchFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE Misc utilities to set up a modeshape unit test
 * 
 */
public class ConnectorIrodsSetupUtilities {
	private IRODSFileSystem irodsFileSystem;
	private Properties testingProperties;
	private TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static ScratchFileUtils scratchFileUtils = null;
	public static final String IRODS_TEST_SUBDIR_PATH = "ModeshapeConnectorRoot";
	private static IRODSTestSetupUtilities irodsTestSetupUtilities = null;
	public static final String SKIP_DIR_BUILD = "test.modeshape.skip.dir.build.if.root.exists";
	private IRODSAccount irodsAccount;

	public static final Logger log = LoggerFactory
			.getLogger(ConnectorIrodsSetupUtilities.class);

	public ConnectorIrodsSetupUtilities() throws JargonException {

		this.irodsFileSystem = IRODSFileSystem.instance();
	}

	public void init() throws JargonException, TestingUtilsException {

		log.info("init()");
		testingPropertiesHelper = new TestingPropertiesHelper();
		testingProperties = testingPropertiesHelper.getTestProperties();
		scratchFileUtils = new ScratchFileUtils(testingProperties);
		scratchFileUtils
				.clearAndReinitializeScratchDirectory(IRODS_TEST_SUBDIR_PATH);
		irodsTestSetupUtilities = new IRODSTestSetupUtilities();

		boolean skipBuild = testingPropertiesHelper.getPropertyValueAsBoolean(
				testingProperties, SKIP_DIR_BUILD);

		this.irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		log.info("configuration and init setup...build sample data under modeshape root...");
		buildModeshape(skipBuild);

	}

	/**
	 * Get the irods absolute path for the root of the modeshape projection
	 * 
	 * @return <code>String</code> with the iRODS absolute path to the root of
	 *         the projection
	 * @throws Exception
	 */
	public String absolutePathForProjectionRoot() throws Exception {
		return testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH)
				+ "/" + IRODS_TEST_SUBDIR_PATH;

	}

	/**
	 * Fixme, use this for preprocessing a config file so this is all pluggable
	 * 
	 * @return
	 * @throws Exception
	 */
	public String idForProjectionRoot() throws Exception {
		return "/irodsGrid";
	}

	/**
	 * build test directories
	 * 
	 * @throws TestingUtilsException
	 * @throws JargonException
	 */
	private void buildModeshape(final boolean skipDirBuildIfRootExists)
			throws TestingUtilsException, JargonException {
		log.info("buildModeshape()");
		log.info("skip dir build if root exists? {}", skipDirBuildIfRootExists);

		log.info("clear and build modeshape subdir in iRODS");
		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);
		IRODSFile rootFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(irodsCollectionRootAbsolutePath);

		boolean exists = rootFile.exists();
		log.info("root exists? {}", exists);
		if (exists && skipDirBuildIfRootExists) {
			log.info("skip the build!");
			return;
		}

		/*
		 * Need to clean out the modeshape test dir in irods
		 */

		irodsTestSetupUtilities.initializeIrodsScratchDirectory();
		irodsTestSetupUtilities
				.initializeDirectoryForTest(IRODS_TEST_SUBDIR_PATH);

		log.info("clearing root file {}...", rootFile);
		rootFile.deleteWithForceOption();
		rootFile.mkdirs();
		log.info("fresh root file");

		/*
		 * build a test universe
		 * 
		 * col1
		 * 
		 * --- file 1-1000
		 */

		scratchFileUtils
				.clearAndReinitializeScratchDirectory(IRODS_TEST_SUBDIR_PATH);

		String rootCollection = "col1";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);

		FileGenerator.generateManyFilesInGivenDirectory(IRODS_TEST_SUBDIR_PATH
				+ "/" + rootCollection, "file", ".txt", 1000, 2, 100);

		/*
		 * 
		 * col2
		 * 
		 * ---- file 1-100
		 */

		rootCollection = "col2";

		FileGenerator.generateManyFilesInGivenDirectory(IRODS_TEST_SUBDIR_PATH
				+ "/" + rootCollection, "file", ".txt", 100, 2, 100);

		/*
		 * ---- subcol1
		 * 
		 * ------------ file 1-10 with avu1-20 each (avus added after all files
		 * and dirs 'put' to irods)
		 */

		rootCollection = "col1/subcol1";

		FileGenerator.generateManyFilesInGivenDirectory(IRODS_TEST_SUBDIR_PATH
				+ "/" + rootCollection, "file", ".txt", 100, 2, 100);

		/*
		 * 
		 * ----- subcol2
		 * 
		 * ------------ file 1-100
		 */

		rootCollection = "col1/subcol2";

		FileGenerator.generateManyFilesInGivenDirectory(IRODS_TEST_SUBDIR_PATH
				+ "/" + rootCollection, "file", ".txt", 100, 2, 100);

		/*
		 * col3
		 * 
		 * ---- file 1-10000
		 */

		DataTransferOperations dataTransferOperations = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);

		File localSourceFile = new File(localCollectionAbsolutePath);

		log.info("doing transfer of test files to irods...");
		dataTransferOperations.putOperation(localSourceFile, rootFile, null,
				null);
		log.info("test dir put into irods");

		log.info("add the avus for col2/subcol1");

		String attribPrefix = "attrib";
		String value = "value";

		DataObjectAO dataObjectAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataObjectAO(irodsAccount);
		AvuData avuData;

		String irodsFileName;

		for (int j = 0; j < 100; j++) {

			irodsFileName = "file" + j + ".txt";

			for (int i = 0; i < 20; i++) {
				avuData = AvuData.instance(attribPrefix + i, value, "");
				dataObjectAO.addAVUMetadata(irodsCollectionRootAbsolutePath
						+ "/" + IRODS_TEST_SUBDIR_PATH + "/col1/subcol2/"
						+ irodsFileName, avuData);
			}
		}

	}

	/**
	 * @return the irodsFileSystem
	 */
	public IRODSFileSystem getIrodsFileSystem() {
		return irodsFileSystem;
	}

	/**
	 * @param irodsFileSystem
	 *            the irodsFileSystem to set
	 */
	public void setIrodsFileSystem(IRODSFileSystem irodsFileSystem) {
		this.irodsFileSystem = irodsFileSystem;
	}

	/**
	 * @return the testingProperties
	 */
	public Properties getTestingProperties() {
		return testingProperties;
	}

	/**
	 * @param testingProperties
	 *            the testingProperties to set
	 */
	public void setTestingProperties(Properties testingProperties) {
		this.testingProperties = testingProperties;
	}

	/**
	 * @return the testingPropertiesHelper
	 */
	public TestingPropertiesHelper getTestingPropertiesHelper() {
		return testingPropertiesHelper;
	}

	/**
	 * @param testingPropertiesHelper
	 *            the testingPropertiesHelper to set
	 */
	public void setTestingPropertiesHelper(
			TestingPropertiesHelper testingPropertiesHelper) {
		this.testingPropertiesHelper = testingPropertiesHelper;
	}

	/**
	 * @return the scratchFileUtils
	 */
	public static ScratchFileUtils getScratchFileUtils() {
		return scratchFileUtils;
	}

	/**
	 * @param scratchFileUtils
	 *            the scratchFileUtils to set
	 */
	public static void setScratchFileUtils(ScratchFileUtils scratchFileUtils) {
		ConnectorIrodsSetupUtilities.scratchFileUtils = scratchFileUtils;
	}

	/**
	 * @return the irodsAccount
	 */
	public IRODSAccount getIrodsAccount() {
		return irodsAccount;
	}

	/**
	 * @param irodsAccount
	 *            the irodsAccount to set
	 */
	public void setIrodsAccount(IRODSAccount irodsAccount) {
		this.irodsAccount = irodsAccount;
	}
}
