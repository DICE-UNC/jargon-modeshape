package org.irods.jargon.modeshape.connector.unittest;

import org.irods.jargon.modeshape.connector.IrodsConnectorTest;
import org.irods.jargon.modeshape.connector.nodetypes.NodeTypeFactoryImplTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ NodeTypeFactoryImplTest.class, IrodsConnectorTest.class })
public class AllTests {

}
