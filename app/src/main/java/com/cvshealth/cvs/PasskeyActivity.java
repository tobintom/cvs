package com.cvshealth.cvs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.CreateCredentialCancellationException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialInterruptedException;
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException;
import androidx.credentials.exceptions.CreateCredentialUnknownException;
import androidx.credentials.exceptions.CreateCustomCredentialException;
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cvshealth.cvs.model.Passkey;
import com.cvshealth.cvs.service.HyprFIDO;
import com.google.android.gms.fido.Fido;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class PasskeyActivity extends AppCompatActivity {

    public static final String PREFERENCES = "CVSPref" ;
    public static final String user = "nameKey";
    public static final String pwe = "pwe";
    SharedPreferences sharedpreferences;

    private MaterialAlertDialogBuilder materialDialogBuilder;
    private View customAlertView;
    private ProgressDialog ringProgressDialog;
    ArrayList<Passkey> list = new ArrayList();
    PasskeyAdapter adapter = new PasskeyAdapter(this, list);
    RecyclerView passkeys = null;
    TextView vt = null;
    Button passkey = null;
    String username = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_passkey);
            sharedpreferences = getSharedPreferences(PasskeyActivity.PREFERENCES, Context.MODE_PRIVATE);
            username = sharedpreferences.getString(PasskeyActivity.user, null);
            TextView v = findViewById(R.id.toollangname1);
            TextView vt = findViewById(R.id.vt);
            passkeys = findViewById(R.id.idpasskeys);
            passkey = (Button) findViewById(R.id.genpass);
            v.setOnClickListener(v1 -> {
                startActivity(new Intent(this, MainActivity.class));
            });

            materialDialogBuilder = new MaterialAlertDialogBuilder(this);
            BottomNavigationView mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigationView);
            mBottomNavigationView.getMenu().findItem(R.id.account).setChecked(true);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            passkeys.setLayoutManager(linearLayoutManager);
            passkeys.setAdapter(adapter);

            Thread thread1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        populatePasskeys(username);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread1.start();

            passkey.setOnClickListener(v3 -> {
                    ringProgressDialog = ProgressDialog.show(this, "Please wait ...", "Creating Passkey ...", true);
                    ringProgressDialog.setCancelable(true);
                    ringProgressDialog.setIcon(R.drawable.ic_baseline_fingerprint_24);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                enrollUserForPasswordless(username);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                });

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void populatePasskeys(String username){
        try {
            vt = findViewById(R.id.vt);
            String payload = HyprFIDO.getUserPasskeys(username);
            JSONArray arr = new JSONArray(payload);
            if(arr!=null && arr.length()>0) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    System.out.println(o.toString());
                    list.add(new Passkey(o.getString("friendlyName"), "Created on " + new Date(Long.parseLong(o.getString("createDate"))).toString(), "Last used " + new Date(Long.parseLong(o.getString("lastUsedTime"))).toString(), o.getString("keyId")));
                }

            }else{
                vt.setText("You have no passkeys registered");
            }
        }catch (Exception e){e.printStackTrace();

        }

    }

    public void enrollUserForPasswordless(String userName){
        try {
            //Get the Registration Challenge
            String jsonData = HyprFIDO.getRegistrationChallenge(userName);
            System.out.println(jsonData);
            if(jsonData!=null && jsonData.trim().length()>0) {
                JSONObject obj = new JSONObject(jsonData);

                System.out.println("CHALLENGE RESPONSE" + obj.toString());
                //Remove unwanted stuff
                obj.remove("ok");
                obj.remove("status");
                obj.remove("errorMessage");
                obj.remove("extensions");

                //Format required stuff
               // obj.put("challenge", Util.getBase64URLEncodedString(obj.getString("challenge")));
               // JSONObject user = obj.getJSONObject("user");
              //  user.put("id",Util.getBase64URLEncodedString(user.getString("id")));

//                JSONArray arr = obj.getJSONArray("excludeCredentials");
//                for(int i=0;i< arr.length();i++){
//                    JSONObject o = arr.getJSONObject(i);
//                    List<String> list = new ArrayList<String>();
//                    list.add("internal");
//                   // o.put("id", Util.getBase64URLEncodedString(o.getString("id")));
//                    o.put("transports",new JSONArray(list));
//                }
               // obj.put("excludeCredentials",arr);
                System.out.println("FINAL PAYLOAD " + obj.toString());
                CredentialManager credentialManager = CredentialManager.create(PasskeyActivity.this);

//                JSONObject clientData = new JSONObject();
//                clientData.put("type","webauthn.create");
//                clientData.put("challenge",obj.getString("challenge"));
//                clientData.put("origin","http://localhost:4200");
//
//                MessageDigest digest = MessageDigest.getInstance("SHA-256");
//                byte[] encodedhash = digest.digest(
//                        clientData.toString().getBytes(StandardCharsets.UTF_8));
//                String clientDataHash = Util.bytesToHex(encodedhash);


                CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest =
                        new CreatePublicKeyCredentialRequest(obj.toString());
                credentialManager.
                        createCredentialAsync(
                        createPublicKeyCredentialRequest,
                        PasskeyActivity.this,
                        null,
                        getMainExecutor(),
                        new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {

                    @Override
                    public void onResult(CreateCredentialResponse result) {
                        try {
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try  {
                                        String payload = result.getData().getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON");
                                        System.out.println("RESPONSE" + payload);
                                        JSONObject jsobObj = new JSONObject(payload);
                                        String rawId = jsobObj.getString("rawId");
                                       // jsobObj.put("rawId",Util.getBase64URLEncodedString(rawId));

                                        JSONObject jchild = jsobObj.getJSONObject("response");
                                        String clientData = jchild.getString("clientDataJSON");
                                        String attestationObject = jchild.getString("attestationObject");
                                       // jchild.put("attestationObject",(Util.getBase64URLEncodedString(attestationObject)));
                                       jchild.remove("transports");
                                       jsobObj.remove("authenticatorAttachment");

                                        //MANIPULATE THE CLIENTJSONDATA. HYPR DOES NOT UNDERSTAND ANDROID
//                                        String sd = new String(Base64.getUrlDecoder().decode(clientData));
//                                        System.out.println(sd);
//                                        JSONObject clientJ = new JSONObject(sd);
//                                         clientJ.remove("androidPackageName");
//                                        clientJ.put("origin","https://thedigitalword.org");
//                                        String newclient = clientJ.toString();
//                                        System.out.println("clientDataJSON "+newclient);
//                                        String p =  (Util.getBase64URLEncodedString(newclient));
//                                        jchild.put("clientDataJSON", p);
//                                        System.out.println("clientDataJSON "+p);

                                        System.out.println("FINAL PAYLOAD : " +jsobObj.toString());

                                        String jsonData = HyprFIDO.getRegistration(jsobObj.toString());
                                        System.out.println(jsonData);
                                        JSONObject obj = new JSONObject(jsonData!=null?jsonData:"");
                                        if(obj.get("status")!=null && obj.get("status").equals("ok")) {
                                            handleSuccessfulEnrollment(result);
                                        }else{
                                            handleEnrollmentError(new Exception("Unknown Exception"));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        handleEnrollmentError(e);
                                    }
                                }
                            });
                            thread.start();
                        }catch(Exception e){
                            e.printStackTrace();
                            handleEnrollmentError(e);
                        }
                    }

                    @Override
                    public void onError(CreateCredentialException e) {
                        if (e instanceof CreatePublicKeyCredentialDomException) {handleEnrollmentError(e);
                            // Handle the passkey DOM errors thrown according to the
                            // WebAuthn spec.
                            //handlePasskeyError(((CreatePublicKeyCredentialDomException)e).getDomError());
                        } else if (e instanceof CreateCredentialCancellationException) {handleEnrollmentError(e);
                            // The user intentionally canceled the operation and chose not
                            // to register the credential.
                        } else if (e instanceof CreateCredentialInterruptedException) {handleEnrollmentError(e);
                            // Retry-able error. Consider retrying the call.
                        } else if (e instanceof CreateCredentialProviderConfigurationException) {handleEnrollmentError(e);
                            // Your app is missing the provider configuration dependency.
                            // Most likely, you're missing the
                            // "credentials-play-services-auth" module.
                        } else if (e instanceof CreateCredentialUnknownException) {handleEnrollmentError(e);
                        } else if (e instanceof CreateCustomCredentialException) {handleEnrollmentError(e);
                            // You have encountered an error from a 3rd-party SDK. If
                            // you make the API call with a request object that's a
                            // subclass of
                            // CreateCustomCredentialRequest using a 3rd-party SDK,
                            // then you should check for any custom exception type
                            // constants within that SDK to match with e.type.
                            // Otherwise, drop or log the exception.
                        } else {handleEnrollmentError(e);
                        }
                    }
                }
            );

            }
            ringProgressDialog.dismiss();
        }catch(Exception e){
            handleEnrollmentError(e);
        }
    }

    public void handleSuccessfulEnrollment(CreateCredentialResponse result){
            populatePasskeys(username);
            PasskeyActivity.this.runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
            ringProgressDialog.dismiss();
            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(PasskeyActivity.this).create();
            alertDialog.setTitle("Success");
            alertDialog.setIcon(R.drawable.ic_baseline_fingerprint_24);
            alertDialog.setMessage("You have successfully enrolled for passwordless login");
            alertDialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        });

    }

    public void handleEnrollmentError(Exception e){
        PasskeyActivity.this.runOnUiThread(() -> {
            ringProgressDialog.dismiss();
            e.printStackTrace();
            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create();
            if (e.getMessage() != null && e.getMessage().contains("One of the excluded credentials exists on the local device")) {
                alertDialog.setTitle("Success");
                alertDialog.setIcon(R.drawable.ic_baseline_fingerprint_24);
                alertDialog.setMessage("You already have a passkey on this device.");
                HashSet ex = (HashSet) sharedpreferences.getStringSet(pwe, new HashSet<String>());
                ex.add(sharedpreferences.getString(user, ""));
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putStringSet(PasskeyActivity.pwe, ex);
                editor.commit();
            } else {
                alertDialog.setTitle("Error");
                alertDialog.setIcon(R.drawable.baseline_error_24);
                alertDialog.setMessage("There was an error during passwordless enrollment.");
            }
            alertDialog.setButton(android.app.AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        });
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
}