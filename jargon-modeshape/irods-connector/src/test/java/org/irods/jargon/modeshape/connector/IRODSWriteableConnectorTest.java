package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

public class IRODSWriteableConnectorTest extends SingleUseAbstractTest {

	protected static final String TEXT_CONTENT = "Some text content";

	private Node testRoot;
	private Projection readOnlyProjection;
	private Projection readOnlyProjectionWithExclusion;
	private Projection readOnlyProjectionWithInclusion;
	private Projection storeProjection;
	private Projection jsonProjection;
	private Projection legacyProjection;
	private Projection noneProjection;
	private Projection pagedProjection;
	private Projection largeFilesProjection;
	private Projection largeFilesProjectionDefault;
	private Projection[] projections;
	private JcrTools tools;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void before() throws Exception {
		tools = new JcrTools();
		readOnlyProjection = new Projection("readonly-files",
				"target/federation/files-read");
		readOnlyProjectionWithExclusion = new Projection(
				"readonly-files-with-exclusion",
				"target/federation/files-read-exclusion");
		readOnlyProjectionWithInclusion = new Projection(
				"readonly-files-with-inclusion",
				"target/federation/files-read-inclusion");
		storeProjection = new Projection("mutable-files-store",
				"target/federation/files-store");
		jsonProjection = new Projection("mutable-files-json",
				"target/federation/files-json");
		legacyProjection = new Projection("mutable-files-legacy",
				"target/federation/files-legacy");
		noneProjection = new Projection("mutable-files-none",
				"target/federation/files-none");
		pagedProjection = new PagedProjection("paged-files",
				"target/federation/paged-files");
		largeFilesProjection = new LargeFilesProjection("large-files",
				"target/federation/large-files");
		largeFilesProjectionDefault = new LargeFilesProjection(
				"large-files-default", "target/federation/large-files-default");

		projections = new Projection[] { readOnlyProjection,
				readOnlyProjectionWithInclusion,
				readOnlyProjectionWithExclusion, storeProjection,
				jsonProjection, legacyProjection, noneProjection,
				pagedProjection, largeFilesProjection,
				largeFilesProjectionDefault };

		// Remove and then make the directory for our federation test ...
		for (Projection projection : projections) {
			projection.initialize();
		}

		startRepositoryWithConfiguration(getClass().getClassLoader()
				.getResourceAsStream(
						"config/repo-config-filesystem-federation.json"));
		registerNodeTypes("cnd/flex.cnd");

		Session session = jcrSession();
		testRoot = session.getRootNode().addNode("testRoot");
		testRoot.addNode("node1");
		session.save();

		readOnlyProjection.create(testRoot, "readonly");
		storeProjection.create(testRoot, "store");
		jsonProjection.create(testRoot, "json");
		legacyProjection.create(testRoot, "legacy");
		noneProjection.create(testRoot, "none");
		pagedProjection.create(testRoot, "pagedFiles");
		largeFilesProjection.create(testRoot, "largeFiles");
		largeFilesProjectionDefault.create(testRoot, "largeFilesDefault");
	}

	@Test
	public void testInitialize() throws RepositoryException, IOException {
		NamespaceRegistry namespaceRegistry = Mockito
				.mock(NamespaceRegistry.class);
		NodeTypeManager nodeTypeManager = Mockito.mock(NodeTypeManager.class);

		IRODSWriteableConnector irodsWriteableConnector = new IRODSWriteableConnector();
		irodsWriteableConnector.initialize(namespaceRegistry, nodeTypeManager);
		Assert.assertNotNull(irodsWriteableConnector.getConnectorContext());
	}

	protected void assertNoSidecarFile(Projection projection, String filePath) {
		assertThat(
				projection
						.getTestFile(
								filePath
										+ JsonSidecarExtraPropertyStore.DEFAULT_EXTENSION)
						.exists(), is(false));
		assertThat(
				projection
						.getTestFile(
								filePath
										+ LegacySidecarExtraPropertyStore.DEFAULT_EXTENSION)
						.exists(), is(false));
		assertThat(
				projection
						.getTestFile(
								filePath
										+ JsonSidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION)
						.exists(), is(false));
		assertThat(
				projection
						.getTestFile(
								filePath
										+ LegacySidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION)
						.exists(), is(false));
	}

	protected void assertJsonSidecarFile(Projection projection, String filePath) {
		File sidecarFile = projection.getTestFile(filePath
				+ JsonSidecarExtraPropertyStore.DEFAULT_EXTENSION);
		if (sidecarFile.exists())
			return;
		sidecarFile = projection.getTestFile(filePath
				+ JsonSidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION);
		assertThat(sidecarFile.exists(), is(true));
	}

	protected void assertFileContains(Projection projection, String filePath,
			InputStream expectedContent) throws IOException {
		assertFileContains(projection, filePath,
				IoUtil.readBytes(expectedContent));
	}

	protected void assertFileContains(Projection projection, String filePath,
			byte[] expectedContent) throws IOException {
		File contentFile = projection.getTestFile(filePath);
		assertThat(contentFile.exists(), is(true));
		byte[] actual = IoUtil.readBytes(contentFile);
		assertThat(actual, is(expectedContent));
	}

	protected void assertBinaryContains(Binary binaryValue,
			byte[] expectedContent) throws IOException, RepositoryException {
		byte[] actual = IoUtil.readBytes(binaryValue.getStream());
		assertThat(actual, is(expectedContent));
	}

	protected void assertLegacySidecarFile(Projection projection,
			String filePath) {
		File sidecarFile = projection.getTestFile(filePath
				+ LegacySidecarExtraPropertyStore.DEFAULT_EXTENSION);
		if (sidecarFile.exists())
			return;
		sidecarFile = projection.getTestFile(filePath
				+ LegacySidecarExtraPropertyStore.DEFAULT_RESOURCE_EXTENSION);
		assertThat(sidecarFile.exists(), is(true));
	}

	protected void assertFolder(Node node, File dir) throws RepositoryException {
		assertThat(dir.exists(), is(true));
		assertThat(dir.canRead(), is(true));
		assertThat(dir.isDirectory(), is(true));
		assertThat(node.getName(), is(dir.getName()));
		assertThat(node.getIndex(), is(1));
		assertThat(node.getPrimaryNodeType().getName(), is("nt:folder"));
		assertThat(node.getProperty("jcr:created").getLong(),
				is(dir.lastModified()));
	}

	protected void assertFile(Node node, File file) throws RepositoryException {
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

	private void assertPathNotFound(String path) throws Exception {
		try {
			session.getNode(path);
			fail(path + " was found, even though it shouldn't have been");
		} catch (PathNotFoundException e) {
			// expected
		}

	}
}

@Immutable
protected class Projection {
	protected final File directory;
	private final String name;

	public Projection(String name, String directoryPath) {
		this.name = name;
		this.directory = new File(directoryPath);
	}

	public String getName() {
		return name;
	}

	public void create(Node parentNode, String childName)
			throws RepositoryException {
		Session session = parentNode.getSession();
		FederationManager fedMgr = session.getWorkspace()
				.getFederationManager();
		fedMgr.createProjection(parentNode.getPath(), getName(), "/", childName);
	}

	public void initialize() throws IOException {
		if (directory.exists())
			FileUtil.delete(directory);
		directory.mkdirs();
		// Make some content ...
		new File(directory, "dir1").mkdir();
		new File(directory, "dir2").mkdir();
		new File(directory, "dir3").mkdir();
		File simpleJson = new File(directory, "dir3/simple.json");
		IoUtil.write(
				getClass().getClassLoader().getResourceAsStream(
						"data/simple.json"), new FileOutputStream(simpleJson));
		File simpleTxt = new File(directory, "dir3/simple.txt");
		IoUtil.write(TEXT_CONTENT, new FileOutputStream(simpleTxt));
	}

	public void delete() {
		if (directory.exists())
			FileUtil.delete(directory);
	}

	public File getTestFile(String relativePath) {
		return new File(directory, relativePath);
	}

	public void testContent(Node federatedNode, String childName)
			throws RepositoryException {
		Session session = federatedNode.getSession();
		String path = federatedNode.getPath() + "/" + childName;

		Node files = session.getNode(path);
		assertThat(files.getName(), is(childName));
		assertThat(files.getPrimaryNodeType().getName(), is("nt:folder"));
		Node dir1 = session.getNode(path + "/dir1");
		Node dir2 = session.getNode(path + "/dir2");
		Node dir3 = session.getNode(path + "/dir3");
		Node simpleJson = session.getNode(path + "/dir3/simple.json");
		Node simpleText = session.getNode(path + "/dir3/simple.txt");
		assertFolder(dir1, getTestFile("dir1"));
		assertFolder(dir2, getTestFile("dir2"));
		assertFolder(dir3, getTestFile("dir3"));
		assertFile(simpleJson, getTestFile("dir3/simple.json"));
		assertFile(simpleText, getTestFile("dir3/simple.txt"));

		// Look up a node by identifier ...
		String externalNodeId = simpleJson.getIdentifier();
		Node simpleJson2 = session.getNodeByIdentifier(externalNodeId);
		assertFile(simpleJson2, getTestFile("dir3/simple.json"));

		// Look up the node again by path ...
		Node simpleJson3 = session.getNode(path + "/dir3/simple.json");
		assertFile(simpleJson3, getTestFile("dir3/simple.json"));

		// Look for a node that isn't there ...
		try {
			session.getNode(path + "/dir3/non-existant.oops");
			fail("Should not have been able to find a non-existing file");
		} catch (PathNotFoundException e) {
			// expected
		}
	}

	@Override
	public String toString() {
		return "Projection: " + name + " (at '" + directory.getAbsolutePath()
				+ "')";
	}
}

protected class PagedProjection extends Projection {

	public PagedProjection(String name, String directoryPath) {
		super(name, directoryPath);
	}

	@Override
	public void testContent(Node federatedNode, String childName)
			throws RepositoryException {
		Session session = federatedNode.getSession();
		String path = federatedNode.getPath() + "/" + childName;

		assertFolder(session, path, "dir1", "dir2", "dir3", "dir4", "dir5");
		assertFolder(session, path + "/dir1", "simple1.json", "simple2.json",
				"simple3.json", "simple4.json", "simple5.json", "simple6.json");
		assertFolder(session, path + "/dir2", "simple1.json", "simple2.json");
		assertFolder(session, path + "/dir3", "simple1.json");
		assertFolder(session, path + "/dir4", "simple1.json", "simple2.json",
				"simple3.json");
		assertFolder(session, path + "/dir5", "simple1.json", "simple2.json",
				"simple3.json", "simple4.json", "simple5.json");
	}

	private void assertFolder(Session session, String path,
			String... childrenNames) throws RepositoryException {
		Node folderNode = session.getNode(path);
		assertThat(folderNode.getPrimaryNodeType().getName(), is("nt:folder"));
		List<String> expectedChildren = new ArrayList<String>(
				Arrays.asList(childrenNames));

		NodeIterator nodes = folderNode.getNodes();
		assertEquals(expectedChildren.size(), nodes.getSize());
		while (nodes.hasNext()) {
			Node node = nodes.nextNode();
			String nodeName = node.getName();
			assertTrue(expectedChildren.contains(nodeName));
			expectedChildren.remove(nodeName);
		}
	}

	@Override
	public void initialize() throws IOException {
		if (directory.exists())
			FileUtil.delete(directory);
		directory.mkdirs();
		// Make some content ...
		new File(directory, "dir1").mkdir();
		addFile("dir1/simple1.json", "data/simple.json");
		addFile("dir1/simple2.json", "data/simple.json");
		addFile("dir1/simple3.json", "data/simple.json");
		addFile("dir1/simple4.json", "data/simple.json");
		addFile("dir1/simple5.json", "data/simple.json");
		addFile("dir1/simple6.json", "data/simple.json");

		new File(directory, "dir2").mkdir();
		addFile("dir2/simple1.json", "data/simple.json");
		addFile("dir2/simple2.json", "data/simple.json");

		new File(directory, "dir3").mkdir();
		addFile("dir3/simple1.json", "data/simple.json");

		new File(directory, "dir4").mkdir();
		addFile("dir4/simple1.json", "data/simple.json");
		addFile("dir4/simple2.json", "data/simple.json");
		addFile("dir4/simple3.json", "data/simple.json");

		new File(directory, "dir5").mkdir();
		addFile("dir5/simple1.json", "data/simple.json");
		addFile("dir5/simple2.json", "data/simple.json");
		addFile("dir5/simple3.json", "data/simple.json");
		addFile("dir5/simple4.json", "data/simple.json");
		addFile("dir5/simple5.json", "data/simple.json");
	}

	private void addFile(String path, String contentFile) throws IOException {
		File file = new File(directory, path);
		IoUtil.write(
				getClass().getClassLoader().getResourceAsStream(contentFile),
				new FileOutputStream(file));
	}

}

protected class LargeFilesProjection extends Projection {

	public LargeFilesProjection(String name, String directoryPath) {
		super(name, directoryPath);
	}

	@Override
	public void testContent(Node federatedNode, String childName)
			throws RepositoryException {
		Session session = federatedNode.getSession();
		String path = federatedNode.getPath() + "/" + childName;
		assertFolder(session, path, "large-file1.png");
	}

	private void assertFolder(Session session, String path,
			String... childrenNames) throws RepositoryException {
		Node folderNode = session.getNode(path);
		assertThat(folderNode.getPrimaryNodeType().getName(), is("nt:folder"));
		List<String> expectedChildren = new ArrayList<String>(
				Arrays.asList(childrenNames));

		NodeIterator nodes = folderNode.getNodes();
		assertEquals(expectedChildren.size(), nodes.getSize());
		while (nodes.hasNext()) {
			Node node = nodes.nextNode();
			String nodeName = node.getName();
			assertTrue(expectedChildren.contains(nodeName));
			expectedChildren.remove(nodeName);
		}
	}

	@Override
	public void initialize() throws IOException {
		if (directory.exists())
			FileUtil.delete(directory);
		directory.mkdirs();
		// Make some content ...
		addFile("large-file1.png", "data/large-file1.png");
	}

	private void addFile(String path, String contentFile) throws IOException {
		File file = new File(directory, path);
		IoUtil.write(
				getClass().getClassLoader().getResourceAsStream(contentFile),
				new FileOutputStream(file));
	}
}
