# Android XML to iOS Xib converter

This project converts Android layout files and resources into iOS layout files and resources.

It is composed of a few parts:

## Plugin

The Gradle plugin does the actual conversion work.  It adds a task in the `ios` group called `xmlToXib`.  Running this task will convert the resources of the Android project into iOS assets and move them into the given destination.  It is configured as follows:

```kotlin
plugins {
    id("com.lightningkite.xmltoxib")
    //...
}
//...

xmlToXib {
    iosProjectName = "My Project"
    iosModuleName = "MyProject"
    iosFolder = project.rootDir.absoluteFile.resolve("../my-ios-project")
}
```

You then need to provide paths to search for layout equivalent files (ending in `xib.yaml`) that define how views and attributes in Android are translated to XIB layouts.  

If you are on a Mac, the files are searched for based on your pods.  The equivalent files are expected to be in the iOS project and its dependencies.

If you are not on a Mac, you can manually define the paths to search in your `local.properties` folder as follows:

```properties
xmltoxib.conversions=path:another/path:yet/another/path
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