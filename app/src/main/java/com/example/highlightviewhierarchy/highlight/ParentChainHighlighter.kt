package com.example.highlightviewhierarchy.highlight

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.onClick

private object HighlighterTraverseKey

internal enum class HighlightRole { None, Source, Ancestor, Descendant }

/**
 * Tap any composable that uses this modifier and the tapped node lights up in [sourceColor],
 * every ancestor that also opted in lights up in [ancestorColor], and every descendant that
 * opted in lights up in [descendantColor]. Tap the same node again to clear the chain.
 *
 * No prop drilling: each call site participates independently. The traversal happens over the
 * modifier-node tree via [TraversableNode], so there is no shared state holder, no
 * CompositionLocal, no registry.
 */
fun Modifier.parentChainHighlighter(
    sourceColor: Color = Color(0x66E91E63),
    ancestorColor: Color = Color(0xFFFFC107),
    descendantColor: Color = Color(0xFF03A9F4),
): Modifier = this then ParentChainHighlighterElement(
    sourceColor = sourceColor,
    ancestorColor = ancestorColor,
    descendantColor = descendantColor,
    debugTag = null,
    onRoleChange = null,
)

/** Test hook. Fires [onRoleChange]([debugTag], newRole) whenever this node's role changes. */
internal fun Modifier.parentChainHighlighterForTest(
    debugTag: String,
    onRoleChange: (String, HighlightRole) -> Unit,
    sourceColor: Color = Color(0x66E91E63),
    ancestorColor: Color = Color(0xFFFFC107),
    descendantColor: Color = Color(0xFF03A9F4),
): Modifier = this then ParentChainHighlighterElement(
    sourceColor = sourceColor,
    ancestorColor = ancestorColor,
    descendantColor = descendantColor,
    debugTag = debugTag,
    onRoleChange = onRoleChange,
)

private data class ParentChainHighlighterElement(
    val sourceColor: Color,
    val ancestorColor: Color,
    val descendantColor: Color,
    val debugTag: String?,
    val onRoleChange: ((String, HighlightRole) -> Unit)?,
) : ModifierNodeElement<ParentChainHighlighterNode>() {

    override fun create() = ParentChainHighlighterNode(
        sourceColor = sourceColor,
        ancestorColor = ancestorColor,
        descendantColor = descendantColor,
        debugTag = debugTag,
        onRoleChange = onRoleChange,
    )

    override fun update(node: ParentChainHighlighterNode) {
        node.sourceColor = sourceColor
        node.ancestorColor = ancestorColor
        node.descendantColor = descendantColor
        node.debugTag = debugTag
        node.onRoleChange = onRoleChange
    }
}

private class ParentChainHighlighterNode(
    var sourceColor: Color,
    var ancestorColor: Color,
    var descendantColor: Color,
    var debugTag: String?,
    var onRoleChange: ((String, HighlightRole) -> Unit)?,
) : DelegatingNode(), DrawModifierNode, TraversableNode, SemanticsModifierNode {

    override val traverseKey: Any = HighlighterTraverseKey

    var role by mutableStateOf(HighlightRole.None)
        private set

    init {
        delegate(SuspendingPointerInputModifierNode {
            detectTapGestures(onTap = { onTap() })
        })
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        // No Role.Button: noisy at every level.
        onClick {
            onTap()
            true
        }
    }

    private fun onTap() {
        val wasSource = role == HighlightRole.Source
        // Clear any prior chain anywhere in the tree first, handles both the toggle case
        // and the case where a different subtree was previously highlighted.
        clearChainFromTop()
        if (wasSource) return
        assignRole(HighlightRole.Source)
        traverseAncestors(HighlighterTraverseKey) { node ->
            (node as ParentChainHighlighterNode).assignRole(HighlightRole.Ancestor)
            true
        }
        traverseDescendants(HighlighterTraverseKey) { node ->
            (node as ParentChainHighlighterNode).assignRole(HighlightRole.Descendant)
            TraverseDescendantsAction.ContinueTraversal
        }
    }

    private fun clearChainFromTop() {
        var top: ParentChainHighlighterNode = this
        traverseAncestors(HighlighterTraverseKey) { node ->
            top = node as ParentChainHighlighterNode
            true
        }
        top.assignRole(HighlightRole.None)
        top.traverseDescendants(HighlighterTraverseKey) { node ->
            (node as ParentChainHighlighterNode).assignRole(HighlightRole.None)
            TraverseDescendantsAction.ContinueTraversal
        }
    }

    private fun assignRole(newRole: HighlightRole) {
        if (role == newRole) return
        role = newRole
        val tag = debugTag
        val cb = onRoleChange
        if (tag != null && cb != null) cb(tag, newRole)
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        when (role) {
            HighlightRole.None -> Unit
            HighlightRole.Source -> drawRect(sourceColor)
            // Stroked so an ancestor's overlay doesn't paint over its descendants.
            HighlightRole.Ancestor -> drawRect(
                color = ancestorColor,
                style = Stroke(width = STROKE_WIDTH.toPx()),
            )
            HighlightRole.Descendant -> drawRect(
                color = descendantColor,
                style = Stroke(width = STROKE_WIDTH.toPx()),
            )
        }
    }

    private companion object {
        val STROKE_WIDTH = 3.dp
    }
}
