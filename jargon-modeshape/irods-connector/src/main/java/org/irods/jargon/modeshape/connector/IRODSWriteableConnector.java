/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.NoResourceDefinedException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.core.utils.CollectionAndPath;
import org.irods.jargon.core.utils.MiscIRODSUtils;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.federation.spi.ConnectorException;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.ExtraPropertiesStore;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.federation.spi.WritableConnector;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.basic.BasicMultiValueProperty;
import org.modeshape.jcr.value.basic.BasicName;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.UrlBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModeShape SPI connector
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class IRODSWriteableConnector extends WritableConnector implements
		ExtraPropertiesStore, Pageable {

	private ConnectorContext connectorContext;
	public static final Logger log = LoggerFactory
			.getLogger(IRODSWriteableConnector.class);

	private static final String FILE_SEPARATOR = System
			.getProperty("file.separator");
	private static final String DELIMITER = "/";
	private static final String NT_FOLDER = "nt:folder";
	private static final String NT_FILE = "nt:file";
	private static final String NT_RESOURCE = "nt:resource";
	private static final String MIX_MIME_TYPE = "mix:mimeType";
	private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
	private static final String JCR_DATA = "jcr:data";
	private static final String JCR_MIME_TYPE = "jcr:mimeType";
	private static final String JCR_ENCODING = "jcr:encoding";
	private static final String JCR_CREATED = "jcr:created";
	private static final String JCR_CREATED_BY = "jcr:createdBy";
	private static final String JCR_LAST_MODIFIED = "jcr:lastModified";
	private static final String JCR_LAST_MODIFIED_BY = "jcr:lastModified";
	private static final String JCR_CONTENT = "jcr:content";
	private static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
	private static final int JCR_CONTENT_SUFFIX_LENGTH = JCR_CONTENT_SUFFIX
			.length();

	private static final String EXTRA_PROPERTIES_JSON = "json";
	private static final String EXTRA_PROPERTIES_LEGACY = "legacy";
	private static final String EXTRA_PROPERTIES_NONE = "none";

	/**
	 * The string path for a {@link File} object that represents the top-level
	 * directory accessed by this connector. This is set via reflection and is
	 * required for this connector.
	 */
	private String directoryPath;
	private File directory;

	/**
	 * A string that is created in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method that
	 * represents the absolute path to the {@link #directory}. This path is
	 * removed from an absolute path of a file to obtain the ID of the node.
	 */
	private String directoryAbsolutePath;
	private int directoryAbsolutePathLength;

	/**
	 * A boolean flag that specifies whether this connector should add the
	 * 'mix:mimeType' mixin to the 'nt:resource' nodes to include the
	 * 'jcr:mimeType' property. If set to <code>true</code>, the MIME type is
	 * computed immediately when the 'nt:resource' node is accessed, which might
	 * be expensive for larger files. This is <code>false</code> by default.
	 */
	private final boolean addMimeTypeMixin = false;

	/**
	 * The regular expression that, if matched by a file or folder, indicates
	 * that the file or folder should be included.
	 */
	private String inclusionPattern;

	/**
	 * The regular expression that, if matched by a file or folder, indicates
	 * that the file or folder should be ignored.
	 */
	private String exclusionPattern;

	/**
	 * The maximum number of children a folder will expose at any given time.
	 */
	private final int pageSize = 20;

	/**
	 * The {@link FilenameFilter} implementation that is instantiated in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
	 */
	private InclusionExclusionFilenameFilter filenameFilter;

	/**
	 * A string that specifies how the "extra" properties are to be stored,
	 * where an "extra" property is any JCR property that cannot be stored
	 * natively on the file system as file attributes. This field is set via
	 * reflection, and the value is expected to be one of these valid values:
	 * <ul>
	 * <li>"<code>store</code>" - Any extra properties are stored in the same
	 * Infinispan cache where the content is stored. This is the default and is
	 * used if the actual value doesn't match any of the other accepted values.</li>
	 * <li>"<code>json</code>" - Any extra properties are stored in a JSON file
	 * next to the file or directory.</li>
	 * <li>"<code>legacy</code>" - Any extra properties are stored in a
	 * ModeShape 2.x-compatible file next to the file or directory. This is
	 * generally discouraged unless you were using ModeShape 2.x and have a
	 * directory structure that already contains these files.</li>
	 * <li>"<code>none</code>" - An extra properties that prevents the storage
	 * of extra properties by throwing an exception when such extra properties
	 * are defined.</li>
	 * </ul>
	 */
	private String extraPropertiesStorage;

	/**
	 * A boolean which determines whether for external binary values (i.e.
	 * {@link UrlBinaryValue}) the SHA1 is computed based on the content of the
	 * file itself or whether it's computed based on the URL string. This is
	 * {@code true} by default, but if the connector needs to deal with very
	 * large values it might be worth turning off.
	 */
	private final boolean contentBasedSha1 = true;

	private NamespaceRegistry registry;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.federation.spi.Connector#initialize(javax.jcr.
	 * NamespaceRegistry, org.modeshape.jcr.api.nodetype.NodeTypeManager)
	 */
	@Override
	public void initialize(final NamespaceRegistry registry,
			final NodeTypeManager nodeTypeManager) throws RepositoryException,
			IOException {
		super.initialize(registry, nodeTypeManager);
		connectorContext = new ConnectorContext();
		log.info("getting documentMapper");
		log.info("initialized");

	}

	/**
	 * Get the namespace registry.
	 * 
	 * @return the namespace registry; never null
	 */
	NamespaceRegistry registry() {
		return registry;
	}

	/**
	 * Utility method for determining if the supplied identifier is for the
	 * "jcr:content" child node of a file. * Subclasses may override this method
	 * to change the format of the identifiers, but in that case should also
	 * override the {@link #fileFor(String)}, {@link #isRoot(String)}, and
	 * {@link #idFor(File)} methods.
	 * 
	 * @param id
	 *            the identifier; may not be null
	 * @return true if the identifier signals the "jcr:content" child node of a
	 *         file, or false otherwise
	 * @see #isRoot(String)
	 * @see #fileFor(String)
	 * @see #idFor(File)
	 */
	protected boolean isContentNode(final String id) {
		return id.endsWith(JCR_CONTENT_SUFFIX);
	}

	/**
	 * Utility method for obtaining the {@link File} object that corresponds to
	 * the supplied identifier. Subclasses may override this method to change
	 * the format of the identifiers, but in that case should also override the
	 * {@link #isRoot(String)}, {@link #isContentNode(String)}, and
	 * {@link #idFor(File)} methods.
	 * 
	 * @param id
	 *            the identifier; may not be null
	 * @return the File object for the given identifier
	 * @see #isRoot(String)
	 * @see #isContentNode(String)
	 * @see #idFor(File)
	 */
	protected File fileFor(String id, final boolean closeInFinally) {
		assert id.startsWith(DELIMITER);
		if (id.endsWith(DELIMITER)) {
			id = id.substring(0, id.length() - DELIMITER.length());
		}
		if (isContentNode(id)) {
			id = id.substring(0, id.length() - JCR_CONTENT_SUFFIX_LENGTH);
		}

		try {
			return (File) getConnectorContext().getIrodsAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount())
					.instanceIRODSFile(directory.getAbsolutePath(), id);
		} catch (JargonException e) {
			log.error("error getting irods file", e);
			throw new ConnectorException(e);
		} finally {
			if (closeInFinally) {
				getConnectorContext().getIrodsAccessObjectFactory()
						.closeSessionAndEatExceptions();
			}
		}

	}

	/**
	 * Utility method for determining if the node identifier is the identifier
	 * of the root node in this external source. Subclasses may override this
	 * method to change the format of the identifiers, but in that case should
	 * also override the {@link #fileFor(String)},
	 * {@link #isContentNode(String)}, and {@link #idFor(File)} methods.
	 * 
	 * @param id
	 *            the identifier; may not be null
	 * @return true if the identifier is for the root of this source, or false
	 *         otherwise
	 * @see #isContentNode(String)
	 * @see #fileFor(String)
	 * @see #idFor(File)
	 */
	protected boolean isRoot(final String id) {
		return DELIMITER.equals(id);
	}

	/**
	 * Utility method for determining the node identifier for the supplied file.
	 * Subclasses may override this method to change the format of the
	 * identifiers, but in that case should also override the
	 * {@link #fileFor(String)}, {@link #isContentNode(String)}, and
	 * {@link #isRoot(String)} methods.
	 * 
	 * @param file
	 *            the file; may not be null
	 * @return the node identifier; never null
	 * @see #isRoot(String)
	 * @see #isContentNode(String)
	 * @see #fileFor(String)
	 */
	protected String idFor(final File file) {
		String path = file.getAbsolutePath();
		if (!path.startsWith(directoryAbsolutePath)) {
			if (directory.getAbsolutePath().equals(path)) {
				// This is the root
				return DELIMITER;
			}
			String msg = JcrI18n.fileConnectorNodeIdentifierIsNotWithinScopeOfConnector
					.text(getSourceName(), directoryPath, path);
			throw new DocumentStoreException(path, msg);
		}
		String id = path.substring(directoryAbsolutePathLength);
		id = id.replaceAll(Pattern.quote(FILE_SEPARATOR), DELIMITER);
		assert id.startsWith(DELIMITER);
		return id;
	}

	/**
	 * Utility method for creating a {@link BinaryValue} for the given
	 * {@link File} object. Subclasses should rarely override this method.
	 * 
	 * @param file
	 *            the file; may not be null
	 * @return the BinaryValue; never null
	 */
	protected ExternalBinaryValue binaryFor(final File file) {
		try {
			return createBinaryValue(file);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility method to create a {@link BinaryValue} object for the given file.
	 * Subclasses should rarely override this method, since the
	 * {@link UrlBinaryValue} will be applicable in most situations.
	 * 
	 * @param file
	 *            the file for which the {@link BinaryValue} is to be created;
	 *            never null
	 * @return the binary value; never null
	 * @throws IOException
	 *             if there is an error creating the value
	 */
	protected ExternalBinaryValue createBinaryValue(final File file)
			throws IOException {
		URL content = createUrlForFile(file);
		return new IRODSBinaryValue(sha1(file), getSourceName(), content,
				file.length(), file.getName(), getMimeTypeDetector(),
				connectorContext);
	}

	/**
	 * Computes the SHA1 for the given file. By default, this method will look
	 * at the {@link FileSystemConnector#contentBasedSha1()} flag and either
	 * take the URL of the file (using @see java.util.File#toURI().toURL() and
	 * return the SHA1 of the URL string or return the SHA1 of the entire file
	 * content.
	 * 
	 * @param file
	 *            a {@link File} instance; never null
	 * @return the SHA1 of the file.
	 */
	protected String sha1(final File file) {
		try {
			if (contentBasedSha1()) {
				byte[] hash = SecureHash.getHash(SecureHash.Algorithm.SHA_1,
						file);
				return StringUtil.getHexString(hash);
			} else {
				return SecureHash.sha1(createUrlForFile(file).toString());
			}
		} catch (Exception e) {
			throw new ConnectorException(e);
		}
	}

	/**
	 * Construct a {@link URL} object for the given file, to be used within the
	 * {@link Binary} value representing the "jcr:data" property of a
	 * 'nt:resource' node.
	 * <p>
	 * Subclasses can override this method to transform the URL into something
	 * different. For example, if the files are being served by a web server,
	 * the overridden method might transform the file-based URL into the
	 * corresponding HTTP-based URL.
	 * </p>
	 * 
	 * @param file
	 *            the file for which the URL is to be created; never null
	 * @return the URL for the file; never null
	 * @throws IOException
	 *             if there is an error creating the URL
	 */
	protected URL createUrlForFile(final File file) throws IOException {
		return file.toURI().toURL();
	}

	protected File createFileForUrl(final URL url) throws URISyntaxException {

		try {

			IRODSFileFactory irodsFileFactory = connectorContext
					.getIrodsAccessObjectFactory().getIRODSFileFactory(
							connectorContext.getProxyAccount());

			String stringFormOfUrl = url.toExternalForm();

			IRODSFile irodsFile = irodsFileFactory
					.instanceIRODSFile(stringFormOfUrl);
			return (File) irodsFile;

		} catch (JargonException e) {
			throw new ConnectorException(e);
		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}

	}

	protected boolean contentBasedSha1() {
		return contentBasedSha1;
	}

	/**
	 * Utility method to determine if the file is excluded by the
	 * inclusion/exclusion filter.
	 * 
	 * @param file
	 *            the file
	 * @return true if the file is excluded, or false if it is to be included
	 */
	protected boolean isExcluded(final File file) {
		return !filenameFilter.accept(file.getParentFile(), file.getName());
	}

	/**
	 * Utility method to ensure that the file is writable by this connector.
	 * 
	 * @param id
	 *            the identifier of the node
	 * @param file
	 *            the file
	 * @throws DocumentStoreException
	 *             if the file is expected to be writable but is not or is
	 *             excluded, or if the connector is readonly
	 */
	protected void checkFileNotExcluded(final String id, final File file) {
		if (isExcluded(file)) {
			String msg = JcrI18n.fileConnectorCannotStoreFileThatIsExcluded
					.text(getSourceName(), id, file.getAbsolutePath());
			throw new DocumentStoreException(id, msg);
		}
	}

	@Override
	public boolean hasDocument(final String id) {
		return fileFor(id, true).exists();
	}

	@Override
	public Document getDocumentById(final String id) {

		try {

			File file = fileFor(id, false);
			if (isExcluded(file) || !file.exists()) {
				return null;
			}
			boolean isRoot = isRoot(id);
			boolean isResource = isContentNode(id);
			DocumentWriter writer = null;
			File parentFile = file.getParentFile();
			if (isResource) {

				writer = newDocument(id);
				BinaryValue binaryValue = binaryFor(file);
				writer.setPrimaryType(NT_RESOURCE);
				writer.addProperty(JCR_DATA, binaryValue);
				if (addMimeTypeMixin) {
					String mimeType = null;
					String encoding = null; // We don't really know this
					try {
						mimeType = binaryValue.getMimeType();
					} catch (Throwable e) {
						getLogger().error(e, JcrI18n.couldNotGetMimeType,
								getSourceName(), id, e.getMessage());
					}
					writer.addProperty(JCR_ENCODING, encoding);
					writer.addProperty(JCR_MIME_TYPE, mimeType);
				}
				writer.addProperty(JCR_LAST_MODIFIED, factories()
						.getDateFactory().create(file.lastModified()));
				writer.addProperty(JCR_LAST_MODIFIED_BY, null); // ignored

				// make these binary not queryable. If we really want to query
				// them,
				// we need to switch to external binaries
				writer.setNotQueryable();
				parentFile = file;
			} else if (file.isFile()) {
				writer = newDocument(id);
				writer.setPrimaryType(NT_FILE);
				writer.addProperty(JCR_CREATED, factories().getDateFactory()
						.create(file.lastModified()));
				writer.addProperty(JCR_CREATED_BY, null); // ignored
				String childId = isRoot ? JCR_CONTENT_SUFFIX : id
						+ JCR_CONTENT_SUFFIX;
				writer.addChild(childId, JCR_CONTENT);
			} else {
				writer = newFolderWriter(id, file, 0);
			}

			if (!isRoot) {
				// Set the reference to the parent ...
				String parentId = idFor(parentFile);
				writer.setParents(parentId);
			}

			// Add the extra properties (if there are any), overwriting any
			// properties with the same names
			// (e.g., jcr:primaryType, jcr:mixinTypes, jcr:mimeType, etc.) ...
			writer.addProperties(extraPropertiesStore().getProperties(id));

			// Add the 'mix:mixinType' mixin; if other mixins are stored in the
			// extra properties, this will append ...
			if (addMimeTypeMixin) {
				writer.addMixinType(MIX_MIME_TYPE);
			}

			// Return the document ...
			return writer.document();

		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}

	}

	private DocumentWriter newFolderWriter(final String id, final File file,
			final int offset) {
		boolean root = isRoot(id);
		DocumentWriter writer = newDocument(id);
		writer.setPrimaryType(NT_FOLDER);
		writer.addProperty(JCR_CREATED,
				factories().getDateFactory().create(file.lastModified()));
		writer.addProperty(JCR_CREATED_BY, null); // ignored
		File[] children = file.listFiles(filenameFilter);
		long totalChildren = 0;
		int nextOffset = 0;
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			// Only include as a child if we can access and read the file.
			// Permissions might prevent us from
			// reading the file, and the file might not exist if it is a broken
			// symlink (see MODE-1768 for details).
			if (child.exists() && child.canRead()
					&& (child.isFile() || child.isDirectory())) {
				// we need to count the total accessible children
				totalChildren++;
				// only add a child if it's in the current page
				if (i >= offset && i < offset + pageSize) {
					// We use identifiers that contain the file/directory name
					// ...
					String childName = child.getName();
					String childId = root ? DELIMITER + childName : id
							+ DELIMITER + childName;
					writer.addChild(childId, childName);
					nextOffset = i + 1;
				}
			}
		}
		// if there are still accessible children add the next page
		if (nextOffset < totalChildren) {
			writer.addPage(id, nextOffset, pageSize, totalChildren);
		}
		return writer;
	}

	@Override
	public String getDocumentId(final String path) {
		String id = path; // this connector treats the ID as the path

		try {
			File file = fileFor(id, true);
			return file.exists() ? id : null;

		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}
	}

	@Override
	public Collection<String> getDocumentPathsById(final String id) {
		// this connector treats the ID as the path
		return Collections.singletonList(id);
	}

	@Override
	public ExternalBinaryValue getBinaryValue(final String id) {
		try {
			File f = createFileForUrl(new URL(id));
			return binaryFor(f);
		} catch (IOException e) {
			throw new DocumentStoreException(id, e);
		} catch (URISyntaxException e) {
			throw new DocumentStoreException(id, e);
		}
	}

	@Override
	public boolean removeDocument(final String id) {

		try {

			File file = fileFor(id, false);
			checkFileNotExcluded(id, file);
			// Remove the extra properties at the old location ...
			extraPropertiesStore().removeProperties(id);
			// Now remove the file (if it is there) ...
			if (!file.exists()) {
				return false;
			}

			IRODSFile irodsFile = (IRODSFile) file;
			irodsFile.delete();

			return true;
		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}
	}

	@Override
	public void storeDocument(final Document document) {
		// Create a new directory or file described by the document ...
		try {
			DocumentReader reader = readDocument(document);
			String id = reader.getDocumentId();
			File file = fileFor(id, false);
			checkFileNotExcluded(id, file);
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			if (!parent.canWrite()) {
				String parentPath = parent.getAbsolutePath();
				String msg = JcrI18n.fileConnectorCannotWriteToDirectory.text(
						getSourceName(), getClass(), parentPath);
				throw new DocumentStoreException(id, msg);
			}
			String primaryType = reader.getPrimaryTypeName();
			Map<Name, Property> properties = reader.getProperties();
			ExtraProperties extraProperties = extraPropertiesFor(id, false);
			extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE,
					JCR_CREATED, JCR_LAST_MODIFIED, JCR_DATA);
			try {
				if (NT_FILE.equals(primaryType)) {
					file.createNewFile();
				} else if (NT_FOLDER.equals(primaryType)) {
					file.mkdirs();
				} else if (isContentNode(id)) {
					Property content = properties.get(JcrLexicon.DATA);
					BinaryValue binary = factories().getBinaryFactory().create(
							content.getFirstValue());
					IRODSFile irodsFile = (IRODSFile) file;

					OutputStream ostream = new BufferedOutputStream(
							connectorContext
									.getIrodsAccessObjectFactory()
									.getIRODSFileFactory(getIrodsAccount())
									.instanceSessionClosingIRODSFileOutputStream(
											irodsFile));
					IoUtil.write(binary.getStream(), ostream);
					if (!NT_RESOURCE.equals(primaryType)) {
						// This is the "jcr:content" child, but the primary type
						// is
						// non-standard so record it as an extra property
						extraProperties.add(properties
								.get(JcrLexicon.PRIMARY_TYPE));
					}
				}
				extraProperties.save();
			} catch (RepositoryException e) {
				throw new DocumentStoreException(id, e);
			} catch (IOException e) {
				throw new DocumentStoreException(id, e);
			} catch (NoResourceDefinedException e) {
				throw new DocumentStoreException(id, e);
			} catch (JargonException e) {
				throw new DocumentStoreException(id, e);
			}
		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}
	}

	@Override
	public String newDocumentId(final String parentId,
			final Name newDocumentName, final Name newDocumentPrimaryType) {
		StringBuilder id = new StringBuilder(parentId);
		if (!parentId.endsWith(DELIMITER)) {
			id.append(DELIMITER);
		}

		// We're only using the name to check, which can be a bit dangerous if
		// users don't follow the JCR conventions.
		// However, it matches what "isContentNode(...)" does.
		String childNameStr = getContext().getValueFactories()
				.getStringFactory().create(newDocumentName);
		if (JCR_CONTENT.equals(childNameStr)) {
			// This is for the "jcr:content" node underneath a file node. Since
			// this doesn't actually result in a file or folder
			// on the file system (it's merged into the file for the parent
			// 'nt:file' node), we'll keep the "jcr" namespace
			// prefix in the ID so that 'isContentNode(...)' works properly ...
			id.append(childNameStr);
		} else {
			// File systems don't universally deal well with ':' in the names,
			// and when they do it can be a bit awkward. Since we
			// don't often expect the node NAMES to contain namespaces (at leat
			// with this connector), we'll just
			// use the local part for the ID ...
			id.append(newDocumentName.getLocalName());
			if (!StringUtil.isBlank(newDocumentName.getNamespaceUri())) {
				// the FS connector does not support namespaces in names
				String ns = newDocumentName.getNamespaceUri();
				getLogger().warn(JcrI18n.fileConnectorNamespaceIgnored,
						getSourceName(), ns, id, childNameStr, parentId);
			}
		}
		return id.toString();
	}

	@Override
	public void updateDocument(final DocumentChanges documentChanges) {

		try {

			String id = documentChanges.getDocumentId();

			Document document = documentChanges.getDocument();
			DocumentReader reader = readDocument(document);

			File file = fileFor(id, false);
			String idOrig = id;

			// if we're dealing with the root of the connector, we can't process
			// any
			// moves/removes because that would go "outside" the
			// connector scope
			if (!isRoot(id)) {
				String parentId = reader.getParentIds().get(0);
				File parent = file.getParentFile();
				String newParentId = idFor(parent);
				if (!parentId.equals(newParentId)) {
					// The node has a new parent (via the 'update' method),
					// meaning
					// it was moved ...
					File newParent = fileFor(newParentId, false);
					File newFile;
					try {
						newFile = (File) connectorContext
								.getIrodsAccessObjectFactory()
								.getIRODSFileFactory(getIrodsAccount())
								.instanceIRODSFile(newParent, file.getName());
					} catch (JargonException e) {
						throw new DocumentStoreException(id, e);

					}
					file.renameTo(newFile);
					if (!parent.exists()) {
						parent.mkdirs(); // in case they were removed since we
											// created them ...
					}
					if (!parent.canWrite()) {
						String parentPath = newParent.getAbsolutePath();
						String msg = JcrI18n.fileConnectorCannotWriteToDirectory
								.text(getSourceName(), getClass(), parentPath);
						throw new DocumentStoreException(id, msg);
					}
					parent = newParent;
					// Remove the extra properties at the old location ...
					extraPropertiesStore().removeProperties(id);
					// Set the id to the new location ...
					id = idFor(newFile);
				} else {
					// It is the same parent as before ...
					if (!parent.exists()) {
						parent.mkdirs(); // in case they were removed since we
											// created them ...
					}
					if (!parent.canWrite()) {
						String parentPath = parent.getAbsolutePath();
						String msg = JcrI18n.fileConnectorCannotWriteToDirectory
								.text(getSourceName(), getClass(), parentPath);
						throw new DocumentStoreException(id, msg);
					}
				}
			}

			// children renames have to be processed in the parent
			DocumentChanges.ChildrenChanges childrenChanges = documentChanges
					.getChildrenChanges();
			Map<String, Name> renamedChildren = childrenChanges.getRenamed();
			for (String renamedChildId : renamedChildren.keySet()) {
				File child = fileFor(renamedChildId, false);
				Name newName = renamedChildren.get(renamedChildId);
				String newNameStr = getContext().getValueFactories()
						.getStringFactory().create(newName);
				File renamedChild;
				try {
					renamedChild = (File) connectorContext
							.getIrodsAccessObjectFactory()
							.getIRODSFileFactory(getIrodsAccount())
							.instanceIRODSFile(file, newNameStr);
				} catch (JargonException e) {
					throw new DocumentStoreException(id, e);
				}

				if (!child.renameTo(renamedChild)) {
					getLogger().debug("Cannot rename {0} to {1}", child,
							renamedChild);
				}
			}

			String primaryType = reader.getPrimaryTypeName();
			Map<Name, Property> properties = reader.getProperties();
			id = idOrig;
			ExtraProperties extraProperties = extraPropertiesFor(id, true);
			extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE,
					JCR_CREATED, JCR_LAST_MODIFIED, JCR_DATA);
			try {
				if (NT_FILE.equals(primaryType)) {
					file.createNewFile();
				} else if (NT_FOLDER.equals(primaryType)) {
					file.mkdir();
				} else if (isContentNode(id)) {
					Property content = reader.getProperty(JCR_DATA);
					BinaryValue binary = factories().getBinaryFactory().create(
							content.getFirstValue());
					IRODSFile irodsFile = (IRODSFile) file;

					OutputStream ostream;
					try {
						ostream = new BufferedOutputStream(connectorContext
								.getIrodsAccessObjectFactory()
								.getIRODSFileFactory(getIrodsAccount())
								.instanceSessionClosingIRODSFileOutputStream(
										irodsFile));
					} catch (NoResourceDefinedException e) {
						throw new DocumentStoreException(id, e);
					} catch (JargonException e) {
						throw new DocumentStoreException(id, e);
					}
					IoUtil.write(binary.getStream(), ostream);
					if (!NT_RESOURCE.equals(primaryType)) {
						// This is the "jcr:content" child, but the primary type
						// is
						// non-standard so record it as an extra property
						extraProperties.add(properties
								.get(JcrLexicon.PRIMARY_TYPE));
					}
				}
				extraProperties.save();
			} catch (RepositoryException e) {
				throw new DocumentStoreException(id, e);
			} catch (IOException e) {
				throw new DocumentStoreException(id, e);
			}
		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}
	}

	@Override
	public Document getChildren(final PageKey pageKey) {
		try {

			String parentId = pageKey.getParentId();
			File folder = fileFor(parentId, false);
			assert folder.isDirectory();
			if (!folder.canRead()) {
				getLogger().debug("Cannot read the {0} folder",
						folder.getAbsolutePath());
				return null;
			}
			return newFolderWriter(parentId, folder, pageKey.getOffsetInt())
					.document();
		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}
	}

	@Override
	public Map<Name, Property> getProperties(final String arg0) {
		try {
			CollectionAndDataObjectListAndSearchAO collectionAndDataObjectListAndSearchAO = connectorContext
					.getIrodsAccessObjectFactory()
					.getCollectionAndDataObjectListAndSearchAO(
							getIrodsAccount());

			ObjStat objStat = collectionAndDataObjectListAndSearchAO
					.retrieveObjectStatForPath(arg0);

			if (objStat.isSomeTypeOfCollection()) {
				return getPropertiesForCollection(arg0);
			} else {
				CollectionAndPath collectionAndPath = MiscIRODSUtils
						.separateCollectionAndPathFromGivenAbsolutePath(arg0);
				return getPropertiesForDataObject(
						collectionAndPath.getCollectionParent(),
						collectionAndPath.getChildName());
			}

		} catch (JargonException e) {
			throw new DocumentStoreException(arg0, "error getting properties");

		} catch (JargonQueryException e) {
			throw new DocumentStoreException(arg0, "error getting properties");

		} finally {
			getConnectorContext().getIrodsAccessObjectFactory()
					.closeSessionAndEatExceptions();
		}

	}

	private Map<Name, Property> getPropertiesForCollection(final String path)
			throws JargonException, JargonQueryException {
		Map<Name, Property> props = new HashMap<Name, Property>();
		Property property;

		CollectionAO collectionAO = connectorContext
				.getIrodsAccessObjectFactory().getCollectionAO(
						getIrodsAccount());

		List<MetaDataAndDomainData> metadatas = collectionAO
				.findMetadataValuesForCollection(path, 0);

		List<Object> avuVals;
		for (MetaDataAndDomainData metadata : metadatas) {
			avuVals = new ArrayList<Object>();
			avuVals.add(metadata.getAvuAttribute());
			avuVals.add(metadata.getAvuValue());
			avuVals.add(metadata.getAvuUnit());

			property = new BasicMultiValueProperty(new BasicName("irods",
					metadata.getAvuAttribute()), avuVals);

			props.put(property.getName(), property);

		}

		return props;

	}

	private Map<Name, Property> getPropertiesForDataObject(final String path,
			final String name) throws JargonException, JargonQueryException {
		Map<Name, Property> props = new HashMap<Name, Property>();
		Property property;

		DataObjectAO dataObjectAO = connectorContext
				.getIrodsAccessObjectFactory().getDataObjectAO(
						getIrodsAccount());

		List<MetaDataAndDomainData> metadatas = dataObjectAO
				.findMetadataValuesForDataObject(path, name);

		List<Object> avuVals;
		for (MetaDataAndDomainData metadata : metadatas) {
			avuVals = new ArrayList<Object>();
			avuVals.add(metadata.getAvuAttribute());
			avuVals.add(metadata.getAvuValue());
			avuVals.add(metadata.getAvuUnit());

			property = new BasicMultiValueProperty(new BasicName("irods",
					metadata.getAvuAttribute()), avuVals);

			props.put(property.getName(), property);

		}

		return props;
	}

	@Override
	public boolean removeProperties(final String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void storeProperties(final String arg0,
			final Map<Name, Property> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProperties(final String arg0,
			final Map<Name, Property> arg1) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the connectorContext
	 */
	public ConnectorContext getConnectorContext() {
		return connectorContext;
	}

	/**
	 * @param connectorContext
	 *            the connectorContext to set
	 */
	public void setConnectorContext(final ConnectorContext connectorContext) {
		this.connectorContext = connectorContext;
	}

	private IRODSAccount getIrodsAccount() {
		IRODSAccount irodsAccount = connectorContext.getProxyAccount();
		if (irodsAccount == null) {
			throw new IllegalStateException("null irods account");
		}
		return irodsAccount;
	}

}
