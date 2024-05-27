package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class DeviceClasses {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DeviceInfo(
        var deviceId:String,
        var manufacturer:String,
        var model:String,
        var sdkVersion:Int,
        var clientVersion: String
    )
}