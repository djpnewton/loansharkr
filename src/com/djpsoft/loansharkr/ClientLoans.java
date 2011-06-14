package com.djpsoft.loansharkr;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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

public class ClientLoans extends ListActivity {
    public static final String SHOW_CLOSED = "SHOW_CLOSED";

    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;

    private static final int ADD_ID = Menu.FIRST;
    private static final int EDIT_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;

    private LoanSharkrDbAdapter mDbHelper;
    private Long mClientId;
    private boolean mShowClosed;

    public class ClientLoanRowCursorAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        public ClientLoanRowCursorAdapter(Context context, Cursor c) {
            super(context, c);
            this.mInflater = getLayoutInflater();
        }

        @Override
        public View newView(Context ctx, Cursor c, ViewGroup viewGroup) {
            View view = mInflater.inflate(R.layout.loan_row, viewGroup, false);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tvDate = (TextView) view.findViewById(R.id.date);
            TextView tvDebt = (TextView) view.findViewById(R.id.debt);
            TextView tvTotalRepayment = (TextView) view.findViewById(R.id.total_repayment_text);
            ImageView ivIcon = (ImageView) view.findViewById(R.id.icon);

            Long start_date = cursor.getLong(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_DATE));
            Long maturity_date = cursor.getLong(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_MATURITYDATE));
            BigDecimal debt = LoanHelper.ConvertIntegerToCurrency(
                    cursor.getLong(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_DEBT)));
            BigDecimal weekly_interest = LoanHelper.ConvertIntegerToCurrency(
                    cursor.getLong(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_WEEKLYINTEREST)));

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d");
            String formattedDateString = formatter.format(new Date(maturity_date));
            tvDate.setText(formattedDateString);
            tvDebt.setText("$" + debt.toPlainString());
            BigDecimal totalRepayment = LoanHelper.CalculateTotalRepayment(new Date(start_date),
                    new Date(maturity_date), debt, weekly_interest);
            tvTotalRepayment.setText("$" + totalRepayment.toString());

            int status = cursor.getInt(cursor.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_STATUS));
            switch (status) {
            case LoanSharkrDbAdapter.LOAN_STATUS_OPEN:
                Calendar cal = Calendar.getInstance();
                if (LoanHelper.LoanIsOverdue(cal.getTime(), new Date(maturity_date))) {
                    ivIcon.setVisibility(View.VISIBLE);
                }
                else {
                    ivIcon.setVisibility(View.GONE);
                }
                break;
            case LoanSharkrDbAdapter.LOAN_STATUS_PAID:
                ivIcon.setImageResource(R.drawable.tick);
                ivIcon.setVisibility(View.VISIBLE);
                break;
            case LoanSharkrDbAdapter.LOAN_STATUS_BAD:
                ivIcon.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_loans);
        setTitle(R.string.client_loans);

        Bundle extras = getIntent().getExtras();
        mClientId = extras.getLong(LoanSharkrDbAdapter.KEY_ROWID);
        mShowClosed = extras.getBoolean(SHOW_CLOSED);

        mDbHelper = new LoanSharkrDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, 0, R.string.menu_add_loan);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case ADD_ID:
                createLoan();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, EDIT_ID, 0, R.string.menu_edit_loan);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete_loan);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case EDIT_ID:
                Intent i = new Intent(this, ClientLoanEdit.class);
                i.putExtra(LoanSharkrDbAdapter.KEY_ROWID, info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
            case DELETE_ID:
                mDbHelper.deleteLoan(info.id);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void fillData() {
        // Get all of the rows from the database and create the item list
        Cursor clientsCursor = mDbHelper.fetchAllLoansFromClient(mClientId, mShowClosed);
        startManagingCursor(clientsCursor);

        // Now create the ClientLoanRowCursorAdapter and set it to display
        ClientLoanRowCursorAdapter loans =
            new ClientLoanRowCursorAdapter(this, clientsCursor);
        setListAdapter(loans);
    }

    private void createLoan() {
        Intent i = new Intent(this, ClientLoanEdit.class);
        i.putExtra(LoanSharkrDbAdapter.KEY_CLIENTID, mClientId);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ClientLoanEdit.class);
        i.putExtra(LoanSharkrDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
}
