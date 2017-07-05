
package com.wenhui.coroutines.example.books;

import java.util.List;
import com.squareup.moshi.Json;

public class VolumeInfo {

    @Json(name = "title")
    private String title;
    @Json(name = "authors")
    private List<String> authors = null;
    @Json(name = "publisher")
    private String publisher;
    @Json(name = "publishedDate")
    private String publishedDate;
    @Json(name = "description")
    private String description;
    @Json(name = "industryIdentifiers")
    private List<IndustryIdentifier> industryIdentifiers = null;
    @Json(name = "readingModes")
    private ReadingModes readingModes;
    @Json(name = "pageCount")
    private Integer pageCount;
    @Json(name = "printType")
    private String printType;
    @Json(name = "categories")
    private List<String> categories = null;
    @Json(name = "averageRating")
    private Double averageRating;
    @Json(name = "ratingsCount")
    private Integer ratingsCount;
    @Json(name = "maturityRating")
    private String maturityRating;
    @Json(name = "allowAnonLogging")
    private Boolean allowAnonLogging;
    @Json(name = "contentVersion")
    private String contentVersion;
    @Json(name = "imageLinks")
    private ImageLinks imageLinks;
    @Json(name = "language")
    private String language;
    @Json(name = "previewLink")
    private String previewLink;
    @Json(name = "infoLink")
    private String infoLink;
    @Json(name = "canonicalVolumeLink")
    private String canonicalVolumeLink;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<IndustryIdentifier> getIndustryIdentifiers() {
        return industryIdentifiers;
    }

    public void setIndustryIdentifiers(List<IndustryIdentifier> industryIdentifiers) {
        this.industryIdentifiers = industryIdentifiers;
    }

    public ReadingModes getReadingModes() {
        return readingModes;
    }

    public void setReadingModes(ReadingModes readingModes) {
        this.readingModes = readingModes;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public String getPrintType() {
        return printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(Integer ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public String getMaturityRating() {
        return maturityRating;
    }

    public void setMaturityRating(String maturityRating) {
        this.maturityRating = maturityRating;
    }

    public Boolean getAllowAnonLogging() {
        return allowAnonLogging;
    }

    public void setAllowAnonLogging(Boolean allowAnonLogging) {
        this.allowAnonLogging = allowAnonLogging;
    }

    public String getContentVersion() {
        return contentVersion;
    }

    public void setContentVersion(String contentVersion) {
        this.contentVersion = contentVersion;
    }

    public ImageLinks getImageLinks() {
        return imageLinks;
    }

    public void setImageLinks(ImageLinks imageLinks) {
        this.imageLinks = imageLinks;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPreviewLink() {
        return previewLink;
    }

    public void setPreviewLink(String previewLink) {
        this.previewLink = previewLink;
    }

    public String getInfoLink() {
        return infoLink;
    }

    public void setInfoLink(String infoLink) {
        this.infoLink = infoLink;
    }

    public String getCanonicalVolumeLink() {
        return canonicalVolumeLink;
    }

    public void setCanonicalVolumeLink(String canonicalVolumeLink) {
        this.canonicalVolumeLink = canonicalVolumeLink;
    }

}
