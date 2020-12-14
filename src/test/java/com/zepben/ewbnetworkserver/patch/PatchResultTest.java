/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 * This file is part of ewb-network-server.
 *
 * ewb-network-server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ewb-network-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with ewb-network-server.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zepben.ewbnetworkserver.patch;

import com.zepben.evolve.cim.iec61970.base.core.ConductingEquipment;
import com.zepben.ewbnetworkserver.TestObjectCreators;
import com.zepben.testutils.junit.SystemLogExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;
import java.util.stream.Collectors;

import static com.zepben.collectionutils.CollectionUtils.setOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PatchResultTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    @Test
    public void storesResults() {
        ConductingEquipment asset1 = createAsset("asset1", setOf("f1", "f2"), setOf("f4", "f5"));
        ConductingEquipment asset2 = createAsset("asset2", setOf("f2", "f3"), setOf("f5", "f6"));

        PatchResult patchResult = new PatchResult(123)
            .addAffectedFeedersFromAsset(asset1)
            .addAffectedFeedersFromAsset(asset2)
            .addError("error1")
            .addError("error%d", 2)
            .addWarning("warning%d", 1)
            .addWarning("warning2");

        assertThat(patchResult.patchId(), equalTo(123));
        assertThat(patchResult.affectedNormalFeeders(), containsInAnyOrder("f1", "f2", "f3"));
        assertThat(patchResult.affectedCurrentFeeders(), containsInAnyOrder("f4", "f5", "f6"));
        assertThat(patchResult.errors(), contains("error1", "error2"));
        assertThat(patchResult.warnings(), contains("warning1", "warning2"));
    }

    private ConductingEquipment createAsset(String id, Set<String> normalFeeders, Set<String> currentFeeders) {
        ConductingEquipment asset = mock(ConductingEquipment.class);

        doReturn(id).when(asset).getMRID();
        doReturn(id).when(asset).getName();
        doReturn(normalFeeders.stream().map(TestObjectCreators::createFeederMock).collect(Collectors.toList())).when(asset).getNormalFeeders();
        doReturn(currentFeeders.stream().map(TestObjectCreators::createFeederMock).collect(Collectors.toList())).when(asset).getCurrentFeeders();

        return asset;
    }

}
