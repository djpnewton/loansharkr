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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LoanSharkr extends ListActivity {
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_OPEN_LOANS = 1;
    private static final int ACTIVITY_CLOSED_LOANS = 2;
    private static final int ACTIVITY_EDIT = 3;

    private static final int ADD_ID = Menu.FIRST;
    private static final int OPEN_LOANS_ID = Menu.FIRST + 1;
    private static final int CLOSED_LOANS_ID = Menu.FIRST + 2;
    private static final int EDIT_ID = Menu.FIRST + 3;
    private static final int DELETE_ID = Menu.FIRST + 4;

    private LoanSharkrDbAdapter mDbHelper;

    public class ClientRowCursorAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        public ClientRowCursorAdapter(Context context, Cursor c) {
            super(context, c);
            this.mInflater = getLayoutInflater();
        }

        @Override
        public View newView(Context ctx, Cursor c, ViewGroup viewGroup) {
            View view = mInflater.inflate(R.layout.client_row, viewGroup, false);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView imageView = (ImageView) view.findViewById(R.id.photo);
            TextView tvClient = (TextView) view.findViewById(R.id.client);
            TextView tvPhone = (TextView) view.findViewById(R.id.phone);
            ImageView ivIcon = (ImageView) view.findViewById(R.id.icon);

            byte[] imgData = cursor.getBlob(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_PHOTO));
            if (imgData != null) {
                Bitmap photo = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
                imageView.setImageBitmap(photo);
            }
            tvClient.setText(cursor.getString(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_CLIENT)));
            tvPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_PHONE)));

            long clientId = cursor.getLong(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_ROWID));
            if (LoanHelper.ClientHasOverdueLoan(mDbHelper, clientId)) {
                ivIcon.setVisibility(View.VISIBLE);
                showLoanAlert();
            }
            else {
                ivIcon.setVisibility(View.GONE);
            }
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_list);
        mDbHelper = new LoanSharkrDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }

    private boolean loanAlertShown = false;
    private void showLoanAlert() {
        if (loanAlertShown == false) {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.loan_due_alert,
                                           (ViewGroup) findViewById(R.id.loan_alert));
            TextView tv = (TextView) layout.findViewById(R.id.quip);
            tv.setText(Quips.GetRandomOverdueLoanQuip());
            Toast toast = new Toast(this);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
            loanAlertShown = true;
        }
    }


    private void fillData() {
        // Get all of the rows from the database and create the item list
        Cursor clientsCursor = mDbHelper.fetchAllClients();
        startManagingCursor(clientsCursor);

        // Now create the ClientRowCursorAdapter and set it to display
        ClientRowCursorAdapter clients =
            new ClientRowCursorAdapter(this, clientsCursor);
        setListAdapter(clients);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, 0, R.string.menu_add_client);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case ADD_ID:
                createClient();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, OPEN_LOANS_ID, 0, R.string.menu_view_open_loans);
        menu.add(0, CLOSED_LOANS_ID, 0, R.string.menu_view_closed_loans);
        menu.add(0, EDIT_ID, 0, R.string.menu_edit_client);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete_client);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent i;
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case OPEN_LOANS_ID:
                i = new Intent(this, ClientLoans.class);
                i.putExtra(LoanSharkrDbAdapter.KEY_ROWID, info.id);
                i.putExtra(ClientLoans.SHOW_CLOSED, false);
                startActivityForResult(i, ACTIVITY_OPEN_LOANS);
                return true;
            case CLOSED_LOANS_ID:
                i = new Intent(this, ClientLoans.class);
                i.putExtra(LoanSharkrDbAdapter.KEY_ROWID, info.id);
                i.putExtra(ClientLoans.SHOW_CLOSED, true);
                startActivityForResult(i, ACTIVITY_CLOSED_LOANS);
                return true;
            case DELETE_ID:
                mDbHelper.deleteClient(info.id);
                fillData();
                return true;
            case EDIT_ID:
                i = new Intent(this, ClientEdit.class);
                i.putExtra(LoanSharkrDbAdapter.KEY_ROWID, info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createClient() {
        Intent i = new Intent(this, ClientEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ClientLoans.class);
        i.putExtra(LoanSharkrDbAdapter.KEY_ROWID, id);
        i.putExtra(ClientLoans.SHOW_CLOSED, false);
        startActivityForResult(i, ACTIVITY_OPEN_LOANS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
}
