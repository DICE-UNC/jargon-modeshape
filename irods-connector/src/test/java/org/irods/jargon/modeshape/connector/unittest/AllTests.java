package org.irods.jargon.modeshape.connector.unittest;

import org.irods.jargon.modeshape.connector.IrodsConnectorTest;
import org.irods.jargon.modeshape.connector.PathUtilitiesTest;
import org.irods.jargon.modeshape.connector.metadata.AvuMetadataConverterImplTest;
import org.irods.jargon.modeshape.connector.nodetypes.NodeTypeFactoryImplTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ NodeTypeFactoryImplTest.class, IrodsConnectorTest.class,
		PathUtilitiesTest.class, AvuMetadataConverterImplTest.class })
public class AllTests {

}
