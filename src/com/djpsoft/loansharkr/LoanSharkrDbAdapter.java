/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.djpsoft.loansharkr;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

/**
 * Simple LoanSharkr database access helper class. Defines the basic CRUD operations
 * and gives the ability to list all clients as well as
 * retrieve or modify a specific client account.
 *
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class LoanSharkrDbAdapter {

    public static final String KEY_ROWID = "_id";
    public static final String KEY_CLIENT = "client";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_NOTES = "notes";
    public static final String KEY_PHOTO = "photo";

    public static final String KEY_CLIENTID = "client_id";
    public static final String KEY_DEBT = "debt";
    public static final String KEY_WEEKLYINTEREST = "weekly_interest";
    public static final String KEY_DATE = "date";
    public static final String KEY_MATURITYDATE = "maturity_date";
    public static final String KEY_STATUS = "status";

    public static final int JPEG_QUALITY = 90;

    public static final int LOAN_STATUS_OPEN = 0;
    public static final int LOAN_STATUS_PAID = 1;
    public static final int LOAN_STATUS_BAD = 2;

    private static final String TAG = "LoanSharkrDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE_CLIENTS =
        "create table clients (_id integer primary key autoincrement, "
        + "client text not null, phone text not null, notes text not null, photo blob);";

    private static final String DATABASE_CREATE_LOANS =
        "create table loans (_id integer primary key autoincrement, "
        + "client_id integer, debt integer, weekly_interest integer, date integer, maturity_date integer, status integer);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE_CLIENTS = "clients";
    private static final String DATABASE_TABLE_LOANS = "loans";
    private static final int DATABASE_VERSION = 4;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_CLIENTS);
            db.execSQL(DATABASE_CREATE_LOANS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS clients");
            db.execSQL("DROP TABLE IF EXISTS loans");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public LoanSharkrDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the client database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public LoanSharkrDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new client using the name and notes and bitmap provided. If the
     * client is successfully created return the new rowId for that client,
     * otherwise return a -1 to indicate failure.
     *
     * @param client the name of the client
     * @param notes the notes about the client
     * @param photo a picture of the client
     * @return rowId or -1 if failed
     */
    public long createClient(String client, String phone, String notes, Bitmap photo) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_CLIENT, client);
        initialValues.put(KEY_PHONE, phone);
        initialValues.put(KEY_NOTES, notes);
        if (photo != null) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            photo.compress(CompressFormat.JPEG, JPEG_QUALITY, s);
            initialValues.put(KEY_PHOTO, s.toByteArray());
        }
        return mDb.insert(DATABASE_TABLE_CLIENTS, null, initialValues);
    }

    /**
     * Delete the client with the given rowId
     *
     * @param rowId id of client to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteClient(long rowId) {
        return mDb.delete(DATABASE_TABLE_CLIENTS, KEY_ROWID + "=" + rowId, null) > 0 &&
               mDb.delete(DATABASE_TABLE_LOANS, KEY_CLIENTID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all clients in the database
     *
     * @return Cursor over all clients
     */
    public Cursor fetchAllClients() {

        return mDb.query(DATABASE_TABLE_CLIENTS, new String[] {KEY_ROWID, KEY_CLIENT,
                KEY_PHONE, KEY_NOTES, KEY_PHOTO}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the client that matches the given rowId
     *
     * @param rowId id of client to retrieve
     * @return Cursor positioned to matching client, if found
     * @throws SQLException if client could not be found/retrieved
     */
    public Cursor fetchClient(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_CLIENTS, new String[] {KEY_ROWID,
                    KEY_CLIENT, KEY_PHONE, KEY_NOTES, KEY_PHOTO}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the client using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     *
     * @param rowId id of client to update
     * @param title value to set client name to
     * @param body value to set client notes to
     * @return true if the client was successfully updated, false otherwise
     */
    public boolean updateClient(long rowId, String client, String phone, String notes, Bitmap photo) {
        ContentValues args = new ContentValues();
        args.put(KEY_CLIENT, client);
        args.put(KEY_PHONE, phone);
        args.put(KEY_NOTES, notes);
        if (photo != null) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            photo.compress(CompressFormat.JPEG, JPEG_QUALITY, s);
            args.put(KEY_PHOTO, s.toByteArray());
        }

        return mDb.update(DATABASE_TABLE_CLIENTS, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor with all the loan rows specified by the client id
     *
     * @param clientId id of client query
     * @param fetchClosed if false return only LOAN_STATUS_OPEN loans,
     * if true then return LOAN_STATUS_PAID and LOAN_STATUS_SUSPENDED loans
     * @return Cursor positioned to matching loans, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchAllLoansFromClient(Long clientId, boolean fetchClosed) {
        String whereClause = KEY_CLIENTID + " = ?";
        String[] whereArgs = new String[] {clientId.toString(), Long.toString(LOAN_STATUS_OPEN)};
        if (fetchClosed) {
            whereClause += " AND " + KEY_STATUS + " > ?";
        }
        else {
            whereClause += " AND " + KEY_STATUS + " = ?";
        }
        return mDb.query(DATABASE_TABLE_LOANS, new String[] {KEY_ROWID, KEY_DEBT, KEY_WEEKLYINTEREST, KEY_DATE, KEY_MATURITYDATE, KEY_STATUS},
            whereClause,
            whereArgs, null, null, null);
    }

    /**
     * Create a new client loan using the clientid, debt amount, interest rate and
     * maturity date provided.
     * If successfully created return the new rowId for that loan, otherwise return
     * a -1 to indicate failure.
     *
     * @param mClientId the client id of the loanee
     * @param debt the value of the debt
     * @param debt the value of the debt
     * @param weekly_interest the weekly_interest rate
     * @param date the starting date of the loan
     * @param maturity_date the date the debt is due
     * @return rowId or -1 if failed
     */
    public long createClientLoan(Long mClientId, BigDecimal debt, BigDecimal weekly_interest, long date, long maturity_date) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_CLIENTID, mClientId);
        initialValues.put(KEY_DEBT, LoanHelper.ConvertCurrencyToInteger(debt));
        initialValues.put(KEY_WEEKLYINTEREST, LoanHelper.ConvertCurrencyToInteger(weekly_interest));
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_MATURITYDATE, maturity_date);
        initialValues.put(KEY_STATUS, LOAN_STATUS_OPEN);
        return mDb.insert(DATABASE_TABLE_LOANS, null, initialValues);
    }

    /**
     * Delete the loan with the given rowId
     *
     * @param rowId id of loan to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteLoan(long rowId) {
        return mDb.delete(DATABASE_TABLE_LOANS, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor positioned at the client loan that matches the given rowId
     *
     * @param rowId id of client to retrieve
     * @return Cursor positioned to matching loan, if found
     * @throws SQLException if loan could not be found/retrieved
     */
	public Cursor fetchClientLoan(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE_LOANS, new String[] {KEY_ROWID,
                    KEY_CLIENTID, KEY_DEBT, KEY_WEEKLYINTEREST, KEY_DATE, KEY_MATURITYDATE, KEY_STATUS}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the loan using the details provided. The loan to be updated is
     * specified using the rowId, and it is altered to use the values passed in
     *
     * @param rowId id of client to update
     * @param debt value to set loan debt
     * @param weekly_interest value to set the loan weekly interest to
     * @param maturity_date value to set the loan maturity date
     * @return true if the client was successfully updated, false otherwise
     */
    public boolean updateClientLoan(long rowId, BigDecimal debt, BigDecimal weekly_interest, long maturity_date, long status) {
        ContentValues args = new ContentValues();
        args.put(KEY_DEBT, LoanHelper.ConvertCurrencyToInteger(debt));
        args.put(KEY_WEEKLYINTEREST, LoanHelper.ConvertCurrencyToInteger(weekly_interest));
        args.put(KEY_MATURITYDATE, maturity_date);
        args.put(KEY_STATUS, status);
        return mDb.update(DATABASE_TABLE_LOANS, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
