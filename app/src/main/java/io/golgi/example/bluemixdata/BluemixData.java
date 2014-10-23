//
// This Software (the “Software”) is supplied to you by Openmind Networks Limited ("Openmind")
// your use, installation, modification or redistribution of this Software constitutes acceptance
// of this disclaimer.
// If you do not agree with the terms of this disclaimer, please do not use, install, modify
// or redistribute this Software.
//
// TO THE MAXIMUM EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION,
// ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A
// PARTICULAR PURPOSE.
//
// Each user of the Software is solely responsible for determining the appropriateness of
// using and distributing the Software and assumes all risks associated with use of the Software,
// including but not limited to the risks and costs of Software errors, compliance with
// applicable laws, damage to or loss of data, programs or equipment, and unavailability or
// interruption of operations.
//
// TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW OPENMIND SHALL NOT HAVE ANY LIABILITY FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
// WITHOUT LIMITATION, LOST PROFITS, LOSS OF BUSINESS, LOSS OF USE, OR LOSS OF DATA), HOWSOEVER
// CAUSED UNDER ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//

package io.golgi.example.bluemixdata;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.data.IBMData;
import com.ibm.mobile.services.data.IBMDataException;
import com.ibm.mobile.services.data.IBMDataObject;
import com.ibm.mobile.services.data.IBMQuery;

import java.util.List;

import bolts.Continuation;
import bolts.Task;


public class BluemixData extends Activity {

    private static final String UPDATE_UI = "UPDATE_UI";
    private static final String OP_COMPLETE = "OP_COMPLETE";
    private EditText editText;

    private DataItem dataItemToAdd = null;
    private DataItem dataItemToDelete = null;
    private ProgressDialog progressDialog;

    private final int DELETE_COMPLETE = 1 << 0;
    private final int ADD_COMPLETE = 1 << 1;
    private final int RETRIEVE_COMPLETE = 1 << 2;
    private int COMPLETES = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_mix_data);

        // initialise Bluemix
        Log.i("BMT","Initialising Bluemix");
        IBMBluemix.initialize(this,                           // context
                "f4195dc1-a7e4-4e6d-a729-c731dac21732",       // applicationId
                "47c98a21b112eef5d193ec1efa183850d4b9a6b8",   // applicationSecret
                "golgitest.mybluemix.net");                   // applicationRoute

        Log.i("BMT","Initialising IBM Data");
        IBMData.initializeService();
        DataItem.registerSpecialization(DataItem.class);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.blue_mix_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver UIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String value = intent.getStringExtra("VALUE");
            editText.setText(value);
            progressDialog.dismiss();
            progressDialog = null;
        }
    };

    private BroadcastReceiver OPReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            COMPLETES |= intent.getIntExtra("VALUE",0);
            if(COMPLETES >= ADD_COMPLETE) {
                COMPLETES = 0;
                if(progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }
        }
    };

    @Override
    public void onResume(){
        super.onResume();
        editText = (EditText)findViewById(R.id.editText);
        LocalBroadcastManager.getInstance(this).registerReceiver(UIReceiver,new IntentFilter(UPDATE_UI));
        LocalBroadcastManager.getInstance(this).registerReceiver(OPReceiver,new IntentFilter(OP_COMPLETE));
        retrieveData();
    }

    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(UIReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(OPReceiver);
    }

    private void retrieveData(){
        progressDialog = ProgressDialog.show(this,"Retrieving data","Please wait...");
        try {
            Log.i("BMT","Attempting to query");
            IBMQuery<DataItem> query = IBMQuery.queryForClass(DataItem.class);
            query.find().continueWith(new Continuation<List<DataItem>, Object>() {
                @Override
                public Object then(Task<List<DataItem>> listTask) throws Exception {
                    final List<DataItem> object = listTask.getResult();
                    if(listTask.isCancelled()){
                        Log.i("BMT","Task cancelled : " + listTask.toString());
                    }
                    else if(listTask.isFaulted()){
                        Log.i("BMT","Task fault : " + listTask.getError().getMessage());
                        throw listTask.getError();
                    }
                    else{
                        // process the success
                        for(DataItem dItem:object){
                            dataItemToDelete = dItem;   // assign to dataItemToDelete as this will
                                                        // be the next to be deleted
                            Log.i("BMT","Received data following fetch : " + dataItemToDelete.getValue());
                            Intent intent = new Intent(UPDATE_UI);
                            intent.putExtra("VALUE",dataItemToDelete.getValue());
                            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                        }
                    }
                    Intent intent = new Intent(OP_COMPLETE);
                    intent.putExtra("VALUE",RETRIEVE_COMPLETE);
                    LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                    return null;
                }
            });
        }
        catch(IBMDataException ex){
            Log.e(this.getLocalClassName(), "Exception : " + ex.getMessage());
        }
    }

    public void submitData(View view){
        Log.i("BMT","Attempting to submit : " + editText.getText().toString());

        progressDialog = ProgressDialog.show(this,"Submitting data","Please wait");

        deleteItem();

        dataItemToAdd = new DataItem();
        dataItemToAdd.setValue(editText.getText().toString());
        dataItemToAdd.save().continueWith(new Continuation<IBMDataObject, Void>() {
            @Override
            public Void then(Task<IBMDataObject> ibmDataObjectTask) throws Exception {
                if(ibmDataObjectTask.isCancelled()){
                    Log.i("BMT","Task cancelled : " + ibmDataObjectTask.toString());
                }
                else if(ibmDataObjectTask.isFaulted()){
                    Log.i("BMT","Task fault : " + ibmDataObjectTask.getError().getMessage());
                    throw ibmDataObjectTask.getError();
                }
                else{
                    Log.i("BMT","Successfully submitted data");
                    Intent intent = new Intent(OP_COMPLETE);
                    intent.putExtra("VALUE",ADD_COMPLETE);
                    LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                }
                return null;
            }
        });
    }

    public void deleteItem(){
        if(dataItemToDelete != null) {
            dataItemToDelete.delete().continueWith(new Continuation<IBMDataObject, Void>() {
                @Override
                public Void then(Task<IBMDataObject> ibmDataObjectTask) throws Exception {
                    if (ibmDataObjectTask.isCancelled()) {
                        Log.i("BMT", "Task cancelled : " + ibmDataObjectTask.toString());
                    } else if (ibmDataObjectTask.isFaulted()) {
                        Log.i("BMT", "Task fault : " + ibmDataObjectTask.getError().getMessage());
                        throw ibmDataObjectTask.getError();
                    } else {
                        Log.i("BMT","Successfully deleted Item");
                        dataItemToDelete = dataItemToAdd;   // assign the last item added as this
                                                            // as this will be the next to be deleted
                        Intent intent = new Intent(OP_COMPLETE);
                        intent.putExtra("VALUE",DELETE_COMPLETE);
                        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                    }
                    return null;
                }
            });
        }
    }
}
