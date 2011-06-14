package com.djpsoft.loansharkr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

import android.database.Cursor;

public final class LoanHelper {

    // Suppress default constructor for noninstantiability
    private LoanHelper() {
        throw new AssertionError();
    }

    /**
     * Calculate the interest (compounding weekly) on a loan.
     *
     * @param startDate the starting date of the loan
     * @param endDate the end date of the loan
     * @param debt the size of the loan
     * @param weekly_interest the weekly interest rate of the loan
     * @return the total repayment amount
     */
    public static BigDecimal CalculateTotalRepayment(Date startDate, Date endDate, BigDecimal debt, BigDecimal weekly_interest) {
        BigDecimal diff_ms = new BigDecimal(endDate.getTime() - startDate.getTime());
        if (diff_ms.compareTo(new BigDecimal(0)) == 1) {
            BigDecimal week_ms = new BigDecimal(1000 * 60 * 60 * 24 * 7);
            BigDecimal weeks = diff_ms.divide(week_ms, 10, RoundingMode.HALF_EVEN);
            BigDecimal interest_multiplier = weekly_interest.divide(new BigDecimal(100));
            while (weeks.compareTo(new BigDecimal(0)) == 1) {
                BigDecimal earnedInterest = debt.multiply(interest_multiplier);
                // if this is a partial week then modify the earned interest to reflect that
                if (weeks.compareTo(new BigDecimal(1)) == -1) {
                    earnedInterest = earnedInterest.multiply(weeks);
                }
                debt = debt.add(earnedInterest);
                weeks = weeks.subtract(new BigDecimal(1));
            }
        }
        return debt.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Figure out if a client has an overdue loan
     *
     * @param clientId the client id
     * @return true if the client has an overdue loan
     */
    public static boolean ClientHasOverdueLoan(LoanSharkrDbAdapter db, long clientId) {
        final Calendar cal = Calendar.getInstance();
        Date currentDate = cal.getTime();
        db.open();
        Cursor c = db.fetchAllLoansFromClient(clientId, false);
        try {
            if (c.moveToFirst())
            {
                do {
                    Date maturityDate = new Date(c.getLong(c.getColumnIndexOrThrow(LoanSharkrDbAdapter.KEY_MATURITYDATE)));
                    if (LoanIsOverdue(currentDate, maturityDate)) {
                        return true;
                    }
                } while (c.moveToNext());
            }
        }
        finally {
            c.close();
        }
        return false;
    }

    /**
     * Figure out if a loan is overdue (day after due date)
     *
     * @param currentDate the current date
     * @param maturityDate the due date of the load
     * @return true if the client has an overdue loan
     */
    public static boolean LoanIsOverdue(Date currentDate, Date maturityDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(maturityDate);
        cal.add(Calendar.DATE, 1);
        Date maturityDatePlusOneDay = cal.getTime();
        return currentDate.after(maturityDatePlusOneDay) || currentDate.equals(maturityDatePlusOneDay);
    }

    /**
     * Convert currency to an integer (ie 5.34 -> 534, 10.10 -> 1010)
     *
     * @param value the currency value
     * @return the integer representation
     */
    public static long ConvertCurrencyToInteger(BigDecimal value) {
        return value.longValue() * 100 +
            value.subtract(new BigDecimal(value.longValue())).multiply(new BigDecimal(100)).longValue();
    }

    /**
     * Convert an integer to currency (ie 534 -> 5.34, 1010 -> 10.10)
     *
     * @param value the currency value
     * @return the integer representation
     */
    public static BigDecimal ConvertIntegerToCurrency(long value) {
        BigDecimal temp = new BigDecimal(value);
        return temp.divide(new BigDecimal(100));
    }

}
