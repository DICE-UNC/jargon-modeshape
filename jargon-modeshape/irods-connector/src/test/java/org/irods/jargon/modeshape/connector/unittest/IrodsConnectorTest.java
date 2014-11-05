/**
 * 
 */
package org.irods.jargon.modeshape.connector.unittest;

import javax.jcr.Node;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;

/**
 * @author Mike Conway - DICE
 *
 */
public class IrodsConnectorTest extends MultiUseAbstractTest {

	private Node testRoot;

	@BeforeClass
	public static void beforeAll() throws Exception {
		RepositoryConfiguration config = RepositoryConfiguration
				.read("config/testConfig1.json");
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

}
