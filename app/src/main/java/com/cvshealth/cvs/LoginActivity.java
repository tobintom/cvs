package com.cvshealth.cvs;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.cvshealth.cvs.service.HyprFIDO;
import com.cvshealth.cvs.util.Util;
import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private TextView mTextView;
    EditText userid,password;
    Button signin, passkey;
    Intent in;
    SharedPreferences sharedpreferences;
    String userName = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sharedpreferences = getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        TextView v = findViewById(R.id.toollangname);
        v.setOnClickListener(v1 -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        signin = (Button) findViewById(R.id.signin_b);
        passkey = (Button) findViewById(R.id.passkey_b);
        userid = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        userName = userid.getText().toString();
        signin.setOnClickListener(v1 -> {
            String user  = userid.getText().toString();
            String pass  = password.getText().toString();
            if(user==null || user.trim().length()==0 || pass==null || pass.trim().length()==0){
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error");
                alertDialog.setMessage("Please enter an ID and password");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }else{
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(MainActivity.user, user);
                editor.commit();
                in = new Intent(LoginActivity.this,MainActivity.class);
                in.putExtra("login","standard");
                startActivity(in);
            }
        });

        //PASSKEY LOGIN -
        passkey.setOnClickListener(v1 -> {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try  {
                        userName  = userid.getText().toString();
                        System.out.println("USER NAME " +userName);
                        passwordlesslogin(userName!=null?userName.trim():"");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        });
    }


    public void passwordlesslogin(String userName){
        try {
            //Get the Authentication Challenge
            String jsonData = HyprFIDO.getAuthenticationChallenge(userName);
            System.out.println("AUTH CHALLENGE RESPONSE" + jsonData);
            if(jsonData!=null && jsonData.trim().length()>0) {
                JSONObject obj = new JSONObject(jsonData);
                JSONArray arr = null;
                try {
                    arr = obj.getJSONArray("allowCredentials");
                }catch (JSONException e){}

                if((arr==null || (arr.length()==0)) && userName!=null && userName.trim().length()>0){
                    handleLoginError(new Exception("NOCREDERROR"));
                }else {
                    //Build Credential Payload
                    JSONObject jsobObj = new JSONObject();
                    jsobObj.put("rpId", obj.getString("rpId"));
                    jsobObj.put("userVerification",obj.getString("userVerification"));
                    jsobObj.put("timeout",obj.getInt("timeout"));
                    //GET CHALLENGE
                    //String ch = Base64.getEncoder().encodeToString(obj.getString("challenge").getBytes());

                    jsobObj.put("challenge", obj.getString("challenge"));
                    //GET ALLOW CREDS
                    if(arr !=null) {
                        jsobObj.put("allowCredentials",arr);
                    }

                    System.out.println("FINAL PAYLOAD " +jsobObj.toString());
                    //Start Authentication
                    CredentialManager credentialManager = CredentialManager.create(LoginActivity.this);

                    GetPublicKeyCredentialOption getPublicKeyCredentialOption =
                            new GetPublicKeyCredentialOption(jsobObj.toString());

                    GetCredentialRequest getCredRequest = new GetCredentialRequest.Builder()
                            .addCredentialOption(getPublicKeyCredentialOption)
                            .build();
                    //Perform Platform Authentication
                    credentialManager.getCredentialAsync(
                            getCredRequest,
                            LoginActivity.this,
                            null,
                            getMainExecutor(),
                            new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                                @Override
                                public void onResult(GetCredentialResponse result) {
                                    // Handle the successfully returned credential.
                                    Credential credential = result.getCredential();
                                    if (credential instanceof PublicKeyCredential) {
                                        String responseJson = ((PublicKeyCredential) credential)
                                                .getAuthenticationResponseJson();
                                        //HYPR SERVER AUTHENTICATION
                                        fidoAuthenticateToServer(responseJson);
                                    } else {
                                        System.out.println("UNexpected Credential Object Found");
                                    }
                                }

                                @Override
                                public void onError(GetCredentialException e) {
                                    handleLoginError(e);
                                }
                            });
                }
            }else{
                handleLoginError(new Exception("Error"));
            }

        }catch (Exception e){
            handleLoginError(e);
        }
    }

    public void fidoAuthenticateToServer(String payload){
        try{
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try  {
                        System.out.println("RESPONSE" + payload);
                        JSONObject jsobObj = new JSONObject(payload);

//                        JSONObject jchild = jsobObj.getJSONObject("response");
//                        String clientData = jchild.getString("clientDataJSON");
//                        //MANIPULATE THE CLIENTJSONDATA. HYPR DOES NOT UNDERSTAND ANDROID
//                        String sd = new String(Base64.getUrlDecoder().decode(clientData));
//                        System.out.println(sd);
//                        JSONObject clientJ = new JSONObject(sd);
//                        clientJ.remove("androidPackageName");
//                        clientJ.put("origin","https://thedigitalword.org");
//                        String newclient = clientJ.toString();
//                        System.out.println("clientDataJSON "+newclient);
//                        String p = Util.getBase64URLEncodedString(newclient);
//                        jchild.put("clientDataJSON", p);
//                        System.out.println("clientDataJSON "+p);

//                        String authenticatorData = jchild.getString("authenticatorData");
//                        jchild.put("authenticatorData",new String(Base64.getUrlEncoder().encode(authenticatorData.getBytes())));
//                        String signature = jchild.getString("signature");
//                        jchild.put("signature",new String(Base64.getUrlEncoder().encode(signature.getBytes())));
//                        String userHandle = jchild.getString("userHandle");
//                        jchild.put("userHandle",new String(Base64.getUrlEncoder().encode(userHandle.getBytes())));

                      //  jsobObj.put("response",jchild);

                        System.out.println("FINAL PAYLOAD : " +jsobObj.toString());
                        //FIDO API CALL
                        String jsonData = HyprFIDO.getAuthentication(jsobObj.toString());
                        System.out.println(jsonData);
                        JSONObject obj = new JSONObject(jsonData!=null?jsonData:"");
                        if(obj.get("status")!=null && obj.get("status").equals("ok")) {
                            handleSuccessfulLogin(obj);
                        }else{
                            handleLoginError(new Exception("Unknown Exception"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        handleLoginError(e);
                    }
                }
            });
            thread.start();

        }catch (Exception e){
            handleLoginError(e);
        }
    }

    public void handleSuccessfulLogin(JSONObject obj){
        SharedPreferences.Editor editor = sharedpreferences.edit();
        String user = "";
        try {
            user = obj.getString("username");
        }catch (Exception e1) {//Handle error separately
        }
        editor.putString(MainActivity.user, user);
        editor.commit();
        in = new Intent(LoginActivity.this,MainActivity.class);
        in.putExtra("login","passkey");
        startActivity(in);
    }

    public void handleLoginError(Exception e){

        LoginActivity.this.runOnUiThread(() -> {
        e.printStackTrace();
        android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create();
        if(e instanceof GetCredentialCancellationException){
            alertDialog.setTitle("Info");
            alertDialog.setIcon(R.drawable.ic_baseline_fingerprint_24);
            alertDialog.setMessage("You have cancelled the passwordless login flow");

        }else
            if(e instanceof NoCredentialException){
                alertDialog.setTitle("Info");
                alertDialog.setIcon(R.drawable.ic_baseline_fingerprint_24);
                alertDialog.setMessage("There were no passkeys found for login. Please login with your user id and password to register a passkey for this device.");
            }else
        if(e.getMessage()!=null && e.getMessage().contains("NOCREDERROR")){
            alertDialog.setTitle("Info");
            alertDialog.setIcon(R.drawable.ic_baseline_fingerprint_24);
            alertDialog.setMessage("You have not been setup for passwordless login. Please login with your password and enroll for passwordless login");

        }else {
            alertDialog.setTitle("Error");
            alertDialog.setIcon(R.drawable.baseline_error_24);
            alertDialog.setMessage("There was an error during passwordless login.");
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




}