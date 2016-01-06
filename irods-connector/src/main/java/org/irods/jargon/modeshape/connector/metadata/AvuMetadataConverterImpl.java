/**
 * 
 */
package org.irods.jargon.modeshape.connector.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.core.service.AbstractJargonService;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.irods.jargon.modeshape.connector.exceptions.IrodsConnectorRuntimeException;
import org.irods.jargon.modeshape.connector.nodetypes.FileFromIdConverter;
import org.irods.jargon.modeshape.connector.nodetypes.FileFromIdConverterImpl;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.basic.BasicMultiValueProperty;
import org.modeshape.jcr.value.basic.BasicName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to convert iRODS AVUs (metadata tuples) to modeshape properties.
 * <p/>
 * Note that our convention is to use a SHA1 hash of the attribute and value portions of the tuple as the 'name' of the property
 * in all methods.
 * 
 * @author Mike Conway - DICE
 *
 */
public class AvuMetadataConverterImpl extends AbstractJargonService implements AvuMetadataConverter {
	
	public static final Logger log = LoggerFactory
			.getLogger(AvuMetadataConverterImpl.class);
	
	private final IrodsWriteableConnector irodsWriteableConnector;

	/**
	 * @param irodsAccessObjectFactory
	 * @param irodsAccount
	 */
	public AvuMetadataConverterImpl(IrodsWriteableConnector irodsWriteableConnector,
			IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount) {
		
		super(irodsAccessObjectFactory, irodsAccount);
		if (irodsWriteableConnector == null) {
			throw new IllegalArgumentException("null irodsWriteableConnector");
		}
		this.irodsWriteableConnector = irodsWriteableConnector;
		
	}
	
	/* (non-Javadoc)
	 * @see org.irods.jargon.modeshape.connector.metadata.AvuMetadataConverter#containsProperty(java.lang.String)
	 */
	@Override
	public boolean containsProperty(final String propertyId) {
		return false;
	}
	
	public Map<String, Property> retrieveAllMetadataAsProperties(final String id) {
		log.info("retrieveAllMetadataAsProperties()");
		if (id  == null || id.isEmpty()) {
			throw new IllegalArgumentException("null id");
		}
		log.info("id:{}", id);
		
		IRODSFile irodsFile = obtainIrodsFileForId(id);
		
		log.info("got file, checking type to retrieve avus");
		
		List<MetaDataAndDomainData> metadata = new ArrayList<MetaDataAndDomainData>();
		try {
			if (irodsFile.isFile()) {
				log.info("is a file, get data object avus");
				DataObjectAO dataObjectAO = this.getIrodsAccessObjectFactory().getDataObjectAO(getIrodsAccount());
				metadata = dataObjectAO.findMetadataValuesForDataObject(irodsFile);
			} else {
				log.info("is a collection, get collection avus");
				CollectionAO collectionAO = this.getIrodsAccessObjectFactory().getCollectionAO(getIrodsAccount());
				metadata = collectionAO.findMetadataValuesForCollection(irodsFile.getAbsolutePath());
			}
		} catch (JargonException | JargonQueryException e) {
			log.error("exception getting avus", e);
			throw new IrodsConnectorRuntimeException("error getting avus", e);
		}
		
		
		return convertAvusToProperties(metadata);
		
		
	}

	/**
	 * convert the irods metadata into JCR properties
	 * @param metadata {@link MetaDataAndDomainData} that represents the avu values
	 * @return <code>Map<String, Property></code> with the jcer {@link Property} values that map to the irods AVUs
	 */
	protected Map<String, Property> convertAvusToProperties(
			List<MetaDataAndDomainData> metadata) {
		
		if (metadata == null) {
			throw new IllegalArgumentException("null metadata");
		}
		Map<String,Property> properties = new HashMap<String, Property>();
		List<Object> propValuesAsArray;
		for (MetaDataAndDomainData metadataValue : metadata) {
			propValuesAsArray = new ArrayList<Object>();
			propValuesAsArray.add(metadataValue.getAvuValue());
			propValuesAsArray.add(metadataValue.getAvuUnit());
			String key = convertAvuValueToSha1(metadataValue);
			properties.put(key, new BasicMultiValueProperty(nameForAvu(metadataValue), propValuesAsArray));

		}
		
		return properties;
		
	}
	
	private Name nameForAvu(final MetaDataAndDomainData metadataAndDomainData) {
		return new BasicName(AvuMetadataConverter.IRODS_AVU_NAMESPACE, metadataAndDomainData.getAvuAttribute());
	}

	public static String convertAvuValueToSha1(MetaDataAndDomainData metadataValue) {
		StringBuilder sb = new StringBuilder();
		sb.append(metadataValue.getAvuAttribute());
		sb.append(metadataValue.getAvuValue());
		sb.append(metadataValue.getAvuUnit());
		return DigestUtils.shaHex(sb.toString().getBytes());
	}

	private IRODSFile obtainIrodsFileForId(final String id) {
		FileFromIdConverter fileFromIdConverter = new FileFromIdConverterImpl(
				irodsAccessObjectFactory, irodsAccount,
				irodsWriteableConnector.getPathUtilities());
		try {
			IRODSFile irodsFile = fileFromIdConverter.fileFor(id);
			log.info("got file:{}", irodsFile);
			return irodsFile;
		} catch (JargonException e) {
			log.error("exception getting file for id:{}", id, e);
			throw new IrodsConnectorRuntimeException("error getting file", e);
		}
	}

}
