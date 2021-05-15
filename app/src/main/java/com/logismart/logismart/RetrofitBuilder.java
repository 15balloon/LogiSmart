package com.logismart.logismart;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitBuilder {
    static Retrofit getRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(RetrofitService.baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
