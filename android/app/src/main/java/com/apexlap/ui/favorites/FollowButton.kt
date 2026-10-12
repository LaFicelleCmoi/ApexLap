package com.apexlap.ui.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.apexlap.i18n.LocalStrings
import com.apexlap.ui.theme.ApexColors

/** Star in the top bar of a driver or team page: follow / unfollow. */
@Composable
fun FollowButton(isFollowed: Boolean, onToggle: () -> Unit) {
    val strings = LocalStrings.current
    IconButton(onClick = onToggle) {
        Icon(
            if (isFollowed) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (isFollowed) strings.unfollow else strings.follow,
            tint = if (isFollowed) ApexColors.Gold else MaterialTheme.colorScheme.onSurface,
        )
    }
}
