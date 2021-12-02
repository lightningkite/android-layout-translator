//
//  Dropdown.swift
//  Lifting Generations
//
//  Created by Joseph Ivie on 3/28/19.
//  Copyright © 2019 Joseph Ivie. All rights reserved.
//

import UIKit


@IBDesignable
public class InputViewButton : UIButton {
    public let toolbar: UIToolbar = {
        let toolBar = UIToolbar()
        toolBar.barStyle = UIBarStyle.default
        toolBar.isTranslucent = true
        toolBar.sizeToFit()
        return toolBar
    }()

    override public init(frame: CGRect) {
        super.init(frame: frame)
        sharedInit()
    }

    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        sharedInit()
    }
    private func sharedInit() {
        self.isUserInteractionEnabled = true
        let doneButton = UIBarButtonItem(title: "Done", style: UIBarButtonItem.Style.plain, target: self, action: #selector(doneClick))
        let spaceButton = UIBarButtonItem(barButtonSystemItem: UIBarButtonItem.SystemItem.flexibleSpace, target: nil, action: nil)
        toolbar.setItems([ spaceButton, doneButton], animated: false)

        let tapRecognizer = UITapGestureRecognizer(target: self, action: #selector(launchPicker))
        self.addGestureRecognizer(tapRecognizer)
    }

    override public var canBecomeFirstResponder: Bool {
        get {
            return true
        }
    }

    private var _inputView: UIView = UIPickerView()
    override public var inputView: UIView {
        get { return _inputView }
        set(value) { _inputView = value }
    }
    override public var inputAccessoryView: UIView? {
        return toolbar
    }

    @objc public func launchPicker() {
        becomeFirstResponder()
    }

    @objc public func doneClick() {
        resignFirstResponder()
    }

}
