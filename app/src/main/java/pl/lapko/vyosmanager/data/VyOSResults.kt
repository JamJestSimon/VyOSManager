package pl.lapko.vyosmanager.data

import com.fasterxml.jackson.databind.JsonNode

data class VyOSResults(
    var success : Boolean,
    var data : JsonNode?,
    var error : String?
)
