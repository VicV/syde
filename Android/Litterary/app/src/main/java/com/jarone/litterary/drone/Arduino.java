package com.jarone.litterary.drone;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Adam on 2016-03-03.
 */
interface Arduino {
    @GET("/{command}")
    Call<ResponseBody> commandRequest(@Path("command") String command);
}