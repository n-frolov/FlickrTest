package com.example.flickrobjects;

import com.google.gson.annotations.SerializedName;

// This class is reused as both Flickr API response and DB storage. It's a pretty common practice,
// but can lead to issues is API response and DB storage divert a lot and use very different data.
// In this case it would make sense to refactor it into two separate classes.

public class Photo {

    @SerializedName("id")
    private String id;

    @SerializedName("owner")
    private String owner;

    @SerializedName("secret")
    private String secret;

    @SerializedName("server")
    private String server;

    @SerializedName("farm")
    private String farm;

    @SerializedName("title")
    private String title;


    public Photo(String id, String owner, String secret, String server, String farm, String title) {
        this.id = id;
        this.owner = owner;
        this.secret = secret;
        this.server = server;
        this.farm = farm;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getSecret() {
        return secret;
    }

    public String getServer() {
        return server;
    }

    public String getFarm() {
        return farm;
    }

    public String getTitle() {
        return title;
    }

}
