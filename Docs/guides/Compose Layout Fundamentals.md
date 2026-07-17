Layouts in Compose
==================

Jetpack Compose makes it easy to design an efficient layout for your app.

The following pages provide details on how to design and implement your layout:

*   [Layout basics](https://developer.android.com/develop/ui/compose/layouts/basics): Learn about the building blocks for a straightforward app UI.
    
*   [Material components and layouts](https://developer.android.com/develop/ui/compose/components): Learn about Material components and layouts in Compose.
    
*   [Custom layouts](https://developer.android.com/develop/ui/compose/layouts/custom): Learn how to take control of your app's layout, and how to design a custom layout of your own.
    
*   [Support different display sizes](https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes): Learn how to use Compose to build layouts that adapt to different screen sizes, orientations, and form factors.
    
*   [Alignment lines](https://developer.android.com/develop/ui/compose/layouts/alignment-lines): Learn how to create custom alignment lines to precisely align and position your UI elements.
    
*   [Intrinsic measurements](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements): Learn how to set an intrinsic height or width for your UI elements, giving you precise control over how the elements are arranged in the layout.
    
*   [ConstraintLayout](https://developer.android.com/develop/ui/compose/layouts/constraintlayout): Learn how to use ConstraintLayout in your Compose UI.
    

\- Compose layout basics

Jetpack Compose makes it much easier to design and build your app's UI. Compose transforms state into UI elements, via:

1.  Composition of elements
    
2.  Layout of elements
    
3.  Drawing of elements
    

This document focuses on the layout of elements, explaining some of the building blocks Compose provides to help you lay out your UI elements.

Goals of layouts in Compose
---------------------------

The Jetpack Compose implementation of the layout system has two main goals:

*   [High performance](https://developer.android.com/develop/ui/compose/layouts/basics#performance)
    
*   Ability to easily write [custom layouts](https://developer.android.com/develop/ui/compose/layouts/custom)
    

**Note:** With the Android View system, you could face some performance issues when nesting certain Views such as **RelativeLayout**. Since Compose avoids multiple measurements, you can nest as deeply as you want without affecting performance.

Basics of composable functions
------------------------------

Composable functions are the basic building block of Compose. A composable function is a function emitting Unit that describes some part of your UI. The function takes some input and generates what's shown on the screen. For more information about composables, take a look at the [Compose mental model](https://developer.android.com/develop/ui/compose/mental-model) documentation.

A composable function might emit several UI elements. However, if you don't provide guidance on how they should be arranged, Compose might arrange the elements in a way you don't like. For example, this code generates two text elements:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard() {      Text("Alfred Sisley")      Text("3 minutes ago")  }   `

Without guidance on how you want them arranged, Compose stacks the text elements on top of each other, making them unreadable:

Compose provides a collection of ready-to-use layouts to help you arrange your UI elements, and makes it easy to define your own, more-specialized layouts.

Standard layout components
--------------------------

In many cases, you can just use [Compose's standard layout elements](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary).

Use [Column](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)) to place items vertically on the screen.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCardColumn() {      Column {          Text("Alfred Sisley")          Text("3 minutes ago")      }  }   `

Similarly, use [Row](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Row.composable#Row(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Horizontal,androidx.compose.ui.Alignment.Vertical,kotlin.Function1)(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Horizontal,androidx.compose.ui.Alignment.Vertical,kotlin.Function1)) to place items horizontally on the screen. Both Column and Row support configuring the alignment of the elements they contain.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCardRow(artist: Artist) {      Row(verticalAlignment = Alignment.CenterVertically) {          Image(bitmap = artist.image, contentDescription = "Artist image")          Column {              Text(artist.name)              Text(artist.lastSeenOnline)          }      }  }   `

Use [Box](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Box.composable#Box(androidx.compose.ui.Modifier,androidx.compose.ui.Alignment,kotlin.Boolean,kotlin.Function1)) to put elements on top of another. Box also supports configuring specific alignment of the elements it contains.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistAvatar(artist: Artist) {      Box {          Image(bitmap = artist.image, contentDescription = "Artist image")          Icon(Icons.Filled.Check, contentDescription = "Check mark")      }  }   `

Often these building blocks are all you need. You can write your own composable function to combine these layouts into a more elaborate layout that suits your app.

**Note:** Compose handles nested layouts efficiently, making them a great way to design a complicated UI. This is an improvement from Android Views, where you need to avoid nested layouts for performance reasons.

To set children's position within a Row, set the horizontalArrangement and verticalAlignment arguments. For a Column, set the verticalArrangement and horizontalAlignment arguments:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCardArrangement(artist: Artist) {      Row(          verticalAlignment = Alignment.CenterVertically,          horizontalArrangement = Arrangement.End      ) {          Image(bitmap = artist.image, contentDescription = "Artist image")          Column { /*...*/ }      }  }   `

The layout model
----------------

In the layout model, the UI tree is laid out in a single pass. Each node is first asked to measure itself, then measure any children recursively, passing size constraints down the tree to children. Then, leaf nodes are sized and placed, with the resolved sizes and placement instructions passed back up the tree.

Briefly, parents measure before their children, but are sized and placed after their children.

Consider the following SearchResult function.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun SearchResult() {      Row {          Image(              // ...          )          Column {              Text(                  // ...              )              Text(                  // ...              )          }      }  }   `

This function yields the following UI tree.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   SearchResult    Row      Image      Column        Text        Text   `

In the SearchResult example, the UI tree layout follows this order:

1.  The root node Row is asked to measure.
    
2.  The root node Row asks its first child, Image, to measure.
    
3.  Image is a leaf node (that is, it has no children), so it reports a size and returns placement instructions.
    
4.  The root node Row asks its second child, Column, to measure.
    
5.  The Column node asks its first Text child to measure.
    
6.  The first Text node is a leaf node, so it reports a size and returns placement instructions.
    
7.  The Column node asks its second Text child to measure.
    
8.  The second Text node is a leaf node, so it reports a size and returns placement instructions.
    
9.  Now that the Column node has measured, sized, and, placed its children, it can determine its own size and placement.
    
10.  Now that the root node Row has measured, sized, and placed its children, it can determine its own size and placement.
    

Performance
-----------

Compose achieves high performance by measuring children only once. Single-pass measurement is good for performance, allowing Compose to efficiently handle deep UI trees. If an element measured its child twice and that child measured each of its children twice and so on, a single attempt to lay out a whole UI would have to do a lot of work, making it hard to keep your app performant.

If your layout needs multiple measurements for some reason, Compose offers a special system, _intrinsic measurements_. You can read more about this feature in [Intrinsic measurements in Compose layouts](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements).

Since measurement and placement are distinct sub-phases of the layout pass, any changes that only affects placement of items, not measurement, can be executed separately.

Using modifiers in your layouts
-------------------------------

As discussed in [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers), you can use modifiers to decorate or augment your composables. Modifiers are essential for customizing your layout. For example, here we chain several modifiers to customize the ArtistCard:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCardModifiers(      artist: Artist,      onClick: () -> Unit  ) {      val padding = 16.dp      Column(          Modifier              .clickable(onClick = onClick)              .padding(padding)              .fillMaxWidth()      ) {          Row(verticalAlignment = Alignment.CenterVertically) { /*...*/ }          Spacer(Modifier.size(padding))          Card(              elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),          ) { /*...*/ }      }  }   `

In the code above, notice different modifier functions used together.

*   clickable makes a composable react to user input and shows a ripple.
    
*   padding puts space around an element.
    
*   fillMaxWidth makes the composable fill the maximum width given to it from its parent.
    
*   size() specifies an element's preferred width and height.
    

**Note:** Among other things, modifiers play a role similar to that of layout parameters in view-based layouts. However, since modifiers are sometimes scope-specific, they offer type safety and also help you to discover and understand what is available and applicable to a certain layout. With XML layouts, it is sometimes hard to find out if a particular layout attribute is applicable to a given view.

Scrollable layouts
------------------

Learn more about scrollable layouts in the [Compose gestures documentation](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures).

For lists and lazy lists, check out the [Compose lists documentation](https://developer.android.com/develop/ui/compose/lists).

Responsive layouts
------------------

A layout should be designed with consideration of different screen orientations and form factor sizes. Compose offers out of the box a few mechanisms to facilitate adapting your composable layouts to various screen configurations.

### Constraints

In order to know the constraints coming from the parent and design the layout accordingly, you can use a BoxWithConstraints. The [measurement constraints](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/BoxWithConstraintsScope) can be found in the scope of the content lambda. You can use these measurement constraints to compose different layouts for different screen configurations:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun WithConstraintsComposable() {      BoxWithConstraints {          Text("My minHeight is $minHeight while my maxWidth is $maxWidth")      }  }   `

Slot-based layouts
------------------

Compose provides a large variety of composables based on [Material Design](https://material.io/design/) with the androidx.compose.material:material dependency (included when creating a Compose project in Android Studio) to make UI building easy. Elements like [Drawer](https://material.io/components/navigation-drawer/), [FloatingActionButton](https://material.io/components/buttons-floating-action-button/), and [TopAppBar](https://material.io/components/app-bars-top) are all provided.

Material components make heavy use of _slot APIs_, a pattern Compose introduces to bring in a layer of customization on top of composables. This approach makes components more flexible, as they accept a child element which can configure itself rather than having to expose every configuration parameter of the child. Slots leave an empty space in the UI for the developer to fill as they wish. For example, these are the slots that you can customize in a [TopAppBar](https://material.io/components/app-bars-top):

Composables usually take a content composable lambda ( content: @Composable () -> Unit). Slot APIs expose multiple content parameters for specific uses. For example, TopAppBar allows you to provide the content for title, navigationIcon, and actions.

For example, [Scaffold](https://developer.android.com/reference/kotlin/androidx/compose/material3/Scaffold.composable#Scaffold(androidx.compose.ui.Modifier,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,androidx.compose.material3.FabPosition,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.foundation.layout.WindowInsets,kotlin.Function1)) allows you to implement a UI with the basic Material Design layout structure. Scaffoldprovides slots for the most common top-level Material components, such as [TopAppBar](https://material.io/components/app-bars-top), [BottomAppBar](https://material.io/components/app-bars-bottom/), [FloatingActionButton](https://material.io/components/buttons-floating-action-button/), and [Drawer](https://material.io/components/navigation-drawer/). By using Scaffold, it's easy to make sure these components are properly positioned and work together correctly.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun HomeScreen(/*...*/) {      ModalNavigationDrawer(drawerContent = { /* ... */ }) {          Scaffold(              topBar = { /*...*/ }          ) { contentPadding ->              // ...          }      }  }   `

Compose modifiers

Modifiers allow you to decorate or augment a composable. Modifiers let you do these sorts of things:

*   Change the composable's size, layout, behavior, and appearance
    
*   Add information, like accessibility labels
    
*   Process user input
    
*   Add high-level interactions, like making an element clickable, scrollable, draggable, or zoomable
    

Modifiers are standard Kotlin objects. Create a modifier by calling one of the [Modifier](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier) class functions:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun Greeting(name: String) {      Column(modifier = Modifier.padding(24.dp)) {          Text(text = "Hello,")          Text(text = name)      }  }   `

You can chain these functions together to compose them:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  private fun Greeting(name: String) {      Column(          modifier = Modifier              .padding(24.dp)              .fillMaxWidth()      ) {          Text(text = "Hello,")          Text(text = name)      }  }   `

In the code above, notice different modifier functions used together.

*   padding puts space around an element.
    
*   fillMaxWidth makes the composable fill the maximum width given to it from its parent.
    

It's a best practice to have _all_ of your composables accept a modifier parameter, and pass that modifier to its first child that emits UI. Doing so makes your code more reusable and makes its behavior more predictable and intuitive. For more information, see the Compose API guidelines, [Elements accept and respect a Modifier parameter](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md#elements-accept-and-respect-a-modifier-parameter).

Order of modifiers matters
--------------------------

The order of modifier functions is **significant**. Since each function makes changes to the Modifierreturned by the previous function, the sequence affects the final result. Let's see an example of this:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(/*...*/) {      val padding = 16.dp      Column(          Modifier              .clickable(onClick = onClick)              .padding(padding)              .fillMaxWidth()      ) {          // rest of the implementation      }  }   `

In the code above the whole area is clickable, including the surrounding padding, because the padding modifier has been applied _after_ the clickable modifier. If the modifiers order is reversed, the space added by padding does not react to user input:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(/*...*/) {      val padding = 16.dp      Column(          Modifier              .padding(padding)              .clickable(onClick = onClick)              .fillMaxWidth()      ) {          // rest of the implementation      }  }   `

**Note:** The explicit order helps you to reason about how different modifiers will interact. Compare this to the view-based system where you had to learn the box model, that margins applied "outside" the element but padding "inside" it, and a background element would be sized accordingly. The modifier design makes this kind of behavior explicit and predictable, and gives you more control to achieve the exact behavior you want. It also explains why there is not a margin modifier but only a **padding** one.

Built-in modifiers
------------------

Jetpack Compose provides a list of built-in modifiers to help you decorate or augment a composable. Here are some common modifiers you'll use to adjust your layouts.

**Note:** Many of these modifiers are designed to help you arrange your UI's layout just the way you need it. For more information about how modifiers work in your layout, see the [Compose layout basics](https://developer.android.com/develop/ui/compose/layouts/basics) documentation.

### padding and size

By default, layouts provided in Compose wrap their children. However, you can set a size by using the [size](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/size.modifier#(androidx.compose.ui.Modifier).size(androidx.compose.ui.unit.Dp)) modifier:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(/*...*/) {      Row(          modifier = Modifier.size(width = 400.dp, height = 100.dp)      ) {          Image(/*...*/)          Column { /*...*/ }      }  }   `

Note that the size you specified might not be respected if it does not satisfy the constraints coming from the layout's parent. If you require the composable size to be fixed regardless of the incoming constraints, use the requiredSize modifier:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(/*...*/) {      Row(          modifier = Modifier.size(width = 400.dp, height = 100.dp)      ) {          Image(              /*...*/              modifier = Modifier.requiredSize(150.dp)          )          Column { /*...*/ }      }  }   `

In this example, even with the parent height set to 100.dp, the height of the Image will be 150.dp, as the requiredSize modifier takes precedence.

**Note:** Layouts are based on constraints, and normally, the parent passes those constraints to the children. The child _should_ respect the constraints. However, that might not always be what the UI requires. There are ways to bypass this child behavior. For example, you can pass modifiers like **requiredSize** directly to the child, overriding the constraints received by the child from the parent, or you can use a custom layout with different behavior. When a child does not respect its constraints, the layout system will hide this from the parent. The parent will see the child's **width** and **height** values as if they were coerced in the constraints provided by the parent. The layout system will then center the child within the space allocated by the parent under the assumption that the child respected the constraints. Developers can override this centering behaviour by applying **wrapContentSize** modifiers to the child.

If you want a child layout to fill all the available height allowed by the parent, add the fillMaxHeight modifier (Compose also provides fillMaxSize and fillMaxWidth):

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(/*...*/) {      Row(          modifier = Modifier.size(width = 400.dp, height = 100.dp)      ) {          Image(              /*...*/              modifier = Modifier.fillMaxHeight()          )          Column { /*...*/ }      }  }   `

To add padding all around an element, set a [padding](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/padding.modifier#(androidx.compose.ui.Modifier).padding(androidx.compose.ui.unit.Dp)) modifier.

If you want to add padding above a text baseline such that you achieve a specific distance from the top of the layout to the baseline, use the paddingFromBaseline modifier:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(artist: Artist) {      Row(/*...*/) {          Column {              Text(                  text = artist.name,                  modifier = Modifier.paddingFromBaseline(top = 50.dp)              )              Text(artist.lastSeenOnline)          }      }  }   `

### Offset

To position a layout relative to its original position, add the [offset](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/offset.modifier#(androidx.compose.ui.Modifier).offset(androidx.compose.ui.unit.Dp,androidx.compose.ui.unit.Dp)) modifier and set the offset in the **x** and **y** axis. Offsets can be positive as well as non-positive. The difference between padding and offset is that adding an offset to a composable does not change its measurements:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(artist: Artist) {      Row(/*...*/) {          Column {              Text(artist.name)              Text(                  text = artist.lastSeenOnline,                  modifier = Modifier.offset(x = 4.dp)              )          }      }  }   `

The offset modifier is applied horizontally according to the layout direction. In a **left-to-right** context, a positive offset shifts the element to the right, while in a **right-to-left** context, it shifts the element to the left. If you need to set an offset without considering layout direction, see the [absoluteOffset](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary#absoluteOffset(androidx.compose.ui.Modifier,androidx.compose.ui.unit.Dp,androidx.compose.ui.unit.Dp)) modifier, in which a positive offset value always shifts the element to the right.

The offset modifier provides two overloads - [offset](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/offset.modifier#(androidx.compose.ui.Modifier).offset(androidx.compose.ui.unit.Dp,androidx.compose.ui.unit.Dp)) that takes the offsets as parameters and [offset](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/offset.modifier#(androidx.compose.ui.Modifier).offset(kotlin.Function1)) that takes in a lambda. For more in depth information on when to use each of these and how to optimize for performance, read through the [Compose performance - Defer reads as long as possible](https://developer.android.com/develop/ui/compose/performance#defer-reads) section.

Scope safety in Compose
-----------------------

In Compose, there are modifiers that can only be used when applied to children of certain composables. Compose enforces this by means of custom scopes.

For example, if you want to make a child as big as the parent Box without affecting the Box size, use the [matchParentSize](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/BoxScope#(androidx.compose.ui.Modifier).matchParentSize()) modifier. matchParentSize is only available in [BoxScope](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/BoxScope). Therefore, it can only be used on a child within a Box parent.

Scope safety prevents you from adding modifiers that wouldn't work in other composables and scopes and saves time from trial and error.

**Note:** In the Android View system, there is no scope safety. Developers usually find themselves trying out different layout params to discover which ones are considered and their meaning in the context of a particular parent.

Scoped modifiers notify the parent about some information the parent should know about the child. These are also commonly referred to as _parent data modifiers_. Their internals are different from the general purpose modifiers, but from a usage perspective, these differences don't matter.

### matchParentSize in Box

As mentioned above, if you want a child layout to be the same size as a parent Box without affecting the Box size, use the matchParentSize modifier.

Note that matchParentSize is only available within a Box scope, meaning that it only applies to _direct_ children of Box composables.

In the example below, the child Spacer takes its size from its parent Box, which in turn takes its size from the biggest children, ArtistCard in this case.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun MatchParentSizeComposable() {      Box {          Spacer(              Modifier                  .matchParentSize()                  .background(Color.LightGray)          )          ArtistCard()      }  }   `

If fillMaxSize were used instead of matchParentSize, the Spacer would take all the available space allowed to the parent, in turn causing the parent to expand and fill all the available space.

### weight in Row and Column

As you have seen in the previous section on [Padding and size](https://developer.android.com/develop/ui/compose/modifiers#padding-and-size), by default, a composable size is defined by the content it is wrapping. You can set a composable size to be flexible within its parent using the weight Modifier that is only available in RowScope, and ColumnScope.

Let’s take a Row that contains two Box composables. The first box is given twice the weight of the second, so it's given twice the width. Since the Row is 210.dp wide, the first Box is 140.dp wide, and the second is 70.dp:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun ArtistCard(/*...*/) {      Row(          modifier = Modifier.fillMaxWidth()      ) {          Image(              /*...*/              modifier = Modifier.weight(2f)          )          Column(              modifier = Modifier.weight(1f)          ) {              /*...*/          }      }  }   `

Extracting and reusing modifiers
--------------------------------

Multiple modifiers can be chained together to decorate or augment a composable. This chain is created via the [Modifier](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier) interface which represents an ordered, immutable list of single [Modifier.Elements](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Element).

Each Modifier.Element represents an individual behavior, like layout, drawing and graphics behaviors, all gesture-related, focus and semantics behaviors, as well as device input events. Their ordering matters: modifier elements that are added first will be applied first.

Sometimes it can be beneficial to reuse the same modifier chain instances in multiple composables, by extracting them into variables and hoisting them into higher scopes. It can improve code readability or help improve your app's performance for a few reasons:

*   The re-allocation of the modifiers won’t be repeated when recomposition occurs for composables that use them
    
*   Modifier chains could potentially be very long and complex, so reusing the same instance of a chain can alleviate the workload Compose runtime needs to do when comparing them
    
*   This extraction promotes code cleanliness, consistency and maintainability across the codebase
    

### Best practices for reusing modifiers

Create your own Modifier chains and extract them to reuse them on multiple composable components. It is completely fine to just save a modifier, as they are data-like objects:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   val reusableModifier = Modifier      .fillMaxWidth()      .background(Color.Red)      .padding(12.dp)   `

#### Extracting and reusing modifiers when observing frequently changing state

When observing frequently changing states inside composables, like animation states or scrollState, there can be a significant amount of recompositions done. In this case, your modifiers will get allocated on every recomposition and potentially for every frame:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun LoadingWheelAnimation() {      val animatedState = animateFloatAsState(/*...*/)      LoadingWheel(          // Creation and allocation of this modifier will happen on every frame of the animation!          modifier = Modifier              .padding(12.dp)              .background(Color.Gray),          animatedState = animatedState      )  }   `

Instead, you can create, extract and reuse the same instance of the modifier and pass it to the composable like this:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   // Now, the allocation of the modifier happens here:  val reusableModifier = Modifier      .padding(12.dp)      .background(Color.Gray)  @Composable  fun LoadingWheelAnimation() {      val animatedState = animateFloatAsState(/*...*/)      LoadingWheel(          // No allocation, as we're just reusing the same instance          modifier = reusableModifier,          animatedState = animatedState      )  }   `

#### Extracting and reusing unscoped modifiers

Modifiers can be unscoped or scoped to a specific composable. In the case of unscoped modifiers, you can easily extract them outside of any composables as simple variables:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   val reusableModifier = Modifier      .fillMaxWidth()      .background(Color.Red)      .padding(12.dp)  @Composable  fun AuthorField() {      HeaderText(          // ...          modifier = reusableModifier      )      SubtitleText(          // ...          modifier = reusableModifier      )  }   `

This can be especially beneficial when combined with Lazy layouts. In most cases, you’d want all of your, potentially significant, amount of items to have the exact same modifiers:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   val reusableItemModifier = Modifier      .padding(bottom = 12.dp)      .size(216.dp)      .clip(CircleShape)  @Composable  private fun AuthorList(authors: List) {      LazyColumn {          items(authors) {              AsyncImage(                  // ...                  modifier = reusableItemModifier,              )          }      }  }   `

#### Extracting and reusing scoped modifiers

When dealing with modifiers that are scoped to certain composables, you can extract them to the highest possible level and reuse where appropriate:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Column(/*...*/) {      val reusableItemModifier = Modifier          .padding(bottom = 12.dp)          // Align Modifier.Element requires a ColumnScope          .align(Alignment.CenterHorizontally)          .weight(1f)      Text1(          modifier = reusableItemModifier,          // ...      )      Text2(          modifier = reusableItemModifier          // ...      )      // ...  }   `

You should only be passing the extracted, scoped modifiers to the same-scoped, direct children. See the section [Scope safety in Compose](https://developer.android.com/develop/ui/compose/modifiers#scope-safety) for more reference on why this matters:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Column(modifier = Modifier.fillMaxWidth()) {      // Weight modifier is scoped to the Column composable      val reusableItemModifier = Modifier.weight(1f)      // Weight will be properly assigned here since this Text is a direct child of Column      Text1(          modifier = reusableItemModifier          // ...      )      Box {          Text2(              // Weight won't do anything here since the Text composable is not a direct child of Column              modifier = reusableItemModifier              // ...          )      }  }   `

#### Further chaining of extracted modifiers

You can further chain or append your extracted modifier chains by calling the [.then()](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#then(androidx.compose.ui.Modifier)) function:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   val reusableModifier = Modifier      .fillMaxWidth()      .background(Color.Red)      .padding(12.dp)  // Append to your reusableModifier  reusableModifier.clickable { /*...*/ }  // Append your reusableModifier  otherModifier.then(reusableModifier)   `

Just keep in mind that [the order of modifiers matters!](https://developer.android.com/develop/ui/compose/modifiers#order-modifier-matters)

**Constraints and modifier order**

In Compose, you can chain multiple modifiers together to change the look and feel of a composable. These modifier chains can affect the _constraints_ passed to composables, which define width and height bounds.

This page describes how chained modifiers affect constraints and, in turn, the measurement and placement of composables.

Modifiers in the UI tree
------------------------

To understand how modifiers influence each other, it's helpful to visualize how they appear in the UI tree, which is generated during the composition phase. For more information, see the [Composition](https://developer.android.com/develop/ui/compose/phases#composition) section.

In the UI tree, you can visualize modifiers as wrapper nodes for the layout nodes:

_**Figure 1.**_ _Modifiers wrapping layout nodes in the UI tree._

Adding more than one modifier to a composable creates a chain of modifiers. When you chain multiple modifiers, each modifier node _wraps the rest of the chain and the layout node within_. For example, when you chain a [clip](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).clip(androidx.compose.ui.graphics.Shape)) and a [size](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).size(androidx.compose.ui.unit.Dp)) modifier, the clip modifier node wraps the size modifier node, which then wraps the Image layout node.

In the layout phase, the [algorithm that walks the tree](https://developer.android.com/develop/ui/compose/phases#layout) stays the same, but each modifier node is visited as well. This way, a modifier can change the size requirements and placement of the modifier or layout node that it wraps.

As shown in Figure 2, the implementation of the Image and Text composables themselves consists of a chain of modifiers wrapping a single layout node.

The implementations of Row and Column are layout nodes that describe how to lay out their children.

_**Figure 2.**_ _The same tree structure as in Figure 1, but with composables in the UI tree visualized as chains of modifiers._

To summarize:

*   Modifiers wrap a single modifier or layout node.
    
*   Layout nodes can lay out multiple child nodes.
    

The following sections describe how to use this mental model to reason about modifier chaining and how it influences the size of composables.

Constraints in the layout phase
-------------------------------

[The layout phase](https://developer.android.com/develop/ui/compose/phases#layout) follows a three-step algorithm to find each layout node's width, height, and x, y coordinate:

1.  **Measure children**: A node measures its children, if any.
    
2.  **Decide own size**: Based on those measurements, a node decides on its own size.
    
3.  **Place children**: Each child node is placed relative to a node's own position.
    

[Constraints](https://developer.android.com/reference/kotlin/androidx/compose/ui/unit/Constraints) help find the right sizes for the nodes during the first two steps of the algorithm. Constraints define the minimum and maximum bounds for a node's width and height. When the node decides on its size, its measured size should fall within this size range.

### Types of constraints

A constraint can be one of the following:

*   **Bounded**: The node has a maximum and minimum width and height.
    

_**Figure 3.**_ _Bounded constraints._

*   **Unbounded**: The node is not constrained to any size. The maximum width and height bounds are set to infinity.
    

_**Figure 4.**_ _Unbounded constraints._

*   **Exact**: The node is asked to follow an exact size requirement. The minimum and maximum bounds are set to the same value.
    

_**Figure 5.**_ _Exact constraints._

*   **Combination**: The node follows a combination of the preceding constraint types. For example, a constraint could bound the width while allowing for an unbounded maximum height, or set an exact width but provide a bounded height.
    

_**Figure 6.**_ _Combinations of bounded and unbounded constraints and exact widths and heights._

The next section describes how these constraints are passed from a parent to a child.

### How constraints are passed from parent to child

During the first step of the algorithm described in [Constraints in the layout phase](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers#constraints-layout), constraints are passed down from parent to child in the UI tree.

When a parent node measures its children, it provides these constraints to each child to let them know how big or small they're allowed to be. Then, when it decides its own size, it also adheres to the constraints that were passed in by its own parents.

At a high level, the algorithm works in the following way:

1.  To decide the size it actually wants to occupy, the root node in the UI tree measures its children and forwards the same constraints to its first child.
    
2.  If the child is a modifier that does not impact measurement, it forwards the constraints to the next modifier. The constraints are passed down the modifier chain as-is unless a modifier that impacts measurement is reached. The constraints are then re-sized accordingly.
    
3.  Once a node is reached that doesn't have any children (referred to as a "leaf node"), it decides its size based on the constraints that were passed in, and returns this resolved size to its parent.
    
4.  The parent adapts its constraints based on this child's measurements, and calls its next child with these adjusted constraints.
    
5.  Once all children of a parent are measured, the parent node decides on its own size and communicates that to its own parent.
    
6.  This way, the whole tree is traversed depth-first. Eventually, all the nodes have decided on their sizes, and the measurement step is completed.
    

For an in-depth example, see the [Constraints and modifier order](https://www.youtube.com/watch?v=OeC5jMV342A&t=204s) video.

Modifiers that affect constraints
---------------------------------

You learned in the previous section that some modifiers can affect constraint size. The following sections describe specific modifiers that impact constraints.

### The size modifier

The [size](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).size(androidx.compose.ui.unit.Dp)) modifier declares the preferred size of the content.

For example, the following UI tree should be rendered in a container of 300dp by 200dp. The constraints are bounded, allowing widths between 100dp and 300dp, and heights between 100dp and 200dp:

_**Figure 7.**_ _Bounded constraints in the UI tree and its representation in a container._

The size modifier adapts incoming constraints to match the value passed to it. In this example, the value is 150dp:

**Figure 8.** The size modifier adjusting constraints to 150dp.

If the width and height are smaller than the smallest constraint bound, or larger than the largest constraint bound, the modifier matches the passed constraints as closely as it can while still adhering to the constraints passed in:

**Figure 9.** The size modifier adhering to the passed constraint as closely as possible.

Note that chaining multiple size modifiers does not work. The first size modifier sets both the minimum and maximum constraints to a fixed value. Even if the second size modifier requests a smaller or larger size, it still needs to adhere to the exact bounds passed in, so it won't override those values:

**Figure 10.** A chain of two size modifiers, in which the second value passed in (50dp) does not override the first value (100dp).

### The requiredSize modifier

Use the [requiredSize](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).requiredSize(androidx.compose.ui.unit.Dp)) modifier instead of [size](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).size(androidx.compose.ui.unit.Dp)) if you need your node to override the incoming constraints. The requiredSize modifier replaces the incoming constraints and passes the size you specify as exact bounds.

When the size is passed back up the tree, the child node will be centered in the available space:

**Figure 11.** The requiredSize modifier overriding incoming constraints from the size modifier.

### The width and height modifiers

The size modifier adapts both the width and height of the constraints. With the width modifier, you can set a fixed width but leave the height undecided. Similarly, with the height modifier, you can set a fixed height, but leave the width undecided:

**Figure 12.** The width modifier and height modifier setting a fixed width and height, respectively.

### The sizeIn modifier

The sizeIn modifier lets you set exact minimum and maximum constraints for width and height. Use the sizeIn modifier if you need fine-grained control over the constraints.

**Figure 13.** The sizeIn modifier with minWidth, maxWidth, minHeight, and maxHeight set.

Examples
--------

This section shows and explains the output from several code snippets with chained modifiers.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Image(      painterResource(R.drawable.hero),      contentDescription = null,      Modifier          .fillMaxSize()          .size(50.dp)  )   `

This snippet produces the following output:

**Figure 14.** The Image fills the maximum size as a result of the modifier chain.

*   The [fillMaxSize](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).fillMaxSize(kotlin.Float)) modifier changes the constraints to set both the minimum width and height to the maximum value — 300dp in width and 200dp in height.
    
*   Even though the size modifier wants to use a size of 50dp, it still needs to adhere to the incoming minimum constraints. So the size modifier will also output the exact constraint bounds of 300 by 200, effectively ignoring the value provided in the size modifier.
    
*   The Image follows these bounds and reports a size of 300 by 200, which is passed all the way up the tree.
    

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Image(      painterResource(R.drawable.hero),      contentDescription = null,      Modifier          .fillMaxSize()          .wrapContentSize()          .size(50.dp)  )   `

This snippet produces the following output:

**Figure 15.** The Image is centered and sized to 50dp.

*   The fillMaxSize modifier adapts the constraints to set both the minimum width and height to the maximum value — 300dp in width, and 200dp in height.
    
*   The wrapContentSize modifier resets the minimum constraints. So, while fillMaxSize resulted in fixed constraints, wrapContentSize _resets it back to bounded constraints_. The following node can now take up the whole space again, or be smaller than the entire space.
    
*   The size modifier sets the constraints to minimum and maximum bounds of 50.
    
*   The Image resolves to a size of 50 by 50, and the size modifier forwards that.
    
*   The [wrapContentSize](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).wrapContentSize(androidx.compose.ui.Alignment,kotlin.Boolean)) modifier has a special property. It takes its child and _puts it in the center of the available minimum bounds_ that were passed to it. The size it communicates to its parents is thus equal to the minimum bounds that were passed into it.
    

By combining just three modifiers, you can define a size for the composable and center it in its parent.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   Image(      painterResource(R.drawable.hero),      contentDescription = null,      Modifier          .clip(CircleShape)          .padding(10.dp)          .size(100.dp)  )   `

This snippet produces the following output:

_**Figure 16.**_ _An incorrectly clipped shape due to modifier order._

*   The clip modifier does not change the constraints.
    
*   The padding modifier lowers the maximum constraints.
    
*   The size modifier sets all constraints to 100dp.
    
*   The Image adheres to those constraints and reports a size of 100dp by 100dp.
    
*   The padding modifier adds 10dp on all sides to the size reported by the Image, so the layout with padding reports a width and height of 120dp.
    
*   Now, in the drawing phase, the clip modifier acts on a canvas of 120dp by 120dp. It creates a circle mask of that size.
    
*   The padding modifier then insets its content by 10dp on all sides, which lowers the canvas size for the Image to 100dp by 100dp.
    
*   The Image is drawn in that smaller canvas. The image is clipped based on the original circle of 120dp, so the output is a non-round result.
    

**Create custom modifiers**
---------------------------

Compose provides many [modifiers](https://developer.android.com/develop/ui/compose/modifiers) for common behaviors right out of the box, but you can also create your own custom modifiers.

Modifiers have multiple parts:

*   A modifier factory
    
    *   This is an extension function on Modifier, which provides an idiomatic API for your modifier and allows modifiers to be chained together. The modifier factory produces the modifier elements used by Compose to modify your UI.
        
*   A modifier element
    
    *   This is where you can implement the behavior of your modifier.
        

There are multiple ways to implement a custom modifier depending on the functionality needed. Often, the simplest way to implement a custom modifier is to implement a custom modifier factory that combines other already defined modifier factories. If you need more custom behavior, implement the modifier element using the Modifier.Node APIs, which are lower level but provide more flexibility.

Chain existing modifiers together
---------------------------------

It is often possible to create custom modifiers by using existing modifiers. For example, [Modifier.clip()](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).clip(androidx.compose.ui.graphics.Shape)) is implemented using the graphicsLayer modifier. This strategy uses existing modifier elements, and you provide your own custom modifier factory.

Before implementing your own custom modifier, see if you can use the same strategy.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   fun Modifier.clip(shape: Shape) = graphicsLayer(shape = shape, clip = true)   `

Or, if you find you are repeating the same group of modifiers often, you can wrap them into your own modifier:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   fun Modifier.myBackground(color: Color) = padding(16.dp)      .clip(RoundedCornerShape(8.dp))      .background(color)   `

Create a custom modifier using a composable modifier factory
------------------------------------------------------------

You can also create a custom modifier using a composable function to pass values to an existing modifier. This is known as a composable modifier factory.

**Note:** In previous versions of Compose, we recommended against this approach and suggested using [**composed {}**](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).composed(kotlin.Function1,kotlin.Function1)) instead using a lint rule. Now that **composed {}** is not recommended, the lint rule has been removed.

Using a composable modifier factory to create a modifier also lets you use higher level compose APIs, such as [animate\*AsState](https://developer.android.com/develop/ui/compose/animation/value-based#animate-as-state) and other [Compose state backed animation APIs](https://developer.android.com/develop/ui/compose/animation/choose-api). For example, the following snippet shows a modifier that animates an alpha change when enabled/disabled:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun Modifier.fade(enable: Boolean): Modifier {      val alpha by animateFloatAsState(if (enable) 0.5f else 1.0f)      return this then Modifier.graphicsLayer { this.alpha = alpha }  }   `

**Warning:** When creating custom modifiers, don't break the modifier chain. You must always reference **this** or else any modifiers previously added will be dropped. You can use **this then Modifier** as in the preceding example or implicitly using **return graphicsLayer { this.alpha = alpha }**.

If your custom modifier is a convenience method to provide default values from a CompositionLocal, the easiest way to implement this is to use a composable modifier factory:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun Modifier.fadedBackground(): Modifier {      val color = LocalContentColor.current      return this then Modifier.background(color.copy(alpha = 0.5f))  }   `

This approach has some caveats, which are detailed in the following sections.

#### CompositionLocal values are resolved at the call site of the modifier factory

When creating a custom modifier using a composable modifier factory, composition locals take the value from the composition tree where they are created, not used. This can lead to unexpected results. For example, consider the composition local modifier example mentioned previously, implemented slightly differently using a composable function:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   @Composable  fun Modifier.myBackground(): Modifier {      val color = LocalContentColor.current      return this then Modifier.background(color.copy(alpha = 0.5f))  }  @Composable  fun MyScreen() {      CompositionLocalProvider(LocalContentColor provides Color.Green) {          // Background modifier created with green background          val backgroundModifier = Modifier.myBackground()          // LocalContentColor updated to red          CompositionLocalProvider(LocalContentColor provides Color.Red) {              // Box will have green background, not red as expected.              Box(modifier = backgroundModifier)          }      }  }   `

If this is not how you expect your modifier to work, use a custom [Modifier.Node](https://developer.android.com/develop/ui/compose/custom-modifiers#implement-custom) instead, as composition locals will be correctly resolved at the usage site and can be safely hoisted.

#### Composable function modifiers are never skipped

Composable factory modifiers are never [skipped](https://developer.android.com/develop/ui/compose/mental-model#skips) because composable functions that have return values cannot be skipped. This means your modifier function will be called on every recomposition, which may be expensive if it recomposes frequently.

#### Composable function modifiers must be called within a composable function

Like all composable functions, a composable factory modifier must be called from within composition. This limits where a modifier can be hoisted to, as it can never be hoisted out of composition. In comparison, non-composable modifier factories can be hoisted out of composable functions to allow easier reuse and improve performance:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   val extractedModifier = Modifier.background(Color.Red) // Hoisted to save allocations  @Composable  fun Modifier.composableModifier(): Modifier {      val color = LocalContentColor.current.copy(alpha = 0.5f)      return this then Modifier.background(color)  }  @Composable  fun MyComposable() {      val composedModifier = Modifier.composableModifier() // Cannot be extracted any higher  }   `

Implement custom modifier behavior using Modifier.Node
------------------------------------------------------

[Modifier.Node](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Node) is a lower level API for creating modifiers in Compose. It is the same API that Compose implements its own modifiers in and is the most performant way to create custom modifiers.

**Note:** There is another API for creating custom modifiers, [**composed {}**](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).composed(kotlin.Function1,kotlin.Function1)). This API is no longer recommended due to the performance issues it created. **Modifier.Node** was designed from the ground up to be far more performant than composed modifiers. For more details on the problems with composed modifiers, see the Android Dev Summit talk [Compose Modifiers Deep Dive](https://www.youtube.com/watch?v=BjGX2RftXsU).

### Implement a custom modifier using Modifier.Node

There are three parts to implementing a custom modifier using Modifier.Node:

*   A [Modifier.Node](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/Modifier.kt;l=184) implementation that holds the logic and state of your modifier.
    
*   A [ModifierNodeElement](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/ModifierNodeElement.kt;l=39) that creates and updates modifier node instances.
    
*   An optional modifier factory, as detailed previously.
    

ModifierNodeElement classes are stateless and new instances are allocated each recomposition, whereas Modifier.Node classes can be stateful and will survive across multiple recompositions, and can even be reused.

The following section describes each part and shows an example of building a custom modifier to draw a circle.

#### Modifier.Node

The Modifier.Node implementation (in this example, CircleNode) implements the functionality of your custom modifier.

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   // Modifier.Node  private class CircleNode(var color: Color) : DrawModifierNode, Modifier.Node() {      override fun ContentDrawScope.draw() {          drawCircle(color)      }  }   `

In this example, it draws the circle with the color passed in to the modifier function.

A node implements Modifier.Node as well as zero or more node types. There are different node types based on the functionality your modifier requires. The preceding example needs to be able to draw, so it implements DrawModifierNode, which lets it override the draw method.

The available types are as follows:

**Node**

**Usage**

**Sample Link**

[LayoutModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/LayoutModifierNode.kt)

A Modifier.Node that changes how its wrapped content is measured and laid out.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/LayoutSample.kt;l=198)

[DrawModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/DrawModifierNode.kt)

A Modifier.Node that draws into the space of the layout.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierSamples.kt;l=313)

[CompositionLocalConsumerModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/CompositionLocalConsumerModifierNode.kt)

Implementing this interface lets your Modifier.Node read composition locals.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierCompositionLocalSample.kt;l=64)

[SemanticsModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/SemanticsModifierNode.kt)

A Modifier.Node that adds semantics key/value for use in testing, accessibility, and similar use cases.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierSamples.kt;l=338)

[PointerInputModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/PointerInputModifierNode.kt)

A Modifier.Node that receives [PointerInputChanges.](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/input/pointer/PointerEvent.kt)

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierSamples.kt;l=366)

[ParentDataModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/ParentDataModifierNode.kt)

A Modifier.Node that provides data to the parent layout.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Box.kt;l=295)

[LayoutAwareModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/LayoutAwareModifierNode.kt)

A Modifier.Node which receives onMeasured and onPlaced callbacks.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierSamples.kt;l=405)

[GlobalPositionAwareModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/GlobalPositionAwareModifierNode.kt)

A Modifier.Node which receives an onGloballyPositioned callback with the final LayoutCoordinates of the layout when the global position of the content may have changed.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierSamples.kt;l=405)

[ObserverModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/ObserverModifierNode.kt)

Modifier.Nodes that implement ObserverNode can provide their own implementation of onObservedReadsChanged that will be called in response to changes to snapshot objects read within an observeReads block.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierCompositionLocalSample.kt;l=64)

[DelegatingNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/DelegatingNode.kt)

A Modifier.Node which is able to delegate work to other Modifier.Node instances.

This can be useful to compose multiple node implementations into one.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/samples/src/main/java/androidx/compose/ui/samples/ModifierSamples.kt)

[TraversableNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/TraversableNode.kt;l=28)

Allows Modifier.Node classes to traverse up/down the node tree for classes of the same type or for a particular key.

[Sample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/integration-tests/ui-demos/src/main/java/androidx/compose/ui/demos/modifier/TraverseModifierDemo.kt;l=123)

Nodes are automatically invalidated when update is called on their corresponding element. Because our example is a DrawModifierNode, any time update is called on the element, the node triggers a redraw and its color correctly updates. It is possible to opt out of auto-invalidation, as detailed in the [Opt out of node auto-invalidation](https://developer.android.com/develop/ui/compose/custom-modifiers#autoinvalidation) section.

#### ModifierNodeElement

A ModifierNodeElement is an immutable class that holds the data to create or update your custom modifier:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   // ModifierNodeElement  private data class CircleElement(val color: Color) : ModifierNodeElement() {      override fun create() = CircleNode(color)      override fun update(node: CircleNode) {          node.color = color      }  }   `

ModifierNodeElement implementations need to override the following methods:

1.  create: This is the function that instantiates your modifier node. This gets called to create the node when your modifier is first applied. Usually, this amounts to constructing the node and configuring it with the parameters that were passed in to the modifier factory.
    
2.  update: This function is called whenever this modifier is provided in the same spot this node already exists, but a property has changed. This is determined by the equals method of the class. The modifier node that was previously created is sent as a parameter to the update call. At this point, you should update the nodes' properties to correspond with the updated parameters. The ability for nodes to be reused this way is key to the performance gains that Modifier.Node brings; therefore, you must update the existing node rather than creating a new one in the update method. In our circle example, the color of the node is updated.
    

Additionally, ModifierNodeElement implementations also need to implement equals and hashCode. update will only get called if an equals comparison with the previous element returns false.

**Warning:** Your **ModifierNodeElement** must implement **equals** and **hashCode** correctly and not rely on instance equality. Without this, your modifier node will be updated unnecessarily and perform poorly. Use a data class to achieve this automatically.

The preceding example uses a data class to achieve this. These methods are used to check if a node needs updating or not. If your element has properties that don't contribute to whether a node needs to be updated, or you want to avoid data classes for binary compatibility reasons, then you can manually implement equals and hashCode, for example, the [padding modifier element](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Padding.kt;l=358).

#### Modifier factory

This is the public API surface of your modifier. Most implementations create the modifier element and add it to the modifier chain:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   // Modifier factory  fun Modifier.circle(color: Color) = this then CircleElement(color)   `

#### Complete example

These three parts come together to create the custom modifier to draw a circle using the Modifier.Node APIs:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   // Modifier factory  fun Modifier.circle(color: Color) = this then CircleElement(color)  // ModifierNodeElement  private data class CircleElement(val color: Color) : ModifierNodeElement() {      override fun create() = CircleNode(color)      override fun update(node: CircleNode) {          node.color = color      }  }  // Modifier.Node  private class CircleNode(var color: Color) : DrawModifierNode, Modifier.Node() {      override fun ContentDrawScope.draw() {          drawCircle(color)      }  }   `

Common situations using Modifier.Node
-------------------------------------

When creating custom modifiers with Modifier.Node, here are some common situations you might encounter.

### Zero parameters

If your modifier has no parameters, then it never needs to update and, furthermore, doesn't need to be a data class. The following is a sample implementation of a modifier that applies a fixed amount of padding to a composable:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   fun Modifier.fixedPadding() = this then FixedPaddingElement  data object FixedPaddingElement : ModifierNodeElement() {      override fun create() = FixedPaddingNode()      override fun update(node: FixedPaddingNode) {}  }  class FixedPaddingNode : LayoutModifierNode, Modifier.Node() {      private val PADDING = 16.dp      override fun MeasureScope.measure(          measurable: Measurable,          constraints: Constraints      ): MeasureResult {          val paddingPx = PADDING.roundToPx()          val horizontal = paddingPx * 2          val vertical = paddingPx * 2          val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))          val width = constraints.constrainWidth(placeable.width + horizontal)          val height = constraints.constrainHeight(placeable.height + vertical)          return layout(width, height) {              placeable.place(paddingPx, paddingPx)          }      }  }   `

### Reference composition locals

Modifier.Node modifiers don't automatically observe changes to Compose state objects, like CompositionLocal. The advantage Modifier.Node modifiers have over modifiers that are just created with a composable factory is that they can read the value of the composition local from where the modifier is used in your UI tree, not where the modifier is allocated, using [currentValueOf](https://developer.android.com/reference/kotlin/androidx/compose/ui/node/CompositionLocalConsumerModifierNode#(androidx.compose.ui.node.CompositionLocalConsumerModifierNode).currentValueOf(androidx.compose.runtime.CompositionLocal)).

However, modifier node instances don't automatically observe state changes. To automatically react to a composition local changing, you can read its current value inside a scope:

*   DrawModifierNode: [ContentDrawScope](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/DrawModifierNode.kt;l=31)
    
*   LayoutModifierNode: [MeasureScope](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/LayoutModifierNode.kt;l=64) & [IntrinsicMeasureScope](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/LayoutModifierNode.kt;l=87)
    
*   SemanticsModifierNode: [SemanticsPropertyReceiver](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsProperties.kt;l=788)
    

This example observes the value of LocalContentColor to draw a background based on its color. As ContentDrawScope does observe snapshot changes, this automatically redraws when the value of LocalContentColor changes:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   class BackgroundColorConsumerNode :      Modifier.Node(),      DrawModifierNode,      CompositionLocalConsumerModifierNode {      override fun ContentDrawScope.draw() {          val currentColor = currentValueOf(LocalContentColor)          drawRect(color = currentColor)          drawContent()      }  }   `

To react to state changes outside of a scope and automatically update your modifier, use an [ObserverModifierNode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/ObserverModifierNode.kt).

For example, [Modifier.scrollable](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/Scrollable.kt;l=269) uses this technique to observe changes in LocalDensity. A simplified example is shown in the following example:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   class ScrollableNode :      Modifier.Node(),      ObserverModifierNode,      CompositionLocalConsumerModifierNode {      // Place holder fling behavior, we'll initialize it when the density is available.      val defaultFlingBehavior = DefaultFlingBehavior(splineBasedDecay(UnityDensity))      override fun onAttach() {          updateDefaultFlingBehavior()          observeReads { currentValueOf(LocalDensity) } // monitor change in Density      }      override fun onObservedReadsChanged() {          // if density changes, update the default fling behavior.          updateDefaultFlingBehavior()      }      private fun updateDefaultFlingBehavior() {          val density = currentValueOf(LocalDensity)          defaultFlingBehavior.flingDecay = splineBasedDecay(density)      }  }   `

### Animate a modifier

Modifier.Node implementations have access to a coroutineScope. This allows for use of the [Compose Animatable APIs](https://developer.android.com/develop/ui/compose/animation/value-based#animatable). For example, this snippet modifies the CircleNode shown previously to fade in and out repeatedly:

**Note:** **Modifier.Node** can be reused in a composable. You can set the new state either by setting it in [**onAttach**](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Node#onAttach()), or by resetting in [**onReset**](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Node#onReset()).

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   class CircleNode(var color: Color) : Modifier.Node(), DrawModifierNode {      private lateinit var alpha: Animatable      override fun ContentDrawScope.draw() {          drawCircle(color = color, alpha = alpha.value)          drawContent()      }      override fun onAttach() {          alpha = Animatable(1f)          coroutineScope.launch {              alpha.animateTo(                  0f,                  infiniteRepeatable(tween(1000), RepeatMode.Reverse)              ) {              }          }      }  }   `

### Share state between modifiers using delegation

Modifier.Node modifiers can delegate to other nodes. There are many use cases for this, such as extracting common implementations across different modifiers, but it can also be used to share common state across modifiers.

For example, a basic implementation of a clickable modifier node that shares interaction data:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   class ClickableNode : DelegatingNode() {      val interactionData = InteractionData()      val focusableNode = delegate(          FocusableNode(interactionData)      )      val indicationNode = delegate(          IndicationNode(interactionData)      )  }   `

### Opt out of node auto-invalidation

Modifier.Node nodes automatically invalidate when their corresponding ModifierNodeElement calls update. For complex modifiers, you might want to opt out of this behavior to gain more fine-grained control over when your modifier invalidates phases.

This is particularly useful if your custom modifier modifies both layout and draw. Opting out of auto-invalidation lets you just invalidate draw when only draw-related properties, such as color, change. This avoids invalidating layout and can improve your modifier's performance.

A hypothetical example of this is shown in the following example with a modifier that has a color, size, and onClick lambda as properties. This modifier invalidates only what is required, skipping any unneeded invalidation:

Plain textANTLR4BashCC#CSSCoffeeScriptCMakeDartDjangoDockerEJSErlangGitGoGraphQLGroovyHTMLJavaJavaScriptJSONJSXKotlinLaTeXLessLuaMakefileMarkdownMATLABMarkupObjective-CPerlPHPPowerShell.propertiesProtocol BuffersPythonRRubySass (Sass)Sass (Scss)SchemeSQLShellSwiftSVGTSXTypeScriptWebAssemblyYAMLXML`   class SampleInvalidatingNode(      var color: Color,      var size: IntSize,      var onClick: () -> Unit  ) : DelegatingNode(), LayoutModifierNode, DrawModifierNode {      override val shouldAutoInvalidate: Boolean          get() = false      private val clickableNode = delegate(          ClickablePointerInputNode(onClick)      )      fun update(color: Color, size: IntSize, onClick: () -> Unit) {          if (this.color != color) {              this.color = color              // Only invalidate draw when color changes              invalidateDraw()          }          if (this.size != size) {              this.size = size              // Only invalidate layout when size changes              invalidateMeasurement()          }          // If only onClick changes, we don't need to invalidate anything          clickableNode.update(onClick)      }      override fun ContentDrawScope.draw() {          drawRect(color)      }      override fun MeasureScope.measure(          measurable: Measurable,          constraints: Constraints      ): MeasureResult {          val size = constraints.constrain(size)          val placeable = measurable.measure(constraints)          return layout(size.width, size.height) {              placeable.place(0, 0)          }      }  }   `