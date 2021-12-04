package team.plexet.product.service.net

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import team.plexet.product.service.net.dto.DataDTO
import team.plexet.product.service.net.dto.ReplyDTO

interface RestApi {
    @POST("/public/api/point/worker")
    fun postData(@Body data: DataDTO): Call<ReplyDTO>
}