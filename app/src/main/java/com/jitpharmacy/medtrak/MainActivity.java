package com.jitpharmacy.medtrak;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    TextView resultBox;
    DBAdapter myDb;
    private SimpleAdapter sa;
    ListView drugListView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //resultBox = (TextView) findViewById(R.id.scan_content);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.initiateScan();
            }
        });
        drugListView = (ListView) findViewById(R.id.drugListView);
        openDB();
        //myDb.insertNewDrug("Clopidogrel",1,"6158899");
        //myDb.insertNewDrug("Lisinopril",1,"6158844");
        //myDb.insertNewDrug("Carvedilol",2,"6158800");
        fillList();
    }

    private void fillList() {

        List<Map<String,String>> al = new ArrayList<Map<String,String>>();
        Map<String,String> item;
        Cursor drugCursor = myDb.getAllRowsDrug();
        if (drugCursor.moveToFirst()){
            do {
                item = new HashMap<String, String>();
                item.put("drug", drugCursor.getString(drugCursor.getColumnIndex(DBAdapter.KEY_DRUG)));
                item.put("freq","Take this medication " + drugCursor.getString(drugCursor.getColumnIndex(DBAdapter.KEY_FREQ)) + " time(s) per day");
                item.put("drugID",drugCursor.getString(drugCursor.getColumnIndex(DBAdapter.KEY_ROWID)));
                al.add(item);
            } while (drugCursor.moveToNext());
        }
        sa = new SimpleAdapter(this,al,android.R.layout.simple_list_item_2,new String[]{"drug","freq"},new int[]{android.R.id.text1,android.R.id.text2});
        drugListView.setAdapter(sa);
        drugListView.setClickable(true);
        drugListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String,String> t = (HashMap) parent.getItemAtPosition(position);
                Log.v(TAG,"DrugID clicked="+t.get("drugID"));
                Intent ddetail = new Intent(MainActivity.this, DrugDetail.class);
                ddetail.putExtra("drugID",t.get("drugID"));
                ddetail.putExtra("drug",t.get("drug"));
                startActivity(ddetail);
            }
        });

    }



    private void openDB() {
        myDb = new DBAdapter(this);
        myDb.open();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDb();
    }

    private void closeDb() {
        myDb.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult.getContents() != null) {
                // handle scan result
                //MessageBox.run(this, "", "toString() returns: " + scanResult.toString());
                final String barcode = scanResult.getContents().toString();
                int id = myDb.getDrugId(barcode);
                String record;
                //found match
                if (id>0) {
                    // TODO: 8/16/2016  implement recording of med scan

                    //record = myDb.getDrugName((int) id);
                    long tsLog = myDb.recordTimestamp(id);
                    Log.v(TAG,"timestamplog = " + tsLog);
                } else {
                    //no match,
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);



                    builder.setTitle("Medication not found");
                    builder.setMessage("Do you want to add " + barcode + " to your medication list?");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing but close the dialog

                            dialog.dismiss();
                            Snackbar linkSB = Snackbar.make(findViewById(android.R.id.content), "Select the medication you want to link", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("Add New", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View View) {
                                            Intent toNew = new Intent(MainActivity.this, AddLink.class);
                                            toNew.putExtra("barcode", barcode);
                                            startActivity(toNew);
                                        }
                                    });
                            linkSB.show();
                        }
                    });

                    builder.setNegativeButton("No, go back", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // Do nothing
                            dialog.dismiss();

                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                    // TODO: 8/16/2016 Implement add/relink of medscan
                    //Intent toNew =  new Intent(MainActivity.this, AddLink.class);
                    //toNew.putExtra("barcode",barcode);
                    //startActivity(toNew);
                    record = "no match found!";
                }
                //resultBox.setText(barcode + " | " + record);
            } else {
                // else continue with any other code you need in the method

            }
        }
    }

    private String displayRecordSet(Cursor cursor) {
        String message = "";
        // populate the message from the cursor

        // Reset cursor to start, checking to see if there's data:
        if (cursor.moveToFirst()) {
            do {
                // Process the data:
                int id = cursor.getInt(DBAdapter.COL_ROWID);
                String drug = cursor.getString(DBAdapter.COL_DRUG);
                int freq = cursor.getInt(DBAdapter.COL_FREQ);

                // Append data to the message:
                message += "id=" + id
                        +", drug=" + drug
                        +", freq=" + freq
                        +"\n";
            } while(cursor.moveToNext());
        }

        // Close the cursor to avoid a resource leak.
        cursor.close();

        return message;
    }
}
