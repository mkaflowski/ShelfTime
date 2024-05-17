package kaf.audiobookshelfwearos.app.data.room

import androidx.room.TypeConverter
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kaf.audiobookshelfwearos.app.data.CollapsedSeries
import kaf.audiobookshelfwearos.app.data.Media

class Converters {
    private val objectMapper = ObjectMapper().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())

    @TypeConverter
    fun fromMedia(media: Media): String {
        return objectMapper.writeValueAsString(media)
    }

    @TypeConverter
    fun toMedia(mediaString: String): Media {
        return objectMapper.readValue(mediaString)
    }

    @TypeConverter
    fun fromCollapsedSeries(series: CollapsedSeries): String {
        return objectMapper.writeValueAsString(series)
    }

    @TypeConverter
    fun toCollapsedSeries(seriesString: String): CollapsedSeries {
        return objectMapper.readValue(seriesString)
    }
}