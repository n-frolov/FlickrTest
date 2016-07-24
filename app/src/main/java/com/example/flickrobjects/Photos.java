package com.example.flickrobjects;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Photos {

    @SerializedName("photo")
    private List<Photo> photo;

    @SerializedName("page")
    private int page;

    @SerializedName("pages")
    private int pages;

    public List<Photo> getPhoto() {
        return photo;
    }

    public int getPage() {
        return page;
    }

    public int getPages() {
        return pages;
    }

}
