package com.cvshealth.cvs.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class Authenticator implements okhttp3.Authenticator{

    private final String token = "hypap-756f693d-3162-43d9-811e-11f36a957117";

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
        return response.request().newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
    }
}
