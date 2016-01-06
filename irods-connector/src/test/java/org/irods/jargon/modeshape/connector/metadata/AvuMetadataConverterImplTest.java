package org.irods.jargon.modeshape.connector.metadata;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.core.query.MetaDataAndDomainData.MetadataDomain;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.jcr.value.Property;

public class AvuMetadataConverterImplTest {

	@Test
	public void testAvuToSha1() throws Exception {
		
		MetaDataAndDomainData metadataAndDomainData =  MetaDataAndDomainData.instance(MetadataDomain.COLLECTION, 
				"xyz", "yyyz", 0, new Date(), new Date(), 1, "attrib", "value", "");
		String actual = AvuMetadataConverterImpl.convertAvuValueToSha1(metadataAndDomainData);
		Assert.assertNotNull(actual);
		Assert.assertFalse(actual.isEmpty());
		
	}
	
	public void testConvertAvusToProperties() throws Exception {
		MetaDataAndDomainData metadataAndDomainData =  MetaDataAndDomainData.instance(MetadataDomain.COLLECTION, 
				"xyz", "yyyz", 0, new Date(), new Date(), 1, "attrib", "value", "");
		List<MetaDataAndDomainData> metadata = new ArrayList<MetaDataAndDomainData>();
		metadata.add(metadataAndDomainData);
		IRODSAccessObjectFactory accessObjectFactory = Mockito.mock(IRODSAccessObjectFactory.class);
		IRODSAccount irodsAccount = TestingPropertiesHelper.buildBogusIrodsAccount();
		IrodsWriteableConnector connector = Mockito.mock(IrodsWriteableConnector.class);
		AvuMetadataConverterImpl avuMetadataConverter = new AvuMetadataConverterImpl(connector, accessObjectFactory, irodsAccount);
		Map<String,Property> actual = avuMetadataConverter.convertAvusToProperties(metadata);
		Assert.assertFalse(actual.isEmpty());
	}

}
