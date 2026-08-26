package com.finnvek.squaretool.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.finnvek.squaretool.BuildConfig
import com.finnvek.squaretool.R
import com.finnvek.squaretool.ui.theme.SquareToolSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            // CPD-OFF
            TopAppBar(
                title = { Text(stringResource(R.string.about_and_privacy)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(SquareToolSpacing.Standard),
        ) {
            // CPD-ON
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = SquareToolSpacing.Section),
            )
            Text(
                stringResource(R.string.visual_planning_disclaimer),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = SquareToolSpacing.Standard),
            )
            Text(
                stringResource(
                    R.string.data_and_privacy,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .padding(
                            top = SquareToolSpacing.Section,
                        ).semantics {
                            heading()
                        },
            )
            Text(
                stringResource(R.string.privacy_statement),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = SquareToolSpacing.Small),
            )
            Text(
                stringResource(
                    R.string.open_source_licenses,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .padding(
                            top = SquareToolSpacing.Section,
                        ).semantics {
                            heading()
                        },
            )
            Text(
                stringResource(R.string.license_summary),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = SquareToolSpacing.Small),
            )
        }
    }
}
