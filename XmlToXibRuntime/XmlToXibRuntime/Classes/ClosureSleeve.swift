//
//  ClosureSleeve.swift
//  XmlToXibRuntime
//
//  Created by Joseph Ivie on 9/10/21.
//

import Foundation
import UIKit

public extension UIView {
    private static let observations = WeakDictionary<UIView, Array<NSKeyValueObservation>>()
    var strongObservers: Array<NSKeyValueObservation> {
        get {
            return UIControl.observations[self] ?? []
        }
        set(value) {
            UIControl.observations[self] = value
        }
    }
}

public extension UIControl {
    private class ListenerDistributor {
        var listeners: Array<(UIControl.State)->Bool> = []
        var observers: Array<NSKeyValueObservation> = []
        init(control: UIControl) {
            observers = [
                control.observe(\.isHighlighted) { [weak self] (control, change) in
                    guard let self = self else { return }
                    self.listeners.removeAll(where: { $0(control.state) })
                },
                control.observe(\.isSelected) { [weak self] (control, change) in
                    guard let self = self else { return }
                    self.listeners.removeAll(where: { $0(control.state) })
                },
                control.observe(\.isEnabled) { [weak self] (control, change) in
                    guard let self = self else { return }
                    self.listeners.removeAll(where: { $0(control.state) })
                }
            ]
        }
    }
    private static let distributors = WeakDictionary<UIControl, ListenerDistributor>()
    var stateListeners: Array<(UIControl.State)->Bool> {
        get {
            return UIControl.distributors[self]?.listeners ?? []
        }
        set(value) {
            UIControl.distributors.getOrPut(self, { ListenerDistributor(control: self) }).listeners = value
        }
    }
}
