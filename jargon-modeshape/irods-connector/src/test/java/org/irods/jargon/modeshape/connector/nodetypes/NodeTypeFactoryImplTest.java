package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.File;

import junit.framework.Assert;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.modeshape.connector.InclusionExclusionFilenameFilter;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.junit.Test;
import org.mockito.Mockito;

public class NodeTypeFactoryImplTest {

	private static final String TEST_ROOT_DIR = "/root/directory";

	@Test
	public void testInstanceFileForId() throws Exception {

		String testId = "/test/id.txt";
		String testIdNoDelim = "/test/id.txt";
		IRODSAccessObjectFactory irodsAccessObjectFactory = Mockito
				.mock(IRODSAccessObjectFactory.class);
		IRODSAccount irodsAccount = IRODSAccount.instance("host", 1247, "user",
				"password", "", "zone", "");

		IRODSFileFactory irodsFileFactory = Mockito
				.mock(IRODSFileFactory.class);
		IRODSFile mockFile = Mockito.mock(IRODSFile.class);
		File parentFile = Mockito.mock(File.class);
		Mockito.when(mockFile.getParentFile()).thenReturn(parentFile);
		Mockito.when(mockFile.getName()).thenReturn(testIdNoDelim);
		Mockito.when(mockFile.isFile()).thenReturn(true);
		Mockito.when(mockFile.exists()).thenReturn(true);
		Mockito.when(
				irodsFileFactory.instanceIRODSFile(TEST_ROOT_DIR + "/",
						testIdNoDelim)).thenReturn(mockFile);

		Mockito.when(irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount))
				.thenReturn(irodsFileFactory);

		InclusionExclusionFilenameFilter filter = Mockito
				.mock(InclusionExclusionFilenameFilter.class);
		Mockito.when(filter.accept(parentFile, testIdNoDelim)).thenReturn(true);
		PathUtilities pathUtilities = new PathUtilities(TEST_ROOT_DIR, filter);
		NodeTypeFactory nodeTypeFactory = new NodeTypeFactoryImpl(
				irodsAccessObjectFactory, irodsAccount, pathUtilities);
		Document document = nodeTypeFactory.instanceForId(testId);
		Assert.assertNotNull("null document returned", document);

	}
}
