package com.example.android.binfoshare;

/**
 * Created by S on 16/02/2017.
 */
public class Album {
    private String name;
    private int thumbnail;

    public Album() {
    }

    public Album(String name, int thumbnail) {
        this.name = name;
        this.thumbnail = thumbnail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public int getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(int thumbnail) {
        this.thumbnail = thumbnail;
    }
}
