package com.example.android.pets;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.example.android.pets.data.PetContract.PetEntry;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /** Database helper that will provide us access to the database */
    private static final int PET_LOADER = 0;
    PetCursorAdapter CursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });
        //Find the ListView which will be populated with the pet data
        ListView petListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        petListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of the pet data in the cursor.
        // There is no pet data yet (until the loader finishes) so pass is null for the cursor.

        //------------------------
        CursorAdapter = new PetCursorAdapter(this,null);
        petListView.setAdapter(CursorAdapter);

        petListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Create a new intent to go to {@link EditorActivity.class}
                Intent intent = new Intent(CatalogActivity.this,EditorActivity.class);


                // Form the content URI that represents the specific pet that was clicked on,
                // by appearing the "id" (passed as input to this method) onto the
                // {@link PetEntry#CONTENT_URI},
                // for example, the URI would be "content://com.example.android.pets/pets/2"
                //if the pet with ID 2 was clicked on
                Uri currentPetUri = ContentUris.withAppendedId(PetEntry.CONTENT_URI,id);

                //Set the URI on the data field of the intent
                intent.setData(currentPetUri);

                //Launch the  {@ link EditorActivity} to display the data for the current pet
                startActivity(intent);
            }
        });


        //kick off the loader
        getLoaderManager().initLoader(PET_LOADER,null,this);
    }

    private void insertPet() {
        // Create a ContentValues object where column names are the keys,
        // and Toto's pet attributes are the values.
        ContentValues values = new ContentValues();
        values.put(PetEntry.COLUMN_PET_NAME, "Toto");
        values.put(PetEntry.COLUMN_PET_BREED, "Terrier");
        values.put(PetEntry.COLUMN_PET_GENDER, PetEntry.GENDER_MALE);
        values.put(PetEntry.COLUMN_PET_WEIGHT, 7);

        // Insert a new row for Toto in the database, returning the ID of that new row.
        // The first argument for db.insert() is the pets table name.
        // The second argument provides the name of a column in which the framework
        // can insert NULL in the event that the ContentValues is empty (if
        // this is set to "null", then the framework will not insert a row when
        // there are no values).
        // The third argument is the ContentValues object containing the info for Toto.

        Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI,values);
    }

    private void deleteAllPets(){
        int rowsDeleted = getContentResolver().delete(PetEntry.CONTENT_URI,null,null);
        Log.v("CatalogActivity", rowsDeleted + "rows deleted from pet database");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertPet();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllPets();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define the projection that specifies the columns from the table.
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,PetEntry.COLUMN_PET_BREED};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   //Parent Activity Context
                PetEntry.CONTENT_URI,         // Provider content URI to query
                projection,                   // Columns to include in the resulting Cursor
                null,                  // No selection clause
                null,               // No selection arguments
                null );               // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link PetCursorAdapter} with this new cursor containing updated pet data
        CursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        CursorAdapter.swapCursor(null);
    }
}
