# Android XML to iOS Xib converter

This project converts Android layout files and resources into iOS layout files and resources.

It is composed of a few parts:

## Plugin

The Gradle plugin does the actual conversion work.  It adds a task in the `ios` group called `xmlToXib`.  Running this task will convert the resources of the Android project into iOS assets and move them into the given destination.  It is configured as follows:

```kotlin
plugins {
    id("com.lightningkite.androidlayouttranslator")
    //...
}
//...

val androidRuntimeVersion: String by extra
dependencies {
    //...
    implementation("com.lightningkite.androidlayouttranslator:android-runtime:${androidRuntimeVersion}")
    equivalents("com.lightningkite.androidlayouttranslator:android-runtime:$androidRuntimeVersion:equivalents")
}

androidLayoutConverter {
    iosProjectName = "My Project"
    iosModuleName = "MyProject"
    iosFolder = rootDir.resolve("ios")
    
    webProjectName = "MyProject"
    webFolder = rootDir.resolve("web")
}
```

You can read more about equivalents [here](equivalents.md).

This also generates [`Binding`]() files like Android does

## Android Safe Insets

A small project that makes a convenient way to handle safe insets in Android that translates effectively to iOS.

Setup:

Initialize `ViewPump` with our inflation interceptor as follows:

```kotlin
import com.lightningkite.safeinsets.SafeInsetsInterceptor

// somewhere before startup
ViewPump.init(SafeInsetsInterceptor)
```

Then use the interceptor in your activity:

```kotlin
class MainActivity : Activity {
    //...
    private var appCompatDelegate: AppCompatDelegate? = null
    override fun getDelegate(): AppCompatDelegate {
        if (appCompatDelegate == null) {
            appCompatDelegate = ViewPumpAppCompatDelegate(
                super.getDelegate(),
                this
            )
        }
        return appCompatDelegate!!
    }
}
```

On views that are supposed to have padding equal to the region taken by the status bar / button bar, add the following attribute:

```xml
app:safeInsets="top|horizontal"
```

And that's it!

## XmlToXibRuntime

The iOS runtime for the translator.  Fairly lightweight, it contains the following:

- `CALayer` tools, allowing layers to autosize and respond to `UIControl` states as they need.  `CALayer` is used as the equivalent of a `Drawable`.
- A `compoundDrawable` extension, allowing you to reach a compound drawable.
- A set of labeled toggles, used to emulate Android's `ChecKBox` and `RadioButton`, based on `M13Checkbox`, a Cocoapod
- A set of custom views extending the ability to customize font usage.
- A `XibView` class that inflates a `.xib` of the same name, used to create `Binding` classes in iOS.
