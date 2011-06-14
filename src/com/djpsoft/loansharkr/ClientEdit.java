/*
 * Copyright (C) 2008 Google Inc.
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

package com.djpsoft.loansharkr;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class ClientEdit extends Activity {

    private static final int ACTIVITY_PHOTO = 0;

    private LoanSharkrDbAdapter mDbHelper;
    private EditText mClientText;
    private EditText mPhoneText;
    private EditText mNotesText;
    private ImageView mPhoto;
    private Long mRowId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new LoanSharkrDbAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.client_edit);
        setTitle(R.string.edit_client);

        mClientText = (EditText) findViewById(R.id.client);
        mPhoneText = (EditText) findViewById(R.id.phone);
        mNotesText = (EditText) findViewById(R.id.notes);
        mPhoto = (ImageView) findViewById(R.id.photo);

        mRowId = null;
        if (savedInstanceState != null) {
            mRowId = (Long) savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_ROWID);
            mClientText.setText((String)savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_CLIENT));
            mPhoneText.setText((String)savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_PHONE));
            mNotesText.setText((String)savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_NOTES));
            if (savedInstanceState.containsKey(LoanSharkrDbAdapter.KEY_PHOTO)) {
                byte[] imgData = (byte[]) savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_PHOTO);
                Bitmap photo = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
                mPhoto.setImageBitmap(photo);
            }
        }
        else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mRowId = extras.getLong(LoanSharkrDbAdapter.KEY_ROWID);
                populateFieldsFromDb();
            }
        }

        Button photoButton = (Button) findViewById(R.id.take_photo);
        photoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                takePhoto();
            }
        });

        Button saveButton = (Button) findViewById(R.id.save_client_changes);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (saveStateToDb() == true) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_PHOTO && resultCode != 0) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            mPhoto.setImageBitmap(thumbnail);
        }
    }

    private void populateFieldsFromDb() {
        if (mRowId != null) {
            Cursor client = mDbHelper.fetchClient(mRowId);
            startManagingCursor(client);
            mClientText.setText(client.getString(
                    client.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_CLIENT)));
            mPhoneText.setText(client.getString(
                    client.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_PHONE)));
            mNotesText.setText(client.getString(
                client.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_NOTES)));
            byte[] imgData = client.getBlob(client.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_PHOTO));
            if (imgData != null) {
                Bitmap photo = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
                mPhoto.setImageBitmap(photo);
            }
        }
    }

    private void takePhoto() {
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, ACTIVITY_PHOTO);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(LoanSharkrDbAdapter.KEY_ROWID, mRowId);
        outState.putSerializable(LoanSharkrDbAdapter.KEY_CLIENT, mClientText.getText().toString());
        outState.putSerializable(LoanSharkrDbAdapter.KEY_PHONE, mPhoneText.getText().toString());
        outState.putSerializable(LoanSharkrDbAdapter.KEY_NOTES, mNotesText.getText().toString());
        BitmapDrawable drawable = (BitmapDrawable)mPhoto.getDrawable();
        if (drawable != null) {
            Bitmap photo = drawable.getBitmap();
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            photo.compress(CompressFormat.JPEG, LoanSharkrDbAdapter.JPEG_QUALITY, s);
            outState.putSerializable(LoanSharkrDbAdapter.KEY_PHOTO, s.toByteArray());
        }
    }

    private boolean saveStateToDb() {
        try {
            if (mClientText.getText().length() == 0) {
                Toast.makeText(this, R.string.error_client_edit_form_no_client, Toast.LENGTH_LONG).show();
                return false;
            }
            String client = mClientText.getText().toString();
            String phone = mPhoneText.getText().toString();
            String notes = mNotesText.getText().toString();
            BitmapDrawable drawable = (BitmapDrawable)mPhoto.getDrawable();
            Bitmap photo = null;
            if (drawable != null) {
                photo = drawable.getBitmap();
            }

            if (mRowId == null) {
                long id = mDbHelper.createClient(client, phone, notes, photo);
                if (id > 0) {
                    mRowId = id;
                }
            } else {
                mDbHelper.updateClient(mRowId, client, phone, notes, photo);
            }
            return true;
        }
        catch (Exception e)
        {
            Toast toast = Toast.makeText(this, R.string.error_db_update, Toast.LENGTH_LONG);
            toast.show();
        }
        return false;
    }
}
