// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.StorageManager;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.command.admin.offering.UpdateDiskOfferingCmd;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.resourcedetail.DiskOfferingDetailVO;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.vm.UnmanagedVMsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerImplTest {
    @Mock
    ConfigDepot configDepot;
    @InjectMocks
    ConfigurationManagerImpl configurationManagerImplSpy = Mockito.spy(new ConfigurationManagerImpl());
    @Mock
    SearchCriteria<DiskOfferingDetailVO> searchCriteriaDiskOfferingDetailMock;
    @Mock
    DiskOffering diskOfferingMock;
    @Mock
    Account accountMock;
    @Mock
    User userMock;
    @Mock
    Domain domainMock;
    @Mock
    ConfigurationDao configDaoMock;
    @Mock
    DataCenterDao zoneDaoMock;
    @Mock
    DomainDao domainDaoMock;
    @Mock
    EntityManager entityManagerMock;
    @Mock
    DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Spy
    DiskOfferingVO diskOfferingVOSpy;
    @Mock
    UpdateDiskOfferingCmd updateDiskOfferingCmdMock;

    Long validId = 1L;
    Long invalidId = 100L;
    List<Long> filteredZoneIds = List.of(1L, 2L, 3L);
    List<Long> existingZoneIds = List.of(1L, 2L, 3L);
    List<Long> filteredDomainIds = List.of(1L, 2L, 3L);
    List<Long> existingDomainIds = List.of(1L, 2L, 3L);
    List<Long> emptyExistingZoneIds = new ArrayList<>();
    List<Long> emptyExistingDomainIds = new ArrayList<>();
    List<Long> emptyFilteredDomainIds = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        configurationManagerImplSpy._configDepot = configDepot;
    }

    @Test
    public void validateIfIntValueIsInRangeTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "3", "1-5");
        Assert.assertNull(testVariable);
    }


    @Test
    public void validateIfIntValueIsInRangeTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "9", "1-5");
        Assert.assertNotNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestValidValuesReturnNull() {
        String testVariable;
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateIfStringValueIsInRangeTestInvalidValuesReturnString() {
        String testVariable;
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertEquals("The provided value is not returnMsg.", testVariable);
        }
    }


    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesValidValueReturnNull() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesInvalidValueReturnMessages() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg2").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg3").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertEquals("The provided value is neither returnMsg1 NOR returnMsg2 NOR returnMsg3.", testVariable);
    }


    @Test
    public void validateRangePrivateIpTestValidValueReturnNull() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(true);
            String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateRangePrivateIpTestInvalidValueReturnString() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(false);
            String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
            Assert.assertEquals("a valid site local IP address", testVariable);
        }
    }

    @Test
    public void validateRangeHypervisorListTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeHypervisorListTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware,Any,InvalidHypervisorName");
        Assert.assertEquals("a valid hypervisor type", testVariable);
    }

    @Test
    public void validateRangeInstanceNameTestValidValueReturnNull() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(true);
            String testVariable = configurationManagerImplSpy.validateRangeInstanceName("ThisStringShouldBeValid");
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateRangeInstanceNameTestInvalidValueReturnString() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(false);
            String testVariable = configurationManagerImplSpy.validateRangeInstanceName("This string should not be valid.");
            Assert.assertEquals("a valid instance name (instance names cannot contain hyphens, spaces or plus signs)", testVariable);
        }
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringShould.Work");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotStartWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeast" +
                "OnePeriodEverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXhNOBNTNAoMCQqJMzcvFSBwHUhmWHftjfTNUaHR");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsNotAValidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueReturnNull() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithAStarAndAPeriod.ThisShouldWorkEvenThoughItIsOverTwoHundredAnd" +
                "ThirtyEight.CharactersLong.BecauseTheFirstTwoCharactersAreIgnored.TheRestOfThisStringWasRandomlyGenerated.MgTUerXPlLyMaUpKTjAhxasFYRCfNCXmtWDwqSDOcTjASWlAXS");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeastOnePeriod" +
                "EverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXNOBNTNAoMChQqJMzcvFSBwHUhmWHftjfTNUaHRKVyXm");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsNotAValidValueReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeOtherTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "SoShouldThis", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeOtherTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "ThisShouldNotWork", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNotNull(testVariable);
    }

    @Test
    public void testValidateIpAddressRelatedConfigValuesUnrelated() {
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues(StorageManager.PreferredStoragePool.key(), "something");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "");
        Mockito.when(configurationManagerImplSpy._configDepot.get("config.ip")).thenReturn(null);
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "something");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues(StorageManager.MountDisabledStoragePool.key(), "false");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateIpAddressRelatedConfigValuesInvalidIp() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.ip");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "abcdefg");
    }

    @Test
    public void testValidateIpAddressRelatedConfigValuesValidIp() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.ip");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "192.168.1.1");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateIpAddressRelatedConfigValuesInvalidIpRange() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type. RemoteAccessVpnManagerImpl.RemoteAccessVpnClientIpRange not accessible here
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.iprange");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.iprange", "xyz-192.168.1.20");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateIpAddressRelatedConfigValuesInvalidIpRange1() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type. RemoteAccessVpnManagerImpl.RemoteAccessVpnClientIpRange not accessible here
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.iprange");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.iprange", "192.168.1.20");
    }

    @Test
    public void testValidateIpAddressRelatedConfigValuesValidIpRange() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type. RemoteAccessVpnManagerImpl.RemoteAccessVpnClientIpRange not accessible here
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.iprange");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.iprange", "192.168.1.1-192.168.1.100");
    }

    @Test
    public void testValidateInvalidConfiguration() {
        Mockito.doReturn(null).when(configDaoMock).findByName(Mockito.anyString());
        String msg = configurationManagerImplSpy.validateConfigurationValue("test.config.name", "testvalue", ConfigKey.Scope.Global.toString());
        Assert.assertEquals("Invalid configuration variable.", msg);
    }

    @Test
    public void testValidateInvalidScopeForConfiguration() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScope()).thenReturn(ConfigKey.Scope.Account.toString());
        Mockito.doReturn(cfg).when(configDaoMock).findByName(Mockito.anyString());
        String msg = configurationManagerImplSpy.validateConfigurationValue("test.config.name", "testvalue", ConfigKey.Scope.Domain.toString());
        Assert.assertEquals("Invalid scope id provided for the parameter test.config.name", msg);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateConfig_ThreadsOnKVMHostToTransferVMwareVMFiles_Failure() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScope()).thenReturn(ConfigKey.Scope.Global.toString());
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ThreadsOnKVMHostToImportVMwareVMFiles;
        Mockito.doReturn(cfg).when(configDaoMock).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());
        configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "11", configKey.scope().toString());
    }

    @Test
    public void testValidateConfig_ThreadsOnKVMHostToTransferVMwareVMFiles_Success() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScope()).thenReturn(ConfigKey.Scope.Global.toString());
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ThreadsOnKVMHostToImportVMwareVMFiles;
        Mockito.doReturn(cfg).when(configDaoMock).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());
        String msg = configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "10", configKey.scope().toString());
        Assert.assertNull(msg);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateConfig_ConvertVmwareInstanceToKvmTimeout_Failure() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScope()).thenReturn(ConfigKey.Scope.Global.toString());
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ConvertVmwareInstanceToKvmTimeout;
        Mockito.doReturn(cfg).when(configDaoMock).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());
        configurationManagerImplSpy.populateConfigValuesForValidationSet();
        configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "0", configKey.scope().toString());
    }

    @Test
    public void testValidateConfig_ConvertVmwareInstanceToKvmTimeout_Success() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScope()).thenReturn(ConfigKey.Scope.Global.toString());
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ConvertVmwareInstanceToKvmTimeout;
        Mockito.doReturn(cfg).when(configDaoMock).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());
        configurationManagerImplSpy.populateConfigValuesForValidationSet();
        String msg = configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "9", configKey.scope().toString());
        Assert.assertNull(msg);
    }

    @Test
    public void validateDomainTestInvalidIdThrowException() {
        Mockito.doReturn(null).when(domainDaoMock).findById(invalidId);
        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.validateDomain(List.of(invalidId)));
    }

    @Test
    public void validateZoneTestInvalidIdThrowException() {
        Mockito.doReturn(null).when(zoneDaoMock).findById(invalidId);
        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.validateZone(List.of(invalidId)));
    }

    @Test
    public void updateDiskOfferingIfCmdAttributeNotNullTestNotNullValueUpdateOfferingAttribute() {
        Mockito.doReturn("DiskOfferingName").when(updateDiskOfferingCmdMock).getDiskOfferingName();
        Mockito.doReturn("DisplayText").when(updateDiskOfferingCmdMock).getDisplayText();
        Mockito.doReturn(1).when(updateDiskOfferingCmdMock).getSortKey();
        Mockito.doReturn(false).when(updateDiskOfferingCmdMock).getDisplayOffering();

        configurationManagerImplSpy.updateDiskOfferingIfCmdAttributeNotNull(diskOfferingVOSpy, updateDiskOfferingCmdMock);

        Assert.assertEquals(updateDiskOfferingCmdMock.getDiskOfferingName(), diskOfferingVOSpy.getName());
        Assert.assertEquals(updateDiskOfferingCmdMock.getDisplayText(), diskOfferingVOSpy.getDisplayText());
        Assert.assertEquals(updateDiskOfferingCmdMock.getSortKey(), (Integer) diskOfferingVOSpy.getSortKey());
        Assert.assertEquals(updateDiskOfferingCmdMock.getDisplayOffering(), diskOfferingVOSpy.getDisplayOffering());
    }

    @Test
    public void updateDiskOfferingIfCmdAttributeNotNullTestNullValueDoesntUpdateOfferingAttribute() {
        Mockito.doReturn("Name").when(diskOfferingVOSpy).getName();
        Mockito.doReturn("DisplayText").when(diskOfferingVOSpy).getDisplayText();
        Mockito.doReturn(1).when(diskOfferingVOSpy).getSortKey();
        Mockito.doReturn(true).when(diskOfferingVOSpy).getDisplayOffering();

        configurationManagerImplSpy.updateDiskOfferingIfCmdAttributeNotNull(diskOfferingVOSpy, updateDiskOfferingCmdMock);

        Assert.assertNotEquals(updateDiskOfferingCmdMock.getDiskOfferingName(), diskOfferingVOSpy.getName());
        Assert.assertNotEquals(updateDiskOfferingCmdMock.getDisplayText(), diskOfferingVOSpy.getDisplayText());
        Assert.assertNotEquals(updateDiskOfferingCmdMock.getSortKey(), (Integer) diskOfferingVOSpy.getSortKey());
        Assert.assertNotEquals(updateDiskOfferingCmdMock.getDisplayOffering(), diskOfferingVOSpy.getDisplayOffering());
    }

    @Test
    public void updateDiskOfferingDetailsDomainIdsTestDifferentDomainIdsDiskOfferingDetailsAddDomainIds() {
        List<DiskOfferingDetailVO> detailsVO = new ArrayList<>();
        Long diskOfferingId = validId;

        configurationManagerImplSpy.updateDiskOfferingDetailsDomainIds(detailsVO, searchCriteriaDiskOfferingDetailMock, diskOfferingId, filteredDomainIds, existingDomainIds);

        for (int i = 0; i < detailsVO.size(); i++) {
            Assert.assertEquals(filteredDomainIds.get(i), (Long) Long.parseLong(detailsVO.get(i).getValue()));
        }
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestDifferentZoneIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, emptyExistingZoneIds, existingDomainIds, filteredDomainIds));
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestEmptyExistingDomainIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, existingZoneIds, emptyExistingDomainIds, filteredDomainIds));
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestEmptyFilteredDomainIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, existingZoneIds, existingDomainIds, emptyFilteredDomainIds));
    }

    @Test
    public void getAccountNonChildDomainsTestValidValuesReturnChildDomains() {
        Mockito.doReturn(null).when(updateDiskOfferingCmdMock).getSortKey();
        List<Long> nonChildDomains = configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdMock, existingDomainIds);

        for (int i = 0; i < existingDomainIds.size(); i++) {
            Assert.assertEquals(existingDomainIds.get(i), nonChildDomains.get(i));
        }
    }

    @Test
    public void getAccountNonChildDomainsTestAllDomainsAreChildDomainsReturnEmptyList() {
        for (Long existingDomainId : existingDomainIds) {
            Mockito.when(domainDaoMock.isChildDomain(accountMock.getDomainId(), existingDomainId)).thenReturn(true);
        }

        List<Long> nonChildDomains = configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdMock, existingDomainIds);

        Assert.assertTrue(nonChildDomains.isEmpty());
    }

    @Test
    public void getAccountNonChildDomainsTestNotNullCmdAttributeThrowException() {
        Mockito.doReturn("name").when(updateDiskOfferingCmdMock).getDiskOfferingName();

        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdMock, existingDomainIds));
    }

    @Test
    public void checkIfDomainIsChildDomainTestNonChildDomainThrowException() {
        Mockito.doReturn(false).when(domainDaoMock).isChildDomain(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(domainMock).when(entityManagerMock).findById(Domain.class, 1L);

        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.checkIfDomainIsChildDomain(diskOfferingMock, accountMock, userMock, filteredDomainIds));
    }
}
