
package com.wenhui.coroutines.example.books;

import com.squareup.moshi.Json;

public class Epub {

    @Json(name = "isAvailable")
    private Boolean isAvailable;

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

}
