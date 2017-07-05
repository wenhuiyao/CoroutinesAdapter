
package com.wenhui.coroutines.example.books;

import com.squareup.moshi.Json;

public class ReadingModes {

    @Json(name = "text")
    private Boolean text;
    @Json(name = "image")
    private Boolean image;

    public Boolean getText() {
        return text;
    }

    public void setText(Boolean text) {
        this.text = text;
    }

    public Boolean getImage() {
        return image;
    }

    public void setImage(Boolean image) {
        this.image = image;
    }

}
