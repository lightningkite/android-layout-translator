//
//  CAStateLayer.swift
//  Butterfly Template
//
//  Created by Joseph Ivie on 9/26/19.
//  Copyright © 2019 Joseph Ivie. All rights reserved.
//

import Foundation
import UIKit

open class AutosizeCALayer: CALayer {
    public struct LayerWithParams {
        var layer: CALayer
        var insets: UIEdgeInsets = .zero
        var scaleOverResize: Bool = true
        public init(
            layer: CALayer,
            insets: UIEdgeInsets = .zero,
            scaleOverResize: Bool = true
        ) {
            self.layer = layer
            self.insets = insets
            self.scaleOverResize = scaleOverResize
        }
    }
    open func addArrangedSublayer(_ layerWithParams: LayerWithParams) {
        addSublayer(layerWithParams.layer)
        childInsets[layerWithParams.layer] = layerWithParams.insets
        childScaleOverResize[layerWithParams.layer] = layerWithParams.scaleOverResize
    }
    public var childInsets: WeakDictionary<CALayer, UIEdgeInsets> = WeakDictionary()
    public var childScaleOverResize: WeakDictionary<CALayer, Bool> = WeakDictionary()
    public var childBaseSize: WeakDictionary<CALayer, CGSize> = WeakDictionary()
    public override init() {
        super.init()
        self.needsDisplayOnBoundsChange = true
    }
    public override init(layer: Any) {
        super.init(layer: layer)

        guard let layer = layer as? AutosizeCALayer else { return }

        for index in (sublayers ?? []).indices {
            if let child = sublayers?[index], let otherChild = layer.sublayers?[index] {
                self.childInsets[child] = layer.childInsets[otherChild]
                self.childScaleOverResize[child] = layer.childScaleOverResize[otherChild]
                self.childBaseSize[child] = layer.childBaseSize[otherChild]
            }
        }
    }
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.needsDisplayOnBoundsChange = true
    }
    public override func layoutSublayers() {
        for layer in sublayers ?? [] {
            var goalRect = self.bounds
            if let inset = childInsets[layer] {
                goalRect = CGRect(x: bounds.minX + inset.left, y: bounds.minY + inset.right, width: bounds.width - inset.left - inset.right, height: bounds.height - inset.top - inset.bottom)
            }
            
            var baseSize: CGSize = .zero
            if let stored = childBaseSize[layer] {
                baseSize = stored
            } else {
                baseSize = layer.bounds.size
                childBaseSize[layer] = baseSize
            }
            
            CATransaction.withDisabledActions {
                if childScaleOverResize[layer] ?? false {
                    layer.setAffineTransform(.identity)
                    layer.frame = goalRect
                    let xScale = goalRect.size.width / baseSize.width
                    let yScale = goalRect.size.height / baseSize.height
                    layer.setAffineTransform(
                        CGAffineTransform.identity
                            .translatedBy(
                                x: -(1 - xScale) * bounds.size.width / 2,
                                y: -(1 - yScale) * bounds.size.height / 2
                            )
                            .scaledBy(
                                x: xScale,
                                y: yScale
                            )
                    )
                } else {
                    layer.frame = goalRect
                }
            }
        }
    }
}

public extension CALayer {
    @objc func attatch(to control: UIControl) {
        for sublayer in sublayers ?? [] {
            sublayer.attatch(to: control)
        }
    }
}

public class StateCALayer: AutosizeCALayer {
    public let states: StateSelector<CALayer>
    public init(states: StateSelector<CALayer>) {
        self.states = states
        super.init()
        for layer in states.values {
            addSublayer(layer)
            layer.isHidden = true
        }
        self.bounds.size = CGSize(
            width: states.values.map { $0.bounds.size.width }.max() ?? 8,
            height: states.values.map { $0.bounds.size.height }.max() ?? 8
        )
        states.normal.isHidden = false
    }
    
    public func update(to: UIControl.State) {
        CATransaction.withDisabledActions {
            for layer in states.values {
                layer.isHidden = true
            }
            states.get(to).isHidden = false
            self.setNeedsDisplay()
        }
    }
    
    private var observers: Array<NSKeyValueObservation> = []
    
    @objc public override func attatch(to control: UIControl) {
        self.update(to: control.state)
        observers = [
            control.observe(\.isHighlighted) { [weak self] (control, change) in
                guard let self = self else { return }
                self.update(to: control.state)
            },
            control.observe(\.isSelected) { [weak self] (control, change) in
                guard let self = self else { return }
                self.update(to: control.state)
            },
            control.observe(\.isEnabled) { [weak self] (control, change) in
                guard let self = self else { return }
                self.update(to: control.state)
            }
        ]
    }
    
    required init?(coder: NSCoder) {
        self.states = StateSelector(normal: CALayer())
        super.init()
    }
    
    public override init(layer: Any) {
        guard let layer = layer as? StateCALayer else {
            self.states = StateSelector(normal: CALayer())
            super.init(layer: layer)
            return
        }
        self.states = StateSelector(
            normal: CALayer(layer: layer.states.normal),
            selected: layer.states.selected.map { CALayer(layer: $0) },
            highlighted: layer.states.highlighted.map { CALayer(layer: $0) },
            disabled: layer.states.disabled.map { CALayer(layer: $0) },
            focused: layer.states.focused.map { CALayer(layer: $0) }
        )
        super.init()
        for layer in states.values {
            addSublayer(layer)
        }
    }
}

open class RectCALayer: CALayer {
    public var maxCornerRadius: CGFloat = 0
    open override func layoutSublayers() {
        self.cornerRadius = min(min(maxCornerRadius, bounds.size.width/2), bounds.size.height/2)
    }
}

open class GradientRectCALayer: CAGradientLayer {
    public var maxCornerRadius: CGFloat = 0
    open override func layoutSublayers() {
        self.cornerRadius = min(min(maxCornerRadius, bounds.size.width/2), bounds.size.height/2)
    }
}

public enum LayerMaker {
    public static func autosize(_ layers: AutosizeCALayer.LayerWithParams...) -> AutosizeCALayer {
        let layer = AutosizeCALayer()
        for sub in layers {
            layer.addArrangedSublayer(sub)
        }
        return layer
    }
    public static func state(_ layers: StateSelector<CALayer>) -> StateCALayer {
        return StateCALayer(states: layers)
    }
    public static func oval(
        fillColor: UIColor,
        strokeColor: UIColor,
        strokeWidth: CGFloat
    ) -> CAShapeLayer {
        let layer = CAShapeLayer()
        layer.path = CGPath(ellipseIn: CGRect(x: 0, y: 0, width: 100, height: 100), transform: nil)
        layer.frame.size = CGSize(width: 100, height: 100)
        layer.lineWidth = strokeWidth
        layer.strokeColor = strokeColor.cgColor
        layer.fillColor = fillColor.cgColor
        return layer
    }
    public static func ovalGradient(
        startColor: UIColor,
        midColor: UIColor?,
        endColor: UIColor,
        gradientAngle: CGFloat,
        strokeColor: UIColor,
        strokeWidth: CGFloat
    ) -> CAGradientLayer {
        let layer = CAShapeLayer()
        layer.path = CGPath(ellipseIn: CGRect(x: 0, y: 0, width: 100, height: 100), transform: nil)
        layer.frame.size = CGSize(width: 100, height: 100)
        layer.lineWidth = strokeWidth
        let gradient = CAGradientLayer()
        gradient.mask = layer
        gradient.colors = [startColor, midColor, endColor]
            .compactMap { $0?.cgColor }
        gradient.setGradientAngle(degrees: gradientAngle)
        return gradient
    }
    public static func rect(
        fillColor: UIColor,
        strokeColor: UIColor,
        strokeWidth: CGFloat,
        topLeftRadius: CGFloat,
        topRightRadius: CGFloat,
        bottomLeftRadius: CGFloat,
        bottomRightRadius: CGFloat
    ) -> CALayer {
        let layer = CALayer()
        layer.backgroundColor = fillColor.cgColor
        layer.borderWidth = strokeWidth
        layer.borderColor = strokeColor.cgColor
        layer.cornerRadius = max(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius)
        if #available(iOS 11.0, *) {
            layer.maskedCorners = CACornerMask([
                topLeftRadius > 0 ? CACornerMask.layerMinXMinYCorner : nil,
                topRightRadius > 0 ? CACornerMask.layerMaxXMinYCorner : nil,
                bottomLeftRadius > 0 ? CACornerMask.layerMinXMaxYCorner : nil,
                bottomRightRadius > 0 ? CACornerMask.layerMaxXMaxYCorner : nil
            ].compactMap { $0 })
        }
        return layer
    }
    public static func rectGradient(
        startColor: UIColor,
        midColor: UIColor?,
        endColor: UIColor,
        gradientAngle: CGFloat,
        strokeColor: UIColor,
        strokeWidth: CGFloat,
        topLeftRadius: CGFloat,
        topRightRadius: CGFloat,
        bottomLeftRadius: CGFloat,
        bottomRightRadius: CGFloat
    ) -> CALayer {
        let layer = CAGradientLayer()
        layer.colors = [startColor, midColor, endColor]
            .compactMap { $0?.cgColor }
        layer.setGradientAngle(degrees: gradientAngle)
        layer.borderWidth = strokeWidth
        layer.borderColor = strokeColor.cgColor
        layer.cornerRadius = max(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius)
        if #available(iOS 11.0, *) {
            layer.maskedCorners = CACornerMask([
                topLeftRadius > 0 ? CACornerMask.layerMinXMinYCorner : nil,
                topRightRadius > 0 ? CACornerMask.layerMaxXMinYCorner : nil,
                bottomLeftRadius > 0 ? CACornerMask.layerMinXMaxYCorner : nil,
                bottomRightRadius > 0 ? CACornerMask.layerMaxXMaxYCorner : nil
            ].compactMap { $0 })
        }
        return layer
    }
}

public protocol CALayerToImage {
    func toImage() -> UIImage?
}

extension CATransaction {
    class func withDisabledActions<T>(_ body: () throws -> T) rethrows -> T {
        if UIView.areAnimationsEnabled {
            return try body()
        }
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        defer {
            CATransaction.commit()
        }
        return try body()
    }
}

extension CALayer : CALayerToImage {

    @objc public func toImage() -> UIImage? {
        if CFGetTypeID(self.contents as CFTypeRef) == CGImage.typeID {
            return UIImage(cgImage: self.contents as! CGImage)
        } else {
            setNeedsDisplay()
            UIGraphicsBeginImageContextWithOptions(self.bounds.size, self.isOpaque, 0.0)
            guard let ctx = UIGraphicsGetCurrentContext() else {
                print("WARNING!  NO CURRENT CONTEXT!")
                return nil
            }
            self.render(in: ctx)
            let img = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            return img
        }
    }
}

public class CAImageLayer: CALayer {
    public var image: UIImage? = nil {
        didSet {
            self.contents = image?.cgImage
            self.bounds.size = image?.size ?? .zero
        }
    }
    @objc override public func toImage() -> UIImage? {
        return image
    }
    public convenience init(_ image: UIImage?) {
        self.init()
        self.image = image
        self.contents = image?.cgImage
        self.bounds.size = image?.size ?? .zero
    }
}

public extension CAGradientLayer {
    func setGradientAngle(degrees: CGFloat){
        let radius: CGFloat = 0.5
        let radians = degrees * CGFloat.pi / 180
        print(radians)
        self.startPoint = CGPoint(
            x: 0.5 - cos(radians) * radius,
            y: 0.5 + sin(radians) * radius
        )
        self.endPoint = CGPoint(
            x: 0.5 + cos(radians) * radius,
            y: 0.5 - sin(radians) * radius
        )
    }
}

public extension UIView {
    private static let isJoinedContainer = WeakDictionary<UIView, Bool>()
    private static let backgroundLayerMap = WeakDictionary<UIView, CALayer?>()
    static var backgroundLayersByName: Dictionary<String, ()->CALayer> = [:]
    
    var relatedControl: UIControl? {
        get {
            if let self = self as? UIControl { return self }
            let containerView = self.containerView
            let containerControl = containerView.findChild { $0 is UIControl } as? UIControl
            return containerControl
        }
    }
    private func forChildren(action: (UIView)->Void) {
        action(self)
        for child in subviews {
            child.forChildren(action: action)
        }
    }
    private func findChild(condition: (UIView)->Bool) -> UIView? {
        if condition(self) { return self }
        else {
            for child in subviews {
                if let result = child.findChild(condition: condition) {
                    return result
                }
            }
            return nil
        }
    }
    var containerView: UIView {
        get {
            if let superview = superview, superview is ContainerView {
                return superview.containerView
            } else {
                return self
            }
        }
    }
    
    @IBInspectable var backgroundLayerName: String {
        get {
            return ""
        }
        set(value) {
            if let maker = UIView.backgroundLayersByName[value] {
                let newLayer = maker()
                backgroundLayerForThis = newLayer
            }
        }
    }
    
    var backgroundLayer: CALayer? {
        get { containerView.backgroundLayerForThis }
        set(value) { containerView.backgroundLayerForThis = value }
    }
    
    var backgroundLayerForThis: CALayer? {
        get {
            return UIView.backgroundLayerMap[self] ?? nil
        }
        set(value) {
            let previousDoubleNillable = UIView.backgroundLayerMap[self]
            if previousDoubleNillable == nil {
                self.onLayoutSubviews.append { view in
                    CATransaction.withDisabledActions {
                        if let layer2 = UIView.backgroundLayerMap[view], let layer = layer2 {
                            layer.frame = view.bounds
                        }
                    }
                    return false
                }
            }
            let previous = previousDoubleNillable ?? nil
            previous?.removeFromSuperlayer()
            if let value = value {
                self.layer.insertSublayer(value, at: 0)
                value.frame = self.layer.frame
                value.zPosition = CGFloat(-Float.greatestFiniteMagnitude)
                UIView.backgroundLayerMap[self] = value
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.01, execute: {
                    if let control = self.relatedControl {
                        value.attatch(to: control)
                    }
                })
            } else {
                UIView.backgroundLayerMap[self] = nil
            }
        }
    }
}
