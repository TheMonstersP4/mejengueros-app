package example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LaunchDto(
    @SerialName("flight_number") val flightNumber: Int,
    @SerialName("name") val name: String,
    @SerialName("date_utc") val dateUtc: String,
    @SerialName("success") val success: Boolean? = null,
)

data class Launch(
    val flightNumber: Int,
    val missionName: String,
    val launchDateUtc: String,
    val launchSuccess: Boolean?,
) {
    val launchYear: Int get() = launchDateUtc.take(4).toIntOrNull() ?: 0
}

fun LaunchDto.toDomain(): Launch =
    Launch(
        flightNumber = flightNumber,
        missionName = name,
        launchDateUtc = dateUtc,
        launchSuccess = success,
    )

fun launchFromRow(
    flightNumber: Long,
    missionName: String,
    launchDateUtc: String,
    launchSuccess: Boolean?,
): Launch =
    Launch(
        flightNumber = flightNumber.toInt(),
        missionName = missionName,
        launchDateUtc = launchDateUtc,
        launchSuccess = launchSuccess,
    )
