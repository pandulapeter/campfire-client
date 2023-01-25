package com.pandulapeter.campfire.presentation.android.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.campfire.shared.ui.CampfireViewModelStateHolder
import com.pandulapeter.campfire.shared.ui.screenComponents.settings.SettingsContentList

@Composable
internal fun SettingsScreenAndroid(
    modifier: Modifier = Modifier,
    stateHolder: CampfireViewModelStateHolder
) = SettingsContentList(
    modifier = modifier,
    uiStrings = stateHolder.uiStrings.value,
    databases = stateHolder.databases.value,
    onDatabaseEnabledChanged = stateHolder::onDatabaseEnabledChanged,
    selectedUiMode = stateHolder.userPreferences.value?.uiMode,
    onSelectedUiModeChanged = stateHolder::onUiModeChanged
)