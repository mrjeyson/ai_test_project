package com.example.test_ai_project.resource.preview

import androidx.compose.ui.tooling.preview.Preview

/**
 * The single preview annotation for the whole app.
 *
 * Every `@Composable` preview uses this rather than a raw `@Preview`, which makes the spec
 * below the one place preview size is decided: re-aiming every preview in the project at a
 * different device is a one-line change here instead of a find-and-replace across a
 * hundred files.
 *
 * SecureVault is a phone app, so this targets a phone. A second annotation — not a second
 * `@Preview` on the same function — is how a tablet variant would be added if the designs
 * ever call for one.
 */
@Preview(
    name = "Phone",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
annotation class DevicePreview
