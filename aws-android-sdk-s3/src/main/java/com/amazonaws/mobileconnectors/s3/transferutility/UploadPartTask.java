/**
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.mobileconnectors.s3.transferutility;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

import java.util.concurrent.Callable;

class UploadPartTask implements Callable<Boolean> {

    private final static String TAG = "UploadPartTask";

    private final UploadPartRequest request;
    private final TransferProgress transferProgress;
    private final AmazonS3 s3;
    private final TransferDBUtil dbUtil;

    public UploadPartTask(UploadPartRequest request, TransferProgress transferProgress,
            AmazonS3 s3, TransferDBUtil dbUtil) {
        this.request = request;
        this.transferProgress = transferProgress;
        this.s3 = s3;
        this.dbUtil = dbUtil;
    }

    /*
     * Runs part upload task and returns whether successfully uploaded.
     */
    @Override
    public Boolean call() throws Exception {
        request.setGeneralProgressListener(new TransferProgressUpdatingListener(
                transferProgress) {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {
                super.progressChanged(progressEvent);
                dbUtil.updateBytesTransferred(request.getMainUploadId(),
                        transferProgress.getBytesTransferred(),
                        false);
            }
        });

        UploadPartResult putPartResult = null;
        try {
            putPartResult = s3.uploadPart(request);
        } catch (Exception e) {
            Log.e(TAG, "Encountered error uploading part", e);
            dbUtil.updateState(request.getId(), TransferState.FAILED);
            return false;
        }
        dbUtil.updateState(request.getId(), TransferState.PART_COMPLETED);
        String etag = putPartResult.getETag();
        dbUtil.updateETag(request.getId(), etag);
        return true;
    }
}