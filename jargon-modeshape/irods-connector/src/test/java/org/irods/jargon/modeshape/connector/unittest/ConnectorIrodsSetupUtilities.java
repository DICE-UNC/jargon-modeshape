/**
 * 
 */
package org.irods.jargon.modeshape.connector.unittest;

import java.util.Properties;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.testutils.IRODSTestSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.TestingUtilsException;
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
	private String modeshapeSubdir;
	public static final String MODESHAPE_ROOT = "test.modeshape.root";
	public static final String SKIP_DIR_BUILD = "test.modeshape.skip.dir.build.if.root.exists";
	private IRODSAccount irodsAccount;

	public static final Logger log = LoggerFactory
			.getLogger(ConnectorIrodsSetupUtilities.class);

	public ConnectorIrodsSetupUtilities() throws JargonException {
		if (testingProperties == null) {
			throw new IllegalArgumentException("null testingProperties");
		}

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
		irodsTestSetupUtilities.initializeIrodsScratchDirectory();
		irodsTestSetupUtilities
				.initializeDirectoryForTest(IRODS_TEST_SUBDIR_PATH);
		modeshapeSubdir = testingProperties.getProperty(MODESHAPE_ROOT);

		boolean skipBuild = testingPropertiesHelper.getPropertyValueAsBoolean(
				testingProperties, SKIP_DIR_BUILD);

		if (modeshapeSubdir == null || modeshapeSubdir.isEmpty()) {
			throw new TestingUtilsException("Null or empty modeshapeSubdir");
		}
		this.irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		log.info("configuration and init setup...build sample data under modeshape root...");
		buildModeshape(skipBuild);

	}

	/**
	 * build test directories
	 * 
	 * @throws TestingUtilsException
	 */
	private void buildModeshape(final boolean skipDirBuildIfRootExists)
			throws TestingUtilsException {
		log.info("buildModeshape()");
		log.info("skip dir build if root exists? {}", skipDirBuildIfRootExists);

		log.info("clear and build modeshape subdir in iRODS");
		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ modeshapeSubdir);
		IRODSFile rootFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(irodsCollectionRootAbsolutePath);

		boolean exists = rootFile.exists();
		log.info("root exists? {}", exists);
		if (exists && skipDirBuildIfRootExists) {
			log.info("skip the build!");
			return;
		}

		/*
		 * Need to
		 */

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

		String rootCollection = "col1";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + modeshapeSubdir + "/" + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		/*
		 * 
		 * col2
		 * 
		 * ---- file 1-100
		 * 
		 * ---- subcol1
		 * 
		 * ------------ file 1-10 with avu1-20 each
		 * 
		 * 
		 * ----- subcol2
		 * 
		 * ------------ file 1-10
		 * 
		 * col3
		 * 
		 * ---- file 1-10000
		 */

	}
}
