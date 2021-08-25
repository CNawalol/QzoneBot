package cn.awalol.qzoneBot.bean.qqMusic

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Album(
    @JsonProperty("mid")
    val mid: String
)