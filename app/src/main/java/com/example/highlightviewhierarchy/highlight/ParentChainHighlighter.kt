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

private enum class HighlightRole { None, Source, Ancestor }

/**
 * Tap any composable that uses this modifier and the tapped node lights up in [sourceColor]
 * while every ancestor that also opted in lights up in [ancestorColor].
 *
 * No prop drilling: each call site participates independently. The ancestor walk happens over
 * the modifier-node tree via [TraversableNode], so there is no shared state holder, no
 * CompositionLocal, no registry.
 */
fun Modifier.parentChainHighlighter(
    sourceColor: Color = Color(0x66E91E63),
    ancestorColor: Color = Color(0x66FFC107),
): Modifier = this then ParentChainHighlighterElement(sourceColor, ancestorColor)

private data class ParentChainHighlighterElement(
    val sourceColor: Color,
    val ancestorColor: Color,
) : ModifierNodeElement<ParentChainHighlighterNode>() {

    override fun create() = ParentChainHighlighterNode(sourceColor, ancestorColor)

    override fun update(node: ParentChainHighlighterNode) {
        node.sourceColor = sourceColor
        node.ancestorColor = ancestorColor
    }
}

private class ParentChainHighlighterNode(
    var sourceColor: Color,
    var ancestorColor: Color,
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
        // Clear any prior chain anywhere in the tree, then mark this node + its ancestors.
        clearChainFromTop()
        role = HighlightRole.Source
        traverseAncestors(HighlighterTraverseKey) { node ->
            (node as ParentChainHighlighterNode).role = HighlightRole.Ancestor
            true
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
        }
    }
}
