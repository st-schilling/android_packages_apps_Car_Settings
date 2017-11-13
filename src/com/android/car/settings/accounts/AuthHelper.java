/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.car.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for monitoring accounts on the device for a given user.
 */
final public class AuthHelper {
    private static final String TAG = "AuthHelper";

    private final Map<String, AuthenticatorDescription> mTypeToAuthDescription = new HashMap<>();
    private final ArrayList<String> mEnabledAccountTypes = new ArrayList<>();
    private final Map<String, Drawable> mAccTypeIconCache = new HashMap<>();

    private final UserHandle mUserHandle;
    private final Context mContext;

    public AuthHelper(Context context, UserHandle userHandle) {
        mContext = context;
        mUserHandle = userHandle;
        // This guarantees that the helper is ready to use once constructed: the account types and
        // authorities are initialized
        onAccountsUpdated(null);
    }

    /**
     * Get a list of enabled account types for device.
     *
     * @return a list of String for allowed account types.
     */
    public String[] getEnabledAccountTypes() {
        return mEnabledAccountTypes.toArray(new String[mEnabledAccountTypes.size()]);
    }

    /**
     * Cache of icon picture for account type.
     *
     * @param accountType the type of account
     */
    public void preloadDrawableForType(final Context context, final String accountType) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getDrawableForType(context, accountType);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    /**
     * Gets an icon associated with a particular account type. If none found, return default.
     * Icon will be cached in mAccTypeIconCache if loaded.
     *
     * @param accountType the type of account
     * @return a drawable for the icon or a default icon returned by
     * {@link PackageManager#getDefaultActivityIcon} if one cannot be found.
     */
    public Drawable getDrawableForType(Context context, String accountType) {
        synchronized (mAccTypeIconCache) {
            if (mAccTypeIconCache.containsKey(accountType)) {
                return mAccTypeIconCache.get(accountType);
            }
        }
        Drawable icon = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            try {
                AuthenticatorDescription desc = mTypeToAuthDescription.get(accountType);
                Context authContext = context.createPackageContextAsUser(desc.packageName, 0,
                        mUserHandle);
                icon = mContext.getPackageManager().getUserBadgedIcon(
                        authContext.getDrawable(desc.iconId), mUserHandle);
                synchronized (mAccTypeIconCache) {
                    mAccTypeIconCache.put(accountType, icon);
                }
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Log.w(TAG, "Name not found for account type " + accountType);
            }
        }
        if (icon == null) {
            icon = context.getPackageManager().getDefaultActivityIcon();
        }
        return icon;
    }

    /**
     * Updates auth descriptions. Subclasses should call this in onCreate()
     * and update any UI that depends on AuthenticatorDescriptions in onAuthDescriptionsUpdated().
     */
    public void updateAuthDescriptions(Context context) {
        AuthenticatorDescription[] authDescs = AccountManager.get(context)
                .getAuthenticatorTypesAsUser(mUserHandle.getIdentifier());
        for (int i = 0; i < authDescs.length; i++) {
            mTypeToAuthDescription.put(authDescs[i].type, authDescs[i]);
        }
    }

    void onAccountsUpdated(Account[] accounts) {
        updateAuthDescriptions(mContext);
        if (accounts == null) {
            accounts = AccountManager.get(mContext).getAccountsAsUser(mUserHandle.getIdentifier());
        }
        mEnabledAccountTypes.clear();
        mAccTypeIconCache.clear();
        for (int i = 0; i < accounts.length; i++) {
            Account account = accounts[i];
            if (!mEnabledAccountTypes.contains(account.type)) {
                mEnabledAccountTypes.add(account.type);
            }
        }
    }
}