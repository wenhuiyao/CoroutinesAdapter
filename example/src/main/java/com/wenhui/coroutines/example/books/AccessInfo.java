
package com.wenhui.coroutines.example.books;

import com.squareup.moshi.Json;

public class AccessInfo {

    @Json(name = "country")
    private String country;
    @Json(name = "viewability")
    private String viewability;
    @Json(name = "embeddable")
    private Boolean embeddable;
    @Json(name = "publicDomain")
    private Boolean publicDomain;
    @Json(name = "textToSpeechPermission")
    private String textToSpeechPermission;
    @Json(name = "epub")
    private Epub epub;
    @Json(name = "pdf")
    private Pdf pdf;
    @Json(name = "webReaderLink")
    private String webReaderLink;
    @Json(name = "accessViewStatus")
    private String accessViewStatus;
    @Json(name = "quoteSharingAllowed")
    private Boolean quoteSharingAllowed;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getViewability() {
        return viewability;
    }

    public void setViewability(String viewability) {
        this.viewability = viewability;
    }

    public Boolean getEmbeddable() {
        return embeddable;
    }

    public void setEmbeddable(Boolean embeddable) {
        this.embeddable = embeddable;
    }

    public Boolean getPublicDomain() {
        return publicDomain;
    }

    public void setPublicDomain(Boolean publicDomain) {
        this.publicDomain = publicDomain;
    }

    public String getTextToSpeechPermission() {
        return textToSpeechPermission;
    }

    public void setTextToSpeechPermission(String textToSpeechPermission) {
        this.textToSpeechPermission = textToSpeechPermission;
    }

    public Epub getEpub() {
        return epub;
    }

    public void setEpub(Epub epub) {
        this.epub = epub;
    }

    public Pdf getPdf() {
        return pdf;
    }

    public void setPdf(Pdf pdf) {
        this.pdf = pdf;
    }

    public String getWebReaderLink() {
        return webReaderLink;
    }

    public void setWebReaderLink(String webReaderLink) {
        this.webReaderLink = webReaderLink;
    }

    public String getAccessViewStatus() {
        return accessViewStatus;
    }

    public void setAccessViewStatus(String accessViewStatus) {
        this.accessViewStatus = accessViewStatus;
    }

    public Boolean getQuoteSharingAllowed() {
        return quoteSharingAllowed;
    }

    public void setQuoteSharingAllowed(Boolean quoteSharingAllowed) {
        this.quoteSharingAllowed = quoteSharingAllowed;
    }

}
