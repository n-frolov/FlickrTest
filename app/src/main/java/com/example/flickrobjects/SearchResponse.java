package com.example.flickrobjects;

import com.google.gson.annotations.SerializedName;

public class SearchResponse {

    @SerializedName("photos")
    private Photos photos;

    public Photos getPhotos() {
        return photos;
    }

}
