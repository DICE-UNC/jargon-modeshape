/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.modeshape.connector.unittest.ConnectorIrodsSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Binary;
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
	private static TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static Session session;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		connectorIrodsSetupUtilities = new ConnectorIrodsSetupUtilities();
		connectorIrodsSetupUtilities.init();

		TestingPropertiesHelper testingPropertiesLoader = new TestingPropertiesHelper();
		testingProperties = testingPropertiesLoader.getTestProperties();

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
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
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
			sb.append("-");
		}
		sb.append("name:");
		sb.append(parent.getName());
		sb.append("  path:");
		sb.append(parent.getPath());
		sb.append(" id:");
		sb.append(parent.getIdentifier());
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
								.absolutePathForProjectionRoot());
		Node actual = session.getNodeByIdentifier("/irodsGrid");

		// connectorIrodsSetupUtilities
		// .idForProjectionRoot());
		assertFolder(actual, rootFile);

	}

	protected void assertBinaryContains(final Binary binaryValue,
			final byte[] expectedContent) throws IOException,
			RepositoryException {
		byte[] actual = IoUtil.readBytes(binaryValue.getStream());
		assertThat(actual, is(expectedContent));
	}

	protected void assertFolder(final Node node, final File dir)
			throws RepositoryException {

		assertThat(dir.exists(), is(true));
		assertThat(dir.canRead(), is(true));
		assertThat(dir.isDirectory(), is(true));
		assertThat(node.getName(), is(dir.getName()));
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

	private void assertPathNotFound(final String path) throws Exception {
		try {
			session.getNode(path);
			fail(path + " was found, even though it shouldn't have been");
		} catch (PathNotFoundException e) {
			// expected
		}

	}

}
