package com.finnvek.squaretool.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.ThemePreference
import com.finnvek.squaretool.ui.navigation.TopLevelDestination
import com.finnvek.squaretool.ui.onboarding.OnboardingScreen
import com.finnvek.squaretool.ui.theme.LocalReduceMotion
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import kotlinx.coroutines.launch

@Suppress("kotlin:S3776") // App startup branches are mutually exclusive declarative states.
@Composable
fun SquareToolApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val settings: AppSettings? by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = null,
    )
    var initialActiveProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var postOnboardingRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var onboardingActionRunning by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val failureMessage = stringResource(R.string.onboarding_action_failed)
    val darkTheme =
        when (settings?.theme ?: ThemePreference.SYSTEM) {
            ThemePreference.SYSTEM -> isSystemInDarkTheme()
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }

    SquareToolTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalReduceMotion provides (settings?.reduceMotion == true),
        ) {
            Surface(modifier = modifier.fillMaxSize()) {
                when {
                    settings == null -> {
                        Box(Modifier.fillMaxSize())
                    }

                    !settings!!.onboardingCompleted -> {
                        Box(Modifier.fillMaxSize()) {
                            OnboardingScreen(
                                onCreateProject = {
                                    if (!onboardingActionRunning) {
                                        onboardingActionRunning = true
                                        postOnboardingRoute = AppRoute.projectEditor()
                                        scope.launch {
                                            runCatching {
                                                container.settingsRepository.setOnboardingCompleted(true)
                                            }.onFailure {
                                                onboardingActionRunning = false
                                                snackbarHostState.showSnackbar(failureMessage)
                                            }
                                        }
                                    }
                                },
                                onExploreSample = {
                                    if (!onboardingActionRunning) {
                                        onboardingActionRunning = true
                                        scope.launch {
                                            runCatching {
                                                val projectId = container.createSampleProject()
                                                initialActiveProjectId = projectId
                                                container.settingsRepository.setLastSelectedNavigationDestination(
                                                    TopLevelDestination.Planner.route,
                                                )
                                                container.settingsRepository.setOnboardingCompleted(true)
                                            }.onFailure {
                                                onboardingActionRunning = false
                                                snackbarHostState.showSnackbar(failureMessage)
                                            }
                                        }
                                    }
                                },
                            )
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }

                    else -> {
                        SquareToolNavigationHost(
                            container = container,
                            settings = settings!!,
                            initialActiveProjectId = initialActiveProjectId,
                            postOnboardingRoute = postOnboardingRoute,
                            onConsumePostOnboardingRoute = { postOnboardingRoute = null },
                        )
                    }
                }
            }
        }
    }
}
