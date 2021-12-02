//
//  LabeledToggle.swift
//  XmlToXibRuntime
//
//  Created by Joseph Ivie on 9/20/21.
//

import UIKit
import M13Checkbox

open class LabeledToggle: UIStackView {
    
    private var _label: UILabel?
    public var label: UILabel {
        get {
            if let _label = _label { return _label }
            _label = self.subviews.first { $0 is UILabel } as? UILabel
            if let _label = _label { return _label }
            else { fatalError("No label contained by the labeled toggle.") }
        }
    }
    
    private var _toggle: ToggleView?
    public var toggle: ToggleView {
        get {
            if let _toggle = _toggle { return _toggle }
            _toggle = self.subviews.first { $0 is ToggleView } as? ToggleView
            if let _toggle = _toggle { return _toggle }
            else { fatalError("No toggle contained by the labeled toggle.") }
        }
    }
    
    open override func addSubview(_ view: UIView) {
        super.addSubview(view)
        if let l = self.subviews.first(where: { $0 is UILabel }) as? UILabel {
            print("Enabling gesture")
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(userDidTapLabel(tapGestureRecognizer:)))
            l.isUserInteractionEnabled = true
            l.addGestureRecognizer(tapGesture)
        }
    }
    
    @objc func userDidTapLabel(tapGestureRecognizer: UITapGestureRecognizer) {
        toggle.toggleIsOn()
    }

}

public protocol ToggleView: UIControl {
    var isOn: Bool { get set }
    func setOn(_ state: Bool, animated: Bool)
    func toggleIsOn()
}

extension UISwitch: ToggleView {
    public func toggleIsOn() {
        self.setOn(!isOn, animated: true)
    }
}
extension M13Checkbox: ToggleView {
    public func toggleIsOn() {
        self.toggleCheckState(true)
        self.sendActions(for: .valueChanged)
    }
    
    public func setOn(_ state: Bool, animated: Bool) {
        self.setCheckState(state ? .checked : .unchecked, animated: animated)
        self.sendActions(for: .valueChanged)
    }
    
    public var isOn: Bool {
        get { checkState == .checked }
        set {
            checkState = newValue ? .checked : .unchecked
            self.sendActions(for: .valueChanged)
        }
    }
}

@IBDesignable
open class ToggleButton: StyledUIButton, ToggleView {
    public var isOn: Bool {
        get { self.isSelected }
        set(value) {
            self.isSelected = value
            self.sendActions(for: .valueChanged)
        }
    }
    public func setOn(_ state: Bool, animated: Bool) {
        self.isSelected = state
    }
    public override init(frame: CGRect) {
        super.init(frame: frame)
    }
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.addTarget(self, action: #selector(buttonClicked), for: .touchUpInside)
    }
    @objc public func buttonClicked() {
        self.isOn = !self.isOn
    }
    public func toggleIsOn() {
        self.setOn(!isOn, animated: true)
    }
}
