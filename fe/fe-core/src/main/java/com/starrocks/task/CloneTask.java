// Copyright 2021-present StarRocks, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// This file is based on code available under the Apache license here:
//   https://github.com/apache/incubator-doris/blob/master/fe/fe-core/src/main/java/org/apache/doris/task/CloneTask.java

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

package com.starrocks.task;

import com.starrocks.thrift.TBackend;
import com.starrocks.thrift.TCloneReq;
import com.starrocks.thrift.TStorageMedium;
import com.starrocks.thrift.TTaskType;

import java.util.List;

public class CloneTask extends AgentTask {
    // these versions are for distinguishing the old clone task(VERSION_1) and the new clone task(VERSION_2)
    public static final int VERSION_1 = 1;
    public static final int VERSION_2 = 2;

    private final int schemaHash;
    private final List<TBackend> srcBackends;
    private final TStorageMedium storageMedium;

    private final long visibleVersion;

    private long srcPathHash = -1;
    private long destPathHash = -1;
    private String destBackendHost;

    private final int timeoutS;

    private int taskVersion = VERSION_1;

    // Migration between different disks on the same backend
    private boolean isLocal = false;

    // Rebuild persistent index at the end of clone task
    private boolean needRebuildPkIndex = false;

    public CloneTask(long backendId, String destBackendHost, long dbId, long tableId, long partitionId, long indexId,
                     long tabletId, int schemaHash, List<TBackend> srcBackends, TStorageMedium storageMedium,
                     long visibleVersion, int timeoutS) {
        super(null, backendId, TTaskType.CLONE, dbId, tableId, partitionId, indexId, tabletId);
        this.schemaHash = schemaHash;
        this.srcBackends = srcBackends;
        this.destBackendHost = destBackendHost;
        this.storageMedium = storageMedium;
        this.visibleVersion = visibleVersion;
        this.timeoutS = timeoutS;
    }

    public int getSchemaHash() {
        return schemaHash;
    }

    public TStorageMedium getStorageMedium() {
        return storageMedium;
    }

    public long getVisibleVersion() {
        return visibleVersion;
    }

    public void setPathHash(long srcPathHash, long destPathHash) {
        this.srcPathHash = srcPathHash;
        this.destPathHash = destPathHash;
        this.taskVersion = VERSION_2;
    }

    public int getTaskVersion() {
        return taskVersion;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setIsLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public void setNeedRebuildPkIndex(boolean needRebuildPkIndex) {
        this.needRebuildPkIndex = needRebuildPkIndex;
    }

    public TCloneReq toThrift() {
        TCloneReq request = new TCloneReq(tabletId, schemaHash, srcBackends);
        request.setStorage_medium(storageMedium);
        request.setCommitted_version(visibleVersion);
        request.setTask_version(taskVersion);
        if (taskVersion == VERSION_2) {
            request.setSrc_path_hash(srcPathHash);
            request.setDest_path_hash(destPathHash);
        }
        request.setTimeout_s(timeoutS);
        request.setIs_local(isLocal);
        request.setNeed_rebuild_pk_index(needRebuildPkIndex);
        return request;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("tablet id: ").append(tabletId).append(", schema hash: ").append(schemaHash);
        sb.append(", storageMedium: ").append(storageMedium.name());
        sb.append(", visible version(hash): ").append(visibleVersion).append("-").append(0);
        sb.append(", src backend: ").append(srcBackends.get(0).getHost()).append(", src path hash: ")
                .append(srcPathHash);
        sb.append(", dest backend: ").append(destBackendHost).append(", dest path hash: ").append(destPathHash);
        sb.append(", is local: ").append(isLocal).append(", need rebuild pk index: ").append(needRebuildPkIndex);
        return sb.toString();
    }
}
