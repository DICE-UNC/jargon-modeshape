/**
 * 
 */
package org.irods.jargon.modeshape.connector.unittest;

import javax.jcr.Node;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.MultiUseAbstractTest;
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
		fedMgr.createProjection(testRoot.getPath(), "local-git-repo", "/",
				"irods-modeshape");
	}

	@AfterClass
	public static final void afterAll() throws Exception {
		MultiUseAbstractTest.afterAll();
	}

	@Before
	public void before() throws Exception {
		testRoot = getSession().getRootNode().getNode("repos");
	}

	@Test
	public void testHello() {
		System.out.println("hello");
	}

}
