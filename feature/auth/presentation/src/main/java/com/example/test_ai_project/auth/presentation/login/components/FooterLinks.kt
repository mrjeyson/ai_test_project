package com.example.test_ai_project.auth.presentation.login.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.spacing

/** The support and whitepaper links at the bottom of the login screen. */
@Composable
internal fun FooterLinks(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FooterLink(
            text = stringResource(id = ResR.string.login_support),
            onClick = { context.openLink(ResR.string.login_support_url) },
        )
        Spacer(modifier = Modifier.width(spacing.small))
        FooterLink(
            text = stringResource(id = ResR.string.login_whitepaper),
            onClick = { context.openLink(ResR.string.login_whitepaper_url) },
        )
    }
}

@Composable
private fun FooterLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A TextButton rather than clickable text: it comes with the 48dp touch target and
    // the pressed state that a bare AppText does not.
    TextButton(onClick = onClick, modifier = modifier) {
        AppText(
            text = text,
            style = AppTextStyle.Label,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun Context.openLink(@StringRes urlRes: Int) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(urlRes)))
    // A device with no browser is unusual but real; crashing over a footer link is not
    // an acceptable outcome.
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, ResR.string.login_no_browser, Toast.LENGTH_SHORT).show()
    }
}
