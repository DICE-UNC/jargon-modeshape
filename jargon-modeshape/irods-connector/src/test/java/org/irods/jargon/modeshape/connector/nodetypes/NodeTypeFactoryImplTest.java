package org.irods.jargon.modeshape.connector.nodetypes;

import junit.framework.Assert;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.modeshape.connector.InclusionExclusionFilenameFilter;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.junit.Test;
import org.mockito.Mockito;

public class NodeTypeFactoryImplTest {

	private static final String TEST_ROOT_DIR = "/root/directory";

	@Test
	public void testInstanceFileForId() throws Exception {
		IRODSAccessObjectFactory irodsAccessObjectFactory = Mockito
				.mock(IRODSAccessObjectFactory.class);
		IRODSAccount irodsAccount = IRODSAccount.instance("host", 1247, "user",
				"password", "", "zone", "");

		String testId = "/test/id";

		InclusionExclusionFilenameFilter filter = Mockito
				.mock(InclusionExclusionFilenameFilter.class);
		PathUtilities pathUtilities = new PathUtilities(TEST_ROOT_DIR, filter);
		NodeTypeFactory nodeTypeFactory = new NodeTypeFactoryImpl(
				irodsAccessObjectFactory, irodsAccount, pathUtilities);
		Document document = nodeTypeFactory.instanceForId(testId);
		Assert.assertNotNull("null document returned", document);

	}
}
