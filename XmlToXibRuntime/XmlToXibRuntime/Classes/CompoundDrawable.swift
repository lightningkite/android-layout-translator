//
//  CompoundDrawable.swift
//  RxSwiftPlus
//
//  Created by Joseph Ivie on 11/16/21.
//

import UIKit

public extension UIView {
    private var compoundDrawableView: UIView? {
        return self.superview?.subviews.dropFirst().first
    }
    var compoundDrawable: (()->CALayer)? {
        get { fatalError() }
        set(value) { self.compoundDrawableView?.backgroundLayerForThis = value?() }
    }
}
