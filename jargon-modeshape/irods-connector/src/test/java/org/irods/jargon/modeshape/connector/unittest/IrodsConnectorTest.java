/**
 * 
 */
package org.irods.jargon.modeshape.connector.unittest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.Session;
import org.modeshape.test.ModeShapeMultiUseTest;
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
public class IrodsConnectorTest extends ModeShapeMultiUseTest {

	private Node testRoot;
	private static ConnectorIrodsSetupUtilities connectorIrodsSetupUtilities;
	public static final Logger log = LoggerFactory
			.getLogger(IrodsConnectorTest.class);

	@BeforeClass
	public static void beforeAll() throws Exception {
		connectorIrodsSetupUtilities = new ConnectorIrodsSetupUtilities();
		connectorIrodsSetupUtilities.init();
		RepositoryConfiguration config = RepositoryConfiguration
				.read("conf/testConfig1.json");
		startRepository(config);

		Session session = getSession();
		// Node testRoot = session.getRootNode().addNode("repos");
		session.save();

		/*
		 * FederationManager fedMgr = session.getWorkspace()
		 * .getFederationManager();
		 */

		/**
		 * from
		 * http://docs.jboss.org/modeshape/4.0.0.Final/api/org/modeshape/jcr
		 * /api/federation/FederationManager.html
		 * 
		 * Creates an external projection by linking an internal node with an
		 * external node, from a given source using an optional alias. If this
		 * is the first node linked to the existing node, it will convert the
		 * existing node to a federated node.
		 * 
		 * Parameters:
		 * 
		 * absNodePath - a non-null string representing the absolute path to an
		 * existing internal node.
		 * 
		 * sourceName - a non-null string representing the name of an external
		 * source, configured in the repository.
		 * 
		 * externalPath - a non-null string representing a path in the external
		 * source, where at which there is an external node that will be linked.
		 * 
		 * alias - an optional string representing the name under which the
		 * alias should be created. If not present, the externalPath will be
		 * used as the name of the alias.
		 */

		// fedMgr.createProjection(testRoot.getPath(), "irods-modeshape", "/",
		// "");
	}

	@AfterClass
	public static final void afterAll() throws Exception {
		ModeShapeMultiUseTest.afterAll();
	}

	@Before
	public void before() throws Exception {
		// testRoot = getSession().getRootNode().getNode("repos");
	}

	@Test
	public void testGetJcrSystem() throws Exception {
		File rootFile = (File) connectorIrodsSetupUtilities
				.getIrodsFileSystem()
				.getIRODSFileFactory(
						connectorIrodsSetupUtilities.getIrodsAccount())
				.instanceIRODSFile(
						connectorIrodsSetupUtilities
								.absolutePathForProjectionRoot());
		Node actual = session.getNodeByIdentifier("jcr:system");
		NodeIterator iter = actual.getNodes();

		while (iter.hasNext()) {
			log.info("next child:{}", iter.next());
		}

	}

	@Test
	public void testGetRoot() throws Exception {

		Node actual = session.getRootNode();
		NodeIterator iter = actual.getNodes();

		while (iter.hasNext()) {
			log.info("next child:{}", iter.next());
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
		Node actual = session.getNodeByIdentifier(connectorIrodsSetupUtilities
				.idForProjectionRoot() + "/col1");
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
