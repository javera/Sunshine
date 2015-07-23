/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.example.android.sunshine.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    /*
        Students: Uncomment this test once you've written the code to create the Location
        table.  Note that you will have to have chosen the same column names that I did in
        my solution for this test to compile, so if you haven't yet done that, this is
        a good time to change your column names to match mine.

        Note that this only tests that the Location table has the correct columns, since we
        give you the code for the weather table.  This test does not look at the
     */
    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while (c.moveToNext());

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the location entry and weather entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while (c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }

    /*
        Students:  Here is where you will build code to test that we can insert and query the
        location database.  We've done a lot of work for you.  You'll want to look in TestUtilities
        where you can uncomment out the "createNorthPoleLocationValues" function.  You can
        also make use of the ValidateCurrentRecord function from within TestUtilities.
    */
    public void testLocationTable() {
        // Test data we're going to insert into the DB to see if it works.

        // First step: Get reference to writable database
        Context context = getContext();

        WeatherDbHelper dbHelper = new WeatherDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();


        // Create ContentValues of what you want to insert
        // (you can use the createNorthPoleLocationValues if you wish)
        ContentValues originalCv = TestUtilities.createNorthPoleLocationValues();


        // Insert ContentValues into database and get a row ID back
        long rowId = TestUtilities.insertNorthPoleLocationValues(context);
        assertFalse("Error inserting the north pole location values", rowId == -1);

        // Query the database and receive a Cursor back
        final String TABLE_NAME = "location";
        final String[] COLUMNS = new String[]{
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
                WeatherContract.LocationEntry.COLUMN_CITY_NAME,
                WeatherContract.LocationEntry.COLUMN_COORD_LAT,
                WeatherContract.LocationEntry.COLUMN_COORD_LONG
        }; // want just the four columns
        final String WHERE = WeatherContract.LocationEntry._ID + " == ?"; // where clause: _id column
        final String[] WHERE_ARGS = new String[]{Long.toString(rowId)}; // _id == retrieved id from insert statement
        final String GROUP_BY = null; // no grouping
        final String HAVING = null; // no having
        final String ORDER_BY = null; // one row, so no ordering


        Cursor results = db.query(TABLE_NAME, COLUMNS, WHERE, WHERE_ARGS, GROUP_BY, HAVING, ORDER_BY);
        assertEquals("Should have gotten 1 row as a result", results.getCount(), 1);
        assertNotNull(results);
        // Move the cursor to a valid database row
        if (results != null) {
            results.moveToNext();
            assertEquals(results.getString(0), originalCv.getAsString(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING));
            assertEquals(results.getString(1), originalCv.getAsString(WeatherContract.LocationEntry.COLUMN_CITY_NAME));
            assertEquals(results.getDouble(2), originalCv.getAsDouble(WeatherContract.LocationEntry.COLUMN_COORD_LAT));
            assertEquals(results.getDouble(3), originalCv.getAsDouble(WeatherContract.LocationEntry.COLUMN_COORD_LONG));
        }

        results.close();
        db.close();
        // Finally, close the cursor and database

    }

    /*
        Students:  Here is where you will build code to test that we can insert and query the
        database.  We've done a lot of work for you.  You'll want to look in TestUtilities
        where you can use the "createWeatherValues" function.  You can
        also make use of the validateCurrentRecord function from within TestUtilities.
     */
    public void testWeatherTable() {

        // First insert the location, and then use the locationRowId to insert
        // the weather. Make sure to cover as many failure cases as you can.

        // Instead of rewriting all of the code we've already written in testLocationTable
        // we can move this code to insertLocation and then call insertLocation from both
        // tests. Why move it? We need the code to return the ID of the inserted location
        // and our testLocationTable can only return void because it's a test.

        // First step: Get reference to writable database
        Context context = getContext();

        WeatherDbHelper dbHelper = new WeatherDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long locationID = TestUtilities.insertNorthPoleLocationValues(context);
        assertTrue(locationID != -1);

        // Create ContentValues of what you want to insert
        // (you can use the createWeatherValues TestUtilities function if you wish)
        ContentValues weatherCV = TestUtilities.createWeatherValues(locationID);

        // Insert ContentValues into database and get a row ID back
        long weatherID = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherCV);
        assertTrue(weatherID != -1);

        // Query the database and receive a Cursor back
        final String TABLE_NAME = WeatherContract.WeatherEntry.TABLE_NAME;
        final String[] COLUMNS = null; // want all columns
        final String WHERE = WeatherContract.WeatherEntry._ID + " == ?"; // where clause: _id column
        final String[] WHERE_ARGS = new String[]{Long.toString(weatherID)}; // _id == retrieved id from insert statement
        final String GROUP_BY = null; // no grouping
        final String HAVING = null; // no having
        final String ORDER_BY = null; // one row, so no ordering

        Cursor results = db.query(TABLE_NAME, COLUMNS, WHERE, WHERE_ARGS, GROUP_BY, HAVING, ORDER_BY);

        // Move the cursor to a valid database row
        TestUtilities.validateCursor("weather entry write/read test error", results, weatherCV);

        // Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)

        results.close();
        db.close();
        // Finally, close the cursor and database
    }


    /*
        Students: This is a helper method for the testWeatherTable quiz. You can move your
        code from testLocationTable to here so that you can call this code from both
        testWeatherTable and testLocationTable.
     */
    public long insertLocation() {
        return -1L;
    }
}
