/**
 * 
 */
package org.irods.jargon.modeshape.connector.unittest;

import javax.jcr.Node;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.test.ModeShapeMultiUseTest;

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

	@BeforeClass
	public static void beforeAll() throws Exception {
		connectorIrodsSetupUtilities = new ConnectorIrodsSetupUtilities();
		connectorIrodsSetupUtilities.init();
		RepositoryConfiguration config = RepositoryConfiguration
				.read("conf/testConfig1.json");
		startRepository(config);

		Session session = getSession();
		Node testRoot = session.getRootNode().addNode("repos");
		session.save();

		FederationManager fedMgr = session.getWorkspace()
				.getFederationManager();

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

		fedMgr.createProjection(testRoot.getPath(), "irods-modeshape", "/", "");
	}

	@AfterClass
	public static final void afterAll() throws Exception {
		ModeShapeMultiUseTest.afterAll();
	}

	@Before
	public void before() throws Exception {
		testRoot = getSession().getRootNode().getNode("repos");
	}

	@Test
	public void testGetRootDocumentById() {
		Assert.fail("implement me!");
	}

}
