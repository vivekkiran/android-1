/**
 * Nextcloud Android client application
 * <p>
 * Copyright (C) 2018 Edvard Holst
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.trashbin;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.ui.activity.BaseActivity;

import java.util.List;

public interface TrashbinContract {

    interface View {
        void showActivities(List<Object> activities, OwnCloudClient client, String nextPageUrl);

        void showActivitiesLoadError(String error);

        void showActivityDetailUI(OCFile ocFile);

        void showActivityDetailUIIsNull();

        void showActivityDetailError(String error);

        void showLoadingMessage();

        void showEmptyContent(String headline, String message);

        void setProgressIndicatorState(boolean isActive);
    }

    interface ActionListener {
        void loadFolder(String remotePath);

        void openActivity(String fileUrl, BaseActivity baseActivity, boolean isSharingSupported);
    }
}
