package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.File;

import junit.framework.Assert;

import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.JargonProperties;
import org.irods.jargon.core.connection.SettableJargonProperties;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileImpl;
import org.irods.jargon.modeshape.connector.InclusionExclusionFilenameFilter;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.ValueFactories;

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
		JargonProperties jargonProperties = new SettableJargonProperties();
		Mockito.when(irodsAccessObjectFactory.getJargonProperties()).thenReturn(jargonProperties);

		IRODSFileFactory irodsFileFactory = Mockito
				.mock(IRODSFileFactory.class);
		IRODSFile mockFile = Mockito.mock(IRODSFile.class);
		File parentFile = Mockito.mock(IRODSFileImpl.class);
		Mockito.when(parentFile.getAbsolutePath()).thenReturn(TEST_ROOT_DIR);
		Mockito.when(mockFile.getParentFile()).thenReturn(parentFile);
		Mockito.when(mockFile.getName()).thenReturn(testIdNoDelim);
		Mockito.when(mockFile.isFile()).thenReturn(true);
		Mockito.when(mockFile.exists()).thenReturn(true);
		Mockito.when(mockFile.lastModified()).thenReturn(0L);
		Mockito.when(mockFile.getAbsolutePath()).thenReturn(
				TEST_ROOT_DIR + "/" + testIdNoDelim);
		Mockito.when(
				irodsFileFactory
						.instanceIRODSFile(TEST_ROOT_DIR, testIdNoDelim))
				.thenReturn(mockFile);

		Mockito.when(irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount))
				.thenReturn(irodsFileFactory);

		InclusionExclusionFilenameFilter filter = Mockito
				.mock(InclusionExclusionFilenameFilter.class);
		Mockito.when(filter.accept(parentFile, testIdNoDelim)).thenReturn(true);

		IrodsWriteableConnector irodsWriteableConnector = Mockito
				.mock(IrodsWriteableConnector.class);

		PathUtilities pathUtilities = new PathUtilities(TEST_ROOT_DIR, filter,
				irodsWriteableConnector);

		Mockito.when(irodsWriteableConnector.getPathUtilities()).thenReturn(
				pathUtilities);
		DocumentWriter documentWriter = Mockito.mock(DocumentWriter.class);
		EditableDocument document = Mockito.mock(EditableDocument.class);
		Mockito.when(documentWriter.document()).thenReturn(document);
		Mockito.when(irodsWriteableConnector.createNewDocumentForId(testId))
				.thenReturn(documentWriter);

		ValueFactories valueFactories = Mockito.mock(ValueFactories.class);
		DateTimeFactory dateFactory = Mockito.mock(DateTimeFactory.class);
		DateTime dateTime = Mockito.mock(DateTime.class);
		Mockito.when(dateFactory.create(0L)).thenReturn(dateTime);
		Mockito.when(valueFactories.getDateFactory()).thenReturn(dateFactory);
		Mockito.when(irodsWriteableConnector.obtainHandleToFactories())
				.thenReturn(valueFactories);

		NodeTypeFactory nodeTypeFactory = new NodeTypeFactoryImpl(
				irodsAccessObjectFactory, irodsAccount, irodsWriteableConnector);
		Document actual = nodeTypeFactory.instanceForId(testId, 0);
		Assert.assertNotNull("null document returned", actual);

	}

}
