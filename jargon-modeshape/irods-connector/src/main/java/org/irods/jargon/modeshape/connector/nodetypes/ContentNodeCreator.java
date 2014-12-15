/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.NoResourceDefinedException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.spi.federation.Connector.ExtraProperties;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.UrlBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creator of content nodes (binary content)
 * 
 * @author Mike Conway - DICE
 * 
 */
public class ContentNodeCreator extends AbstractNodeTypeCreator {

	public static final Logger log = LoggerFactory
			.getLogger(ContentNodeCreator.class);

	private final FileFromIdConverter fileFromIdConverter;

	/**
	 * @param irodsAccessObjectFactory
	 * @param irodsAccount
	 * @param connector
	 */
	public ContentNodeCreator(
			IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount, IrodsWriteableConnector connector) {
		super(irodsAccessObjectFactory, irodsAccount, connector);
		this.fileFromIdConverter = new FileFromIdConverterImpl(
				irodsAccessObjectFactory, irodsAccount,
				connector.getPathUtilities());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #instanceForId(java.lang.String, int)
	 */
	@Override
	public Document instanceForId(String id, int offset)
			throws RepositoryException {
		log.info("instanceForId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}
		log.info("id:{}", id);
		log.info("offset:{}", offset);

		// trim the /jcr:content suffix
		String trimmedId = id.substring(0, id.length()
				- PathUtilities.JCR_CONTENT_SUFFIX_LENGTH);
		IRODSFile file;
		try {
			file = fileFromIdConverter.fileFor(trimmedId);
		} catch (JargonException e) {
			log.error("error getting file from id", e);
			throw new RepositoryException("error getting file", e);
		}
		if (this.getPathUtilities().isExcluded(file)) {
			log.info("file is excluded by filter or does not exist..return null");
			return null;
		}

		DocumentWriter writer = newDocument(id);
		BinaryValue binaryValue = createBinaryValue(file, id);
		writer.setPrimaryType(PathUtilities.NT_RESOURCE);
		writer.addProperty(PathUtilities.JCR_DATA, binaryValue);
		if (this.getConnector().isAddMimeTypeMixin()) {
			String mimeType = null;
			String encoding = null; // We don't really know this
			try {
				mimeType = binaryValue.getMimeType();
			} catch (IOException e) {
				log.error("io exception getting mime type", e);
				throw new RepositoryException("io exception getting mime type",
						e);
			}

			writer.addProperty(PathUtilities.JCR_ENCODING, encoding);
			writer.addProperty(PathUtilities.JCR_MIME_TYPE, mimeType);
		}
		writer.addProperty(PathUtilities.JCR_LAST_MODIFIED, factories()
				.getDateFactory().create(file.lastModified()));
		writer.addProperty(PathUtilities.JCR_LAST_MODIFIED_BY, null); // ignored

		// make these binary not queryable. If we really want to query
		// them,
		// we need to switch to external binaries
		writer.setNotQueryable();

		if (!PathUtilities.isRoot(trimmedId)) {
			// Set the reference to the parent ...
			String parentId = this.getPathUtilities().idFor(file);
			writer.setParents(parentId);
		}

		// Add the 'mix:mixinType' mixin; if other mixins are stored in the
		// extra properties, this will append ...
		if (this.getConnector().isAddMimeTypeMixin()) {
			writer.addMixinType(MIX_MIME_TYPE);
		}

		// Return the document ...
		return writer.document();
	}

	/**
	 * Utility method to create a {@link BinaryValue} object for the given file.
	 * Subclasses should rarely override this method, since the
	 * {@link UrlBinaryValue} will be applicable in most situations.
	 * 
	 * @param file
	 *            the file for which the {@link BinaryValue} is to be created;
	 *            never null
	 * @param id
	 *            the <code>String</code> id for this content
	 * @return the binary value; never null
	 * @throws IOException
	 *             if there is an error creating the value
	 */
	protected ExternalBinaryValue createBinaryValue(final IRODSFile file,
			final String id) {

		log.info("createBinaryFile()");
		assert file != null;
		log.info("file:{}", file);

		return new IrodsBinaryValue(this.getPathUtilities().sha1(file), this
				.getConnector().getSourceName(), file.getAbsolutePath(),
				file.length(), file.getName(), this.getConnector()
						.getMimeTypeDetector(),
				this.getIrodsAccessObjectFactory(), this.getIrodsAccount());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #store(org.infinispan.schematic.document.Document)
	 */
	@Override
	public void store(Document document) {
		log.info("store()");

		if (document == null) {
			throw new IllegalArgumentException("null document");
		}

		DocumentReader reader = this.getConnector()
				.produceDocumentReaderFromDocument(document);
		String id = reader.getDocumentId();
		log.info("file to store:{}", id);
		FileFromIdConverter converter = new FileFromIdConverterImpl(
				this.getIrodsAccessObjectFactory(), this.getIrodsAccount(),
				this.getPathUtilities());
		IRODSFile file;
		try {
			file = converter.fileFor(id);
		} catch (JargonException e) {
			log.error("jargonException getting file for storing folder", e);
			throw new DocumentStoreException(id, "unable to get file for store");
		}
		if (this.isExcluded((File) file)) {
			throw new DocumentStoreException(id, "file is excluded");
		}
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		if (!parent.canWrite()) {
			throw new DocumentStoreException(id,
					"unable to write to parent file");
		}

		Map<Name, Property> properties = reader.getProperties();
		ExtraProperties extraProperties = this.getConnector()
				.retrieveExtraPropertiesForId(id, false);
		extraProperties.addAll(properties).except(
				PathUtilities.JCR_PRIMARY_TYPE, PathUtilities.JCR_CREATED,
				PathUtilities.JCR_LAST_MODIFIED, PathUtilities.JCR_DATA);
		extraProperties.save();

		Property content = properties.get(JcrLexicon.DATA);
		BinaryValue binary = factories().getBinaryFactory().create(
				content.getFirstValue());
		IRODSFile irodsFile = file;

		OutputStream ostream;
		try {
			ostream = new BufferedOutputStream(this.getConnector()
					.getIrodsFileSystem().getIRODSAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount())
					.instanceSessionClosingIRODSFileOutputStream(irodsFile));
			IoUtil.write(binary.getStream(), ostream);

		} catch (NoResourceDefinedException e) {
			log.error("exception in store", e);
			throw new DocumentStoreException(id, e);
		} catch (JargonException e) {
			log.error("exception in store", e);
			throw new DocumentStoreException(id, e);
		} catch (IOException e) {
			log.error("exception in store", e);
			throw new DocumentStoreException(id, e);
		} catch (RepositoryException e) {
			log.error("exception in store", e);
			throw new DocumentStoreException(id, e);
		}

	}

}
