package com.finnvek.squaretool.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.squaretool.R
import com.finnvek.squaretool.ui.theme.SquareToolSpacing

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val icon: ImageVector,
)

private val onboardingPages =
    listOf(
        OnboardingPage(R.string.design_your_squares, R.string.design_your_squares_body, Icons.Outlined.Palette),
        OnboardingPage(R.string.build_your_blanket, R.string.build_your_blanket_body, Icons.Outlined.GridView),
        OnboardingPage(R.string.keep_a_clear_project_plan, R.string.keep_a_clear_project_plan_body, Icons.Outlined.AutoGraph),
    )

@Composable
fun OnboardingScreen(
    onCreateProject: () -> Unit,
    onExploreSample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val isChoice = page == onboardingPages.size

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SquareToolSpacing.Large, vertical = SquareToolSpacing.Section),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(152.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isChoice) Icons.Outlined.Yard else onboardingPages[page].icon,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(SquareToolSpacing.Section))
            Text(
                text = stringResource(if (isChoice) R.string.ready_to_begin else onboardingPages[page].title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(SquareToolSpacing.Medium))
            Text(
                text = stringResource(if (isChoice) R.string.ready_to_begin_body else onboardingPages[page].body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isChoice) {
                Spacer(Modifier.height(SquareToolSpacing.Large))
                Text(
                    text = stringResource(R.string.page_of_pages, page + 1, onboardingPages.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(SquareToolSpacing.ExtraLarge))
        if (isChoice) {
            Button(
                onClick = onCreateProject,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("create_project_choice"),
            ) { Text(stringResource(R.string.create_new_project)) }
            Spacer(Modifier.height(SquareToolSpacing.Medium))
            OutlinedButton(
                onClick = onExploreSample,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("sample_project_choice"),
            ) { Text(stringResource(R.string.explore_sample_project)) }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
            ) {
                OutlinedButton(
                    onClick = { if (page == 0) page = onboardingPages.size else page-- },
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text(stringResource(if (page == 0) R.string.skip else R.string.back)) }
                Button(
                    onClick = { page++ },
                    modifier = Modifier.weight(1f).height(56.dp).testTag("onboarding_next"),
                    colors = ButtonDefaults.buttonColors(),
                ) { Text(stringResource(R.string.next)) }
            }
        }
    }
}
