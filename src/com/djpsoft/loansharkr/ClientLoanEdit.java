package com.djpsoft.loansharkr;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class ClientLoanEdit extends Activity implements TextWatcher  {

    private LoanSharkrDbAdapter mDbHelper;

    private EditText mLoanStartText;
    private EditText mLoanEndText;
    private Button mPickDate;
    private EditText mDebtText;
    private EditText mWeeklyInterestText;
    private TextView mTotalPaymentText;
    private RadioButton mLoanOpen;
    private RadioButton mLoanPaid;
    private RadioButton mLoanBad;
    private Long mRowId;
    private Long mClientId;

    private Date mLoanStart;
    private Date mLoanEnd;
    private int mLoanStatus;

    static final int DATE_DIALOG_ID = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new LoanSharkrDbAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.loan_edit);
        setTitle(R.string.loan_edit);

        // capture our View elements
        mLoanStartText = (EditText) findViewById(R.id.loan_start_edit);
        mLoanEndText = (EditText) findViewById(R.id.loan_end_edit);
        mPickDate = (Button) findViewById(R.id.pick_date);
        mDebtText = (EditText) findViewById(R.id.debt_edit);
        mWeeklyInterestText = (EditText) findViewById(R.id.weekly_interest_edit);
        mTotalPaymentText = (TextView) findViewById(R.id.total_payment_text);
        mLoanOpen = (RadioButton) findViewById(R.id.loan_open);
        mLoanPaid = (RadioButton) findViewById(R.id.loan_paid);
        mLoanBad = (RadioButton) findViewById(R.id.loan_bad);

        // listen to changes so we can update total repayment
        mLoanStartText.addTextChangedListener(this);
        mLoanEndText.addTextChangedListener(this);
        mDebtText.addTextChangedListener(this);
        mWeeklyInterestText.addTextChangedListener(this);

        // add a click listener to the date button
        mPickDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

        // add checked change listeners to the radio buttons
        mLoanOpen.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked) {
                    mLoanStatus = LoanSharkrDbAdapter.LOAN_STATUS_OPEN;
                }
            }
        });
        mLoanPaid.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked) {
                    mLoanStatus = LoanSharkrDbAdapter.LOAN_STATUS_PAID;
                }
            }
        });
        mLoanBad.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked) {
                    mLoanStatus = LoanSharkrDbAdapter.LOAN_STATUS_BAD;
                }
            }
        });

        // get the current date (remove time of day component)
        final Calendar c = Calendar.getInstance();
        Date d = c.getTime();
        mLoanStart = new Date(d.getYear(), d.getMonth(), d.getDate());
        mLoanEnd = new Date(d.getYear(), d.getMonth(), d.getDate());

        mLoanStatus = LoanSharkrDbAdapter.LOAN_STATUS_OPEN;

        // initialize rowId/clientId
        Bundle extras = getIntent().getExtras();
        mRowId = null;
        if (savedInstanceState != null) {
            mRowId = (Long) savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_ROWID);
            mDebtText.setText((String)savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_DEBT));
            mWeeklyInterestText.setText((String)savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_WEEKLYINTEREST));
            mLoanStart.setTime((Long)savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_DATE));
            mLoanEnd.setTime((Long)savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_MATURITYDATE));
            mLoanStatus = (Integer) savedInstanceState.getSerializable(LoanSharkrDbAdapter.KEY_STATUS);
            populateDate();
            populateLoanStatus();
        }
        else {
            if (extras.containsKey(LoanSharkrDbAdapter.KEY_ROWID)) {
                mRowId = extras.getLong(LoanSharkrDbAdapter.KEY_ROWID);
            }
            mClientId = null;
            if (extras.containsKey(LoanSharkrDbAdapter.KEY_CLIENTID)) {
                mClientId = extras.getLong(LoanSharkrDbAdapter.KEY_CLIENTID);
            }
            populateFieldsFromDb();
        }

        Button saveButton = (Button) findViewById(R.id.save_loan_changes);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (saveStateToDb() == true) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }

    private void populateFieldsFromDb() {
        if (mRowId != null) {
            Cursor loan = mDbHelper.fetchClientLoan(mRowId);
            startManagingCursor(loan);
            BigDecimal debt = LoanHelper.ConvertIntegerToCurrency(
                    loan.getLong(loan.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_DEBT)));
            mDebtText.setText(debt.toPlainString());
            BigDecimal weekly_interest = LoanHelper.ConvertIntegerToCurrency(
                    loan.getLong(loan.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_WEEKLYINTEREST)));
            mWeeklyInterestText.setText(weekly_interest.toPlainString());
            Long date_ms = loan.getLong(loan.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_DATE));
            mLoanStart = new Date(date_ms);
            date_ms = loan.getLong(loan.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_MATURITYDATE));
            mLoanEnd = new Date(date_ms);
            mLoanStatus = (int)loan.getLong(loan.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_STATUS));
        }
        populateDate();
        populateLoanStatus();
    }

    private void populateDate() {
        mLoanStartText.setText(DateFormat.format("MMMM dd, yyyy", mLoanStart));
        mLoanEndText.setText(DateFormat.format("MMMM dd, yyyy", mLoanEnd));
    }

    private void populateLoanStatus() {
        switch (mLoanStatus) {
        case LoanSharkrDbAdapter.LOAN_STATUS_OPEN:
            mLoanOpen.setChecked(true);
            break;
        case LoanSharkrDbAdapter.LOAN_STATUS_PAID:
            mLoanPaid.setChecked(true);
            break;
        case LoanSharkrDbAdapter.LOAN_STATUS_BAD:
            mLoanBad.setChecked(true);
            break;
        }
    }

    // the callback received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                    mLoanEnd = new Date(year - 1900, monthOfYear, dayOfMonth);
                    populateDate();
                }
            };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DATE_DIALOG_ID:
            return new DatePickerDialog(this,
                        mDateSetListener,
                        mLoanEnd.getYear() + 1900, mLoanEnd.getMonth(), mLoanEnd.getDate());
        }
        return null;
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        // TODO Auto-generated method stub
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub
    }

    public void afterTextChanged(Editable s) {
        try {
        BigDecimal debt = new BigDecimal(mDebtText.getText().toString());
        BigDecimal weekly_interest = new BigDecimal(mWeeklyInterestText.getText().toString());
        BigDecimal total_payment = LoanHelper.CalculateTotalRepayment(mLoanStart, mLoanEnd, debt, weekly_interest);
        mTotalPaymentText.setText("$" + total_payment.toString());
        }
        catch (Exception e) {
            mTotalPaymentText.setText(R.string.na);
        }
        finally {

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(LoanSharkrDbAdapter.KEY_ROWID, mRowId);
        outState.putSerializable(LoanSharkrDbAdapter.KEY_DEBT, mDebtText.getText().toString());
        outState.putSerializable(LoanSharkrDbAdapter.KEY_WEEKLYINTEREST, mWeeklyInterestText.getText().toString());
        outState.putSerializable(LoanSharkrDbAdapter.KEY_DATE, mLoanStart.getTime());
        outState.putSerializable(LoanSharkrDbAdapter.KEY_MATURITYDATE, mLoanEnd.getTime());
        outState.putSerializable(LoanSharkrDbAdapter.KEY_STATUS, mLoanStatus);
    }

    private boolean saveStateToDb() {
        try {
            if (mDebtText.getText().length() == 0) {
                Toast.makeText(this, R.string.error_loan_edit_form_no_debt, Toast.LENGTH_LONG).show();
                return false;
            }
            if (mWeeklyInterestText.getText().length() == 0) {
                Toast.makeText(this, R.string.error_loan_edit_form_no_weeklyinterest, Toast.LENGTH_LONG).show();
                return false;
            }
            BigDecimal debt = new BigDecimal(mDebtText.getText().toString());
            BigDecimal weekly_interest = new BigDecimal(mWeeklyInterestText.getText().toString());
            if (mRowId == null) {
                long id = mDbHelper.createClientLoan(mClientId, debt, weekly_interest,
                    mLoanStart.getTime(), mLoanEnd.getTime());
                if (id > 0) {
                    mRowId = id;
                }
            } else {
                mDbHelper.updateClientLoan(mRowId, debt, weekly_interest, mLoanEnd.getTime(), mLoanStatus);
            }
            return true;
        }
        catch (NumberFormatException e)
        {
            Toast toast = Toast.makeText(this, R.string.error_loan_edit_form, Toast.LENGTH_LONG);
            toast.show();
        }
        catch (Exception e)
        {
            Toast toast = Toast.makeText(this, R.string.error_db_update, Toast.LENGTH_LONG);
            toast.show();
        }
        return false;
    }
}
