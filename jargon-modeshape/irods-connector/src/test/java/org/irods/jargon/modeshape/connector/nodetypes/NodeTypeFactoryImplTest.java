package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.irods.jargon.core.connection.IRODSAccount;
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

		IRODSFileFactory irodsFileFactory = Mockito
				.mock(IRODSFileFactory.class);
		IRODSFile mockFile = Mockito.mock(IRODSFile.class);
		File parentFile = (File) Mockito.mock(IRODSFileImpl.class);
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
		PathUtilities pathUtilities = new PathUtilities(TEST_ROOT_DIR, filter);

		IrodsWriteableConnector irodsWriteableConnector = Mockito
				.mock(IrodsWriteableConnector.class);
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

	@Test
	public void testInstanceCollectionForId() throws Exception {

		String testId = "/test/collection";
		String testIdNoDelim = "/test/collection";
		IRODSAccessObjectFactory irodsAccessObjectFactory = Mockito
				.mock(IRODSAccessObjectFactory.class);
		IRODSAccount irodsAccount = IRODSAccount.instance("host", 1247, "user",
				"password", "", "zone", "");

		InclusionExclusionFilenameFilter filter = Mockito
				.mock(InclusionExclusionFilenameFilter.class);

		IRODSFileFactory irodsFileFactory = Mockito
				.mock(IRODSFileFactory.class);
		IRODSFile mockFile = Mockito.mock(IRODSFile.class);
		File parentFile = Mockito.mock(File.class);
		File[] children = new File[0];
		Mockito.when(parentFile.getAbsolutePath()).thenReturn(TEST_ROOT_DIR);
		Mockito.when(mockFile.getParentFile()).thenReturn(parentFile);
		Mockito.when(mockFile.getName()).thenReturn(testIdNoDelim);
		Mockito.when(mockFile.isFile()).thenReturn(false);
		Mockito.when(mockFile.exists()).thenReturn(true);
		Mockito.when(mockFile.lastModified()).thenReturn(0L);
		Mockito.when(mockFile.getAbsolutePath()).thenReturn(
				TEST_ROOT_DIR + "/" + testIdNoDelim);
		Mockito.when(mockFile.listFiles(filter)).thenReturn(children);
		Mockito.when(
				irodsFileFactory
						.instanceIRODSFile(TEST_ROOT_DIR, testIdNoDelim))
				.thenReturn(mockFile);

		Mockito.when(irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount))
				.thenReturn(irodsFileFactory);

		Mockito.when(filter.accept(parentFile, testIdNoDelim)).thenReturn(true);
		PathUtilities pathUtilities = new PathUtilities(TEST_ROOT_DIR, filter);

		IrodsWriteableConnector irodsWriteableConnector = Mockito
				.mock(IrodsWriteableConnector.class);
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

	@Test
	public void testInstanceContentForId() throws Exception {

		String testId = "/test/id.txt/jcr:content";
		String testIdNoDelim = "/test/id.txt";
		IRODSAccessObjectFactory irodsAccessObjectFactory = Mockito
				.mock(IRODSAccessObjectFactory.class);
		IRODSAccount irodsAccount = IRODSAccount.instance("host", 1247, "user",
				"password", "", "zone", "");

		IRODSFileFactory irodsFileFactory = Mockito
				.mock(IRODSFileFactory.class);
		IRODSFile mockFile = Mockito.mock(IRODSFile.class);
		File parentFile = Mockito.mock(File.class);
		Mockito.when(parentFile.getAbsolutePath()).thenReturn(TEST_ROOT_DIR);
		Mockito.when(mockFile.getParentFile()).thenReturn(parentFile);
		Mockito.when(mockFile.getName()).thenReturn(testIdNoDelim);
		Mockito.when(mockFile.isFile()).thenReturn(true);
		Mockito.when(mockFile.exists()).thenReturn(true);
		Mockito.when(mockFile.lastModified()).thenReturn(0L);
		Mockito.when(mockFile.length()).thenReturn(100L);
		Mockito.when(mockFile.getAbsolutePath()).thenReturn(
				TEST_ROOT_DIR + "/" + testIdNoDelim);
		Mockito.when(mockFile.toFileBasedURL()).thenReturn(
				new URL("file://blah"));
		Mockito.when(
				irodsFileFactory
						.instanceIRODSFile(TEST_ROOT_DIR, testIdNoDelim))
				.thenReturn(mockFile);

		Mockito.when(irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount))
				.thenReturn(irodsFileFactory);

		InclusionExclusionFilenameFilter filter = Mockito
				.mock(InclusionExclusionFilenameFilter.class);
		Mockito.when(filter.accept(parentFile, testIdNoDelim)).thenReturn(true);
		PathUtilities pathUtilities = new PathUtilities(TEST_ROOT_DIR, filter);

		IrodsWriteableConnector irodsWriteableConnector = Mockito
				.mock(IrodsWriteableConnector.class);
		Mockito.when(irodsWriteableConnector.getPathUtilities()).thenReturn(
				pathUtilities);
		DocumentWriter documentWriter = Mockito.mock(DocumentWriter.class);
		EditableDocument document = Mockito.mock(EditableDocument.class);
		Mockito.when(documentWriter.document()).thenReturn(document);
		Mockito.when(
				irodsWriteableConnector.createNewDocumentForId(testIdNoDelim))
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
