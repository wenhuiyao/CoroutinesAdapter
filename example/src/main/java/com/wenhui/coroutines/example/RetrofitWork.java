package com.wenhui.coroutines.example;

import com.wenhui.coroutines.BaseAction;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Create custom background work
 */
public class RetrofitWork<T> extends BaseAction<T> {

    private Call<T> call;

    public RetrofitWork(Call<T> call) {
        this.call = call;
    }

    @Override
    public T run() throws Exception {
        final Response<T> response = call.execute();
        if (response.isSuccessful()) {
            return response.body();
        }

        throw new RetrofitError(response.code(),
                response.message(),
                response.headers(),
                response.errorBody());
    }

    public static class RetrofitError extends Exception {

        public final int code;
        public final String message;
        public final Headers headers;
        public final ResponseBody errorBody;

        private RetrofitError(int code,
                String message,
                Headers headers,
                ResponseBody errorBody) {

            this.code = code;
            this.message = message;
            this.headers = headers;
            this.errorBody = errorBody;
        }

    }
}
