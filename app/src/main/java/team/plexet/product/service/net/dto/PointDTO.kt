package team.plexet.product.service.net.dto

class PointDTO {
    var sensor: Int = 0
    var period: Long = 0
    override fun toString(): String {
        return "ReplyDTO(sensor=$sensor, period=$period)"
    }
}