/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.File;
import java.util.List;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.FileNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a file node (iRODS file) based in its id
 * 
 * @author Mike Conway - DICE
 * 
 */
public class FileNodeCreator extends AbstractNodeTypeCreator {

	public static final Logger log = LoggerFactory
			.getLogger(FileNodeCreator.class);

	public static final int JCR_CONTENT_SUFFIX_LENGTH = PathUtilities.JCR_CONTENT_SUFFIX
			.length();

	public FileNodeCreator(IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount,
			IrodsWriteableConnector irodsWriteableConnector) {
		super(irodsAccessObjectFactory, irodsAccount, irodsWriteableConnector);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #instanceForId(java.lang.String, int)
	 */
	@Override
	public Document instanceForId(final String id, final int offset)
			throws JargonException {
		log.info("instanceForId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}
		log.info("id:{}", id);
		log.info("offset:{}", offset);

		IRODSFile file = fileFor(id);
		if (this.getPathUtilities().isExcluded(file)) {
			log.info("file is excluded by filter or does not exist..return null");
			return null;
		}
		log.info("see if this is a data object or collection and act appropriately");

		if (file.isFile()) {
			return instanceForIdAsFile(id, file);
		} else {
			return instanceForIdAsCollection(id, file, offset);
		}

	}

	private Document instanceForIdAsCollection(final String id,
			final IRODSFile file, final int offset) {
		log.info("instanceForIdAsCollection()");
		DocumentWriter writer = newDocument(id);
		writer.setPrimaryType(PathUtilities.NT_FOLDER);
		writer.addMixinType(PathUtilities.JCR_IRODS_IRODSOBJECT);
		writer.addProperty(PathUtilities.JCR_CREATED, factories()
				.getDateFactory().create(file.lastModified()));
		writer.addProperty(PathUtilities.JCR_CREATED_BY, null); // ignored

		log.info("adding AVU children for the collection");
		addAvuChildrenForCollection(file.getAbsolutePath(), id, writer);
		log.info("AVU children added");

		File[] children = file.listFiles(this.getPathUtilities()
				.getInclusionExclusionFilenameFilter());
		long totalChildren = 0;
		int nextOffset = 0;
		log.info("parent is:{}", file.getAbsolutePath());
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
				if (i >= offset
						&& i < offset + IrodsWriteableConnector.PAGE_SIZE) {
					// We use identifiers that contain the file/directory name
					// ...

					String childName = child.getName();
					String childId = PathUtilities.isRoot(id) ? PathUtilities.DELIMITER
							+ childName
							: id + PathUtilities.DELIMITER + childName;

					writer.addChild(childId, childName);

					log.info("added child directory with name:{}", childName);

				}
				nextOffset = i + 1;
			}
		}

		// if there are still accessible children add the next page
		if (nextOffset < totalChildren) {
			writer.addPage(id, nextOffset, IrodsWriteableConnector.PAGE_SIZE,
					totalChildren);
		}

		return writer.document();
	}

	private Document instanceForIdAsFile(String id, IRODSFile file) {
		log.info("instanceFrIdAsFile()");
		DocumentWriter writer = this.newDocument(id);
		writer.setPrimaryType(PathUtilities.NT_FILE);
		writer.addMixinType(PathUtilities.JCR_IRODS_IRODSOBJECT);

		writer.addProperty(PathUtilities.JCR_CREATED, factories()
				.getDateFactory().create(file.lastModified()));
		writer.addProperty(PathUtilities.JCR_CREATED_BY, null); // ignored

		String childId = PathUtilities.formatChildIdForDocument(id);
		writer.addChild(childId, PathUtilities.JCR_CONTENT);
		if (!PathUtilities.isRoot(id)) {
			log.info("not root, set reference to parent");
			// Set the reference to the parent ...
			String parentId = this.getPathUtilities().idFor(
					file.getParentFile());
			writer.setParents(parentId);
		}

		// Add the 'mix:mixinType' mixin; if other mixins are stored in the
		// extra properties, this will append ...
		if (this.isAddMimeTypeMixin()) {
			writer.addMixinType(MIX_MIME_TYPE);
		}

		log.info("see if avus added...");
		addAvuChildrenForDataObject(file.getAbsolutePath(), id, writer);
		log.info("done!");
		// Return the document ...
		return writer.document();
	}

	/**
	 * Given an id in ModeShape terms, return the corresponding iRODS file
	 * 
	 * @param id
	 *            <code>String</code> with the ModeShape id
	 * @return {@link File} that is the iRODS file
	 * @throws JargonException
	 */
	private IRODSFile fileFor(String id) throws JargonException {
		log.info("fileFor()");
		log.info("id:{}", id);

		String strippedId = PathUtilities.stripTrailingDelimFromIdIfPresent(id);
		String parentPath = this.getPathUtilities()
				.getDirectoryPathWithTrailingSlash();

		log.info("getting file for parent path:{}", parentPath);
		log.info("child path:{}", strippedId);

		return this.getIrodsAccessObjectFactory()
				.getIRODSFileFactory(getIrodsAccount())
				.instanceIRODSFile(parentPath, strippedId);

	}

	/**
	 * For a given data object, create a set of child documents of type
	 * irods:avu that represents the AVU metadata
	 * 
	 * @param path
	 *            <code>String</code> with the path to the iRODS collection
	 * @param writer
	 *            {@link DocumentWriter} for the parent collection
	 */
	private void addAvuChildrenForDataObject(final String path,
			final String id, final DocumentWriter writer) {

		log.info("addAvuChildrenForDataObject()");
		if (!this.isIncludeAvus()) {
			log.info("avus not included");
			return;
		}

		log.info("avus included, do query");
		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps;

			fileForProps = (File) getIrodsAccessObjectFactory()
					.getIRODSFileFactory(this.getIrodsAccount())
					.instanceIRODSFile(path);

			log.info("file abs path to search for collection AVUs:{}",
					fileForProps.getAbsolutePath());

			DataObjectAO dataObjectAO = getIrodsAccessObjectFactory()
					.getDataObjectAO(getIrodsAccount());

			metadatas = dataObjectAO
					.findMetadataValuesForDataObject(fileForProps
							.getAbsolutePath());

			addChildrenForEachAvu(writer, metadatas, fileForProps, id);

		} catch (FileNotFoundException e) {
			log.error("fnf retrieving avus", e);
			throw new DocumentStoreException(
					"file not found for retrieving avus", e);
		} catch (JargonException e) {
			log.error("jargon exception retrieving avus", e);
			throw new DocumentStoreException(
					"jargon exception retrieving avus", e);
		}
	}

	/**
	 * For a given collection, create a set of child documents of type irods:avu
	 * that represents the AVU metadata
	 * 
	 * @param path
	 *            <code>String</code> with the path to the iRODS collection
	 * @param writer
	 *            {@link DocumentWriter} for the parent collection
	 */
	private void addAvuChildrenForCollection(final String path,
			final String id, final DocumentWriter writer) {

		assert path != null && !path.isEmpty();
		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps;

			fileForProps = (File) IRODSFileSystemSingletonWrapper.instance()
					.getIRODSAccessObjectFactory()
					.getIRODSFileFactory(this.getIrodsAccount())
					.instanceIRODSFile(path);

			log.info("file abs path to search for collection AVUs:{}",
					fileForProps.getAbsolutePath());

			CollectionAO collectionAO = IRODSFileSystemSingletonWrapper
					.instance().getIRODSAccessObjectFactory()
					.getCollectionAO(getIrodsAccount());

			metadatas = collectionAO.findMetadataValuesForCollection(
					fileForProps.getAbsolutePath(), 0);

			addChildrenForEachAvu(writer, metadatas, fileForProps, id);

		} catch (FileNotFoundException e) {
			log.error("fnf retrieving avus", e);
			throw new DocumentStoreException(
					"file not found for retrieving avus", e);
		} catch (JargonException e) {
			log.error("jargon exception retrieving avus", e);
			throw new DocumentStoreException(
					"jargon exception retrieving avus", e);
		} catch (JargonQueryException e) {
			log.error("jargon query exception retrieving avus", e);
			throw new DocumentStoreException(
					"jargon query exception retrieving avus", e);
		}
	}

	/**
	 * For each AVU in the list, add as a child of the given document
	 * 
	 * @param writer
	 * @param metadatas
	 * @param fileForProps
	 */
	private void addChildrenForEachAvu(final DocumentWriter writer,
			List<MetaDataAndDomainData> metadatas, File fileForProps,
			final String id) {
		StringBuilder sb;
		for (MetaDataAndDomainData metadata : metadatas) {
			sb = new StringBuilder();
			sb.append(id);
			sb.append(AVU_ID);
			sb.append(metadata.getAvuId());
			String childName = sb.toString();
			sb.append(PathUtilities.JCR_AVU_SUFFIX);
			String childId = sb.toString();
			log.info("adding avu child with childName:{}", childName);
			log.info("avu childId:{}", childId);
			writer.addChild(childId, childName);
		}
	}
}
