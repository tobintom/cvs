package com.cvshealth.cvs.model;

import com.cvshealth.cvs.PasskeyActivity;

public class Passkey {

    private String commonName;
    private String created;
    private String last;
    private String credid;

    public Passkey(String commonName, String created, String last, String credid){
        this.commonName = commonName;
        this.created =created;
        this.last =last;
        this.credid=credid;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getCredid() {
        return credid;
    }

    public void setCredid(String credid) {
        this.credid = credid;
    }
}
