package com.wenhui.coroutines.example;

import com.wenhui.coroutines.example.books.GoogleBooks;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitService {

    @GET("https://www.googleapis.com/books/v1/volumes")
    Call<GoogleBooks> getGoogleBook(@Query("q") String q);

}
