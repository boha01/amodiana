package com.mycompany.application;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * High-priority initialization provider (initOrder = 2147483647).
 * Runs SilkGuard signature spoofing BEFORE PlayGamesInitProvider,
 * FirebaseInitProvider, FacebookInitProvider, and VkidInitProvider cache PackageInfo.
 */
public class GuardInitProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        MainActivity.ensureSilkGuard(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
