package com.logismart.logismart;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface RetrofitService {
    String baseURL = "http://logismart.cafe24.com/";
    @FormUrlEncoded
    @POST("send_driver_info.jsp")
    Call<JsonObject> save_info(@Field("name") String name,
                               @Field("birth") String birth,
                               @Field("phone") String phone);
}
