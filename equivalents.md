# Layout Equivalents

## Element equivalents

```yaml
- id: LinearLayout  # The name of the element inside of an Android layout file
  type: element
  
  # Tells the translator to automatically wrap the view in a plain UIView if any of these attributes are present, and 
  # apply those attributes to the wrapper rather than the direct view.
  autoWrapFor:  
    - 'android:background'
  
  # An XPath defining where to insert child elements.
  insertChildrenAt: subviews
  
  # A constant indicating what kind of rule to use when adding children.
  # Available constants are linear, frame, frame-first-visible, scroll-vertical, scroll-horizontal
  childRule: linear
  
  # The template of XML to insert into the XIB.
  # Apple kindly didn't document the XIB format, so you'll need to find out what you need by experimentation.
  # You can view the raw XIB within XCode by right-clicking the file and selecting 'view as source code'.
  template: |
  <stackView opaque="NO" contentMode="scaleToFill" translatesAutoresizingMaskIntoConstraints="NO" >
  <subviews>
  </subviews>
  <constraints>
  </constraints>
  </stackView>
```

## Attribute Equivalents

```yaml
- id: android:orientation  # The name of the attribute
  type: attribute
  
  # The element this applies to
  element: LinearLayout
  
  # Actions to apply to the element
  rules:
    # An XPath to modify or create if not present, blank meaning the element itself
    "":
      # Add attributes
      attribute:
        axis: ~value~  # ~value~ is replaced with the value of the attribute.
```

Full available options:

```yaml
- id: android:someAttribute  # The name of the attribute
  type: attribute

  # What kind of value you intend to capture
  valueType: FontLiteral/FontSet/ColorLiteral/ColorResource/ColorStateResource/Vector/Bitmap/Shape/DrawableState/Layer/LayoutResource/DimensionLiteral/DimensionResource/Number/StringLiteral/StringResource/Style/Value/Dimension/Drawable/NamedDrawable/NamedDrawableWithSize/XmlDrawable/DrawableXml/Color/String/Font
  
  # The element this applies to
  element: LinearLayout
  
  # A map of name to rule shortcut.
  xib:
    identifier: SubNode/Attribute/UserDefined/StateSubNode/StateAttribute
    
  # A template of code to run after inflation
  code: ~this~.image = R.drawable.~name~().toImage()
  
  # If present, will only run if the value is equal to this value.
  equalTo: someValue
  
  # Actions to apply to the element
  rules:
    # An XPath to modify or create if not present, blank meaning the element itself
    "":
      # This is a 'subrule'
      # Add attributes
      attribute:
        axis: ~value~  # ~value~ is replaced with the value of the attribute.
      ifContains:
        # runs the given subrule if the attribute value contains the given string
        someValue:
          # another subrule here
      # Append an element from a template
      append:
        - '<constraint firstAttribute="width" relation="greaterThanOrEqual" constant="~number~"/>'
```