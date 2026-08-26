package com.finnvek.squaretool.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.squaretool.ui.theme.SquareToolSpacing

@Composable
fun SquareToolEditorActions(
    @StringRes cancelLabel: Int,
    @StringRes saveLabel: Int,
    saveTestTag: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
            Text(stringResource(cancelLabel))
        }
        Button(onClick = onSave, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag(saveTestTag)) {
            Text(stringResource(saveLabel))
        }
    }
}
