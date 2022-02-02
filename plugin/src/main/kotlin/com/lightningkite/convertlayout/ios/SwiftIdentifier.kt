package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.xml.get
import org.w3c.dom.Element

data class SwiftIdentifier(
    val module: String?,
    val name: String
)

fun Element.swiftIdentifier(): SwiftIdentifier {
    return this["customClass"]?.let {
        SwiftIdentifier(
            this["customModule"] ?: this["customModuleAlt"],
            it
        )
    } ?: when(this.tagName) {
        "activityIndicatorView" -> SwiftIdentifier("UIKit", "UIActivityIndicatorView")
        "progressView" -> SwiftIdentifier("UIKit", "UIProgressView")
        "imageView" -> SwiftIdentifier("UIKit", "UIImageView")
        "view" -> SwiftIdentifier("UIKit", "UIView")
        "switch" -> SwiftIdentifier("UIKit", "UISwitch")
        "slider" -> SwiftIdentifier("UIKit", "UISlider")
        "button" -> SwiftIdentifier("UIKit", "UIButton")
        "stackView" -> SwiftIdentifier("UIKit", "UIStackView")
        "textField" -> SwiftIdentifier("UIKit", "UITextField")
        "textView" -> SwiftIdentifier("UIKit", "UITextView")
        "scrollView" -> SwiftIdentifier("UIKit", "UIScrollView")
        "segmentedControl" -> SwiftIdentifier("UIKit", "UISegmentedControl")
        "collectionView" -> SwiftIdentifier("UIKit", "UICollectionView")
        "pageControl" -> SwiftIdentifier("UIKit", "UIPageControl")
        "label" -> SwiftIdentifier("UIKit", "UILabel")
        "mapView" -> SwiftIdentifier("MapKit", "MKMapView")
        "wkWebView" -> SwiftIdentifier("WebKit", "WKWebView")
        else -> throw IllegalArgumentException("Couldn't identify swift name of element for ${this.tagName}")
    }
}
