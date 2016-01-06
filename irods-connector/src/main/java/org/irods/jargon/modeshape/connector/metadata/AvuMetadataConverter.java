package org.irods.jargon.modeshape.connector.metadata;

/**
 * Service that can bridge between iRODS AVUs and modeshape properties
 * @author Mike Conway - DICE
 *
 */
public interface AvuMetadataConverter {
	
	public static final String IRODS_AVU_NAMESPACE = "http://www.irods.org/metadata";

	/**
	 * Does the properties store contain the given property
	 * @param propertyId
	 * @return
	 */
	public abstract boolean containsProperty(String propertyId);

}