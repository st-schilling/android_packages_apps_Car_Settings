/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.settings.users;

import static android.content.pm.UserInfo.FLAG_INITIALIZED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ProfileDetailsDeletePreferenceControllerTest {
    private static final String TEST_USERNAME = "Test Username";

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;
    private CarUxRestrictions mCarUxRestrictions;
    private ProfileDetailsDeletePreferenceController mPreferenceController;
    private RemoveUserHandler mRemoveUserHandler;

    @Mock
    private FragmentController mMockFragmentController;
    @Mock
    private UserHelper mMockUserHelper;
    @Mock
    private UserManager mMockUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mPreference = new Preference(mContext);
        mRemoveUserHandler = new RemoveUserHandler(
                mContext, mMockUserHelper, mMockUserManager, mMockFragmentController);
        mPreferenceController = new ProfileDetailsDeletePreferenceController(mContext,
                /* preferenceKey= */ "key", mMockFragmentController, mCarUxRestrictions,
                mMockUserHelper, mRemoveUserHandler);
    }

    @Test
    public void onCreate_userNotRestricted_deleteButtonShown() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onCreate_userRestricted_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        when(mMockUserManager.hasUserRestriction(UserManager.DISALLOW_REMOVE_USER))
                .thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onCreate_userIsViewingSelf_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 0, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mRemoveUserHandler.canRemoveUser(userInfo)).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onCreate_userIsNotViewingSelf_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 0, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mRemoveUserHandler.canRemoveUser(userInfo)).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onCreate_isDemoUser_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        when(mMockUserManager.isDemoUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }
}
