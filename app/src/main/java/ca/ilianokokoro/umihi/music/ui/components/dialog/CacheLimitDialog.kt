package ca.ilianokokoro.umihi.music.ui.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ca.ilianokokoro.umihi.music.R

@Composable
fun CacheLimitDialog(
    selectedOption: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    val options = listOf("500 MB", "1 GB", "2 GB", stringResource(R.string.unlimited))
    
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.cache_limit_title)) },
        text = {
            Column {
                options.forEachIndexed { index, label ->
                    TextButton(
                        onClick = {
                            onSelect(index)
                        }
                    ) {
                        Text(
                            if (index == selectedOption) "✓ $label" else label
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
