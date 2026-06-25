package example.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun LaunchStatusText(isSuccessful: Boolean?) {
    val color =
        when (isSuccessful) {
            true -> AppTheme.status.success
            false -> AppTheme.status.failure
            null -> AppTheme.status.unknown
        }

    Text(
        text =
            when (isSuccessful) {
                true -> "Successful"
                false -> "Unsuccessful"
                null -> "Unknown"
            },
        color = color,
        style = MaterialTheme.typography.bodyMedium,
    )
}
