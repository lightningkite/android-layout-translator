//
//  onLayoutSubviewsSwizzle.swift
//  KhrysalisTemplate
//
//  Created by Joseph Ivie on 8/6/19.
//  Copyright © 2019 Joseph Ivie. All rights reserved.
//
import Foundation
import UIKit

extension UIView {

    private static var extOngoing = WeakDictionary<UIView, Bool>()
    private static var ext = WeakDictionary<UIView, Array<(UIView)->Bool>>()
    public var onLayoutSubviewsOngoing: Bool {
        get {
            return UIView.extOngoing[self] ?? false
        }
        set(value) {
            UIView.extOngoing[self] = value
        }
    }
    public var onLayoutSubviews: Array<(UIView)->Bool> {
        get {
            if let current = UIView.ext[self] {
                return current
            }
            return []
        }
        set(value) {
            UIView.ext[self] = value
        }
    }



    private static let theSwizzler: Void = {
        let instance = UIView(frame: .zero)
        let aClass: AnyClass! = object_getClass(instance)
        let originalMethod = class_getInstanceMethod(aClass, #selector(layoutSubviews))
        let swizzledMethod = class_getInstanceMethod(aClass, #selector(swizzled_layoutSubviews))
        if let originalMethod = originalMethod, let swizzledMethod = swizzledMethod {
            // switch implementation..
            method_exchangeImplementations(originalMethod, swizzledMethod)
        }
    }()
    public static func useLayoutSubviewsLambda() {
        _ = theSwizzler
        _ = UIButton.theSwizzler
    }
    @objc func swizzled_layoutSubviews(){
        self.swizzled_layoutSubviews()
        if !onLayoutSubviewsOngoing {
            onLayoutSubviewsOngoing = true
            self.onLayoutSubviews.removeAll(where: { $0(self) })
            onLayoutSubviewsOngoing = false
        }
    }
}

extension UIButton {
    fileprivate static let theSwizzler: Void = {
        let instance = UIButton(frame: .zero)
        let aClass: AnyClass! = object_getClass(instance)
        let originalMethod = class_getInstanceMethod(aClass, #selector(layoutSubviews))
        let swizzledMethod = class_getInstanceMethod(aClass, #selector(swizzled_layoutSubviewsButton))
        if let originalMethod = originalMethod, let swizzledMethod = swizzledMethod {
            // switch implementation..
            method_exchangeImplementations(originalMethod, swizzledMethod)
        }
    }()
    @objc func swizzled_layoutSubviewsButton(){
        self.swizzled_layoutSubviewsButton()
        if !onLayoutSubviewsOngoing {
            onLayoutSubviewsOngoing = true
            self.onLayoutSubviews.removeAll(where: { $0(self) })
            onLayoutSubviewsOngoing = false
        }
    }
}
