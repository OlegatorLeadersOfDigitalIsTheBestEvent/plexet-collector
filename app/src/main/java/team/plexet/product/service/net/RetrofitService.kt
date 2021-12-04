package team.plexet.product.service.net

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitService {
    companion object{
        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://backend.plexet.team/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        val instance = retrofit.create(RestApi::class.java)
    }
}