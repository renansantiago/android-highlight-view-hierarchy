package com.example.highlightviewhierarchy.highlight

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseDescendants

private object HighlighterTraverseKey

private enum class HighlightRole { None, Source, Ancestor, Descendant }

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
    ancestorColor: Color = Color(0x66FFC107),
    descendantColor: Color = Color(0x6603A9F4),
): Modifier = this then ParentChainHighlighterElement(sourceColor, ancestorColor, descendantColor)

private data class ParentChainHighlighterElement(
    val sourceColor: Color,
    val ancestorColor: Color,
    val descendantColor: Color,
) : ModifierNodeElement<ParentChainHighlighterNode>() {

    override fun create() = ParentChainHighlighterNode(sourceColor, ancestorColor, descendantColor)

    override fun update(node: ParentChainHighlighterNode) {
        node.sourceColor = sourceColor
        node.ancestorColor = ancestorColor
        node.descendantColor = descendantColor
    }
}

private class ParentChainHighlighterNode(
    var sourceColor: Color,
    var ancestorColor: Color,
    var descendantColor: Color,
) : DelegatingNode(), DrawModifierNode, TraversableNode {

    override val traverseKey: Any = HighlighterTraverseKey

    var role by mutableStateOf(HighlightRole.None)
        private set

    init {
        delegate(SuspendingPointerInputModifierNode {
            detectTapGestures(onTap = { onTap() })
        })
    }

    private fun onTap() {
        val wasSource = role == HighlightRole.Source
        // Clear any prior chain anywhere in the tree first, handles both the toggle case
        // and the case where a different subtree was previously highlighted.
        clearChainFromTop()
        if (wasSource) return
        role = HighlightRole.Source
        traverseAncestors(HighlighterTraverseKey) { node ->
            (node as ParentChainHighlighterNode).role = HighlightRole.Ancestor
            true
        }
        traverseDescendants(HighlighterTraverseKey) { node ->
            (node as ParentChainHighlighterNode).role = HighlightRole.Descendant
            TraverseDescendantsAction.ContinueTraversal
        }
    }

    private fun clearChainFromTop() {
        var top: ParentChainHighlighterNode = this
        traverseAncestors(HighlighterTraverseKey) { node ->
            top = node as ParentChainHighlighterNode
            true
        }
        top.role = HighlightRole.None
        top.traverseDescendants(HighlighterTraverseKey) { node ->
            (node as ParentChainHighlighterNode).role = HighlightRole.None
            TraverseDescendantsAction.ContinueTraversal
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        when (role) {
            HighlightRole.None -> Unit
            HighlightRole.Source -> drawRect(sourceColor)
            HighlightRole.Ancestor -> drawRect(ancestorColor)
            HighlightRole.Descendant -> drawRect(descendantColor)
        }
    }
}
