# android-highlight-view-hierarchy

A small Compose sample project showing a custom modifier that highlights a tapped composable, every ancestor that opted in, and (optionally) every descendant that opted in. The whole thing is exposed as a single extension on `Modifier`:

```kotlin
Text("hello", modifier = Modifier.parentChainHighlighter())
```

There is no setup composable at the root, no `CompositionLocal`, no shared state holder, every call site participates independently.

## Demo hierarchy

```
Root Box
└─ Column
   ├─ Header Card
   │   └─ Text "Tap me: header"
   ├─ Row
   │   ├─ Card A
   │   │   └─ Column
   │   │       ├─ Text "Card A: top"
   │   │       └─ Text "Card A: bottom"
   │   └─ Card B
   │       └─ Text "Card B"
   └─ Footer Text
```

The legend at the top of the screen maps colours to roles: pink = the tapped node (Source), amber = ancestors, cyan = descendants.

## Running it

```bash
./gradlew :app:installDebug
adb shell am start -n com.example.highlightviewhierarchy/.MainActivity
```

Run the instrumented test:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.highlightviewhierarchy.ParentChainHighlighterTest
```

## Thought Process

The brief explicitly forbids prop drilling, which to me was the most important hint in the whole task: it rules out the obvious approach of holding a `MutableState` at the root and threading callbacks through every level. Whatever the modifier does, it has to do it locally.

The cleanest fit for "look at my ancestors and siblings without anyone wiring me up" is the modern `Modifier.Node` API combined with `TraversableNode`. Each instance of the modifier is a node in the modifier-node tree, and `traverseAncestors` / `traverseDescendants` walk up and down it. That maps directly onto what the brief is asking for. I chose that and built the rest around it.

I considered two alternatives. `CompositionLocal` was wrong on inspection, locals are resolved at the *allocation* site of the modifier rather than where it lands in the tree, so reading a "is anyone above me highlighted?" value would lie. `ModifierLocal` is the older provider/consumer mechanism and would have worked, but it requires a sentinel provider somewhere up the tree to seed the state, which to me is prop drilling under a different name. I started sketching it and switched once I was sure `TraversableNode` would do the job.

A few decisions worth calling out. I used `Modifier.Node` directly rather than the older `Modifier.composed { }`, the docs steer away from composed because it can't be equality-skipped during recomposition. Making the element a data class gives me `equals` and `hashCode` for free, and that is what keeps the same node alive across recompositions instead of tearing it down and rebuilding each time. The trickiest behaviour was clicking across sibling subtrees: the previous chain has to clear before the new one lights up, otherwise you end up with stale highlights. I handle that by walking up to the topmost participating ancestor and clearing its whole subtree before re-marking from the tapped node.

One bug surfaced as soon as I ran the demo on a device. Ancestors draw their overlay after their `drawContent`, so a translucent fill on a parent ends up painting over its child's source colour, the whole chain looked like one big amber blob. I switched ancestors and descendants to strokes and kept the source as a fill, so the tapped node still pops, the chain reads as nested outlines, and the legend swatches match what is actually rendered.

Things I left on the table. The highlight state lives on the modifier node, so it does not survive rotation; a `rememberSaveable` integration would need a hoisted state and that arguably *is* prop drilling, so I left it. The semantic action exposes the tap to screen readers but I deliberately didn't set `Role.Button` everywhere, every container would announce itself as a button, which is noisier than helpful. With another hour I would animate the overlay with `animateColorAsState` and probably move the colours behind theme tokens.
