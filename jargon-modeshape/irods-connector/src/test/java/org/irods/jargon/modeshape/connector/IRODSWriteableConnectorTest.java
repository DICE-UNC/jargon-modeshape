package org.irods.jargon.modeshape.connector;

import java.io.IOException;

import javax.jcr.RepositoryException;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

public class IRODSWriteableConnectorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void TestInitialize() throws RepositoryException, IOException {
		NamespaceRegistry namespaceRegistry = Mockito
				.mock(NamespaceRegistry.class);
		NodeTypeManager nodeTypeManager = Mockito.mock(NodeTypeManager.class);

		IRODSWriteableConnector irodsWriteableConnector = new IRODSWriteableConnector();
		irodsWriteableConnector.initialize(namespaceRegistry, nodeTypeManager);
		Assert.assertNotNull(irodsWriteableConnector.getConnectorContext());
	}
}
