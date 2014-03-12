package org.irods.jargon.modeshape.connector;

import java.util.Properties;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NodeDefinition;

import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.testutils.AssertionHelper;
import org.irods.jargon.testutils.IRODSTestSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.filemanip.ScratchFileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
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

	static Logger log = LoggerFactory
			.getLogger(IRODSWriteableConnectorRepoTest.class);

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
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		irodsFileSystem.closeAndEatExceptions();
	}

	@Before
	public void before() throws Exception {

	}

	@Test
	public void testStartEngine() throws Exception {
		org.modeshape.jcr.ModeShapeEngine engine = new ModeShapeEngine();
		engine.start();
		RepositoryConfiguration config = RepositoryConfiguration
				.read("conf/testConfig1.json");

		javax.jcr.Repository repo = engine.deploy(config);
		Future<Boolean> future = engine.shutdown();
		if (future.get()) { // optional, but blocks until engine is completely
							// shutdown or interrupted
			System.out.println("Shut down ModeShape");
		}
	}

	@Test
	public void testCreateAndListADir() throws Exception {

		String testDirName = "testCreateAndListADir";

		org.modeshape.jcr.ModeShapeEngine engine = new ModeShapeEngine();
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
		javax.jcr.Session session = repo.login("default");

		Node rootNodeIrods = session.getNode("/");
		NodeDefinition def = rootNodeIrods.getDefinition();
		log.info("root node def:{}", def);

		NodeIterator iter = rootNodeIrods.getNodes();

		while (iter.hasNext()) {
			Node next = iter.nextNode();
			log.info("next node:{}", next);
		}

		Node jcrNode = session.getNode("/irodsGrid");
		def = jcrNode.getDefinition();
		log.info("jcr node irodsGrid:{}", jcrNode);

		iter = jcrNode.getNodes();

		while (iter.hasNext()) {
			Node next = iter.nextNode();
			log.info("next node under irodsGrid:{}", next);
		}

		session.save();

		// projection.create(testRoot, "readonly");

		Future<Boolean> future = engine.shutdown();
		if (future.get()) { // optional, but blocks until engine is completely
							// shutdown or interrupted
			System.out.println("Shut down ModeShape");
		}
	}
}
