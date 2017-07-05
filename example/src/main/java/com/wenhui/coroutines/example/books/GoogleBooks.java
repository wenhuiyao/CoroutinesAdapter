
package com.wenhui.coroutines.example.books;

import com.squareup.moshi.Json;

import java.util.List;

public class GoogleBooks {

    @Json(name = "kind")
    private String kind;
    @Json(name = "totalItems")
    private Integer totalItems;
    @Json(name = "items")
    private List<Item> items = null;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

}
