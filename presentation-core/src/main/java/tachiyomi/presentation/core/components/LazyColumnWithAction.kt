package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LazyColumnWithAction(
    contentPadding: PaddingValues,
    actionLabel: String,
    onClickAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        val state = rememberLazyListState()
        val einkPager = rememberEinkPager(state)
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues().plusEinkPageButtons(einkPager),
                content = content,
            )
            EinkPageButtons(einkPager)
        }

        HorizontalDivider()

        Button(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            enabled = actionEnabled,
            onClick = onClickAction,
        ) {
            Text(
                text = actionLabel,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
