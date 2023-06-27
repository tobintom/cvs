package com.cvshealth.cvs.service;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HyprFIDO {

    private static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static String URL_BASE = "https://cvshealthdemo.gethypr.com";

    public static String getRegistrationChallenge(String userName)throws Exception{
        OkHttpClient client = Service.getServiceClient().newBuilder()
                .authenticator(new Authenticator())
                .build();
        //Build JSON Object
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", userName);
            jsonObject.put("displayName", userName);
            jsonObject.put("attestation", "none");
            JSONObject authSelector = new JSONObject();
            authSelector.put("authenticatorAttachment","platform");
            authSelector.put("userVerification","required");
            authSelector.put("residentKey","preferred");
            authSelector.put("requireResidentKey","true");
            jsonObject.put("authenticatorSelection",authSelector);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Build URL
        RequestBody body = RequestBody.create(JSON, jsonObject.toString());
        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL_BASE + "/rp/api/versioned/fido2/attestation/options")
                .newBuilder();
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

    public static String getRegistration(String jsonString)throws Exception{
        OkHttpClient client = Service.getServiceClient().newBuilder()
                .authenticator(new Authenticator())
                .build();
        //Build URL
        RequestBody body = RequestBody.create(JSON, jsonString);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL_BASE + "/rp/api/versioned/fido2/attestation/result")
                .newBuilder();
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .post(body)
                .build();
        Response response = client.newCall(request).execute();


        return response.body().string();
    }

    public static String getAuthenticationChallenge(String userName)throws Exception{
        OkHttpClient client = Service.getServiceClient().newBuilder()
                .authenticator(new Authenticator())
                .build();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", userName);
            jsonObject.put("userVerification", "required");
            JSONObject authSelector = new JSONObject();
            authSelector.put("userVerification","required");
            authSelector.put("residentKey","preferred");
            jsonObject.put("authenticatorSelection",authSelector);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Build URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL_BASE + "/rp/api/versioned/fido2/assertion/options").newBuilder();
        RequestBody body = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

    public static String getAuthentication(String jsonString)throws Exception{
        OkHttpClient client = Service.getServiceClient().newBuilder()
                .authenticator(new Authenticator())
                .build();
        //Build URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL_BASE + "/rp/api/versioned/fido2/assertion/result").newBuilder();
        RequestBody body = RequestBody.create(JSON, jsonString);
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

    public static String getUserPasskeys(String username)throws Exception{
        OkHttpClient client = Service.getServiceClient().newBuilder()
                .authenticator(new Authenticator())
                .build();
        //Build URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL_BASE + "/rp/api/versioned/fido2/user?username="+username).newBuilder();
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

    public static String deleteUserPasskey(String username, String keyid)throws Exception{
        OkHttpClient client = Service.getServiceClient().newBuilder()
                .authenticator(new Authenticator())
                .build();
        //Build URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL_BASE + "/rp/api/versioned/fido2/user?username="+username+"&keyId="+keyid).newBuilder();
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .delete()
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }
}
