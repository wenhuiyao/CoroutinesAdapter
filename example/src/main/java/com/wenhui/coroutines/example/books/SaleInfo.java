
package com.wenhui.coroutines.example.books;

import com.squareup.moshi.Json;

public class SaleInfo {

    @Json(name = "country")
    private String country;
    @Json(name = "saleability")
    private String saleability;
    @Json(name = "isEbook")
    private Boolean isEbook;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSaleability() {
        return saleability;
    }

    public void setSaleability(String saleability) {
        this.saleability = saleability;
    }

    public Boolean getIsEbook() {
        return isEbook;
    }

    public void setIsEbook(Boolean isEbook) {
        this.isEbook = isEbook;
    }

}
