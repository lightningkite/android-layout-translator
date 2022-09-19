//Stub file made with Butterfly 2 (by Lightning Kite)
import Foundation
import UIKit

@IBDesignable
open class StyledUILabel: UILabel {
    @IBInspectable
    public var lineSpacingMultiplier: CGFloat = 1 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var letterSpacing: CGFloat = 0 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var textAllCaps: Bool = false {
        didSet {
            refresh()
        }
    }
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
    }
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        text = super.text
    }
    
    private var _textData: String? = nil
    open override var text: String? {
        get { return _textData ?? super.text }
        set(value) {
            _textData = value
            refresh()
        }
    }
    
    open func refresh() {
        var toSet = _textData ?? ""
        if textAllCaps {
            toSet = toSet.uppercased()
        }
        
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = self.textAlignment
        paragraphStyle.lineHeightMultiple = lineSpacingMultiplier
        
        self.attributedText = NSAttributedString(string: toSet, attributes: [.kern: letterSpacing * font.pointSize, .paragraphStyle: paragraphStyle])
    }
}


@IBDesignable
open class StyledUITextView: UITextView {
    @IBInspectable
    public var lineSpacingMultiplier: CGFloat = 1 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var letterSpacing: CGFloat = 0 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var textAllCaps: Bool = false {
        didSet {
            refresh()
        }
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        text = super.text
    }
    
    private var _textData: String? = nil
    open override var text: String? {
        get { return _textData ?? super.text }
        set(value) {
            _textData = value
            refresh()
        }
    }
    
    open func refresh() {
        var toSet = _textData ?? ""
        if textAllCaps {
            toSet = toSet.uppercased()
        }
        
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = self.textAlignment
        paragraphStyle.lineHeightMultiple = lineSpacingMultiplier
        
        self.attributedText = NSAttributedString(string: toSet, attributes: [.kern: letterSpacing * (font?.pointSize ?? 0.0), NSAttributedString.Key.paragraphStyle: paragraphStyle])
    }
}

@IBDesignable
open class StyledUITextField: UITextField {
    @IBInspectable
    public var lineSpacingMultiplier: CGFloat = 1 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var letterSpacing: CGFloat = 0 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var textAllCaps: Bool = false {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var textColorPlaceholder: UIColor? = nil {
        didSet {
            refresh()
        }
    }
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
//        _textData = super.text
        _placeholderData = super.placeholder
        setup()
        refresh()
    }

    private func setup() {
//        self.addTarget(self, action: #selector(valueChanged), for: )
    }
//
//    private var _textData: String? = nil
//    open override var text: String? {
//        get { return _textData ?? super.text }
//        set(value) {
//            _textData = value
//            refresh()
//        }
//    }
//
    private var _placeholderData: String? = nil
    open override var placeholder: String? {
        get { return _placeholderData ?? super.placeholder }
        set(value) {
            _placeholderData = value
            refresh()
        }
    }
//
//    open func refreshText() {
//        var toSet = _textData ?? ""
//        if textAllCaps {
//            toSet = toSet.uppercased()
//        }
//
//        let paragraphStyle = NSMutableParagraphStyle()
//        paragraphStyle.alignment = self.textAlignment
//        paragraphStyle.lineHeightMultiple = lineSpacingMultiplier
//
//        self.attributedText = NSAttributedString(string: toSet, attributes: [.kern: letterSpacing * (font?.pointSize ?? 0.0), NSAttributedString.Key.paragraphStyle: paragraphStyle])
//    }
    open func refreshPlaceholder() {
        var toSet = _placeholderData ?? ""
        if textAllCaps {
            toSet = toSet.uppercased()
        }

        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = self.textAlignment
        paragraphStyle.lineHeightMultiple = lineSpacingMultiplier

        var attrs: Dictionary<NSAttributedString.Key, Any> = [.kern: letterSpacing * (font?.pointSize ?? 0.0), NSAttributedString.Key.paragraphStyle: paragraphStyle]
        if let placeholderColor = textColorPlaceholder {
            attrs[.foregroundColor] = placeholderColor
        }
        self.attributedPlaceholder = NSAttributedString(string: toSet, attributes: attrs)
    }
    open func refresh() {
//        refreshText()
        refreshPlaceholder()
    }
}

@IBDesignable
open class StyledUIButton: UIButton {
    @IBInspectable
    public var lineSpacingMultiplier: CGFloat = 1 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var letterSpacing: CGFloat = 0 {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var textAllCaps: Bool = false {
        didSet {
            refresh()
        }
    }
    @IBInspectable
    public var textAlignment: NSTextAlignment = .center {
        didSet {
            refresh()
        }
    }
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
    }
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        for state in UIControl.State.options {
            if let x = super.title(for: state), state == .normal || x != super.title(for: .normal) {
                _textData[state.rawValue] = x
            }
        }
        refresh()
    }
    
    private var _textData: Dictionary<UInt, String> = [:]
    override open func setTitle(_ title: String?, for state: UIControl.State) {
        _textData[state.rawValue] = title
        if state == .normal {
            refresh()
        } else {
            refresh(state: state)
        }
    }
    override open func title(for state: UIControl.State) -> String? {
        return _textData[state.rawValue] ?? super.title(for: state)
    }
    
    public func refresh() {
        for state in UIControl.State.options {
            refresh(state: state)
        }
    }
    
    open func refresh(state: UIControl.State) {
        var toSet = _textData[state.rawValue] ?? _textData[State.normal.rawValue] ?? ""
        if textAllCaps {
            toSet = toSet.uppercased()
        }
        
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = self.textAlignment
        paragraphStyle.lineHeightMultiple = lineSpacingMultiplier
        let font = titleLabel?.font
        
        self.setAttributedTitle(NSAttributedString(string: toSet, attributes: [.kern: letterSpacing * (font?.pointSize ?? 0.0), NSAttributedString.Key.paragraphStyle: paragraphStyle, .foregroundColor: self.titleColor(for: state) ?? self.titleColor(for: .normal) ?? UIColor.black]), for: state)
    }
}

private extension UIControl.State {
    static let options =  [
        UIControl.State.normal,
        UIControl.State.disabled,
        UIControl.State.focused,
        UIControl.State.highlighted,
        UIControl.State.reserved,
        UIControl.State.selected,
        UIControl.State.application
    ]
}

public extension UITextView {
    func setTextHtml(html: String) {
        let fullHtml = """
        <!doctype html>
        <html>
          <head>
            <style>
              body {
                font-family: -apple-system;
                font-size: \(self.font?.pointSize ?? 14)pt;
                color: \(self.textColor?.hexString ?? "black");
              }
            </style>
          </head>
          <body>
            \(html)
          </body>
        </html>
        """
        let htmlData = NSString(string: fullHtml).data(using: String.Encoding.unicode.rawValue)
        let attributedString = try! NSAttributedString(
            data: htmlData!,
            options: [.documentType: NSAttributedString.DocumentType.html],
            documentAttributes: nil
        )
        self.attributedText = attributedString
    }
}

public extension UILabel {
    func setTextHtml(html: String) {
        let fullHtml = """
        <!doctype html>
        <html>
          <head>
            <style>
              body {
                font-family: -apple-system;
                font-size: \(self.font.pointSize)pt;
                color: \(self.textColor.hexString);
              }
            </style>
          </head>
          <body>
            \(html)
          </body>
        </html>
        """
        print(fullHtml)
        let htmlData = NSString(string: fullHtml).data(using: String.Encoding.unicode.rawValue)
        let attributedString = try! NSAttributedString(
            data: htmlData!,
            options: [.documentType: NSAttributedString.DocumentType.html],
            documentAttributes: nil
        )
        self.attributedText = attributedString
    }
}

private extension UIColor {
    var hexString: String {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0

        let multiplier = CGFloat(255.999999)

        self.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

        if alpha == 1.0 {
            return String(
                format: "#%02lX%02lX%02lX",
                Int(red * multiplier),
                Int(green * multiplier),
                Int(blue * multiplier)
            )
        }
        else {
            return String(
                format: "#%02lX%02lX%02lX%02lX",
                Int(red * multiplier),
                Int(green * multiplier),
                Int(blue * multiplier),
                Int(alpha * multiplier)
            )
        }
    }
}
