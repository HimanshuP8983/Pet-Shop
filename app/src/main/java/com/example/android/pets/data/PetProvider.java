package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class PetProvider extends ContentProvider {

    private PetDbHelper mDbHelper;

    /* Tag for the log messages*/
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    /* URI matcher code for the content URI for the pets table */
    private static final int PETS = 100;

    /* URI matcher code for the content URI for a single pet in the pets table */
    private static final int PET_ID = 101;
    /*
    * UriMatcher object to match a content URI to a corresponding code
    * The input passed into the constructor represents the code to return for the root URI.
    * It's common to use NO_MATCH as the input for this case.
    */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);


    //static initializer. This is run the first time anything is called from this class.
    static{
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS,PETS);

        // The content URI of the form "content://com.example.android.pets/pets/#" will map to the
        // the integer code {@link #PET_ID}. This URI is used to provide access to ONE single row of the pets table.
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS + "/#", PET_ID);

    }

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }


    @Override
    public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // Cursor will hold the result of query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to specific code
        int match = sUriMatcher.match(uri);
        switch(match){
            case PETS:
                // For the PETS code, query the pets table  directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                // TODO: Perform database query on pets table
                cursor = database.query(PetContract.PetEntry.TABLE_NAME,projection,selection,selectionArgs,null,null,sortOrder);
                break;
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.

                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments String array.

                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri)) };


                // This will perform a query on the pets table where the _id equals 3 to return a
                // cursor containing that row of the table.
                cursor = database.query(PetContract.PetEntry.TABLE_NAME,projection,selection,
                        selectionArgs,null,null,sortOrder);
                break;
            default: throw new IllegalArgumentException("Cannot query URI " + uri);
        }
        // Set notification URI on the cursor
        // so we know what content URI the Cursor was created for
        // if the data at this URI changes, then we know we need to update the cursor
        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri,ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch(match){
            case PETS:
                return insertPet(uri,contentValues);
            default: throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }

    }

    //Sanity Check
    private Uri insertPet(Uri uri, ContentValues contentValues) {
        //Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Check the name is not null
        String name = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        String breed = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_BREED);
        Integer gender = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        Integer weight = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);

        if(name == null){
            throw new IllegalArgumentException("Pet requires a name");
        }
        if(breed == null || !PetContract.PetEntry.isValidGender(gender)){
            throw  new IllegalArgumentException("Pet requires valid breed");
        }
        if(weight == null && weight<0){
            throw new IllegalArgumentException("Pet requires valid weight");
        }


        //Insert the new pet with the given values
        long id = database.insert(PetContract.PetEntry.TABLE_NAME,null,contentValues);
        if(id == -1){
            Log.e(LOG_TAG,"Failed to insert row for "+ uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return ContentUris.withAppendedId(uri,id);
    }



    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that are deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                rowsDeleted = database.delete(PetContract.PetEntry.TABLE_NAME,selection,selectionArgs);
                break;
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{ String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(PetContract.PetEntry.TABLE_NAME,selection,selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported");
        }
        if(rowsDeleted!=0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch(match){
            case PETS:
                return updatePet(uri, values, selection, selectionArgs);
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri,values,selection,selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO: Update the selected pets in the pets database table
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetContract.PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }

        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)) {
            Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }
        // TODO: Return the number of rows that were affected
        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(PetContract.PetEntry.TABLE_NAME,values,selection,selectionArgs);

        // if 1 or more rows are updated, then notify all the listeners that the data at the given URI has changed
        if(rowsUpdated!=0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        // return the number of rows updated
        return rowsUpdated;
    }
}
