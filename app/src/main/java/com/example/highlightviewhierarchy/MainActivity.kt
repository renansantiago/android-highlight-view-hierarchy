package com.example.highlightviewhierarchy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.highlightviewhierarchy.highlight.parentChainHighlighter
import com.example.highlightviewhierarchy.ui.theme.HighlightViewHierarchyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HighlightViewHierarchyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DemoTree(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun DemoTree(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
            .parentChainHighlighter(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .parentChainHighlighter(),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .parentChainHighlighter(),
            ) {
                Text(
                    text = "Tap me: header",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(16.dp)
                        .parentChainHighlighter(),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .parentChainHighlighter(),
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .parentChainHighlighter(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(12.dp)
                            .parentChainHighlighter(),
                    ) {
                        Text(
                            text = "Card A: top",
                            modifier = Modifier.parentChainHighlighter(),
                        )
                        Text(
                            text = "Card A: bottom",
                            modifier = Modifier.parentChainHighlighter(),
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .parentChainHighlighter(),
                ) {
                    Text(
                        text = "Card B",
                        modifier = Modifier
                            .padding(12.dp)
                            .parentChainHighlighter(),
                    )
                }
            }

            Text(
                text = "Footer",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .parentChainHighlighter(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoTreePreview() {
    HighlightViewHierarchyTheme {
        DemoTree()
    }
}
