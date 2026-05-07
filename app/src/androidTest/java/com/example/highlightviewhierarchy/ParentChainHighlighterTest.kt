package com.example.highlightviewhierarchy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.highlightviewhierarchy.highlight.HighlightRole
import com.example.highlightviewhierarchy.highlight.parentChainHighlighterForTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ParentChainHighlighterTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tappingALeaf_marksItAsSourceAndItsAncestorsAsAncestor() {
        val roles = mutableMapOf<String, HighlightRole>()

        rule.setContent {
            val record: (String, HighlightRole) -> Unit = remember {
                { tag, role -> roles[tag] = role }
            }
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .parentChainHighlighterForTest(debugTag = "root", onRoleChange = record)
                    .testTag("root"),
            ) {
                Column(
                    modifier = Modifier
                        .parentChainHighlighterForTest(debugTag = "mid", onRoleChange = record)
                        .testTag("mid"),
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .parentChainHighlighterForTest(debugTag = "leaf", onRoleChange = record)
                            .testTag("leaf"),
                    )
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .parentChainHighlighterForTest(debugTag = "sibling", onRoleChange = record)
                            .testTag("sibling"),
                    )
                }
            }
        }

        rule.onNodeWithTag("leaf").performClick()

        rule.runOnIdle {
            assertEquals(HighlightRole.Source, roles["leaf"])
            assertEquals(HighlightRole.Ancestor, roles["mid"])
            assertEquals(HighlightRole.Ancestor, roles["root"])
            // Sibling stayed at None throughout, so the callback never fired for it.
            assertFalse("sibling should not have been touched", roles.containsKey("sibling"))
        }
    }
}
